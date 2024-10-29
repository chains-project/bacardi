package se.kth.injector;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import se.kth.DockerBuild;
import se.kth.PipelineComponent;
import se.kth.Util.FileUtils;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestListenerInjector extends PipelineComponent {
    private final Path containerOutputDirectory;
    private final Path testOutputDirectory;
    private final Path pomsDirectory;

    public TestListenerInjector(Path testOutputDirectory, Path containerOutputDirectory, Path pomsDirectory) {
        this.testOutputDirectory = testOutputDirectory;
        this.containerOutputDirectory = containerOutputDirectory;
        this.pomsDirectory = pomsDirectory;
    }

    @Override
    public void ensureOutputDirsExist() {
        FileUtils.ensureDirectoryExists(containerOutputDirectory);
        FileUtils.ensureDirectoryExists(testOutputDirectory);
        FileUtils.ensureDirectoryExists(this.pomsDirectory);
    }

    @Override
    public void execute(BreakingUpdate breakingUpdate) {
        String commitId = breakingUpdate.breakingCommit;
        String imageId = breakingUpdate.getPreImageId();

        try {
            DockerBuild dockerBuild = new DockerBuild(false);
            dockerBuild.ensureBaseMavenImageExists(imageId);

            Path subPath = this.testOutputDirectory.resolve(commitId);
            Path potentialOutput = Config.getTmpDirPath().resolve(subPath);
            if (Files.exists(potentialOutput)) {
                return;
            }

            MountsBuilder mountsBuilder = new MountsBuilder(imageId)
                    .withMountsForModifiedPomFiles()
                    .withMetaInfMounts()
                    .withListenerMounts()
                    .withOutputMount(this.testOutputDirectory, commitId);

            List<Mount> mounts = mountsBuilder.build();

            HostConfig config = HostConfig.newHostConfig()
                    .withMounts(mounts);

            Path modifiedRootPom = mountsBuilder.getRootModifiedPomFile();

            String containerId = dockerBuild.startSpinningContainer(imageId, config);

            String testOutput = dockerBuild.executeInContainer(containerId, "mvn", "-l", "test-output.log", "test");

            Path containerOutput = this.containerOutputDirectory.resolve(commitId);
            Path copyPath = dockerBuild.copyProjectFromContainer(containerId,
                    modifiedRootPom.getParent().toString(),
                    containerOutput.toAbsolutePath());

            dockerBuild.removeContainer(containerId);

            System.out.println("done");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
