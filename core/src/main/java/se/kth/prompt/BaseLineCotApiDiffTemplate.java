package se.kth.prompt;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.ConstructType;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.japicmp_analyzer.ApiChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BaseLineCotApiDiffTemplate extends AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(BaseLineCotApiDiffTemplate.class);


    @Override
    public String header() {
        return """
                 You are an advanced Automatic Program Repair (APR) tool specialized in fixing breaking dependency updates. Your task is to analyze client code that is failing due to changes in an external dependency's API and propose a fix that can be applied to the client code.
                
                
                """.stripTrailing();
    }

    @Override
    public String classCode() {

        try {
            String classCode = Files.readString(Path.of(promptModel.getClassInfo()));
            return """
                    ```java
                    %s
                    ```
                    """.formatted(classCode)
                    .stripTrailing();
        } catch (IOException e) {
            log.error("Error reading the class file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String errorLog() {

        Set<String> apiDiffUnique = new HashSet<>();

        String errorInformation = """
                """;

        String apiDiff = """
                """;

        List<DetectedFileWithErrors> detected = promptModel.getDetectedFileWithErrors().stream().toList();
        Set<String> errorMessageList = new HashSet<>();
        for (DetectedFileWithErrors detectedFileWithErrors : detected) {
            final var errorLogsForPrompt = getErrorSection(detectedFileWithErrors);

            if (!errorMessageList.contains(errorLogsForPrompt)) {
                errorMessageList.add(errorLogsForPrompt);
                errorInformation = errorInformation.concat(errorLogsForPrompt);

            }

            Set<ApiChange> apiChangeSet = detectedFileWithErrors.getApiChanges();

            String constructType = ConstructType.getAsCausingConstructs(detectedFileWithErrors.getCodeElement());

            for (ApiChange apiChange : apiChangeSet) {
                String changeStatus = "";

                if (apiChange.getCategory().isEmpty()) {
                    changeStatus = constructType;
                } else {
                    String type = ConstructType.getConstruct(apiChange.getCategory().getFirst().getType().toString());
                    changeStatus = Objects.requireNonNullElse(type, constructType);
                }


                String apiChangeInfo = """
                             %s %s has been %s in the new version of the dependency.
                        """.formatted(changeStatus, apiChange.getElement(), apiChange.getAction().toString());

                if (apiDiffUnique.contains(apiChangeInfo)) {
                    continue;
                }
                apiDiffUnique.add(apiChangeInfo);
                apiDiff = apiDiff.concat(apiChangeInfo);
            }
        }

        String apiDiffList = """
                3. The changes in the API of the dependency:
                <api_changes>
                %s
                </api_changes>
                """.formatted(apiDiff);


        return """
                <error_information>
                %s
                </error_information>
                
                %s
                """.formatted(errorInformation.stripTrailing(), apiDiffList.stripTrailing());

    }

    @NotNull
    private static String getErrorSection(DetectedFileWithErrors detectedFileWithErrors) {
        String errorMessage = detectedFileWithErrors.getErrorInfo().getErrorMessage();
        String additionalInfo = detectedFileWithErrors.getErrorInfo().getAdditionalInfo();
        String errorLogsForPrompt = """
                %s
                """.formatted(errorMessage);
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            errorLogsForPrompt = errorLogsForPrompt.concat("""
                    %s
                    """.formatted(additionalInfo));
        }
        return errorLogsForPrompt;
    }

    @Override
    public String generatePrompt() {

        return """
                %s
                First, review the following information:
                
                1. The client code that is failing:
                %s
                
                2. The error information:
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
                
                Now, implement your fix based on your analysis. When creating your solution, adhere to the following guidelines:
                
                1. Provide a complete and compilable class in a fenced code block.
                2. Do not remove any code that you don't want to update; keep it in the code block.
                3. Do not use placeholders like "// ... (rest of the code remains unchanged)" in your response.
                4. You CANNOT change the function signature of any method, but you may create variables if it simplifies the code.
                5. You CAN remove the @Override annotation IF AND ONLY IF the method no longer overrides a method in the updated dependency version.
                6. If fixing the issue requires addressing missing imports, ensure the correct package or class is used in accordance with the newer dependency version.
                7. Avoid removing any existing code unless it directly causes a compilation or functionality error.
                8. Ensure that your fix addresses the breaking dependency update and returns the whole class, as per the user's feedback.
                
                Return only the fixed class, ensuring it fully compiles and adheres to these constraints. Begin your response with the fenced code block containing the complete, fixed class.
                """.formatted(header(), classCode(), errorLog());
    }


}
