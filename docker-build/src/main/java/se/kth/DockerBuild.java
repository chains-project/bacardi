package se.kth;

import com.github.dockerjava.api.DockerClient;

import java.util.logging.Logger;

public class DockerBuild {

    Logger logger = Logger.getLogger(DockerBuild.class.getName());
    private static DockerClient dockerClient;
    private static final int EXIT_CODE = 0;


    /**
     *  Method to remove Docker container
     * @param containerId - container id
     * @return boolean - true if container is removed successfully, false otherwise
     */
    public boolean removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            logger.warning("Failed to remove container with id: " + containerId);
            return false;
        }
        logger.info("Container with id: " + containerId + " removed successfully");
        return true;
    }

}
