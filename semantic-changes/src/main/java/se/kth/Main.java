package se.kth;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import picocli.CommandLine;
import se.kth.instrumentation.ProjectExtractor;
import se.kth.util.Config;

import java.nio.file.Path;
import java.util.List;

public class Main implements Runnable {

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
        DockerBuild dockerBuild = new DockerBuild(false);
        Path extractedProjectsOutputDir = Config.getTmpDirPath().resolve("instrumentation-output");
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMounts(List.of(new Mount()
                        .withSource(semanticAgentPath.toString())
                        .withTarget("/instrumentation/semantic-agent-1.0-SNAPSHOT.jar")
                        .withType(MountType.BIND)));
        ProjectExtractor projectExtractor = new ProjectExtractor(dockerBuild, extractedProjectsOutputDir, hostConfig);

        String[] entryPoint = String.format("mvn test -DargLine=\"-javaagent:/instrumentation/semantic-agent-1" +
                ".0-SNAPSHOT.jar=%s\"", methodName).split(" ");
        Path preOutputPath = projectExtractor.extract(oldVersionImage, entryPoint);
        Path postOutputPath = projectExtractor.extract(newVersionImage, entryPoint);
        System.out.println("Saved to path: " + preOutputPath);
        System.out.println("Saved to path: " + postOutputPath);
    }


    @Override
    public void run() {

    }
}