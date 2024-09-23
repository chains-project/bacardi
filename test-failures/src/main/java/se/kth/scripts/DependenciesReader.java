package se.kth.scripts;

import se.kth.model.Dependency;
import se.kth.utils.FileUtils;
import se.kth.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class DependenciesReader {

    public static void main(String[] args) {
        String directory = "/home/leonard/tmp-output/";
        Path outputPath = Paths.get(directory, "results.json");
        List<File> allFiles = FileUtils.getFilesInDirectory(directory);
        Map<String, Set<Dependency>> projects = new HashMap<>();

        for (File file : allFiles) {
            Set<Dependency> dependencies = parseDependencies(file);
            projects.put(trim(file.getName()), dependencies);
        }

        Map<String, Set<Dependency>> junit3Projects = projects.entrySet().stream()
                .filter(tuple -> containsArtifact("junit", "3", tuple.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Set<Dependency>> junit4Projects = projects.entrySet().stream()
                .filter(tuple -> containsArtifact("junit", "4", tuple.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Set<Dependency>> junit5Projects = projects.entrySet().stream()
                .filter(tuple -> containsGroupId("org.junit.jupiter", tuple.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Set<Dependency>> junit4andJupiterProjects = projects.entrySet().stream()
                .filter(tuple -> containsArtifact("junit", "4", tuple.getValue()))
                .filter(tuple -> containsGroupId("org.junit.jupiter", tuple.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Set<Dependency>> testNGProjects = projects.entrySet().stream()
                .filter(tuple -> containsGroupId("org.testng", tuple.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Set<Dependency>> others = projects.entrySet().stream()
                .filter(tuple -> !junit3Projects.containsValue(tuple.getValue())
                        && !junit4Projects.containsValue(tuple.getValue())
                        && !junit5Projects.containsValue(tuple.getValue())
                        && !testNGProjects.containsValue(tuple.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        saveToFile(outputPath, junit3Projects, junit4Projects, junit5Projects, testNGProjects, others);

    }

    private static boolean containsArtifact(String artifactName, String versionPrefix, Set<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (dependency.artifactId.equals(artifactName) && dependency.version.startsWith(versionPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsGroupId(String groupId, Set<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (dependency.groupId.startsWith(groupId)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Dependency> parseDependencies(File file) {
        Set<Dependency> dependencies = new HashSet<>();
        //parse the tree file and create a tree
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line.contains("->")) {
                    String[] parts = line.split("->");
                    String child = parts[1].trim();
                    Dependency childDependency = readDependency(child);
                    dependencies.add(childDependency);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dependencies;
    }


    /**
     * Read a dependency from a line
     *
     * @param line the line
     * @return the dependency
     */
    private static Dependency readDependency(String line) {
        String[] parts = line.split(":");
        String scope = parts.length < 5 ? "" : parts[4];
        return new Dependency(
                parts[0].split("\"")[1],
                parts[1],
                parts[3].split("\"")[0],
                parts[2],
                scope);
    }

    private static String trim(String input) {
        String regex = "(?<=:)[a-f0-9]{40}(?=-)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return input;
        }
    }

    private static void saveToFile(Path path, Map<String, Set<Dependency>> junit3Projects, Map<String,
            Set<Dependency>> junit4Projects, Map<String, Set<Dependency>> junit5Projects, Map<String,
            Set<Dependency>> testNGProjects, Map<String,
            Set<Dependency>> others) {

        Map<String, Set<String>> output = new HashMap<>();
        output.put("junit3", junit3Projects.keySet());
        output.put("junit4", junit4Projects.keySet());
        output.put("junit5", junit5Projects.keySet());
        output.put("testng", testNGProjects.keySet());
        output.put("others", others.keySet());

        JsonUtils.writeToFile(path, output);
    }
}
