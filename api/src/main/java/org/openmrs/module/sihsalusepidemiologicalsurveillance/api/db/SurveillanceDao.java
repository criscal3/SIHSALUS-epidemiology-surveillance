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
	
	List<EventoNotificable> events();
	
	List<ReglaAlertaBrote> rules(EventoNotificable event);
	
	FocoEpidemiologico focus(EventoNotificable event, String locationUuid);
	
	List<ConteoCasosPeriodo> counts(EventoNotificable event, String period, int fromYear, int toYear);
	
	void replaceCounts(EventoNotificable event, List<ConteoCasosPeriodo> counts);
	
	void lockPatient(Patient patient);
	
	void lockEvent(EventoNotificable event);
	
	List<Encounter> encounters(String encounterTypeUuid, Date from, Date to, String onsetConceptUuid);
	
	List<Encounter> possibleDuplicates(String encounterTypeUuid, Patient patient, List<String> diagnoses, Date from, Date to,
	        String onsetConceptUuid);
}
