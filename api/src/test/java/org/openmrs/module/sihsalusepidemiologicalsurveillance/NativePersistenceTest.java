package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.SurveillanceDao;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.NotifiableEvent;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/** OpenMRS core synthetic dataset, real Spring service wiring and Hibernate queries. */
public class NativePersistenceTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private SurveillanceDao dao;
	
	@Test
	public void persistsAndReadsAnEventThroughTheRegisteredHibernateMapping() {
		NotifiableEvent event = new NotifiableEvent();
		event.setConcept(Context.getConceptService().getConcept(3));
		event.setName("Synthetic surveillance");
		event.setPeriodicity("semanal");
		event.setDeadlineDays(7);
		dao.save(event);
		assertNotNull(event.getId());
		assertEquals(event.getUuid(), dao.byUuid(NotifiableEvent.class, event.getUuid()).getUuid());
	}
	
	@Test
	public void nativeClinicalQueriesCompileAgainstCoreMappings() {
		assertTrue(dao.encounters(new Date(0), new Date(), "00000000-0000-4000-8000-000000000002").isEmpty());
		assertTrue(dao.possibleDuplicates(Context.getPatientService().getPatient(2),
		    Arrays.asList("00000000-0000-4000-8000-000000000003"), new Date(0), new Date(),
		    "00000000-0000-4000-8000-000000000002").isEmpty());
	}
	
	@Test
	public void registersNativeClinicalDataAndReadsItAfterClearingTheSession() {
		SyntheticFixture f = new SyntheticFixture();
		org.openmrs.Patient patient = Context.getPatientService().getPatient(2);
		org.openmrs.User actor = Context.getAuthenticatedUser();
		org.openmrs.Location location = Context.getLocationService().getLocation(1);
		org.openmrs.Provider provider = new org.openmrs.Provider();
		provider.setPerson(actor.getPerson());
		provider.setIdentifier("synthetic-surveillance");
		Context.getProviderService().saveProvider(provider);
		org.openmrs.EncounterRole role = Context.getEncounterService().getAllEncounterRoles(false).get(0);
		org.openmrs.EncounterType type = Context.getEncounterService().getAllEncounterTypes(false).get(0);
		Date careDate = new Date(System.currentTimeMillis() - 86400000L);
		org.openmrs.Visit visit = new org.openmrs.Visit(patient, Context.getVisitService().getAllVisitTypes().get(0),
		        careDate);
		Context.getVisitService().saveVisit(visit);
		org.openmrs.Encounter source = new org.openmrs.Encounter();
		source.setPatient(patient);
		source.setVisit(visit);
		source.setLocation(location);
		source.setEncounterType(type);
		source.setEncounterDatetime(careDate);
		Context.getEncounterService().saveEncounter(source);
		for (String key : f.m.questions.keySet()) {
			String datatype = java.util.Arrays.asList("event", "status", "origin", "severity", "species").contains(key)
			        ? "Coded"
			        : "onset".equals(key) ? "Date" : "pregnancy".equals(key) ? "Boolean" : "Text";
			org.openmrs.Concept concept = nativeConcept("Synthetic " + key, datatype);
			f.m.questions.put(key, concept.getUuid());
		}
		for (org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog.Choice choice : f.m.statuses)
			choice.conceptUuid = nativeConcept("Synthetic status " + choice.key, "N/A").getUuid();
		for (org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog.Choice choice : f.m.origins)
			choice.conceptUuid = nativeConcept("Synthetic origin " + choice.key, "N/A").getUuid();
		for (org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog.Choice choice : f.disease.severities)
			choice.conceptUuid = nativeConcept("Synthetic severity " + choice.key, "N/A").getUuid();
		org.openmrs.ConceptSource codingSource = new org.openmrs.ConceptSource();
		codingSource.setName("Synthetic ICD-10");
		codingSource.setDescription("Test only");
		Context.getConceptService().saveConceptSource(codingSource);
		f.m.icd10SourceUuid = codingSource.getUuid();
		org.openmrs.ConceptReferenceTerm term = new org.openmrs.ConceptReferenceTerm();
		term.setConceptSource(codingSource);
		term.setCode("A90");
		Context.getConceptService().saveConceptReferenceTerm(term);
		org.openmrs.Concept diagnosis = nativeConcept("Synthetic diagnosis", "N/A");
		org.openmrs.ConceptMap mapping = new org.openmrs.ConceptMap();
		mapping.setConceptReferenceTerm(term);
		mapping.setConceptMapType(Context.getConceptService().getConceptMapType(1));
		diagnosis.addConceptMapping(mapping);
		Context.getConceptService().saveConcept(diagnosis);
		for (org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog.DiagnosisMapping d : f.disease.diagnoses)
			d.diagnosisConceptUuid = diagnosis.getUuid();
		NotifiableEvent event = new NotifiableEvent();
		event.setConcept(nativeConcept("Synthetic disease event", "N/A"));
		event.setName("Synthetic event");
		event.setPeriodicity("semanal");
		event.setDeadlineDays(7);
		dao.save(event);
		f.disease.eventUuid = event.getUuid();
		f.request.eventUuid = event.getUuid();
		f.request.patientUuid = patient.getUuid();
		f.request.sourceEncounterUuid = source.getUuid();
		f.request.providerUuid = provider.getUuid();
		f.request.locationUuid = location.getUuid();
		f.m.encounterRoleUuid = role.getUuid();
		f.request.onsetDate = new org.openmrs.module.sihsalusepidemiologicalsurveillance.api.EpidemiologicalCalendar(f.m)
		        .local(careDate).toString();
		org.mockito.Mockito.when(f.access.require(org.mockito.Mockito.anyString())).thenReturn(actor);
		org.mockito.Mockito.when(f.clinical.patient(patient.getUuid())).thenReturn(patient);
		org.mockito.Mockito.when(f.clinical.encounter(source.getUuid())).thenReturn(source);
		org.mockito.Mockito.when(f.clinical.provider(provider.getUuid())).thenReturn(provider);
		org.mockito.Mockito.when(f.clinical.location(location.getUuid())).thenReturn(location);
		org.mockito.Mockito.when(f.clinical.encounterRole(role.getUuid())).thenReturn(role);
		org.mockito.Mockito.when(f.clinical.encounterType(type.getUuid())).thenReturn(type);
		org.mockito.Mockito.when(f.clinical.concept(org.mockito.Mockito.anyString()))
		        .thenAnswer(i -> Context.getConceptService().getConceptByUuid(i.getArgument(0)));
		org.mockito.Mockito.when(f.clinical.saveEncounter(org.mockito.Mockito.any()))
		        .thenAnswer(i -> Context.getEncounterService().saveEncounter(i.getArgument(0)));
		org.mockito.Mockito.when(f.clinical.saveDiagnosis(org.mockito.Mockito.any()))
		        .thenAnswer(i -> Context.getDiagnosisService().save(i.getArgument(0)));
		f.service.setDao(dao);
		org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.CaseResult result = f.service
		        .registerCase(f.request);
		Context.flushSession();
		Context.clearSession();
		org.openmrs.Encounter persisted = Context.getEncounterService().getEncounterByUuid(result.uuid);
		assertEquals(5, persisted.getAllObs(false).size());
		assertEquals(1, persisted.getDiagnoses().size());
		assertEquals("A90", result.icd10);
		assertEquals(visit.getUuid(), persisted.getVisit().getUuid());
		assertEquals(1,
		    dao.encounters(new Date(careDate.getTime() - 86400000L), new Date(), f.m.questions.get("onset")).size());
	}
	
	private org.openmrs.Concept nativeConcept(String name, String datatype) {
		org.openmrs.Concept concept = new org.openmrs.Concept();
		concept.addName(new org.openmrs.ConceptName(name, java.util.Locale.ENGLISH));
		concept.setDatatype(Context.getConceptService().getConceptDatatypeByName(datatype));
		concept.setConceptClass(Context.getConceptService().getConceptClass(1));
		return Context.getConceptService().saveConcept(concept);
	}
}
