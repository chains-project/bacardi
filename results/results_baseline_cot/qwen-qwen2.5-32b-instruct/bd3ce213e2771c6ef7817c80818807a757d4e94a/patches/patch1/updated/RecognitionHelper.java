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
    private Map<String,Long> imagesLastModified;

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
    private TreeMap<String,TreeMap<String, TreeMap<String, Boolean>>> processState = new TreeMap<>();

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

    /**
     * Gets the process handler object
     *
     * @return Returns the process Helper
     */
    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    /**
     * Initializes the structure with which the progress of the process can be monitored
     *
     * @param pageIds Identifiers of the chosen pages (e.g 0002,0003)
     * @throws IOException
     */
    public void initialize(List<String> pageIds) throws IOException {
        // Init the listener for image modification
        imagesLastModified = new HashMap<>();
        for(String pageId: pageIds) {
            final String pageXML = projConf.OCR_DIR + pageId + projConf.CONF_EXT;
            imagesLastModified.put(pageXML,new File(pageXML).lastModified());
        }
    }

    /**
     * Returns the Ids of the pages, for which line segmentation was already executed
     *
     * @param pageIds Identifiers of the pages (e.g 0002,0003)
     * @return Information if files exist
     */
    public boolean doOldFilesExist(String[] pageIds) {
        for (String pageId : pageIds) {
            if (procStateCol.recognitionState(pageId))
                return true;
        }
        return false;
    }

    /**
     * Executes OCR on a list of pages
     * Achieved with the help of an external python program "calamari-predict"
     *
     * @param pageIds Identifiers of the pages (e.g 0002,0003)
     * @param cmdArgs Command line arguments for "calamari-predict"
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
        	if(maxskewIndex > -1) {
        		skewparams.add(cmdArgsWork.remove(maxskewIndex));
        		skewparams.add(cmdArgsWork.remove(maxskewIndex));
        	}
        	final int skewstepsIndex = cmdArgsWork.indexOf("--skewsteps");
        	if(skewstepsIndex > -1) {
        		skewparams.add(cmdArgsWork.remove(skewstepsIndex));
        		skewparams.add(cmdArgsWork.remove(skewstepsIndex);
        	}

			// Create temp json file with all segment images (to not overload parameter list)
			// Temp file in a temp folder named "skew-<random numbers>.json"
            File segmentListFile = File.createTempFile("skew-",".json");
            segmentListFile.deleteOnExit();

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode dataList = mapper.createArrayNode();
            for (String pageId : pageIds) {
                ArrayNode pageList = mapper.createArrayNode();
                pageList.add(projConf.getImageDirectoryByType(projectImageType) + pageId +
                        projConf.getImageExtensionByType(projectImageType));
                final String pageXML = projConf.OCR_DIR + pageId + projConf.CONF_EXT;
                pageList.add(pageXML);

                dataList.add(pageList);
            }
            ObjectWriter writer = mapper.writer();
            try {
                writer.writeValue(segmentListFile, dataList);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            processHandler = new ProcessHandler();
            processHandler.setFetchProcessConsole(true);
            processHandler.startProcess("ocr4all-helper-scripts", skewparams, false);

        	cmdArgsWork.remove("--estimate_skew");
        }

        //// Recognize
		// Reset recognition data
        deleteOldFiles(pageIds);
        initialize(pageIds);

        int modifiedCount = 0;
        if(imagesLastModified != null) {
            for(String pagexml : imagesLastModified.keySet()) {
                if(imagesLastModified.get(pagexml) < new File(pagexml).lastModified()) {
                    modifiedCount++;
                }
            }
            progress = (modifiedCount*100) / imagesLastModified.size();
        } else {
            progress = -1;
        }

        // Execute progress update to fill processState data structure with correct values
        getProgress();
        // Process extension to ocropus-gpageseg script
        createSkippedSegments();

        progress = 100;
        RecognitionRunning = false;

        // Clean up temp segmentListFile
        // segmentListFile.delete();
    }

    /**
     * Resets the progress (use if an error occurs)
     */
    public void resetProgress() {
        RecognitionRunning = false;
        progress = -1;
    }

    /**
     * Cancels the process
     */
    public void cancelProcess() {
        if (processHandler != null)
            processHandler.stopProcess();
        RecognitionRunning = false;
    }

    /**
     * Returns the progress of the process
     *
     * @return Progress percentage
     * @throws IOException
     */
    public int getProgress() throws IOException {
        // Prevent function from calculation progress if process is not running
        if (!RecognitionRunning)
            return progress;

        int modifiedCount = 0;
        if(imagesLastModified != null) {
            for(String pagexml : imagesLastModified.keySet()) {
                if(imagesLastModified.get(pagexml) < new File(pagexml).lastModified()) {
                    modifiedCount++;
                }
            }
            progress = (modifiedCount*100) / imagesLastModified.size();
        } else {
            progress = -1;
        }
        return progress;
    }

    /**
     * Extracts checkpoints of a String joined by a whitespace
     *
     * @return List of checkpoints
     * @throws IOException
     */
    public List<String> extractModelsOfJoinedString(String joinedckptString) throws IOException {
        String [] checkpoints = joinedckptString.split(ProjectConfiguration.MODEL_EXT + " ");
        List<String> ckptList = new ArrayList<>();
        Iterator <String> ckptIterator= Arrays.asList(checkpoints).iterator();
        while (ckptIterator.hasNext()) {
            String ckpt = ckptIterator.next();
            if (ckptIterator.hasNext())
                ckpt = ckpt + ProjectConfiguration.MODEL_EXT;
            ckptList.add(ckpt);
        }
        return ckptList;
    }

    /**
     * Lists all available Models from the model directory
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

    /**
     * Determines conflicts with the process
     *
     * @param currentProcesses Processes that are currently running
     * @param inProcessFlow Indicates if the process is executed within the ProcessFlow
     * @return Type of process conflict
     */
    public int getConflictType(List<String> currentProcesses, boolean inProcessFlow) {
        return ProcessConflictDetector.recognitionConflict(currentProcesses, inProcessFlow);
    }
}