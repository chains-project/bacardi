package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.JavaVersionIncompatibility;
import se.kth.models.JavaVersionInfo;
import se.kth.models.YamlInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionInformation {

    private static final Logger log = LoggerFactory.getLogger(JavaVersionInformation.class);
    // Start line of the error message to look for in the log file
    String startLine = "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin";

    // End line of the error message to look for in the log file
    String endLine = "[ERROR] -> [Help 1]";

    private File logFilePath;


    public JavaVersionInformation(File logFilePath) {
        this.logFilePath = logFilePath;
    }

    /**
     * Parse the log file and extract the error messages.
     *
     * @param logFilePath The path to the log file.
     * @return A set of error messages.
     * @throws IOException If an I/O error occurs.
     */
    public Set<String> parseErrors(String logFilePath) throws IOException {
        Set<String> errors = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(logFilePath));
        StringBuilder currentError = null;
        String line;

        // Define the regular expression pattern to find error lines
        Pattern errorPattern = Pattern.compile("\\[ERROR\\] /[\\w\\-/]+\\.java");
        boolean parseStart = false;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(endLine)) {
                break;
            }
            if (line.startsWith(startLine)) {
                parseStart = true;
            }
            if (parseStart) {
                Matcher matcher = errorPattern.matcher(line);
                if (line.startsWith("[ERROR]") && matcher.find()) {
                    if (currentError != null) {
                        errors.add(currentError.toString().trim());
                    }
                    currentError = new StringBuilder();
                    currentError.append(line);
                } else if (currentError != null && !line.trim().isEmpty() && line.contains("  ")) {
                    // Line with more than one space
                    currentError.append(System.lineSeparator()).append(line);
                } else {
                    // Line that doesn't follow the pattern
                    if (currentError != null) {
                        errors.add(currentError.toString().trim());
                        currentError = null;
                    }
                }
            }
        }

        if (currentError != null) {
            errors.add(currentError.toString().trim());
        }

        reader.close();
        return errors;
    }

    /**
     * Extract version incompatibility errors from the set of error messages.
     *
     * @param errors A set of error messages.
     * @return A map of JavaVersionIncompatibility to a set of error messages.
     */
    public Map<JavaVersionIncompatibility, Set<String>> extractVersionErrors(Set<String> errors) {
        Map<JavaVersionIncompatibility, Set<String>> versionErrors = new HashMap<>();

        // Define the regular expression pattern to find version incompatibility errors
        Pattern versionPattern = Pattern.compile("class file has wrong version (\\d+\\.\\d+), should be (\\d+\\.\\d+)");

        for (String error : errors) {
            Matcher versionMatcher = versionPattern.matcher(error);
            while (versionMatcher.find()) {
                JavaVersionIncompatibility j = new JavaVersionIncompatibility(versionMatcher.group(1), versionMatcher.group(2), error);

                if (versionErrors.containsKey(j)) {
                    versionErrors.get(j).add(error);
                } else {
                    Set<String> errorSet = new HashSet<>();
                    errorSet.add(error);
                    versionErrors.put(j, errorSet);
                }
            }
        }
        return versionErrors;
    }

    /**
     * Analyzes the log file and client path to gather Java version information and incompatibility errors.
     *
     * @param logFilePath The path to the log file.
     * @param clientPath  The path to the client directory.
     * @return A JavaVersionInfo object containing the analysis results.
     */
    public JavaVersionInfo analyse(String logFilePath, String clientPath) {

        // Create an instance of JavaVersionFinder to find Java versions in the client path
        JavaVersionFinder javaVersionFinder = new JavaVersionFinder();

        // Find Java versions in the client path
        List<YamlInfo> javaVersions = javaVersionFinder.findJavaVersions(clientPath);

        // Create a new JavaVersionInfo object to store the analysis results
        JavaVersionInfo javaVersionFailure = new JavaVersionInfo();

        try {
            // Parse errors from the log file
            Set<String> errors = parseErrors(logFilePath);
            // Extract version incompatibility errors from the parsed errors
            Map<JavaVersionIncompatibility, Set<String>> versionFailures = extractVersionErrors(errors);

            // Set the Java versions found in workflow files
            javaVersionFailure.setJavaInWorkflowFiles(javaVersions);
            // Set the version incompatibility errors
            javaVersionFailure.setDiffVersionErrors(versionFailures);
            // Set the error messages
            javaVersionFailure.setErrorMessages(errors);
            // Set the incompatibility version
            javaVersionFailure.setIncompatibilityVersion();

        } catch (IOException e) {
            // Log an error message and rethrow the exception as a RuntimeException
            log.error("Error parsing log file", e);
            throw new RuntimeException(e);
        }
        // Return the JavaVersionInfo object containing the analysis results
        return javaVersionFailure;
    }
}
