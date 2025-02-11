package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.PromptModel;
import se.kth.model.PromptPipeline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@lombok.Getter
@lombok.Setter
public class GeneratePrompt {
    private static final Logger log = LoggerFactory.getLogger(GeneratePrompt.class);

    private PromptPipeline pipeline = PromptPipeline.BASELINE;
    private PromptModel promptModel;
    AbstractPromptTemplate promptTemplate = null;

    public GeneratePrompt() {
    }

    public GeneratePrompt(PromptPipeline pipeline, PromptModel promptModel) {
        this.pipeline = pipeline;
        this.promptModel = promptModel;
    }

    public void savePrompt(String prompt) {
        // Save the prompt to a file
    }

    public String generatePrompt() {
        log.info("Generating prompt for pipeline: {}", pipeline);

        switch (pipeline) {
            case BASELINE:
                promptTemplate = new BasePromptTemplate();
                promptTemplate.setPromptModel(promptModel);
                break;
            case BASELINE_ANTHROPIC:
                promptTemplate = new BasePromptAnthropicTemplate();
                promptTemplate.setPromptModel(promptModel);
                break;
            case BASELINE_BUGGY_LINE:
                promptTemplate = new BasePromptBuggyLineTemplate();
                promptTemplate.setPromptModel(promptModel);
                break;
            case BASELINE_ANTHROPIC_BUGGY:
                promptTemplate = new BasePromptAnthropicBuggyTemplate();
                promptTemplate.setPromptModel(promptModel);
                break;
            case BASELINE_API_DIFF:
                log.info("Baseline API diff pipeline not implemented yet");
                promptTemplate = new BasePromptApiDiffTemplate();
                promptTemplate.setPromptModel(promptModel);
                break;
            case FIX_YOU:
                promptTemplate = new FixYouPromptTemplate();
                promptTemplate.setPromptModel(promptModel);
                break;
            case ADVANCED:
                log.info("Advanced pipeline not implemented yet");
                throw new RuntimeException("Advanced pipeline not implemented yet");
            default:
                log.error("Invalid pipeline: {}", pipeline);
                throw new RuntimeException("Invalid pipeline: " + pipeline);
        }
        // Check if the prompt template is not null
        assert promptTemplate != null;
        String prompt = promptTemplate.generatePrompt();
        return prompt;
    }

    public String callPythonScript(String scriptPath, Path prompt) {

        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath, prompt.toAbsolutePath().toString());
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Read all errors from the process
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errors = new StringBuilder();

            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append(System.lineSeparator());
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Script executed successfully:");
            } else {
                System.err.println("Script failed with exit code: " + exitCode);
                System.err.println(errors.toString());
            }
        } catch (Exception e) {
            log.error("Error executing the Python script: {}", scriptPath, e);
        }
        return output.toString();
    }

    public String extractContentFromModelResponse(String input) {

        return promptTemplate.parseLLMResponce(input);
    }

    /**
     * replace the all content of one file with the response from the model
     */
    public void replaceFileContent(String sourceFilePath, String targetFilePath) {
        try {
            // Read the content of the source file
            String content = Files.readString(Path.of(sourceFilePath));

            // Write the content to the target file
            Files.writeString(Path.of(targetFilePath), content);

            log.info("Successfully replaced content of {} with content from {}", targetFilePath, sourceFilePath);
        } catch (IOException e) {
            log.error("Error replacing file content", e);
            throw new RuntimeException(e);
        }
    }

}