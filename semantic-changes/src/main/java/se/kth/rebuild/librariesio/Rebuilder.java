package se.kth.rebuild.librariesio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;

import java.net.URL;
import java.util.List;
import java.util.Optional;

public class Rebuilder {

    private static final Logger logger = LoggerFactory.getLogger(Rebuilder.class);

    private static final List<String> JAVA_VERSIONS = List.of("java-8", "java-11", "java-17", "java-21");

    private final DockerBuild dockerBuild;

    public Rebuilder(DockerBuild dockerBuild) {
        this.dockerBuild = dockerBuild;
    }

    public Optional<String> rebuildProject(URL gitUrl, String version) {

        return Optional.empty();
    }

    private void tryPullProject(URL gitUrl, String version) {
        logger.info("Trying to pull project {} at version {}", gitUrl, version);
        Optional<String> imageName = JAVA_VERSIONS.stream()
                .map(javaVersion -> this.dockerBuild.createImageForRepositoryAtVersion(DockerBuild.BASE_IMAGE + "-" + javaVersion, gitUrl, version, ""))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get);
        if (imageName.isPresent()) {
            logger.info("Success in creating image for {} at version {}", gitUrl, version);
        } else {
            logger.info("Failed to create image for {} at version {}", gitUrl, version);
        }
    }

    private void tryRunTests() {

    }
}
