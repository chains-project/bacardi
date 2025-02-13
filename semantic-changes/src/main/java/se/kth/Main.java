package se.kth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.api.model.HostConfig;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;
import se.kth.Util.FileUtils;
import se.kth.comparison.ValueComparator;
import se.kth.instrumentation.HostConfigBuilder;
import se.kth.instrumentation.ProjectExtractor;
import se.kth.matching.Difference;
import se.kth.matching.Matcher;
import se.kth.model.MethodInvocation;
import se.kth.util.Config;
import se.kth.util.ResultsWriter;

import java.nio.file.Path;
import java.util.List;

public class Main {

    @CommandLine.Option(
            names = {"-o", "--oldVersion"},
            description = "Name of the docker image of the old version",
            required = true)
    static String oldVersionImage = "ghcr.io/chains-project/breaking-updates:jsoup-1.7.1";

    @CommandLine.Option(
            names = {"-n", "--newVersion"},
            description = "Name of the docker image of the new version",
            required = true)
    static String newVersionImage = "ghcr.io/chains-project/breaking-updates:jsoup-1.7.3";

    @CommandLine.Option(
            names = {"-a", "--agentPath"},
            description = "Path to the jar of the semantic agent",
            required = true)
    static Path semanticAgentPath = Path.of("/home", "leonard", "code", "java", "semantic-agent", "target", "semantic" +
            "-agent-1.0-SNAPSHOT.jar");

    @CommandLine.Option(
            names = {"-m", "--methodName"},
            description = "Fully qualified name (\"fqn.your.TargetClass:targetMethod\") of the method to instrument",
            required = true)
    static String methodName = "org.jsoup.nodes.Element:prepend(java.lang.String)";

    @CommandLine.Option(
            names = {"--outputPath"},
            description = "Path to the directory where the output should be stored",
            required = false)
    static Path outputPath = Config.getTmpDirPath().resolve("differences");


    public static void main(String[] args) {
        run("1", oldVersionImage, newVersionImage, methodName);
    }


    public static boolean run(String id, String preImageName, String postImageName, String targetMethod) {
        DockerBuild dockerBuild = new DockerBuild(false);
        Path extractedProjectsOutputDir = Config.getTmpDirPath().resolve("instrumentation-output").resolve(id);
        FileUtils.ensureDirectoryExists(extractedProjectsOutputDir.getParent());
        HostConfigBuilder configBuilder = new HostConfigBuilder(semanticAgentPath.toString());
        HostConfig hostConfig = configBuilder.build();
        ProjectExtractor projectExtractor = new ProjectExtractor(dockerBuild, extractedProjectsOutputDir, hostConfig,
                targetMethod);

        Path preOutputPath = projectExtractor.extract(preImageName);
        Path postOutputPath = projectExtractor.extract(postImageName);

        List<Pair<MethodInvocation, MethodInvocation>> pairs = new Matcher().readAndMatch(preOutputPath,
                postOutputPath);

        try {
            List<List<Difference>> differences = ValueComparator.compareAllReturnValues(pairs);
            if (differences.stream()
                    .anyMatch(differences1 -> !differences1.isEmpty())) {
                System.out.println("Differences found:");
                differences.forEach(differences1 -> differences1.forEach(System.out::println));
                ResultsWriter.saveDifferences(differences, outputPath.resolve(id + ".json"));
                return true;
            } else {
                System.out.println("No Differences found");
                return false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}