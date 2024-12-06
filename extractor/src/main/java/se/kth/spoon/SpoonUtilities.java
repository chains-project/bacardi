package se.kth.spoon;

import se.kth.japicmp_analyzer.ApiChange;
import se.kth.models.ErrorInfo;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;

/**
 * This class is responsible for extracting information from Spoon.
 */
public class SpoonUtilities {
    /**
     * The Spoon model.
     */
    private final CtModel model;
    private final Client client;

    public SpoonUtilities(Client client) {
        this.client = client;
       /*
        @TODO: implement the case for MavenLauncher
        */

        this.model = client.createModel();
    }

    /**
     * This method is responsible for checking if an element should be ignored.
     * Double check with the configuration.
     *
     * @param element The element.
     * @return True if the element should be ignored, false otherwise.
     */
    public static boolean shouldBeIgnored(CtElement element) {
        return !(element instanceof CtComment) && !element.isImplicit();
    }

    /**
     * This method is responsible for localizing an element from the client application code.
     *
     * @param lineNumber The line number.
     * @param fileName   The file name.
     * @return The element.
     */
    public Set<CtElement> localizeElementFromTheClientApplicationCode(int lineNumber, String fileName) {
        List<CtElement> result = model.filterChildren(element ->
                shouldBeIgnored(element)
                        && element.getPosition().isValidPosition()
                        && element.getPosition().toString().contains(fileName)
                        && element.getPosition().getLine() == lineNumber
        ).list();

//        add imports manually because they are not in the children list
        model.getRootPackage().getFactory().CompilationUnit().getMap().forEach((k, v) -> {
            if (v.getPosition().toString().contains(fileName)) {
                v.getImports().forEach(imp -> {
                    if (shouldBeIgnored(imp)
                            && imp.getPosition().isValidPosition()
                            && imp.getPosition().getLine() == lineNumber) {
                        result.add(imp);
                    }
                });
            }
        });
        return new HashSet<>(result);
    }

    /**
     * This method is responsible for extracting the parent element.
     *
     * @param elements The elements.
     * @return The parent element.
     */
    private static Optional<CtElement> extractParent(List<CtElement> elements) {
        return elements.stream()
                .filter(ctElement -> !elements.contains(ctElement.getParent()))
                .findFirst();
    }

    public Set<CtElement> localizeErrorInfoElements(ErrorInfo errorInfo) {
        // Get all elements from the client application code
        Set<CtElement> rootElement = this.localizeElementFromTheClientApplicationCode(
                Integer.parseInt(errorInfo.getClientLinePosition()),
                errorInfo.getFileName()
        );
        if (rootElement.isEmpty()) {
            return Collections.emptySet();
        }
        return rootElement;
    }

    public Set<CtElement> filterElements(Set<CtElement> elements, List<String> changedClassnames, Set<ApiChange> apiChanges) {
        List<CtElement> tmp = elements.stream()
                .filter(element -> checkIfAnyConstructIsCalledFromLibrary(element, changedClassnames))
                .toList();
        //create relation between the elements and the api changes
        DirectFailuresScan scanner = new DirectFailuresScan(apiChanges);
        for (CtElement element : tmp) {
            element.accept(scanner);
        }
        System.out.println("Executed elements: " + scanner.getMatchedApiChanges().size());

        Set<ApiChange> executedElements = scanner.getMatchedApiChanges();
        executedElements.forEach(matchedApiChange -> {
            executedElements.addAll(apiChanges.stream().filter(apiChange -> apiChange.getName().equals(matchedApiChange.getName())).toList());
        });
        System.out.println("Matched API changes: " + executedElements.size());
        scanner.getMatchedApiChanges().forEach(apiChange -> {
            System.out.println("Found a match: " + apiChange.getLongName() + " : " + apiChange.getCategory());
            System.out.println("Diff: " + apiChange.toDiffString());
            System.out.println("Name: " + apiChange.getName());
            System.out.println("-------------------");
        });


        return scanner.getExecutedElements();
    }

    public boolean checkIfAnyConstructIsCalledFromLibrary(CtElement element, List<String> changedClassnames) {
        return element.getReferencedTypes().stream()
                .map(CtTypeReference::getQualifiedName)
                .anyMatch(changedClassnames::contains);
    }
}
