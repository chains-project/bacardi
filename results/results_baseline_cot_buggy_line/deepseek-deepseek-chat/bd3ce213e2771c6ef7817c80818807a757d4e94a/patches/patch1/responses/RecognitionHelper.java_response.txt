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
import com.fasterxml.jackson.core.JsonProcessingException;

import de.uniwue.config.ProjectConfiguration;
import de.uniwue.feature.ProcessConflictDetector;
import de.uniwue.feature.ProcessHandler;
import de.uniwue.feature.ProcessStateCollector;

/**
 * Helper class for recognition module
 */
public class RecognitionHelper {
    // ... (previous code remains unchanged until the execute method)

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
                skewparams.add(cmdArgsWork.remove(skewstepsIndex));
            }

            // Create temp json file with all segment images (to not overload parameter list)
            // Temp file in a temp folder named "skew-<random numbers>.json"
            File segmentListFile = File.createTempFile("skew-",".json");
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
            try {
                ObjectWriter writer = mapper.writer();
                writer.writeValue(segmentListFile, dataList);
            } catch (JsonProcessingException e) {
                throw new IOException("Failed to write JSON data", e);
            }

            processHandler = new ProcessHandler();
            processHandler.setFetchProcessConsole(true);
            processHandler.startProcess("ocr4all-helper-scripts", skewparams, false);

            cmdArgsWork.remove("--estimate_skew");
        }

        // ... (rest of the code remains unchanged)
    }

    // ... (remaining methods remain unchanged)
}