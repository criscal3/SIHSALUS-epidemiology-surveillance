package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class PeriodCaseCount extends BaseOpenmrsObject {
	
	private Integer id;
	
	private NotifiableEvent event;
	
	private String periodType;
	
	private Integer year;
	
	private Integer periodNumber;
	
	private Date date;
	
	private Integer caseCount;
	
	private Integer addressHierarchyEntryId;
	
	private String zoneLevel;
	
	private Date startDate, endDate, calculationDate, dateCreated, dateChanged;
	
	private String diagnosisType;
	
	private User creator, changedBy;
	
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
	
	public String getPeriodType() {
		return periodType;
	}
	
	public void setPeriodType(String periodType) {
		this.periodType = periodType;
	}
	
	public Integer getYear() {
		return year;
	}
	
	public void setYear(Integer year) {
		this.year = year;
	}
	
	public Integer getPeriodNumber() {
		return periodNumber;
	}
	
	public void setPeriodNumber(Integer periodNumber) {
		this.periodNumber = periodNumber;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public Integer getCaseCount() {
		return caseCount;
	}
	
	public void setCaseCount(Integer caseCount) {
		this.caseCount = caseCount;
	}
	
	public Integer getAddressHierarchyEntryId() {
		return addressHierarchyEntryId;
	}
	
	public void setAddressHierarchyEntryId(Integer value) {
		addressHierarchyEntryId = value;
	}
	
	public String getZoneLevel() {
		return zoneLevel;
	}
	
	public void setZoneLevel(String value) {
		zoneLevel = value;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Date value) {
		startDate = value;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date value) {
		endDate = value;
	}
	
	public String getDiagnosisType() {
		return diagnosisType;
	}
	
	public void setDiagnosisType(String value) {
		diagnosisType = value;
	}
	
	public Date getCalculationDate() {
		return calculationDate;
	}
	
	public void setCalculationDate(Date value) {
		calculationDate = value;
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
