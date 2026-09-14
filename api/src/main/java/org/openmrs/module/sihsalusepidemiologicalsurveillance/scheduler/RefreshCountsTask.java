package org.openmrs.module.sihsalusepidemiologicalsurveillance.scheduler;

import org.openmrs.api.context.Context;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.SurveillanceService;
import org.openmrs.scheduler.tasks.AbstractTask;

/** OpenMRS scheduler supplies the authenticated daemon context and transaction proxy. */
public class RefreshCountsTask extends AbstractTask {
	
	@Override
	public synchronized void execute() {
		if (isExecuting())
			return;
		startExecuting();
		try {
			service().refreshCounts();
		}
		finally {
			stopExecuting();
		}
	}
	
	protected SurveillanceService service() {
		return Context.getService(SurveillanceService.class);
	}
}
