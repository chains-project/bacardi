package se.kth.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.Util.FileUtils;

import java.nio.file.Path;
import java.util.Arrays;

public class ProjectExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ProjectExtractor.class);

    private final DockerBuild dockerBuild;
    private final Path outputDir;

    public ProjectExtractor(DockerBuild dockerBuild, Path outputDir) {
        this.dockerBuild = dockerBuild;
        this.outputDir = outputDir;
        FileUtils.ensureDirectoryExists(outputDir);
    }

    public Path extract(String imageId) {
        String containerId = this.dockerBuild.startSpinningContainer(imageId);
        String outputSubPath = Arrays.stream(imageId.split("/")).toList().getLast();
        Path outputPath = outputDir.resolve(outputSubPath);
        Path containerOutputDir = dockerBuild.copyProjectFromContainer(containerId, "project", outputPath);
        if (containerOutputDir == null) {
            logger.warn("Failed to extract project from {} to local files {}", imageId, outputPath);
        } else {
            logger.info("Successfully extracted project from {} to local files {}", imageId, outputPath);
        }
        dockerBuild.removeContainer(containerId);
        return containerOutputDir;
    }
}
