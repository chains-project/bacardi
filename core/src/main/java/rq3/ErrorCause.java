package rq3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Attempt;
import se.kth.Result;
import se.kth.Util.JsonUtils;
import se.kth.models.MavenErrorLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static se.kth.Util.BreakingUpdateProvider.getPreviousResults;
import static se.kth.Util.BreakingUpdateProvider.readJavaVersionIncompatibilities;
import static se.kth.Util.Constants.*;

public class ErrorCause {

    private static final Logger log = LoggerFactory.getLogger(ErrorCause.class);

    private static final String RESULTS_PATH = "results";
    private static final String OUTPUT_RQ3 = "result_rq3";

    static final ArrayList<String> listOfJavaVersionIncompatibilities = readJavaVersionIncompatibilities(JAVA_VERSION_INCOMPATIBILITY_FILE);

    static final ArrayList<String> listOfApiDiff = readJavaVersionIncompatibilities(APIDIFF_FILE);

    public static void main(String[] args) throws IOException {


        if (!Files.exists(Path.of(OUTPUT_PATH))) {
            // create file
            Files.createDirectories(Path.of(OUTPUT_PATH));
        }

        getErrorCause();

    }

    private static void getErrorCause() throws IOException {


        File rootDirectory = new File(RESULTS_PATH);

        Map<String, Map<String, Map<String, Integer>>> totalErrorsPerModel = new HashMap<>();

        // Get all subdirectories in the root directory
        if (rootDirectory.exists() && rootDirectory.isDirectory()) {
            File[] promptFolders = rootDirectory.listFiles(File::isDirectory);
            final var path = Path.of(OUTPUT_RQ3);
            if (!Files.exists(path)) {
                // create file
                Files.createDirectory(path);
            }

            if (promptFolders != null) {
                for (File promptFolder : promptFolders) {
                    // Get all subdirectories in the prompt folder


                    if (!Files.exists(path.resolve(promptFolder.getName()))) {
                        // create file
                        Files.createDirectory(path.resolve(promptFolder.getName()));
                    }

                    File[] modelFolders = promptFolder.listFiles(File::isDirectory);

                    if (modelFolders != null) {
                        Map<String, Map<String, Integer>> modelErrorCounts = new HashMap<>();

                        for (File files : modelFolders) {
                            // Get all  subdirectories in the project folder
                            if (Files.notExists(files.toPath())) {
                                // create file
                                Files.createDirectory(files.toPath());
                            }

                            String jsonResults = files.toPath().resolve("result_repair_".concat(files.getName()).concat(".json")).toString();
                            Map<String, Result> resultsMap = getPreviousResults(jsonResults);
                            // Read the JSON file and convert it to a BreakingUpdate object
                            Map<String, CauseInformation> commitErrorCause = findErrorPerModel(jsonResults, resultsMap);


                            int breakingChangesCount = commitErrorCause.values().stream()
                                    .mapToInt(cause -> cause.getByBreakingChanges().size())
                                    .sum();
                            int llmChangesCount = commitErrorCause.values().stream()
                                    .mapToInt(cause -> cause.getByLLMs().size())
                                    .sum();

                            Map<String, Integer> errorCounts = new HashMap<>();
                            errorCounts.put("breakingChanges", breakingChangesCount);
                            errorCounts.put("llmChanges", llmChangesCount);
                            modelErrorCounts.put(files.getName(), errorCounts);

                            // Print the breaking update details
                            String jsonOutput = path.resolve(promptFolder.getName()).resolve(files.getName().concat(".json")).toString();
//
                            final var jsonOutputPath = Path.of(jsonOutput);
//                            Write results in json file
                            if (!Files.exists(jsonOutputPath)) {
                                // create file
                                try {
                                    Files.createFile(jsonOutputPath);
                                } catch (IOException e) {
                                    log.error("Error creating the JSON output file: {}", e.getMessage(), e);
                                }
                            }
                            JsonUtils.writeToFile(jsonOutputPath, commitErrorCause);
//                                    });
                        }
                        totalErrorsPerModel.put(promptFolder.getName(), modelErrorCounts);
                    }
                }

                String totalErrorsJsonPath = path.resolve("total_errors.json").toString();
                final var totalErrorsOutputPath = Path.of(totalErrorsJsonPath);

                if (!Files.exists(totalErrorsOutputPath)) {
                    try {
                        Files.createFile(totalErrorsOutputPath);
                    } catch (IOException e) {
                        log.error("Error creating the total errors JSON file: {}", e.getMessage(), e);
                    }
                }
                JsonUtils.writeToFile(totalErrorsOutputPath, totalErrorsPerModel);
            }
        }

    }

    /**
     * Finds the error per model in the given JSON file.
     *
     * @param jsonPath
     * @return
     */
    private static Map<String, CauseInformation> findErrorPerModel(String jsonPath, Map<String, Result> resultsMap) {
        Map<String, CauseInformation> errorCause = new HashMap<>();


        resultsMap.forEach((key, value) -> {
            //looking into the commit
            if (listOfApiDiff.contains(key)) {
                Attempt attempt = value.getAttempts().getFirst();
                int newErrorsAmount = attempt.getNewErrors();
                if (newErrorsAmount > 0) {

                    Map<String, ArrayList<String>> prefixErrorsMap = new HashMap<String, ArrayList<String>>();
                    processErrors("prefix", prefixErrorsMap, attempt.outputFolder());
                    Map<String, ArrayList<String>> postfixErrorsMap = new HashMap<String, ArrayList<String>>();
                    processErrors("postfix", postfixErrorsMap, attempt.outputFolder());
                    Map<String, List<String>> newErrorList = getNewErrors(prefixErrorsMap, postfixErrorsMap);
                    if (!newErrorList.isEmpty()) {
                        //compare lines
                        CauseInformation cause = LLMOrBreakingChange(newErrorList, jsonPath, key);
                        errorCause.put(key, cause);
                    }
                }
            }
        });
        return errorCause;
    }

    private static int countNumberError(MavenErrorLog errorLog) {
        AtomicInteger count = new AtomicInteger();
        errorLog.getErrorInfo().forEach((key, value) -> {
            count.addAndGet(value.size());
        });

        return count.get();
    }

    public static Map<String, List<String>> getNewErrors(Map<String, ArrayList<String>> prefixErrorsMap,
                                                         Map<String, ArrayList<String>> postfixErrorsMap) {
        Map<String, List<String>> newErrorsMap = new HashMap<>();

        for (Map.Entry<String, ArrayList<String>> entry : postfixErrorsMap.entrySet()) {
            String fileName = entry.getKey();
            ArrayList<String> postfixErrors = new ArrayList<>(entry.getValue());
            ArrayList<String> prefixErrors = new ArrayList<>(prefixErrorsMap.getOrDefault(fileName, new ArrayList<>()));

            // Sort prefixErrors and postfixErrors by line number
            prefixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));
            postfixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));

            List<String> newErrors = new ArrayList<>();

            if (prefixErrors.isEmpty() && !postfixErrors.isEmpty()) {
                newErrors.addAll(postfixErrors);
            } else {
                for (String postfixError : postfixErrors) {
                    String[] postfixParts = postfixError.split(":", 2);
                    int postfixLine = Integer.parseInt(postfixParts[0]);
                    String postfixMessage = postfixParts[1];

                    boolean isNew = true;
                    for (int i = 0; i < prefixErrors.size(); i++) {
                        String prefixError = prefixErrors.get(i);
                        String[] prefixParts = prefixError.split(":", 2);
                        int prefixLine = Integer.parseInt(prefixParts[0]);
                        String prefixMessage = prefixParts[1];

                        if (Math.abs(prefixLine - postfixLine) <= 3 && postfixMessage.equals(prefixMessage)) {
                            isNew = false;
                            prefixErrors.remove(i); // Remove matched error from prefixErrors
                            break;
                        }
                    }

                    if (isNew) {
                        newErrors.add(postfixError);
                    }
                }
            }

            if (!newErrors.isEmpty()) {
                newErrorsMap.put(fileName, newErrors);
            }
        }

        return newErrorsMap;
    }

    private static CauseInformation LLMOrBreakingChange(Map<String, List<String>> resultsMap, String jsonPath, String commit) {
        Path patch = Path.of(Path.of(jsonPath).getParent().toString(), commit, "patches", "patch1");
        ArrayList<BreakingInfo> breakingChangesError = new ArrayList<>();
        ArrayList<BreakingInfo> llmChangesError = new ArrayList<>();

        resultsMap.forEach((key, value) -> {
            //Get each error per file
            Path originalFile = patch.resolve("original").resolve(key.replace(".txt", ""));
            Path patchFile = patch.resolve("updated").resolve(key.replace(".txt", ""));
            // iterate over each new error and analyze if belong to LLM or breaking change
            value.forEach(error -> {
                int lineNumber = Integer.parseInt(error.split(":")[0]);
                BreakingInfo info = null;
                // Get the line from the error message
                if (!Files.exists(originalFile)) {
//                    String newBuggyLine = getBuggyLine(error, originalFile);
                    info = new BreakingInfo(lineNumber, "New File", patchFile.toString());
                    breakingChangesError.add(info);
                } else {
                    String newBuggyLine = getBuggyLine(error, patchFile);
//                    System.out.printf("New Buggy Line: number: %s, line: %s ", lineNumber, newBuggyLine);
//                    System.out.println("------------------------");
                    // Store info in the BreakingInfo object about the error
                    info = new BreakingInfo(lineNumber, newBuggyLine, patchFile.toString());
                    // Check if the line is a breaking change or LLM
                    boolean isBreakingChange = originalFileContainNewBreakingLine(info, originalFile);
                    if (isBreakingChange) {
                        breakingChangesError.add(info);
                    } else {
                        llmChangesError.add(info);
                    }
                }
            });
        });

        CauseInformation causeInformation = new CauseInformation(
                breakingChangesError,
                llmChangesError
        );
        return causeInformation;

    }

    public static int processErrors(String stage, Map<String, ArrayList<String>> errorsMap, String outputFolder) {
        int count = 0;
        Path errorFolder = Path.of(outputFolder, "errors", stage);
        if (Files.exists(errorFolder)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(errorFolder)) {
                for (Path file : stream) {
                    ArrayList<String> errorsList = new ArrayList<>();
                    try (BufferedReader br = Files.newBufferedReader(file)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorsList.add(line);
                            count++;
                        }
                    } catch (IOException e) {
                        log.error("Error reading file: {}", file, e);
                    }
                    errorsMap.put(file.getFileName().toString(), errorsList);
                }
            } catch (IOException e) {
                log.error("Error reading directory: {}", errorFolder, e);
            }
        }
        return count;
    }

    private static boolean originalFileContainNewBreakingLine(BreakingInfo info, Path originalFile) {
        //read all lines in the original file
        ArrayList<String> originalLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(originalFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                originalLines.add(line.strip());
            }
        } catch (IOException e) {
            log.error("Error reading the original file: {}", e.getMessage(), e);
        }
        // contains the line in the original file
        return originalLines.contains(info.codeLine());
    }

    private static String getBuggyLine(String error, Path filePath) {
        String lineNumber = error.split(":")[0];

        if (!Files.exists(filePath)) {
            log.error("File does not exist: {}", filePath);
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String currentLine;
            int currentLineNumber = 0;

            while ((currentLine = reader.readLine()) != null) {
                currentLineNumber++;
                if (currentLineNumber == Integer.parseInt(lineNumber)) {
                    return currentLine.strip();
                }
            }
        } catch (IOException e) {
            log.error("Error reading the file: {}", e.getMessage(), e);
        }
        return "";
    }

    public static List<File> getFilesInDirectory(String directoryPath) {
        List<File> files = new ArrayList<>();
        Path dirPath = Path.of(directoryPath);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                files.add(path.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading directory: " + directoryPath, e);
        }

        return files;
    }


}

