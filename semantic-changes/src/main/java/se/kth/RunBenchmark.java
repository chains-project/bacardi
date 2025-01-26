package se.kth;

import com.fasterxml.jackson.databind.type.CollectionType;
import se.kth.Util.JsonUtils;
import se.kth.util.BenchmarkCase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RunBenchmark {

    private static final Path benchmarkFile = Paths.get("semantic-changes/src/main/resources/semb/dataset.json");

    public static void main(String[] args) {
        System.out.println(benchmarkFile.toAbsolutePath());

        CollectionType jsonType = JsonUtils.getTypeFactory().constructCollectionType(List.class, BenchmarkCase.class);
        List<BenchmarkCase> benchmarkCases = JsonUtils.readFromFile(benchmarkFile, jsonType);
        for (BenchmarkCase benchmarkCase : benchmarkCases) {
            Main.run(benchmarkCase.getPreVersionImageName(), benchmarkCase.getPostVersionImageName(),
                    benchmarkCase.getTargetMethod());
        }
    }
}
