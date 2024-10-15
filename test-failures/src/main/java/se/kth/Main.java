package se.kth;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import se.kth.injector.TestFrameworkHandler;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        List<BreakingUpdate> breakingUpdates = TestFailuresProvider.getTestFailuresFromResources(filePath);

        for (BreakingUpdate update : breakingUpdates) {
            String imageId = update.getPreImageId();

            TestFrameworkHandler testFrameworkHandler = new TestFrameworkHandler(imageId)
                    .withMountsForModifiedPomFiles()
                    .withMetaInfMounts()
                    .withListenerMounts()
                    .withOutputMount();

            List<Mount> mounts = testFrameworkHandler.getMounts();

            HostConfig config = HostConfig.newHostConfig()
                    .withMounts(mounts);

            Path modifiedRootPom = testFrameworkHandler.getRootModifiedPomFile();

            DockerBuild dockerBuild = new DockerBuild(false);
            String containerId = dockerBuild.startSpinningContainer(imageId, config);
            String commandOutput = dockerBuild.executeInContainer(containerId, "tree");

            String projectOutputPath = modifiedRootPom.getParent().toString();
            Path copyPath = dockerBuild.copyProjectFromContainer(containerId, modifiedRootPom.getParent().toString(),
                    Config.getTmpDirPath().resolve("container").toAbsolutePath());

            String testOutput = dockerBuild.executeInContainer(containerId, "mvn", "-f", modifiedRootPom.toString(),
                    "test");

            dockerBuild.removeContainer(containerId);

            System.out.println("done");
        }
    }
}