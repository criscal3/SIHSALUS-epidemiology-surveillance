package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Location;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class EpidemiologicalFocus extends BaseOpenmrsObject {
	
	private Integer id;
	
	private NotifiableEvent event;
	
	private Location location;
	
	private String classification;
	
	private Boolean receptive;
	
	private Date lastCaseDate;
	
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
}
