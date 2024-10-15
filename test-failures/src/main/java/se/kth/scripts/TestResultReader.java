package se.kth.scripts;

import org.junit.platform.engine.TestExecutionResult;
import se.kth.listener.CustomExecutionListener;
import se.kth.utils.FileUtils;
import se.kth.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestResultReader {
    public static void main(String[] args) throws IOException {
        Path resultsJsonPath = Path.of("test-failures", "src", "main", "resources", "results.json");

        Map<String, List<String>> results = JsonUtils.readFromFile(resultsJsonPath.toFile().toPath(), Map.class);


        Path directory = Path.of(".tmp2", "output").toAbsolutePath();

        List<Path> nonEmptyDirs = Files.walk(directory)
                .filter(path -> {
                    try {
                        return Files.isDirectory(path) && Files.list(path).findFirst().isPresent();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Path> emptyDirs = Files.walk(directory)
                .filter(path -> {
                    try {
                        return Files.isDirectory(path) && Files.list(path).findFirst().isEmpty();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Path> all = Files.walk(directory)
                .filter(Files::isDirectory)
                .toList();

        List<String> emptyJunit5 = emptyDirs.stream()
                .map(path -> path.getFileName().toString().replace("-pre", ""))
                .filter(s -> results.get("junit5").contains(s))
                .collect(Collectors.toUnmodifiableList());


        List<String> realMigrations = emptyDirs.stream()
                .map(path -> path.getFileName().toString().replace("-pre", ""))
                .filter(s -> results.get("junit4").contains(s) && !results.get("junit5").contains(s))
                .collect(Collectors.toUnmodifiableList());

        List<String> unrealMigrations = emptyDirs.stream()
                .map(path -> path.getFileName().toString().replace("-pre", ""))
                .filter(s -> results.get("junit4").contains(s) && results.get("junit5").contains(s))
                .collect(Collectors.toUnmodifiableList());
        int total = 0;
        int failed = 0;


        for (Path path : nonEmptyDirs) {

            for (File file : FileUtils.getFilesInDirectory(path.toString())) {
                CustomExecutionListener.TestResult testResult =
                        (CustomExecutionListener.TestResult) FileUtils.readFromBinary(file.getPath());

                total++;
                if (testResult.status != TestExecutionResult.Status.SUCCESSFUL) {
                    System.out.println(testResult);
                    failed++;
                }
                System.out.println(testResult);
            }

        }
        System.out.println("Finished");
    }
}
