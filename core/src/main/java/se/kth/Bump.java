package se.kth;


import com.fasterxml.jackson.databind.JavaType;
import com.github.dockerjava.api.command.CreateContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.Util.JsonUtils;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static se.kth.Util.BreakingUpdateProvider.readLinesFromFile;
import static se.kth.Util.Constants.BENCHMARK_PATH;

public class Bump {

    private static final Logger log = LoggerFactory.getLogger(Bump.class);

    private final static String CLIENT_PATH = "/Users/frank/Documents/Work/PHD/bacardi/projects";

    static Set<Result> resultsList = new HashSet<>();
    private final static String JSON_PATH = "result_repair_test1.json";


    public static void main(String[] args) {


        log.info("");
        log.info("*********************************************");
        log.info("Bump analysis started");
        log.info("*********************************************");
        log.info("");


        //filtering breaking dependency updates
        log.info("Filtering breaking dependency updates");
        log.info("");

        // List of breaking updates
        List<BreakingUpdate> breaking = BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH, FailureCategory.COMPILATION_FAILURE);

        //enable filter for only specific breaking update category
        String javaVersionIncompatibilityFilePath = "/Users/frank/Documents/Work/PHD/BUMP/bump-execution/analysis/client_fail_due_java_version_incompatibility.txt";

        ArrayList<String> javaVersionIncompatibilityLines = readLinesFromFile(javaVersionIncompatibilityFilePath);

        // read Json file with attempts information
        JavaType type = JsonUtils.getTypeFactory().constructCollectionType(List.class, Result.class);

        if (!Files.exists(Path.of(JSON_PATH))) {
            try {
                Files.createFile(Path.of(JSON_PATH));
                JsonUtils.writeToFile(Path.of(JSON_PATH), new ArrayList<Result>());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        resultsList.addAll(JsonUtils.readFromFile(Path.of(JSON_PATH), type));


        DockerBuild dockerBuild = new DockerBuild(true);
        // identify breaking updates and download image and copy the project

        breaking
                .stream()
                .filter(e -> e.breakingCommit.equals("bdbb81614557858922836294d1d6dd3dd661f10c")) // filter by breaking commit
//                .filter(e -> javaVersionIncompatibilityLines.contains(e.breakingCommit)) // filter by failure category
                .forEach(e -> {

                    log.info("");
                    log.info("|-------------------------------------------|");
                    log.info("|Processing breaking update: %s".formatted(e.breakingCommit));
                    log.info("|-------------------------------------------|");
                    log.info("");


                    final var clientFolder = Path.of("%s/%s".formatted(CLIENT_PATH, e.breakingCommit));
                    if (Files.exists(clientFolder)) {
                        log.info("Project already exists. Skipping download process...");
                    } else {
                        // copy m2 folder to local path
                        Path localPath = Path.of("%s/%s/m2/".formatted(CLIENT_PATH, e.breakingCommit));
                        Path fromContainerM2 = Path.of("/root/.m2/");
                        getProjectData(e, dockerBuild, fromContainerM2, localPath);

                        //copy project from container
                        Path fromContainerProject = Path.of(e.project);
                        String imageId = getProjectData(e, dockerBuild, fromContainerProject, clientFolder);


                        //delete Image
                        if (imageId != null) {
//                            dockerBuild.deleteImage(imageId);
                        }
                    }


                    repair(e, dockerBuild, clientFolder);
                });
    }


    /**
     * Repairs the given project based on the breaking update.
     *
     * @param breakingUpdate The breaking update information.
     * @param dockerBuild    The Docker build instance.
     * @param project        The path to the project.
     */
    public static void repair(BreakingUpdate breakingUpdate, DockerBuild dockerBuild, Path project) {

    /*
      Read the log file and extract the failure category
      If the failure category is BUILD_SUCCESS, no need to analyze further
     */
        File logFile = new File("%s/%s/%s/%s.log".formatted(CLIENT_PATH, breakingUpdate.breakingCommit, breakingUpdate.project, breakingUpdate.breakingCommit));

        FailureCategoryExtract failureCategoryExtract = new FailureCategoryExtract(logFile);

        final FailureCategory failureCategory = failureCategoryExtract.getFailureCategory(logFile);

        if (failureCategory == FailureCategory.BUILD_SUCCESS) {
            log.info("Build success. No need to analyze further.");
            return;
        }

        BacardiCore bacardiCore = new BacardiCore(project.resolve(breakingUpdate.project), logFile.toPath(), failureCategoryExtract, true);

        Result results = bacardiCore.analyze();
        results.setBreakingCommit(breakingUpdate.breakingCommit);
        resultsList.add(results);

        JsonUtils.writeToFile(Path.of(JSON_PATH), resultsList);

    }


    private static String getProjectData(BreakingUpdate breakingUpdate, DockerBuild dockerBuild, Path fromContainer, Path toLocal) {

        String imageId = null;
        try {
            imageId = breakingUpdate.breakingUpdateReproductionCommand.replace("docker run ", "");
            String[] entrypoint = new String[]{"/bin/sh"};

            //pull image
            dockerBuild.ensureBaseMavenImageExists(imageId);
            CreateContainerResponse container = dockerBuild.startContainerEntryPoint(imageId, entrypoint);

            // Copy m2 folder to local path in the breaking commit folder
            dockerBuild.copyM2FolderToLocalPath(container.getId(), fromContainer, toLocal);

            dockerBuild.removeContainer(container.getId());

            return imageId;


        } catch (InterruptedException e) {
            log.error("Something went wrong", e);
        }
        return imageId;
    }


}
