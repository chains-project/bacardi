package se.kth.spoon;

import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode

public class Client {
    private final Path sourcePath;

    private List<Path> classpath = Collections.emptyList();

    public Client(Path sourcePath) {
        this.sourcePath = sourcePath;
    }

    public CtModel createModel() {

        Launcher launcher;

        // Analyze the source folders if it is a Maven project
//        if (Files.exists(sourcePath.resolve("pom.xml"))) {
//            launcher = new MavenLauncher(sourcePath.toString(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE, new String[0]);
//        } else {
            launcher = new Launcher();
            launcher.addInputResource(sourcePath.toString());
//        }

        // Ignore missing types/classpath related errors
        launcher.getEnvironment().setNoClasspath(true);
        // Ignore files with any syntax errors
        launcher.getEnvironment().setIgnoreSyntaxErrors(true);
        // Ignore duplicate declarations
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.getEnvironment().setAutoImports(true);
//

        String[] cp = classpath.stream().map(p -> p.toAbsolutePath().toString()).toList().toArray(new String[0]);
        launcher.getEnvironment().setSourceClasspath(cp);
        return launcher.buildModel();

    }


}
