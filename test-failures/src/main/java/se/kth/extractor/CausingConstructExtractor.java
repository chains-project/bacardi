package se.kth.extractor;

import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.PipelineComponent;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.BreakingUpdate;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class CausingConstructExtractor extends PipelineComponent {

    private final TrueFailingTestCasesProvider failingTestCasesProvider;

    public CausingConstructExtractor(TrueFailingTestCasesProvider failingTestCasesProvider) {
        this.failingTestCasesProvider = failingTestCasesProvider;
    }

    @Override
    public void execute(BreakingUpdate breakingUpdate) {
        String commitId = breakingUpdate.breakingCommit;
        List<TestResult> failingTestCases = failingTestCasesProvider.getTrueFailingTestCases(commitId);

        TestFileLoader testFileLoader = new TestFileLoader(commitId);
        for (TestResult testResult : failingTestCases) {
            List<ImmutablePair<StackTraceElement, Optional<File>>> projectFiles =
                    testFileLoader.loadProjectTestFiles(testResult.throwable.stackTrace);
            if (this.foundAnyProjectTestFile(projectFiles)) {
                SpoonLocalizer.localize(projectFiles, testResult, breakingUpdate.updatedDependency);
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
}
