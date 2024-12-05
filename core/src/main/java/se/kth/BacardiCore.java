package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.LogUtils;
import se.kth.direct_failures.RepairDirectFailures;
import se.kth.java_version.RepairJavaVersionIncompatibility;
import se.kth.model.DependencyTree;
import se.kth.model.SetupPipeline;
import se.kth.models.*;
import se.kth.wError.RepairWError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class BacardiCore {

    private static final Logger log = LoggerFactory.getLogger(BacardiCore.class);
    private final Path project;
    private final Path logFile;

    private final FailureCategoryExtract failureCategoryExtract;
    private FailureCategory previousFailureCategory;
    private FailureCategory failureCategory;
    private SetupPipeline setupPipeline;

    private Result result;

    Boolean isBump = false;
    String actualImage;

    public BacardiCore(Path project, Path logFile, FailureCategoryExtract failureCategoryExtract, Boolean isBump) {
        this.project = Objects.requireNonNull(project, "Project path cannot be null");
        this.logFile = Objects.requireNonNull(logFile, "Log file path cannot be null");
        this.failureCategoryExtract = Objects.requireNonNull(failureCategoryExtract, "Failure category cannot be null");
        this.isBump = isBump;
        verify();
    }

    public BacardiCore(SetupPipeline setupPipeline, FailureCategoryExtract failureCategoryExtract, Boolean isBump) {
        this.project = Objects.requireNonNull(setupPipeline.getClientFolder(), "Project path cannot be null");
        this.logFile = Objects.requireNonNull(setupPipeline.getLogFilePath(), "Log file path cannot be null");
        this.failureCategoryExtract = Objects.requireNonNull(failureCategoryExtract, "Failure category cannot be null");
        this.setupPipeline = Objects.requireNonNull(setupPipeline, "Check setup pipeline");
        this.isBump = isBump;
        verify();
    }


    public void verify() {
        if (!Files.exists(project)) {
            throw new IllegalArgumentException("Project path does not exist.");
        }
        if (!Files.exists(logFile)) {
            throw new IllegalArgumentException("Log file path does not exist.");
        }
    }


    public Result analyze() {

        failureCategory = failureCategoryExtract.getFailureCategory(setupPipeline.getLogFilePath().toFile());
        // Result value for each attempt
        result = new Result(failureCategory);

        int attempts = 0;


        while (failureCategory != FailureCategory.BUILD_SUCCESS && attempts < 3) {


            // Check if the project is a git repository
            GitManager gitManager = new GitManager(project.toFile());
            // Check the status of the repository and create a new branch for the original status

            gitManager.checkRepoStatus();

            switch (failureCategory) {
                case JAVA_VERSION_FAILURE:
                    log.info("Java version failure detected.");
                    failureCategory = repairJavaVersionIncompatibility(gitManager);
                    break;
                case TEST_FAILURE:
                    log.info("Test failure detected.");
                    break;
                case WERROR_FAILURE:
                    log.info("Werror failure detected.");
                    failureCategory = repairWErrorIncompatibility(gitManager);
                    break;
                case COMPILATION_FAILURE:
                    log.info("Compilation failure detected.");
                    failureCategory = repairDirectCompilationFailure(gitManager);
                    break;
                case DEPENDENCY_LOCK_FAILURE:
                    log.info("Dependency lock failure detected.");
                    break;
                case DEPENDENCY_RESOLUTION_FAILURE:
                    log.info("Dependency resolution failure detected.");
                    break;
                case ENFORCER_FAILURE:
                    log.info("Enforcer failure detected.");
                    break;

                default:
                    log.info("Unknown failure category.");
            }

            Attempt attempt = new Attempt(attempts, failureCategory, failureCategory == FailureCategory.BUILD_SUCCESS);
            log.info("Attempt: {}", attempt);
            result.getAttempts().add(attempt);
            // number of attempts to repair the failure
            attempts++;
        }

        if (failureCategory == FailureCategory.BUILD_SUCCESS) {
//            DockerBuild.deleteImage(actualImage);
            log.info("Build success in attempt: {}", attempts);
        }

        return result;
    }


    /**
     * Repairs direct compilation failures.
     *
     * @param gitManager the Git manager to handle repository operations
     * @return the new failure category after attempting the repair
     */
    private FailureCategory repairDirectCompilationFailure(GitManager gitManager) {
        // checking if the previous failure category is different from the current failure category and create a new branch
        if (previousFailureCategory != failureCategory) {
            previousFailureCategory = failureCategory;
            gitManager.newBranch(Constants.BRANCH_DIRECT_COMPILATION_FAILURE);
        }
        DockerBuild dockerBuild = setupPipeline.getDockerBuild();

        // Ensure the base Maven image exists
        try {
            dockerBuild.ensureBaseMavenImageExists(setupPipeline.getDockerImage());
        } catch (InterruptedException e) {
            log.error("Error ensuring base maven image exists.", e);
            throw new RuntimeException(e);
        }

        Path project = setupPipeline.getClientFolder();
        Path logFile = setupPipeline.getLogFilePath();

        //check if exist any dependency conflict
        RepairDirectFailures repairDirectFailures = new RepairDirectFailures(setupPipeline.getDockerBuild(),
                setupPipeline);

        // Check and try dependency resolution conflicts
        Path treeFile = project.resolve("tree.json");
        Path generatedTreeFile = repairDirectFailures.generateDependencyTree(treeFile, setupPipeline.getDockerImage(), setupPipeline.getClientFolder().getFileName().toString());
        // Check if there are any conflicts
        List<DependencyTree> dependencyTreeList = repairDirectFailures.identifyConflicts(generatedTreeFile);
        // If there are conflicts, try to resolve them
        for (DependencyTree dependencyTree : dependencyTreeList) {
            try {
                RepairDirectFailures.modifyPomFile(project.resolve("pom.xml"), dependencyTree);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (dependencyTreeList.isEmpty()) {
            log.info("No conflicts found");
        } else {
            log.info("Conflicts found");
            String dockerImage = repairDirectFailures.reproduce();
        }


        FailureCategory category = failureCategoryExtract.getFailureCategory(setupPipeline.getLogFilePath().toFile());
        if (category == FailureCategory.BUILD_SUCCESS) {
            //repair process failed
            gitManager.commitAllChanges("Direct compilation failure repair");
            //roll back to the original branch
        }

        return category;
    }


    private FailureCategory repairJavaVersionIncompatibility(GitManager gitManager) {

        //Create a branch for the java version incompatibility repair
        gitManager.newBranch(Constants.BRANCH_JAVA_VERSION_INCOMPATIBILITY);

        JavaVersionInformation javaVersionInformation = new JavaVersionInformation(setupPipeline.getLogFilePath().toFile());
        JavaVersionInfo javaVersionInfo = javaVersionInformation.analyse(setupPipeline.getLogFilePath().toString(), project.toAbsolutePath().toString());

        JavaVersionIncompatibility incompatibility = javaVersionInfo.getIncompatibility();
        String newJavaVersion = javaVersionInfo.getIncompatibility().mapVersions(incompatibility.wrongVersion());


        LogUtils.logWithBox(log, "Starting Java version incompatibility repair.");

        RepairJavaVersionIncompatibility repairJavaVersionIncompatibility = new RepairJavaVersionIncompatibility(javaVersionInfo, project, isBump);

        actualImage = repairJavaVersionIncompatibility.repair(setupPipeline);
        setupPipeline.setDockerImage(actualImage);

        List<YamlInfo> javaVersions = javaVersionInfo.getJavaInWorkflowFiles();

        Path logFile = project.resolve("output.log".formatted(result.getAttempts().size()));

        //check if the new failure category is success
        FailureCategory newFailureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

//        if (newFailureCategory.equals(failureCategoryExtract.)newFailureCategory == FailureCategory.BUILD_SUCCESS) {
        repairJavaVersionIncompatibility.updateJavaVersions(project.toString(), 17);
//        }

        gitManager.commitAllChanges("Java version incompatibility repair");


        return newFailureCategory;

    }

    private FailureCategory repairWErrorIncompatibility(GitManager gitManager) {
        //Create a branch for the werror repair
        if (previousFailureCategory != failureCategory) {
            gitManager.newBranch(Constants.BRANCH_WERROR);
        }

        //get Docker image in case of bump
        /*
        modify the version to get the docker image from bump and not from the project
        */

        try {
            setupPipeline.getDockerBuild().ensureBaseMavenImageExists(setupPipeline.getDockerImage());
        } catch (InterruptedException e) {
            log.error("Error ensuring base maven image exists.", e);
            throw new RuntimeException(e);
        }

        LogUtils.logWithBox(log, "Starting Werror incompatibility repair.");

        Path logFile = setupPipeline.getLogFilePath();

        try {

            WerrorInformation werrorInformation = new WerrorInformation(setupPipeline.getLogFilePath().toFile());

            WerrorInfo werrorInfo = werrorInformation.analyzeWerror(setupPipeline.getClientFolder().toString());

            RepairWError repairWError = new RepairWError(project, isBump, setupPipeline.getDockerImage(), setupPipeline);

            if (repairWError.isWerrorJavaVersionIncompatibilityError(logFile.toAbsolutePath().toString())) {
                //find all pom files with werror
                repairWError.replaceJavaVersion(project.resolve("pom.xml").toString(), 17);
            }

            repairWError.reproduce();
            // update the setup pipeline with the new log file and all information
            setupPipeline = repairWError.getSetupPipeline();

            log.info("Werror info: {}", werrorInfo);

        } catch (Exception e) {
            log.error("Error extracting warning lines.", e);
        }

        gitManager.commitAllChanges("Werror incompatibility repair");
        return failureCategoryExtract.getFailureCategory(logFile.toFile());

    }


}