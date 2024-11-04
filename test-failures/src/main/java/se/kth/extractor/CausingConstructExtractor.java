package se.kth.extractor;

import japicmp.model.JApiClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.PipelineComponent;
import se.kth.japicmp.JapicmpAnalyzer;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.BreakingUpdate;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

public class CausingConstructExtractor extends PipelineComponent {

    private final TrueFailingTestCasesProvider failingTestCasesProvider;
    private final JapicmpAnalyzer japicmpAnalyzer;
    private final Path projectBaseDir;

    private int notFound = 0;
    private int total = 0;
    private int found = 0;
    private int noFileFound = 0;
    private int failed = 0;

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
        List<String> classNamesJapicmp = classes.stream()
                .map(JApiClass::getFullyQualifiedName)
                .toList();

        TestFileLoader testFileLoader = new TestFileLoader(commitId);
        for (TestResult testResult : failingTestCases) {

            try {
                List<ImmutablePair<StackTraceElement, Optional<File>>> projectFiles =
                        testFileLoader.loadProjectTestFiles(testResult.throwable.stackTrace);
                total++;
                if (this.foundAnyProjectTestFile(projectFiles)) {
                    List<StackTraceElement> locatedElements = this.getLocatedStackTraceElements(projectFiles);
                    SpoonLocalizer spoonLocalizer = new SpoonLocalizer(projectBaseDir.resolve(commitId));
                    Set<CtElement> involvedElements = spoonLocalizer.localize(locatedElements);
                    if (!involvedElements.isEmpty()) {
                        var types = involvedElements.stream()
                                .map(CtElement::getReferencedTypes)
                                .toList();
                        var result = this.checkChangedInvolved(types, classNamesJapicmp);
                        if (!result.isEmpty()) {
                            found++;
                        } else {
                            notFound++;
                        }
                    } else {
                        notFound++;
                    }
                } else {
                    noFileFound++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                failed++;
            }
            System.out.println("noFileFound: " + noFileFound);
            System.out.println("notFound: " + notFound);
            System.out.println("found: " + found);
            System.out.println("failed: " + failed);
            System.out.println("total: " + total);
            System.out.println("-----------------------");
        }
    }

    @Override
    public void finish() {
        System.out.println("");
    }

    private List<Set<String>> checkChangedInvolved(List<Set<CtTypeReference<?>>> references, List<String> changedClassnames) {
        var tmp = references.stream()
                .map(ctTypeReferences -> ctTypeReferences.stream()
                        .map(ctTypeReference -> ctTypeReference.getQualifiedName())
                        .collect(Collectors.toSet()))
                .toList();
        var involved = tmp.stream()
                .filter(strings -> containsAny(strings, changedClassnames))
                .toList();
        return involved;
    }

    private boolean containsAny(Set<String> first, List<String> second) {
        for (String s : second) {
            if (first.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean foundAnyProjectTestFile(List<ImmutablePair<StackTraceElement, Optional<File>>> projectTestFilePerStackTraceElement) {
        return projectTestFilePerStackTraceElement.stream()
                .anyMatch(pair -> pair.right.isPresent());
    }

    private List<StackTraceElement> getLocatedStackTraceElements(List<ImmutablePair<StackTraceElement, Optional<File>>> projectTestFilePerStackTraceElement) {
        return projectTestFilePerStackTraceElement.stream()
                .filter(pair -> pair.right.isPresent())
                .map(ImmutablePair::getLeft)
                .toList();
    }
}
