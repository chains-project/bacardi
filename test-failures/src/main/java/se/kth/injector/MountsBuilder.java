package se.kth.injector;

import org.apache.maven.api.model.Model;
import se.kth.ApptainerBuild;
import se.kth.model.MavenModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static se.kth.injector.PomFileLocator.getAllModels;
import static se.kth.injector.PomFileLocator.getPomFilePaths;

/**
 * Builds the list of {@code hostPath:containerPath[:ro]} bind-mount specs Apptainer's
 * {@code --bind} flag expects, replacing Docker's {@code Mount}/{@code HostConfig} builder
 * API (Apptainer applies binds per-exec rather than at container-creation time).
 */
public class MountsBuilder {

    private String imageId;
    private String containerId;
    private final List<MavenModel> models;
    private List<String> binds = new LinkedList<>();
    private List<Path> modifiedPomFiles = new LinkedList<>();
    private final ApptainerBuild containerBuild = new ApptainerBuild(false);


    public MountsBuilder(String imageId) throws InterruptedException {
        this.imageId = imageId;
        this.containerBuild.ensureBaseMavenImageExists(imageId);

        this.containerId = this.containerBuild.startSpinningContainer(imageId);

        List<Path> paths = getPomFilePaths(containerId);
        this.models = getAllModels(containerId, paths);
    }

    public MountsBuilder withMountsForModifiedPomFiles(Path pomsDirectory) {
        List<Path> paths = getPomFilePaths(containerId);
        List<MavenModel> models = getAllModels(containerId, paths);

        List<String> pomFileBinds = models.stream()
                .map(mavenModel -> createBindForModifiedPomFile(mavenModel, pomsDirectory))
                .toList();
        this.binds.addAll(pomFileBinds);

        return this;
    }

    public MountsBuilder withMetaInfMounts() {
        List<String> metaInfBinds = models.stream()
                .map(this::createBindMountForMetaInf)
                .toList();
        this.binds.addAll(metaInfBinds);

        return this;
    }

    public MountsBuilder withListenerMounts() {
        List<String> listenerBinds = models.stream()
                .map(this::createBindMountForListener)
                .toList();
        this.binds.addAll(listenerBinds);

        return this;
    }

    public MountsBuilder withOutputMount(Path baseDir, String outputDir) {
        String outputBind = this.createBindMountForOutput(baseDir, outputDir);
        this.binds.add(outputBind);

        return this;
    }

    public List<String> build() {
        this.containerBuild.removeContainer(this.containerId);
        return this.binds;
    }

    public Path getRootModifiedPomFile() {
        return this.modifiedPomFiles.stream().min(Comparator.comparingInt(Path::getNameCount))
                .orElse(null);
    }

    private String createBindForModifiedPomFile(MavenModel mavenModel, Path pomsDirectory) {
        Model model = mavenModel.getModel();
        model.getDependencies();

        TestFrameworkHandler testFrameworkHandler = new TestFrameworkHandler(model);
        Model newModel = testFrameworkHandler.migrate();

        String modifiedPomFileName = "pom-" + newModel.hashCode() + ".xml";
        String modifiedPomMountFileName = "pom.xml";

        String modifiedPomFileAbsolutePath = PomFileLocator.writeCustomPomFile(newModel, modifiedPomFileName, pomsDirectory);
        Path modifiedPomFileContainerPath = mavenModel.getFilePath().resolveSibling(modifiedPomMountFileName);
        this.modifiedPomFiles.add(modifiedPomFileContainerPath);

        return "%s:%s".formatted(modifiedPomFileAbsolutePath, modifiedPomFileContainerPath);
    }

    private String createBindMountForMetaInf(MavenModel mavenModel) {
        Path metaInfPath = Path.of("src", "test", "resources", "META-INF", "services", "org.junit.platform.launcher" +
                ".TestExecutionListener");
        Path containerPath = mavenModel.getFilePath().resolveSibling(metaInfPath);
        Path metaInfSourcePath = Path.of("test-failures", "src", "main", "resources", "org.junit.platform.launcher" +
                ".TestExecutionListener").toAbsolutePath();

        return "%s:%s:ro".formatted(metaInfSourcePath, containerPath);
    }

    private String createBindMountForListener(MavenModel mavenModel) {
        Path listenerPath = Path.of("src", "test", "java", "se", "kth", "listener", "CustomExecutionListener.java");
        Path listenerContainerPath = mavenModel.getFilePath().resolveSibling(listenerPath);
        Path listenerSourcePath = Path.of("test-failures", "src", "main", "java", "se", "kth", "listener",
                "CustomExecutionListener.java").toAbsolutePath();

        return "%s:%s:ro".formatted(listenerSourcePath, listenerContainerPath);
    }

    private String createBindMountForOutput(Path baseDir, String outputDir) {
        Path containerPath = Path.of("/bacardi-output");
        Path localOutputPath = baseDir.resolve(outputDir);
        try {
            Files.createDirectory(localOutputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "%s:%s".formatted(localOutputPath, containerPath);
    }
}
