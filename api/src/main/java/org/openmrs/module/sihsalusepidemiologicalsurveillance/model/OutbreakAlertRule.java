package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class OutbreakAlertRule extends BaseOpenmrsObject {
	
	private Integer id;
	
	private NotifiableEvent event;
	
	private String conditionType;
	
	private Integer windowWeeks;
	
	private Double threshold;
	
	private Boolean active;
	
	private Integer version;
	
	private Date validFrom;
	
	private Date validTo;
	
	private User creator;
	
	private Date dateCreated;
	
	private User changedBy;
	
	private Date dateChanged;
	
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
	
	public Integer getVersion() {
		return version;
	}
	
	public void setVersion(Integer value) {
		version = value;
	}
	
	public Date getValidFrom() {
		return validFrom;
	}
	
	public void setValidFrom(Date value) {
		validFrom = value;
	}
	
	public Date getValidTo() {
		return validTo;
	}
	
	public void setValidTo(Date value) {
		validTo = value;
	}
	
	public User getCreator() {
		return creator;
	}
	
	public void setCreator(User value) {
		creator = value;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date value) {
		dateCreated = value;
	}
	
	public User getChangedBy() {
		return changedBy;
	}
	
	public void setChangedBy(User value) {
		changedBy = value;
	}
	
	public Date getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(Date value) {
		dateChanged = value;
	}
}
