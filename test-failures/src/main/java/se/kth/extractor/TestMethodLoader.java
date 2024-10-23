package se.kth.extractor;

import se.kth.utils.Config;
import se.kth.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestMethodLoader {

    private final StackTraceElement[] stackTrace;

    public TestMethodLoader(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    public List<File> localizeProjectTestFiles(String breakingUpdateId) {
        Path projectRelativePath = Path.of("breaking-container", breakingUpdateId);
        Path projectDir = Config.getTmpDirPath().resolve(projectRelativePath);

        return Arrays.stream(stackTrace)
                .map(stackTraceElement -> FileUtils.findFileInDirectory(projectDir, stackTraceElement.getFileName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
