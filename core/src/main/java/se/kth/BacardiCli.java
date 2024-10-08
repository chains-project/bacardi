package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import se.kth.models.FailureCategory;

import java.nio.file.Path;

public class BacardiCli {

    static Logger log = LoggerFactory.getLogger(BacardiCli.class);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BacardiEntryPoint()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Command(subcommands = {Bacardi.class, Classifier.class}, mixinStandardHelpOptions = true, version = "1.0")
    public static class BacardiEntryPoint implements Runnable {
        @Override
        public void run() {
            CommandLine.usage(this, System.out);
        }
    }

    @CommandLine.Command(name = "bacardi", mixinStandardHelpOptions = true, version = "0.1")
    private static class Bacardi implements Runnable {

        @CommandLine.Option(
                names = {"-c", "--project"},
                paramLabel = "Client project",
                description = "A client project to analyze.",
                required = true)
        Path project;


        @CommandLine.Option(
                names = {"-l", "--logFile"},
                paramLabel = "Log file",
                description = "A log file to analyze.",
                required = true)
        Path logFile;

        @Override
        public void run() {

            log.info("Analyzing project: {}", project.getFileName());

            FailureCategoryExtract failureCategoryExtract = new FailureCategoryExtract(logFile.toFile());
            final FailureCategory failureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

            if (failureCategory == FailureCategory.BUILD_SUCCESS) {
                log.info("Build success. No need to analyze further.");
                return;
            }

            BacardiCore bacardiCore = new BacardiCore(project, logFile, failureCategoryExtract);
            bacardiCore.analyze();

        }
    }


    @CommandLine.Command(name = "classifier", mixinStandardHelpOptions = true, version = "0.1")
    private static class Classifier implements Runnable {
        Logger log = LoggerFactory.getLogger(Classifier.class);

        @CommandLine.Option(
                names = {"-l", "--logFile"},
                paramLabel = "Log file",
                description = "A log file to analyze.",
                required = true)
        Path logFile;

        @Override
        public void run() {

            log.info("Analyzing log file: {}", logFile.getFileName());

            FailureCategoryExtract failureCategoryExtract = new FailureCategoryExtract(logFile.toFile());

            final FailureCategory failureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

            log.info("Failure category: {}", failureCategory);

        }
    }



}
