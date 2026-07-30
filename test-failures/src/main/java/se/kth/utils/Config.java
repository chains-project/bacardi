package se.kth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public static Path getTmpDirPath() {
        Path tmpPath = Paths.get(".tmp").toAbsolutePath();
        FileUtils.ensureDirectoryExists(tmpPath);
        return tmpPath;
    }

    public static Path relativeToTmpDir(String other) {
        return getTmpDirPath().resolve(other);
    }

    public static Path getBumpDir() {
        Path currentDir = Paths.get("").toAbsolutePath();
        Path bumpRelativePath = Path.of("bump", "data", "benchmark");
        return currentDir.resolveSibling(bumpRelativePath);
    }

    public static Path getResourcesDir() {
        return Path.of("test-failures", "src", "main", "resources").toAbsolutePath();
    }
}
