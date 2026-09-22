package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.SurveillanceException;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class NotifiableEventTest {
	
	@Test
	public void createsNewEventAndRejectsReusingItsConcept() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> body = body(f);
		body.put("uuid", SyntheticFixture.uuid(99));
		assertEquals(false, f.service.saveEvent(body).get("retired"));
		when(f.dao.eventByConcept(f.event.getConcept())).thenReturn(f.event);
		expect("EVENT_CONCEPT_ALREADY_EXISTS", () -> f.service.saveEvent(body));
		verify(f.dao, times(1)).save(any(NotifiableEvent.class));
	}
	
	@Test
	public void updateRejectsChangingClinicalConcept() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> body = body(f);
		body.put("conceptUuid", f.diagnosis.getUuid());
		expect("EVENT_CONCEPT_IMMUTABLE", () -> f.service.updateEvent(f.event.getUuid(), body));
		verify(f.dao, never()).save(any());
	}
	
	private Map<String, Object> body(SyntheticFixture f) {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("uuid", f.event.getUuid());
		body.put("name", "Updated event");
		body.put("conceptUuid", f.event.getConcept().getUuid());
		body.put("periodicity", "diaria");
		body.put("deadlineDays", 1);
		when(f.clinical.concept(f.event.getConcept().getUuid())).thenReturn(f.event.getConcept());
		return body;
	}
	
	@Test
	public void updatePreservesIdentityAndAudits() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> result = f.service.updateEvent(f.event.getUuid(), body(f));
		assertEquals("Updated event", result.get("name"));
		assertEquals(f.event.getUuid(), result.get("uuid"));
		assertEquals(1, result.get("deadlineDays"));
		verify(f.dao).lockEvent(f.event);
		verify(f.dao).save(f.event);
		verify(f.dao).save(any(SurveillanceAudit.class));
	}
	
	@Test
	public void deleteIsIdempotentAndKeepsHistoricalIdentity() {
		SyntheticFixture f = new SyntheticFixture();
		f.service.deleteEvent(f.event.getUuid());
		f.service.deleteEvent(f.event.getUuid());
		assertTrue(f.service.getEvents(false).isEmpty());
		assertEquals(1, f.service.getEvents(true).size());
		assertEquals(true, f.service.getEvent(f.event.getUuid()).get("retired"));
		verify(f.dao, times(1)).save(f.event);
		verify(f.dao, times(1)).save(any(SurveillanceAudit.class));
		assertEquals(0, f.service.report(f.event.getUuid(), "2026-01-01", "2026-01-02", "dia").total);
		expect("EVENT_RETIRED", () -> f.service.registerCase(f.request));
		verify(f.clinical, never()).saveEncounter(any(Encounter.class));
	}
	
	@Test
	public void updateAndCreateRejectIdentityConflicts() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> body = body(f);
		expect("EVENT_ALREADY_EXISTS", () -> f.service.saveEvent(body));
		expect("EVENT_NOT_FOUND", () -> f.service.updateEvent(SyntheticFixture.uuid(99), body));
		body.put("uuid", SyntheticFixture.uuid(99));
		expect("EVENT_UUID_MISMATCH", () -> f.service.updateEvent(f.event.getUuid(), body));
		verify(f.dao, never()).save(any());
	}
	
	@Test
	public void missingClinicalQuestionDoesNotBlockCatalogButRejectsWrite() {
		SyntheticFixture f = new SyntheticFixture();
		when(f.clinical.concept(f.m.questions.get("status"))).thenReturn(null);
		assertNotNull(f.service.getCatalog().get("catalog"));
		assertEquals("UP", f.service.healthcheck().get("status"));
		expect("CLINICAL_CONCEPT_UNAVAILABLE", () -> f.service.registerCase(f.request));
		verify(f.clinical, never()).saveEncounter(any(Encounter.class));
	}
	
	@Test
	public void deniedAdministrationDoesNotReadOrMutateEvent() {
		SyntheticFixture f = new SyntheticFixture();
		doThrow(new SurveillanceException(403, "ACCESS_DENIED")).when(f.access).require(SurveillanceConstants.MANAGE);
		expect("ACCESS_DENIED", () -> f.service.deleteEvent(f.event.getUuid()));
		expect("ACCESS_DENIED", () -> f.service.updateEvent(f.event.getUuid(), new HashMap<String, Object>()));
		verify(f.dao, never()).byUuid(any(), anyString());
		verify(f.dao, never()).save(any());
	}
	
	private void expect(String code, Runnable action) {
		try {
			action.run();
			fail("Expected " + code);
		}
		catch (SurveillanceException error) {
			assertEquals(code, error.getCode());
		}
	}
}
