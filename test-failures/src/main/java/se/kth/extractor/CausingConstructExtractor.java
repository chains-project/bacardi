package se.kth.extractor;

import japicmp.model.JApiClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.PipelineComponent;
import se.kth.Util.JsonUtils;
import se.kth.japicmp.JapicmpAnalyzer;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtFieldImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class CausingConstructExtractor extends PipelineComponent {

    private static Logger log = LoggerFactory.getLogger(CausingConstructExtractor.class);

    private final TrueFailingTestCasesProvider failingTestCasesProvider;
    private final JapicmpAnalyzer japicmpAnalyzer;
    private final Path projectBaseDir;

    private AtomicInteger notFound = new AtomicInteger(0);
    private AtomicInteger total = new AtomicInteger(0);
    private AtomicInteger found = new AtomicInteger(0);
    private AtomicInteger noFileFound = new AtomicInteger(0);
    private AtomicInteger foundExact = new AtomicInteger(0);
    private AtomicInteger noChangeInvolved = new AtomicInteger(0);
    private AtomicInteger failed = new AtomicInteger(0);

    private final ConcurrentHashMap<String, ConcurrentHashMap> result = new ConcurrentHashMap<>();

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
        this.result.put(commitId, new ConcurrentHashMap<String, List<CausingConstruct>>());

        List<JApiClass> classes = this.japicmpAnalyzer.getChanges(breakingUpdate);
        List<String> classNamesJapicmp = classes.stream()
                .map(JApiClass::getFullyQualifiedName)
                .toList();

        TestFileLoader testFileLoader = new TestFileLoader(commitId);
        SpoonLocalizer spoonLocalizer = new SpoonLocalizer(projectBaseDir.resolve(commitId));

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            for (TestResult testResult : failingTestCases) {
                executorService.submit(() -> {
                    analyzeTestResult(testResult, testFileLoader, commitId, classNamesJapicmp, spoonLocalizer);
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void analyzeTestResult(TestResult testResult, TestFileLoader testFileLoader, String commitId,
                                   List<String> classNamesJapicmp, SpoonLocalizer spoonLocalizer) {
        try {
            List<ImmutablePair<StackTraceElement, Optional<File>>> projectFiles =
                    testFileLoader.loadProjectTestFiles(testResult.throwable.stackTrace);
            if (!isTestFailure(testResult)) {
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
                    var raw = spoonLocalizer.localize(locatedElements).stream().toList();
                    List<CtElement> involvedElements = this.checkChangedInvolved(raw, classNamesJapicmp);
                    List<CtElement> result = this.removeParents(involvedElements);
                    if (!involvedElements.isEmpty()) {
                        if (!result.isEmpty()) {
                            this.result.get(commitId).put(testResult.testIdentifier, getAsCausingConstructs(result));
                            if (result.size() == 1) {
                                foundExact.getAndIncrement();
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

    private List<CausingConstruct> getAsCausingConstructs(List<CtElement> elements) {
        return elements.stream()
                .map(ctElement -> {
                    if (ctElement instanceof CtExecutableReference<?>) {
                        if (((CtExecutableReference<?>) ctElement).isConstructor()) {
                            return CausingConstruct.CONSTRUCTOR;
                        } else {
                            return CausingConstruct.METHOD;
                        }
                    }
                    if (ctElement instanceof CtFieldRead<?>) {
                        return CausingConstruct.FIELD_REFERENCE;
                    }
                    if (ctElement instanceof CtTypeReference<?>) {
                        return CausingConstruct.TYPE_REFERENCE;
                    }
                    if (ctElement instanceof CtVariableRead<?>) {
                        return CausingConstruct.VARIABLE_REFERENCE;
                    }
                    if (ctElement instanceof CtFieldReference<?>) {
                        return CausingConstruct.FIELD_REFERENCE;
                    }
                    if (ctElement instanceof CtFieldImpl<?>) {
                        return CausingConstruct.TYPE_REFERENCE;
                    }
                    if (ctElement instanceof CtInvocation<?>) {
                        if (((CtInvocation<?>) ctElement).getExecutable().isConstructor()) {
                            return CausingConstruct.CONSTRUCTOR;
                        } else {
                            return CausingConstruct.METHOD;
                        }
                    }
                    return CausingConstruct.OTHER;
                })
                .toList();
    }

    private static boolean isTestFailure(TestResult testResult) {
        return testResult.throwable.className.contains("ssertion") || testResult.throwable.className.contains(
                "ssumption");
    }

    @Override
    public void finish() {
        Path outputPath = Config.getResourcesDir().resolve("causing-constructs.json");
        JsonUtils.writeToFile(outputPath, this.result);
        log.info("Finished writing output of causing constructs");
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

    private List<CtElement> getAllParents(CtElement ctElement) {
        if (ctElement.getParent() == null) {
            return List.of();
        } else {
            return Stream.concat(getAllParents(ctElement.getParent()).stream(), Stream.of(ctElement.getParent())).toList();
        }
    }

    private List<CtElement> removeParents(List<CtElement> elements) {
        List<CtElement> parents = elements.stream()
                .filter(ctElement -> ctElement.getParent() != null)
                .map(this::getAllParents)
                .flatMap(List::stream)
                .toList();
        return elements.stream()
                .filter(ctElement -> !parents.contains(ctElement))
                .toList();
    }

    private boolean foundAnyProjectTestFile(List<ImmutablePair<StackTraceElement, Optional<File>>> projectTestFilePerStackTraceElement) {
        return projectTestFilePerStackTraceElement.stream()
                .anyMatch(pair -> pair.right.isPresent());
    }

    private List<StackTraceElement> getLocatedStackTraceElements(List<ImmutablePair<StackTraceElement,
            Optional<File>>> projectTestFilePerStackTraceElement) {
        return projectTestFilePerStackTraceElement.stream()
                .filter(pair -> pair.right.isPresent())
                .map(ImmutablePair::getLeft)
                .toList();
    }
}
