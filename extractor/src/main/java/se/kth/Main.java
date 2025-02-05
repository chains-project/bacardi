package se.kth;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.japicmp_analyzer.JApiCmpAnalyze;
import se.kth.models.MavenErrorLog;
import se.kth.spoon.ApiMetadata;
import se.kth.spoon.Client;
import se.kth.spoon.SpoonFullyQualifiedNameExtractor;
import se.kth.spoon.SpoonUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {

        String benchmark = "/Users/frank/Documents/Work/PHD/BUMP/bump/data/benchmark/%s.json".formatted("3eb708f53f5f1ce897c3554bb73f5de5698dd171");

        File jsonFile = new File(benchmark);

        /*
         * Extract all errors
         */
        ObjectMapper mapper = new ObjectMapper().setDateFormat(new StdDateFormat());
        TypeFactory typeFactory = mapper.getTypeFactory();
        JavaType type = typeFactory.constructMapType(Map.class, String.class, Object.class);

        Map<String, Object> json = mapper.readValue(jsonFile, type);

        System.out.println(json);

        String breakingCommit = (String) json.get("breakingCommit");
        String project = (String) json.get("project");
        Map<String, Object> updatedDependency = (Map<String, Object>) json.get("updatedDependency");
        String previousVersion = (String) updatedDependency.get("previousVersion");
        String newVersion = (String) updatedDependency.get("newVersion");
        String dependencyGroupID = (String) updatedDependency.get("dependencyGroupID");
        String dependencyArtifactID = (String) updatedDependency.get("dependencyArtifactID");

        String previousJar = "%s-%s.jar".formatted(dependencyArtifactID, previousVersion);
        String newJar = "%s-%s.jar".formatted(dependencyArtifactID, newVersion);


        File logFile = new File("/Users/frank/Documents/Work/PHD/bacardi/projects/%s/%s/%s.log".formatted(breakingCommit, project, breakingCommit));

        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(logFile);

        try {
            MavenErrorLog errorLog = mavenErrorInformation.extractLineNumbersWithPaths(String.valueOf(logFile));

            Path newJarVersion = Path.of("/Users/frank/Documents/Work/PHD/bacardi/projects/%s/%s".formatted(breakingCommit, newJar));
            Path oldJarVersion = Path.of("/Users/frank/Documents/Work/PHD/bacardi/projects/%s/%s".formatted(breakingCommit, previousJar));

            ApiMetadata newApi = new ApiMetadata(newJarVersion.getFileName().toString(), newJarVersion);
            ApiMetadata oldApi = new ApiMetadata(oldJarVersion.getFileName().toString(), oldJarVersion);

            JApiCmpAnalyze japicmpAnalyzer = new JApiCmpAnalyze(oldApi, newApi);

            Client client = new Client(Path.of("/Users/frank/Documents/Work/PHD/bacardi/projects/%s/%s".formatted(breakingCommit, project)));
            client.setClasspath(List.of(oldJarVersion));
            SpoonUtilities spoonResults = new SpoonUtilities(
                    client
            );

            SpoonConstructExtractor causingConstructExtractor = new SpoonConstructExtractor(errorLog, japicmpAnalyzer, spoonResults,"pipeline");

            Map<String, Set<DetectedFileWithErrors>> fileWithErrorsMap = causingConstructExtractor.extractCausingConstructs();

            fileWithErrorsMap.forEach((key, value) -> {
                System.out.println("Key: " + key);
                value.forEach(detectedFileWithErrors -> {

                    detectedFileWithErrors.getExecutedElements().forEach(executedElement -> {
                        System.out.println("Executed element: " + executedElement);
                        System.out.println("FullyQualifiedName  " + SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(executedElement));
                    });

                    System.out.println("Detected file: " + detectedFileWithErrors.methodName);
                    detectedFileWithErrors.getApiChanges().forEach(apiChange -> {
                        System.out.println("Api change: " + apiChange.toDiffString());
                    });


                });
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}