package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import org.openmrs.BaseOpenmrsObject;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class ReglaAlertaBrote extends BaseOpenmrsObject {
	
	private Integer id;
	
	private EventoNotificable evento;
	
	private String tipoCondicion;
	
	private Integer ventanaSemanas;
	
	private Double valorUmbral;
	
	private Boolean activa;
	
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
	
	public String getTipoCondicion() {
		return tipoCondicion;
	}
	
	public void setTipoCondicion(String tipoCondicion) {
		this.tipoCondicion = tipoCondicion;
	}
	
	public Integer getVentanaSemanas() {
		return ventanaSemanas;
	}
	
	public void setVentanaSemanas(Integer ventanaSemanas) {
		this.ventanaSemanas = ventanaSemanas;
	}
	
	public Double getValorUmbral() {
		return valorUmbral;
	}
	
	public void setValorUmbral(Double valorUmbral) {
		this.valorUmbral = valorUmbral;
	}
	
	public Boolean getActiva() {
		return activa;
	}
	
	public void setActiva(Boolean activa) {
		this.activa = activa;
	}
}
