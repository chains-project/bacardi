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
     * Ugly hack but helpers will be rewritten for the next release anyways. Don't use as basis for future code!
     *
     * @param pageIds Identifiers of the pages (e.g 0002,0003)
     * @param cmdArgs Command line arguments for "calamari-predict"
     * @throws IOException
     */
    public void execute(List<String> pageIds, final List<String> cmdArgs) throws IOException {
        RecognitionRunning = true;
        progress = 0;

        List<String> command = new ArrayList<>();
        if(cmdArgs.contains("--data.output_glyphs")){
            cmdArgs.remove("--data.output_glyphs");
            command.add("--data.output_glyphs");
            command.add("True");
        }
        if(cmdArgs.contains("--data.output_confidences")){
            cmdArgs.remove("--data.output_confidences");
            command.add("--data.output_confidences");
            command.add("True");
        }

        command.add("--data.images");
        // Create temp json file with all segment images (to not overload parameter list)
        // Temp file in a temp folder named "calamari-<random numbers>.json"
        File segmentListFile = File.createTempFile("calamari-",".json");
        segmentListFile.deleteOnExit();

        List<String> content = new ArrayList<>();
        for (String pageId : pageIds) {
            content.add(projConf.getImageDirectoryByType(projectImageType) + pageId +
                    projConf.getImageExtensionByType(projectImageType));
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();
        try {
            writer.writeValue(segmentListFile, content);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // Add checkpoints
        Iterator<String> cmdArgsIterator = cmdArgs.iterator();
        while (cmdArgsIterator.hasNext()) {
            String arg = cmdArgsIterator.next();
            command.add(arg);
            if (arg.equals("--checkpoint") && cmdArgsIterator.hasNext()) {
                command.addAll(extractModelsOfJoinedString(cmdArgsIterator.next()));
            }
        }

        command.add("--data");
        command.add("PageXML");
        // Set output extension to input extension in order to overwrite the original file
        // (default would've been .pred.xml)
        command.add("--data.gt_extension");
        command.add(".xml");

        command.add("--data.text_index");
        command.add("1");

        processHandler = new ProcessHandler();
        processHandler.setFetchProcessConsole(true);
        processHandler.startProcess("calamari-predict", command, false);

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
     * Returns the process state
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
    public List<String> extractModelsOfJoinedString(String joinedckptString){
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