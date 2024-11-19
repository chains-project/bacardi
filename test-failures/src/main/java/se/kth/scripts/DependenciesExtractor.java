package se.kth.scripts;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;
import se.kth.utils.Config;

import java.nio.file.Path;
import java.util.List;

public class DependenciesExtractor {

    public static void main(String[] args) {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        String outputDirectory = "/home/leonard/tmp-output";
        String dockerOutputDirectory = "/app";
        List<BreakingUpdate> testFailureUpdates =
                BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(filePath,
                        FailureCategory.TEST_FAILURE);

        DockerClient dockerClient = Config.getDockerClient();

        Volume volume = new Volume(dockerOutputDirectory);
        Bind bind = new Bind(outputDirectory, volume, AccessMode.rw);

        for (BreakingUpdate update : testFailureUpdates) {
            extractSingleContainer(update, dockerOutputDirectory, dockerClient, bind);
        }
    }

    private static void extractSingleContainer(BreakingUpdate update, String dockerOutputDirectory,
                                               DockerClient dockerClient, Bind bind) {
        try {
            String imageId = update.preCommitReproductionCommand.replace("docker run ", "");
            String outputTag = getOutputFileTagFromImageId(imageId);
            Path dockerOutputPath = Path.of(dockerOutputDirectory, outputTag + ".log");

            String command =
                    "mvn dependency:tree -DoutputType=dot -DoutputFile=%s -DappendOutput=true -am".formatted(dockerOutputPath.toAbsolutePath().toString());
            String[] entrypoint = new String[]{"/bin/sh", "-c", command};

            dockerClient.pullImageCmd(imageId).exec(new PullImageResultCallback()).awaitCompletion();

            CreateContainerResponse container = dockerClient
                    .createContainerCmd(imageId)
                    .withHostConfig(new HostConfig().withBinds(bind))
                    .withEntrypoint(entrypoint)
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String getOutputFileTagFromImageId(String imageId) {
        String[] split = imageId.split("/");
        return split[split.length - 1];
    }
}
