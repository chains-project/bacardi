package se.kth.instrumentation;

import com.github.dockerjava.api.model.HostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.Util.FileUtils;

import java.nio.file.Path;

public class ProjectExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ProjectExtractor.class);

    private final DockerBuild dockerBuild;
    private final Path outputBasePath;
    private final HostConfig hostConfig;

    public ProjectExtractor(DockerBuild dockerBuild, Path outputDir, HostConfig hostConfig) {
        this.dockerBuild = dockerBuild;
        this.outputBasePath = outputDir;
        this.hostConfig = hostConfig;
        FileUtils.ensureDirectoryExists(outputDir);
    }

    public Path extract(String imageName, String[] entryPoint) {
        String containerId = dockerBuild.startSpinningContainer(imageName, hostConfig);
        dockerBuild.executeInContainer(containerId, entryPoint);
        String outputDir = imageName.split("/")[2];
        Path outputPath = outputBasePath.resolve(outputDir);
        Path containerOutputDir = dockerBuild.copyProjectFromContainer(containerId, "project", outputPath);
        if (containerOutputDir == null) {
            logger.warn("Failed to extract project from {} to local files {}", containerId, outputPath);
        } else {
            logger.info("Successfully extracted project from {} to local files {}", containerId, outputPath);
        }
        dockerBuild.removeContainer(containerId);
        return containerOutputDir;
    }
}
