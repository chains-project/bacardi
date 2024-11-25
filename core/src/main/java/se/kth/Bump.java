package se.kth;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.Util.JsonUtils;
import se.kth.Util.LogUtils;
import se.kth.model.BreakingUpdate;
import se.kth.model.SetupPipeline;
import se.kth.models.FailureCategory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static se.kth.Util.BreakingUpdateProvider.*;
import static se.kth.Util.Constants.BENCHMARK_PATH;
import static se.kth.Util.Constants.JAVA_VERSION_INCOMPATIBILITY_FILE;

public class Bump {

    private static final Logger log = LoggerFactory.getLogger(Bump.class);

    private final static String CLIENT_PATH = "/Users/frank/Documents/Work/PHD/bacardi/projects";

    static Set<Result> resultsList = new HashSet<>();
    static Map<String, Result> resultsMap = new HashMap<>();
    private final static String JSON_PATH = "result_repair_test33.json";


    public static void main(String[] args) {


        LogUtils.logWithBox(log, "Bump analysis started");

        //filtering breaking dependency updates
        log.info("Filtering breaking dependency updates");
        log.info("");
        // Filter by java version incompatibility
        ArrayList<String> listOfJavaVersionIncompatibilities = readJavaVersionIncompatibilities(JAVA_VERSION_INCOMPATIBILITY_FILE);


        // List of breaking updates
//        List<BreakingUpdate> breaking = BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH, FailureCategory.COMPILATION_FAILURE, listOfJavaVersionIncompatibilities);
        List<BreakingUpdate> breaking = BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH, FailureCategory.COMPILATION_FAILURE);

        // read Json file with attempts information
        resultsMap.putAll(getPreviousResults(JSON_PATH));

        DockerBuild dockerBuild = new DockerBuild(true);
        // identify breaking updates and download image and copy the project


        breaking
                .stream()
                .filter(e -> e.breakingCommit.equals("bdbb81614557858922836294d1d6dd3dd661f10c")) // filter by breaking commit
//                .filter(e -> javaVersionIncompatibilityLines.contains(e.breakingCommit)) // filter by failure category
                .forEach(e -> {

                    //starting processing breaking update

                    LogUtils.logWithBox(log, "Processing breaking update: %s".formatted(e.breakingCommit));
                    //Full path Folder/breaking-commit/project

                    Path clientFolder = settingClientFolderAndM2Folder(e, dockerBuild);

                    SetupPipeline setupPipeline = new SetupPipeline();
                    //adding breaking update to the pipeline
                    setupPipeline.setBreakingUpdate(e);
                    //adding docker build to the pipeline
                    setupPipeline.setDockerBuild(dockerBuild);
                    //adding client folder to the pipeline
                    setupPipeline.setClientFolder(clientFolder.resolve(e.project));
                    //adding log file path to the pipeline
                    setupPipeline.setLogFilePath(Path.of("%s/%s.log".formatted(clientFolder.resolve(e.project), e.breakingCommit)));
                    //adding m2 folder path to the pipeline
                    setupPipeline.setM2FolderPath(Path.of("%s/%s/m2/".formatted(CLIENT_PATH, e.breakingCommit)));
                    //adding docker image to the pipeline
                    setupPipeline.setDockerImage(e.breakingUpdateReproductionCommand.replace("docker run ", ""));

                    //start repair process
                    repair(setupPipeline);
                });
    }

    private static Path settingClientFolderAndM2Folder(BreakingUpdate e, DockerBuild dockerBuild) {
        //Setting same m2 folder for the new docker image
        final var clientFolder = Path.of("%s/%s".formatted(CLIENT_PATH, e.breakingCommit));
        if (Files.exists(clientFolder)) {
            log.info("Project already exists. Skipping download process...");
        } else {
            // copy m2 folder to local path
            Path localPath = Path.of("%s/%s/m2/".formatted(CLIENT_PATH, e.breakingCommit));
            Path fromContainerM2 = Path.of("/root/.m2/");
            //Copy m2 folder from container
            getProjectData(e, dockerBuild, fromContainerM2, localPath);

            //copy project from container
            Path fromContainerProject = Path.of(e.project);
            String imageId = getProjectData(e, dockerBuild, fromContainerProject, clientFolder);
            //delete Image
//            if (imageId != null) {
//                            dockerBuild.deleteImage(imageId);
//            }
        }
        return clientFolder;
    }


    /**
     * Repair the breaking update
     *
     * @param setupPipeline the pipeline setup
     */
    public static void repair(SetupPipeline setupPipeline) {

    /*
      Read the log file and extract the failure category
      If the failure category is BUILD_SUCCESS, no need to analyze further
     */
        File logFile = setupPipeline.getLogFilePath().toFile();

        FailureCategoryExtract failureCategoryExtract = new FailureCategoryExtract(logFile);

        final FailureCategory failureCategory = failureCategoryExtract.getFailureCategory(logFile);

        if (failureCategory == FailureCategory.BUILD_SUCCESS) {
            log.info("Build success. No need to analyze further.");
            return;
        }

        BacardiCore bacardiCore = new BacardiCore(setupPipeline, failureCategoryExtract, true);

        Result results = bacardiCore.analyze();

        resultsMap.put(setupPipeline.getBreakingUpdate().breakingCommit, results);
        JsonUtils.writeToFile(Path.of(JSON_PATH), resultsMap);

    }


}
