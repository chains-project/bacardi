package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.failure_detection.DetectedFileWithErrors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BasePromptAnthropicBuggyTemplate extends AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(BasePromptAnthropicTemplate.class);

    public BasePromptAnthropicBuggyTemplate() {
        super();
    }

    @Override
    public String header() {
        return """
                You are an Automatic Program Repair (APR) tool specialized in fixing Java code issues caused by breaking dependency updates.
                Your task is to analyze the provided code and error message, then propose a patch that can be applied to the client code to resolve the issue.
                """;
    }

    @Override
    public String classCode() {
        try {
            String classCode = Files.readString(Path.of(promptModel.getClassInfo()));
            return """
                    Here is the client code that is failing:
                    ```java
                    %s
                    ```
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
                And here is the error message:
                %s
                """.formatted(errorInformation);
    }

    public String buggyLine() {

        String buggyLine = """
                """;

        for (DetectedFileWithErrors detectedFileWithErrors : promptModel.getDetectedFileWithErrors()) {
            String lineInCode = detectedFileWithErrors.getLineInCode();
            String line = """
                    ```java
                    %s
                    ```
                    """.formatted(lineInCode);

            buggyLine = buggyLine.concat(line);
        }

        return """
                the error is triggered in the following specific lines in the previous code:

                %s
                """.formatted(buggyLine);
    }

    @Override
    public String generatePrompt() {

        return """
                 %s
                 %s
                 %s
                 %s
                 Before proposing a fix, please analyze the error message and client code. Wrap your analysis inside <code_analysis> tags:

                <code_analysis>
                1. Examine the error message:
                   - Identify the specific issue related to the dependency update.
                   - Note the line number or method where the error occurs.
                   - Determine which dependency and version is causing the issue.

                2. Review the client code:
                   - Locate the problematic areas mentioned in the error message.
                   - Identify any related code that might be affected by the changes.

                3. Consider potential fixes that adhere to the following constraints:
                   - Do not change any function signatures.
                   - Only remove the @Override annotation if the method no longer overrides a method in the updated dependency version.
                   - Ensure correct imports are used, considering the newer dependency version.
                   - Avoid removing existing code unless it directly causes a compilation or functionality error.

                4. Plan the necessary changes to fix the issue:
                   - List the specific modifications required.
                   - Consider potential side effects of the proposed changes.
                   - Ensure the fix addresses the root cause of the error.
                </code_analysis>

                Based on your analysis, propose a patch to fix the issue. Your response should be a complete and compilable Java class in a fenced code block. Adhere to these guidelines:

                1. Do not change any function signatures.
                2. You may create variables if it simplifies the code.
                3. Remove the @Override annotation only if the method no longer overrides a method in the updated dependency version.
                4. If fixing the issue requires addressing missing imports, ensure the correct package or class is used in accordance with the newer dependency version.
                5. Avoid removing any existing code unless it directly causes a compilation or functionality error.
                6. Ensure the entire class is included and that it will compile correctly.

                Please provide your fixed class in the following format:

                ```java
                // Your complete, fixed Java class here
                ```

                Remember to focus specifically on issues related to the dependency update when proposing your fix.
                \s"""
                .formatted(header(), classCode(), buggyLine(), errorLog());
    }
}
