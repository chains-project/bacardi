package se.kth.injector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import se.kth.model.MavenModel;
import se.kth.utils.Config;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PomFileLocator {

    private static DockerClient dockerClient = Config.getDockerClient();

    public static List<String> getPomFileLocations(String containerId) {
        ExecCreateCmdResponse findResponse = dockerClient.execCreateCmd(containerId)
                .withCmd("find", "/", "-type", "f", "-name", "pom.xml")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        ByteArrayOutputStream findOutputStream = new ByteArrayOutputStream();

        try {
            dockerClient.execStartCmd(findResponse.getId()).exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    if (item.getStreamType() == StreamType.STDOUT || item.getStreamType() == StreamType.STDERR) {
                        try {
                            findOutputStream.write(item.getPayload());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            return List.of();
        }

        String commandOutput = findOutputStream.toString(StandardCharsets.UTF_8);

        String[] paths = commandOutput.split("\n");
        return List.of(paths);
    }

    public static Model getModel(String containerId, String pomFilePath) {
        ExecCreateCmdResponse catResponse = dockerClient.execCreateCmd(containerId)
                .withCmd("cat", pomFilePath)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        ByteArrayOutputStream listOutputStream = new ByteArrayOutputStream();

        try {
            dockerClient.execStartCmd(catResponse.getId()).exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    if (item.getStreamType() == StreamType.STDOUT || item.getStreamType() == StreamType.STDERR) {
                        try {
                            listOutputStream.write(item.getPayload());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            return null;
        }

        try {
            MavenStaxReader reader = new MavenStaxReader();
            return reader.read(new ByteArrayInputStream(listOutputStream.toByteArray()));
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<MavenModel> getAllModels(String containerId, List<String> pomFilePaths) {
        return pomFilePaths.stream()
                .map(path -> new MavenModel(path, getModel(containerId, path)))
                .toList();
    }

}
