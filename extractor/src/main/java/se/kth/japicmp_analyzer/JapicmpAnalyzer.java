package se.kth.japicmp_analyzer;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiClass;

import java.util.List;

public class JapicmpAnalyzer {

    private final ApiMetadata oldApi;
    private final ApiMetadata newApi;

    public JapicmpAnalyzer(ApiMetadata oldAPi, ApiMetadata newApi) {
        this.oldApi = oldAPi;
        this.newApi = newApi;
    }

    public List<JApiClass> getChanges() {
        JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(getDefaultOptions());
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

        JApiCmpArchive preArchive = new JApiCmpArchive(oldApi.getPath().toFile(), oldApi.getName());
        JApiCmpArchive breakingArchive = new JApiCmpArchive(newApi.getPath().toFile(), newApi.getName());
        return jarArchiveComparator.compare(preArchive, breakingArchive);
    }

    private Options getDefaultOptions() {
        Options defaultOptions = Options.newDefault();
        defaultOptions.setOutputOnlyBinaryIncompatibleModifications(true);
        defaultOptions.setIgnoreMissingClasses(true);

        return defaultOptions;
    }
}
