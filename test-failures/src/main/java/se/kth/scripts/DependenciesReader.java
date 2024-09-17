package se.kth.scripts;

import se.kth.model.Dependency;
import se.kth.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependenciesReader {

    public static void main(String[] args) {
        String directory = "/home/leonard/tmp-output/";
        List<File> allFiles = FileUtils.getFilesInDirectory(directory);
        List<Set<Dependency>> projects = new ArrayList<>();

        for (File file : allFiles) {
            Set<Dependency> dependencies = parseDependencies(file);
            projects.add(dependencies);
        }

        List<Set<Dependency>> junit3Projects = projects.stream()
                .filter(project -> containsArtifact("junit", "3", project))
                .toList();

        List<Set<Dependency>> junit4Projects = projects.stream()
                .filter(dependencies -> containsArtifact("junit", "4", dependencies))
                .toList();

        List<Set<Dependency>> junit5Projects = projects.stream()
                .filter(dependencies -> containsGroupId("org.junit.jupiter", dependencies))
                .toList();

        List<Set<Dependency>> junit4andJupiterProjects = projects.stream()
                .filter(dependencies -> containsArtifact("junit", "4", dependencies))
                .filter(dependencies -> containsGroupId("org.junit.jupiter", dependencies))
                .toList();

        List<Set<Dependency>> testNGProjects = projects.stream()
                .filter(dependencies -> containsGroupId("org.testng", dependencies))
                .toList();

        List<Set<Dependency>> others = projects.stream()
                .filter(dependencies -> !junit3Projects.contains(dependencies)
                        && !junit4Projects.contains(dependencies)
                        && !junit5Projects.contains(dependencies)
                        && !testNGProjects.contains(dependencies))
                .toList();

        System.out.println(junit4Projects);

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
}
