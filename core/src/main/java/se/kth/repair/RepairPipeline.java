package se.kth.repair;

public interface RepairPipeline {

    /**
     * Repair the project
     *
     * @param dockerImage The docker image to use
     * @param projectPath The path to the project
     * @param logFilePath The path to the log file
     */
    public abstract void repair(String dockerImage, String projectPath, String logFilePath);
}
