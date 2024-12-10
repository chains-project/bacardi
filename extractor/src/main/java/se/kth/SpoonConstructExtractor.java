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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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


    public Map<String, DetectedFileWithErrors> extractCausingConstructs() {

        List<JApiClass> classes = this.japicmpAnalyzer.getChanges();
        //get all class names from the japicmpAnalyzer
        List<String> classNamesJapicmp = classes.stream()
                .map(JApiClass::getFullyQualifiedName)
                .toList();

        Set<ApiChange> apiChanges = this.japicmpAnalyzer.getAllChanges(classes);

        final var detectedFiles = getStringDetectedFileWithErrorsMap(classNamesJapicmp, apiChanges);

        detectedFiles.forEach((key, value) -> {
            log.info("Detected file: {}", key);
            log.info("API changes: {}", value.getApiChanges());
            value.getApiChanges().forEach(apiChange -> {
                log.info("API change: {}", apiChange.toDiffString());
            });
            log.info("Executed elements: {}", value.getExecutedElements());
            log.info("Error info: {}", value.getErrorInfo());
            log.info("Fault information: {}", value.toString());
        });
        return detectedFiles;
    }

    private Map<String, DetectedFileWithErrors> getStringDetectedFileWithErrorsMap(List<String> classNamesJapicmp, Set<ApiChange> apiChanges) {
        Map<String, DetectedFileWithErrors> detectedFiles = new HashMap<>();

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
                DetectedFileWithErrors detectedFault = spoonUtilities.filterElements(elements, classNamesJapicmp, apiChanges);
                detectedFault.setErrorInfo(errorInfo);
                CtElement element = detectedFault.getExecutedElements().stream().findFirst().orElse(null);
                detectedFault = spoonUtilities.getMethodAndClassInformationForElement(element, detectedFault, errorInfo);

                detectedFiles.put(key, detectedFault);
            });
        });
        return detectedFiles;
    }


}
