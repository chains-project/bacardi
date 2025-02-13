package se.kth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.FileUtils;
import se.kth.matching.Difference;
import se.kth.model.BenchmarkResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ResultsWriter {

    private static final Logger logger = LoggerFactory.getLogger(ResultsWriter.class);

    public static void saveDifferences(List<List<Difference>> differences, Path path) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            FileUtils.ensureDirectoryExists(path.getParent());
            mapper.writeValue(new File(path.toString()), differences);
            logger.info("Results written to: {}", path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveBenchmarkResult(List<BenchmarkResult> result, Path path) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            FileUtils.ensureDirectoryExists(path.getParent());
            mapper.writeValue(new File(path.toString()), result);
            logger.info("Results written to: {}", path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
