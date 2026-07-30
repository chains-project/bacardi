package se.kth.injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ApptainerBuild;
import se.kth.PipelineComponent;
import se.kth.Util.FileUtils;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestWithListenerRunner extends PipelineComponent {

    private static final Logger logger = LoggerFactory.getLogger(TestWithListenerRunner.class);

    private final Path containerOutputDirectory;
    private final Path testOutputDirectory;
    private final Path pomsDirectory;
    private final boolean runBreaking;

    public TestWithListenerRunner(Path testOutputDirectory, Path containerOutputDirectory, Path pomsDirectory,
                                  boolean runBreaking) {
        this.testOutputDirectory = testOutputDirectory;
        this.containerOutputDirectory = containerOutputDirectory;
        this.pomsDirectory = pomsDirectory;
        this.runBreaking = runBreaking;
    }

    @Override
    public void ensureOutputDirsExist() {
        FileUtils.ensureDirectoryExists(this.containerOutputDirectory);
        FileUtils.ensureDirectoryExists(this.testOutputDirectory);
        FileUtils.ensureDirectoryExists(this.pomsDirectory);
    }

    @Override
    public void execute(BreakingUpdate breakingUpdate) {
        String commitId = breakingUpdate.breakingCommit;
        String imageId = this.runBreaking ? breakingUpdate.getBreakingImageId() : breakingUpdate.getPreImageId();

        try {
            ApptainerBuild containerBuild = new ApptainerBuild(false);
            containerBuild.ensureBaseMavenImageExists(imageId);

            Path subPath = this.testOutputDirectory.resolve(commitId);
            Path potentialOutput = Config.getTmpDirPath().resolve(subPath);
            if (Files.exists(potentialOutput)) {
                return;
            }

            MountsBuilder mountsBuilder = new MountsBuilder(imageId)
                    .withMountsForModifiedPomFiles(pomsDirectory)
                    .withMetaInfMounts()
                    .withListenerMounts()
                    .withOutputMount(this.testOutputDirectory, commitId);

            List<String> binds = mountsBuilder.build();

            String containerId = containerBuild.startSpinningContainer(imageId, binds);
            String testOutput = containerBuild.executeInContainer(containerId, "mvn", "-l", "test-output.log", "test");

            Path modifiedRootPom = mountsBuilder.getRootModifiedPomFile();
            Path containerOutput = this.containerOutputDirectory.resolve(commitId);
            Path copyPath = containerBuild.copyProjectFromContainer(containerId,
                    modifiedRootPom.getParent().toString(),
                    containerOutput.toAbsolutePath());

            containerBuild.removeContainer(containerId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
