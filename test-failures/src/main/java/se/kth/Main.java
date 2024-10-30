package se.kth;

import se.kth.analysis.TestResultAnalyzer;
import se.kth.injector.TestWithListenerRunner;
import se.kth.utils.Config;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        Path testListenerTestOutput = Config.relativeToTmpDir("pre-output");
        Path testListenerContainerOutput = Config.relativeToTmpDir("pre-container");
        Path pomsDirectory = Config.relativeToTmpDir("pre-poms");
        TestWithListenerRunner testPreRunner = new TestWithListenerRunner(testListenerTestOutput,
                testListenerContainerOutput, pomsDirectory, false);

        TestResultAnalyzer testResultAnalyzer = new TestResultAnalyzer(testListenerTestOutput);

        Path testListenerBreakingOutput = Config.relativeToTmpDir("breaking-output");
        Path testListenerBreakingContainer = Config.relativeToTmpDir("breaking-container");
        Path breakingPomsDirectory = Config.relativeToTmpDir("breaking-poms");
        TestWithListenerRunner testBreakingRunner = new TestWithListenerRunner(testListenerBreakingOutput,
                testListenerBreakingContainer, breakingPomsDirectory, true);

        Pipeline pipeline = new Pipeline()
                .with(testPreRunner)
                .with(testResultAnalyzer)
                .with(testBreakingRunner);

        pipeline.run();
    }
}