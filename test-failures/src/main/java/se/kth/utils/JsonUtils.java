package se.kth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T readFromFile(Path path, Class<T> jsonType) {
        try {
            return mapper.readValue(Files.readString(path), jsonType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeToFile(Path path, Object obj) {
        try {
            mapper.writeValue(path.toFile(), obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
