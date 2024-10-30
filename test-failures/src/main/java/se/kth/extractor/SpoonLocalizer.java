package se.kth.extractor;

import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.model.UpdatedDependency;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class SpoonLocalizer {

    public static void localize(List<ImmutablePair<StackTraceElement, Optional<File>>> files, TestResult testResult,
                                UpdatedDependency updatedDependency) {
        ImmutablePair<StackTraceElement, File> first = files.stream()
                .filter(pair -> pair.right.isPresent())
                .findFirst()
                .map(pair -> new ImmutablePair<>(pair.left, pair.right.get()))
                .get();

        var firstElement = files.stream().filter(pair -> pair.left.equals(first.left)).findFirst().orElse(null);
        int firstIndex = files.indexOf(firstElement);

        List<StackTraceElement> stackTraceElements = files.stream()
                .map(pair -> pair.left)
                .toList();
        int firstDependencyIndex = findFirstUpdatedDependencyClassInStackTrace(stackTraceElements, updatedDependency);

        StackTraceElement testMethod = first.left;
        String testClassFileName = testMethod.getFileName();
        long lineNumber = testMethod.getLineNumber();

        SpoonAPI spoon = new Launcher();
        spoon.addInputResource(first.right.getAbsolutePath());
        CtModel model = spoon.buildModel();

        File testFile = first.right;
        List<CtElement> result = new LinkedList<>();
        model.getElements(new TypeFilter<>(CtElement.class)).stream()
                .forEach(ctElement -> {
                    if (!ctElement.isImplicit() && ctElement.getPosition().isValidPosition()) {
                        System.out.println(ctElement.getPosition().getLine());
                        if (ctElement.getPosition().getLine() == lineNumber) {
                            result.add(ctElement);
                        }
                    }
                });
        List<CtElement> parent = extractParents(result);
    }

    private static List<CtElement> extractParents(List<CtElement> elements) {
        return elements.stream()
                .filter(ctElement -> !elements.contains(ctElement.getParent()))
                .toList();
    }

    private static int findFirstUpdatedDependencyClassInStackTrace(List<StackTraceElement> stackTraceElements,
                                                                   UpdatedDependency updatedDependency) {
        String groupId = updatedDependency.dependencyGroupID;
        StackTraceElement first = stackTraceElements.stream()
                .filter(e -> e.getClassName().startsWith(groupId))
                .findFirst()
                .orElse(null);

        if (first != null) {
            return stackTraceElements.indexOf(first);
        } else {
            return -1;
        }
    }
}
