package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import org.openmrs.BaseOpenmrsObject;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class OutbreakAlertRule extends BaseOpenmrsObject {
	
	private Integer id;
	
	private NotifiableEvent event;
	
	private String conditionType;
	
	private Integer windowWeeks;
	
	private Double threshold;
	
	private Boolean active;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public NotifiableEvent getEvent() {
		return event;
	}
	
	public void setEvent(NotifiableEvent event) {
		this.event = event;
	}
	
	public String getConditionType() {
		return conditionType;
	}
	
	public void setConditionType(String conditionType) {
		this.conditionType = conditionType;
	}
	
	public Integer getWindowWeeks() {
		return windowWeeks;
	}
	
	public void setWindowWeeks(Integer windowWeeks) {
		this.windowWeeks = windowWeeks;
	}
	
	public Double getThreshold() {
		return threshold;
	}
	
	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}
	
	public Boolean getActive() {
		return active;
	}
	
	public void setActive(Boolean active) {
		this.active = active;
	}
}
