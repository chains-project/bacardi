package se.kth.Util;

import com.fasterxml.jackson.databind.type.MapType;
import com.github.dockerjava.api.command.CreateContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.Result;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;

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

import static se.kth.Util.FileUtils.getFilesInDirectory;


public class BreakingUpdateProvider {


    static Logger log = LoggerFactory.getLogger(DockerBuild.class);

    /**
     * Retrieves a list of BreakingUpdate objects from the specified directory, filtered by the given category.
     *
     * @param directory the directory to search for files
     * @param category  the category to filter the BreakingUpdate objects
     * @return a list of BreakingUpdate objects that match the specified category
     */
    public static List<BreakingUpdate> getBreakingUpdatesFromResourcesByCategory(String directory,
                                                                                 FailureCategory category) {
        List<File> files = getFilesInDirectory(directory);
        return files.stream()
                .map(File::toPath)
                .map(e -> JsonUtils.readFromFile(e, BreakingUpdate.class))
                .filter(breakingUpdate -> breakingUpdate.failureCategory == category)
                .toList();
    }


    /**
     * Retrieves a list of BreakingUpdate objects from the specified directory, filtered by the given category and additional filter.
     *
     * @param directory the directory to search for files
     * @param category  the category to filter the BreakingUpdate objects
     * @param filter    an additional filter to apply to the results, such as a list of breaking commits from Bump
     * @return a list of BreakingUpdate objects that match the specified category and filter
     */
    public static List<BreakingUpdate> getBreakingUpdatesFromResourcesByCategory(String directory,
                                                                                 FailureCategory category, ArrayList<String> filter) {
        List<File> files = getFilesInDirectory(directory);
        return files.stream()
                .map(File::toPath)
                .map(e -> JsonUtils.readFromFile(e, BreakingUpdate.class))
                .filter(breakingUpdate -> breakingUpdate.failureCategory == category)
                .filter(breakingUpdate -> filter.contains(breakingUpdate.breakingCommit))
                .toList();
    }


    public static ArrayList<String> readLinesFromFile(String filePath) {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            log.error("Error reading file: {}", e.getMessage(), e);

        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.error("Error closing file: {}", e.getMessage(), e);
            }
        }

        return lines;
    }


    public static ArrayList<String> readJavaVersionIncompatibilities(String filePath) {
        //enable filter for only specific breaking update category
        return readLinesFromFile(filePath);
    }

    public static Map<String, Result> getPreviousResults(String jsonPath) {
        final var path = Path.of(jsonPath);
        MapType jsonType = JsonUtils.getTypeFactory().constructMapType(Map.class, String.class, Result.class);
        Map<String, Result> resultsMap = new HashMap<>();


        if (!Files.exists(path)) {
            try {
                //create empty file
                Files.createFile(path);
                JsonUtils.writeToFile(path, resultsMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        resultsMap.putAll(JsonUtils.readFromFile(path, jsonType));

        return resultsMap;

    }

    public static String getProjectData(String imageId, DockerBuild dockerBuild, Path toLocal, Path m2InContainer, Path projectInContainer, Path jar) {

        try {

            String[] entrypoint = new String[]{"/bin/sh","sleep","1000000"};

            //pull image
            dockerBuild.ensureBaseMavenImageExists(imageId);
            CreateContainerResponse container = dockerBuild.startContainerEntryPoint(imageId, entrypoint);
            // Copy m2 folder to local path in the breaking commit folder and project folder
            if (m2InContainer != null) {
                dockerBuild.copyM2FolderToLocalPath(container.getId(), m2InContainer, toLocal.resolve("m2"));
                dockerBuild.copyM2FolderToLocalPath(container.getId(), projectInContainer, toLocal);
            }
            if (jar != null) {
                // Copy the jar file to the local path
                dockerBuild.copyFromContainer(container.getId(), jar.toString(), toLocal.resolve(jar.getFileName()));
            }
            // Copy the jar file to the local path

            dockerBuild.removeContainer(container.getId());
            return imageId;
        } catch (InterruptedException e) {
            log.error("Something went wrong", e);
            throw new RuntimeException(e);
        }
    }

}
