package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.YamlInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionFinder {

    Logger log = LoggerFactory.getLogger(JavaVersionFinder.class);

    public List<YamlInfo> findJavaVersions(String projectPath) {

        List<YamlInfo> javaVersions = new ArrayList<>();
        File projectFolder = new File(projectPath);
        if (!projectFolder.exists() || !projectFolder.isDirectory()) {
            //
            log.error("Folder not exist {} or is not a directory.", projectPath);
            return null;
        }
        searchJavaVersionsInFolder(projectFolder, javaVersions);
        return javaVersions;
    }

    private void searchJavaVersionsInFolder(File folder, List<YamlInfo> javaVersions) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                searchJavaVersionsInFolder(file, javaVersions);
            } else if (file.getName().endsWith(".yml") || file.getName().contains((".yaml"))) {
                YamlInfo versions = getJavaVersionsFromFile(file);
                if (versions != null && !versions.getJavaVersions().isEmpty()) {
                    javaVersions.add(versions);
                }
            }
        }
    }

    private YamlInfo getJavaVersionsFromFile(File file) {

        YamlInfo yamlInfo = null;
        List<Integer> versions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Patter to search example "java: [ '8', '17', '18' ]"
                Pattern pattern = Pattern.compile("java:\\s*\\[\\s*'(\\d+)'(?:\\s*,\\s*'\\d+')*\\s*]");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String match = matcher.group(0); // Get the complete string of the pattern
                    Pattern numberPattern = Pattern.compile("'(\\d+)'"); // Pattern to extract the numbers
                    Matcher numberMatcher = numberPattern.matcher(match);
                    while (numberMatcher.find()) {
                        int number = Integer.parseInt(numberMatcher.group(1)); // Get the number
                        if (!versions.contains(number)) {
                            versions.add(number);
                        }
                    }
                }

                pattern = Pattern.compile("java:\\s*\\[\\s*(\\d+)(?:\\s*,\\s*(\\d+))*\\s*]");
                matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String match = matcher.group(0); // Get the complete string of the pattern
                    Pattern numberPattern = Pattern.compile("(\\d+)"); // Pattern to extract the numbers
                    Matcher numberMatcher = numberPattern.matcher(match);
                    while (numberMatcher.find()) {
                        int number = Integer.parseInt(numberMatcher.group(1)); // Get the number
                        if (!versions.contains(number)) {
                            versions.add(number);
                        }
                    }
                }

                pattern = Pattern.compile("java_version:\\s*\\[\\s*(\\d+)(?:\\s*,\\s*(\\d+))*\\s*]");

                matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String match = matcher.group(0); // Get the complete string of the pattern
                    Pattern numberPattern = Pattern.compile("(\\d+)"); // Pattern to extract the numbers
                    Matcher numberMatcher = numberPattern.matcher(match);

                    while (numberMatcher.find()) {
                        int number = Integer.parseInt(numberMatcher.group(1)); // Get the number

                        if (!versions.contains(number)) {
                            versions.add(number);
                        }
                    }
                }

                // Pattern to search example "java-version: 8"
                Pattern pattern2 = Pattern.compile("java-version:\\s*(\\d+)");
                Matcher matcher2 = pattern2.matcher(line);
                while (matcher2.find()) {
                    if (!versions.contains(Integer.parseInt(matcher2.group(1)))) {
                        versions.add(Integer.parseInt(matcher2.group(1)));
                    }
                }

                Pattern pattern3 = Pattern.compile("java-version:\\s*'?(\\d+)'?");
                Matcher matcher3 = pattern3.matcher(line);
                while (matcher3.find()) {
                    if (!versions.contains(Integer.parseInt(matcher3.group(1)))) {
                        versions.add(Integer.parseInt(matcher3.group(1)));
                    }
                }
                Pattern pattern4 = Pattern.compile("java:\\s*\\[\\s*'?(\\d+)'?\\s*,?\\s*'?\\s*]");
                Matcher matcher4 = pattern4.matcher(line);
                while (matcher4.find()) {
                    if (!versions.contains(Integer.parseInt(matcher4.group(1)))) {
                        versions.add(Integer.parseInt(matcher4.group(1)));
                    }
                }

                Pattern pattern5 = Pattern.compile("java-version:\\s*\\[\\s*(\\d+)(?:\\s*,\\s*(\\d+))*\\s*]");
                Matcher matcher5 = pattern5.matcher(line);
                while (matcher5.find()) {
                    Pattern numberPattern = Pattern.compile("(\\d+)");
                    Matcher numberMatcher = numberPattern.matcher(matcher5.group(0));
                    while (numberMatcher.find()) {
                        int number = Integer.parseInt(numberMatcher.group(1));
                        if (!versions.contains(number)) {
                            versions.add(number);
                        }
                    }
                }

                if (!versions.isEmpty()) {
                    yamlInfo = new YamlInfo(file.getName(), line, new ArrayList<>(versions));
                    versions.clear();
                }

            }
        } catch (IOException e) {
            System.out.println("Fail to read file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
        return yamlInfo;
    }


}
