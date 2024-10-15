package se.kth.injector;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.model.v4.MavenStaxWriter;
import se.kth.DockerBuild;
import se.kth.model.MavenModel;
import se.kth.utils.Config;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class PomFileLocator {

    private static DockerBuild dockerBuild = new DockerBuild(false);

    public static List<Path> getPomFilePaths(String containerId) {
        String commandOutput = dockerBuild.executeInContainer(containerId, "find", "/", "-type", "f", "-name",
                "pom" + ".xml");

        String[] paths = commandOutput.split("\n");
        return Stream.of(paths)
                .map(Path::of)
                .toList();
    }

    public static Model getModel(String containerId, Path pomFilePath) {
        String commandOutput = dockerBuild.executeInContainer(containerId, "cat", pomFilePath.toString());

        try {
            MavenStaxReader reader = new MavenStaxReader();
            return reader.read(new ByteArrayInputStream(commandOutput.getBytes()));
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<MavenModel> getAllModels(String containerId, List<Path> pomFilePaths) {
        return pomFilePaths.stream()
                .map(path -> new MavenModel(path, getModel(containerId, path)))
                .toList();
    }

    public static MavenModel getParentPom(List<MavenModel> models) {
        return models.stream().min(Comparator.comparingInt(value -> value.getFilePath().getNameCount()))
                .orElse(null);
    }

    public static String writeCustomPomFile(Model model, String fileName) {
        MavenStaxWriter writer = new MavenStaxWriter();

        Path tmpDir = Config.getTmpDirPath();
        File customPomFile = tmpDir.resolve(fileName).toFile();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(customPomFile);
            writer.write(fileOutputStream, model);
            fileOutputStream.close();
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(e);
        }

        return customPomFile.getAbsolutePath();
    }

}
