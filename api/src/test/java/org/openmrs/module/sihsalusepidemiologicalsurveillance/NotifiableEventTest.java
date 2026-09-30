package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.SurveillanceException;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.NotifiableEvent;

public class NotifiableEventTest {
	
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
