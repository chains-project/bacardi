package se.kth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import se.kth.models.FailureCategory;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Attempt(@Getter int index, @Getter FailureCategory failureCategory, @Getter int prefixFiles,
        @Getter int postfixFiles, @Getter int fixedFiles, @Getter int unfixedFiles, @Getter int newFiles,
        @Getter int prefixErrors, @Getter int postfixErrors, @Getter int fixedErrors, @Getter int unfixedErrors,
        @Getter int newErrors, @Getter String outputFolder, @Getter boolean successful) {

    @JsonCreator
    public Attempt(
            @JsonProperty("index") int index,
            @JsonProperty("failureCategory") FailureCategory failureCategory,
            @JsonProperty("outputFolder") String outputFolder,
            @JsonProperty("successful") boolean successful) {
        this(index, failureCategory, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, outputFolder, successful);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public Attempt(int index, FailureCategory failureCategory, int prefixFiles, int postfixFiles, int fixedFiles,
            int unfixedFiles, int newFiles, int prefixErrors, int postfixErrors, int fixedErrors, int unfixedErrors,
            int newErrors, String outputFolder, boolean successful) {
        this.index = index;
        this.failureCategory = failureCategory;
        ArrayList<String> prefixFilesList = new ArrayList<String>();
        ArrayList<String> postfixFilesList = new ArrayList<String>();
        this.outputFolder = outputFolder;
        this.successful = successful;
        this.prefixFiles = processFiles("prefix", prefixFilesList);
        this.postfixFiles = processFiles("postfix", postfixFilesList);
        this.fixedFiles = processFixedFiles(prefixFilesList, postfixFilesList);
        this.unfixedFiles = processUnfixedFiles(prefixFilesList, postfixFilesList);
        this.newFiles = processNewFiles(prefixFilesList, postfixFilesList);
        Map<String, ArrayList<String>> prefixErrorsMap = new HashMap<String, ArrayList<String>>();
        Map<String, ArrayList<String>> postfixErrorsMap = new HashMap<String, ArrayList<String>>();
        this.prefixErrors = processErrors("prefix", prefixErrorsMap);
        this.postfixErrors = processErrors("postfix", postfixErrorsMap);
        this.fixedErrors = processFixedErrors(prefixErrorsMap, postfixErrorsMap);
        this.unfixedErrors = processUnfixedErrors(prefixErrorsMap, postfixErrorsMap);
        this.newErrors = processNewErrors(prefixErrorsMap, postfixErrorsMap);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public int processFiles(String stage, ArrayList<String> filesList) {
        int count = 0;
        Path fileFolder = Path.of(outputFolder, "files");
        if (fileFolder.toFile().exists()) {
            Path stagefixFilePath = Path.of(outputFolder, "files", stage + "_Files.txt");
            if (stagefixFilePath.toFile().exists()) {

                try (BufferedReader br = Files.newBufferedReader(stagefixFilePath)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        filesList.add(line);
                        count++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return count;
    }

    public int processFixedFiles(ArrayList<String> prefixFilesList, ArrayList<String> postfixFilesList) {
        int fixedFilesCount = 0;
        for (String file : prefixFilesList) {
            if (!postfixFilesList.contains(file)) {
                fixedFilesCount++;
            }
        }
        return fixedFilesCount;
    }

    public int processUnfixedFiles(ArrayList<String> prefixFilesList, ArrayList<String> postfixFilesList) {
        int unfixedFilesCount = 0;
        for (String file : prefixFilesList) {
            if (postfixFilesList.contains(file)) {
                unfixedFilesCount++;
            }
        }
        return unfixedFilesCount;
    }

    public int processNewFiles(ArrayList<String> prefixFilesList, ArrayList<String> postfixFilesList) {
        int newFilesCount = 0;
        for (String file : postfixFilesList) {
            if (!prefixFilesList.contains(file)) {
                newFilesCount++;
            }
        }
        return newFilesCount;
    }

    public int processErrors(String stage, Map<String, ArrayList<String>> errorsMap) {
        int count = 0;
        Path errorFolder = Path.of(outputFolder, "errors", stage);
        if (Files.exists(errorFolder)) {
            try {
                for (Path file : Files.newDirectoryStream(errorFolder)) {
                    ArrayList<String> errorsList = new ArrayList<>();
                    try (BufferedReader br = Files.newBufferedReader(file)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorsList.add(line);
                            count++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    errorsMap.put(file.getFileName().toString(), errorsList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return count;

    }

    public int processFixedErrors(Map<String, ArrayList<String>> prefixErrorsMap,
            Map<String, ArrayList<String>> postfixErrorsMap) {
        int fixedErrorsCount = 0;

        for (Map.Entry<String, ArrayList<String>> entry : prefixErrorsMap.entrySet()) {
            String fileName = entry.getKey();
            ArrayList<String> prefixErrors = new ArrayList<>(entry.getValue());
            ArrayList<String> postfixErrors = new ArrayList<>(
                    postfixErrorsMap.getOrDefault(fileName, new ArrayList<>()));

            // Sort prefixErrors and postfixErrors by line number
            prefixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));
            postfixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));

            for (String prefixError : prefixErrors) {
                String[] prefixParts = prefixError.split(":", 2);
                int prefixLine = Integer.parseInt(prefixParts[0]);
                String prefixMessage = prefixParts[1];

                boolean isFixed = true;
                for (int i = 0; i < postfixErrors.size(); i++) {
                    String postfixError = postfixErrors.get(i);
                    String[] postfixParts = postfixError.split(":", 2);
                    int postfixLine = Integer.parseInt(postfixParts[0]);
                    String postfixMessage = postfixParts[1];

                    if (Math.abs(prefixLine - postfixLine) <= 3 && prefixMessage.equals(postfixMessage)) {
                        isFixed = false;
                        postfixErrors.remove(i); // Remove matched error from postfixErrors
                        break;
                    }
                }

                if (isFixed) {
                    fixedErrorsCount++;
                }
            }
        }
        return fixedErrorsCount;
    }

    public int processUnfixedErrors(Map<String, ArrayList<String>> prefixErrorsMap,
            Map<String, ArrayList<String>> postfixErrorsMap) {
        int unfixedErrorsCount = 0;

        for (Map.Entry<String, ArrayList<String>> entry : prefixErrorsMap.entrySet()) {
            String fileName = entry.getKey();
            ArrayList<String> prefixErrors = new ArrayList<>(entry.getValue());
            ArrayList<String> postfixErrors = new ArrayList<>(
                    postfixErrorsMap.getOrDefault(fileName, new ArrayList<>()));

            // Sort prefixErrors and postfixErrors by line number
            prefixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));
            postfixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));

            for (String prefixError : prefixErrors) {
                String[] prefixParts = prefixError.split(":", 2);
                int prefixLine = Integer.parseInt(prefixParts[0]);
                String prefixMessage = prefixParts[1];

                boolean isFixed = true;
                for (int i = 0; i < postfixErrors.size(); i++) {
                    String postfixError = postfixErrors.get(i);
                    String[] postfixParts = postfixError.split(":", 2);
                    int postfixLine = Integer.parseInt(postfixParts[0]);
                    String postfixMessage = postfixParts[1];

                    if (Math.abs(prefixLine - postfixLine) <= 3 && prefixMessage.equals(postfixMessage)) {
                        isFixed = false;
                        postfixErrors.remove(i); // Remove matched error from postfixErrors
                        break;
                    }
                }

                if (!isFixed) {
                    unfixedErrorsCount++;
                }
            }
        }
        return unfixedErrorsCount;
    }

    public int processNewErrors(Map<String, ArrayList<String>> prefixErrorsMap,
                                 Map<String, ArrayList<String>> postfixErrorsMap) {
        int newErrorsCount = 0;

        for (Map.Entry<String, ArrayList<String>> entry : postfixErrorsMap.entrySet()) {
            String fileName = entry.getKey();
            ArrayList<String> postfixErrors = new ArrayList<>(entry.getValue());
            ArrayList<String> prefixErrors = new ArrayList<>(prefixErrorsMap.getOrDefault(fileName, new ArrayList<>()));

            // Sort prefixErrors and postfixErrors by line number
            prefixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));
            postfixErrors.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1.split(":")[0]),
                    Integer.parseInt(e2.split(":")[0])));

            if (prefixErrors.isEmpty() && !postfixErrors.isEmpty()) {
                newErrorsCount += postfixErrors.size();
            }

            else {
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
                        newErrorsCount++;
                    }
                }
            }

        }

        return newErrorsCount;

    }
}