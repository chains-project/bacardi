package se.kth.prompt;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasePromptCotBuggyLineApiDiffTemplate extends AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(BasePromptCotBuggyLineApiDiffTemplate.class);


    @Override
    public String header() {
        return """
                You are an Automatic Program Repair (APR) tool specialized in fixing Java code issues caused by breaking dependency updates. Your task is to analyze the provided code, error information, and API changes, then propose and apply a patch to fix the issue while adhering to specific constraints.
                
                Here is the Java code that is failing:
                
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
                    """.formatted(classCode).stripTrailing();
        } catch (IOException e) {
            log.error("Error reading the class file", e);
            throw new RuntimeException(e);
        }
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
        String numberOfLines = "";

        if (promptModel.getDetectedFileWithErrors().size() > 1) {
            numberOfLines = "The errors are triggered in the following specific lines:";
        } else {
            numberOfLines = "The error is triggered in the following specific line:";
        }

        return """
                %s
                
                <error_lines>
                %s
                </error_lines>
                """.formatted(numberOfLines, buggyLine.stripTrailing()).stripTrailing();
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
            String errorMessage = detectedFileWithErrors.getErrorInfo().getErrorMessage();
            String additionalInfo = detectedFileWithErrors.getErrorInfo().getAdditionalInfo();
            String errorLogsForPrompt = """
                    %s
                    %s
                    """.formatted(errorMessage, additionalInfo);
            if (!errorMessageList.contains(errorMessage)) {
                errorMessageList.add(errorMessage);
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
                The API of the dependency has changed. Here are the relevant changes:
                
                %s
                """.formatted(apiDiff);


        return """
                %s
                
                Additional error information:
                
                <error_information>
                %s
                </error_information>
                """.formatted(apiDiffList.stripTrailing(), errorInformation.stripTrailing()).stripTrailing();
    }

    @Override
    public String generatePrompt() {
        return """
                %s
                
                %s
                
                %s
                
                %s
                
                Your task is to fix the issue by modifying only the client code. Follow these steps:
                
                1. Analyze the problem and propose changes by wrapping the work inside fix_planning tags. This analysis is for your internal use only and will not be included in the final output. In your analysis:
                   a. Quote relevant parts of the code and API changes
                   b. Identify the specific API changes causing the issue
                   c. List the affected lines of code
                   d. Propose potential fixes for each affected line
                   e. Consider any potential side effects of the proposed changes
                   f. Explicitly check if the proposed changes adhere to all the given constraints
                   g. Provide a final summary of the chosen fix and why it's the best solution
                
                2. Apply the fix to the Java code.
                
                3. Output the complete, fixed Java class in a fenced code block. This should be the only visible output in your response.
                
                Constraints:
                1. Do not change the function signature of any method.
                2. You may create variables if it simplifies the code.
                3. Remove the @Override annotation if and only if the method no longer overrides a method in the updated dependency version.
                4. If fixing the issue requires addressing missing imports, ensure the correct package or class is used in accordance with the newer dependency version.
                5. Do not remove any existing code unless it directly causes a compilation or functionality error.
                6. Include all code, even unchanged portions, in your final output.
                7. Do not use placeholder comments like "// ... (rest of the code remains unchanged)".
                
                Your final output should only contain the complete, fixed Java class in a fenced code block, without any explanations or analysis visible. The <fix_planning> section is for your internal use only.
                
                Example output structure:
                
                ```java
                [Complete, fixed Java class]
                ```
                
                Please proceed with your analysis and solution.
                """.formatted(header(), classCode(), buggyLine(), errorLog());

    }

    public String parseLLMResponce(String response) {

        Pattern patron = Pattern.compile("```java\\s*([\\s\\S]*?)```");
        Matcher matcher = patron.matcher(response);

        String lastOccurrence = "";

        while (matcher.find()) {
            lastOccurrence = matcher.group(1);
        }

        if (lastOccurrence.isEmpty()) {
            log.error("Error extracting content from the model response");
            return null;
        }

        return lastOccurrence;
    }
}