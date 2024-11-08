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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CausingConstructExtractor extends PipelineComponent {

    private final TrueFailingTestCasesProvider failingTestCasesProvider;
    private final JapicmpAnalyzer japicmpAnalyzer;
    private final Path projectBaseDir;

    private final ConcurrentLinkedQueue<CtElement> result = new ConcurrentLinkedQueue();

    private AtomicInteger notFound = new AtomicInteger(0);
    private AtomicInteger total = new AtomicInteger(0);
    private AtomicInteger found = new AtomicInteger(0);
    private AtomicInteger noFileFound = new AtomicInteger(0);
    private AtomicInteger foundExact = new AtomicInteger(0);
    private AtomicInteger noChangeInvolved = new AtomicInteger(0);
    private AtomicInteger failed = new AtomicInteger(0);

    private final List<CtElement> exactMatches = new LinkedList<>();

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

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            for (TestResult testResult : failingTestCases) {
                executorService.submit(() -> {
                    analyzeTestResult(testResult, testFileLoader, commitId, classNamesJapicmp);
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void analyzeTestResult(TestResult testResult, TestFileLoader testFileLoader, String commitId, List<String> classNamesJapicmp) {
        try {
            List<ImmutablePair<StackTraceElement, Optional<File>>> projectFiles =
                    testFileLoader.loadProjectTestFiles(testResult.throwable.stackTrace);
            if (!testResult.throwable.className.contains("ssertion") && !testResult.throwable.className.contains("ssumption")) {
                List<String> files = projectFiles.stream()
                        .filter(pair -> pair.right.isPresent())
                        .map(pair -> pair.right.get())
                        .map(e -> {
                            try {
                                return Files.readString(e.toPath());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                        .toList();
                total.getAndIncrement();
                if (this.foundAnyProjectTestFile(projectFiles)) {
                    List<StackTraceElement> locatedElements = this.getLocatedStackTraceElements(projectFiles);
                    SpoonLocalizer spoonLocalizer = new SpoonLocalizer(projectBaseDir.resolve(commitId));
                    var raw = spoonLocalizer.localize(locatedElements).stream().toList();
                    List<CtElement> involvedElements = this.removeParents(raw);
                    if (!involvedElements.isEmpty()) {
                        var result = this.checkChangedInvolved(involvedElements, classNamesJapicmp);
                        if (!result.isEmpty()) {
                            if (result.size() == 1) {
                                foundExact.getAndIncrement();
                                this.exactMatches.add(result.getFirst());
                            } else {
                                System.out.println(result.size());
                            }
                            found.getAndIncrement();
                        } else {
                            noChangeInvolved.getAndIncrement();
                        }
                    } else {
                        notFound.getAndIncrement();
                    }
                } else {
                    noFileFound.getAndIncrement();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            failed.getAndIncrement();
        }

        System.out.println("noFileFound: " + noFileFound);
        System.out.println("notFound: " + notFound);
        System.out.println("noChangeInvolved: " + noChangeInvolved);
        System.out.println("found: " + found);
        System.out.println("foundExact: " + foundExact);
        System.out.println("failed: " + failed);
        System.out.println("total: " + total);
        System.out.println("-----------------------");
    }

    @Override
    public void finish() {
        System.out.println("");
    }

    private List<CtElement> checkChangedInvolved(List<CtElement> elements, List<String> changedClassnames) {
        return removeParents(elements).stream()
                .filter(ctElement -> referencesChangedType(ctElement, changedClassnames))
                .toList();
    }

    private boolean referencesChangedType(CtElement element, List<String> changedClassnames) {
        List<String> referencedElementsTypes = element.getReferencedTypes().stream()
                .map(CtTypeReference::getQualifiedName)
                .toList();
        return containsAny(changedClassnames, referencedElementsTypes);
    }

    private boolean containsAny(List<String> first, List<String> second) {
        for (String s : second) {
            if (first.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private List<CtElement> removeParents(List<CtElement> elements) {
        List<CtElement> parents = elements.stream()
                .filter(ctElement -> ctElement.getParent() != null)
                .map(CtElement::getParent)
                .toList();
        return elements.stream()
                .filter(ctElement -> !parents.contains(ctElement))
                .toList();
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
