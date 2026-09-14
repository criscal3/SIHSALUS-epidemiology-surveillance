package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Concept;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class EventoNotificable extends BaseOpenmrsObject {
	
	private Integer id;
	
	private Concept concept;
	
	private String nombre;
	
	private String periodicidad;
	
	private Integer plazoDias;
	
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
	
	public String getNombre() {
		return nombre;
	}
	
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	public String getPeriodicidad() {
		return periodicidad;
	}
	
	public void setPeriodicidad(String periodicidad) {
		this.periodicidad = periodicidad;
	}
	
	public Integer getPlazoDias() {
		return plazoDias;
	}
	
	public void setPlazoDias(Integer plazoDias) {
		this.plazoDias = plazoDias;
	}
}
