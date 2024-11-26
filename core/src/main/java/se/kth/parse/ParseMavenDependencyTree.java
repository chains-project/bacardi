package se.kth.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.DependencyTree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParseMavenDependencyTree {

    private final Logger log = LoggerFactory.getLogger(ParseMavenDependencyTree.class);

    private final String filePath;


    public ParseMavenDependencyTree(String filePath) {
        this.filePath = Objects.requireNonNull(filePath);
    }


    /**
     * Parses the Maven dependency tree from the JSON file specified by the file path.
     * Using -DoutputType=json without -Dverbose option in mvn dependency:tree command
     *
     * @return a list of DependencyTree objects representing the parsed dependencies
     */
    public List<DependencyTree> parseDependencies() {
        ObjectMapper objectMapper = new ObjectMapper();
        List<DependencyTree> dependencies = new ArrayList<>();
        try {
            DependencyTree rootDependency = objectMapper.readValue(new File(filePath), DependencyTree.class);
            populateDependencies(rootDependency, 0, null, dependencies, new int[]{1});
        } catch (IOException e) {
            log.error("Error parsing the Maven dependency tree from the file: {}", filePath, e);
        }
        return dependencies;
    }

    /**
     * Populates the dependencies list with the given dependency and its children.
     *
     * @param dependency       the root dependency to start populating from
     * @param level            the current level of the dependency in the tree
     * @param parent           the parent dependency of the current dependency
     * @param dependencies     the list to populate with dependencies
     * @param levelOnePosition an array to keep track of the position of level 1 dependencies
     */
    public void populateDependencies(DependencyTree dependency, int level, DependencyTree parent, List<DependencyTree> dependencies, int[] levelOnePosition) {
        dependency.setLevel(level);
        dependency.setParent(parent);

        // if the dependency is at level 1, set its tree position and increment the position counter
        // this information help to understand the position of the dependency in the tree and decide if we need to modify or add a new dependency
        if (level == 1) {
            dependency.setTreePosition(levelOnePosition[0]++);
        } else {
            dependency.setTreePosition(-1);
        }

        dependencies.add(dependency);
        if (dependency.getChildren() != null) {
            for (DependencyTree child : dependency.getChildren()) {
                populateDependencies(child, level + 1, dependency, dependencies, levelOnePosition);
            }
        }
    }

    /**
     * Finds the children of a {@link DependencyTree} by its groupId and artifactId.
     *
     * @param dependencies the list of {@link DependencyTree} to search through
     * @param groupId      the groupId of the dependency to find
     * @param artifactId   the artifactId of the dependency to find
     * @return a list of children {@link DependencyTree}, or an empty list if no match is found
     */
    public List<DependencyTree> findChildrenByName(List<DependencyTree> dependencies, String groupId, String artifactId) {
        for (DependencyTree dependency : dependencies) {
            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                return dependency.getChildren();
            }
        }
        return new ArrayList<>();
    }

    /**
     * Finds dependencies that have the same or lower level than the given children dependencies,
     * but with a different version.
     *
     * @param dependencies the list of all dependencies to search through
     * @param children     the list of child dependencies to compare against
     * @return a list of {@link DependencyTree} that have the same or lower level than the children, but with a different version
     */
    public List<DependencyTree> hasSameOrLowerLevelDependencies(List<DependencyTree> dependencies, List<DependencyTree> children) {

        List<DependencyTree> sameOrLowerLevelDependencies = new ArrayList<>();
        for (DependencyTree child : children) {
            for (DependencyTree dependency : dependencies) {
                if (dependency.getArtifactId().equals(child.getArtifactId())
                        && dependency.getLevel() <= child.getLevel()
                        && !dependency.getVersion().equals(child.getVersion())) {
                    dependency.setVersion(child.getVersion());
                    sameOrLowerLevelDependencies.add(dependency);
                }
            }
        }
        return sameOrLowerLevelDependencies;
    }

    /**
     * Prints all level 1 dependencies from the given list of dependencies.
     *
     * @param dependencies the list of {@link DependencyTree} to search through
     */
    public void printLevelOneDependencies(List<DependencyTree> dependencies) {
        for (DependencyTree dependency : dependencies) {
            if (dependency.getLevel() == 1) {
                System.out.println("Dependency: " + dependency.getGroupId() + ":" + dependency.getArtifactId() + " at position: " + dependency.getTreePosition());
            }
        }
    }



}