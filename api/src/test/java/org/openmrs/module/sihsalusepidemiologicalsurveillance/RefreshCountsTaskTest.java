package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.SurveillanceService;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.scheduler.RefreshCountsTask;

public class RefreshCountsTaskTest {
	
	@Test
	public void refreshesThroughServiceAndClearsExecutionStateAfterFailure() {
		final SurveillanceService service = mock(SurveillanceService.class);
		RefreshCountsTask task = new RefreshCountsTask() {
			
			@Override
			protected SurveillanceService service() {
				return service;
			}
		};
		doThrow(new IllegalStateException("Synthetic failure")).doNothing().when(service).refreshCounts();
		try {
			task.execute();
			fail("Expected failure");
		}
		catch (IllegalStateException expected) {}
		assertFalse(task.isExecuting());
		task.execute();
		verify(service, times(2)).refreshCounts();
		assertFalse(task.isExecuting());
	}
}
