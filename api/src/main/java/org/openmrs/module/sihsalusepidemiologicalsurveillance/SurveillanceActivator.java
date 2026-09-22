package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.TaskDefinition;

/**
 * Registers the scheduler task only once this module's class loader is available. The scheduler
 * table is shared by all modules. Starting this task during core startup makes OpenMRS resolve its
 * module class before the module class loader has been installed.
 */
public class SurveillanceActivator extends BaseModuleActivator {
	
	private static final Log LOG = LogFactory.getLog(SurveillanceActivator.class);
	
	private static final String COUNTS_TASK_NAME = "SIH Salus surveillance counts";
	
	@Override
	public void started() {
		TaskDefinition task = Context.getSchedulerService().getTaskByName(COUNTS_TASK_NAME);
		if (task == null)
			return;
		try {
			Context.getSchedulerService().scheduleTask(task);
		}
		catch (SchedulerException e) {
			LOG.error("Unable to schedule SIH Salus surveillance counts", e);
		}
	}
}
