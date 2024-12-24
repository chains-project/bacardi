package se.kth;

import japicmp.model.JApiClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.japicmp_analyzer.ApiChange;
import se.kth.japicmp_analyzer.JApiCmpAnalyze;
import se.kth.models.ErrorInfo;
import se.kth.models.MavenErrorLog;
import se.kth.spoon.SpoonUtilities;
import spoon.reflect.declaration.CtElement;

import java.util.*;


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


    public Map<String, Set<DetectedFileWithErrors>> extractCausingConstructs() {

        List<JApiClass> classes = this.japicmpAnalyzer.getChanges();
        //get all class names from the japicmpAnalyzer
        List<String> classNamesJapicmp = classes.stream()
                .map(JApiClass::getFullyQualifiedName)
                .toList();

        Set<ApiChange> apiChanges = this.japicmpAnalyzer.getAllChanges(classes);

        final var detectedFiles = getStringDetectedFileWithErrorsMap(classNamesJapicmp, apiChanges);

        return detectedFiles;
    }

    private Map<String, Set<DetectedFileWithErrors>> getStringDetectedFileWithErrorsMap(List<String> classNamesJapicmp, Set<ApiChange> apiChanges) {
        Map<String, Set<DetectedFileWithErrors>> detectedFiles = new HashMap<>();

        Map<String, Set<ErrorInfo>> errorInfoMap = mavenErrorLog.getErrorInfo();
        /*
         * For each error in the error log, we try to find the corresponding construct in the client application code
         * Each key is the bug file that contains the error
         * Each value is a set of error information extracted from the log file and related to de bug file
         */
        errorInfoMap.forEach((key, value) -> {
            value.forEach(errorInfo -> {
                // all elements from the buggy line in the client application code
                Set<CtElement> elements = spoonUtilities.localizeErrorInfoElements(errorInfo);
                DetectedFileWithErrors detectedFault = spoonUtilities.filterElements(elements, classNamesJapicmp, apiChanges);
                detectedFault.setErrorInfo(errorInfo);


                CtElement element = detectedFault.getExecutedElements().isEmpty() ? null : detectedFault.getExecutedElements().stream().toList().getFirst();
                if (element != null) {
                    detectedFault = spoonUtilities.getMethodAndClassInformationForElement(element, detectedFault, errorInfo);
                    if (detectedFiles.containsKey(key)) {
                        detectedFiles.get(key).add(detectedFault);
                    } else {
                        detectedFiles.put(key, new HashSet<>(Set.of(detectedFault)));
                    }
                } else {
                    if (detectedFiles.containsKey(key)) {
                        detectedFiles.get(key).add(detectedFault);
                    } else {
                        detectedFiles.put(key, new HashSet<>(Set.of(detectedFault)));
                    }
                }


            });
        });
        return detectedFiles;
    }


}
