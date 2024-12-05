package se.kth.direct_failures;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.model.DependencyTree;
import se.kth.model.SetupPipeline;
import se.kth.models.FailureCategory;
import se.kth.parse.ParseMavenDependencyTree;
import se.kth.parse.PomModel;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class RepairDirectFailures {

    private static Logger log = LoggerFactory.getLogger(RepairDirectFailures.class);
    private final DockerBuild dockerBuild;
    private SetupPipeline setupPipeline;


    public RepairDirectFailures(DockerBuild dockerBuild, SetupPipeline setupPipeline) {
        this.dockerBuild = dockerBuild;
        this.setupPipeline = setupPipeline;
    }


    public Path generateDependencyTree(Path treeFile, String dockerImage, String projectPath) {

        String[] command = {"/bin/sh", "-c", "mvn dependency:3.7.0:tree -DoutputType=json -Dverbose=true -DoutputFile=tree.json"};

        String containerId = dockerBuild.startSpinningContainer(dockerImage);

        String outputExecution = dockerBuild.executeInContainer(containerId, command);
        if (outputExecution.contains("BUILD FAILURE")) {
            log.error("Error while generating dependency tree");
            return null;
        }
        return dockerBuild.copyFromContainer(containerId, projectPath.concat("/tree.json"), treeFile);
    }

    public List<DependencyTree> identifyConflicts(Path mavenDependencyTreeFile) {
        List<DependencyTree> dependencyTrees = checkDependencyResolutionConflict(mavenDependencyTreeFile.toString());
        if (dependencyTrees.isEmpty()) {
            log.info("No conflicts found");
        } else {
            log.info("Conflicts found");
            dependencyTrees.forEach(dependencyTree -> log.info(dependencyTree.toString()));
        }

        return dependencyTrees;
    }


    public String reproduce() {

        try {
            dockerBuild.copyFolderToDockerImage(setupPipeline.getDockerImage(), setupPipeline.getClientFolder().toString());

            Path logFilePath = setupPipeline.getLogFilePath().getParent().resolve("output.log");

            dockerBuild.reproduce(setupPipeline.getDockerImage(), FailureCategory.COMPILATION_FAILURE, setupPipeline.getClientFolder(), logFilePath);

            setupPipeline.setLogFilePath(logFilePath);

            return setupPipeline.getDockerImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void modifyPomFile(Path pomFile, DependencyTree dependencyTree) throws IOException {
        //
        PomModel pomModel = new PomModel(pomFile);
        File pom = new File(pomFile.toString());
        File backupPomFile = new File(pomFile.toString().replace(".xml", "_backup.xml"));
        Files.copy(pom.toPath(), backupPomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            Model newModel = pomModel.modifyDependency(dependencyTree);

            //create new pom file
            OutputStream outputStream = Files.newOutputStream(pomFile);
            MavenStaxWriter writer = new MavenStaxWriter();
            writer.write(outputStream, newModel);
        } catch (IOException | XMLStreamException e) {
            log.error("Error modifying the pom file", e);
            throw new RuntimeException(e);
        }
    }


    public List<DependencyTree> checkDependencyResolutionConflict(String dependencyTreePath) {

        ParseMavenDependencyTree parseMavenDependencyTree = new ParseMavenDependencyTree(dependencyTreePath);
        List<DependencyTree> dependencies = parseMavenDependencyTree.parseDependencies();
        List<DependencyTree> children = parseMavenDependencyTree.findChildrenByName(dependencies,
                this.setupPipeline.getBreakingUpdate().updatedDependency.dependencyGroupID,
                this.setupPipeline.getBreakingUpdate().updatedDependency.dependencyArtifactID);
        return parseMavenDependencyTree.hasSameOrLowerLevelDependencies(dependencies, children);
    }
}
