package se.kth.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public static DockerClient getDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();


        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    public static Path getTmpDirPath() {
        Path tmpPath = Paths.get(".tmp").toAbsolutePath();
        FileUtils.ensureDirectoryExists(tmpPath);
        return tmpPath;
    }

    public static Path relativeToTmpDir(String other) {
        return getTmpDirPath().resolve(other);
    }

    public static Path getBumpDir() {
        Path currentDir = Paths.get("").toAbsolutePath();
        Path bumpRelativePath = Path.of("bump", "data", "benchmark");
        return currentDir.resolveSibling(bumpRelativePath);
    }

    public static Path getResourcesDir() {
        return Path.of("test-failures", "src", "main", "resources").toAbsolutePath();
    }
}
