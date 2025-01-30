package se.kth.rebuild.util;

import se.kth.DockerBuild;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class CreateDockerImage {
    public static void main(String[] args) throws MalformedURLException {
        Path outputPath = Path.of("/home/leonard/code/java/bacardi/.tmp2/docker-output");
        String baseImage = "ghcr.io/chains-project/breaking-updates:base-image-java-8";
        String githubUrl = "https://github.com/jhy/jsoup.git";
        String versionTag = "jsoup-1.7.3";
        String imageName = "jsoup-1.7.3";

        DockerBuild dockerBuild = new DockerBuild(false);
        Optional<String> containerId = dockerBuild.createImageForRepositoryAtVersion(baseImage, new URL(githubUrl),
                versionTag, imageName, outputPath);
        if (containerId.isPresent()) {
            System.out.println(containerId);
        }
    }
}
