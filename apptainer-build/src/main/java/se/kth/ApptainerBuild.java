package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.FailureCategory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages the Apptainer sandboxes used to reproduce breaking dependency updates and run
 * their Maven builds. Unlike Docker, Apptainer has no daemon and no Java SDK, so every
 * operation here shells out to the {@code apptainer} CLI via {@link ProcessBuilder}.
 * <p>
 * Each OCI image reference is materialized as a writable "sandbox" directory
 * ({@code apptainer build --sandbox}), which is the mutable equivalent of a committed
 * Docker image: writes into the sandbox persist on disk immediately, so there is no
 * separate "commit" step, and copying files in/out of a "container" is a plain filesystem
 * copy against the sandbox path.
 */
public class ApptainerBuild {

    private static final Logger log = LoggerFactory.getLogger(ApptainerBuild.class);
    private static final int EXIT_CODE_OK = 0;
    private static final String APPTAINER_BIN = "apptainer";
    private static final String SANDBOX_REF_MARKER = ".apptainer-ref";
    public static final String BASE_IMAGE = "ghcr.io/chains-project/breaking-updates:base-image";

    private final Boolean isBump;
    private final int max_attempts;
    private final Path sandboxRoot;
    private final Map<String, List<String>> activeBinds = new ConcurrentHashMap<>();
    private final Map<String, Object> buildLocks = new ConcurrentHashMap<>();

    public ApptainerBuild(Boolean isBump, int max_attempts) {
        this(isBump, max_attempts, null);
    }

    public ApptainerBuild(Boolean isBump) {
        this(isBump, 1, null);
    }

    public ApptainerBuild(Boolean isBump, int max_attempts, String sandboxRootDir) {
        this.isBump = isBump;
        this.max_attempts = max_attempts;
        String root = (sandboxRootDir == null || sandboxRootDir.isBlank())
                ? System.getProperty("user.dir") + "/.apptainer-sandboxes"
                : sandboxRootDir;
        this.sandboxRoot = Paths.get(root);
        verifyApptainerAvailable();
        try {
            Files.createDirectories(sandboxRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create Apptainer sandbox root at " + sandboxRoot, e);
        }
    }

    private void verifyApptainerAvailable() {
        try {
            ExecResult result = runProcess(List.of(APPTAINER_BIN, "--version"));
            if (result.exitCode() != EXIT_CODE_OK) {
                throw new RuntimeException("Found 'apptainer' on PATH but '--version' failed: " + result.output());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(
                    "Could not run the 'apptainer' CLI. Install Apptainer and ensure fakeroot/rootless support is configured.",
                    e);
        }
    }

    // ---------------------------------------------------------------------
    // Sandbox lifecycle
    // ---------------------------------------------------------------------

    /**
     * Deterministic, filesystem-safe sandbox directory for a given OCI image reference.
     */
    private Path sandboxDirFor(String imageRef) {
        return sandboxRoot.resolve(sha256(imageRef));
    }

    private boolean sandboxExists(Path sandboxDir) {
        return Files.exists(sandboxDir.resolve(SANDBOX_REF_MARKER));
    }

    /**
     * Ensures a writable sandbox exists locally for the given OCI image reference, building
     * it from the registry via {@code apptainer build --sandbox} if it hasn't been pulled
     * yet. Safe to call concurrently for different (or the same) image references.
     */
    public void ensureBaseMavenImageExists(String image) throws InterruptedException {
        Object lock = buildLocks.computeIfAbsent(image, k -> new Object());
        synchronized (lock) {
            Path sandboxDir = sandboxDirFor(image);
            if (sandboxExists(sandboxDir)) {
                log.info("Sandbox already present for {} at {}", image, sandboxDir);
                return;
            }
            log.info("Sandbox not present, pulling docker://{} ...", image);
            try {
                Files.createDirectories(sandboxDir.getParent());
                ExecResult result = runProcess(List.of(APPTAINER_BIN, "build", "--sandbox", "--force",
                        sandboxDir.toString(), "docker://" + image));
                if (result.exitCode() != EXIT_CODE_OK) {
                    throw new RuntimeException(
                            "Failed to build Apptainer sandbox for image %s:\n%s".formatted(image, result.output()));
                }
                Files.writeString(sandboxDir.resolve(SANDBOX_REF_MARKER), image);
                log.info("Done building sandbox for {}", image);
            } catch (IOException e) {
                throw new RuntimeException("I/O error while building Apptainer sandbox for " + image, e);
            }
        }
    }

    /**
     * Resolves (building it if needed) the sandbox for an image and returns its path,
     * replacing Docker's "start a long-lived container so it can be exec'd into
     * repeatedly" — a sandbox is just a directory, so nothing needs to be "started".
     */
    public String startSpinningContainer(String imageId) throws InterruptedException {
        ensureBaseMavenImageExists(imageId);
        return sandboxDirFor(imageId).toString();
    }

    /**
     * Same as {@link #startSpinningContainer(String)}, but remembers a set of
     * {@code hostPath:containerPath} bind-mount specs to apply to every subsequent
     * {@link #executeInContainer} call against this sandbox — replacing Docker's
     * container-creation-time {@code HostConfig}/{@code Mount} list.
     */
    public String startSpinningContainer(String imageId, List<String> binds) throws InterruptedException {
        String sandboxPath = startSpinningContainer(imageId);
        activeBinds.put(sandboxPath, binds == null ? List.of() : binds);
        return sandboxPath;
    }

    public String startContainerEntryPoint(String imageId, String[] entrypoint) throws InterruptedException {
        // entrypoint is unused: sandboxes don't need a long-running entry process, exec
        // can be run directly against the sandbox any number of times.
        return startSpinningContainer(imageId);
    }

    /**
     * There's no running container/process to stop under the sandbox model; this just
     * forgets any bind-mount specs registered for the sandbox.
     */
    public boolean removeContainer(String containerId) {
        activeBinds.remove(containerId);
        return true;
    }

    public void deleteSandbox(String imageId) {
        Path sandboxDir = sandboxDirFor(imageId);
        if (!Files.exists(sandboxDir)) {
            log.warn("Sandbox for {} not found, skipping deletion", imageId);
            return;
        }
        try (Stream<Path> stream = Files.walk(sandboxDir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("Failed to delete {}", p, e);
                }
            });
            log.info("Sandbox for {} deleted successfully", imageId);
        } catch (IOException e) {
            log.error("Could not delete sandbox for {}", imageId, e);
        }
    }

    // ---------------------------------------------------------------------
    // Exec
    // ---------------------------------------------------------------------

    /**
     * Executes a command inside the given sandbox and returns its combined stdout/stderr.
     */
    public String executeInContainer(String containerId, String... command) {
        List<String> full = new ArrayList<>(List.of(APPTAINER_BIN, "exec", "--fakeroot", "--writable"));
        for (String bind : activeBinds.getOrDefault(containerId, List.of())) {
            prepareBindDestination(containerId, bind);
            full.add("--bind");
            full.add(bind);
        }
        full.add(containerId);
        full.addAll(List.of(command));
        try {
            return runProcess(full).output();
        } catch (IOException | InterruptedException e) {
            log.error("Error executing command in sandbox {}", containerId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * A writable (non-overlay) Apptainer sandbox can't auto-create bind-mount
     * destinations, unlike Docker — the target path must already exist inside the
     * sandbox, as a file or directory matching the source's type, or the mount fails.
     */
    private void prepareBindDestination(String containerId, String bindSpec) {
        String[] parts = bindSpec.split(":");
        if (parts.length < 2) {
            return;
        }
        Path source = Paths.get(parts[0]);
        Path destination = resolveInSandbox(containerId, parts[1]);
        try {
            if (Files.isDirectory(source)) {
                Files.createDirectories(destination);
            } else if (!Files.exists(destination)) {
                if (destination.getParent() != null) {
                    Files.createDirectories(destination.getParent());
                }
                Files.createFile(destination);
            }
        } catch (IOException e) {
            log.error("Could not prepare bind destination {} in sandbox {}", parts[1], containerId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Command to compile and test the breaking update.
     */
    private static String getPostCmd() {
        return "set -o pipefail && mvn test -Dcheckstyle.skip=true -Dpmd.skip=true  -B | tee mavenLog.log";
    }

    /**
     * Reproduces a breaking update inside the given sandbox, up to {@code max_attempts}
     * times, storing the Maven log for each attempt.
     */
    public Result reproduce(String image, FailureCategory failureCategory, Path client, Path logFile) {
        Result breakingUpdateReproductionResult = new Result(failureCategory);
        String sandboxDir = sandboxDirFor(image).toString();
        String clientName = client.getFileName().toString();

        for (int attemptCount = 1; attemptCount <= max_attempts; attemptCount++) {
            try {
                ExecResult result = runProcess(List.of(APPTAINER_BIN, "exec", "--fakeroot", "--writable",
                        "--pwd", "/" + clientName, sandboxDir, "sh", "-c", getPostCmd()));

                if (result.exitCode() != EXIT_CODE_OK) {
                    storeLogFile(sandboxDir, client, logFile);
                    log.info("Breaking commit failed in the {} attempt.", attemptCount);
                    breakingUpdateReproductionResult.getAttempts().add(new Attempt(attemptCount,
                            FailureCategory.UNKNOWN_FAILURE, logFile.getParent().toString(), false));
                } else {
                    log.info("Breaking commit did not fail in the {} attempt.", attemptCount);
                    breakingUpdateReproductionResult.getAttempts().add(new Attempt(attemptCount,
                            FailureCategory.BUILD_SUCCESS, logFile.getParent().toString(), false));
                    storeLogFile(sandboxDir, client, logFile);
                }
            } catch (IOException | InterruptedException e) {
                log.error("Error reproducing build in sandbox {}", sandboxDir, e);
                throw new RuntimeException(e);
            }
        }
        return breakingUpdateReproductionResult;
    }

    public Path storeLogFile(String containerId, Path client, Path logFile) {
        String clientName = client.getFileName().toString();
        Path source = Paths.get(containerId, clientName, "mavenLog.log");
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.copy(source, logFile, StandardCopyOption.REPLACE_EXISTING);
            return logFile;
        } catch (IOException e) {
            log.error("Could not store the log file for breaking update ", e);
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Copy in/out — plain filesystem operations since a sandbox is a real directory
    // ---------------------------------------------------------------------

    /**
     * Copies a local folder into the sandbox root (mirrors the old "copy folder to Docker
     * image at /" + commit dance — no commit needed, the sandbox already persists writes).
     */
    public String copyFolderToDockerImage(String dockerImage, String folderPath) throws IOException {
        Path sandboxDir = sandboxDirFor(dockerImage);
        Path source = Paths.get(folderPath);
        Path target = sandboxDir.resolve(source.getFileName().toString());
        copyRecursively(source, target);
        log.info("Copied folder {} into sandbox for {}", folderPath, dockerImage);
        return dockerImage;
    }

    public Path copyFromContainer(String containerId, String file, Path dir) {
        Path source = resolveInSandbox(containerId, file);
        try {
            if (!Files.exists(source)) {
                log.error("File {} not found in sandbox {}", file, containerId);
                return null;
            }
            if (Files.exists(dir)) {
                log.info("Destination {} already exists, skipping copy of {}", dir, file);
                return dir;
            }
            if (dir.getParent() != null) {
                Files.createDirectories(dir.getParent());
            }
            Files.copy(source, dir, StandardCopyOption.REPLACE_EXISTING);
            log.info("File {} copied successfully", file);
            return dir;
        } catch (IOException e) {
            log.error("Could not copy the file {}", file, e);
            return null;
        }
    }

    public Path copyProjectFromContainer(String containerId, String project, Path dir) {
        Path source = resolveInSandbox(containerId, "/" + project);
        try {
            copyRecursively(source, dir);
            log.info("Project {} copied successfully", project);
            return dir;
        } catch (IOException e) {
            log.error("Could not copy the project {}", project, e);
            return null;
        }
    }

    public void copyM2FolderToLocalPath(String containerId, Path fromContainer, Path localPath) {
        Path source = resolveInSandbox(containerId, fromContainer.toString());
        try {
            if (Files.isDirectory(source)) {
                // Matches `docker cp container:/dir localPath` semantics: the source
                // directory is copied *into* localPath as a subdirectory named after its
                // own basename, not flattened directly into localPath.
                Path target = localPath.resolve(source.getFileName().toString());
                if (Files.notExists(localPath)) {
                    log.info("Creating local path {}", localPath);
                    Files.createDirectories(localPath);
                }
                log.info("Copying folder {} from sandbox to local path", localPath.getFileName());
                copyRecursively(source, target);
                log.info("Folder {} copied successfully", localPath.getFileName());
            } else {
                copyRecursively(source, localPath);
                log.info("File {} copied successfully", localPath.getFileName());
            }
        } catch (IOException e) {
            log.error("Could not copy the {} folder", localPath, e);
        }
    }

    private Path resolveInSandbox(String sandboxDir, String pathInContainer) {
        String relative = pathInContainer.startsWith("/") ? pathInContainer.substring(1) : pathInContainer;
        return Paths.get(sandboxDir, relative);
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        if (Files.isSymbolicLink(source)) {
            copySymlink(source, target);
        } else if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            // Don't follow symlinks while walking: project checkouts routinely contain
            // symlinks (some intentionally broken, e.g. optional submodule placeholders),
            // and a real (non-tar-based) filesystem copy must preserve them as links
            // rather than dereferencing them the way Files.copy/isDirectory do by default.
            try (Stream<Path> stream = Files.walk(source)) {
                for (Path src : (Iterable<Path>) stream::iterator) {
                    if (src.equals(source)) {
                        continue;
                    }
                    Path dest = target.resolve(source.relativize(src).toString());
                    if (Files.isSymbolicLink(src)) {
                        copySymlink(src, dest);
                    } else if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        if (dest.getParent() != null) {
                            Files.createDirectories(dest.getParent());
                        }
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                }
            }
        } else {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copySymlink(Path source, Path target) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.deleteIfExists(target);
        Files.createSymbolicLink(target, Files.readSymbolicLink(source));
    }

    // ---------------------------------------------------------------------
    // Java-version base image handling
    // ---------------------------------------------------------------------

    /**
     * Builds (or reuses) a per-project sandbox derived from the Java-17 (or default) base
     * image, with the client project and its {@code .m2} folder copied in directly. Since
     * sandboxes persist writes immediately, there's no separate "commit with tag" step —
     * the returned image name is just the key used to address this sandbox afterwards.
     */
    public String createNewBaseImageWithNewJavaVersion(Path client, String javaVersion, String dockerImage) {
        String baseImage;
        String imageName;
        Path clientName = client.toAbsolutePath().getFileName();

        if (javaVersion.equals("Java 17")) {
            baseImage = "%s-java-17".formatted(BASE_IMAGE);
            imageName = "%s:breaking-update-java-17".formatted(clientName);
        } else {
            baseImage = BASE_IMAGE;
            imageName = "%s:base".formatted(clientName);
        }

        try {
            ensureBaseMavenImageExists(baseImage);
            log.info("Creating Apptainer sandbox for breaking update {}", clientName);

            Path baseSandbox = sandboxDirFor(baseImage);
            Path newSandbox = sandboxDirFor(imageName);
            if (!sandboxExists(newSandbox)) {
                copyRecursively(baseSandbox, newSandbox);
                Files.writeString(newSandbox.resolve(SANDBOX_REF_MARKER), imageName);
            }

            copyRecursively(client, newSandbox.resolve(clientName.toString()));

            Path m2 = client.resolveSibling("m2/.m2").normalize();
            if (Files.exists(m2)) {
                log.info("Copying M2 folder to sandbox");
                copyRecursively(m2, newSandbox.resolve("root").resolve(".m2"));
            }

            log.warn("Created Apptainer sandbox for {}", clientName);
            return imageName;
        } catch (InterruptedException e) {
            log.error("Could not pull base image {} ", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Could not create sandbox for {}", clientName, e);
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Used by the semantic-changes rebuild utility (se.kth.rebuild.util.CreateDockerImage)
    // ---------------------------------------------------------------------

    public Optional<String> createImageForRepositoryAtVersion(String baseImage, URL gitUrl, String versionTag,
                                                                String imageName, Path outputPath) {
        String projectDirectoryName = "project";
        try {
            ensureBaseMavenImageExists(baseImage);
            Path sandboxDir = sandboxDirFor(baseImage);
            String cmd = ("git clone --branch %s %s %s && cd %s && mvn test -B -l output.log -DtestFailureIgnore=true "
                    + "-Dmaven.test.failure.ignore=true").formatted(versionTag, gitUrl, projectDirectoryName,
                    projectDirectoryName);

            ExecResult result = runProcess(List.of(APPTAINER_BIN, "exec", "--fakeroot", "--writable",
                    sandboxDir.toString(), "sh", "-c", cmd));
            String sandboxPath = sandboxDir.toString();

            if (result.exitCode() != EXIT_CODE_OK) {
                log.warn("Could not create sandbox for project {} at version {} in {}", gitUrl, versionTag, baseImage);
                copyFromContainer(sandboxPath, "/" + projectDirectoryName + "/output.log",
                        outputPath.resolve("output.log"));
                return Optional.empty();
            }

            log.info("Successfully created sandbox for project {} at version {} in {}", gitUrl, versionTag, baseImage);
            copyFromContainer(sandboxPath, "/" + projectDirectoryName + "/output.log",
                    outputPath.resolve("output.log"));

            Path taggedSandbox = sandboxDirFor("ghcr.io/chains-project/breaking-updates:" + imageName);
            copyRecursively(sandboxDir, taggedSandbox);
            Files.writeString(taggedSandbox.resolve(SANDBOX_REF_MARKER),
                    "ghcr.io/chains-project/breaking-updates:" + imageName);
            return Optional.of(sandboxPath);
        } catch (IOException | InterruptedException e) {
            log.error("Error creating sandbox for repository at version", e);
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------------
    // Process helper
    // ---------------------------------------------------------------------

    /**
     * Runs an {@code apptainer} command and returns its stdout only. stderr is kept
     * separate (not merged) since Apptainer itself writes diagnostic "WARNING:"/"INFO:"
     * lines (about mounts, chdir fallbacks, etc.) to stderr — merging them into stdout
     * would corrupt output that callers parse as real command results (e.g. a `find`ed
     * dependency path). stderr is still drained and logged for debugging.
     */
    private ExecResult runProcess(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        StringBuilder errors = new StringBuilder();
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (!errors.isEmpty()) {
            log.debug("apptainer stderr for {}: {}", command, errors);
        }
        return new ExecResult(exitCode, output.toString());
    }

    private record ExecResult(int exitCode, String output) {
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
