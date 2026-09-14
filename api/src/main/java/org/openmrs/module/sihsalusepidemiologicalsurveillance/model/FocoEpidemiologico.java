package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Location;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class FocoEpidemiologico extends BaseOpenmrsObject {
	
	private Integer id;
	
	private EventoNotificable evento;
	
	private Location location;
	
	private String clasificacion;
	
	private Boolean receptivo;
	
	private Date fechaUltimoCaso;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public EventoNotificable getEvento() {
		return evento;
	}
	
	public void setEvento(EventoNotificable evento) {
		this.evento = evento;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public String getClasificacion() {
		return clasificacion;
	}
	
	public void setClasificacion(String clasificacion) {
		this.clasificacion = clasificacion;
	}
	
	public Boolean getReceptivo() {
		return receptivo;
	}
	
	public void setReceptivo(Boolean receptivo) {
		this.receptivo = receptivo;
	}
	
	public Date getFechaUltimoCaso() {
		return fechaUltimoCaso;
	}
	
	public void setFechaUltimoCaso(Date fechaUltimoCaso) {
		this.fechaUltimoCaso = fechaUltimoCaso;
	}
}
