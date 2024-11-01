package se.kth;

import se.kth.analysis.TestResultAnalyzer;
import se.kth.extractor.CausingConstructExtractor;
import se.kth.extractor.TrueFailingTestCasesProvider;
import se.kth.injector.TestWithListenerRunner;
import se.kth.japicmp.JapicmpAnalyzer;
import se.kth.japicmp.UpdatedDependencyExtractor;
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

        Path preM2OutputDirectory = Config.relativeToTmpDir("pre-m2");
        Path breakingM2OutputDirectory = Config.relativeToTmpDir("breaking-m2");
        UpdatedDependencyExtractor updatedDependencyExtractor = new UpdatedDependencyExtractor(preM2OutputDirectory,
                breakingM2OutputDirectory);


        Path unsuccessfulTestCasesFile = Config.getResourcesDir().resolve("unsuccessfulTestCases.json");
        TrueFailingTestCasesProvider testCasesProvider = new TrueFailingTestCasesProvider(testListenerBreakingOutput,
                unsuccessfulTestCasesFile);
        JapicmpAnalyzer japicmpAnalyzer = new JapicmpAnalyzer(preM2OutputDirectory, breakingM2OutputDirectory);
        CausingConstructExtractor causingConstructExtractor = new CausingConstructExtractor(testCasesProvider,
                japicmpAnalyzer);

        Pipeline pipeline = new Pipeline()
                .with(testPreRunner)
                .with(testResultAnalyzer)
                .with(testBreakingRunner)
                .with(updatedDependencyExtractor)
                .with(causingConstructExtractor);

        pipeline.run();
    }
}