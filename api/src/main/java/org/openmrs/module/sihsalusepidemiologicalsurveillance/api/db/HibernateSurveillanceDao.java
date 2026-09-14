package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class HibernateSurveillanceDao implements SurveillanceDao {
	
	private DbSessionFactory sessionFactory;
	
	public void setSessionFactory(DbSessionFactory value) {
		sessionFactory = value;
	}
	
	@Override
	public <T extends BaseOpenmrsObject> T save(T entity) {
		if (entity.getUuid() == null)
			entity.setUuid(UUID.randomUUID().toString());
		sessionFactory.getCurrentSession().saveOrUpdate(entity);
		return entity;
	}
	
	@Override
	public <T extends BaseOpenmrsObject> T byUuid(Class<T> type, String uuid) {
		return type.cast(
		    sessionFactory.getCurrentSession().createCriteria(type).add(Restrictions.eq("uuid", uuid)).uniqueResult());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<EventoNotificable> events() {
		return sessionFactory.getCurrentSession().createCriteria(EventoNotificable.class).addOrder(Order.asc("nombre"))
		        .list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ReglaAlertaBrote> rules(EventoNotificable event) {
		return sessionFactory.getCurrentSession().createCriteria(ReglaAlertaBrote.class)
		        .add(Restrictions.eq("evento", event)).add(Restrictions.eq("activa", true)).list();
	}
	
	@Override
	public FocoEpidemiologico focus(EventoNotificable event, String locationUuid) {
		return (FocoEpidemiologico) sessionFactory.getCurrentSession().createCriteria(FocoEpidemiologico.class)
		        .add(Restrictions.eq("evento", event)).createAlias("location", "l")
		        .add(Restrictions.eq("l.uuid", locationUuid)).uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ConteoCasosPeriodo> counts(EventoNotificable event, String period, int fromYear, int toYear) {
		return sessionFactory.getCurrentSession().createCriteria(ConteoCasosPeriodo.class)
		        .add(Restrictions.eq("evento", event)).add(Restrictions.eq("tipoPeriodo", period))
		        .add(Restrictions.between("anio", fromYear, toYear)).addOrder(Order.asc("anio"))
		        .addOrder(Order.asc("numeroPeriodo")).list();
	}
	
	@Override
	public void replaceCounts(EventoNotificable event, List<ConteoCasosPeriodo> counts) {
		// Serialize refreshes per event. No truncate/drop and no writes to an analytic replica.
		lockEvent(event);
		sessionFactory.getCurrentSession().createQuery("delete from ConteoCasosPeriodo where evento = :event")
		        .setParameter("event", event).executeUpdate();
		for (ConteoCasosPeriodo count : counts)
			save(count);
	}
	
	@Override
	public void lockEvent(EventoNotificable event) {
		sessionFactory.getCurrentSession()
		        .createSQLQuery(
		            "select evento_notificable_id from evento_notificable where evento_notificable_id = :id for update")
		        .setInteger("id", event.getId()).uniqueResult();
	}
	
	@Override
	public void lockPatient(Patient patient) {
		sessionFactory.getCurrentSession().createSQLQuery("select patient_id from patient where patient_id = :id for update")
		        .setInteger("id", patient.getId()).uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Encounter> encounters(String type, Date from, Date to, String onset) {
		return sessionFactory.getCurrentSession().createQuery(
		    "select distinct e from Diagnosis d join d.encounter e join e.obs o where d.voided = false and e.voided = false "
		            + "and e.patient.voided = false and e.encounterType.uuid = :type and o.voided = false "
		            + "and o.concept.uuid = :onset and o.valueDatetime >= :from and o.valueDatetime < :to")
		        .setString("type", type).setString("onset", onset).setTimestamp("from", from).setTimestamp("to", to).list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Encounter> possibleDuplicates(String type, Patient patient, List<String> diagnoses, Date from, Date to,
	        String onset) {
		return sessionFactory.getCurrentSession()
		        .createQuery("select distinct d.encounter from Diagnosis d, Obs o "
		                + "where d.encounter = o.encounter and d.voided = false and d.encounter.voided = false "
		                + "and d.encounter.patient = :patient and d.encounter.encounterType.uuid = :type "
		                + "and d.diagnosis.coded.uuid in (:diagnoses) and o.voided = false and o.concept.uuid = :onset "
		                + "and o.valueDatetime >= :from and o.valueDatetime < :to")
		        .setParameter("patient", patient).setString("type", type).setParameterList("diagnoses", diagnoses)
		        .setString("onset", onset).setTimestamp("from", from).setTimestamp("to", to).list();
	}
}
