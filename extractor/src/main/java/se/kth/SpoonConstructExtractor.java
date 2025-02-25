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
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class SpoonConstructExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpoonConstructExtractor.class);

    private final MavenErrorLog mavenErrorLog;
    private final JApiCmpAnalyze japicmpAnalyzer;
    private final SpoonUtilities spoonUtilities;
    private final String pipeline;


    public SpoonConstructExtractor(MavenErrorLog mavenErrorLog, JApiCmpAnalyze japicmpAnalyzer, SpoonUtilities spoonUtilities, String pipeline) {
        this.mavenErrorLog = mavenErrorLog;
        this.japicmpAnalyzer = japicmpAnalyzer;
        this.spoonUtilities = spoonUtilities;
        this.pipeline = pipeline;
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

    public static Path getDirectFilePath(String filePath, String projectPath) {


        String[] split = projectPath.split("/");
        String folderName = split[split.length - 1];
        String cFile = filePath.replaceFirst("^/" + folderName, "");

        Path absolutePath = Paths.get(projectPath, cFile);

        return absolutePath;


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
            Path filePath = getDirectFilePath(key, spoonUtilities.getClient().getSourcePath().toString());

            CtModel model = spoonUtilities.getClient().createModel(filePath);
            spoonUtilities.setModel(model);

            value.forEach(errorInfo -> {
                // all elements from the buggy line in the client application code
                Set<CtElement> elements = spoonUtilities.localizeErrorInfoElements(errorInfo);


                Set<DetectedFileWithErrors> detectedElements = spoonUtilities.filterElements(elements, classNamesJapicmp, apiChanges);

                detectedElements.forEach(d -> {
                    d.setErrorInfo(errorInfo);
                });


                if (detectedFiles.containsKey(key)) {
                    if (detectedElements.isEmpty()) {
                        detectedFiles.get(key).add(new DetectedFileWithErrors(errorInfo));
                    } else {
                        detectedFiles.get(key).addAll(detectedElements);
                    }
                } else {
                    if (detectedElements.isEmpty()) {
                        detectedFiles.put(key, new HashSet<>(Set.of(new DetectedFileWithErrors(errorInfo))));
                    } else {
                        detectedFiles.put(key, detectedElements);
                    }
                }
            });
        });
        return detectedFiles;
    }

}
