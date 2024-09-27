package se.kth.injector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import se.kth.TestFailuresProvider;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class PomFileLocator {

    private static DockerClient dockerClient = Config.getDockerClient();

    public static void main(String[] args) throws InterruptedException, IOException {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        List<BreakingUpdate> breakingUpdates = TestFailuresProvider.getTestFailuresFromResources(filePath);

        for (BreakingUpdate update : breakingUpdates) {
            String imageId = update.getPreImageId();

            String command = "";
            String[] entrypoint = new String[]{"/bin/sh", "-c", command};

            dockerClient.pullImageCmd(imageId).exec(new PullImageResultCallback()).awaitCompletion();

            CreateContainerResponse container = dockerClient.createContainerCmd(imageId)
                    .withCmd("sh", "-c", "while :; do sleep 1; done") // TODO not sure if really needed
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();
            String containerId = container.getId();

            ExecCreateCmdResponse execResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd("find", "/", "-type", "f", "-name", "pom.xml")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            dockerClient.execStartCmd(execResponse.getId()).exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    if (item.getStreamType() == StreamType.STDOUT || item.getStreamType() == StreamType.STDERR) {
                        try {
                            outputStream.write(item.getPayload());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).awaitCompletion();

            String commandOutput = outputStream.toString("UTF-8");
            String[] paths = commandOutput.split("\n");
            System.out.println("Command output: \n" + commandOutput);
        }
    }
}
