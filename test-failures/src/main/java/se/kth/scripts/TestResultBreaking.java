package se.kth.scripts;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import se.kth.DockerBuild;
import se.kth.TestFailuresProvider;
import se.kth.injector.MountsBuilder;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;
import se.kth.Util.JsonUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestResultBreaking {
    public static void main(String[] args) throws InterruptedException {
        Path bumpDir = Config.getBumpDir();
        Path resourcesDir = Config.getResourcesDir();
        Path collectablePath = resourcesDir.resolve("collectable.json");
        Path unsuccessfulTestCasesPath = resourcesDir.resolve("unsuccessfulTestCases.json");

        List<BreakingUpdate> breakingUpdates = TestFailuresProvider.getTestFailuresFromResources(bumpDir.toString());
        List<String> collectable = (List<String>) JsonUtils.readFromFile(collectablePath, Map.class).get("collectable");
        Map<String, List<String>> unsuccessfulTestCases = JsonUtils.readFromFile(unsuccessfulTestCasesPath, Map.class);

        List<BreakingUpdate> collectableBreakingUpdates = breakingUpdates.stream()
                .filter(breakingUpdate -> collectable.contains(breakingUpdate.breakingCommit))
                .toList();

        try (ExecutorService executorService = Executors.newFixedThreadPool(30)) {
            for (BreakingUpdate breakingUpdate : collectableBreakingUpdates) {
                executorService.submit(() -> {
                    try {
                        executeBreakingUpdate(breakingUpdate);
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

    private static void executeBreakingUpdate(BreakingUpdate breakingUpdate) throws InterruptedException {
        String imageId = breakingUpdate.getBreakingImageId();
        String hash = breakingUpdate.breakingCommit;

        DockerBuild dockerBuild = new DockerBuild(false);
        dockerBuild.ensureBaseMavenImageExists(imageId);

        try {
            MountsBuilder mountsBuilder = new MountsBuilder(imageId)
                    .withMountsForModifiedPomFiles()
                    .withMetaInfMounts()
                    .withListenerMounts()
                    .withOutputMount("breaking-output", hash);

            List<Mount> mounts = mountsBuilder.build();
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMounts(mounts);

            Path modifiedRootPom = mountsBuilder.getRootModifiedPomFile();
            String containerId = dockerBuild.startSpinningContainer(imageId, hostConfig);

            dockerBuild.executeInContainer(containerId, "mvn", "-l", "test-output.log", "test");

            Path containerOutput = Path.of("breaking-container", hash);
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
