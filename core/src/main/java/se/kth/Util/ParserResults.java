package se.kth.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ParserResults {

    static Logger log = LoggerFactory.getLogger(ParserResults.class);

    public static boolean searchPatternInFile(String filePath, String pattern) {
        try {
            List<String> lines = Files.readAllLines(Path.of(filePath));
            for (String line : lines) {
                if (line.contains(pattern)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.error("Error reading file", e);
        }
        return false;
    }

}
