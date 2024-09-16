package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;

public class BacardiCli {

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

        @Override
        public void run() {
            System.out.println("Analyzing project: " + project);

        }
    }


    @CommandLine.Command(name = "classifier", mixinStandardHelpOptions = true, version = "0.1")
    private static class Classifier implements Runnable {
        Logger log = LoggerFactory.getLogger(Main.class);

        @CommandLine.Option(
                names = {"-l", "--logFile"},
                paramLabel = "Log file",
                description = "A log file to analyze.",
                required = true)
        Path logFile;

        @Override
        public void run() {
            log.info("Analyzing log file: {}", logFile.getFileName());

            MavenLogAnalyzer mavenLogAnalyzer = new MavenLogAnalyzer(logFile.toFile());

            final FailureCategory failureCategory = mavenLogAnalyzer.getFailureCategory();

            log.info("Failure category: {}", failureCategory);

        }
    }



}
