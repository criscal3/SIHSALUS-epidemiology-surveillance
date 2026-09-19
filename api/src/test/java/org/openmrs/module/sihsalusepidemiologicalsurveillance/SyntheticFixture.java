package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;

import org.openmrs.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.impl.SurveillanceServiceImpl;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

class SyntheticFixture {
	
	final Metadata m = new Metadata();
	
	final ClinicalData clinical = mock(ClinicalData.class);
	
	final SurveillanceDao dao = mock(SurveillanceDao.class);
	
	final MetadataResolver resolver = mock(MetadataResolver.class);
	
	final SurveillanceAccess access = mock(SurveillanceAccess.class);
	
	final CaseValidator validator = new CaseValidator();
	
	final SurveillanceServiceImpl service = new SurveillanceServiceImpl();
	
	final User actor = new User(1);
	
	final Patient patient = new Patient(2);
	
	final Provider provider = new Provider(3);
	
	final Location location = new Location(4);
	
	final Encounter source = new Encounter(5);
	
	final EncounterType type = new EncounterType();
	
	final EncounterRole role = new EncounterRole();
	
	final EventoNotificable event = new EventoNotificable();
	
	final Concept diagnosis = concept(6);
	
	final Metadata.Disease disease = new Metadata.Disease();
	
	final CaseRequest request = new CaseRequest();
	
	static String uuid(int number) {
		return String.format("00000000-0000-4000-8000-%012d", number);
	}
	
	static Concept concept(int id) {
		Concept c = new Concept(id);
		c.setUuid(uuid(id));
		c.setRetired(false);
		return c;
	}
	
	SyntheticFixture() {
		m.surveillanceStartDate = "2020-01-01";
		m.trueConceptUuid = uuid(80);
		m.falseConceptUuid = uuid(81);
		when(clinical.trueConcept()).thenReturn(concept(80));
		when(clinical.falseConcept()).thenReturn(concept(81));
		patient.setUuid(uuid(2));
		patient.setBirthdate(new EpidemiologicalCalendar(m).date(LocalDate.of(1990, 1, 1)));
		patient.setGender("F");
		patient.setVoided(false);
		actor.setPerson(new Person(1));
		actor.getPerson().setUuid(uuid(1));
		provider.setUuid(uuid(3));
		provider.setPerson(actor.getPerson());
		provider.setRetired(false);
		location.setUuid(uuid(4));
		location.setRetired(false);
		Visit visit = new Visit();
		visit.setUuid(uuid(10));
		visit.setPatient(patient);
		visit.setVoided(false);
		source.setUuid(uuid(5));
		source.setPatient(patient);
		source.setVisit(visit);
		source.setLocation(location);
		source.setEncounterDatetime(new EpidemiologicalCalendar(m).date(LocalDate.of(2026, 1, 20)));
		type.setUuid(uuid(11));
		m.encounterTypeUuid = type.getUuid();
		source.setEncounterType(type);
		role.setUuid(uuid(12));
		m.encounterRoleUuid = role.getUuid();
		m.icd10SourceUuid = uuid(13);
		ConceptSource codeSource = new ConceptSource();
		codeSource.setUuid(m.icd10SourceUuid);
		ConceptReferenceTerm term = new ConceptReferenceTerm();
		term.setConceptSource(codeSource);
		term.setCode("A90");
		term.setRetired(false);
		ConceptMap map = new ConceptMap();
		map.setConceptReferenceTerm(term);
		diagnosis.setConceptMappings(new HashSet<ConceptMap>(Arrays.asList(map)));
		event.setId(1);
		event.setUuid(uuid(14));
		event.setConcept(concept(15));
		event.setNombre("Synthetic event");
		event.setPeriodicidad("semanal");
		event.setPlazoDias(7);
		disease.eventUuid = event.getUuid();
		m.diseases.add(disease);
		add(m.statuses, "SUSPECTED", 20);
		add(m.statuses, "CONFIRMED", 21);
		add(m.statuses, "DISCARDED", 22);
		add(m.origins, "AUTOCHTHONOUS", 23);
		add(disease.severities, "MILD", 24);
		add(disease.severities, "SEVERE", 25);
		for (String severity : Arrays.asList("MILD", "SEVERE")) {
			Metadata.DiagnosisMapping d = new Metadata.DiagnosisMapping();
			d.severity = severity;
			d.diagnosisConceptUuid = diagnosis.getUuid();
			d.icd10Code = "A90";
			disease.diagnoses.add(d);
		}
		int question = 30;
		for (String key : Arrays.asList("event", "status", "severity", "origin", "species", "onset", "pregnancy",
		    "sourceEncounter", "laboratoryResult", "ethnicity")) {
			Concept c = concept(question++);
			m.questions.put(key, c.getUuid());
			when(clinical.concept(c.getUuid())).thenReturn(c);
		}
		when(clinical.patient(patient.getUuid())).thenReturn(patient);
		when(clinical.encounter(source.getUuid())).thenReturn(source);
		when(clinical.provider(provider.getUuid())).thenReturn(provider);
		when(clinical.location(location.getUuid())).thenReturn(location);
		when(clinical.concept(diagnosis.getUuid())).thenReturn(diagnosis);
		when(clinical.encounterType(type.getUuid())).thenReturn(type);
		when(clinical.encounterRole(role.getUuid())).thenReturn(role);
		when(clinical.saveEncounter(any(Encounter.class))).thenAnswer(i -> {
			Encounter e = i.getArgument(0);
			e.setId(100);
			return e;
		});
		when(dao.events()).thenReturn(Arrays.asList(event));
		when(dao.byUuid(EventoNotificable.class, event.getUuid())).thenReturn(event);
		when(dao.rules(event)).thenReturn(Collections.<ReglaAlertaBrote> emptyList());
		when(dao.counts(any(), anyString(), anyInt(), anyInt())).thenReturn(Collections.<ConteoCasosPeriodo> emptyList());
		when(dao.encounters(anyString(), any(), any(), anyString())).thenReturn(new ArrayList<Encounter>());
		when(dao.possibleDuplicates(anyString(), any(), anyList(), any(), any(), anyString()))
		        .thenReturn(Collections.<Encounter> emptyList());
		when(resolver.get()).thenReturn(m);
		when(access.require(anyString())).thenReturn(actor);
		validator.setClinical(clinical);
		service.setClinical(clinical);
		service.setDao(dao);
		service.setMetadata(resolver);
		service.setAccess(access);
		service.setValidator(validator);
		request.uuid = uuid(90);
		request.patientUuid = patient.getUuid();
		request.sourceEncounterUuid = source.getUuid();
		request.providerUuid = provider.getUuid();
		request.locationUuid = location.getUuid();
		request.eventUuid = event.getUuid();
		request.status = "SUSPECTED";
		request.severity = "MILD";
		request.origin = "AUTOCHTHONOUS";
		request.onsetDate = "2026-01-19";
	}
	
	private void add(List<Metadata.Choice> choices, String key, int id) {
		Metadata.Choice c = new Metadata.Choice();
		c.key = key;
		c.conceptUuid = uuid(id);
		c.label = key;
		choices.add(c);
		when(clinical.concept(c.conceptUuid)).thenReturn(concept(id));
	}
	
	Obs lab(boolean positive) {
		Concept test = concept(70), question = concept(71), answer = concept(positive ? 72 : 73);
		Metadata.LabTest definition = new Metadata.LabTest();
		definition.orderConceptUuid = test.getUuid();
		definition.resultConceptUuid = question.getUuid();
		definition.positiveAnswerUuids.add(uuid(72));
		definition.negativeAnswerUuids.add(uuid(73));
		disease.laboratoryTests.add(definition);
		TestOrder order = new TestOrder();
		order.setConcept(test);
		order.setPatient(patient);
		order.setEncounter(source);
		Obs obs = new Obs(patient, question, source.getEncounterDatetime(), location);
		obs.setUuid(uuid(74));
		obs.setOrder(order);
		obs.setEncounter(source);
		obs.setValueCoded(answer);
		request.laboratoryResultUuid = obs.getUuid();
		when(clinical.observation(obs.getUuid())).thenReturn(obs);
		return obs;
	}
}
