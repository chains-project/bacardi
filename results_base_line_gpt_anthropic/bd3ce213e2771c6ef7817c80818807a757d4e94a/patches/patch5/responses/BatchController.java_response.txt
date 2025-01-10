/**
 * File:     BatchController.java
 * Package:  de.uniwue.controller
 * 
 * Author:   Herbert Baier
 * Date:     18.09.2020
 */
package de.uniwue.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.uniwue.batch.BatchWorkflow;
import de.uniwue.batch.WorkflowConfiguration;
import de.uniwue.batch.report.BatchScheduledOverview;
import de.uniwue.batch.report.BatchWorkflowDetail;
import de.uniwue.batch.report.BatchWorkflowOverview;
import de.uniwue.batch.report.ProcessManagerOverview;
import de.uniwue.config.ProjectConfiguration;
import de.uniwue.feature.ProcessStateCollector;
import de.uniwue.helper.OverviewHelper;
import de.uniwue.helper.RecognitionHelper;

/**
 * Defines batch controllers.
 *
 * @author Herbert Baier
 * @version 1.0
 * @since 1.8
 */
@Controller
@RequestMapping(value = "batch")
public class BatchController {
    /**
     * The batch process manager.
     */
    private final BatchProcessManager batchProcessManager;

    /**
     * Creates a batch controller.
     * 
     * @param batchProcessManager The batch process manager.
     * @since 1.8
     */
    public BatchController(BatchProcessManager batchProcessManager) {
        super();
        this.batchProcessManager = batchProcessManager;
    }

    /**
     * Returns the project overview.
     * 
     * @param name The project name.
     * @param type The project type. It can be Binary or Gray. The default value is
     *             Binary.
     * @return
     * @since 1.8
     */
    @RequestMapping(value = "/project", method = RequestMethod.GET)
    public @ResponseBody ProjectOverview getProject(@RequestParam String name,
            @RequestParam(required = false) String type) {
        if (type == null)
            type = WorkflowConfiguration.Type.Binary.name();

        String projectFolder;
        ProcessStateCollector processStateCollector;

        try {
            projectFolder = OverviewHelper.listProjects().get(name);
            if (!projectFolder.endsWith(File.separator))
                projectFolder = projectFolder + File.separator;

            processStateCollector = new ProcessStateCollector(new ProjectConfiguration(projectFolder), type);

        } catch (Exception e) {
            throw new IllegalArgumentException("can not extract pages information - " + e.getMessage());
        }

        File inputFolder = new File(projectFolder, BatchWorkflow.ProjectFolder.INPUT.name()); // Changed to INPUT
        if (!inputFolder.exists())
            throw new IllegalStateException("the input folder of project '" + name + "' does not exist.");

        ProjectOverview report = new ProjectOverview(name, type);

        for (final File fileEntry : Objects.requireNonNull(inputFolder.listFiles()))
            if (fileEntry.isFile()
                    && FilenameUtils.getExtension(fileEntry.getName()).equals(BatchWorkflow.SOURCE_IMAGE_EXTENSION)) // Changed to SOURCE_IMAGE_EXTENSION
                report.getPages().add(FilenameUtils.removeExtension(fileEntry.getName()));

        report.getPages().sort(String::compareToIgnoreCase);

        for (String page : report.getPages())
            report.getPageStates().add(new PageState(page, processStateCollector.preprocessingState(page),
                    processStateCollector.segmentationState(page), processStateCollector.lineSegmentationState(page),
                    processStateCollector.recognitionState(page)));

        return report;
    }

    // ... rest of the methods remain unchanged ...

    /**
     * Defines project overviews.
     *
     * @author Herbert Baier
     * @version 1.0
     * @since 1.8
     */
    private class ProjectOverview {
        // ... unchanged ...
    }

    /**
     * PageState is an immutable class that defines page states.
     *
     * @author Herbert Baier
     * @version 1.0
     * @since 1.8
     */
    private class PageState {
        // ... unchanged ...
    }

    /**
     * ParserReport ins an immutable class that defines parser reports for workflow
     * configurations.
     *
     * @author Herbert Baier
     * @version 1.0
     * @since 1.8
     */
    private static class ParserReport {
        // ... unchanged ...
    }
}