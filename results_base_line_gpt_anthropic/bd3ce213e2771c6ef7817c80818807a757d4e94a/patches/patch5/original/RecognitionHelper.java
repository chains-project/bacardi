package de.uniwue.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.core.exc.StreamWriteException; // Add this import

import de.uniwue.config.ProjectConfiguration;
import de.uniwue.feature.ProcessConflictDetector;
import de.uniwue.feature.ProcessHandler;
import de.uniwue.feature.ProcessStateCollector;

/**
 * Helper class for recognition module
 */
public class RecognitionHelper {
    /**
     * Object to access project configuration
     */
    private ProjectConfiguration projConf;

    /**
     * Image type of the project
     * Possible values: { Binary, Gray }
     */
    private String projectImageType;

    /**
     * Object to use generic functionalities
     */
    private GenericHelper genericHelper;

    /**
     * Object to determine process states
     */
    private ProcessStateCollector procStateCol;

    /**
     * Helper object for process handling
     */
    private ProcessHandler processHandler;

    /**
     * Progress of the Recognition process
     */
    private int progress = -1;

    /**
     * Indicates if a Recognition process is already running
     */
    private boolean RecognitionRunning = false;

    /**
     * Last time the images/pagexml are modified
     */
    private Map<String, Long> imagesLastModified;

    /**
     * Structure to monitor the progress of the process
     * pageId : segmentId : lineSegmentId : processedState
     *
     * Structure example:
     * {
     *     "0002": {
     *         "0002__000__paragraph" : {
     *             "0002__000__paragraph__000" : true,
     *             "0002__000__paragraph__001" : false,
     *             ...
     *         },
     *         ...
     *     },
     *     ...
     * }
     */
    private TreeMap<String, TreeMap<String, TreeMap<String, Boolean>>> processState = new TreeMap<>();

    /**
     * Constructor
     *
     * @param projectDir Path to the project directory
     * @param projectImageType Type of the project (binary, gray)
     *
     */
    public RecognitionHelper(String projectDir, String projectImageType) {
        this.projectImageType = projectImageType;
        projConf = new ProjectConfiguration(projectDir);
        genericHelper = new GenericHelper(projConf);
        procStateCol = new ProcessStateCollector(projConf, projectImageType);
        processHandler = new ProcessHandler();
    }

    // ... (other methods remain unchanged)

    /**
     * Executes OCR on a list of pages
     * Achieved with the help of the external python program "calamary-predict"
     *
     * @param pageIds Identifiers of the pages (e.g 0002,0003)
     * @param cmdArgs Command line arguments for "calamary-predict"
     * @throws IOException
     */
    public void execute(List<String> pageIds, final List<String> cmdArgs) throws IOException {
        RecognitionRunning = true;
        progress = 0;

        List<String> cmdArgsWork = new ArrayList<>(cmdArgs);

        //// Estimate Skew
        if (cmdArgsWork.contains("--estimate_skew")) {
            // Calculate the skew of all regions where none was calculated before
            List<String> skewparams = new ArrayList<>();
            skewparams.add("skewestimate");
            final int maxskewIndex = cmdArgsWork.indexOf("--maxskew");
            if (maxskewIndex > -1) {
                skewparams.add(cmdArgsWork.remove(maxskewIndex));
                skewparams.add(cmdArgsWork.remove(maxskewIndex));
            }
            final int skewstepsIndex = cmdArgsWork.indexOf("--skewsteps");
            if (skewstepsIndex > -1) {
                skewparams.add(cmdArgsWork.remove(skewstepsIndex));
                skewparams.add(cmdArgsWork.remove(skewstepsIndex));
            }

            // Create temp json file with all segment images (to not overload parameter list)
            // Temp file in a temp folder named "skew-<random numbers>.json"
            File segmentListFile = File.createTempFile("skew-", ".json");
            skewparams.add(segmentListFile.toString());
            segmentListFile.deleteOnExit(); // Delete if OCR4all terminates
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode dataList = mapper.createArrayNode();
            for (String pageId : pageIds) {
                ArrayNode pageList = mapper.createArrayNode();
                pageList.add(projConf.getImageDirectoryByType(projectImageType) + pageId +
                        projConf.getImageExtensionByType(projectImageType));
                final String pageXML = projConf.OCR_DIR + pageId + projConf.CONF_EXT;
                pageList.add(pageXML);

                // Add affected line segment images with their absolute path to the json file
                dataList.add(pageList);
            }
            ObjectWriter writer = mapper.writer();
            writer.writeValue(segmentListFile, dataList);

            processHandler = new ProcessHandler();
            processHandler.setFetchProcessConsole(true);
            processHandler.startProcess("ocr4all-helper-scripts", skewparams, false);

            cmdArgsWork.remove("--estimate_skew");
        }

        // ... (remainder of the method remains unchanged)

        // Clean up temp segmentListFile
        // segmentListFile.delete();
    }

    // ... (other methods remain unchanged)

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
     * Example: /var/ocr4all/models/custom/Bibel/heading/0.ckpt.json
     * Display: Bibel/heading/0
     *
     * The models need to be in the following structure:
     * ANY_PATH/{MODEL_NAME}/ANY_NAME.ckpt.json
     *
     * @return Map of models (key = modelName | value = path)
     * @throws IOException
     */
    public static TreeMap<String, String> listModels() throws IOException {
        TreeMap<String, String> models = new TreeMap<String, String>();

        File modelsDir = new File(ProjectConfiguration.PROJ_MODEL_DIR);
        if (!modelsDir.exists())
            return models;

        // Add all models to map (follow symbolic links on the filesystem due to Docker container)
        Files.walk(Paths.get(ProjectConfiguration.PROJ_MODEL_DIR), FileVisitOption.FOLLOW_LINKS)
                .map(Path::toFile)
                .filter(fileEntry -> fileEntry.getName().endsWith(ProjectConfiguration.MODEL_EXT))
                .forEach(
                        fileEntry -> {
                            // Remove OS path and model extension from display string (only display significant information)
                            String modelName = fileEntry.getAbsolutePath();
                            modelName = modelName.replace(ProjectConfiguration.PROJ_MODEL_DEFAULT_DIR, "");
                            modelName = modelName.replace(ProjectConfiguration.PROJ_MODEL_CUSTOM_DIR, "");
                            modelName = modelName.replace(ProjectConfiguration.MODEL_EXT, "");

                            models.put(modelName, fileEntry.getAbsolutePath());
                        });

        return models;
    }

    // ... (other methods remain unchanged)
}