package se.kth.model;

import se.kth.ApptainerBuild;

import java.nio.file.Path;

@lombok.Getter
@lombok.Setter
@lombok.ToString

public class SetupPipeline {

    String containerImage;
    Path clientFolder;
    Path logFilePath;
    String branch;
    Path m2FolderPath;
    ApptainerBuild containerBuild;
    BreakingUpdate breakingUpdate;
    Path outPutPatchFolder;
    String libraryName;
    String baseVersion;
    String newVersion;

    public SetupPipeline() {

    }

}
