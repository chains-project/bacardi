package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.FailureCategory;

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

        System.out.println("Analyzing project: " + project.getFileName());
        System.out.println("Log file: " + logFile.getFileName());
        System.out.println("Failure category: " + failureCategory);


        log.info("Analyzing project: {}", project.getFileName());
        log.info("Log file: {}", logFile.getFileName());
        log.info("Failure category: {}", failureCategory);


    }
}
