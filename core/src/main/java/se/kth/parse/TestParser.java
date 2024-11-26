package se.kth.parse;

import se.kth.direct_failures.RepairDirectFailures;
import se.kth.model.DependencyTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TestParser {
    public static void main(String[] args) {

        ParseMavenDependencyTree parseMavenDependencyTree = new ParseMavenDependencyTree("/Users/frank/Documents/Work/PHD/bacardi/projects/bd3ce213e2771c6ef7817c80818807a757d4e94a/OCR4all/tree.json");


        List<DependencyTree> dependencies = parseMavenDependencyTree.parseDependencies();
        List<DependencyTree> children = parseMavenDependencyTree.findChildrenByName(dependencies, "com.fasterxml.jackson.core", "jackson-databind");
        List<DependencyTree> hasSameOrLowerLevelDependencies = parseMavenDependencyTree.hasSameOrLowerLevelDependencies(dependencies, children);
        System.out.println("Children: " + children);
        System.out.println("Has same or lower level dependencies: " + hasSameOrLowerLevelDependencies);

//        PomModel pomModel = new PomModel();

        try {
//            pomModel.modifyDependency(hasSameOrLowerLevelDependencies.get(0));
            RepairDirectFailures.modifyPomFile(Path.of("/Users/frank/Documents/Work/PHD/bacardi/projects/bd3ce213e2771c6ef7817c80818807a757d4e94a/OCR4all/pom.xml"), hasSameOrLowerLevelDependencies.get(0));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
