package Util;

import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;
import se.kth.utils.JsonUtils;

import java.io.File;
import java.util.List;

import static se.kth.utils.FileUtils.getFilesInDirectory;


public class BreakingUpdateProvider {


    public static List<BreakingUpdate> getBreakingUpdatesFromResourcesByCategory(String directory, FailureCategory category) {
        List<File> files = getFilesInDirectory(directory);
        return files.stream()
                .map(File::toPath)
                .map(e -> JsonUtils.readFromFile(e, BreakingUpdate.class))
                .filter(breakingUpdate -> breakingUpdate.failureCategory == category)
                .toList();
    }
}
