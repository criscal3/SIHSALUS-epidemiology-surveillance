package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Concept;
import org.openmrs.User;

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
	
	private String referenceRegulation;
	
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
	
	public String getReferenceRegulation() {
		return referenceRegulation;
	}
	
	public void setReferenceRegulation(String value) {
		referenceRegulation = value;
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
