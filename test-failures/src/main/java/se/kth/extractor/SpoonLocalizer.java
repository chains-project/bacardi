package se.kth.extractor;

import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtAnnotationImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtFieldImpl;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class SpoonLocalizer {

    private final CtModel model;

    public SpoonLocalizer(Path projectPath) {
        MavenLauncher launcher = new MavenLauncher(projectPath.toString(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
        launcher.buildModel();
        this.model = launcher.getModel();
    }

    public Optional<CtElement> localizeElementFromStackTraceElement(StackTraceElement stackTraceElement) {
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
        return parent;
    }

    public List<CtElement> localizeTestRootElementsFromStackTraceElement(StackTraceElement stackTraceElement) {
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
                return List.of(parentElement);
            }
            if (parentElement instanceof CtAnnotationImpl<?>) {
                return List.of(parentElement);
            }
            return getTestMethod(parentElement);
        } else {
            System.out.println("no parent found");
        }
        return null;
    }

    private List<CtElement> getTestMethod(CtElement element) {
        if (element == null) {
            return null;
        }
        CtElement parent = element.getParent();

        if (parent instanceof CtMethodImpl) {
            return isAnnotatedAsTest((CtMethodImpl) parent) ? List.of(parent) : getTestMethod(parent);
        }
        if (parent instanceof CtClassImpl<?>) {
            return this.getClassElements((CtClassImpl<?>) parent);
        }
        return getTestMethod(parent);
    }

    private List<CtElement> getClassElements(CtClassImpl<?> ctClass) {
        Stream<CtElement> staticFields = ctClass.getFields().stream()
                .filter(CtModifiable::isStatic)
                .map(ctField -> (CtElement) ctField);
        Stream<CtElement> annotations = ctClass.getAnnotations().stream()
                .map(ctAnnotation -> (CtElement) ctAnnotation);
        return Stream.concat(staticFields, annotations).toList();
    }

    private boolean isAnnotatedAsTest(CtMethodImpl method) {
        List<String> testAnnotationNames = List.of("Test", "ParameterizedTest", "RepeatedTest", "After", "Before",
                "AfterEach", "BeforeEach", "AfterAll", "BeforeAll");
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
        CtElement rootElement = testElements.stream()
                .map(this::localizeElementFromStackTraceElement)
                .filter(elements -> !elements.isEmpty())
                .findFirst()
                .get()
                .get();
        return this.getAllChildren(List.of(rootElement));
    }

    private static Optional<CtElement> extractParent(List<CtElement> elements) {
        return elements.stream()
                .filter(ctElement -> !elements.contains(ctElement.getParent()))
                .findFirst();
    }
}
