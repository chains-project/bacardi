package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.MavenErrorLog;

import java.io.File;
import java.io.IOException;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        File logFile = new File("/Users/frank/Documents/Work/PHD/bacardi/projects/bd3ce213e2771c6ef7817c80818807a757d4e94a/OCR4all/bd3ce213e2771c6ef7817c80818807a757d4e94a.log");

        log.info("Analyzing log file: {}", logFile.getAbsolutePath());

        FailureCategoryExtract failureCategoryExtract = new FailureCategoryExtract(logFile);


        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(logFile);
        try {
            MavenErrorLog errorLog = mavenErrorInformation.extractLineNumbersWithPaths(String.valueOf(logFile));
            System.out.println(errorLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(failureCategoryExtract.getFailureCategory(logFile));
    }

}