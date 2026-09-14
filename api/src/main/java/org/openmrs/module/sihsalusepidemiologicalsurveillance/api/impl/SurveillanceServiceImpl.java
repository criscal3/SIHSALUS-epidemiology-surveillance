package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.openmrs.*;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.SurveillanceConstants;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class SurveillanceServiceImpl extends BaseOpenmrsService implements SurveillanceService {
	
	private SurveillanceDao dao;
	
	private ClinicalData clinical;
	
	private MetadataResolver metadata;
	
	private SurveillanceAccess access;
	
	private CaseValidator validator;
	
	private final CaseObservations reader = new CaseObservations();
	
	private final ReportCalculator calculator = new ReportCalculator();
	
	private final OutbreakEngine engine = new OutbreakEngine();
	
	public void setDao(SurveillanceDao value) {
		dao = value;
	}
	
	public void setClinical(ClinicalData value) {
		clinical = value;
	}
	
	public void setMetadata(MetadataResolver value) {
		metadata = value;
	}
	
	public void setAccess(SurveillanceAccess value) {
		access = value;
	}
	
	public void setValidator(CaseValidator value) {
		validator = value;
	}
	
	@Override
	public Map<String, Object> getMetadata() {
		access.require(SurveillanceConstants.ACCESS);
		Metadata m = metadata.get();
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("metadata", m);
		List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
		for (Metadata.Disease disease : m.diseases)
			events.add(eventJson(dao.byUuid(EventoNotificable.class, disease.eventUuid)));
		response.put("events", events);
		return response;
	}
	
	@Override
	public CaseResult registerCase(CaseRequest request) {
		User actor = access.require(SurveillanceConstants.REGISTER);
		access.require(SurveillanceConstants.VIEW);
		Metadata m = metadata.get();
		// Resolve only identity first so a replay remains valid after a visit closes or a lab result is corrected.
		if (request == null || !MetadataResolver.present(request.patientUuid) || !MetadataResolver.present(request.uuid))
			throw new SurveillanceException(422, "REQUIRED_FIELDS");
		Patient patient = clinical.patient(request.patientUuid);
		if (patient == null || patient.getVoided())
			throw new SurveillanceException(422, "INVALID_REFERENCE");
		dao.lockPatient(patient);
		Encounter existing = clinical.encounter(request.uuid);
		if (existing != null) {
			Obs onset = CaseObservations.find(existing, m.questions.get("onset"));
			if (existing.getVoided() || !patient.equals(existing.getPatient())
			        || !m.encounterTypeUuid.equals(existing.getEncounterType().getUuid()) || existing.getCreator() == null
			        || !existing.getCreator().equals(actor) || onset == null
			        || !fingerprint(request).equals(onset.getComment()))
				throw new SurveillanceException(409, "IDEMPOTENCY_CONFLICT");
			CaseResult result = assess(existing, m);
			result.replayed = true;
			return result;
		}
		CaseValidator.Validated v = validator.validate(request, m, actor);
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		List<String> diagnoses = new ArrayList<String>();
		for (Metadata.DiagnosisMapping mapping : v.disease.diagnoses)
			diagnoses.add(mapping.diagnosisConceptUuid);
		if (!dao.possibleDuplicates(m.encounterTypeUuid, v.patient, diagnoses,
		    calendar.date(v.onset.minusDays(m.duplicateWindowDays)),
		    calendar.date(v.onset.plusDays(m.duplicateWindowDays + 1)), m.questions.get("onset")).isEmpty())
			throw new SurveillanceException(409, "POSSIBLE_DUPLICATE",
			        Arrays.asList("patientUuid", "eventUuid", "onsetDate"));
		EventoNotificable event = dao.byUuid(EventoNotificable.class, request.eventUuid);
		// Serialize threshold crossings across patients; READ_COMMITTED observes the preceding commit after waiting.
		dao.lockEvent(event);
		Encounter encounter = new Encounter();
		encounter.setUuid(request.uuid);
		encounter.setPatient(v.patient);
		encounter.setVisit(v.source.getVisit());
		encounter.setLocation(v.location);
		encounter.setEncounterDatetime(v.source.getEncounterDatetime());
		encounter.setEncounterType(clinical.encounterType(m.encounterTypeUuid));
		EncounterProvider participation = new EncounterProvider();
		participation.setEncounter(encounter);
		participation.setProvider(v.provider);
		participation.setEncounterRole(clinical.encounterRole(m.encounterRoleUuid));
		participation.setCreator(actor);
		participation.setDateCreated(new Date());
		encounter.getEncounterProviders().add(participation);
		encounter.setCreator(actor);
		encounter.setDateCreated(new Date());
		coded(encounter, m, "event", event.getConcept());
		coded(encounter, m, "status", clinical.concept(MetadataResolver.choice(m.statuses, request.status).conceptUuid));
		coded(encounter, m, "origin", clinical.concept(MetadataResolver.choice(m.origins, request.origin).conceptUuid));
		coded(encounter, m, "severity",
		    clinical.concept(MetadataResolver.choice(v.disease.severities, request.severity).conceptUuid));
		if (!v.disease.species.isEmpty())
			coded(encounter, m, "species",
			    clinical.concept(MetadataResolver.choice(v.disease.species, request.species).conceptUuid));
		Obs onset = obs(encounter, m, "onset");
		onset.setValueDatetime(calendar.date(v.onset));
		onset.setComment(fingerprint(request));
		text(encounter, m, "sourceEncounter", v.source.getUuid());
		if (v.laboratory != null) {
			Obs reference = text(encounter, m, "laboratoryResult", v.laboratory.getUuid());
			reference.setOrder(v.laboratory.getOrder());
		}
		if (v.ethnicity != null)
			text(encounter, m, "ethnicity", v.ethnicity);
		if (v.pregnant != null)
			obs(encounter, m, "pregnancy").setValueCoded(v.pregnant ? clinical.trueConcept() : clinical.falseConcept());
		clinical.saveEncounter(encounter);
		Diagnosis diagnosis = new Diagnosis();
		diagnosis.setEncounter(encounter);
		diagnosis.setPatient(v.patient);
		diagnosis.setDiagnosis(new CodedOrFreeText(v.diagnosis, null, null));
		diagnosis.setRank(1);
		diagnosis.setCertainty("CONFIRMED".equals(request.status) ? ConditionVerificationStatus.CONFIRMED
		        : ConditionVerificationStatus.PROVISIONAL);
		if ("DISCARDED".equals(request.status)) {
			diagnosis.setVoided(true);
			diagnosis.setVoidedBy(actor);
			diagnosis.setDateVoided(new Date());
			diagnosis.setVoidReason("Surveillance classification: discarded");
		}
		clinical.saveDiagnosis(diagnosis);
		encounter.setDiagnoses(new HashSet<Diagnosis>(Arrays.asList(diagnosis)));
		CaseResult result = assess(encounter, m);
		audit(actor, "registro", "encounter", encounter.getId());
		if (!result.immediateAlerts.isEmpty() || !result.outbreakAlerts.isEmpty())
			clinical.alert(actor, "Vigilancia epidemiologica: revisar alertas del registro " + encounter.getUuid());
		return result;
	}
	
	@Override
	public CaseResult getCase(String uuid) {
		User actor = access.require(SurveillanceConstants.VIEW);
		Metadata m = metadata.get();
		Encounter encounter = clinical.encounter(uuid);
		if (encounter == null || encounter.getVoided()
		        || !m.encounterTypeUuid.equals(encounter.getEncounterType().getUuid()))
			throw new SurveillanceException(404, "CASE_NOT_FOUND");
		audit(actor, "consulta", "encounter", encounter.getId());
		return assess(encounter, m);
	}
	
	private CaseResult assess(Encounter encounter, Metadata m) {
		Map<String, String> eventMap = eventConcepts();
		CaseRecord record = reader.read(encounter, m, eventMap);
		EventoNotificable event = dao.byUuid(EventoNotificable.class, record.eventUuid);
		if (event == null || record.onset == null)
			throw new SurveillanceException(422, "INVALID_CASE_DATA");
		CaseResult result = new CaseResult();
		result.uuid = encounter.getUuid();
		for (Diagnosis diagnosis : encounter.getDiagnoses()) {
			if (diagnosis.getDiagnosis() != null && diagnosis.getDiagnosis().getCoded() != null) {
				result.diagnosisConceptUuid = diagnosis.getDiagnosis().getCoded().getUuid();
				result.icd10 = MetadataResolver.icd10(diagnosis.getDiagnosis().getCoded(), m);
				break;
			}
		}
		result.periodicity = event.getPeriodicidad();
		result.deadlineDays = event.getPlazoDias();
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		LocalDate week = calendar.start(record.onset, "semana");
		List<CaseRecord> recent = records(m, week.minusWeeks(11), week.plusWeeks(1), eventMap);
		if (recent.stream().noneMatch(r -> record.encounterUuid.equals(r.encounterUuid)))
			recent.add(record);
		List<ConteoCasosPeriodo> history = dao.counts(event, "semana",
		    calendar.year(week.minusWeeks(11), "semana") - m.historicalYears, calendar.year(week, "semana") - 1);
		engine.evaluate(record, dao.focus(event, record.locationUuid), dao.rules(event), recent, history, m, result);
		if (!result.immediateAlerts.isEmpty()) {
			result.periodicity = "inmediata";
			result.deadlineDays = 0;
		}
		return result;
	}
	
	@Override
	public SurveillanceReport report(String eventUuid, String from, String to, String period) {
		User actor = access.require(SurveillanceConstants.REPORT);
		Metadata m = metadata.get();
		MetadataResolver.disease(m, eventUuid);
		EventoNotificable event = dao.byUuid(EventoNotificable.class, eventUuid);
		LocalDate start, end;
		try {
			start = LocalDate.parse(from);
			end = LocalDate.parse(to);
		}
		catch (Exception ex) {
			throw new SurveillanceException(422, "INVALID_DATE_RANGE");
		}
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		calendar.number(start, period);
		if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 731 || end.isAfter(calendar.today()))
			throw new SurveillanceException(422, "INVALID_DATE_RANGE");
		if (start.isBefore(LocalDate.parse(m.surveillanceStartDate)))
			throw new SurveillanceException(422, "OUTSIDE_COVERAGE");
		List<CaseRecord> records = records(m, start, end.plusDays(1), eventConcepts());
		List<ConteoCasosPeriodo> counts = dao.counts(event, period, calendar.year(start, period) - m.historicalYears,
		    calendar.year(end, period) - 1);
		SurveillanceReport report = calculator.calculate(eventUuid, start, end, period, records, counts, m);
		audit(actor, "generacion de reportes", "evento_notificable", event.getId());
		return report;
	}
	
	@Override
	public void refreshCounts() {
		access.require(SurveillanceConstants.MANAGE);
		Metadata m = metadata.get();
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		LocalDate start = LocalDate.parse(m.surveillanceStartDate), today = calendar.today();
		List<CaseRecord> records = records(m, start, today.plusDays(1), eventConcepts());
		for (EventoNotificable event : dao.events())
			dao.replaceCounts(event, calculator.aggregate(event, records, m, today));
	}
	
	private List<CaseRecord> records(Metadata m, LocalDate from, LocalDate until, Map<String, String> events) {
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		List<CaseRecord> records = new ArrayList<CaseRecord>();
		for (Encounter encounter : dao.encounters(m.encounterTypeUuid, calendar.date(from), calendar.date(until),
		    m.questions.get("onset"))) {
			CaseRecord record = reader.read(encounter, m, events);
			if (record.eventUuid != null)
				records.add(record);
		}
		return records;
	}
	
	private Map<String, String> eventConcepts() {
		Map<String, String> values = new HashMap<String, String>();
		for (EventoNotificable event : dao.events())
			values.put(event.getConcept().getUuid(), event.getUuid());
		return values;
	}
	
	private Obs obs(Encounter e, Metadata m, String key) {
		Obs obs = new Obs(e.getPatient(), clinical.concept(m.questions.get(key)), e.getEncounterDatetime(), e.getLocation());
		e.addObs(obs);
		return obs;
	}
	
	private void coded(Encounter e, Metadata m, String key, Concept value) {
		obs(e, m, key).setValueCoded(value);
	}
	
	private Obs text(Encounter e, Metadata m, String key, String value) {
		Obs obs = obs(e, m, key);
		obs.setValueText(value);
		return obs;
	}
	
	private void audit(User actor, String action, String entity, Integer id) {
		AuditoriaVigilancia audit = new AuditoriaVigilancia();
		audit.setUsuario(actor);
		audit.setFechaHora(new Date());
		audit.setTipoAccion(action);
		audit.setEntidadAfectada(entity);
		audit.setRegistroAfectadoId(id);
		audit.setResultadoAccion("SUCCESS");
		dao.save(audit);
	}
	
	public static String fingerprint(CaseRequest r) {
		try {
			StringBuilder canonical = new StringBuilder();
			for (String value : Arrays.asList(r.uuid, r.patientUuid, r.sourceEncounterUuid, r.providerUuid, r.locationUuid,
			    r.eventUuid, r.status, r.severity, r.origin, r.species, r.onsetDate, r.laboratoryResultUuid)) {
				String text = value == null ? "" : value;
				canonical.append(text.length()).append(':').append(text);
			}
			byte[] digest = MessageDigest.getInstance("SHA-256")
			        .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : digest)
				hex.append(String.format("%02x", b & 255));
			return hex.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Fingerprint unavailable");
		}
	}
	
	@Override
	public Map<String, Object> saveEvent(Map<String, Object> body) {
		User actor = access.require(SurveillanceConstants.MANAGE);
		String uuid = string(body, "uuid"), name = string(body, "name"), periodicity = string(body, "periodicity");
		Concept concept = clinical.concept(string(body, "conceptUuid"));
		int deadline = number(body, "deadlineDays");
		if (concept == null || concept.getRetired() || name.isEmpty() || name.length() > 255 || deadline < 0
		        || deadline > 365 || !Arrays.asList("semanal", "inmediata", "diaria").contains(periodicity))
			throw new SurveillanceException(422, "INVALID_EVENT");
		EventoNotificable event = dao.byUuid(EventoNotificable.class, uuid);
		if (event == null) {
			event = new EventoNotificable();
			event.setUuid(uuid);
		} else if (!concept.equals(event.getConcept()))
			throw new SurveillanceException(409, "EVENT_CONCEPT_IMMUTABLE");
		event.setConcept(concept);
		event.setNombre(name);
		event.setPeriodicidad(periodicity);
		event.setPlazoDias(deadline);
		dao.save(event);
		audit(actor, "configuracion", "evento_notificable", event.getId());
		return eventJson(event);
	}
	
	@Override
	public Map<String, Object> saveRule(Map<String, Object> body) {
		User actor = access.require(SurveillanceConstants.MANAGE);
		String uuid = string(body, "uuid"), eventUuid = string(body, "eventUuid"), type = string(body, "condition");
		EventoNotificable event = dao.byUuid(EventoNotificable.class, eventUuid);
		int window = body.containsKey("windowWeeks") ? number(body, "windowWeeks") : 2;
		if (event == null || !Arrays.asList("EPIDEMIC", "SUSTAINED", "ELIMINATED_FOCUS").contains(type) || window < 2
		        || window > 12 || !(body.get("active") instanceof Boolean))
			throw new SurveillanceException(422, "INVALID_OUTBREAK_RULE");
		ReglaAlertaBrote rule = dao.byUuid(ReglaAlertaBrote.class, uuid);
		if (rule == null) {
			rule = new ReglaAlertaBrote();
			rule.setUuid(uuid);
		}
		rule.setEvento(event);
		rule.setTipoCondicion(type);
		rule.setVentanaSemanas("SUSTAINED".equals(type) ? window : null);
		rule.setValorUmbral("EPIDEMIC".equals(type) ? 75d : "SUSTAINED".equals(type) ? 50d : null);
		rule.setActiva((Boolean) body.get("active"));
		dao.save(rule);
		audit(actor, "configuracion", "regla_alerta_brote", rule.getId());
		return Collections.<String, Object> singletonMap("uuid", rule.getUuid());
	}
	
	private String string(Map<String, Object> body, String key) {
		if (body == null || !(body.get(key) instanceof String))
			throw new SurveillanceException(422, "REQUIRED_FIELDS");
		String value = ((String) body.get(key)).trim();
		if (key.toLowerCase(Locale.ROOT).contains("uuid")) {
			try {
				if (!UUID.fromString(value).toString().equalsIgnoreCase(value))
					throw new IllegalArgumentException();
			}
			catch (Exception ex) {
				throw new SurveillanceException(422, "INVALID_REFERENCE");
			}
		}
		return value;
	}
	
	private int number(Map<String, Object> body, String key) {
		Object value = body.get(key);
		if (!(value instanceof Number) || ((Number) value).doubleValue() != ((Number) value).intValue())
			throw new SurveillanceException(422, "INVALID_NUMBER");
		return ((Number) value).intValue();
	}
	
	private Map<String, Object> eventJson(EventoNotificable event) {
		Map<String, Object> value = new LinkedHashMap<String, Object>();
		value.put("uuid", event.getUuid());
		value.put("name", event.getNombre());
		value.put("conceptUuid", event.getConcept().getUuid());
		value.put("periodicity", event.getPeriodicidad());
		value.put("deadlineDays", event.getPlazoDias());
		return value;
	}
}
