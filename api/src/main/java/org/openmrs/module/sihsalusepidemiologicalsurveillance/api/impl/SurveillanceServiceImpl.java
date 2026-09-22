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
	
	private ClinicalCatalogService catalog;
	
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
	
	public void setCatalog(ClinicalCatalogService value) {
		catalog = value;
	}
	
	public void setAccess(SurveillanceAccess value) {
		access = value;
	}
	
	public void setValidator(CaseValidator value) {
		validator = value;
	}
	
	@Override
	public Map<String, Object> getCatalog() {
		access.require(SurveillanceConstants.ACCESS);
		ClinicalCatalog m = catalog.get();
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("catalog", m);
		List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
		Set<String> includedUuids = new HashSet<String>();
		for (ClinicalCatalog.Disease disease : m.diseases) {
			NotifiableEvent event = dao.byUuid(NotifiableEvent.class, disease.eventUuid);
			if (event != null && !event.isRetired()) {
				events.add(eventJson(event));
				includedUuids.add(event.getUuid());
			}
		}
		for (NotifiableEvent event : dao.events()) {
			if (!event.isRetired() && !includedUuids.contains(event.getUuid())) {
				events.add(eventJson(event));
				includedUuids.add(event.getUuid());
			}
		}
		response.put("events", events);
		return response;
	}
	
	@Override
	public Map<String, Object> healthcheck() {
		access.require(SurveillanceConstants.ACCESS);
		return Collections.<String, Object> singletonMap("status", "UP");
	}
	
	@Override
	public List<Map<String, Object>> getEvents(boolean includeRetired) {
		access.require(SurveillanceConstants.ACCESS);
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (NotifiableEvent event : dao.events())
			if (includeRetired || !event.isRetired())
				result.add(eventJson(event));
		return result;
	}
	
	@Override
	public Map<String, Object> getEvent(String uuid) {
		access.require(SurveillanceConstants.ACCESS);
		return eventJson(requiredEvent(uuid));
	}
	
	private NotifiableEvent requiredEvent(String uuid) {
		Map<String, Object> reference = Collections.<String, Object> singletonMap("uuid", uuid);
		NotifiableEvent event = dao.byUuid(NotifiableEvent.class, string(reference, "uuid"));
		if (event == null)
			throw new SurveillanceException(404, "EVENT_NOT_FOUND");
		return event;
	}
	
	@Override
	public Map<String, Object> updateEvent(String uuid, Map<String, Object> body) {
		access.require(SurveillanceConstants.MANAGE);
		NotifiableEvent event = requiredEvent(uuid);
		if (body == null)
			throw new SurveillanceException(422, "REQUIRED_FIELDS");
		if (body.containsKey("uuid") && !uuid.equals(body.get("uuid")))
			throw new SurveillanceException(409, "EVENT_UUID_MISMATCH");
		Map<String, Object> update = new LinkedHashMap<String, Object>(body);
		update.put("uuid", uuid);
		dao.lockEvent(event);
		if (event.isRetired())
			throw new SurveillanceException(409, "EVENT_RETIRED");
		return writeEvent(update, event);
	}
	
	@Override
	public void deleteEvent(String uuid) {
		User actor = access.require(SurveillanceConstants.MANAGE);
		NotifiableEvent event = requiredEvent(uuid);
		dao.lockEvent(event);
		if (!event.isRetired()) {
			event.setRetired(true);
			dao.save(event);
			audit(actor, "delete", "NotifiableEvent", event.getId());
		}
	}
	
	@Override
	public CaseResult registerCase(CaseRequest request) {
		User actor = access.require(SurveillanceConstants.REGISTER);
		access.require(SurveillanceConstants.VIEW);
		ClinicalCatalog m = catalog.get();
		// Resolve only identity first so a replay remains valid after a visit closes or a lab result is corrected.
		if (request == null || !ClinicalCatalogService.present(request.patientUuid)
		        || !ClinicalCatalogService.present(request.uuid))
			throw new SurveillanceException(422, "REQUIRED_FIELDS");
		Patient patient = clinical.patient(request.patientUuid);
		if (patient == null || patient.getVoided())
			throw new SurveillanceException(422, "INVALID_REFERENCE");
		dao.lockPatient(patient);
		Encounter existing = clinical.encounter(request.sourceEncounterUuid);
		if (existing != null && !existing.getVoided() && patient.equals(existing.getPatient())) {
			Obs onset = CaseObservations.find(existing, m.questions.get("onset"));
			if (onset != null && (onset.getCreator() == null || !onset.getCreator().equals(actor)
			        || !fingerprint(request).equals(onset.getComment())))
				throw new SurveillanceException(409, "IDEMPOTENCY_CONFLICT");
			if (onset != null) {
				CaseResult result = assess(existing, m);
				result.replayed = true;
				return result;
			}
		}
		CaseValidator.Validated v = validator.validate(request, m, actor);
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		List<String> diagnoses = new ArrayList<String>();
		for (ClinicalCatalog.DiagnosisMapping mapping : v.disease.diagnoses)
			diagnoses.add(mapping.diagnosisConceptUuid);
		if (!dao.possibleDuplicates(v.patient, diagnoses, calendar.date(v.onset.minusDays(m.duplicateWindowDays)),
		    calendar.date(v.onset.plusDays(m.duplicateWindowDays + 1)), m.questions.get("onset")).isEmpty())
			throw new SurveillanceException(409, "POSSIBLE_DUPLICATE",
			        Arrays.asList("patientUuid", "eventUuid", "onsetDate"));
		NotifiableEvent event = dao.byUuid(NotifiableEvent.class, request.eventUuid);
		if (event == null)
			throw new SurveillanceException(404, "EVENT_NOT_FOUND");
		// Serialize threshold crossings across patients; READ_COMMITTED observes the preceding commit after waiting.
		dao.lockEvent(event);
		if (event.isRetired())
			throw new SurveillanceException(409, "EVENT_RETIRED");
		Encounter encounter = v.source;
		coded(encounter, m, "event", event.getConcept());
		coded(encounter, m, "status",
		    clinical.concept(ClinicalCatalogService.choice(m.statuses, request.status).conceptUuid));
		coded(encounter, m, "origin",
		    clinical.concept(ClinicalCatalogService.choice(m.origins, request.origin).conceptUuid));
		coded(encounter, m, "severity",
		    clinical.concept(ClinicalCatalogService.choice(v.disease.severities, request.severity).conceptUuid));
		if (!v.disease.species.isEmpty())
			coded(encounter, m, "species",
			    clinical.concept(ClinicalCatalogService.choice(v.disease.species, request.species).conceptUuid));
		Obs onset = obs(encounter, m, "onset");
		onset.setValueDatetime(calendar.date(v.onset));
		onset.setComment(fingerprint(request));
		onset.setCreator(actor);
		if (v.laboratory != null) {
			Obs reference = text(encounter, m, "laboratoryResult", v.laboratory.getUuid());
			reference.setOrder(v.laboratory.getOrder());
		}
		if (v.ethnicity != null && CaseObservations.find(encounter, m.questions.get("ethnicity")) == null)
			coded(encounter, m, "ethnicity", clinical.concept(v.ethnicity));
		if (v.pregnant != null && CaseObservations.find(encounter, m.questions.get("pregnancy")) == null)
			obs(encounter, m, "pregnancy").setValueCoded(v.pregnant ? clinical.trueConcept() : clinical.falseConcept());
		clinical.saveEncounter(encounter);
		Diagnosis diagnosis = null;
		Set<Diagnosis> encounterDiagnoses = encounter.getDiagnoses();
		if (encounterDiagnoses == null) {
			encounterDiagnoses = new HashSet<Diagnosis>();
			encounter.setDiagnoses(encounterDiagnoses);
		}
		for (Diagnosis candidate : encounterDiagnoses)
			if (!candidate.getVoided() && candidate.getDiagnosis() != null
			        && v.diagnosis.equals(candidate.getDiagnosis().getCoded())) {
				diagnosis = candidate;
				break;
			}
		if (diagnosis == null && !"DISCARDED".equals(request.status)) {
			diagnosis = new Diagnosis();
			diagnosis.setEncounter(encounter);
			diagnosis.setPatient(v.patient);
			diagnosis.setDiagnosis(new CodedOrFreeText(v.diagnosis, null, null));
			diagnosis.setRank(1);
			diagnosis.setCertainty("CONFIRMED".equals(request.status) ? ConditionVerificationStatus.CONFIRMED
			        : ConditionVerificationStatus.PROVISIONAL);
			clinical.saveDiagnosis(diagnosis);
		}
		if (diagnosis != null)
			encounterDiagnoses.add(diagnosis);
		CaseResult result = assess(encounter, m);
		audit(actor, "registro", "encounter", encounter.getId());
		if (!result.immediateAlerts.isEmpty() || !result.outbreakAlerts.isEmpty())
			clinical.alert(actor, "Vigilancia epidemiologica: revisar alertas del registro " + encounter.getUuid());
		return result;
	}
	
	@Override
	public CaseResult getCase(String uuid) {
		User actor = access.require(SurveillanceConstants.VIEW);
		ClinicalCatalog m = catalog.get();
		Encounter encounter = clinical.encounter(uuid);
		if (encounter == null || encounter.getVoided() || CaseObservations.find(encounter, m.questions.get("event")) == null)
			throw new SurveillanceException(404, "CASE_NOT_FOUND");
		audit(actor, "consulta", "encounter", encounter.getId());
		return assess(encounter, m);
	}
	
	private CaseResult assess(Encounter encounter, ClinicalCatalog m) {
		Map<String, String> eventMap = eventConcepts();
		CaseRecord record = reader.read(encounter, m, eventMap);
		NotifiableEvent event = dao.byUuid(NotifiableEvent.class, record.eventUuid);
		if (event == null || record.onset == null)
			throw new SurveillanceException(422, "INVALID_CASE_DATA");
		CaseResult result = new CaseResult();
		result.uuid = encounter.getUuid();
		ClinicalCatalog.Disease disease = ClinicalCatalogService.disease(m, record.eventUuid);
		String species = CaseObservations.choiceKey(disease.species, CaseObservations.coded(encounter, m, "species"));
		ClinicalCatalog.DiagnosisMapping selected = null;
		for (ClinicalCatalog.DiagnosisMapping mapping : disease.diagnoses)
			if (Objects.equals(mapping.severity, record.severity) && Objects.equals(mapping.species, species)) {
				selected = mapping;
				break;
			}
		if (selected == null && "SEVERE".equals(record.severity))
			for (ClinicalCatalog.DiagnosisMapping mapping : disease.diagnoses)
				if ("SEVERE".equals(mapping.severity) && mapping.species == null)
					selected = mapping;
		if (selected == null)
			throw new SurveillanceException(422, "INVALID_CASE_DATA");
		result.diagnosisConceptUuid = selected.diagnosisConceptUuid;
		result.icd10 = selected.icd10Code;
		result.periodicity = event.getPeriodicity();
		result.deadlineDays = event.getDeadlineDays();
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		LocalDate week = calendar.start(record.onset, "semana");
		List<CaseRecord> recent = records(m, week.minusWeeks(11), week.plusWeeks(1), eventMap);
		if (recent.stream().noneMatch(r -> record.encounterUuid.equals(r.encounterUuid)))
			recent.add(record);
		List<PeriodCaseCount> history = dao.counts(event, "semana",
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
		ClinicalCatalog m = catalog.get();
		ClinicalCatalogService.disease(m, eventUuid);
		NotifiableEvent event = dao.byUuid(NotifiableEvent.class, eventUuid);
		if (event == null)
			throw new SurveillanceException(404, "EVENT_NOT_FOUND");
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
		List<PeriodCaseCount> counts = dao.counts(event, period, calendar.year(start, period) - m.historicalYears,
		    calendar.year(end, period) - 1);
		SurveillanceReport report = calculator.calculate(eventUuid, start, end, period, records, counts, m);
		audit(actor, "generacion de reportes", "NotifiableEvent", event.getId());
		return report;
	}
	
	@Override
	public void refreshCounts() {
		access.require(SurveillanceConstants.MANAGE);
		ClinicalCatalog m = catalog.get();
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		LocalDate start = LocalDate.parse(m.surveillanceStartDate), today = calendar.today();
		List<CaseRecord> records = records(m, start, today.plusDays(1), eventConcepts());
		for (NotifiableEvent event : dao.events())
			dao.replaceCounts(event, calculator.aggregate(event, records, m, today));
	}
	
	private List<CaseRecord> records(ClinicalCatalog m, LocalDate from, LocalDate until, Map<String, String> events) {
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		List<CaseRecord> records = new ArrayList<CaseRecord>();
		for (Encounter encounter : dao.encounters(calendar.date(from), calendar.date(until), m.questions.get("onset"))) {
			CaseRecord record = reader.read(encounter, m, events);
			if (record.eventUuid != null)
				records.add(record);
		}
		return records;
	}
	
	private Map<String, String> eventConcepts() {
		Map<String, String> values = new HashMap<String, String>();
		for (NotifiableEvent event : dao.events())
			values.put(event.getConcept().getUuid(), event.getUuid());
		return values;
	}
	
	private Obs obs(Encounter e, ClinicalCatalog m, String key) {
		Concept question = clinical.concept(m.questions.get(key));
		if (question == null || question.getRetired())
			throw new SurveillanceException(503, "CLINICAL_CONCEPT_UNAVAILABLE", Arrays.asList(key));
		String datatype = "onset".equals(key) ? "Date" : "laboratoryResult".equals(key) ? "Text" : "Coded";
		if (question.getDatatype() == null || !datatype.equals(question.getDatatype().getName()))
			throw new SurveillanceException(503, "CLINICAL_DATATYPE_MISMATCH", Arrays.asList(key));
		Obs obs = new Obs(e.getPatient(), question, e.getEncounterDatetime(), e.getLocation());
		e.addObs(obs);
		return obs;
	}
	
	private void coded(Encounter e, ClinicalCatalog m, String key, Concept value) {
		if (value == null || value.getRetired())
			throw new SurveillanceException(503, "CLINICAL_CONCEPT_UNAVAILABLE", Arrays.asList(key));
		obs(e, m, key).setValueCoded(value);
	}
	
	private Obs text(Encounter e, ClinicalCatalog m, String key, String value) {
		Obs obs = obs(e, m, key);
		obs.setValueText(value);
		return obs;
	}
	
	private void audit(User actor, String action, String entity, Integer id) {
		SurveillanceAudit audit = new SurveillanceAudit();
		audit.setUser(actor);
		audit.setTimestamp(new Date());
		audit.setActionType(action);
		audit.setAffectedEntity(entity);
		audit.setAffectedRecordId(id);
		audit.setResult("SUCCESS");
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
		access.require(SurveillanceConstants.MANAGE);
		String uuid = string(body, "uuid");
		if (dao.byUuid(NotifiableEvent.class, uuid) != null)
			throw new SurveillanceException(409, "EVENT_ALREADY_EXISTS");
		return writeEvent(body, null);
	}
	
	private Map<String, Object> writeEvent(Map<String, Object> body, NotifiableEvent existing) {
		User actor = access.require(SurveillanceConstants.MANAGE);
		String uuid = string(body, "uuid"), name = string(body, "name"), periodicity = string(body, "periodicity");
		Concept concept = clinical.concept(string(body, "conceptUuid"));
		int deadline = number(body, "deadlineDays");
		if (concept == null || concept.getRetired() || name.isEmpty() || name.length() > 255 || deadline < 0
		        || deadline > 365 || !Arrays.asList("semanal", "inmediata", "diaria").contains(periodicity))
			throw new SurveillanceException(422, "INVALID_EVENT");
		NotifiableEvent event = existing;
		if (event == null) {
			if (dao.eventByConcept(concept) != null)
				throw new SurveillanceException(409, "EVENT_CONCEPT_ALREADY_EXISTS");
			event = new NotifiableEvent();
			event.setUuid(uuid);
		} else if (!concept.equals(event.getConcept()))
			throw new SurveillanceException(409, "EVENT_CONCEPT_IMMUTABLE");
		event.setConcept(concept);
		event.setName(name);
		event.setPeriodicity(periodicity);
		event.setDeadlineDays(deadline);
		dao.save(event);
		audit(actor, "configuracion", "NotifiableEvent", event.getId());
		return eventJson(event);
	}
	
	@Override
	public Map<String, Object> saveRule(Map<String, Object> body) {
		User actor = access.require(SurveillanceConstants.MANAGE);
		String uuid = string(body, "uuid"), eventUuid = string(body, "eventUuid"), type = string(body, "condition");
		NotifiableEvent event = dao.byUuid(NotifiableEvent.class, eventUuid);
		int window = body.containsKey("windowWeeks") ? number(body, "windowWeeks") : 2;
		if (event == null || event.isRetired() || !Arrays.asList("EPIDEMIC", "SUSTAINED", "ELIMINATED_FOCUS").contains(type)
		        || window < 2 || window > 12 || !(body.get("active") instanceof Boolean))
			throw new SurveillanceException(422, "INVALID_OUTBREAK_RULE");
		OutbreakAlertRule rule = dao.byUuid(OutbreakAlertRule.class, uuid);
		if (rule == null) {
			rule = new OutbreakAlertRule();
			rule.setUuid(uuid);
		}
		rule.setEvent(event);
		rule.setConditionType(type);
		rule.setWindowWeeks("SUSTAINED".equals(type) ? window : null);
		rule.setThreshold("EPIDEMIC".equals(type) ? 75d : "SUSTAINED".equals(type) ? 50d : null);
		rule.setActive((Boolean) body.get("active"));
		dao.save(rule);
		audit(actor, "configuracion", "OutbreakAlertRule", rule.getId());
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
	
	private Map<String, Object> eventJson(NotifiableEvent event) {
		Map<String, Object> value = new LinkedHashMap<String, Object>();
		value.put("uuid", event.getUuid());
		value.put("name", event.getName());
		value.put("conceptUuid", event.getConcept().getUuid());
		if (event.getConcept().getName() != null) {
			value.put("conceptDisplay", event.getConcept().getName().getName());
		} else if (event.getConcept().getDisplayString() != null) {
			value.put("conceptDisplay", event.getConcept().getDisplayString());
		}
		value.put("periodicity", event.getPeriodicity());
		value.put("deadlineDays", event.getDeadlineDays());
		value.put("retired", event.isRetired());
		return value;
	}
}
