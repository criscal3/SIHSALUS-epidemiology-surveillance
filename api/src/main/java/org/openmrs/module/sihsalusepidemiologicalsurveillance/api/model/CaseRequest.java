package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model;

/** Client-generated UUID is the idempotency key and becomes the native encounter UUID. */
public class CaseRequest {
	
	public String uuid;
	
	public String patientUuid;
	
	public String sourceEncounterUuid;
	
	public String providerUuid;
	
	public String locationUuid;
	
	public String eventUuid;
	
	public String status;
	
	public String severity;
	
	public String origin;
	
	public String species;
	
	public String onsetDate;
	
	public String laboratoryResultUuid;
}
