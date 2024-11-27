package se.kth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.util.List;

@lombok.Getter
@lombok.Setter
@EqualsAndHashCode(callSuper = false)
public class DependencyTree extends Dependency {


    @JsonProperty("children")
    private List<DependencyTree> children;
    @JsonProperty("classifier")
    private String classifier;
    @JsonProperty("optional")
    private String optional;
    private int level;
    private DependencyTree parent;
    private int treePosition;

    public DependencyTree(String groupId, String artifactId, String version, String type, String scope, List<DependencyTree> children) {
        super(groupId, artifactId, version, type, scope);
    }

    public DependencyTree(String groupId, String artifactId, String version, String type, String scope, String classifier, List<DependencyTree> children, int level, DependencyTree parent, String optional) {
        super(groupId, artifactId, version, type, scope);
        this.classifier = classifier;
        this.children = children;
        this.level = level;
        this.parent = parent;
        this.optional = optional;
    }

    public DependencyTree() {
        super();
    }


    @Override
    public String toString() {
        return "DependencyTree{" + "groupId='" + getGroupId() + '\'' + ", artifactId='" + getArtifactId() + '\'' + ", version='" + getVersion() + '\'' +
                ", type='" + getType() + '\'' + ", scope='" + getScope() + '\'' + ", classifier='" + classifier + '\'' + ", optional='" + optional + '\'' +
                ", level=" + level + ", parent=" + parent + ", treePosition=" + treePosition + '}';
    }

}
