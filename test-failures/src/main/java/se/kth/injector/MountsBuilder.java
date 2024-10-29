package se.kth.injector;

import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import org.apache.maven.api.model.Model;
import se.kth.DockerBuild;
import se.kth.model.MavenModel;
import se.kth.utils.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static se.kth.injector.PomFileLocator.getAllModels;
import static se.kth.injector.PomFileLocator.getPomFilePaths;

public class MountsBuilder {

    private String imageId;
    private String containerId;
    private final List<MavenModel> models;
    private List<Mount> mounts = new LinkedList<>();
    private List<Path> modifiedPomFiles = new LinkedList<>();
    private final DockerBuild dockerBuild = new DockerBuild(false);


    public MountsBuilder(String imageId) throws InterruptedException {
        this.imageId = imageId;
        this.dockerBuild.ensureBaseMavenImageExists(imageId);

        this.containerId = this.dockerBuild.startSpinningContainer(imageId);

        List<Path> paths = getPomFilePaths(containerId);
        this.models = getAllModels(containerId, paths);
    }

    public MountsBuilder withMountsForModifiedPomFiles() {
        List<Path> paths = getPomFilePaths(containerId);
        List<MavenModel> models = getAllModels(containerId, paths);

        List<Mount> pomFileMounts = models.stream()
                .map(this::createBindForModifiedPomFile)
                .toList();
        this.mounts.addAll(pomFileMounts);

        return this;
    }

    public MountsBuilder withMetaInfMounts() {
        List<Mount> metaInfMounts = models.stream()
                .map(this::createBindMountForMetaInf)
                .toList();
        this.mounts.addAll(metaInfMounts);

        return this;
    }

    public MountsBuilder withListenerMounts() {
        List<Mount> listenerMounts = models.stream()
                .map(this::createBindMountForListener)
                .toList();
        this.mounts.addAll(listenerMounts);

        return this;
    }

    public MountsBuilder withOutputMount(Path baseDir, String outputDir) {
        Mount outputMount = this.createBindMountForOutput(baseDir, outputDir);
        this.mounts.add(outputMount);

        return this;
    }

    public List<Mount> build() {
        this.dockerBuild.removeContainer(this.containerId);
        return this.mounts;
    }

    public Path getRootModifiedPomFile() {
        return this.modifiedPomFiles.stream().min(Comparator.comparingInt(Path::getNameCount))
                .orElse(null);
    }

    private Mount createBindForModifiedPomFile(MavenModel mavenModel) {
        Model model = mavenModel.getModel();
        model.getDependencies();

        TestFrameworkHandler testFrameworkHandler = new TestFrameworkHandler(model);
        Model newModel = testFrameworkHandler.migrate();

        String modifiedPomFileName = "pom-" + newModel.hashCode() + ".xml";
        String modifiedPomMountFileName = "pom.xml";

        String modifiedPomFileAbsolutePath = PomFileLocator.writeCustomPomFile(newModel, modifiedPomFileName);
        Path modifiedPomFileDockerPath = mavenModel.getFilePath().resolveSibling(modifiedPomMountFileName);
        this.modifiedPomFiles.add(modifiedPomFileDockerPath);

        return new Mount()
                .withSource(modifiedPomFileAbsolutePath)
                .withTarget(modifiedPomFileDockerPath.toString())
                .withType(MountType.BIND);
    }

    private Mount createBindMountForMetaInf(MavenModel mavenModel) {
        Path metaInfPath = Path.of("src", "test", "resources", "META-INF", "services", "org.junit.platform.launcher" +
                ".TestExecutionListener");
        Path dockerPath = mavenModel.getFilePath().resolveSibling(metaInfPath);
        Path metaInfSourcePath = Path.of("test-failures", "src", "main", "resources", "org.junit.platform.launcher" +
                ".TestExecutionListener").toAbsolutePath();

        return new Mount()
                .withSource(metaInfSourcePath.toString())
                .withTarget(dockerPath.toString())
                .withReadOnly(true)
                .withType(MountType.BIND);
    }

    private Mount createBindMountForListener(MavenModel mavenModel) {
        Path listenerPath = Path.of("src", "test", "java", "se", "kth", "listener", "CustomExecutionListener.java");
        Path listenerDockerPath = mavenModel.getFilePath().resolveSibling(listenerPath);
        Path listenerSourcePath = Path.of("test-failures", "src", "main", "java", "se", "kth", "listener",
                "CustomExecutionListener.java").toAbsolutePath();

        return new Mount()
                .withSource(listenerSourcePath.toString())
                .withTarget(listenerDockerPath.toString())
                .withReadOnly(true)
                .withType(MountType.BIND);
    }

    private Mount createBindMountForOutput(Path baseDir, String outputDir) {
        Path dockerPath = Path.of("/bacardi-output");
        Path localOutputPath = baseDir.resolve(outputDir);
        try {
            Files.createDirectory(localOutputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Mount()
                .withSource(localOutputPath.toString())
                .withTarget(dockerPath.toString())
                .withType(MountType.BIND);
    }
}

