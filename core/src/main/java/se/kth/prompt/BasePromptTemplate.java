package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.failure_detection.DetectedFileWithErrors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BasePromptTemplate extends AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(BasePromptTemplate.class);

    @Override
    public String header() {
        return """
                Act as an Automatic Program Repair (APR) tool, reply only with code, without explanation.
                You are specialized in breaking dependency updates, in which the failure is caused by an external dependency.
                To solve the failure you can only work on the client code.""";
    }

    @Override
    public String classCode() {
        try {
            String classCode = Files.readString(Path.of(promptModel.getClassInfo()));
            return """
                    the following client code fails:
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
                with the following error information:
                %s
                """.formatted(errorInformation);
    }

    @Override
    public String generatePrompt() {

        log.info("Generating prompt for baseline pipeline");

        return """
                 %s
                 %s
                 %s

                 propose a patch that can be applied to the code to fix the issue.
                 Return only a complete and compilable class in a fenced code block.
                 You CANNOT change the function signature of any method but may create variables if it simplifies the code.
                 You CAN remove the @Override annotation IF AND ONLY IF the method no longer overrides a method in the updated dependency version.
                 If fixing the issue requires addressing missing imports, ensure the correct package or class is used in accordance with the newer dependency version.
                 Avoid removing any existing code unless it directly causes a compilation or functionality error.
                 Return only the fixed class, ensuring it fully compiles and adheres to these constraints.
                \s""".formatted(header(), classCode(), errorLog());
    }
}
