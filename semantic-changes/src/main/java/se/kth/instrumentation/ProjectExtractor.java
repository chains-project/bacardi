package se.kth.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ApptainerBuild;
import se.kth.Util.FileUtils;

import java.nio.file.Path;
import java.util.List;

public class ProjectExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ProjectExtractor.class);

    private final ApptainerBuild containerBuild;
    private final Path outputBasePath;
    private final List<String> binds;
    private final String targetMethod;

    public ProjectExtractor(ApptainerBuild containerBuild, Path outputDir, List<String> binds, String targetMethod) {
        this.containerBuild = containerBuild;
        this.outputBasePath = outputDir;
        this.binds = binds;
        this.targetMethod = targetMethod;
        FileUtils.ensureDirectoryExists(outputDir);
    }

    public Path extract(String imageName) {
        String[] entryPoint = String.format("mvn test -l output.log -DargLine=\"-javaagent:/instrumentation/semantic-agent-1" +
                ".0-SNAPSHOT.jar=%s\"", targetMethod).split(" ");

        String containerId;
        try {
            containerBuild.ensureBaseMavenImageExists(imageName);
            containerId = containerBuild.startSpinningContainer(imageName, binds);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage());
            throw new RuntimeException(e);
        }
        containerBuild.executeInContainer(containerId, entryPoint);
        String outputDir = imageName.split("/")[2];
        Path outputPath = outputBasePath.resolve(outputDir);
        Path containerOutputDir = containerBuild.copyProjectFromContainer(containerId, "project", outputPath);
        if (containerOutputDir == null) {
            logger.warn("Failed to extract project from {} to local files {}", containerId, outputPath);
        } else {
            logger.info("Successfully extracted project from {} to local files {}", containerId, outputPath);
        }
        containerBuild.removeContainer(containerId);
        return containerOutputDir;
    }
}
