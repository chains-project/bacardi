package se.kth.injector;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;

import java.util.LinkedList;
import java.util.List;

public class TestFrameworkHandler {

    private final Model model;

    public TestFrameworkHandler(Model model) {
        this.model = model;
    }

    public Model migrate() {
        String jupiterVersion = "5.11.2";
        Dependency vintageEngine = Dependency.newBuilder()
                .groupId("org.junit.vintage")
                .artifactId("junit-vintage-engine")
                .version(jupiterVersion)
                .scope("test")
                .build();

        Dependency jupiterPlatform = Dependency.newBuilder()
                .groupId("org.junit.platform")
                .artifactId("junit-platform-launcher")
                .version("1.11.2")
                .scope("test")
                .build();

        Dependency jupiterEngine = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-engine")
                .version(jupiterVersion)
                .scope("test")
                .build();

        Dependency jupiterApi = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version(jupiterVersion)
                .scope("test")
                .build();

        List<Dependency> newDependencies = new LinkedList<>(model.getDependencies());
        newDependencies.add(vintageEngine);
        newDependencies.add(jupiterPlatform);
        newDependencies.add(jupiterEngine);
        newDependencies.add(jupiterApi);

        return Model.newBuilder(model)
                .dependencies(newDependencies)
                .build();
    }
}
