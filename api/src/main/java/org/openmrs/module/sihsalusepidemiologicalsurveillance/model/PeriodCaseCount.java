package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class PeriodCaseCount extends BaseOpenmrsObject {
	
	private Integer id;
	
	private NotifiableEvent event;
	
	private String periodType;
	
	private Integer year;
	
	private Integer periodNumber;
	
	private Date date;
	
	private Integer caseCount;
	
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
}
