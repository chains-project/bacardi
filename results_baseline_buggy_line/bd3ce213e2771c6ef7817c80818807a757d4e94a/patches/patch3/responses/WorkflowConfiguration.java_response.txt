/**
 * File:     WorkflowConfiguration.java
 * Package:  de.uniwue.batch
 *
 * Author:   Herbert Baier
 * Date:     22.09.2020
 */
package de.uniwue.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uniwue.batch.process.AdjustmentType;
import de.uniwue.batch.process.Process;
import de.uniwue.batch.process.ResultGenerationStrategy;
import de.uniwue.batch.process.ResultGenerationType;
import de.uniwue.helper.OverviewHelper;
import de.uniwue.helper.RecognitionHelper;

/**
 * Defines batch workflow configurations.
 *
 * author Herbert Baier
 * @version 1.0
 * @since 1.8
 */
public class WorkflowConfiguration {
    // ... (other parts of the class remain unchanged)

    /**
     * Validates the workflow configuration.
     *
     * @param configuration The workflow configuration.
     * @throws IllegalArgumentException Throws on workflow configuration troubles.
     * @since 1.8
     */
    public static void validate(WorkflowConfiguration configuration) throws IllegalArgumentException {
        if (configuration == null)
            throw new IllegalArgumentException("the batch workflow configuration has to be set.");

        if (configuration.getProject() == null)
            throw new IllegalArgumentException("the project is not set.");

        if (!OverviewHelper.listProjects().containsKey(configuration.getProject()))
            throw new IllegalArgumentException("the project '" + configuration.getProject() + "' is unknown.");

        if (configuration.getType() == null)
            throw new IllegalArgumentException("the project type is not set. Allowed types: " + getCSV(Type.values()));

        if (configuration.getProcessing() == null)
            throw new IllegalArgumentException(
                    "the processing mode is not set. Allowed modes: " + getCSV(Processing.values()));

        if (configuration.getWorkflow() == null)
            throw new IllegalArgumentException("the workflow is not set.");

        if (configuration.getPages() != null)
            for (String page : configuration.getPages())
                if (page == null)
                    throw new IllegalArgumentException("a page is not defined.");

        if (configuration.getProcesses() == null)
            throw new IllegalArgumentException("the process configurations are not set.");

        Set<String> processIds = new HashSet<>();
        Set<String> models;
        try {
            // Updated to use the new method for listing models
            models = RecognitionHelper.listAvailableModels().keySet(); // Assuming the method has been renamed
        } catch (IOException e) {
            throw new IllegalArgumentException("the models can not be read.");
        }

        for (ProcessConfiguration process : configuration.getProcesses())
            if (process == null)
                throw new IllegalArgumentException("a process configuration is not defined.");
            else if (process.getId() == null || process.getId().trim().length() == 0)
                throw new IllegalArgumentException("the identifier of a process configuration is not set.");
            else if (!processIds.add(process.getId()))
                throw new IllegalArgumentException(
                        "duplicated process configuration for identifier '" + process.getId() + "'.");
            else {
                boolean isModelArgument = false;
                for (String argument : process.getArguments()) {
                    if (argument == null)
                        throw new IllegalArgumentException(
                                "an argument of the process configurations '" + process.getId() + "' is not defined.");
                    else if (Process.recognition.equals(process.getProcess()) && "--checkpoint".equals(argument))
                        isModelArgument = true;
                    else if (isModelArgument) {
                        if (argument.startsWith("--"))
                            isModelArgument = false;
                        else if (!models.contains(argument))
                            throw new IllegalArgumentException("the model '" + argument + "' is unknown.");
                    }

                }
            }

        for (String processId : configuration.getWorkflow())
            if (processId == null)
                throw new IllegalArgumentException("a workflow identifier is not set.");
            else if (!processIds.contains(processId))
                throw new IllegalArgumentException(
                        "no process configuration matches the workflow identifier '" + processId + "'.");

    }

    // ... (other parts of the class remain unchanged)
}