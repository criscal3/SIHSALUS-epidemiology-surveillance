package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model;

import java.time.LocalDate;

/** Internal clinical projection; never serialized by aggregate endpoints. */
public class CaseRecord {
	
	public String encounterUuid;
	
	public String eventUuid;
	
	public String locationUuid;
	
	public String status;
	
	public String severity;
	
	public String origin;
	
	public LocalDate onset;
	
	public LocalDate birthDate;
	
	public String gender;
	
	public String ethnicity;
	
	public Boolean pregnant;
	
	public boolean deceased;
}
