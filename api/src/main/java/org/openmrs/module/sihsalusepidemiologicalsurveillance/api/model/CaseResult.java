package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model;

import java.util.ArrayList;
import java.util.List;

public class CaseResult {
	
	public String uuid;
	
	public String diagnosisConceptUuid;
	
	public String icd10;
	
	public String periodicity;
	
	public int deadlineDays;
	
	public boolean replayed;
	
	public List<String> immediateAlerts = new ArrayList<String>();
	
	public List<String> outbreakAlerts = new ArrayList<String>();
	
	public List<String> warnings = new ArrayList<String>();
}
