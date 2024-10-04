package se.kth;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.FailureCategory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DockerBuild {

    Logger log = LoggerFactory.getLogger(DockerBuild.class);
    private static DockerClient dockerClient;
    private static final int EXIT_CODE_OK = 0;
    private static final List<String> containers = new ArrayList<>();
    public static final String BASE_IMAGE = "ghcr.io/chains-project/breaking-updates:base-image";


    public DockerBuild() {
        createDockerClient();
    }


    /**
     * Method to remove Docker container
     *
     * @param containerId - container id
     * @return boolean - true if container is removed successfully, false otherwise
     */
    public boolean removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            log.warn("Failed to remove container with id: {}", containerId);
            return false;
        }
        log.info("Container with id: " + containerId + " removed successfully");
        return true;
    }

    /**
     * Copies a project from a Docker container to a specified directory.
     *
     * @param containerId the ID of the Docker container
     * @param project     the name of the project to copy
     * @param dir         the directory to copy the project to
     * @return the path to the directory where the project was copied, or null if the copy failed
     */
    public Path copyProjectFromContainer(String containerId, String project, Path dir) {
        containers.add(containerId);

        try (InputStream dependencyStream = dockerClient.copyArchiveFromContainerCmd(containerId, "/" + project)
                .exec()) {
            try (TarArchiveInputStream tarStream = new TarArchiveInputStream(dependencyStream)) {
                TarArchiveEntry entry;
                while ((entry = tarStream.getNextTarEntry()) != null) {
                    if (!entry.isDirectory()) {
                        Path filePath = dir.resolve(entry.getName());

                        if (!Files.exists(filePath)) {
                            Files.createDirectories(filePath.getParent());
                            Files.createFile(filePath);

                            byte[] fileContent = tarStream.readAllBytes();
                            Files.write(filePath, fileContent, StandardOpenOption.WRITE);
                        }
                    }
                }
            }
            log.info("Project {} copied successfully", project);
            return dir;
        } catch (Exception e) {
            log.error("Could not copy the project {}", project, e);
            return null;
        }
    }

    public String createBaseImageForBreakingUpdate(Path client, String javaVersion) throws IOException {

        String baseImage;
        String imageName = "";
        Path clientName = client.toAbsolutePath().getFileName();

        if (javaVersion.equals("Java 17")) {
            baseImage = "%s-java-17".formatted(BASE_IMAGE);
            imageName = "%s:base-17".formatted(clientName);
        } else {
            baseImage = BASE_IMAGE;
            imageName = "%s:base".formatted(clientName);
        }

        try {
            ensureBaseMavenImageExists(baseImage);
        } catch (InterruptedException e) {
            log.error("Could not pull base image", e);
            throw new RuntimeException(e);
        }

        log.info("Creating docker image for breaking update {}", clientName);

        CreateContainerResponse container = dockerClient.createContainerCmd(baseImage)
                .withWorkingDir("/%s".formatted(clientName))
                .withCmd("sh")
                .withCmd("mkdir", "-p", "/%s".formatted(clientName))
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        Path localFolder = Paths.get("%s".formatted(client));
        Path tar = Paths.get("%s.tar".formatted(clientName));

        createTarFile(localFolder, tar);
        String containerPath = "/%s".formatted(clientName);
        try (var tarStream = Files.newInputStream(tar)) {
            dockerClient.copyArchiveToContainerCmd(container.getId())
                    .withTarInputStream(tarStream)
                    .withRemotePath(containerPath)
                    .exec();
        } catch (IOException e) {
            log.error("Could not copy project to container", e);
            throw new RuntimeException(e);
        }

        WaitContainerResultCallback waitResult = dockerClient.waitContainerCmd(container.getId())
                .exec(new WaitContainerResultCallback());
        if (waitResult.awaitStatusCode() != EXIT_CODE_OK) {
            log.warn("Could not create docker image for {}", clientName);
            throw new RuntimeException(waitResult.toString());
        }
        dockerClient.commitCmd(container.getId())
                .withRepository(clientName.toString().toLowerCase())
                .withTag("base-17").exec();
        log.warn("Created docker image for  {}", clientName);
        dockerClient.removeContainerCmd(container.getId()).exec();
        return imageName;
    }


    public void createTarFile(Path sourceDir, Path tarFile) throws IOException {
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(tarFile.toFile()))) {
            // Config the tar file to use GNU tar format
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        // Create a tar entry
                        TarArchiveEntry entry = new TarArchiveEntry(sourceDir.relativize(path).toString());
                        entry.setSize(path.toFile().length());
                        try {
                            out.putArchiveEntry(entry);
                            Files.copy(path, out);
                            out.closeArchiveEntry();
                        } catch (IOException e) {
                            log.error("Could not create tar file", e);
                            throw new RuntimeException(e);
                        }
                    });
            // Close the output stream
            out.finish();
        }
    }


    private void createDockerClient() {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUrl("https://hub.docker.com")
                .build();
        DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .connectTimeout(30)
                .build();
        dockerClient = DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    public void ensureBaseMavenImageExists(String image) throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            log.info("Base image not present, pulling {}", image);
            dockerClient.pullImageCmd(image)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            log.info("Done pulling Maven image {}", image);
        }
    }

    private String startContainer(String cmd, String image, String client) {
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withWorkingDir("/" + client)
                .withCmd("sh", "-c", cmd)
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

    /**
     * Reproduces a breaking update using a specified Docker image and failure category.
     *
     * @param image           the Docker image to use for the reproduction
     * @param failureCategory the category of failure to reproduce
     * @param client          the client name
     * @return a Result object containing the outcome of the reproduction attempts
     */
    public Result reproduce(String image, FailureCategory failureCategory, String client) {

        // Store result for each attempt
        Result breakingUpdateReproductionResult = new Result(failureCategory);

        Map<String, String> startedContainers = new HashMap<>();
        int attemptCount;

        for (attemptCount = 3; attemptCount < 4; attemptCount++) {

            startedContainers.put("postContainer%s".formatted(attemptCount), startContainer(getPostCmd(), image, client));

            WaitContainerResultCallback result = dockerClient.waitContainerCmd(startedContainers.get("postContainer%s"
                    .formatted(attemptCount))).exec(new WaitContainerResultCallback());

            if (result.awaitStatusCode().intValue() != EXIT_CODE_OK) {
                // if fail store the log file
                storeLogFile(startedContainers.get("postContainer%s".formatted(attemptCount)), client);
                // stop the process and store the log file
                log.info("Breaking commit failed in the {} attempt.", attemptCount);
                breakingUpdateReproductionResult.getAttempts().add(new Attempt(attemptCount, FailureCategory.UNKNOWN_FAILURE, false));

            } else {
                log.info("Breaking commit did not fail in the {} attempt.", attemptCount);
                if (attemptCount == 3) {
                    breakingUpdateReproductionResult.getAttempts().add(new Attempt(attemptCount, FailureCategory.BUILD_SUCCESS, false));
                    storeLogFile(startedContainers.get("postContainer%s".formatted(attemptCount)), client);
                }
            }

            // remove the containers

        }
        startedContainers.forEach((key, value) -> removeContainer(value));
        return breakingUpdateReproductionResult;
    }


    /**
     * Command to compile and test the breaking update
     */
    private static String getPostCmd() {
        return "set -o pipefail && mvn clean test -B | tee mavenLog.log";

    }

    private Path storeLogFile(String containerId, String client) {

        Path logOutputLocation = Path.of("maven.log");
        String logLocation = "/%s/mavenLog.log".formatted(client);
        try (InputStream logStream = dockerClient.copyArchiveFromContainerCmd(containerId, logLocation).exec()) {
            byte[] fileContent = logStream.readAllBytes();
            Files.write(logOutputLocation, fileContent);
            return logOutputLocation;
        } catch (IOException e) {
            log.error("Could not store the log file for breaking update ", e);
            throw new RuntimeException(e);
        }
    }


}
