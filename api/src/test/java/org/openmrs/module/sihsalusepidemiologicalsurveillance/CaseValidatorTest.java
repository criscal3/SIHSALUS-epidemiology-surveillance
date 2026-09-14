package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openmrs.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;

public class CaseValidatorTest {
	
	@Test
	public void acceptsSuspectedWithoutLaboratoryAndKeepsUnknownPregnancy() {
		SyntheticFixture f = new SyntheticFixture();
		assertNull(f.validator.validate(f.request, f.m, f.actor).pregnant);
	}
	
	@Test
	public void confirmedRequiresLaboratory() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "CONFIRMED";
		fails(f, "LAB_RESULT_REQUIRED");
	}
	
	@Test
	public void confirmedRequiresPositiveResult() {
		SyntheticFixture f = new SyntheticFixture();
		f.lab(false);
		f.request.status = "CONFIRMED";
		fails(f, "LAB_STATUS_CONFLICT");
	}
	
	@Test
	public void positiveResultMustBelongToSamePatient() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "CONFIRMED";
		f.lab(true).setPerson(new Patient(999));
		fails(f, "INVALID_LAB_RESULT");
	}
	
	@Test
	public void positiveResultMustHaveTestOrder() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "CONFIRMED";
		f.lab(true).setOrder(new Order());
		fails(f, "INVALID_LAB_RESULT");
	}
	
	@Test
	public void rejectsVoidedLaboratoryResult() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "CONFIRMED";
		f.lab(true).setVoided(true);
		fails(f, "INVALID_LAB_RESULT");
	}
	
	@Test
	public void acceptsConfirmedWithMatchingPositiveTest() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "CONFIRMED";
		f.lab(true);
		assertNotNull(f.validator.validate(f.request, f.m, f.actor).laboratory);
	}
	
	@Test
	public void discardedRequiresNegativeResult() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "DISCARDED";
		f.lab(false);
		assertNotNull(f.validator.validate(f.request, f.m, f.actor).laboratory);
	}
	
	@Test
	public void rejectsAnotherPatientSource() {
		SyntheticFixture f = new SyntheticFixture();
		f.source.setPatient(new Patient(99));
		fails(f, "INVALID_SOURCE_ENCOUNTER");
	}
	
	@Test
	public void rejectsProviderImpersonation() {
		SyntheticFixture f = new SyntheticFixture();
		f.provider.setPerson(new Person(999));
		fails(f, "INVALID_PROVIDER");
	}
	
	@Test
	public void rejectsOnsetBeforeBirth() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.onsetDate = "1980-01-01";
		fails(f, "INVALID_ONSET_DATE");
	}
	
	@Test
	public void rejectsFutureAndMalformedDates() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.onsetDate = "2999-01-01";
		fails(f, "INVALID_ONSET_DATE");
		f.request.onsetDate = "2026-02-30";
		fails(f, "INVALID_ONSET_DATE");
	}
	
	@Test
	public void rejectsMissingClassifications() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.origin = "";
		fails(f, "REQUIRED_FIELDS");
	}
	
	@Test
	public void readsExplicitPregnancyFromSourceObservation() {
		SyntheticFixture f = new SyntheticFixture();
		Obs obs = new Obs();
		obs.setConcept(f.clinical.concept(f.m.questions.get("pregnancy")));
		obs.setValueCoded(f.clinical.trueConcept());
		f.source.addObs(obs);
		assertEquals(Boolean.TRUE, f.validator.validate(f.request, f.m, f.actor).pregnant);
	}
	
	@Test
	public void rejectsLaboratoryFromAnotherVisit() {
		SyntheticFixture f = new SyntheticFixture();
		f.request.status = "CONFIRMED";
		Encounter other = new Encounter();
		other.setPatient(f.patient);
		other.setVisit(new Visit());
		f.lab(true).setEncounter(other);
		fails(f, "INVALID_LAB_RESULT");
	}
	
	@Test
	public void rejectsOnsetAfterRecordedDeath() {
		SyntheticFixture f = new SyntheticFixture();
		f.patient.setDeathDate(new EpidemiologicalCalendar(f.m).date(java.time.LocalDate.of(2026, 1, 1)));
		fails(f, "INVALID_ONSET_DATE");
	}
	
	private void fails(SyntheticFixture f, String code) {
		try {
			f.validator.validate(f.request, f.m, f.actor);
			fail("Expected rejection");
		}
		catch (SurveillanceException ex) {
			assertEquals(code, ex.getCode());
		}
	}
}
