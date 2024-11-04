package se.kth.extractor;

import japicmp.model.JApiClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.PipelineComponent;
import se.kth.japicmp.JapicmpAnalyzer;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.BreakingUpdate;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class CausingConstructExtractor extends PipelineComponent {

    private final TrueFailingTestCasesProvider failingTestCasesProvider;
    private final JapicmpAnalyzer japicmpAnalyzer;
    private final Path projectBaseDir;

    private int notFound = 0;
    private int total = 0;
    private int found = 0;
    private int noFileFound = 0;

    public CausingConstructExtractor(TrueFailingTestCasesProvider failingTestCasesProvider,
                                     JapicmpAnalyzer japicmpAnalyzer, Path projectBaseDir) {
        this.failingTestCasesProvider = failingTestCasesProvider;
        this.japicmpAnalyzer = japicmpAnalyzer;
        this.projectBaseDir = projectBaseDir;
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
            total++;
            if (this.foundAnyProjectTestFile(projectFiles)) {
                StackTraceElement testFile = getFirstTestFile(projectFiles);
                SpoonLocalizer spoonLocalizer = new SpoonLocalizer(projectBaseDir.resolve(commitId));
                if (spoonLocalizer.localize(testFile, testResult, breakingUpdate.updatedDependency, classes)) {
                    found++;
                } else {
                    notFound++;
                }
            } else {
                noFileFound++;
            }
            System.out.println("noFileFound: " + noFileFound);
            System.out.println("notFound: " + notFound);
            System.out.println("found: " + found);
            System.out.println("total: " + total);
            System.out.println("-----------------------");
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

    private StackTraceElement getFirstTestFile(List<ImmutablePair<StackTraceElement, Optional<File>>> projectTestFilePerStackTraceElement) {
        List<ImmutablePair<StackTraceElement, Optional<File>>> projectFiles =
                projectTestFilePerStackTraceElement.stream()
                        .filter(pair -> pair.right.isPresent())
                        .toList();
        return projectFiles.getLast()
                .left;
    }
}
