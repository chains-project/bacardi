package se.kth.Util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.SetupPipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static List<File> getFilesInDirectory(String directory) {
        return Stream.of(new File(directory).listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toList());
    }

    public static Object readFromBinary(String filePath) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filePath))) {
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<File> getDirectoriesInDirectory(String directory) {
        return Arrays.stream(new File(directory).listFiles())
                .filter(File::isDirectory)
                .toList();
    }

    public static Optional<File> findFileInDirectory(Path directory, String fileName) {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(path -> path.getFileName().toString().equals(fileName))
                    .map(Path::toFile)
                    .findFirst();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }
    }

    public static void ensureDirectoryExists(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                logger.error("Could not create directory", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static String getAbsolutePath(SetupPipeline pipeline, String relativePath) {

        String basePath = pipeline.getClientFolder().toString().split("/" + pipeline.getBreakingUpdate().project)[0];

        String absolutePath = basePath.concat(relativePath);
        // Return the absolute path as a string
        return absolutePath;
    }
}
