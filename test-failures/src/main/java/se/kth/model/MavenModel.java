package se.kth.model;

import org.apache.maven.api.model.Model;

import java.nio.file.Path;

@lombok.Getter
public class MavenModel {
    private final Path filePath;
    private final Model model;

    public MavenModel(Path filePath, Model model) {
        this.filePath = filePath;
        this.model = model;
    }

    public boolean dependsOn(String groupId, String artifactId) {
        return model.getDependencies().stream().anyMatch(dependency -> dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId));
    }

}
