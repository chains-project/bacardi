package de.uniwue.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    // ... (other fields and methods remain unchanged)

    /**
     * Executes OCR on a list of pages
     * Achieved with the help of the external python program "calamari-predict"
     *
     * @param pageIds Identifiers of the pages (e.g 0002,0003)
     * @param cmdArgs Command line arguments for "calamari-predict"
     * @throws IOException
     */
    public void execute(List<String> pageIds, final List<String> cmdArgs) throws IOException {
        // ... (other code remains unchanged)

        // Create temp json file with all segment images (to not overload parameter list)
        File segmentListFile = File.createTempFile("calamari-", ".files");
        segmentListFile.deleteOnExit();

        List<String> content = new ArrayList<>();
        for (String pageId : pageIds) {
            content.add(projConf.getImageDirectoryByType(projectImageType) + pageId + projConf.getImageExtensionByType(projectImageType));
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();
        try {
            writer.writeValue(segmentListFile, content);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to write JSON content to file", e);
        }

        // ... (rest of the method remains unchanged)
    }

    // ... (rest of the class remains unchanged)
}