package se.kth.util;

import se.kth.Util.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    public static Path getTmpDirPath() {
        Path tmpPath = Paths.get(".tmp").toAbsolutePath();
        FileUtils.ensureDirectoryExists(tmpPath);
        return tmpPath;
    }
}
