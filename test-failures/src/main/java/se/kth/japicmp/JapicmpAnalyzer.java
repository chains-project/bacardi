package se.kth.japicmp;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiClass;
import se.kth.Util.FileUtils;
import se.kth.model.BreakingUpdate;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class JapicmpAnalyzer {

    private final Path preM2Path;
    private final Path breakingM2Path;

    public JapicmpAnalyzer(Path preM2Path, Path breakingM2Path) {
        this.preM2Path = preM2Path;
        this.breakingM2Path = breakingM2Path;
    }

    public List<JApiClass> getChanges(BreakingUpdate breakingUpdate) {
        JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(getDefaultOptions());
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
        String commitId = breakingUpdate.breakingCommit;

        File jarFilePre = this.getJarFile(this.preM2Path, commitId);
        File jarFileBreaking = this.getJarFile(this.breakingM2Path, commitId);

        JApiCmpArchive preArchive = new JApiCmpArchive(jarFilePre, "v1");
        JApiCmpArchive breakingArchive = new JApiCmpArchive(jarFileBreaking, "v2");
        return jarArchiveComparator.compare(preArchive, breakingArchive);
    }

    private File getJarFile(Path basePath, String commitId) {
        Path projectPath = basePath.resolve(commitId);
        return FileUtils.getFilesInDirectory(projectPath.toString()).getFirst();
    }

    private Options getDefaultOptions() {
        Options defaultOptions = Options.newDefault();
        defaultOptions.setOutputOnlyModifications(true);
        defaultOptions.setIgnoreMissingClasses(true);
        return defaultOptions;
    }
}
