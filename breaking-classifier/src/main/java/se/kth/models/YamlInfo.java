package se.kth.models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents information about Java version incompatibilities.
 */
@lombok.Getter
@lombok.Setter
public class YamlInfo {

    private String yamlFile;
    private String line;
    List<Integer> javaVersions;

    public YamlInfo(String file) {
        this.yamlFile = file;
        javaVersions = new ArrayList<>();
    }

    public YamlInfo(String yamlFile, String line) {
        this.yamlFile = yamlFile;
        this.line = line;
        javaVersions = new ArrayList<>();


    }

    public YamlInfo(String yamlFile, String line, List<Integer> javaVersions) {
        this.yamlFile = yamlFile;
        this.line = line;
        this.javaVersions = javaVersions;
    }

    @Override
    public String toString() {
        return "YamlInfo{" +
                "yamlFile='" + yamlFile + '\'' +
                ", line='" + line + '\'' +
                ", javaVersions=" + javaVersions +
                '}';
    }


}
