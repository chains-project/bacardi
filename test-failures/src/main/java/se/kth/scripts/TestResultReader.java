package se.kth.scripts;

import org.junit.platform.engine.TestExecutionResult.Status;
import se.kth.listener.CustomExecutionListener;
import se.kth.utils.FileUtils;
import se.kth.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestResultReader {
    public static void main(String[] args) throws IOException {
        Path directory = Path.of(".tmp", "output").toAbsolutePath();

        List<Path> all = Files.walk(directory)
                .filter(Files::isDirectory)
                .toList();

        List<Path> collectableDirs = all.stream()
                .filter(path -> {
                    try {
                        return Files.list(path).findFirst().isPresent();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Path> nonCollectableDirs = all.stream()
                .filter(path -> {
                    try {
                        return Files.list(path).findFirst().isEmpty();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<String> collectableCommitIds = collectableDirs.stream()
                .map(path -> path.getFileName().toString())
                .toList();
        List<String> nonCollectableCommitIds = nonCollectableDirs.stream()
                .map(path -> path.getFileName().toString())
                .toList();

        Map<String, List<String>> collectableNonCollectableProjects = new HashMap<>();
        collectableNonCollectableProjects.put("collectable", collectableCommitIds);
        collectableNonCollectableProjects.put("nonCollectable", nonCollectableCommitIds);
        Path collectableOutputPath = Path.of("test-failures", "src", "main", "resources", "collectable.json");
        JsonUtils.writeToFile(collectableOutputPath, collectableNonCollectableProjects);

        Map<String, List<String>> unsuccessfulTestCasesResult = new HashMap<>();
        for (Path path : collectableDirs) {
            List<String> unsuccessfulTestCases = new LinkedList<>();
            for (File file : FileUtils.getFilesInDirectory(path.toString())) {
                CustomExecutionListener.TestResult testResult =
                        (CustomExecutionListener.TestResult) FileUtils.readFromBinary(file.getPath());
                if (testResult.status != Status.SUCCESSFUL) {
                    unsuccessfulTestCases.add(testResult.testIdentifier);
                }
            }
            if (!unsuccessfulTestCases.isEmpty()) {
                unsuccessfulTestCasesResult.put(path.getFileName().toString(), unsuccessfulTestCases);
            }
        }

        Path unsuccessfulTestCasesOutputPath = Path.of("test-failures", "src", "main", "resources", "unsuccessfulTestCases.json");
        JsonUtils.writeToFile(unsuccessfulTestCasesOutputPath, unsuccessfulTestCasesResult);
    }
}
