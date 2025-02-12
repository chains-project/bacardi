package se.kth.instrumentation;

import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;

import java.nio.file.Path;

public class ModelBuilder {

    private final Path projectRootDirectory;
    private final Launcher launcher;

    public ModelBuilder(Path projectRootDirectory) {
        this.projectRootDirectory = projectRootDirectory;
        this.launcher = new MavenLauncher(projectRootDirectory.toString(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
        this.launcher.setSourceOutputDirectory("/home/leonard/code/java/bacardi/.tmp/instrumentation-sources/breaking-updates:jsoup-1.7.1/spoon");
        this.launcher.getEnvironment().setAutoImports(true);
    }

    public CtModel buildModel() {
        return launcher.buildModel();
    }

    public Launcher getLauncher() {
        return this.launcher;
    }
}
