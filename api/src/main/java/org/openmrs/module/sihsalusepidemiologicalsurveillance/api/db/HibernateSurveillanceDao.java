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
		return sessionFactory.getCurrentSession().createCriteria(NotifiableEvent.class).list();
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
		// Foci are keyed by Address Hierarchy entries, not service locations. RF-21 is out of scope.
		return null;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<PeriodCaseCount> counts(NotifiableEvent event, String period, int fromYear, int toYear) {
		return sessionFactory.getCurrentSession().createCriteria(PeriodCaseCount.class).add(Restrictions.eq("event", event))
		        .add(Restrictions.eq("periodType", period.toUpperCase(java.util.Locale.ENGLISH)))
		        .add(Restrictions.between("year", fromYear, toYear)).addOrder(Order.asc("year"))
		        .addOrder(Order.asc("periodNumber")).list();
	}
	
	@Override
	public void replaceCounts(NotifiableEvent event, List<PeriodCaseCount> counts) {
		// Serialize refreshes per event. Natural-key updates preserve idempotence.
		lockEvent(event);
		for (PeriodCaseCount count : counts) {
			PeriodCaseCount existing = (PeriodCaseCount) sessionFactory.getCurrentSession()
			        .createCriteria(PeriodCaseCount.class).add(Restrictions.eq("event", event))
			        .add(Restrictions.eq("addressHierarchyEntryId", count.getAddressHierarchyEntryId()))
			        .add(Restrictions.eq("periodType", count.getPeriodType())).add(Restrictions.eq("year", count.getYear()))
			        .add(Restrictions.eq("periodNumber", count.getPeriodNumber()))
			        .add(Restrictions.eq("diagnosisType", count.getDiagnosisType())).uniqueResult();
			if (existing == null)
				save(count);
			else {
				existing.setCaseCount(count.getCaseCount());
				existing.setStartDate(count.getStartDate());
				existing.setEndDate(count.getEndDate());
				existing.setCalculationDate(count.getCalculationDate());
				existing.setChangedBy(count.getCreator());
				existing.setDateChanged(count.getCalculationDate());
				save(existing);
			}
		}
	}
	
	@Override
	public void lockEvent(NotifiableEvent event) {
		sessionFactory.getCurrentSession()
		        .createSQLQuery(
		            "select notifiable_event_id from notifiable_event where notifiable_event_id = :id for update")
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
	
	@Override
	public Integer populatedCenterId(String uuid) {
		Object value = sessionFactory.getCurrentSession().createSQLQuery(
		    "select e.address_hierarchy_entry_id from address_hierarchy_entry e join address_hierarchy_level l on l.address_hierarchy_level_id = e.level_id where e.uuid = :uuid and l.address_field = 'CITY_VILLAGE'")
		        .setString("uuid", uuid).uniqueResult();
		return value == null ? null : ((Number) value).intValue();
	}
	
	@Override
	public Integer districtIdForPopulatedCenter(Integer populatedCenterId) {
		Object value = sessionFactory.getCurrentSession().createSQLQuery(
		    "select p.address_hierarchy_entry_id from address_hierarchy_entry e join address_hierarchy_entry p on p.address_hierarchy_entry_id = e.parent_id join address_hierarchy_level l on l.address_hierarchy_level_id = p.level_id where e.address_hierarchy_entry_id = :id and l.address_field = 'COUNTY_DISTRICT'")
		        .setInteger("id", populatedCenterId).uniqueResult();
		return value == null ? null : ((Number) value).intValue();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<SurveillanceCase> surveillanceCases() {
		return sessionFactory.getCurrentSession().createCriteria(SurveillanceCase.class)
		        .add(Restrictions.eq("voided", false)).list();
	}
	
	@Override
	public SurveillanceCase caseByDiagnosis(org.openmrs.Diagnosis diagnosis) {
		return (SurveillanceCase) sessionFactory.getCurrentSession().createCriteria(SurveillanceCase.class)
		        .add(Restrictions.eq("diagnosis", diagnosis)).add(Restrictions.eq("voided", false)).uniqueResult();
	}
	
	@Override
	public String addressUuid(Integer id) {
		Object value = sessionFactory.getCurrentSession()
		        .createSQLQuery("select uuid from address_hierarchy_entry where address_hierarchy_entry_id = :id")
		        .setInteger("id", id).uniqueResult();
		return value == null ? null : value.toString();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> addressChildren(String field, String parentUuid) {
		String sql = "select e.uuid, e.name from address_hierarchy_entry e join address_hierarchy_level l on l.address_hierarchy_level_id = e.level_id "
		        + "left join address_hierarchy_entry p on p.address_hierarchy_entry_id = e.parent_id where l.address_field = :field "
		        + (parentUuid == null ? "and e.parent_id is null " : "and p.uuid = :parent ") + "order by e.name";
		org.hibernate.Query query = sessionFactory.getCurrentSession().createSQLQuery(sql).setString("field", field);
		if (parentUuid != null)
			query.setString("parent", parentUuid);
		return query.list();
	}
}
