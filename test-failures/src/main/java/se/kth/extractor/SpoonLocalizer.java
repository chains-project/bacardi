package se.kth.extractor;

import japicmp.model.JApiClass;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.UpdatedDependency;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
                .anyMatch(s -> testAnnotationNames.contains(s));
    }

    public boolean localize(StackTraceElement first, TestResult testResult,
                            UpdatedDependency updatedDependency, List<JApiClass> changedClasses) {
        StackTraceElement testMethod = first;

        CtElement element = localizeTestRootElementFromStackTraceElement(testMethod);
        if (element == null) {
            System.out.println("not found");
        }
        return element != null;
    }

    private static Optional<CtElement> extractParent(List<CtElement> elements) {
        return elements.stream()
                .filter(ctElement -> !elements.contains(ctElement.getParent()))
                .findFirst();
    }
}
