package se.kth.utils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    public static List<File> getFilesInDirectory(String directory) {
        return Stream.of(new File(directory).listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toList());
    }
}
