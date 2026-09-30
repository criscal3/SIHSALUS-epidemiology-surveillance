package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Location;
import org.openmrs.User;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class EpidemiologicalFocus extends BaseOpenmrsObject {
	
	private Integer id;
	
	private NotifiableEvent event;
	
	private Location location;
	
	private String classification;
	
	private Boolean receptive;
	
	private Date lastCaseDate;
	
	private Integer addressHierarchyEntryId;
	
	private SurveillanceCase lastCase;
	
	private Date classificationStartDate;
	
	private Date calculationDate;
	
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
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public String getClassification() {
		return classification;
	}
	
	public void setClassification(String classification) {
		this.classification = classification;
	}
	
	public Boolean getReceptive() {
		return receptive;
	}
	
	public void setReceptive(Boolean receptive) {
		this.receptive = receptive;
	}
	
	public Date getLastCaseDate() {
		return lastCaseDate;
	}
	
	public void setLastCaseDate(Date lastCaseDate) {
		this.lastCaseDate = lastCaseDate;
	}
	
	public Integer getAddressHierarchyEntryId() {
		return addressHierarchyEntryId;
	}
	
	public void setAddressHierarchyEntryId(Integer value) {
		addressHierarchyEntryId = value;
	}
	
	public SurveillanceCase getLastCase() {
		return lastCase;
	}
	
	public void setLastCase(SurveillanceCase value) {
		lastCase = value;
	}
	
	public Date getClassificationStartDate() {
		return classificationStartDate;
	}
	
	public void setClassificationStartDate(Date value) {
		classificationStartDate = value;
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
