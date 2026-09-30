package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;

public class IndividualRecord extends BaseOpenmrsObject {
	
	private Integer id;
	
	private Integer year;
	
	private Integer epidemiologicalWeek;
	
	private String formatVersion;
	
	private String contentHash;
	
	private Date downloadDate;
	
	private User creator;
	
	private Date dateCreated;
	
	private User changedBy;
	
	private Date dateChanged;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer value) {
		id = value;
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
	
	public String getFormatVersion() {
		return formatVersion;
	}
	
	public void setFormatVersion(String value) {
		formatVersion = value;
	}
	
	public String getContentHash() {
		return contentHash;
	}
	
	public void setContentHash(String value) {
		contentHash = value;
	}
	
	public Date getDownloadDate() {
		return downloadDate;
	}
	
	public void setDownloadDate(Date value) {
		downloadDate = value;
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
