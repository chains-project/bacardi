package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.failure_detection.DetectedFileWithErrors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FixYouPromptTemplate extends AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(FixYouPromptTemplate.class);

    public FixYouPromptTemplate() {
        super();
    }

    @Override
    public String header() {
        return """
                Due to an update of Library %s from version %s to version %s, the code in file %s below is causing errors.
                """
                .formatted(promptModel.getLibraryName(), promptModel.getBaseVersion(), promptModel.getNewVersion(),
                        Path.of(promptModel.getClassInfo()).getFileName());
    }

    @Override
    public String classCode() {
        try {
            String classCode = Files.readString(Path.of(promptModel.getClassInfo()));
            return """
                    %s
                    """.formatted(classCode);
        } catch (IOException e) {
            log.error("Error reading the class file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String errorLog() {

        String errorInformation = """
                """;

        List<DetectedFileWithErrors> detected = promptModel.getDetectedFileWithErrors().stream().toList();
        for (DetectedFileWithErrors detectedFileWithErrors : detected) {
            String errorMessage = detectedFileWithErrors.getErrorInfo().getErrorMessage();
            String additionalInfo = detectedFileWithErrors.getErrorInfo().getAdditionalInfo();
            String errorLogsForPrompt = """
                    %s
                    %s
                    """.formatted(errorMessage, additionalInfo);
            errorInformation = errorInformation.concat(errorLogsForPrompt);
        }
        return """
                Here is the error message:
                %s
                """.formatted(errorInformation);
    }

    @Override
    public String generatePrompt() {

        log.info("Generating prompt for fix you pipeline");

        return """
                %s
                %s
                Update the provided code to fix this error.

                Focus only on updates that do not change the code's functionality and are related to the version update of the library.

                You must reply in the following exact numbered format.

                1. `The full updated code in a fenced code block` do not remove any code that you don't want to update keep it in the code block. Do not use "// ... (rest of the code remains unchanged)" in your response.
                2. Explanation of the changes you made.

                Provided code:

                ```
                %s
                ```

                Your Response:"""
                .formatted(header(), errorLog(), classCode());
    }
}