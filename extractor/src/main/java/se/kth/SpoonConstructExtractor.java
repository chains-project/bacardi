package se.kth;

import japicmp.model.JApiClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.japicmp_analyzer.ApiChange;
import se.kth.japicmp_analyzer.JApiCmpAnalyze;
import se.kth.models.ErrorInfo;
import se.kth.models.MavenErrorLog;
import se.kth.spoon.SpoonFullyQualifiedNameExtractor;
import se.kth.spoon.SpoonUtilities;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public class SpoonConstructExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpoonConstructExtractor.class);

    private final MavenErrorLog mavenErrorLog;
    private final JApiCmpAnalyze japicmpAnalyzer;
    private final SpoonUtilities spoonUtilities;



    public SpoonConstructExtractor(MavenErrorLog mavenErrorLog, JApiCmpAnalyze japicmpAnalyzer, SpoonUtilities spoonUtilities) {
        this.mavenErrorLog = mavenErrorLog;
        this.japicmpAnalyzer = japicmpAnalyzer;
        this.spoonUtilities = spoonUtilities;
    }


    public void extractCausingConstructs() {

        List<JApiClass> classes = this.japicmpAnalyzer.getChanges();
        //get all class names from the japicmpAnalyzer
        List<String> classNamesJapicmp = classes.stream()
                .map(JApiClass::getFullyQualifiedName)
                .toList();

        Set<ApiChange> apiChanges = this.japicmpAnalyzer.getAllChanges(classes);

        Map<String, Set<ErrorInfo>> errorInfoMap = mavenErrorLog.getErrorInfo();
        /*
         * For each error in the error log, we try to find the corresponding construct in the client application code
         * Each key is the bug file that contains the error
         * Each value is a set of error information extracted from the log file and related to de bug file
         */
        errorInfoMap.forEach((key, value) -> {
            log.info("Analyzing error in file: {}", key);
            value.forEach(errorInfo -> {
                // all elements from the buggy line in the client application code
                Set<CtElement> elements = spoonUtilities.localizeErrorInfoElements(errorInfo);
                Set<CtElement> filteredElements = spoonUtilities.filterElements(elements, classNamesJapicmp, apiChanges);

//                List<CtElement> involvedElements = this.checkChangedInvolved(elements.stream().toList(), classNamesJapicmp);
//                List<CtElement> result = this.removeParents(involvedElements);

                log.info("Involved elements: ");
                filteredElements.forEach(ctElement -> {
                    log.info("Element: {}", ctElement);
                });

            });
        });
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
                .map(this::getAllParents)
                .flatMap(List::stream)
                .toList();
        return elements.stream()
                .filter(ctElement -> !parents.contains(ctElement))
                .toList();
    }

    private List<CtElement> getAllParents(CtElement ctElement) {
        if (ctElement.getParent() == null) {
            return List.of();
        } else {
            return Stream.concat(getAllParents(ctElement.getParent()).stream(), Stream.of(ctElement.getParent())).toList();
        }
    }
}
