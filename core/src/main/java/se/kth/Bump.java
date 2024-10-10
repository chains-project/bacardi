package se.kth;

import com.github.dockerjava.api.command.CreateContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;
import se.kth.utils.JsonUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static se.kth.Util.Constants.BENCHMARK_PATH;

public class Bump {

    private static final Logger log = LoggerFactory.getLogger(Bump.class);

    private final static String CLIENT_PATH = "/Users/frank/Documents/Work/PHD/bacardi/projects";

    static List<Result> resultsList = new ArrayList<>();


    public static void main(String[] args) {


        log.info("");
        log.info("*********************************************");
        log.info("Bump analysis started");
        log.info("*********************************************");
        log.info("");


        //filtering breaking dependency updates
        log.info("Filtering breaking dependency updates");
        log.info("");


        List<BreakingUpdate> breaking = BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH, FailureCategory.COMPILATION_FAILURE);

        log.info("Breaking dependency updates: {}", breaking.size());
        log.info("");
        breaking.forEach(e -> log.info("{}", e.breakingCommit));
        log.info("");


        DockerBuild dockerBuild = new DockerBuild(true);
        // identify breaking updates and download image and copy the project

        breaking.stream()
                .filter(e -> e.breakingCommit.equals("c8da6c3c823d745bb37b072a4a33b6342a86dcd9"))
                .forEach(e -> {

                    // copy m2 folder to local path
                    Path localPath = Path.of("%s/%s/m2/".formatted(CLIENT_PATH, e.breakingCommit));
                    Path fromContainerM2 = Path.of("/root/.m2/");
                    getProjectData(e, dockerBuild, fromContainerM2, localPath);

                    //copy project from container
                    Path fromContainerProject = Path.of(e.project);
                    Path toLocalProject = Path.of("%s/%s".formatted(CLIENT_PATH, e.breakingCommit));
                    getProjectData(e, dockerBuild, fromContainerProject, toLocalProject);


                    repair(e, dockerBuild, toLocalProject);
                });
    }


    public static void repair(BreakingUpdate breakingUpdate, DockerBuild dockerBuild, Path project) {

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

        JsonUtils.writeToFile(Path.of("result_repair.json"), resultsList);

    }


    private static void getProjectData(BreakingUpdate breakingUpdate, DockerBuild dockerBuild, Path fromContainer, Path toLocal) {


        try {
            String imageId = breakingUpdate.breakingUpdateReproductionCommand.replace("docker run ", "");

            String[] entrypoint = new String[]{"/bin/sh"};

            //pull image
            dockerBuild.ensureBaseMavenImageExists(imageId);

            CreateContainerResponse container = dockerBuild.startContainerEntryPoint(imageId, entrypoint);

            // Copy m2 folder to local path in the breaking commit folder
            dockerBuild.copyM2FolderToLocalPath(container.getId(), fromContainer, toLocal);
//            dockerBuild.copyProjectFromContainer(container.getId(), breakingUpdate.project, Path.of("%s/%s", CLIENT_PATH, breakingUpdate.breakingCommit));

        } catch (InterruptedException e) {
            log.error("Something went wrong", e);
            e.printStackTrace();
        }

    }


}
