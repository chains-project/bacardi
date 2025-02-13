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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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


        String[] partes = projectPath.split("/");
        String nombreCarpeta = partes[partes.length - 1]; // Última parte del path

        // Eliminar la parte duplicada del archivo
        String archivoLimpio = filePath.replaceFirst("^/" + nombreCarpeta, "");

        // Construir la ruta completa
        Path rutaCompleta = Paths.get(projectPath, archivoLimpio);

        return rutaCompleta;


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
                    detectedFiles.get(key).addAll(detectedElements);
                } else {
                    detectedFiles.put(key, detectedElements);
                }
            });
        });
        return detectedFiles;
    }

}
