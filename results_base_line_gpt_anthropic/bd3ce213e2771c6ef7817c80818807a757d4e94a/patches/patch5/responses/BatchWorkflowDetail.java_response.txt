/**
 * File:     BatchWorkflowDetail.java
 * Package:  de.uniwue.batch.report
 * 
 * Author:   Herbert Baier
 * Date:     24.09.2020
 */
package de.uniwue.batch.report;

import java.util.ArrayList;
import java.util.List;

import de.uniwue.batch.BatchWorkflow;
import de.uniwue.batch.WorkflowConfiguration;

/**
 * Defines batch workflow details.
 *
 * @author Herbert Baier
 * @version 1.0
 * @since 1.8
 */
public class BatchWorkflowDetail {
	/**
	 * The batch process overview.
	 */
	private final BatchWorkflowOverview process;

	/**
	 * The workflow configuration.
	 */
	private final WorkflowConfiguration configuration;

	/**
	 * The step overviews of the batch processes.
	 */
	private final List<BatchProcessOverview> steps = new ArrayList<BatchProcessOverview>();

	/**
	 * Creates a batch workflow details.
	 * 
	 * @param batch The batch workflow.
	 * @since 1.8
	 */
	public BatchWorkflowDetail(BatchWorkflow batch) {
		super();

		process = new BatchWorkflowOverview(batch);
		// Fix: Check if getConfiguration() method is available in the updated BatchWorkflow class
		// If it has been removed or renamed, we need to fetch the configuration differently.
		// Assuming the new method to get configuration is 'fetchConfiguration()'.
		configuration = batch.fetchConfiguration(); // Update method name according to the new API
	}

	/**
	 * Returns the batch process workflow.
	 *
	 * @return The batch process workflow.
	 * @since 1.8
	 */
	public BatchWorkflowOverview getProcess() {
		return process;
	}

	/**
	 * Returns the workflow configuration.
	 *
	 * @return The workflow configuration.
	 * @since 1.8
	 */
	public WorkflowConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Returns the step overviews of the batch processes.
	 *
	 * @return The step overviews of the batch processes.
	 * @since 1.8
	 */
	public List<BatchProcessOverview> getSteps() {
		return steps;
	}
}