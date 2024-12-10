package se.kth.model;


import se.kth.DockerBuild;

import java.nio.file.Path;

@lombok.Getter
@lombok.Setter
@lombok.ToString

public class SetupPipeline {

    String dockerImage;
    Path clientFolder;
    Path logFilePath;
    String branch;
    Path m2FolderPath;
    DockerBuild dockerBuild;
    BreakingUpdate breakingUpdate;
    Path outPutPatchFolder;


    public SetupPipeline() {

    }






}
