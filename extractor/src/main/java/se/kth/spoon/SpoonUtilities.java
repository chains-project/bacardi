package se.kth.spoon;

import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.japicmp_analyzer.ApiChange;
import se.kth.models.ErrorInfo;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.*;

/**
 * This class is responsible for extracting information from Spoon.
 */
@lombok.Getter
@lombok.Setter
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

    public DetectedFileWithErrors filterElements(Set<CtElement> elements, List<String> changedClassnames, Set<ApiChange> apiChanges) {
        List<CtElement> tmp = elements.stream()
                .filter(element -> checkIfAnyConstructIsCalledFromLibrary(element, changedClassnames))
                .toList();
        //create relation between the elements and the api changes
        DirectFailuresScan scanner = new DirectFailuresScan(apiChanges);
        for (CtElement element : tmp) {
            element.accept(scanner);
        }

        Set<ApiChange> executedElements = scanner.getMatchedApiChanges();

        executedElements.forEach(matchedApiChange -> {
            executedElements.addAll(apiChanges.stream().filter(apiChange -> apiChange.getName().equals(matchedApiChange.getName())).toList());
        });
        return new DetectedFileWithErrors(executedElements, scanner.getExecutedElements());
    }

    public boolean checkIfAnyConstructIsCalledFromLibrary(CtElement element, List<String> changedClassnames) {
        return element.getReferencedTypes().stream()
                .map(CtTypeReference::getQualifiedName)
                .anyMatch(changedClassnames::contains);
    }

    public DetectedFileWithErrors getMethodAndClassInformationForElement(CtElement element, DetectedFileWithErrors fault, ErrorInfo errorInfo) {

        CtModel localModel = buildLocalModel(element.getPosition().getFile().getAbsoluteFile().toString());

        CtType<?> mainClass = localModel.getAllTypes().iterator().next();
        mainClass.getElements(new TypeFilter<>(CtMethodImpl.class)).forEach(e -> {
            System.out.println("File Name: " + e.getPosition().getFile().getName());
            if (this.containsAnError(e, errorInfo)) {
                fault.methodName = e.getSimpleName();
                fault.qualifiedMethodCode = e.toStringDebug();
                fault.methodCode = e.getOriginalSourceFragment().getSourceCode();

                CtClass<?> parentClass = e.getParent(CtClass.class);
                Set<CtMethod<?>> newMethods = new HashSet<CtMethod<?>>();
                Set<CtMethod<?>> oldMethods = parentClass.getMethods();
                newMethods.add(e);

                parentClass.setMethods(newMethods);

                fault.inClassCode = parentClass.toString();
                fault.qualifiedInClassCode = parentClass.toStringDebug();
                parentClass.setMethods(oldMethods);

                fault.clientLineNumber = getRealLinePosition(e);
                fault.clientEndLineNumber = e.getPosition().getEndLine();
                fault.plausibleDependencyIdentifier = null;
            }
        });

        return fault;

    }
    private CtModel buildLocalModel(String filePath) {

        Launcher spoon = new Launcher();
        spoon.getEnvironment().setAutoImports(true);
        spoon.addInputResource(filePath);
        spoon.buildModel();

        return spoon.getModel();
    }

    private boolean containsAnError(CtElement element, ErrorInfo errorInfo) {
        int startLineNumber = this.getRealLinePosition(element);
        int endLineNumber = element.getPosition().getEndLine();
        int errorLineNumber = Integer.parseInt(errorInfo.getClientLinePosition());

        return errorLineNumber >= startLineNumber && errorLineNumber <= endLineNumber;

    }

    /**
     * TODO Check this method and found a better way to get the error info
     *
     * @param element The element.
     * @return The error info.
     */
    private int getRealLinePosition(CtElement element) {
        // Need to do this trick as getLine does not take into account for decorators, and comments
        String[] lines = element.getOriginalSourceFragment().getSourceCode().split("\r\n|\r|\n");
        int numberOfLines = lines.length;
        return element.getPosition().getEndLine() - numberOfLines + 1;
    }
}
