package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Encounter;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class NotificacionNoti extends BaseOpenmrsObject {
	
	private Integer id;
	
	private Encounter encounter;
	
	private EventoNotificable evento;
	
	private String versionFormato;
	
	private String contenidoExportable;
	
	private String estadoEnvio;
	
	private Date fechaGeneracion;
	
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
	
	public EventoNotificable getEvento() {
		return evento;
	}
	
	public void setEvento(EventoNotificable evento) {
		this.evento = evento;
	}
	
	public String getVersionFormato() {
		return versionFormato;
	}
	
	public void setVersionFormato(String versionFormato) {
		this.versionFormato = versionFormato;
	}
	
	public String getContenidoExportable() {
		return contenidoExportable;
	}
	
	public void setContenidoExportable(String contenidoExportable) {
		this.contenidoExportable = contenidoExportable;
	}
	
	public String getEstadoEnvio() {
		return estadoEnvio;
	}
	
	public void setEstadoEnvio(String estadoEnvio) {
		this.estadoEnvio = estadoEnvio;
	}
	
	public Date getFechaGeneracion() {
		return fechaGeneracion;
	}
	
	public void setFechaGeneracion(Date fechaGeneracion) {
		this.fechaGeneracion = fechaGeneracion;
	}
}
