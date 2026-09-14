package org.openmrs.module.sihsalusepidemiologicalsurveillance.model;

import java.util.Date;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;

/** Persisted surveillance entity. Clinical data remains in native OpenMRS entities. */
public class AuditoriaVigilancia extends BaseOpenmrsObject {
	
	private Integer id;
	
	private User usuario;
	
	private Date fechaHora;
	
	private String tipoAccion;
	
	private String entidadAfectada;
	
	private Integer registroAfectadoId;
	
	private String campoModificado;
	
	private String valorAnterior;
	
	private String valorNuevo;
	
	private String resultadoAccion;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public User getUsuario() {
		return usuario;
	}
	
	public void setUsuario(User usuario) {
		this.usuario = usuario;
	}
	
	public Date getFechaHora() {
		return fechaHora;
	}
	
	public void setFechaHora(Date fechaHora) {
		this.fechaHora = fechaHora;
	}
	
	public String getTipoAccion() {
		return tipoAccion;
	}
	
	public void setTipoAccion(String tipoAccion) {
		this.tipoAccion = tipoAccion;
	}
	
	public String getEntidadAfectada() {
		return entidadAfectada;
	}
	
	public void setEntidadAfectada(String entidadAfectada) {
		this.entidadAfectada = entidadAfectada;
	}
	
	public Integer getRegistroAfectadoId() {
		return registroAfectadoId;
	}
	
	public void setRegistroAfectadoId(Integer registroAfectadoId) {
		this.registroAfectadoId = registroAfectadoId;
	}
	
	public String getCampoModificado() {
		return campoModificado;
	}
	
	public void setCampoModificado(String campoModificado) {
		this.campoModificado = campoModificado;
	}
	
	public String getValorAnterior() {
		return valorAnterior;
	}
	
	public void setValorAnterior(String valorAnterior) {
		this.valorAnterior = valorAnterior;
	}
	
	public String getValorNuevo() {
		return valorNuevo;
	}
	
	public void setValorNuevo(String valorNuevo) {
		this.valorNuevo = valorNuevo;
	}
	
	public String getResultadoAccion() {
		return resultadoAccion;
	}
	
	public void setResultadoAccion(String resultadoAccion) {
		this.resultadoAccion = resultadoAccion;
	}
}
