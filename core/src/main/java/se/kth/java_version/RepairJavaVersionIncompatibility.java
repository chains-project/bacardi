package se.kth.java_version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.Result;
import se.kth.models.FailureCategory;
import se.kth.models.JavaVersionIncompatibility;
import se.kth.models.JavaVersionInfo;

import java.io.IOException;
import java.nio.file.Path;

public class RepairJavaVersionIncompatibility {


    private final JavaVersionInfo javaVersionInfo;
    private final Path clientCode;

    private final Logger log = LoggerFactory.getLogger(RepairJavaVersionIncompatibility.class);


    public RepairJavaVersionIncompatibility(JavaVersionInfo javaVersionInfo, Path clientCode) {
        this.javaVersionInfo = javaVersionInfo;
        this.clientCode = clientCode;
    }


    public void repair() {
        DockerBuild dockerBuild = new DockerBuild();
        JavaVersionIncompatibility incompatibility = javaVersionInfo.getIncompatibility();

        try {
            // create a base image for breaking update with the project code
            String dockerImageName = dockerBuild.createBaseImageForBreakingUpdate(clientCode, javaVersionInfo.getIncompatibility().mapVersions(incompatibility.wrongVersion()));
            //reproduce the breaking update in the new java version
            Result result = dockerBuild.reproduce(dockerImageName, FailureCategory.JAVA_VERSION_FAILURE, clientCode.getFileName().toString());

            log.info("Reproduction result: {}", result);

        } catch (IOException e) {
            log.error("Error creating base image for breaking update.", e);
            throw new RuntimeException(e);
        }
    }


}
