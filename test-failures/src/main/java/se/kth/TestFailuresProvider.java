package se.kth;

import se.kth.model.BreakingUpdate;
import se.kth.utils.JsonUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestFailuresProvider {
    public static List<BreakingUpdate> getTestFailuresFromResources(String directory) {
        List<File> files = getFilesInDirectory(directory);
        return files.stream()
                .map(File::toPath)
                .map(e -> JsonUtils.readFromFile(e, BreakingUpdate.class))
                .filter(breakingUpdate -> breakingUpdate.failureCategory == FailureCategory.TEST_FAILURE)
                .toList();
    }

    private static List<File> getFilesInDirectory(String directory) {
        return Stream.of(new File(directory).listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toList());
    }
}
