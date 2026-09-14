package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class ConteoCasosPeriodo extends BaseOpenmrsObject {
	
	private Integer id;
	
	private EventoNotificable evento;
	
	private String tipoPeriodo;
	
	private Integer anio;
	
	private Integer numeroPeriodo;
	
	private Date fecha;
	
	private Integer numeroCasos;
	
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
	
	public String getTipoPeriodo() {
		return tipoPeriodo;
	}
	
	public void setTipoPeriodo(String tipoPeriodo) {
		this.tipoPeriodo = tipoPeriodo;
	}
	
	public Integer getAnio() {
		return anio;
	}
	
	public void setAnio(Integer anio) {
		this.anio = anio;
	}
	
	public Integer getNumeroPeriodo() {
		return numeroPeriodo;
	}
	
	public void setNumeroPeriodo(Integer numeroPeriodo) {
		this.numeroPeriodo = numeroPeriodo;
	}
	
	public Date getFecha() {
		return fecha;
	}
	
	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}
	
	public Integer getNumeroCasos() {
		return numeroCasos;
	}
	
	public void setNumeroCasos(Integer numeroCasos) {
		this.numeroCasos = numeroCasos;
	}
}
