package fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.impl;

import fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.ClassFinder;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.api.testset.TestListResolver;
import java.io.File;
import java.util.List;

public class SourceFolderFinder implements ClassFinder {

    private String srcFolder;
    private static final String DEFAULT_TEST_PATTERN = "**/*Test*.class";

    public SourceFolderFinder(String srcFolder) {
        this.srcFolder = srcFolder;
    }

    @Override
    public String[] getClasses() {
        return getClassesLoc(new File(srcFolder)).toArray(new String[0]);
    }

    static List<String> getClassesLoc(File testSrcFolder) {
        // Replace the removed TestListResolver.getWildcard() with a constant pattern.
        DirectoryScanner directoryScanner = new DirectoryScanner(testSrcFolder, DEFAULT_TEST_PATTERN);
        return directoryScanner.scan().getClasses();
    }
}