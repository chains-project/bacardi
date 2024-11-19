package se.kth.japicmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.PipelineComponent;
import se.kth.Util.FileUtils;
import se.kth.model.BreakingUpdate;
import se.kth.model.UpdatedDependency;

import java.nio.file.Files;
import java.nio.file.Path;

public class UpdatedDependencyExtractor extends PipelineComponent {

    private static final Logger logger = LoggerFactory.getLogger(UpdatedDependencyExtractor.class);

    private final Path preOutputPath;
    private final Path breakingOutputPath;
    private final DockerBuild dockerBuild;

    public UpdatedDependencyExtractor(Path preOutputPath, Path breakingOutputPath) {
        this.preOutputPath = preOutputPath;
        this.breakingOutputPath = breakingOutputPath;
        this.dockerBuild = new DockerBuild(false);
    }

    @Override
    public void ensureOutputDirsExist() {
        FileUtils.ensureDirectoryExists(preOutputPath);
        FileUtils.ensureDirectoryExists(breakingOutputPath);
    }

    @Override
    public void execute(BreakingUpdate breakingUpdate) {
        String commitId = breakingUpdate.breakingCommit;
        UpdatedDependency updatedDependency = breakingUpdate.updatedDependency;

        Path containerPreOutputPath = this.preOutputPath.resolve(commitId);
        if (!Files.exists(containerPreOutputPath)) {
            this.extractDependencyJar(breakingUpdate.getPreImageId(), updatedDependency,
                    updatedDependency.previousVersion, containerPreOutputPath);
        }

        Path containerBreakingOutputPath = this.breakingOutputPath.resolve(commitId);
        if (!Files.exists(containerBreakingOutputPath)) {
            this.extractDependencyJar(breakingUpdate.getBreakingImageId(), updatedDependency,
                    updatedDependency.newVersion, containerBreakingOutputPath);
        }
    }

    private void extractDependencyJar(String imageId, UpdatedDependency updatedDependency, String version,
                                      Path outputPath) {
        try {
            this.dockerBuild.ensureBaseMavenImageExists(imageId);
            String containerId = this.dockerBuild.startSpinningContainer(imageId);

            String dependencyName = updatedDependency.dependencyArtifactID;
            String[] command = new String[]{"find", "/root/.m2/", "-type", "f", "-name",
                    dependencyName + "-" + version + ".jar"};
            String output = this.dockerBuild.executeInContainer(containerId, command);
            String[] outputLines = output.split("\n");
            String dependencyPath = outputLines[0];
            if (outputLines.length != 1) {
                logger.warn("More than one path found for dependency " + dependencyName + " at: \n" + output);
            }

            Path fromContainerM2 = Path.of(dependencyPath);
            this.dockerBuild.copyM2FolderToLocalPath(containerId, fromContainerM2, outputPath);

            this.dockerBuild.removeContainer(containerId);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
