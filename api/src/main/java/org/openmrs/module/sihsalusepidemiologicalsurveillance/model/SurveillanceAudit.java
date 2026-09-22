package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class SurveillanceAudit extends BaseOpenmrsObject {
	
	private Integer id;
	
	private User user;
	
	private Date timestamp;
	
	private String actionType;
	
	private String affectedEntity;
	
	private Integer affectedRecordId;
	
	private String changedField;
	
	private String previousValue;
	
	private String newValue;
	
	private String result;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getActionType() {
		return actionType;
	}
	
	public void setActionType(String actionType) {
		this.actionType = actionType;
	}
	
	public String getAffectedEntity() {
		return affectedEntity;
	}
	
	public void setAffectedEntity(String affectedEntity) {
		this.affectedEntity = affectedEntity;
	}
	
	public Integer getAffectedRecordId() {
		return affectedRecordId;
	}
	
	public void setAffectedRecordId(Integer affectedRecordId) {
		this.affectedRecordId = affectedRecordId;
	}
	
	public String getChangedField() {
		return changedField;
	}
	
	public void setChangedField(String changedField) {
		this.changedField = changedField;
	}
	
	public String getPreviousValue() {
		return previousValue;
	}
	
	public void setPreviousValue(String previousValue) {
		this.previousValue = previousValue;
	}
	
	public String getNewValue() {
		return newValue;
	}
	
	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}
	
	public String getResult() {
		return result;
	}
	
	public void setResult(String result) {
		this.result = result;
	}
}
