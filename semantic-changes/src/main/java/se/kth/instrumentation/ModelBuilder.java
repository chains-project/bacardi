package se.kth.instrumentation;

import spoon.Launcher;
import spoon.reflect.CtModel;

import java.nio.file.Path;

public class ModelBuilder {

    private final Path projectRootDirectory;

    public ModelBuilder(Path projectRootDirectory) {
        this.projectRootDirectory = projectRootDirectory;
    }

    public CtModel buildModel() {
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectRootDirectory.toString());
        return launcher.buildModel();
    }
}
