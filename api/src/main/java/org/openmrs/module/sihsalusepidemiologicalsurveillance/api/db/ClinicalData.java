package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db;

import org.openmrs.*;
import org.openmrs.api.context.Context;

/** Native OpenMRS service boundary; does not introduce a second clinical persistence model. */
public class ClinicalData {
	
	public Concept concept(String uuid) {
		return Context.getConceptService().getConceptByUuid(uuid);
	}
	
	public Concept concept(Integer id) {
		return Context.getConceptService().getConcept(id);
	}
	
	public Patient patient(String uuid) {
		return Context.getPatientService().getPatientByUuid(uuid);
	}
	
	public Encounter encounter(String uuid) {
		return Context.getEncounterService().getEncounterByUuid(uuid);
	}
	
	public EncounterType encounterType(String uuid) {
		return Context.getEncounterService().getEncounterTypeByUuid(uuid);
	}
	
	public EncounterRole encounterRole(String uuid) {
		return Context.getEncounterService().getEncounterRoleByUuid(uuid);
	}
	
	public Location location(String uuid) {
		return Context.getLocationService().getLocationByUuid(uuid);
	}
	
	public Provider provider(String uuid) {
		return Context.getProviderService().getProviderByUuid(uuid);
	}
	
	public Obs observation(String uuid) {
		return Context.getObsService().getObsByUuid(uuid);
	}
	
	public PersonAttributeType attributeType(String uuid) {
		return Context.getPersonService().getPersonAttributeTypeByUuid(uuid);
	}
	
	public ConceptSource conceptSource(String uuid) {
		return Context.getConceptService().getConceptSourceByUuid(uuid);
	}
	
	public Encounter saveEncounter(Encounter encounter) {
		return Context.getEncounterService().saveEncounter(encounter);
	}
	
	public Diagnosis saveDiagnosis(Diagnosis diagnosis) {
		return Context.getDiagnosisService().save(diagnosis);
	}
	
	public void alert(org.openmrs.User user, String text) {
		Context.getAlertService().saveAlert(new org.openmrs.notification.Alert(text, user));
	}
	
	public Concept trueConcept() {
		return Context.getConceptService().getTrueConcept();
	}
	
	public Concept falseConcept() {
		return Context.getConceptService().getFalseConcept();
	}
}
