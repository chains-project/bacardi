package se.kth;

import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.JsonUtils;
import se.kth.model.BenchmarkResult;
import se.kth.util.ResultsWriter;
import se.kth.util.SemBUpdate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class RunBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(RunBenchmark.class);

    private static final Path benchmarkFile = Paths.get("semantic-changes/src/main/resources/semb/dataset.json");

    private static final Path resultsPath = Paths.get("semantic-changes/src/main/resources/semb/results.json");

    public static void main(String[] args) {
        CollectionType jsonType = JsonUtils.getTypeFactory().constructCollectionType(List.class, SemBUpdate.class);
        List<SemBUpdate> semBUpdates = JsonUtils.readFromFile(benchmarkFile, jsonType);
        List<BenchmarkResult> results = new LinkedList<>();
        for (SemBUpdate semBUpdate : semBUpdates) {
            logger.info("Starting update: " + semBUpdate.getId());
            boolean result = Main.run(String.valueOf(semBUpdate.getId()), semBUpdate.getPreVersionImageName(),
                    semBUpdate.getPostVersionImageName(), semBUpdate.getTargetMethod());
            results.add(new BenchmarkResult(String.valueOf(semBUpdate.getId()), semBUpdate.isSemB(),
                    semBUpdate.isGroundTruth(), result));
        }

        ResultsWriter.saveBenchmarkResult(results, resultsPath);
    }
}
