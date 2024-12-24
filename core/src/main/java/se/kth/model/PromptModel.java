package se.kth.model;

import se.kth.failure_detection.DetectedFileWithErrors;

import java.util.Set;

@lombok.Getter
@lombok.Setter

/*
 * This class is a model for the prompt that will be displayed to the user.
 * It contains the class information and the detected errors related to the class.
 */
public class PromptModel {

    String classInfo;
    Set<DetectedFileWithErrors> DetectedFileWithErrors;

    public PromptModel(String classInfo, Set<DetectedFileWithErrors> DetectedFileWithErrors) {
        this.classInfo = classInfo;
        this.DetectedFileWithErrors = DetectedFileWithErrors;
    }

}
