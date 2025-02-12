package se.kth.model;

import se.kth.failure_detection.DetectedFileWithErrors;

import java.util.Set;

@lombok.Getter
@lombok.Setter

/*
 * This class is a model for the prompt that will be displayed to the user.
 * It contains the class information and the detected errors related to the
 * class.
 */
public class PromptModel {

    String classInfo;
    Set<DetectedFileWithErrors> DetectedFileWithErrors;
    String libraryName;
    String baseVersion;
    String newVersion;

    public PromptModel(String classInfo, Set<DetectedFileWithErrors> DetectedFileWithErrors) {
        this.classInfo = classInfo;
        this.DetectedFileWithErrors = DetectedFileWithErrors;
    }

    public PromptModel(String classInfo, Set<DetectedFileWithErrors> DetectedFileWithErrors, String libraryName,
            String baseVersion, String newVersion) {
        this.classInfo = classInfo;
        this.DetectedFileWithErrors = DetectedFileWithErrors;
        this.libraryName = libraryName;
        this.baseVersion = baseVersion;
        this.newVersion = newVersion;
    }

}
