package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneratePrompt {
    private static final Logger log = LoggerFactory.getLogger(GeneratePrompt.class);

    public static String callPythonScript(String scriptPath, String[] args) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);
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
                System.out.println(output);
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
        String pattern = "```java(.*?)```";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(input);

        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            log.error("Error extracting content from the model response");
            return null;
        }
    }


}