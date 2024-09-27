package se.kth.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
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
}
