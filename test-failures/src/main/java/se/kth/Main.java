package se.kth;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import se.kth.injector.MountsBuilder;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        List<BreakingUpdate> breakingUpdates = TestFailuresProvider.getTestFailuresFromResources(filePath);

        for (BreakingUpdate update : breakingUpdates) {
            String imageId = update.getPreImageId();
            String hash = imageId.split(":")[1].replace("-pre", "");

            Path subPath = Path.of("output", imageId.split(":")[1]);
            Path potentialOutput = Config.getTmpDirPath().resolve(subPath);
            if (Files.exists(potentialOutput)) {
                continue;
            }

            try {

                MountsBuilder mountsBuilder = new MountsBuilder(imageId)
                        .withMountsForModifiedPomFiles()
                        .withMetaInfMounts()
                        .withListenerMounts()
                        .withOutputMount();

                List<Mount> mounts = mountsBuilder.build();

                HostConfig config = HostConfig.newHostConfig()
                        .withMounts(mounts);

                Path modifiedRootPom = mountsBuilder.getRootModifiedPomFile();

                DockerBuild dockerBuild = new DockerBuild();
                String containerId = dockerBuild.startSpinningContainer(imageId, config);

                String projectOutputPath = modifiedRootPom.getParent().toString();

                String testOutput = dockerBuild.executeInContainer(containerId, "mvn", "-l", "test-output.log", "test");

                Path containerOutput = Path.of("container", imageId.split(":")[1]);
                Path copyPath = dockerBuild.copyProjectFromContainer(containerId,
                        modifiedRootPom.getParent().toString(),
                        Config.getTmpDirPath().resolve(containerOutput).toAbsolutePath());

                dockerBuild.removeContainer(containerId);

                System.out.println("done");
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}