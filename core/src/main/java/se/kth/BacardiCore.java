package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.java_version.RepairJavaVersionIncompatibility;
import se.kth.models.*;
import se.kth.wError.RepairWError;

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
    Result result;

    Boolean isBump = false;

    String actualImage;

    public BacardiCore(Path project, Path logFile, FailureCategoryExtract failureCategoryExtract, Boolean isBump) {
        this.project = Objects.requireNonNull(project, "Project path cannot be null");
        this.logFile = Objects.requireNonNull(logFile, "Log file path cannot be null");
        this.failureCategoryExtract = Objects.requireNonNull(failureCategoryExtract, "Failure category cannot be null");
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

        failureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

        // Result value for each attempt
        result = new Result(failureCategory);

        int attempts = 0;

        while (failureCategory != FailureCategory.BUILD_SUCCESS && attempts < 3) {

            // Check if the project is a git repository
            GitManager gitManager = new GitManager(project.toFile());
            // Check the status of the repository and create a new branch for the original status

            previousFailureCategory = failureCategory;

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
                    repairWErrorIncompatibility(gitManager);
                    break;
                case COMPILATION_FAILURE:
                    log.info("Compilation failure detected.");
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
            DockerBuild.deleteImage(actualImage);
            log.info("Build success in attempt: {}", attempts);
        }

        return result;
    }


    private FailureCategory repairJavaVersionIncompatibility(GitManager gitManager) {

        if (previousFailureCategory == failureCategory) {
            return previousFailureCategory;
        }

        //Create a branch for the java version incompatibility repair
        gitManager.newBranch(Constants.BRANCH_JAVA_VERSION_INCOMPATIBILITY);

        JavaVersionInformation javaVersionInformation = new JavaVersionInformation(logFile.toFile());
        JavaVersionInfo javaVersionInfo = javaVersionInformation.analyse(logFile.toAbsolutePath().toString(), project.toAbsolutePath().toString());

        JavaVersionIncompatibility incompatibility = javaVersionInfo.getIncompatibility();
        String newJavaVersion = javaVersionInfo.getIncompatibility().mapVersions(incompatibility.wrongVersion());


        log.info("");
        log.info("************************************************************");
        log.info("Starting Java version incompatibility repair.");
        log.info("*************************************************************");
        log.info("");

        RepairJavaVersionIncompatibility repairJavaVersionIncompatibility = new RepairJavaVersionIncompatibility(javaVersionInfo, project, isBump);

        actualImage = repairJavaVersionIncompatibility.repair();

        List<YamlInfo> javaVersions = javaVersionInfo.getJavaInWorkflowFiles();

        Path logFile = project.resolve("output_%s.log".formatted(result.getAttempts().size()));

        //check if the new failure category is success
        FailureCategory newFailureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

//        if (newFailureCategory.equals(failureCategoryExtract.)newFailureCategory == FailureCategory.BUILD_SUCCESS) {
        repairJavaVersionIncompatibility.updateJavaVersions(project.toString(), 17);
//        }

        gitManager.commitAllChanges("Java version incompatibility repair");

        return newFailureCategory;

    }

    private FailureCategory repairWErrorIncompatibility(GitManager gitManager) {
        //Create a branch for the java version incompatibility repair
        gitManager.newBranch(Constants.BRANCH_WERROR);


        log.info("");
        log.info("************************************************************");
        log.info("Starting Werror repair.");
        log.info("*************************************************************");
        log.info("");

        Path logFile = project.resolve("output.log");

        try {

            WerrorInformation werrorInformation = new WerrorInformation(logFile.toFile());

            WerrorInfo werrorInfo = werrorInformation.analyzeWerror(project.toString());

            RepairWError repairWError = new RepairWError();

            if (repairWError.isWerrorJavaVersionIncompatibilityError(logFile.toAbsolutePath().toString())) {
                //find all pom files with werror
                repairWError.replaceJavaVersion(project.resolve("pom.xml").toString(), 17);
            }

            log.info("Werror info: {}", werrorInfo);
        } catch (Exception e) {
            log.error("Error extracting warning lines.", e);
        }

        gitManager.commitAllChanges("Werror incompatibility repair");

        return failureCategoryExtract.getFailureCategory(logFile.toFile());

    }
}