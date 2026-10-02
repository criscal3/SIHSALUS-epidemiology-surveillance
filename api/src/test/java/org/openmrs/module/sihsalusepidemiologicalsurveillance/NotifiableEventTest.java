package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.SurveillanceException;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.NotifiableEvent;

public class NotifiableEventTest {
	
	@Test
	public void editsAllAttributesWithoutReplacingIdentityOrCreator() {
		SyntheticFixture f = new SyntheticFixture();
		String uuid = f.event.getUuid();
		f.event.setCreator(f.actor);
		Date created = new Date(1000);
		f.event.setDateCreated(created);
		org.openmrs.Concept concept = SyntheticFixture.concept(97);
		when(f.clinical.concept(concept.getUuid())).thenReturn(concept);
		Map<String, Object> request = body(f, "2026-02-01");
		request.put("conceptUuid", concept.getUuid());
		request.put("periodicity", "INMEDIATA");
		request.put("referenceRegulation", "NTS actualizada");
		request.put("validTo", "2026-12-31");
		Map<String, Object> result = f.service.updateEvent(uuid, request);
		assertEquals(uuid, result.get("uuid"));
		assertEquals(concept.getUuid(), result.get("conceptUuid"));
		assertEquals("INMEDIATA", result.get("periodicity"));
		assertEquals("NTS actualizada", result.get("referenceRegulation"));
		assertEquals("2026-02-01", result.get("validFrom"));
		assertEquals("2026-12-31", result.get("validTo"));
		assertSame(f.actor, f.event.getCreator());
		assertSame(created, f.event.getDateCreated());
		assertSame(f.actor, f.event.getChangedBy());
		assertNotNull(f.event.getDateChanged());
		verify(f.dao).lockEvent(f.event);
		verify(f.dao).save(f.event);
		request.put("validTo", null);
		assertNull(f.service.updateEvent(uuid, request).get("validTo"));
	}
	
	@Test
	public void invalidEditsDoNotMutateTheEvent() {
		for (String field : Arrays.asList("validFrom", "validTo", "periodicity", "referenceRegulation")) {
			SyntheticFixture f = new SyntheticFixture();
			Date originalStart = f.event.getValidFrom();
			Map<String, Object> request = body(f, "2026-02-01");
			request.put(field, "validTo".equals(field) ? "2025-01-01" : "");
			try {
				f.service.updateEvent(f.event.getUuid(), request);
				fail("Invalid edit must fail: " + field);
			}
			catch (SurveillanceException error) {
				assertEquals(422, error.getStatus());
			}
			assertSame(originalStart, f.event.getValidFrom());
			verify(f.dao, never()).save(any(NotifiableEvent.class));
		}
	}
	
	@Test
	public void editingRequiresManagePrivilege() {
		SyntheticFixture f = new SyntheticFixture();
		when(f.access.require(SurveillanceConstants.MANAGE)).thenThrow(new SurveillanceException(403, "FORBIDDEN"));
		try {
			f.service.updateEvent(f.event.getUuid(), body(f, "2026-02-01"));
			fail("Unauthorized edit must fail");
		}
		catch (SurveillanceException error) {
			assertEquals(403, error.getStatus());
		}
		verify(f.dao, never()).save(any(NotifiableEvent.class));
	}
	
	@Test
	public void newEventsAcceptImmediateAndRejectDailyPeriodicity() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> request = body(f, "2026-01-01");
		request.put("periodicity", "INMEDIATA");
		assertEquals("INMEDIATA", f.service.saveEvent(request).get("periodicity"));
		request.put("periodicity", "DIARIA");
		try {
			f.service.saveEvent(request);
			fail("Daily events must be rejected");
		}
		catch (SurveillanceException error) {
			assertEquals(422, error.getStatus());
			assertEquals("INVALID_EVENT", error.getCode());
		}
		verify(f.dao, times(1)).save(any(NotifiableEvent.class));
	}
	
	@Test
	public void postCreatesIndependentVersionsForTheSameConcept() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> first = body(f, "2026-01-01");
		Map<String, Object> second = body(f, "2027-01-01");
		second.put("uuid", SyntheticFixture.uuid(99));
		assertEquals("SEMANAL", f.service.saveEvent(first).get("periodicity"));
		assertEquals("2027-01-01", f.service.saveEvent(second).get("validFrom"));
		verify(f.dao, times(2)).save(any(NotifiableEvent.class));
	}
	
	@Test
	public void postGeneratesUuidWhenItIsNotProvided() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> request = body(f, "2026-01-01");
		request.remove("uuid");
		
		String uuid = (String) f.service.saveEvent(request).get("uuid");
		
		assertNotNull(uuid);
		UUID.fromString(uuid);
		verify(f.dao).save(any(NotifiableEvent.class));
	}
	
	@Test
	public void validToClosesAnExistingVersion() {
		SyntheticFixture f = new SyntheticFixture();
		Map<String, Object> result = f.service.updateEvent(f.event.getUuid(),
		    Collections.<String, Object> singletonMap("validTo", "2026-12-31"));
		assertEquals("2026-12-31", result.get("validTo"));
		verify(f.dao).lockEvent(f.event);
		verify(f.dao).save(f.event);
	}
	
	@Test
	public void validToBeforeValidFromIsRejected() {
		SyntheticFixture f = new SyntheticFixture();
		try {
			f.service.updateEvent(f.event.getUuid(), Collections.<String, Object> singletonMap("validTo", "2019-12-31"));
			fail();
		}
		catch (SurveillanceException error) {
			assertEquals("INVALID_EVENT", error.getCode());
		}
	}
	
	private Map<String, Object> body(SyntheticFixture f, String validFrom) {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("uuid", SyntheticFixture.uuid(98));
		body.put("conceptUuid", f.event.getConcept().getUuid());
		body.put("periodicity", "SEMANAL");
		body.put("referenceRegulation", "NTS 228-MINSA/CDC-2025");
		body.put("validFrom", validFrom);
		when(f.clinical.concept(f.event.getConcept().getUuid())).thenReturn(f.event.getConcept());
		return body;
	}
}
