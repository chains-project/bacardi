package se.kth.parse;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.model.v4.MavenStaxWriter;
import se.kth.model.DependencyTree;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PomModel {

    private final Model model;

    public PomModel(Path pomFilePath) {
        model = parsePomFile(pomFilePath);
    }

    public Model parsePomFile(Path pomFilePath) {
        MavenStaxReader reader = new MavenStaxReader();
        try (InputStream inputStream = Files.newInputStream(pomFilePath)) {
            return reader.read(inputStream);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public Model modifyDependency(DependencyTree dependencyTree) throws IOException, XMLStreamException {
        List<Dependency> dependencyList = new ArrayList<>(model.getDependencies());
        boolean dependencyExists = false;

        for (Dependency dependency : dependencyList) {
            if (dependency.getArtifactId().equals(dependencyTree.getArtifactId()) && dependency.getGroupId().equals(dependencyTree.getGroupId())) {
                System.out.println("Dependency exists");
                Dependency withNewVersion = dependency.withVersion(dependencyTree.getVersion());
                int index = dependencyList.indexOf(dependency);
                dependencyList.set(index, withNewVersion);
                break;
            }
        }

     return model.withDependencies(dependencyList);
    }
}
