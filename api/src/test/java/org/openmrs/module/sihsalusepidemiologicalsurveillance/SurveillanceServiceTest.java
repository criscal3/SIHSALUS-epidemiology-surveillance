package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openmrs.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class SurveillanceServiceTest {
	
	@Test
	public void registrationPersistsNativeEncounterDiagnosisAndAudit() {
		SyntheticFixture f = new SyntheticFixture();
		CaseResult result = f.service.registerCase(f.request);
		assertEquals(f.request.uuid, result.uuid);
		assertEquals("A90", result.icd10);
		assertEquals("semanal", result.periodicity);
		ArgumentCaptor<Encounter> capture = ArgumentCaptor.forClass(Encounter.class);
		verify(f.clinical).saveEncounter(capture.capture());
		Encounter e = capture.getValue();
		assertSame(f.source.getVisit(), e.getVisit());
		assertEquals(6, e.getAllObs(false).size());
		verify(f.clinical).saveDiagnosis(any(Diagnosis.class));
		verify(f.dao).save(any(AuditoriaVigilancia.class));
		org.mockito.InOrder order = inOrder(f.dao, f.clinical);
		order.verify(f.dao).lockPatient(f.patient);
		order.verify(f.dao).lockEvent(f.event);
		order.verify(f.clinical).saveEncounter(any(Encounter.class));
	}
	
	@Test
	public void duplicatePreventsAnyClinicalWrite() {
		SyntheticFixture f = new SyntheticFixture();
		when(f.dao.possibleDuplicates(anyString(), any(), anyList(), any(), any(), anyString()))
		        .thenReturn(Arrays.asList(f.source));
		try {
			f.service.registerCase(f.request);
			fail();
		}
		catch (SurveillanceException ex) {
			assertEquals("POSSIBLE_DUPLICATE", ex.getCode());
		}
		verify(f.clinical, never()).saveEncounter(any());
	}
	
	@Test
	public void retryReturnsSameEncounterWithoutSecondWrite() {
		SyntheticFixture f = new SyntheticFixture();
		f.service.registerCase(f.request);
		ArgumentCaptor<Encounter> capture = ArgumentCaptor.forClass(Encounter.class);
		verify(f.clinical).saveEncounter(capture.capture());
		when(f.clinical.encounter(f.request.uuid)).thenReturn(capture.getValue());
		assertTrue(f.service.registerCase(f.request).replayed);
		verify(f.clinical, times(1)).saveEncounter(any());
	}
	
	@Test
	public void changedPayloadCannotOverwriteSuccessfulRequest() {
		SyntheticFixture f = new SyntheticFixture();
		f.service.registerCase(f.request);
		ArgumentCaptor<Encounter> capture = ArgumentCaptor.forClass(Encounter.class);
		verify(f.clinical).saveEncounter(capture.capture());
		when(f.clinical.encounter(f.request.uuid)).thenReturn(capture.getValue());
		f.request.severity = "SEVERE";
		try {
			f.service.registerCase(f.request);
			fail();
		}
		catch (SurveillanceException ex) {
			assertEquals("IDEMPOTENCY_CONFLICT", ex.getCode());
		}
		verify(f.clinical, times(1)).saveEncounter(any());
	}
	
	@Test
	public void deniedWriteDoesNotReadClinicalData() {
		SyntheticFixture f = new SyntheticFixture();
		doThrow(new SurveillanceException(403, "ACCESS_DENIED")).when(f.access).require(SurveillanceConstants.REGISTER);
		try {
			f.service.registerCase(f.request);
			fail();
		}
		catch (SurveillanceException ex) {
			assertEquals(403, ex.getStatus());
		}
		verify(f.clinical, never()).patient(anyString());
	}
	
	@Test
	public void deniedReportsDoNotReadAnyCases() {
		SyntheticFixture f = new SyntheticFixture();
		doThrow(new SurveillanceException(403, "ACCESS_DENIED")).when(f.access).require(SurveillanceConstants.REPORT);
		try {
			f.service.report(f.event.getUuid(), "2026-01-01", "2026-01-10", "semana");
			fail();
		}
		catch (SurveillanceException ex) {
			assertEquals(403, ex.getStatus());
		}
		verify(f.dao, never()).encounters(anyString(), any(), any(), anyString());
	}
	
	@Test
	public void severeCaseOverridesConfiguredWeeklyPeriodicity() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.severity = "SEVERE";
		CaseResult result = f.service.registerCase(f.request);
		assertEquals("inmediata", result.periodicity);
		assertEquals(0, result.deadlineDays);
		assertTrue(result.immediateAlerts.contains("SEVERE_CASE"));
		verify(f.clinical).alert(eq(f.actor), anyString());
	}
	
	@Test
	public void refreshUsesCoverageAndReplacesAggregates() {
		SyntheticFixture f = new SyntheticFixture();
		f.m.surveillanceStartDate = "2026-01-01";
		f.service.refreshCounts();
		verify(f.dao).replaceCounts(eq(f.event), argThat(counts -> !counts.isEmpty()));
	}
}
