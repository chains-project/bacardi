package se.kth;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import se.kth.injector.MountsBuilder;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        String filePath = "/home/leohus/bump/data/benchmark/";
        List<BreakingUpdate> breakingUpdates = TestFailuresProvider.getTestFailuresFromResources(filePath);

        try (ExecutorService executorService = Executors.newFixedThreadPool(30)) {

            for (BreakingUpdate update : breakingUpdates) {
                String imageId = update.getPreImageId();
                executorService.submit(() -> {
                    try {
                        executeForImageId(imageId);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }
        System.out.println("finished");
    }

    private static void executeForImageId(String imageId) throws InterruptedException {
        String hash = imageId.split(":")[1].replace("-pre", "");

        DockerBuild dockerBuild = new DockerBuild(false);
        dockerBuild.ensureBaseMavenImageExists(imageId);

        Path subPath = Path.of("output", hash);
        Path potentialOutput = Config.getTmpDirPath().resolve(subPath);
        if (Files.exists(potentialOutput)) {
            return;
        }

        try {
            MountsBuilder mountsBuilder = new MountsBuilder(imageId)
                    .withMountsForModifiedPomFiles()
                    .withMetaInfMounts()
                    .withListenerMounts()
                    .withOutputMount(hash);

            List<Mount> mounts = mountsBuilder.build();

            HostConfig config = HostConfig.newHostConfig()
                    .withMounts(mounts);

            Path modifiedRootPom = mountsBuilder.getRootModifiedPomFile();
          
            String containerId = dockerBuild.startSpinningContainer(imageId, config);

            String testOutput = dockerBuild.executeInContainer(containerId, "mvn", "-l", "test-output.log", "test");

            Path containerOutput = Path.of("container", hash);
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