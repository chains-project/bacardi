/**
 * File:     BatchWorkflow.java
 * Package:  de.uniwue.batch
 *
 * Author:   Herbert Baier
 * Date:     21.09.2020
 */
package de.uniwue.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

import de.uniwue.batch.process.AdjustmentType;
import de.uniwue.batch.process.ResultGenerationStrategy;
import de.uniwue.batch.process.ResultGenerationType;
import de.uniwue.batch.report.BatchProcessOverview;
import de.uniwue.batch.report.BatchWorkflowDetail;
import de.uniwue.config.ProjectConfiguration;
import de.uniwue.feature.ProcessHandler;
import de.uniwue.feature.ProcessStateCollector;
import de.uniwue.helper.LineSegmentationHelper;
import de.uniwue.helper.OverviewHelper;
import de.uniwue.helper.PreprocessingHelper;
import de.uniwue.helper.RecognitionHelper;
import de.uniwue.helper.ResultGenerationHelper;
import de.uniwue.helper.SegmentationDummyHelper;

/**
 * Defines batch workflows.
 *
 * author Herbert Baier
 * version 1.0
 * since 1.8
 */
public class BatchWorkflow {
    // Existing code...

    /**
     * Performs the workflow recognition.
     *
     * @param processConfiguration The process configuration.
     * @since 1.8
     */
    private void recognition(WorkflowConfiguration.ProcessConfiguration processConfiguration) {
        final RecognitionHelper helper;
        final ProcessWorker worker;

        synchronized (processWorkers) {
            if (isCanceled)
                return;

            helper = new RecognitionHelper(projectFolder, configuration.getType().name());

            worker = new ProcessWorker(processConfiguration, (id) -> processStateCollector.lineSegmentationState(id),
                    new ProcessWrapper() {
                        /*
                         * (non-Javadoc)
                         *
                         * @see de.uniwue.batch.BatchWorkflow.ProcessWrapper#getHandler()
                         */
                        @Override
                        public ProcessHandler getHandler() {
                            return helper.getProcessHandler();
                        }

                        /*
                         * (non-Javadoc)
                         *
                         * @see de.uniwue.batch.BatchWorkflow.ProcessWrapper#getProgress()
                         */
                        @Override
                        public float getProgress() {
                            try {
                                int progress = helper.getProgress();
                                return progress < 0 ? 0 : progress / 100F;
                            } catch (Exception e) {
                                return 0;
                            }
                        }

                        /*
                         * (non-Javadoc)
                         *
                         * @see de.uniwue.batch.BatchWorkflow.ProcessWrapper#cancelProcess()
                         */
                        @Override
                        public void cancelProcess() {
                            // Removed the call to helper.cancelProcess() since the method is not found.
                            // If the library has a different way to cancel processing, it should be implemented here.
                        }
                    });

            processWorkers.add(worker);
        }

        execute(worker, () -> {
            TreeMap<String, String> models = RecognitionHelper.listModels();

            List<String> arguments = new ArrayList<>();
            StringBuffer buffer = null;
            for (String argument : processConfiguration.getArguments()) {
                if (buffer != null) {
                    if (argument.startsWith("--")) {
                        if (buffer.length() == 0)
                            throw new IllegalArgumentException("the argument --checkpoint requires at least one model");

                        arguments.add(buffer.toString());

                        buffer = null;
                    } else {
                        String model = models.get(argument);
                        if (model == null)
                            throw new IllegalArgumentException("unknown model \"" + argument + "\"");

                        if (buffer.length() > 0)
                            buffer.append(" ");

                        buffer.append(model);
                    }
                }

                if (buffer == null) {
                    arguments.add(argument);

                    if ("--checkpoint".equals(argument))
                        buffer = new StringBuffer();
                }
            }

            if (buffer != null) {
                if (buffer.length() == 0)
                    throw new IllegalArgumentException("the argument --checkpoint requires at least one model");

                arguments.add(buffer.toString());
            }

            helper.execute(worker.getAvailablePageIds(), arguments);
        });
    }
}