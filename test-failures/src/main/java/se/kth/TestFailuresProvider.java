package se.kth;

import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;
import se.kth.Util.JsonUtils;

import java.io.File;
import java.util.List;

import static se.kth.Util.FileUtils.getFilesInDirectory;

public class TestFailuresProvider {
    public static List<BreakingUpdate> getTestFailuresFromResources(String directory) {
        List<File> files = getFilesInDirectory(directory);
        return files.stream()
                .map(File::toPath)
                .map(e -> JsonUtils.readFromFile(e, BreakingUpdate.class))
                .filter(breakingUpdate -> breakingUpdate.failureCategory == FailureCategory.TEST_FAILURE)
                .toList();
    }
}
