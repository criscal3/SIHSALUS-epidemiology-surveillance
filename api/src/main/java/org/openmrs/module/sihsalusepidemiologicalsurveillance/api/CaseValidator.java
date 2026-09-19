package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import java.time.*;
import java.util.*;

import org.openmrs.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.ClinicalData;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;

public class CaseValidator {

	private ClinicalData clinical;

	public void setClinical(ClinicalData value) {
		clinical = value;
	}

	public static class Validated {

		public Patient patient;

		public Encounter source;

		public Location location;

		public Provider provider;

		public Concept diagnosis;

		public String icd10;

		public Obs laboratory;

		public LocalDate onset;

		public Boolean pregnant;

		public String ethnicity;

		public Metadata.Disease disease;
	}

	public Validated validate(CaseRequest r, Metadata m, User actor) {
		List<String> missing = new ArrayList<String>();
		if (r == null)
			throw new SurveillanceException(422, "REQUIRED_FIELDS");
		String[] values = { r.uuid, r.patientUuid, r.sourceEncounterUuid, r.providerUuid, r.locationUuid, r.eventUuid,
		        r.status, r.severity, r.origin, r.onsetDate };
		String[] names = { "uuid", "patientUuid", "sourceEncounterUuid", "providerUuid", "locationUuid", "eventUuid",
		        "status", "severity", "origin", "onsetDate" };
		for (int i = 0; i < values.length; i++)
			if (!MetadataResolver.present(values[i]))
				missing.add(names[i]);
		if (!missing.isEmpty())
			throw new SurveillanceException(422, "REQUIRED_FIELDS", missing);
		try {
			for (String uuid : Arrays.asList(r.uuid, r.patientUuid, r.sourceEncounterUuid, r.providerUuid, r.locationUuid,
			    r.eventUuid))
				if (!UUID.fromString(uuid).toString().equalsIgnoreCase(uuid))
					throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException ex) {
			throw new SurveillanceException(422, "INVALID_REFERENCE");
		}
		Validated v = new Validated();
		v.patient = clinical.patient(r.patientUuid);
		v.source = clinical.encounter(r.sourceEncounterUuid);
		v.location = clinical.location(r.locationUuid);
		v.provider = clinical.provider(r.providerUuid);
		if (v.patient == null || v.patient.getVoided() || v.source == null || v.source.getVoided()
		        || !v.patient.equals(v.source.getPatient()) || v.source.getVisit() == null
		        || v.source.getEncounterDatetime() == null || v.source.getEncounterDatetime().after(new Date())
		        || v.source.getVisit().getVoided() || !v.patient.equals(v.source.getVisit().getPatient()))
			throw new SurveillanceException(422, "INVALID_SOURCE_ENCOUNTER");
		if (v.source.getEncounterType() == null || !m.encounterTypeUuid.equals(v.source.getEncounterType().getUuid()))
			throw new SurveillanceException(422, "INVALID_SOURCE_ENCOUNTER");
		if (v.location == null || v.location.getRetired())
			throw new SurveillanceException(422, "INVALID_LOCATION");
		if (v.source.getLocation() == null || !v.source.getLocation().equals(v.location))
			throw new SurveillanceException(422, "INVALID_LOCATION");
		if (v.provider == null || v.provider.getRetired() || v.provider.getPerson() == null || actor == null
		        || !v.provider.getPerson().equals(actor.getPerson()))
			throw new SurveillanceException(403, "INVALID_PROVIDER");
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		try {
			v.onset = LocalDate.parse(r.onsetDate);
		}
		catch (Exception ex) {
			throw new SurveillanceException(422, "INVALID_ONSET_DATE");
		}
		if (v.onset.isAfter(calendar.today())
		        || (v.patient.getBirthdate() != null && v.onset.isBefore(calendar.local(v.patient.getBirthdate())))
		        || v.onset.isAfter(calendar.local(v.source.getEncounterDatetime()))
		        || (v.patient.getDeathDate() != null && v.onset.isAfter(calendar.local(v.patient.getDeathDate()))))
			throw new SurveillanceException(422, "INVALID_ONSET_DATE");
		v.disease = MetadataResolver.disease(m, r.eventUuid);
		MetadataResolver.choice(m.statuses, r.status);
		MetadataResolver.choice(m.origins, r.origin);
		MetadataResolver.choice(v.disease.severities, r.severity);
		if (!v.disease.species.isEmpty())
			MetadataResolver.choice(v.disease.species, r.species);
		else if (MetadataResolver.present(r.species))
			throw new SurveillanceException(422, "INVALID_CLASSIFICATION");
		for (Metadata.DiagnosisMapping mapping : v.disease.diagnoses)
			if (Objects.equals(mapping.severity, r.severity) && Objects.equals(empty(mapping.species), empty(r.species))) {
				v.diagnosis = clinical.concept(mapping.diagnosisConceptUuid);
				v.icd10 = mapping.icd10Code;
			}
		if (v.diagnosis == null && "SEVERE".equals(r.severity))
			for (Metadata.DiagnosisMapping mapping : v.disease.diagnoses)
				if ("SEVERE".equals(mapping.severity) && !MetadataResolver.present(mapping.species)) {
					v.diagnosis = clinical.concept(mapping.diagnosisConceptUuid);
					v.icd10 = mapping.icd10Code;
				}
		if (v.diagnosis == null || v.diagnosis.getRetired())
			throw new SurveillanceException(422, "DIAGNOSIS_MAPPING_UNAVAILABLE");
		if (!MetadataResolver.present(v.icd10))
			v.icd10 = MetadataResolver.icd10(v.diagnosis, m);
		validateLab(r, m, v, calendar);
		Obs pregnancy = CaseObservations.find(v.source, m.questions.get("pregnancy"));
		if (pregnancy != null)
			v.pregnant = CaseObservations.booleanValue(pregnancy, m);
		if (v.pregnant == null && MetadataResolver.present(m.pregnancyAttributeTypeUuid)) {
			PersonAttribute attr = v.patient.getAttribute(clinical.attributeType(m.pregnancyAttributeTypeUuid));
			if (attr != null && !attr.getVoided()) {
				if ("true".equalsIgnoreCase(attr.getValue()))
					v.pregnant = true;
				else if ("false".equalsIgnoreCase(attr.getValue()))
					v.pregnant = false;
			}
		}
		if (MetadataResolver.present(m.ethnicityAttributeTypeUuid)) {
			PersonAttribute attr = v.patient.getAttribute(clinical.attributeType(m.ethnicityAttributeTypeUuid));
			if (attr != null && !attr.getVoided())
				try {
					Concept ethnicity = clinical.concept(Integer.valueOf(attr.getValue()));
					if (ethnicity != null && !ethnicity.getRetired())
						v.ethnicity = ethnicity.getUuid();
				}
				catch (NumberFormatException ignored) {
					// An invalid person attribute must not be represented as a clinical concept.
				}
		}
		if (v.ethnicity == null)
			v.ethnicity = CaseObservations.coded(v.source, m, "ethnicity");
		return v;
	}

	private void validateLab(CaseRequest r, Metadata m, Validated v, EpidemiologicalCalendar calendar) {
		if (!MetadataResolver.present(r.laboratoryResultUuid)) {
			if (!"SUSPECTED".equals(r.status))
				throw new SurveillanceException(422, "LAB_RESULT_REQUIRED");
			return;
		}
		Obs result = clinical.observation(r.laboratoryResultUuid);
		v.laboratory = result;
		if (result == null || result.getVoided() || result.getPerson() == null
		        || !result.getPerson().getUuid().equals(v.patient.getUuid()) || !(result.getOrder() instanceof TestOrder)
		        || result.getConcept() == null || result.getOrder().getConcept() == null || result.getEncounter() == null
		        || result.getEncounter().getVoided() || !v.patient.equals(result.getEncounter().getPatient())
		        || !v.source.getVisit().equals(result.getEncounter().getVisit()) || result.getOrder().getVoided()
		        || !v.patient.equals(result.getOrder().getPatient()) || result.getOrder().getEncounter() == null
		        || result.getOrder().getEncounter().getVoided()
		        || !v.source.getVisit().equals(result.getOrder().getEncounter().getVisit()) || result.getValueCoded() == null
		        || result.getObsDatetime() == null || calendar.local(result.getObsDatetime()).isBefore(v.onset)
		        || result.getObsDatetime().after(new Date()))
			throw new SurveillanceException(422, "INVALID_LAB_RESULT");
		String expected = null;
		for (Metadata.LabTest test : v.disease.laboratoryTests) {
			if (test.orderConceptUuid.equals(result.getOrder().getConcept().getUuid())
			        && test.resultConceptUuid.equals(result.getConcept().getUuid())) {
				if (test.positiveAnswerUuids.contains(result.getValueCoded().getUuid()))
					expected = "CONFIRMED";
				if (test.negativeAnswerUuids.contains(result.getValueCoded().getUuid()))
					expected = "DISCARDED";
			}
		}
		if (expected == null || !expected.equals(r.status))
			throw new SurveillanceException(422, "LAB_STATUS_CONFLICT");
	}

	private static String empty(String value) {
		return value == null ? "" : value;
	}
}
