package se.kth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;
import se.kth.comparison.ValueComparator;
import se.kth.instrumentation.ProjectExtractor;
import se.kth.matching.Difference;
import se.kth.matching.Matcher;
import se.kth.model.MethodInvocation;
import se.kth.util.Config;

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
            description = "Fully qualified name (\"fqn.your.TargetClass#targetMethod\") of the method to instrument",
            required = true)
    static String methodName = "org.jsoup.nodes.Element#select";

    @CommandLine.Option(
            names = {"--outputPath"},
            description = "Path to the directory where the output should be stored",
            required = false)
    static Path outputPath = Path.of("");


    public static void main(String[] args) {
        run(oldVersionImage, newVersionImage, methodName);
    }


    public static void run(String preImageName, String postImageName, String targetMethod) {
        DockerBuild dockerBuild = new DockerBuild(false);
        Path extractedProjectsOutputDir = Config.getTmpDirPath().resolve("instrumentation-output");
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMounts(List.of(new Mount()
                        .withSource(semanticAgentPath.toString())
                        .withTarget("/instrumentation/semantic-agent-1.0-SNAPSHOT.jar")
                        .withType(MountType.BIND)));
        ProjectExtractor projectExtractor = new ProjectExtractor(dockerBuild, extractedProjectsOutputDir, hostConfig);

        String[] entryPoint = String.format("mvn test -DargLine=\"-javaagent:/instrumentation/semantic-agent-1" +
                ".0-SNAPSHOT.jar=%s\"", targetMethod).split(" ");
        Path preOutputPath = projectExtractor.extract(preImageName, entryPoint);
        Path postOutputPath = projectExtractor.extract(postImageName, entryPoint);

        List<Pair<MethodInvocation, MethodInvocation>> pairs = new Matcher().readAndMatch(preOutputPath,
                postOutputPath);

        try {
            List<List<Difference>> differences = ValueComparator.compareAll(pairs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}