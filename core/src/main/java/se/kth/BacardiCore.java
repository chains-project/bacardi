package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.FailureCategory;
import se.kth.models.WerrorInfo;

import java.nio.file.Path;

public class BacardiCore {

    private static final Logger log = LoggerFactory.getLogger(BacardiCore.class);
    private Path project;

    private Path logFile;

    private FailureCategory failureCategory;


    public BacardiCore(Path project, Path logFile, FailureCategory failureCategory) {
        this.project = project;
        this.logFile = logFile;
        this.failureCategory = failureCategory;
    }


    public void analyze() {

        switch (failureCategory) {
            case JAVA_VERSION_FAILURE:
                log.info("Java version failure detected.");
                break;
            case TEST_FAILURE:
                log.info("Test failure detected.");
                break;
            case WERROR_FAILURE:
                log.info("Werror failure detected.");
                WerrorInformation werrorInformation = new WerrorInformation(logFile.toFile());
                try {
                   WerrorInfo werrorInfo =  werrorInformation.analyzeWerror(project.toString());
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


    }
}
