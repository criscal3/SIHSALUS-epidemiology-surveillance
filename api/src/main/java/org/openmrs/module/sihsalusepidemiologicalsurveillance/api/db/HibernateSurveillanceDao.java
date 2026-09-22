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
	public List<NotifiableEvent> events() {
		return sessionFactory.getCurrentSession().createCriteria(NotifiableEvent.class).addOrder(Order.asc("name")).list();
	}
	
	@Override
	public NotifiableEvent eventByConcept(org.openmrs.Concept concept) {
		return (NotifiableEvent) sessionFactory.getCurrentSession().createCriteria(NotifiableEvent.class)
		        .add(Restrictions.eq("concept", concept)).uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<OutbreakAlertRule> rules(NotifiableEvent event) {
		return sessionFactory.getCurrentSession().createCriteria(OutbreakAlertRule.class)
		        .add(Restrictions.eq("event", event)).add(Restrictions.eq("active", true)).list();
	}
	
	@Override
	public EpidemiologicalFocus focus(NotifiableEvent event, String locationUuid) {
		return (EpidemiologicalFocus) sessionFactory.getCurrentSession().createCriteria(EpidemiologicalFocus.class)
		        .add(Restrictions.eq("event", event)).createAlias("location", "l")
		        .add(Restrictions.eq("l.uuid", locationUuid)).uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<PeriodCaseCount> counts(NotifiableEvent event, String period, int fromYear, int toYear) {
		return sessionFactory.getCurrentSession().createCriteria(PeriodCaseCount.class).add(Restrictions.eq("event", event))
		        .add(Restrictions.eq("periodType", period)).add(Restrictions.between("year", fromYear, toYear))
		        .addOrder(Order.asc("year")).addOrder(Order.asc("periodNumber")).list();
	}
	
	@Override
	public void replaceCounts(NotifiableEvent event, List<PeriodCaseCount> counts) {
		// Serialize refreshes per event. No truncate/drop and no writes to an analytic replica.
		lockEvent(event);
		sessionFactory.getCurrentSession().createQuery("delete from PeriodCaseCount where event = :event")
		        .setParameter("event", event).executeUpdate();
		for (PeriodCaseCount count : counts)
			save(count);
	}
	
	@Override
	public void lockEvent(NotifiableEvent event) {
		sessionFactory.getCurrentSession()
		        .createSQLQuery("select event_id from surveillance_notifiable_event where event_id = :id for update")
		        .setInteger("id", event.getId()).uniqueResult();
		sessionFactory.getCurrentSession().refresh(event);
	}
	
	@Override
	public void lockPatient(Patient patient) {
		sessionFactory.getCurrentSession().createSQLQuery("select patient_id from patient where patient_id = :id for update")
		        .setInteger("id", patient.getId()).uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Encounter> encounters(Date from, Date to, String onset) {
		return sessionFactory.getCurrentSession().createQuery(
		    "select distinct e from Diagnosis d join d.encounter e join e.obs o where d.voided = false and e.voided = false "
		            + "and e.patient.voided = false and o.voided = false "
		            + "and o.concept.uuid = :onset and o.valueDatetime >= :from and o.valueDatetime < :to")
		        .setString("onset", onset).setTimestamp("from", from).setTimestamp("to", to).list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Encounter> possibleDuplicates(Patient patient, List<String> diagnoses, Date from, Date to, String onset) {
		return sessionFactory.getCurrentSession()
		        .createQuery("select distinct d.encounter from Diagnosis d, Obs o "
		                + "where d.encounter = o.encounter and d.voided = false and d.encounter.voided = false "
		                + "and d.encounter.patient = :patient "
		                + "and d.diagnosis.coded.uuid in (:diagnoses) and o.voided = false and o.concept.uuid = :onset "
		                + "and o.valueDatetime >= :from and o.valueDatetime < :to")
		        .setParameter("patient", patient).setParameterList("diagnoses", diagnoses).setString("onset", onset)
		        .setTimestamp("from", from).setTimestamp("to", to).list();
	}
}
