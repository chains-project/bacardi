package se.kth;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.Util.JsonUtils;
import se.kth.Util.LogUtils;
import se.kth.model.BreakingUpdate;
import se.kth.model.PromptPipeline;
import se.kth.model.SetupPipeline;
import se.kth.models.FailureCategory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static se.kth.Util.BreakingUpdateProvider.*;
import static se.kth.Util.Constants.*;

public class Bump {

    private static final Logger log = LoggerFactory.getLogger(Bump.class);
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final static String CLIENT_PATH = PROJECTS_PATH;

    static Map<String, Result> resultsMap = new ConcurrentHashMap<>();
    private final static String JSON_PATH = "result_repair_gpt_test.json";

    ForkJoinPool customThreadPool = new ForkJoinPool(availableProcessors);


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

        ForkJoinPool customThreadPool = new ForkJoinPool(4); //

        customThreadPool.submit(() ->
                breaking
                        .parallelStream()
                        .filter(e -> e.breakingCommit.equals("0abf7148300f40a1da0538ab060552bca4a2f1d8")) // filter by breaking commitAllChanges
//                        .filter(e -> !listOfJavaVersionIncompatibilities.contains(e.breakingCommit))
//                        .filter(e -> !resultsMap.containsKey(e.breakingCommit))// filter by failure category
                        .forEach(e -> {
                            try {
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
                                //adding output patch folder to the pipeline
                                setupPipeline.setOutPutPatchFolder(Path.of("results"));
                                //start repair process
                                repair(setupPipeline);
                            } catch (Exception ee) {
                                log.error("Error processing breaking update: %s".formatted(e.breakingCommit), ee);
                            }

                        })).join();
        customThreadPool.shutdown();
    }

    private static Path settingClientFolderAndM2Folder(BreakingUpdate e, DockerBuild dockerBuild) {
        //Setting same m2 folder for the new docker image
        final var clientFolder = Path.of("%s/%s".formatted(CLIENT_PATH, e.breakingCommit));
        if (Files.exists(clientFolder)) {
            log.info("Project already exists. Skipping download process...");
        } else {
            try {
                Files.createDirectory(clientFolder);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            // copy m2 folder to local path

            Path fromContainerM2 = Path.of("/root/.m2/");

            //root/.m2/repository/org/yaml/snakeyaml/2.1/snakeyaml-2.1.jar
            //create this route

            Path prevoiusJarInContainerPath = fromContainerM2.resolve("repository/%s/%s/%s/%s-%s.jar".formatted(
                    e.updatedDependency.dependencyGroupID.replace(".", "/"),
                    e.updatedDependency.dependencyArtifactID.replace(".", "/"),
                    e.updatedDependency.previousVersion,
                    e.updatedDependency.dependencyArtifactID,
                    e.updatedDependency.previousVersion));

            Path newJarInContainerPath = fromContainerM2.resolve("repository/%s/%s/%s/%s-%s.jar".formatted(
                    e.updatedDependency.dependencyGroupID.replace(".", "/"),
                    e.updatedDependency.dependencyArtifactID.replace(".", "/"),
                    e.updatedDependency.newVersion,
                    e.updatedDependency.dependencyArtifactID,
                    e.updatedDependency.newVersion));


            String preBreakingImage = e.preCommitReproductionCommand.replace("docker run ", "");
            Path fromContainerProject = Path.of(e.project);

            String breakingImage = e.breakingUpdateReproductionCommand.replace("docker run ", "");

            //get jar from container for previous version
            if (PIPELINE.equals(PromptPipeline.BASELINE_API_DIFF)) {
                getProjectData(preBreakingImage, dockerBuild, clientFolder, null, null, prevoiusJarInContainerPath);
            }
            //get jar from container for new version and m2 folder and project
            getProjectData(breakingImage, dockerBuild, clientFolder, fromContainerM2, fromContainerProject, newJarInContainerPath);


            //copy project from container
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
