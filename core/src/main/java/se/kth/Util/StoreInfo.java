package se.kth.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.model.SetupPipeline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@lombok.Getter
@lombok.Setter
public class StoreInfo {

    Logger log = LoggerFactory.getLogger(StoreInfo.class);

    private final SetupPipeline setupPipeline;
    private String patchNumber;
    Path patchFolder;

    public StoreInfo(SetupPipeline setupPipeline, Path patchFolder) {
        this.setupPipeline = setupPipeline;
        this.patchFolder = patchFolder;
        try {
            long patchCount = Files.list(patchFolder.getParent()).count() - 1;
            patchNumber = String.valueOf(patchCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StoreInfo(SetupPipeline setupPipeline, boolean setup) {
        this.setupPipeline = setupPipeline;
        if (setup) {
            this.patchFolder = checkPatchFolder();
        } else
            patchFolder = setupPipeline.getOutPutPatchFolder();
    }

    private Path checkPatchFolder() {
        Path patchesPath = setupPipeline.getOutPutPatchFolder()
                .resolve(setupPipeline.getBreakingUpdate().breakingCommit)
                .resolve("patches");

        if (!Files.exists(patchesPath)) {
            // Create the folder
            patchNumber = "1";
            try {
                Files.createDirectories(patchesPath); // Ensure parent directories exist
                return Files.createDirectory(patchesPath.resolve("patch" + patchNumber));
            } catch (IOException e) {
                log.error("Error creating the patch folder", e);
                throw new RuntimeException(e);
            }
        } else {
            // Get the number of patches in the folder
            // Increment the number of patches
            // Create the new patch folder
            try {
                long patchCount = Files.list(patchesPath).count();
                patchNumber = String.valueOf(patchCount + 1);
            } catch (IOException e) {
                log.error("Error creating the patch folder", e);
                throw new RuntimeException(e);
            }
            try {
                return Files.createDirectory(patchesPath.resolve("patch" + patchNumber));
            } catch (IOException e) {
                log.error("Error creating the patch folder", e);
                throw new RuntimeException(e);
            }
        }
    }

    public Path copyContentToFile(String targetFile, String content) {
        // Write the content to the file
        try {
            Path file = this.patchFolder.resolve(targetFile);
            // ensure the parent directory exists
            if (!Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this.patchFolder.resolve(targetFile);
    }

    public boolean executeDiffCommand(String originalFile, String patchFile, Path outputFile) {
        try {
            if (!Files.exists(outputFile.getParent())) {
                Files.createDirectory(outputFile.getParent());
            }

            // Build the diff command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "diff", "-w", "-t", originalFile, patchFile);

            // Redirect the output to the specified file
            processBuilder.redirectOutput(new File(outputFile.toString()));

            // Start the process
            Process process = processBuilder.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Check the exit code for success
            if (exitCode == 0) {
                log.info("Files compared successfully.");
            } else if (exitCode == 1) {
                log.info("Files differ. Differences saved to: {}", outputFile);
            } else {
                log.error("Error while executing the diff command. Exit code: {}", exitCode);
            }
            // return if the file is empty or not
            return Files.size(outputFile) > 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void storeFilesErrors(String stage, Map<String, Set<DetectedFileWithErrors>> listOfFilesWithErrors) {

        Path filesFolder = Path.of(this.patchFolder.toString(), "files");
        if (!Files.exists(filesFolder)) {
            try {
                Files.createDirectories(filesFolder);
            } catch (IOException e) {
                log.error("Error creating files folder", e);
                throw new RuntimeException(e);
            }
        }

        Path processErrorFilesFolder = Path.of(filesFolder.toString(), stage + "_Files.txt");
        try {
            Files.write(processErrorFilesFolder,
                    listOfFilesWithErrors.keySet().stream()
                            .map(Object::toString)
                            .collect(Collectors.toList()));

            Path errorFolder = this.patchFolder.resolve("errors/" + stage);
            if (!Files.exists(errorFolder)) {
                Files.createDirectories(errorFolder);
            }
            for (Map.Entry<String, Set<DetectedFileWithErrors>> entry : listOfFilesWithErrors.entrySet()) {
                Set<String> uniqueMessage = new HashSet<>();
                for (DetectedFileWithErrors errorFile : entry.getValue()) {
                    Path errorFilePath = errorFolder.resolve(Path.of(entry.getKey()).getFileName() + ".txt");
                    String errormessage = errorFile.getErrorInfo().getErrorMessage();
                    errormessage = errormessage.replaceAll("\\[ERROR\\] .*:\\[\\d+,\\d+\\] ", "");
                    errormessage = errormessage + errorFile.getErrorInfo().getAdditionalInfo().replace("\n", " ");
                    if (!uniqueMessage.contains(errorFile.getErrorInfo().getClientLinePosition() + ":" + errormessage + "\n")) {
                        uniqueMessage.add(errorFile.getErrorInfo().getClientLinePosition() + ":" + errormessage + "\n");
                        Files.writeString(errorFilePath,
                                errorFile.getErrorInfo().getClientLinePosition() + ":" + errormessage + "\n",
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }

                }
            }
        } catch (IOException e) {
            log.error("Error writing {} error files", stage, e);
            throw new RuntimeException(e);
        }
    }

}