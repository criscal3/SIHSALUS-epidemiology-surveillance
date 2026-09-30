package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;

public class OutbreakAlert extends BaseOpenmrsObject {
	
	private Integer id, addressHierarchyEntryId, year, epidemiologicalWeek, alertId;
	
	private OutbreakAlertRule rule;
	
	private Date generationDate, dateCreated, dateChanged;
	
	private User creator, changedBy;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer value) {
		id = value;
	}
	
	public OutbreakAlertRule getRule() {
		return rule;
	}
	
	public void setRule(OutbreakAlertRule value) {
		rule = value;
	}
	
	public Integer getAddressHierarchyEntryId() {
		return addressHierarchyEntryId;
	}
	
	public void setAddressHierarchyEntryId(Integer value) {
		addressHierarchyEntryId = value;
	}
	
	public Integer getYear() {
		return year;
	}
	
	public void setYear(Integer value) {
		year = value;
	}
	
	public Integer getEpidemiologicalWeek() {
		return epidemiologicalWeek;
	}
	
	public void setEpidemiologicalWeek(Integer value) {
		epidemiologicalWeek = value;
	}
	
	public Date getGenerationDate() {
		return generationDate;
	}
	
	public void setGenerationDate(Date value) {
		generationDate = value;
	}
	
	public Integer getAlertId() {
		return alertId;
	}
	
	public void setAlertId(Integer value) {
		alertId = value;
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
