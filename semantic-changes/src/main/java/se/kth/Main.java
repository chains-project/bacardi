package se.kth;

import se.kth.instrumentation.Instrumenter;
import se.kth.instrumentation.ModelBuilder;
import se.kth.instrumentation.ProjectExtractor;
import se.kth.instrumentation.model.TargetMethod;
import se.kth.util.Config;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> images = List.of("ghcr.io/chains-project/breaking-updates:jsoup-1.7.1");

        DockerBuild dockerBuild = new DockerBuild(false);
        Path extractedProjectsOutputDir = Config.getTmpDirPath().resolve("instrumentation-sources");
        ProjectExtractor projectExtractor = new ProjectExtractor(dockerBuild, extractedProjectsOutputDir);
        for (String image : images) {
            Path sourcesPath = projectExtractor.extract(image);

            ModelBuilder modelBuilder = new ModelBuilder(sourcesPath);
            CtModel model = modelBuilder.buildModel();

            Instrumenter instrumenter = new Instrumenter(model);

            TargetMethod targetMethod = new TargetMethod("org.jsoup.nodes.Element", "prepend", List.of("java.lang.String"));
            instrumenter.instrumentForMethod(targetMethod);

        }
    }
}