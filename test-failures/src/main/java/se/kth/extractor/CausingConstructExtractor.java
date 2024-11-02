package se.kth.extractor;

import japicmp.model.JApiClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.PipelineComponent;
import se.kth.japicmp.JapicmpAnalyzer;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.BreakingUpdate;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class CausingConstructExtractor extends PipelineComponent {

    private final TrueFailingTestCasesProvider failingTestCasesProvider;
    private final JapicmpAnalyzer japicmpAnalyzer;

    public CausingConstructExtractor(TrueFailingTestCasesProvider failingTestCasesProvider,
                                     JapicmpAnalyzer japicmpAnalyzer) {
        this.failingTestCasesProvider = failingTestCasesProvider;
        this.japicmpAnalyzer = japicmpAnalyzer;
    }

    @Override
    public void execute(BreakingUpdate breakingUpdate) {
        String commitId = breakingUpdate.breakingCommit;
        List<TestResult> failingTestCases = failingTestCasesProvider.getTrueFailingTestCases(commitId);

        List<JApiClass> classes = this.japicmpAnalyzer.getChanges(breakingUpdate);


        TestFileLoader testFileLoader = new TestFileLoader(commitId);
        for (TestResult testResult : failingTestCases) {
            List<ImmutablePair<StackTraceElement, Optional<File>>> projectFiles =
                    testFileLoader.loadProjectTestFiles(testResult.throwable.stackTrace);
            if (this.foundAnyProjectTestFile(projectFiles)) {
                File testFile = getFirstTestFile(projectFiles);
                SpoonLocalizer spoonLocalizer = new SpoonLocalizer(testFile);
                spoonLocalizer.localize(projectFiles, testResult, breakingUpdate.updatedDependency, classes);
            }
        }
    }

    @Override
    public void finish() {
        System.out.println("");
    }

    private boolean foundAnyProjectTestFile(List<ImmutablePair<StackTraceElement, Optional<File>>> projectTestFilePerStackTraceElement) {
        return projectTestFilePerStackTraceElement.stream()
                .anyMatch(pair -> pair.right.isPresent());
    }

    private File getFirstTestFile(List<ImmutablePair<StackTraceElement, Optional<File>>> projectTestFilePerStackTraceElement) {
        return projectTestFilePerStackTraceElement.stream()
                .filter(pair -> pair.right.isPresent())
                .findFirst()
                .get()
                .right
                .get();
    }
}
