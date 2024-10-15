package se.kth.injector;

import org.apache.maven.api.model.Dependency;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestFrameworkHandler {

    private final List<Dependency> dependencies;

    public TestFrameworkHandler(List<Dependency> dependencies) {
        this.dependencies = new LinkedList<>(dependencies);
    }

    public List<Dependency> getWithAddedDependencies() {
        List<Dependency> result = new ArrayList<>();

        String jupiterVersionToUse = this.containsAnyJupiterDependency() ? this.getJupiterVersion() : "5.10.1";

        if (this.containsOnlyJunit4()) {
            this.performJunit4Migration();
        } else if (this.containsJunit4() && this.containsAnyJupiterDependency()) {
            this.performMixedMigration(jupiterVersionToUse);
        } else if (!this.containsJunit4() && this.containsAnyJupiterDependency()) {
            this.performJupiterMigration(jupiterVersionToUse);
        } else {
            this.performJunit4Migration();
        }

        return result;
    }

    private boolean containsJunit4() {
        return this.containsDependency("junit", "junit");
    }

    private boolean containsOnlyJunit4() {
        return this.containsJunit4() && !this.containsAnyJupiterDependency();
    }

    private boolean containsAnyJupiterDependency() {
        return this.dependencies.stream().anyMatch(d -> d.getGroupId().equals("org.junit.jupiter"));
    }

    private String getJupiterVersion() {
        return this.dependencies.stream()
                .filter(d -> d.getGroupId().equals("org.junit.jupiter"))
                .findFirst()
                .get()
                .getVersion();
    }

    private boolean containsDependency(String groupId, String artifactId) {
        return this.dependencies.stream().anyMatch(
                dependency -> dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId));
    }

    private void performJunit4Migration() {
        Dependency vintageEngine = Dependency.newBuilder()
                .groupId("org.junit.vintage")
                .artifactId("junit-vintage-engine")
                .version("5.10.1")
                .scope("test")
                .build();

        Dependency jupiterAggregator = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter")
                .version("5.10.1")
                .scope("test")
                .build();

        this.dependencies.add(vintageEngine);
        this.dependencies.add(jupiterAggregator);
    }

    private void performMixedMigration(String jupiterVersion) {
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

        Dependency jupiterAggregator = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter")
                .version(jupiterVersion)
                .scope("test")
                .build();

        this.safeAdd(vintageEngine);
        this.safeAdd(jupiterPlatform);
        this.safeAdd(jupiterEngine);
        this.safeAdd(jupiterApi);
        this.safeAdd(jupiterAggregator);
    }

    private void performJupiterMigration(String jupiterVersion) {
        Dependency jupiterPlatform = Dependency.newBuilder()
                .groupId("org.junit.platform")
                .artifactId("junit-platform-launcher")
                .version("1.11.2")
                .scope("test")
                .build();

        Dependency jupiterAggregator = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter")
                .version(jupiterVersion)
                .scope("test")
                .build();

        this.safeAdd(jupiterPlatform);
        this.safeAdd(jupiterAggregator);
    }

    private boolean safeAdd(Dependency dependency) {
        boolean alreadyContained = this.dependencies.stream()
                .anyMatch(d -> d.getGroupId().equals(dependency.getGroupId()) && d.getArtifactId().equals(dependency.getArtifactId()));
        if (alreadyContained) {
            return false;
        } else {
            this.dependencies.add(dependency);
            return true;
        }
    }
}
