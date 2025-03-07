package fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.impl;

import fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.ClassFinder;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;

import java.io.File;
import java.util.List;

public class SourceFolderFinder implements ClassFinder {

    private String srcFolder;

    public SourceFolderFinder(String srcFolder) {
        this.srcFolder = srcFolder;
    }

    @Override
    public String[] getClasses() {
        return getClassesLoc(new File(srcFolder)).toArray(new String[0]);
    }

    static List<String> getClassesLoc(File testSrcFolder) {
        // Assuming the default wildcard pattern is "**/*.java" for scanning Java source files.
        // This pattern should be adjusted based on the actual requirement or the new API provided by the updated dependency.
        String wildcardPattern = "**/*.java";
        DirectoryScanner directoryScanner = new DirectoryScanner(testSrcFolder, wildcardPattern);
        return directoryScanner.scan().getClasses();
    }
}