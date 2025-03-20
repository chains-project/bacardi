package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.failure_detection.DetectedFileWithErrors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BaseLineCotTemplate extends AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(BaseLineCotTemplate.class);

    public BaseLineCotTemplate() {
        super();
    }

    @Override
    public String header() {
        return """
       Act as an Automatic Program Repair (APR) tool, reply only with code, without explanation.
       You are specialized in breaking dependency updates, in which the failure is caused by an external dependency.
       To solve the failure you can only work on the client code.
       """;
    }

    @Override
    public String classCode() {
        try {
            String classCode = Files.readString(Path.of(promptModel.getClassInfo()));
            return """
                    the following client code fails:
                    ```java
                        %s
                    ```
                    """.formatted(classCode.stripTrailing());
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
                with the following error message:
                %s
                """.formatted(errorInformation.stripTrailing());
    }

    @Override
    public String generatePrompt() {

        return """
                %s
                
                %s
                
                %s
                
                 Before proposing a fix, please analyze the situation and plan your approach within <repair_strategy> tags:
                
                 1. Identify the specific API changes that are causing the failure in the client code.
                 2. Compare the old and new API versions, noting any changes in method signatures, return types, or parameter lists.
                 3. Determine which parts of the client code need to be updated to accommodate these API changes.
                 4. Consider any constraints or requirements for the fix (e.g., not changing function signatures, potential import adjustments).
                 5. Plan the minimal set of changes needed to fix the issue while keeping the code functional and compliant with the new API.
                 6. Consider potential side effects of the proposed changes on other parts of the code.
                 7. Ensure that the planned changes will result in a complete and compilable class.
                 8. If applicable, note any additional imports that may be needed due to the API changes.
                
                 propose a patch that can be applied to the code to fix the issue.
                 Return only a complete and compilable class in a fenced code block.
                 Do not remove any code that you don't want to update keep it in the code block. Do not use "// ... (rest of the code remains unchanged)" in your response.
                 You CANNOT change the function signature of any method but may create variables if it simplifies the code.
                 You CAN remove the @Override annotation IF AND ONLY IF the method no longer overrides a method in the updated dependency version.
                 If fixing the issue requires addressing missing imports, ensure the correct package or class is used in accordance with the newer dependency version.
                 Avoid removing any existing code unless it directly causes a compilation or functionality error. Don't use the comment "// ... (rest of the class remains unchanged)".
                 Return only the fixed class, ensuring it fully compiles and adheres to these constraints.
                \s"""
                .formatted(header().stripTrailing(), classCode().stripTrailing(), errorLog().stripTrailing());
    }
}
