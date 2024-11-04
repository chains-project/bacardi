package se.kth.extractor;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtAnnotationImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtFieldImpl;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.*;

public class SpoonLocalizer {

    private final CtModel model;

    public SpoonLocalizer(Path projectPath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath.toString());
        launcher.buildModel();
        this.model = launcher.getModel();
    }

    public CtElement localizeTestRootElementFromStackTraceElement(StackTraceElement stackTraceElement) {
        int lineNumber = stackTraceElement.getLineNumber();
        String fileName = stackTraceElement.getFileName();
        List<CtElement> result = new LinkedList<>();

        this.model.getElements(new TypeFilter<>(CtElement.class)).stream()
                .forEach(ctElement -> {
                    if (!ctElement.isImplicit() && ctElement.getPosition().isValidPosition()) {
                        if (ctElement.getPosition().getFile().getName().equals(fileName) &&
                                ctElement.getPosition().getLine() == lineNumber) {
                            result.add(ctElement);
                        }
                    }
                });
        Optional<CtElement> parent = extractParent(result);
        if (parent.isPresent()) {
            CtElement parentElement = parent.get();
            if (parentElement instanceof CtFieldImpl<?>) {
                return parentElement;
            }
            if (parentElement instanceof CtAnnotationImpl<?>) {
                return parentElement;
            }
            return getTestMethod(parentElement);
        } else {
            System.out.println("no parent found");
        }
        return null;
    }

    private CtElement getTestMethod(CtElement element) {
        if (element == null) {
            return null;
        }
        CtElement parent = element.getParent();

        if (parent instanceof CtMethodImpl) {
            return isAnnotatedAsTest((CtMethodImpl) parent) ? parent : getTestMethod(parent);
        }
        if (parent instanceof CtClassImpl<?>) {
            return null;
        }
        return getTestMethod(parent);
    }

    private boolean isAnnotatedAsTest(CtMethodImpl method) {
        List<String> testAnnotationNames = List.of("Test", "ParameterizedTest", "RepeatedTest", "After", "Before",
                "AfterEach",
                "BeforeEach", "AfterAll", "BeforeAll");
        List<CtAnnotation<? extends Annotation>> annotations = method.getAnnotations();
        return annotations.stream()
                .map(CtAnnotation::getName)
                .anyMatch(testAnnotationNames::contains);
    }

    public Set<CtElement> getAllChildren(List<CtElement> elements) {
        Set<CtElement> executedElements = new HashSet<>();
        CustomScanner scanner = new CustomScanner(executedElements);
        for (CtElement element : elements) {
            element.accept(scanner);
        }
        return scanner.getExecutedElements();
    }

    public Set<CtElement> localize(List<StackTraceElement> testElements) {
        List<CtElement> rootElements = testElements.stream()
                .map(this::localizeTestRootElementFromStackTraceElement)
                .filter(Objects::nonNull)
                .toList();
        return this.getAllChildren(rootElements);
    }

    private static Optional<CtElement> extractParent(List<CtElement> elements) {
        return elements.stream()
                .filter(ctElement -> !elements.contains(ctElement.getParent()))
                .findFirst();
    }
}
