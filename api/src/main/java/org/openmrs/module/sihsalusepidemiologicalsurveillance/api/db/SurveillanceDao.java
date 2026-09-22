package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db;

import java.util.Date;
import java.util.List;

import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public interface SurveillanceDao {
	
	<T extends BaseOpenmrsObject> T save(T entity);
	
	<T extends BaseOpenmrsObject> T byUuid(Class<T> type, String uuid);
	
	List<NotifiableEvent> events();
	
	NotifiableEvent eventByConcept(org.openmrs.Concept concept);
	
	List<OutbreakAlertRule> rules(NotifiableEvent event);
	
	EpidemiologicalFocus focus(NotifiableEvent event, String locationUuid);
	
	List<PeriodCaseCount> counts(NotifiableEvent event, String period, int fromYear, int toYear);
	
	void replaceCounts(NotifiableEvent event, List<PeriodCaseCount> counts);
	
	void lockPatient(Patient patient);
	
	void lockEvent(NotifiableEvent event);
	
	List<Encounter> encounters(Date from, Date to, String onsetConceptUuid);
	
	List<Encounter> possibleDuplicates(Patient patient, List<String> diagnoses, Date from, Date to, String onsetConceptUuid);
}
