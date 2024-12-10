package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.SetupPipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StoreInfo {

    Logger log = LoggerFactory.getLogger(StoreInfo.class);

    private final SetupPipeline setupPipeline;
    private String patchNumber;
    Path patchFolder;

    public StoreInfo(SetupPipeline setupPipeline) {
        this.setupPipeline = setupPipeline;
        patchFolder = checkPatchFolder();

    }

    private Path checkPatchFolder() {
        if (!Files.exists(setupPipeline.getOutPutPatchFolder())) {
            // Create the folder
            patchNumber = "1";
        } else {
            // Get the number of patches in the folder
            // Increment the number of patches
            // Create the new patch folder
            try {
                long patchCount = Files.list(setupPipeline.getOutPutPatchFolder().resolve(setupPipeline.getBreakingUpdate().breakingCommit).resolve("patches")).count();
                patchNumber = String.valueOf(patchCount + 1);
            } catch (IOException e) {
                log.error("Error creating the patch folder", e);
                throw new RuntimeException(e);
            }
            try {
                return Files.createDirectory(setupPipeline.getOutPutPatchFolder().resolve(setupPipeline.getBreakingUpdate().breakingCommit).resolve("patches").resolve("patch" + patchNumber));
            } catch (IOException e) {
                log.error("Error creating the patch folder", e);
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public void copyResponseToFile(String fileName, String content) {
        // Write the content to the file
        try {
            Path file = this.patchFolder.resolve(fileName);
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
