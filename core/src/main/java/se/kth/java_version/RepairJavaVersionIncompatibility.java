package se.kth.java_version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.Result;
import se.kth.models.FailureCategory;
import se.kth.models.JavaVersionIncompatibility;
import se.kth.models.JavaVersionInfo;
import se.kth.models.YamlInfo;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for detecting and repairing Java version incompatibilities in project configurations.
 */
public class RepairJavaVersionIncompatibility {

    private final JavaVersionInfo javaVersionInfo;
    private final Path clientCode;
    private final Logger log = LoggerFactory.getLogger(RepairJavaVersionIncompatibility.class);

    /**
     * Constructor that initializes the repair process with the provided Java version info and client code.
     *
     * @param javaVersionInfo Information about the Java versions used in the project.
     * @param clientCode Path to the client code.
     */
    public RepairJavaVersionIncompatibility(JavaVersionInfo javaVersionInfo, Path clientCode) {
        this.javaVersionInfo = javaVersionInfo;
        this.clientCode = clientCode;
    }

    /**
     * Attempts to repair Java version incompatibility by creating a Docker image, reproducing the failure,
     * and logging the Java versions found in the YAML files of the project.
     */
    public void repair() {
        DockerBuild dockerBuild = new DockerBuild();
        JavaVersionIncompatibility incompatibility = javaVersionInfo.getIncompatibility();

        try {
            // Create a base image for the breaking update with the project code
            String dockerImageName = dockerBuild.createBaseImageForBreakingUpdate(clientCode, javaVersionInfo.getIncompatibility().mapVersions(incompatibility.wrongVersion()));

            // Reproduce the breaking update in the new Java version
            Result result = dockerBuild.reproduce(dockerImageName, FailureCategory.JAVA_VERSION_FAILURE, clientCode.getFileName().toString());

            // Log the Java versions found in YAML workflow files
            List<YamlInfo> javaVersions = javaVersionInfo.getJavaInWorkflowFiles();
            javaVersions.forEach(yamlInfo -> {
                log.info("Yaml file: {}", yamlInfo.getYamlFile());
                log.info("Line: {}", yamlInfo.getLine());
                log.info("Java versions: {}", yamlInfo.getJavaVersions());
            });

        } catch (IOException e) {
            log.error("Error creating base image for breaking update.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates all Java versions found in the YAML configuration files within the project to the specified new version.
     *
     * @param projectPath Path to the project folder.
     * @param newVersion New Java version to be updated to.
     */
    public void updateJavaVersions(String projectPath, int newVersion) {
        File projectFolder = new File(projectPath);
        updateJavaVersionsInFolder(projectFolder, newVersion);
    }

    /**
     * Recursively updates Java versions in all files within the specified folder.
     *
     * @param folder Folder containing the project files.
     * @param newVersion New Java version to be updated to.
     */
    private void updateJavaVersionsInFolder(File folder, int newVersion) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                updateJavaVersionsInFolder(file, newVersion);
            } else if (file.getName().endsWith(".yml") || file.getName().contains(".yaml")) {
                updateJavaVersionsInFile(file, newVersion);
            }
        }
    }

    /**
     * Updates the Java versions within a single file by replacing the current versions with the new one,
     * while maintaining versions that are greater or equal to the new version.
     *
     * @param file File where Java versions need to be updated.
     * @param newVersion New Java version to update to.
     */
    private void updateJavaVersionsInFile(File file, int newVersion) {
        List<String> fileContent = new ArrayList<>();
        boolean fileModified = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String updatedLine = replaceJavaVersionsInLine(line, newVersion);
                if (!updatedLine.equals(line)) {
                    fileModified = true;
                }
                fileContent.add(updatedLine);
            }

        } catch (IOException e) {
            log.error("Failed to read file {}: {}", file.getAbsolutePath(), e.getMessage());
        }

        // If the file was modified, overwrite it
        if (fileModified) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : fileContent) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                log.error("Failed to write to file {}: {}", file.getAbsolutePath(), e.getMessage());
            }
        }
    }

    /**
     * Replaces Java versions in a line with the new version, keeping the format of the original version string.
     *
     * @param line Line containing Java versions.
     * @param newVersion New Java version to replace in the line.
     * @return Updated line with the new version.
     */
    private String replaceJavaVersionsInLine(String line, int newVersion) {
        // Patterns to search for in the YAML file to identify Java versions
        Pattern[] patterns = new Pattern[]{
                Pattern.compile("java:\\s*\\[\\s*'?(\\d+)'?(?:\\s*,\\s*'?(\\d+)'?)*\\s*]"),  // Formats like '8', '11'
                Pattern.compile("java:\\s*\\[\\s*(\\d+)(?:\\s*,\\s*(\\d+))*\\s*]"),  // List without quotes
                Pattern.compile("java:\\s*\\[\\s*'?(\\d+)'?\\s*,?\\s*'?\\s*]"),  // Single number case
                Pattern.compile("java_version:\\s*\\[\\s*(\\d+)(?:\\s*,\\s*(\\d+))*\\s*]"),  // Format like 8, 11 (without quotes)
                Pattern.compile("java-version:\\s*(\\d+)"),  // Format without a list
                Pattern.compile("java-version:\\s*'?(\\d+)'?"), // Format without a list with or without quotes
                Pattern.compile("java-version:\\s*\\[\\s*(\\d+)(?:\\s*,\\s*(\\d+))*\\s*]")  // List format without quotes
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                // Extract Java versions from the current line
                List<Integer> versions = extractJavaVersions(line);

                // Filter versions, keeping only those greater than or equal to the new version
                List<Integer> filteredVersions = filterJavaVersions(versions, newVersion);

                // If the pattern matches "java-version" (individual or list)
                if (pattern.pattern().contains("java-version")) {
                    if (filteredVersions.isEmpty()) {
                        return line; // No valid versions, return the original line
                    }
                    // Replace only the version, maintaining the format
                    if (pattern.pattern().contains("\\[")) {  // If it's a list
                        String newVersionString = formatVersionList(filteredVersions, false);
                        line = matcher.replaceFirst("java-version: [" + newVersionString + "]");
                    } else {
                        line = matcher.replaceFirst("java-version: " + filteredVersions.getFirst());
                    }
                } else if (pattern.pattern().contains("java_version")) {
                    // Case without quotes for java_version
                    String newVersionString = formatVersionList(filteredVersions, false);
                    line = matcher.replaceFirst("java_version: [" + newVersionString + "]");
                } else if (pattern.pattern().contains("'?(\\d+)'?")) {
                    // Case with or without single quotes in a list of java
                    String newVersionString = formatVersionList(filteredVersions, true);
                    line = matcher.replaceFirst("java: [" + newVersionString + "]");
                }
            }
        }

        return line;
    }

    /**
     * Formats a list of versions into a string, optionally adding quotes around each version.
     *
     * @param versions List of Java versions.
     * @param useQuotes Whether to wrap each version in quotes.
     * @return Formatted version list as a string.
     */
    private String formatVersionList(List<Integer> versions, boolean useQuotes) {
        StringBuilder versionString = new StringBuilder();
        for (int i = 0; i < versions.size(); i++) {
            if (useQuotes) {
                versionString.append("'").append(versions.get(i)).append("'");
            } else {
                versionString.append(versions.get(i));
            }
            if (i < versions.size() - 1) {
                versionString.append(", ");
            }
        }
        return versionString.toString();
    }

    /**
     * Extracts Java versions from a line of text.
     *
     * @param line The line of text containing Java versions.
     * @return A list of extracted Java versions.
     */
    private List<Integer> extractJavaVersions(String line) {
        List<Integer> versions = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            versions.add(Integer.parseInt(matcher.group(1)));
        }

        return versions;
    }

    /**
     * Filters Java versions, removing versions lower than the specified new version.
     *
     * @param versions List of Java versions.
     * @param newVersion The new Java version to keep or add.
     * @return A filtered list of Java versions.
     */
    private List<Integer> filterJavaVersions(List<Integer> versions, int newVersion) {
        List<Integer> filteredVersions = new ArrayList<>();
        for (int version : versions) {
            if (version >= newVersion) {
                filteredVersions.add(version);
            }
        }
        // Add the new version if it's not in the list
        if (!filteredVersions.contains(newVersion)) {
            filteredVersions.add(newVersion);
        }
        return filteredVersions;
    }
}
