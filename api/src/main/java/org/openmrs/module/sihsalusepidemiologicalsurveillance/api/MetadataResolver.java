package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.*;

import org.openmrs.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.Metadata;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.EventoNotificable;

public class MetadataResolver {

	private ClinicalData clinical;

	private SurveillanceDao dao;

	public void setClinical(ClinicalData value) {
		clinical = value;
	}

	public void setDao(SurveillanceDao value) {
		dao = value;
	}

	public Metadata get() {
		try {
			Metadata m = SurveillanceCatalog.create();
			validate(m);
			return m;
		}
		catch (Exception ex) {
			throw new SurveillanceException(503, "METADATA_NOT_CONFIGURED");
		}
	}

	public void validate(Metadata m) {
		check(clinical.trueConcept() != null && clinical.falseConcept() != null);
		m.trueConceptUuid = clinical.trueConcept().getUuid();
		m.falseConceptUuid = clinical.falseConcept().getUuid();
		check(m.version == 1 && m.duplicateWindowDays >= 1 && m.duplicateWindowDays <= 365);
		check(m.historicalYears >= 3 && m.historicalYears <= 20);
		check(m.minimumHistoricalYears >= 3 && m.minimumHistoricalYears <= m.historicalYears);
		check(m.minimalDaysInFirstWeek >= 1 && m.minimalDaysInFirstWeek <= 7);
		check(m.surveillanceStartDate != null);
		check(!java.time.LocalDate.parse(m.surveillanceStartDate).isAfter(java.time.LocalDate.now(ZoneId.of(m.timezone))));
		ZoneId.of(m.timezone);
		DayOfWeek.valueOf(m.firstDayOfWeek);
		// Replica routing is explicit: never silently fall back to the primary in production.
		check("primary".equals(m.analyticsDatasource));
		check(clinical.encounterType(m.encounterTypeUuid) != null
		        && !clinical.encounterType(m.encounterTypeUuid).getRetired());
		check(clinical.encounterRole(m.encounterRoleUuid) != null
		        && !clinical.encounterRole(m.encounterRoleUuid).getRetired());
		check(clinical.conceptSource(m.icd10SourceUuid) != null);
		if (present(m.ethnicityAttributeTypeUuid))
			check(clinical.attributeType(m.ethnicityAttributeTypeUuid) != null
			        && "org.openmrs.Concept".equals(clinical.attributeType(m.ethnicityAttributeTypeUuid).getFormat()));
		if (present(m.pregnancyAttributeTypeUuid))
			check(clinical.attributeType(m.pregnancyAttributeTypeUuid) != null
			        && "java.lang.Boolean".equals(clinical.attributeType(m.pregnancyAttributeTypeUuid).getFormat()));
		Map<String, String> datatypes = new LinkedHashMap<String, String>();
		for (String key : Arrays.asList("event", "status", "severity", "origin", "species"))
			datatypes.put(key, "Coded");
		datatypes.put("onset", "Date");
		datatypes.put("pregnancy", "Coded");
		for (String key : Arrays.asList("laboratoryResult"))
			datatypes.put(key, "Text");
		datatypes.put("ethnicity", "Coded");
		Set<String> used = new HashSet<String>();
		for (Map.Entry<String, String> field : datatypes.entrySet()) {
			Concept question = requiredConcept(m.questions.get(field.getKey()));
			check(question.getDatatype() != null && field.getValue().equals(question.getDatatype().getName()));
			check(used.add(question.getUuid()));
		}
		choices(m, "status", m.statuses);
		choices(m, "origin", m.origins);
		for (String key : Arrays.asList("SUSPECTED", "CONFIRMED", "DISCARDED"))
			choice(m.statuses, key);
		for (String key : Arrays.asList("AUTOCHTHONOUS", "IMPORTED_NATIONAL", "IMPORTED_INTERNATIONAL", "INDUCED",
		    "INTRODUCED", "RELAPSE", "RECRUDESCENCE"))
			choice(m.origins, key);
		check(!m.diseases.isEmpty());
		Set<String> events = new HashSet<String>();
		for (Metadata.Disease disease : m.diseases) {
			check(events.add(disease.eventUuid));
			EventoNotificable event = dao.byUuid(EventoNotificable.class, disease.eventUuid);
			check(event != null && !event.getConcept().getRetired());
			check(Arrays.asList("semanal", "inmediata", "diaria").contains(event.getPeriodicidad()));
			check(event.getPlazoDias() != null && event.getPlazoDias() >= 0);
			// The event is selected from this fixed, validated catalogue. The content package
			// currently does not provide answers for the event question.
			requiredConcept(event.getConcept().getUuid());
			choices(m, "severity", disease.severities);
			if (!disease.species.isEmpty())
				choices(m, "species", disease.species);
			check(!disease.diagnoses.isEmpty());
			Set<String> combinations = new HashSet<String>();
			for (Metadata.DiagnosisMapping mapping : disease.diagnoses) {
				choice(disease.severities, mapping.severity);
				if (!disease.species.isEmpty() && present(mapping.species))
					choice(disease.species, mapping.species);
				else if (disease.species.isEmpty())
					check(!present(mapping.species));
				check(combinations.add(mapping.severity + "|" + Objects.toString(mapping.species, "")));
				check(mapping.icd10Code != null && mapping.icd10Code.matches("[A-Z][0-9]{2}[0-9A-Z]{0,2}"));
				requiredConcept(mapping.diagnosisConceptUuid);
			}
			for (Metadata.LabTest test : disease.laboratoryTests) {
				requiredConcept(test.orderConceptUuid);
				Concept result = requiredConcept(test.resultConceptUuid);
				check("Coded".equals(result.getDatatype().getName()));
				check(!test.positiveAnswerUuids.isEmpty() && !test.negativeAnswerUuids.isEmpty());
				for (String answer : test.positiveAnswerUuids) {
					check(!test.negativeAnswerUuids.contains(answer));
					allowedAnswer(test.resultConceptUuid, answer);
				}
				for (String answer : test.negativeAnswerUuids)
					allowedAnswer(test.resultConceptUuid, answer);
			}
		}
	}

	private void choices(Metadata m, String key, List<Metadata.Choice> choices) {
		check(!choices.isEmpty());
		Set<String> keys = new HashSet<String>();
		Set<String> values = new HashSet<String>();
		for (Metadata.Choice choice : choices) {
			check(present(choice.key) && present(choice.label) && keys.add(choice.key) && values.add(choice.conceptUuid));
			allowedAnswer(m.questions.get(key), choice.conceptUuid);
		}
	}

	private void allowedAnswer(String questionUuid, String answerUuid) {
		requiredConcept(answerUuid);
		for (ConceptAnswer answer : requiredConcept(questionUuid).getAnswers(false)) {
			if (answer.getAnswerConcept() != null && answerUuid.equals(answer.getAnswerConcept().getUuid()))
				return;
		}
		throw new IllegalArgumentException("Invalid configured answer");
	}

	public Concept requiredConcept(String uuid) {
		if (!present(uuid))
			throw new IllegalArgumentException("Missing concept");
		Concept concept = clinical.concept(uuid);
		check(concept != null && !concept.getRetired());
		return concept;
	}

	public static Metadata.Choice choice(List<Metadata.Choice> choices, String key) {
		for (Metadata.Choice choice : choices)
			if (Objects.equals(choice.key, key))
				return choice;
		throw new SurveillanceException(422, "INVALID_CLASSIFICATION");
	}

	public static Metadata.Disease disease(Metadata m, String uuid) {
		for (Metadata.Disease disease : m.diseases)
			if (Objects.equals(disease.eventUuid, uuid))
				return disease;
		throw new SurveillanceException(422, "INVALID_EVENT");
	}

	public static String icd10(Concept concept, Metadata m) {
		Set<String> codes = new HashSet<String>();
		for (ConceptMap map : concept.getConceptMappings()) {
			ConceptReferenceTerm term = map.getConceptReferenceTerm();
			if (term != null && !term.getRetired() && term.getConceptSource() != null
			        && m.icd10SourceUuid.equals(term.getConceptSource().getUuid()))
				codes.add(term.getCode());
		}
		if (codes.size() != 1)
			throw new SurveillanceException(503, "ICD10_MAPPING_UNAVAILABLE");
		return codes.iterator().next();
	}

	public static String diagnosisCode(Metadata m, Concept concept) {
		for (Metadata.Disease disease : m.diseases)
			for (Metadata.DiagnosisMapping mapping : disease.diagnoses)
				if (mapping.diagnosisConceptUuid.equals(concept.getUuid()) && present(mapping.icd10Code))
					return mapping.icd10Code;
		return icd10(concept, m);
	}

	public static boolean present(String text) {
		return text != null && !text.trim().isEmpty();
	}

	private static void check(boolean valid) {
		if (!valid)
			throw new IllegalArgumentException("Invalid metadata");
	}
}
