package se.kth;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.FailureCategory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;


public class DockerBuild {

    static Logger log = LoggerFactory.getLogger(DockerBuild.class);
    private static DockerClient dockerClient;
    private static final int EXIT_CODE_OK = 0;
    private static final List<String> containers = new ArrayList<>();
    public static final String BASE_IMAGE = "ghcr.io/chains-project/breaking-updates:base-image";

    private Boolean isBump = false;


    public DockerBuild(Boolean isBump) {
        this.isBump = isBump;
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
        log.info("Container with id: {} removed successfully", containerId);
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
            copyFiles(dir, dependencyStream);
            log.info("Project {} copied successfully", project);
            return dir;
        } catch (Exception e) {
            log.error("Could not copy the project {}", project, e);
            return null;
        }
    }

    public Optional<String> createImageForRepositoryAtVersion(String baseImage, URL gitUrl, String versionTag,
                                                              String imageName, Path outputPath) {
        String projectDirectoryName = "project";

        log.info("Creating container for {} with version {} in {}", gitUrl, versionTag, baseImage);
        CreateContainerResponse container = dockerClient.createContainerCmd(baseImage)
                .withCmd("/bin/sh", "-c",
                        ("git clone --branch %s %s %s && cd %s && mvn test -B -l output.log -DtestFailureIgnore=true " +
                                "-Dmaven.test.failure.ignore=true").formatted(versionTag,
                                gitUrl, projectDirectoryName, projectDirectoryName))
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        WaitContainerResultCallback waitResult = dockerClient.waitContainerCmd(container.getId())
                .exec(new WaitContainerResultCallback());
        if (waitResult.awaitStatusCode() != EXIT_CODE_OK) {
            log.warn("Could not create docker image for project {} at version {} in {}", gitUrl, versionTag, baseImage);
            this.copyM2FolderToLocalPath(container.getId(), Paths.get("/project/output.log"), outputPath);
            dockerClient.removeContainerCmd(container.getId()).exec();
            return Optional.empty();
        } else {
            log.info("Successfully created docker image for project {} at version {} in {}", gitUrl, versionTag,
                    baseImage);
            this.copyM2FolderToLocalPath(container.getId(), Paths.get("/project/output.log"), outputPath);
            dockerClient.commitCmd(container.getId())
                    .withWorkingDir("/project")
                    .withRepository("ghcr.io/chains-project/breaking-updates")
                    .withTag(imageName)
                    .exec();
            dockerClient.removeContainerCmd(container.getId()).exec();
            return Optional.of(container.getId());
        }
    }

    public String createBaseImageForBreakingUpdate(Path client, String javaVersion, String dockerImage) throws IOException {

        String baseImage;
        String imageName = "";
        Path clientName = client.toAbsolutePath().getFileName();

        if (dockerImage == null) {
            if (javaVersion.equals("Java 17")) {
                baseImage = "%s-java-17".formatted(BASE_IMAGE);
                imageName = "%s:base-17".formatted(clientName);
            } else {
                baseImage = BASE_IMAGE;
                imageName = "%s:base".formatted(clientName);
            }
        } else {
            baseImage = dockerImage;
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
//                .withCmd("mkdir", "-p", "/%s".formatted(clientName))
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        Path localFolder = Paths.get("%s".formatted(client));
        Path tar = Paths.get("%s.tar".formatted(clientName));

        String containerPath = "/%s".formatted(clientName);

        CopyArchiveToContainerCmd copyProjectToContainer = dockerClient.copyArchiveToContainerCmd(container.getId())
                .withHostResource(localFolder.toAbsolutePath().toString()) // local path
                .withRemotePath("/"); //  container path

        copyProjectToContainer.exec();

        if (isBump) {
            Path m2 = localFolder.resolveSibling("m2/.m2").normalize();
            log.info("Copying M2 folder to container ++++++++++++++++++++++++");
            CopyArchiveToContainerCmd copyCmd = dockerClient.copyArchiveToContainerCmd(container.getId())
                    .withHostResource(m2.toAbsolutePath().toString()) // local path
                    .withRemotePath("/root/"); //  container path
            copyCmd.exec();
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
            log.info("Pulling Maven image {} ...", image);
            dockerClient.pullImageCmd(image)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            log.info("Done pulling Maven image {}", image);
        }
    }

    private String startContainer(String cmd, String image, Path client) {

        String clientName = client.getFileName().toString();

        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withWorkingDir("/" + clientName)
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
    public Result reproduce(String image, FailureCategory failureCategory, Path client) {

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
        return "set -o pipefail && mvn test -B | tee mavenLog.log";

    }

    private Path storeLogFile(String containerId, Path client) {
        String clientName = client.getFileName().toString();

        Path logOutputLocation = client.resolve("output.log");
        String logLocation = "/%s/mavenLog.log".formatted(clientName);
        try (InputStream logStream = dockerClient.copyArchiveFromContainerCmd(containerId, logLocation).exec()) {
            byte[] fileContent = logStream.readAllBytes();
            Files.write(logOutputLocation, fileContent);
            return logOutputLocation;
        } catch (IOException e) {
            log.error("Could not store the log file for breaking update ", e);
            throw new RuntimeException(e);
        }
    }

    public CreateContainerResponse startContainerEntryPoint(String imageId, String[] entrypoint) {
        CreateContainerResponse container = dockerClient
                .createContainerCmd(imageId)
                .withEntrypoint(entrypoint)
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container;
    }

    public void copyM2FolderToLocalPath(String containerId, Path fromContainer, Path localPath) {

        if (Files.notExists(localPath)) {
            try {
                log.info("Creating local path {}", localPath);
                Files.createDirectories(localPath);
            } catch (IOException e) {
                log.error("Could not create local path", e);
                throw new RuntimeException(e);
            }
        }
        log.info("");
        log.info("Copying folder {} from container to local path", localPath.getFileName());

        try (InputStream m2Stream = dockerClient.copyArchiveFromContainerCmd(containerId, fromContainer.toString())
                .exec()) {
            copyFiles(localPath, m2Stream);
            log.info("Folder {} copied successfully", localPath.getFileName());
        } catch (Exception e) {
            log.error("Could not copy the {} folder", localPath, e);
        }
    }

    private void copyFiles(Path localPath, InputStream m2Stream) throws IOException {
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(m2Stream)) {
            TarArchiveEntry entry;
            while ((entry = tarStream.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    Path filePath = localPath.resolve(entry.getName());

                    if (!Files.exists(filePath)) {
                        Files.createDirectories(filePath.getParent());
                        Files.createFile(filePath);

                        byte[] fileContent = tarStream.readAllBytes();
                        Files.write(filePath, fileContent, StandardOpenOption.WRITE);
                    }
                }
            }
        }
    }


    public static void deleteImage(String imageId) {
        try {
            dockerClient.removeImageCmd(imageId).withForce(true).exec();
        } catch (Exception e) {
            log.error("Could not delete image {}", imageId, e);
        }
    }

    /**
     * Starts a container which just spins infinitely long, meant to keep the container alive and execute multiple
     * commands later on. The container must be killed manually!
     *
     * @param imageId    the docker image to use
     * @param hostConfig the HostConfig the container should be started with
     * @return the containerID of the started container
     */
    public String startSpinningContainer(String imageId, HostConfig hostConfig) {
        CreateContainerResponse container = dockerClient
                .createContainerCmd(imageId)
                .withHostConfig(hostConfig)
                .withEntrypoint("sh", "-c", "sleep 60")
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }

    /**
     * Starts a container which just spins infinitely long, meant to keep the container alive and execute multiple
     * commands later on. The container must be killed manually!
     *
     * @param imageId the docker image to use
     * @return the containerID of the started container
     */
    public String startSpinningContainer(String imageId) {
        CreateContainerResponse container = dockerClient
                .createContainerCmd(imageId)
                .withEntrypoint("sh", "-c", "sleep 60")
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }

    /**
     * Executes the given command inside an already running container and returns the output.
     *
     * @param containerId the ID of the container to execute the command in
     * @param command     the command to execute
     * @return the output of the command
     */
    public String executeInContainer(String containerId, String... command) {
        ExecCreateCmdResponse response = dockerClient.execCreateCmd(containerId)
                .withCmd(command)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            dockerClient.execStartCmd(response.getId()).exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    if (item.getStreamType() == StreamType.STDOUT || item.getStreamType() == StreamType.STDERR) {
                        try {
                            outputStream.write(item.getPayload());
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

}
