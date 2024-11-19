package se.kth.extractor;

import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.utils.Config;
import se.kth.Util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestFileLoader {

    private final Path projectDirectory;

    public TestFileLoader(String breakingUpdateId) {
        Path projectRelativePath = Path.of("breaking-container", breakingUpdateId);
        this.projectDirectory = Config.getTmpDirPath().resolve(projectRelativePath);
    }

    public List<ImmutablePair<StackTraceElement, Optional<File>>> loadProjectTestFiles(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace)
                .map(stackTraceElement -> new ImmutablePair<StackTraceElement, Optional<File>>(stackTraceElement,
                        FileUtils.findFileInDirectory(this.projectDirectory,
                                stackTraceElement.getFileName())))
                .toList();
    }

}
