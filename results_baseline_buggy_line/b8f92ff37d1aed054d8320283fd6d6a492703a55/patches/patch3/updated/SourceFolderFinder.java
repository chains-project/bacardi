package fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.impl;

import fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.ClassFinder;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.api.testset.TestListResolver;

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
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(testSrcFolder);
        // Update the way to get the wildcard pattern to match the new API
        String wildcard = "**/*.class"; // Assuming we want to include all class files
        directoryScanner.setIncludes(new String[]{wildcard});
        directoryScanner.scan();
        return List.of(directoryScanner.getIncludedFiles());
    }
}