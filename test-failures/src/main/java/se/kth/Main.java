package se.kth;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import se.kth.model.BreakingUpdate;
import se.kth.model.MavenModel;
import se.kth.utils.Config;

import java.util.List;

import static se.kth.injector.PomFileLocator.getAllModels;
import static se.kth.injector.PomFileLocator.getPomFileLocations;

public class Main {

    private static DockerClient dockerClient = Config.getDockerClient();

    public static void main(String[] args) throws InterruptedException {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        List<BreakingUpdate> breakingUpdates = TestFailuresProvider.getTestFailuresFromResources(filePath);

        for (BreakingUpdate update : breakingUpdates) {
            String imageId = update.getPreImageId();

            dockerClient.pullImageCmd(imageId).exec(new PullImageResultCallback()).awaitCompletion();

            CreateContainerResponse container = dockerClient.createContainerCmd(imageId)
                    .withCmd("sh", "-c", "sleep 60")
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();
            String containerId = container.getId();


            List<String> paths = getPomFileLocations(containerId);
            List<MavenModel> models = getAllModels(containerId, paths);

            System.out.println(models);
        }
    }
}