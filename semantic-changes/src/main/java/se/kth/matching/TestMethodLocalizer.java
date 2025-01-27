package se.kth.matching;


import se.kth.extractor.SpoonLocalizer;
import spoon.reflect.declaration.CtElement;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class TestMethodLocalizer {

    private final SpoonLocalizer spoonLocalizer;

    public TestMethodLocalizer(Path projectPath) {
        this.spoonLocalizer = new SpoonLocalizer(projectPath);
    }

    public Optional<CtMethodImpl> locateTestMethods(StackTraceElement[] stackTraceElements) {
        for (int i = stackTraceElements.length - 1; i >= 0; i--) {
            StackTraceElement stackTraceElement = stackTraceElements[i];
            Optional<CtElement> element = spoonLocalizer.localizeElementFromStackTraceElement(stackTraceElement);
            if (element.isPresent()) {
                List<CtElement> testRootElements =
                        spoonLocalizer.localizeTestRootElementsFromStackTraceElement(stackTraceElement);
                if (testRootElements != null && !testRootElements.isEmpty()) {
                    if (testRootElements.getFirst() instanceof CtMethodImpl) {
                        return Optional.of((CtMethodImpl) testRootElements.getFirst());
                    }
                }
            }
        }
        return Optional.empty();
    }
}
