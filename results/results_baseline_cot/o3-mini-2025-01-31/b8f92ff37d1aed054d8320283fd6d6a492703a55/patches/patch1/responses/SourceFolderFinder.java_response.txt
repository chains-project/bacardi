package fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.impl;

import fr.spoonlabs.flacoco.core.test.strategies.classloader.finder.classes.ClassFinder;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;

import java.io.File;
import java.util.List;

public class SourceFolderFinder implements ClassFinder {

    private static final String DEFAULT_TEST_WILDCARD = "**/*Test.java";

    private String srcFolder;

    public SourceFolderFinder(String srcFolder) {
        this.srcFolder = srcFolder;
    }

    @Override
    public String[] getClasses() {
        return getClassesLoc(new File(srcFolder)).toArray(new String[0]);
    }

    static List<String> getClassesLoc(File testSrcFolder) {
        DirectoryScanner directoryScanner = new DirectoryScanner(testSrcFolder, DEFAULT_TEST_WILDCARD);
        return directoryScanner.scan().getClasses();
    }
}