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
import java.util.Arrays;
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
    private final static String JSON_PATH = OUTPUT_PATH + String.format("/result_repair_%s.json", LLM);

    public static void main(String[] args) {

        if (RESTART) {

            deleteDirectoryContent(Path.of(PROJECTS_PATH));
            deleteDirectoryContent(Path.of(OUTPUT_PATH));

        }
        LogUtils.logWithBox(log, "Bump analysis started. PIPELINE: %s MODEL: %s".formatted(PIPELINE, LLM));

        // filtering breaking dependency updates
        log.info("Filtering breaking dependency updates");
        log.info("");
        // Filter by java version incompatibility
        ArrayList<String> listOfJavaVersionIncompatibilities = readJavaVersionIncompatibilities(
                JAVA_VERSION_INCOMPATIBILITY_FILE);

        // List of breaking updates
        // List<BreakingUpdate> breaking =
        // BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH,
        // FailureCategory.COMPILATION_FAILURE, listOfJavaVersionIncompatibilities);
        List<BreakingUpdate> breaking = BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH,
                FailureCategory.COMPILATION_FAILURE);

        // read Json file with attempts information
        resultsMap.putAll(getPreviousResults(JSON_PATH));

        DockerBuild dockerBuild = new DockerBuild(true, MAX_ATTEMPTS);

        // Make sure project folder exits
        if (!Files.exists(Path.of(PROJECTS_PATH))) {
            try {
                Files.createDirectories(Path.of(PROJECTS_PATH));
            } catch (IOException e) {
                log.error("Error creating project folder", e);
                e.printStackTrace();
            }
        }

        // identify breaking updates and download image and copy the project
        ForkJoinPool customThreadPool = new ForkJoinPool(4); //

        if (!SPECIFIC_FILE.isEmpty()) {
            customThreadPool.submit(() -> breaking
                    .parallelStream()
                    .filter(e -> e.breakingCommit.equals(SPECIFIC_FILE)) // filter by breaking
                    // commitAllChanges
                    .filter(e -> !listOfJavaVersionIncompatibilities.contains(e.breakingCommit))
//                    .filter(e -> !resultsMap.containsKey(e.breakingCommit))// filter by failure
                    // category
                    .forEach(e -> {
                        threadrun(dockerBuild, e);
                    })).join();
            customThreadPool.shutdown();
            customThreadPool.close();

        } else {
            customThreadPool.submit(() -> breaking
                    .parallelStream()
                    // commitAllChanges
                    .filter(e -> !listOfJavaVersionIncompatibilities.contains(e.breakingCommit))
                    .filter(e -> !resultsMap.containsKey(e.breakingCommit))// filter by failure
                    // category
                    .forEach(e -> {
                        threadrun(dockerBuild, e);
                    })).join();
            customThreadPool.shutdown();
            customThreadPool.close();
        }
    }

    private static void deleteDirectoryContent(Path of) {
        try {
            Files.walk(of)
                    .sorted((path1, path2) -> path2.compareTo(path1)) // sort in reverse order to delete files before
                    // directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Failed to delete " + path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error deleting directory content", e);
        }
    }

    private static void threadrun(DockerBuild dockerBuild, BreakingUpdate e) {

        try {
            // starting processing breaking update
            LogUtils.logWithBox(log, "Processing breaking update: %s".formatted(e.breakingCommit));
            // Full path Folder/breaking-commit/project
            Path clientFolder = settingClientFolderAndM2Folder(e, dockerBuild);

            SetupPipeline setupPipeline = new SetupPipeline();
            // adding breaking update to the pipeline
            setupPipeline.setBreakingUpdate(e);
            // adding docker build to the pipeline
            setupPipeline.setDockerBuild(dockerBuild);
            // adding client folder to the pipeline
            setupPipeline.setClientFolder(clientFolder.resolve(e.project));
            // adding log file path to the pipeline
            setupPipeline.setLogFilePath(
                    Path.of("%s/%s.log".formatted(clientFolder.resolve(e.project), e.breakingCommit)));
            // adding m2 folder path to the pipeline
            setupPipeline.setM2FolderPath(Path.of("%s/%s/m2/".formatted(CLIENT_PATH, e.breakingCommit)));
            // adding docker image to the pipeline
            setupPipeline.setDockerImage(e.breakingUpdateReproductionCommand.replace("docker run ", ""));
            // adding output patch folder to the pipeline
            setupPipeline.setOutPutPatchFolder(Path.of(OUTPUT_PATH));
            setupPipeline.setLibraryName(e.updatedDependency.dependencyGroupID);
            setupPipeline.setBaseVersion(e.updatedDependency.previousVersion);
            setupPipeline.setNewVersion(e.updatedDependency.newVersion);
            // start repair process
            repair(setupPipeline);
        } catch (Exception ee) {
            log.error("Error processing breaking update: %s".formatted(e.breakingCommit), ee);
        }
    }

    private static Path settingClientFolderAndM2Folder(BreakingUpdate e, DockerBuild dockerBuild) {
        // Setting same m2 folder for the new docker image
        final var clientFolder = Path.of("%s/%s".formatted(CLIENT_PATH, e.breakingCommit));
        try {
            if (Files.exists(clientFolder) && Files.list(clientFolder).findAny().isPresent()) {
                log.info("Project already exists and is not empty. Skipping download process...");
            } else {
                try {
                    if (!Files.exists(clientFolder))
                        Files.createDirectory(clientFolder);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                // copy m2 folder to local path

                Path fromContainerM2 = Path.of("/root/.m2/");

                // root/.m2/repository/org/yaml/snakeyaml/2.1/snakeyaml-2.1.jar
                // create this route

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

                // get jar from container for previous version
                PromptPipeline[] pipeline = {PromptPipeline.BASELINE_API_DIFF};
                if (Arrays.asList(pipeline).contains(PIPELINE)) {
                    getProjectData(preBreakingImage, dockerBuild, clientFolder, null, null, prevoiusJarInContainerPath);
                }
                // get jar from container for new version and m2 folder and project
                getProjectData(breakingImage, dockerBuild, clientFolder, fromContainerM2, fromContainerProject,
                        newJarInContainerPath);

                // copy project from container
                // delete Image
                // if (imageId != null) {
                // dockerBuild.deleteImage(imageId);
                // }
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
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
         * Read the log file and extract the failure category
         * If the failure category is BUILD_SUCCESS, no need to analyze further
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

        // delete image
//        deleteDockerImage(setupPipeline);

    }

    public static void deleteDockerImage(SetupPipeline setupPipeline) {

        DockerBuild.deleteImage(
                setupPipeline.getBreakingUpdate().breakingUpdateReproductionCommand.replace("docker run ", ""));
        // TODO: isn't this image created in specific pipeline, shouldn't we add an
        // if-statement
        PromptPipeline[] pipeline = {PromptPipeline.BASELINE_API_DIFF};
        if (Arrays.asList(pipeline).contains(PIPELINE)) {
            DockerBuild
                    .deleteImage(setupPipeline.getBreakingUpdate().preCommitReproductionCommand.replace("docker run ", ""));
        }
    }

}
