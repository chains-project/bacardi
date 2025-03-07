package de.uniwue.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.uniwue.config.ProjectConfiguration;
import de.uniwue.feature.ProcessConflictDetector;
import de.uniwue.feature.ProcessHandler;
import de.uniwue.feature.ProcessStateCollector;

/**
 * Helper class for recognition module
 */
public class RecognitionHelper {
    // ... (rest of the class remains unchanged)

    /**
     * Lists all available Models from the model directory
     * Consider the subsequent information to load models correctly
     *
     * Possible model location directories:
     * ProjectConfiguration.PROJ_MODEL_DEFAULT_DIR
     * ProjectConfiguration.PROJ_MODEL_CUSTOM_DIR
     *
     * Model path structures on the filesystem:
     * Default: OS_PATH/{TRAINING_IDENTIFIER}/{ID}.ckpt.json
     * Custom:  OS_PATH/{PROJECT_NAME}/{TRAINING_IDENTIFIER}/{ID}.ckpt.json
     *
     * Example: /var/ocr4all/models/default/Baiter_000/Baiter.ckpt.json
     * Display: Baiter_000/Baiter
     * Example: /var/ocr4all/models/custom/Bibel/0/0.ckpt.json
     * Display: Bibel/0/0
     *
     * The models need to be in the following structure:
     * ANY_PATH/{MODEL_NAME}/ANY_NAME.ckpt.json
     *
     * @return Map of models (key = modelName | value = path)
     * @throws IOException
     */
    public static TreeMap<String, String> listModels() throws IOException {
        TreeMap<String, String> models = new TreeMap<>();

        File modelsDir = new File(ProjectConfiguration.PROJ_MODEL_DIR);
        if (!modelsDir.exists())
            return models;

        // Add all models to map (follow symbolic links on the filesystem due to Docker container)
        try (var stream = Files.walk(Paths.get(ProjectConfiguration.PROJ_MODEL_DIR), FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isRegularFile)
                  .forEach(fileEntry -> {
                      // Remove OS path and model extension from display string (only display significant information)
                      String modelName = fileEntry.toString();
                      modelName = modelName.replace(ProjectConfiguration.PROJ_MODEL_DEFAULT_DIR, "");
                      modelName = modelName.replace(ProjectConfiguration.PROJ_MODEL_CUSTOM_DIR, "");
                      modelName = modelName.replace(ProjectConfiguration.MODEL_EXT, "");

                      models.put(modelName, fileEntry.toString());
                  });
        }

        return models;
    }

    // ... (rest of the class remains unchanged)
}