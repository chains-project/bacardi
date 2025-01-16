package se.kth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import se.kth.models.FailureCategory;

public record Attempt(@Getter int index, @Getter FailureCategory failureCategory, @Getter int prefixFiles,
        @Getter int postfixFiles, @Getter int fixedFiles, @Getter int unfixedFiles, @Getter int newFiles,
        @Getter int prefixErrors, @Getter int postfixErrors, @Getter int fixedErrors, @Getter int unfixedErrors,
        @Getter int newErrors, @Getter String outputFolder, @Getter boolean successful) {

    @JsonCreator
    public Attempt(
            @JsonProperty("index") int index,
            @JsonProperty("failureCategory") FailureCategory failureCategory,
            @JsonProperty("outputFolder") String outputFolder,
            @JsonProperty("successful") boolean successful) {
        this(index, failureCategory, processPrefixFiles(outputFolder), processPostfixFiles(outputFolder),
                processFixedFiles(outputFolder), processUnfixedFiles(outputFolder), processNewFiles(outputFolder),
                processPrefixErrors(outputFolder), processPostfixErrors(outputFolder), processFixedErrors(outputFolder),
                processUnfixedErrors(outputFolder), processNewErrors(outputFolder), outputFolder, successful);
    }

    public Attempt(int index, FailureCategory failureCategory, int prefixFiles, int postfixFiles, int fixedFiles,
            int unfixedFiles, int newFiles, int prefixErrors, int postfixErrors, int fixedErrors, int unfixedErrors,
            int newErrors, String outputFolder, boolean successful) {
        this.index = index;
        this.failureCategory = failureCategory;
        this.prefixFiles = prefixFiles;
        this.postfixFiles = postfixFiles;
        this.fixedFiles = fixedFiles;
        this.unfixedFiles = unfixedFiles;
        this.newFiles = newFiles;
        this.prefixErrors = prefixErrors;
        this.postfixErrors = postfixErrors;
        this.fixedErrors = fixedErrors;
        this.unfixedErrors = unfixedErrors;
        this.newErrors = newErrors;
        this.outputFolder = outputFolder;
        this.successful = successful;
    }

    private static int processPrefixFiles(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // prefixFiles
        return 0; // Replace with actual logic
    }

    private static int processPostfixFiles(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // postfixFiles
        return 0; // Replace with actual logic
    }

    private static int processFixedFiles(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // fixedFiles
        return 0; // Replace with actual logic
    }

    private static int processUnfixedFiles(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // unfixedFiles
        return 0; // Replace with actual logic
    }

    private static int processNewFiles(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // newFiles
        return 0; // Replace with actual logic
    }

    private static int processPrefixErrors(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // prefixErrors
        return 0; // Replace with actual logic
    }

    private static int processPostfixErrors(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // postfixErrors
        return 0; // Replace with actual logic
    }

    private static int processFixedErrors(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // fixedErrors
        return 0; // Replace with actual logic
    }

    private static int processUnfixedErrors(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // unfixedErrors
        return 0; // Replace with actual logic
    }

    private static int processNewErrors(String outputFile) {
        // Implement the logic to process the output folder and return the value for
        // newErrors
        return 0; // Replace with actual logic
    }
}