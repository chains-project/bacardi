package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.java_version.RepairJavaVersionIncompatibility;
import se.kth.models.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static se.kth.Util.Constants.MAVEN_LOG_FILE;

public class BacardiCore {

    private static final Logger log = LoggerFactory.getLogger(BacardiCore.class);
    private final Path project;

    private final Path logFile;

    private final FailureCategoryExtract failureCategoryExtract;

    Boolean isBump = false;

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

        FailureCategory failureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

        Result result = new Result(failureCategory);


        int attempts = 0;

        while (failureCategory != FailureCategory.BUILD_SUCCESS && attempts < 3) {

            switch (failureCategory) {
                case JAVA_VERSION_FAILURE:
                    log.info("Java version failure detected.");
                    failureCategory = repairJavaVersionIncompatibility();
                    break;
                case TEST_FAILURE:
                    log.info("Test failure detected.");
                    break;
                case WERROR_FAILURE:
                    log.info("Werror failure detected.");
                    WerrorInformation werrorInformation = new WerrorInformation(logFile.toFile());
                    try {
                        WerrorInfo werrorInfo = werrorInformation.analyzeWerror(project.toString());
                        log.info("Werror info: {}", werrorInfo);
                    } catch (Exception e) {
                        log.error("Error extracting warning lines.", e);
                    }
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
            log.info("Build success in attempt: {}", attempts);
        }

        return result;
    }


    private FailureCategory repairJavaVersionIncompatibility() {


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

        repairJavaVersionIncompatibility.repair();

        File logFilePath = new File(MAVEN_LOG_FILE);


        List<YamlInfo> javaVersions = javaVersionInfo.getJavaInWorkflowFiles();


        //check if the new failure category is success
        FailureCategory newFailureCategory = failureCategoryExtract.getFailureCategory(logFilePath);

//        if (newFailureCategory.equals(failureCategoryExtract.)newFailureCategory == FailureCategory.BUILD_SUCCESS) {
        repairJavaVersionIncompatibility.updateJavaVersions(project.toString(), 17);
//        }

        return failureCategoryExtract.getFailureCategory(logFilePath);


    }
}
