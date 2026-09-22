package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Encounter;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class NotiNotification extends BaseOpenmrsObject {
	
	private Integer id;
	
	private Encounter encounter;
	
	private NotifiableEvent event;
	
	private String formatVersion;
	
	private String exportContent;
	
	private String sendStatus;
	
	private Date generatedAt;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Encounter getEncounter() {
		return encounter;
	}
	
	public void setEncounter(Encounter encounter) {
		this.encounter = encounter;
	}
	
	public NotifiableEvent getEvent() {
		return event;
	}
	
	public void setEvent(NotifiableEvent event) {
		this.event = event;
	}
	
	public String getFormatVersion() {
		return formatVersion;
	}
	
	public void setFormatVersion(String formatVersion) {
		this.formatVersion = formatVersion;
	}
	
	public String getExportContent() {
		return exportContent;
	}
	
	public void setExportContent(String exportContent) {
		this.exportContent = exportContent;
	}
	
	public String getSendStatus() {
		return sendStatus;
	}
	
	public void setSendStatus(String sendStatus) {
		this.sendStatus = sendStatus;
	}
	
	public Date getGeneratedAt() {
		return generatedAt;
	}
	
	public void setGeneratedAt(Date generatedAt) {
		this.generatedAt = generatedAt;
	}
}
