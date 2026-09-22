package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Concept;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class NotifiableEvent extends BaseOpenmrsObject {
	
	private boolean retired;
	
	public boolean isRetired() {
		return retired;
	}
	
	public void setRetired(boolean retired) {
		this.retired = retired;
	}
	
	private Integer id;
	
	private Concept concept;
	
	private String name;
	
	private String periodicity;
	
	private Integer deadlineDays;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Concept getConcept() {
		return concept;
	}
	
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPeriodicity() {
		return periodicity;
	}
	
	public void setPeriodicity(String periodicity) {
		this.periodicity = periodicity;
	}
	
	public Integer getDeadlineDays() {
		return deadlineDays;
	}
	
	public void setDeadlineDays(Integer deadlineDays) {
		this.deadlineDays = deadlineDays;
	}
}
