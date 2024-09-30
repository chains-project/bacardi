package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.java_version.RepairJavaVersionIncompatibility;
import se.kth.models.FailureCategory;
import se.kth.models.JavaVersionInfo;
import se.kth.models.WerrorInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class BacardiCore {

    private static final Logger log = LoggerFactory.getLogger(BacardiCore.class);
    private Path project;

    private Path logFile;

    private FailureCategoryExtract failureCategoryExtract;


    public BacardiCore(Path project, Path logFile, FailureCategoryExtract failureCategoryExtract) {
        this.project = Objects.requireNonNull(project, "Project path cannot be null");
        this.logFile = Objects.requireNonNull(logFile, "Log file path cannot be null");
        this.failureCategoryExtract = Objects.requireNonNull(failureCategoryExtract, "Failure category cannot be null");
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


    public void analyze() {

        FailureCategory failureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

        int attempts = 1;

        while (failureCategory != FailureCategory.BUILD_SUCCESS && attempts < 2) {

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
                default:
                    log.info("Unknown failure category.");
            }
            attempts++;
        }
    }


    private FailureCategory repairJavaVersionIncompatibility() {
        JavaVersionInformation javaVersionInformation = new JavaVersionInformation(logFile.toFile());
        JavaVersionInfo javaVersionInfo = javaVersionInformation.analyse(logFile.toAbsolutePath().toString(), project.toAbsolutePath().toString());

        log.info("************************************************************");
        log.info("Starting Java version incompatibility repair.");
        log.info("*************************************************************");

        RepairJavaVersionIncompatibility repairJavaVersionIncompatibility = new RepairJavaVersionIncompatibility(javaVersionInfo, project);
        repairJavaVersionIncompatibility.repair();

        File logFilePath = new File("maven.log");
        //get new failure category

        javaVersionInfo.getJavaInWorkflowFiles().forEach((k, v) -> {
            log.info("Java version in workflow file: {}", k);
            log.info("Java version in workflow file: {}", v);
        });

        return failureCategoryExtract.getFailureCategory(logFilePath);


    }
}
