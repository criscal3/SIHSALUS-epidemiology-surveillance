package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class OutbreakEngineTest {
	
	final Metadata m = new Metadata();
	
	final OutbreakEngine engine = new OutbreakEngine();
	
	final LocalDate date = LocalDate.of(2026, 1, 18);
	
	CaseRecord current() {
		CaseRecord r = new CaseRecord();
		r.encounterUuid = "current";
		r.eventUuid = "event";
		r.status = "CONFIRMED";
		r.origin = "AUTOCHTHONOUS";
		r.onset = date;
		return r;
	}
	
	ReglaAlertaBrote rule(String type) {
		ReglaAlertaBrote r = new ReglaAlertaBrote();
		r.setActiva(true);
		r.setTipoCondicion(type);
		r.setVentanaSemanas(2);
		return r;
	}
	
	FocoEpidemiologico eliminated() {
		FocoEpidemiologico f = new FocoEpidemiologico();
		f.setClasificacion("eliminado");
		f.setReceptivo(false);
		return f;
	}
	
	List<CaseRecord> records(int previous, int now) {
		List<CaseRecord> records = new ArrayList<CaseRecord>();
		for (int i = 0; i < previous; i++) {
			CaseRecord r = current();
			r.onset = date.minusWeeks(1);
			r.encounterUuid = "previous" + i;
			records.add(r);
		}
		for (int i = 0; i < now; i++) {
			CaseRecord r = current();
			r.encounterUuid = "now" + i;
			records.add(r);
		}
		return records;
	}
	
	List<ConteoCasosPeriodo> history() {
		List<ConteoCasosPeriodo> values = new ArrayList<ConteoCasosPeriodo>();
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		for (int year = 2021; year <= 2025; year++)
			for (LocalDate week : Arrays.asList(date, date.minusWeeks(1))) {
				ConteoCasosPeriodo value = new ConteoCasosPeriodo();
				value.setAnio(year);
				value.setNumeroPeriodo(calendar.number(week, "semana"));
				value.setNumeroCasos(1);
				values.add(value);
			}
		return values;
	}
	
	@Test
	public void immediateAndOutbreakAlertsAreIndependent() {
		CaseRecord r = current();
		r.severity = "SEVERE";
		r.deceased = true;
		r.pregnant = true;
		CaseResult result = new CaseResult();
		engine.evaluate(r, eliminated(), Arrays.asList(rule("ELIMINATED_FOCUS")), records(0, 1), history(), m, result);
		assertEquals(5, result.immediateAlerts.size());
		assertEquals(Arrays.asList("AUTOCHTHONOUS_ELIMINATED"), result.outbreakAlerts);
	}
	
	@Test
	public void discardedCasesCannotTriggerAnyAlert() {
		CaseRecord r = current();
		r.status = "DISCARDED";
		r.severity = "SEVERE";
		CaseResult result = new CaseResult();
		engine.evaluate(r, eliminated(), Arrays.asList(rule("ELIMINATED_FOCUS")), records(0, 1), history(), m, result);
		assertTrue(result.immediateAlerts.isEmpty());
		assertTrue(result.outbreakAlerts.isEmpty());
	}
	
	@Test
	public void suspectedPregnancyDoesNotTriggerConfirmedPregnancyAlert() {
		CaseRecord r = current();
		r.status = "SUSPECTED";
		r.pregnant = true;
		CaseResult result = new CaseResult();
		engine.evaluate(r, null, Collections.<ReglaAlertaBrote> emptyList(), records(0, 1), history(), m, result);
		assertFalse(result.immediateAlerts.contains("CONFIRMED_PREGNANCY"));
	}
	
	@Test
	public void importedCaseDoesNotTriggerAutochthonousOutbreak() {
		CaseRecord r = current();
		r.origin = "IMPORTED_NATIONAL";
		CaseResult result = new CaseResult();
		engine.evaluate(r, eliminated(), Arrays.asList(rule("ELIMINATED_FOCUS")), records(0, 1), history(), m, result);
		assertTrue(result.outbreakAlerts.isEmpty());
		assertFalse(result.immediateAlerts.isEmpty());
	}
	
	@Test
	public void crossingQ3TriggersEpidemic() {
		CaseResult result = new CaseResult();
		engine.evaluate(current(), null, Arrays.asList(rule("EPIDEMIC")), records(0, 2), history(), m, result);
		assertTrue(result.outbreakAlerts.contains("EPIDEMIC_THRESHOLD"));
	}
	
	@Test
	public void equalityWithQ3IsNotAnEpidemic() {
		CaseResult result = new CaseResult();
		engine.evaluate(current(), null, Arrays.asList(rule("EPIDEMIC")), records(0, 1), history(), m, result);
		assertTrue(result.outbreakAlerts.isEmpty());
	}
	
	@Test
	public void twoIncreasingWeeksAtQ2TriggerSustainedAlert() {
		CaseResult result = new CaseResult();
		engine.evaluate(current(), null, Arrays.asList(rule("SUSTAINED")), records(2, 3), history(), m, result);
		assertTrue(result.outbreakAlerts.contains("SUSTAINED_INCREASE"));
	}
	
	@Test
	public void flatOrDecliningWeeksDoNotTriggerSustainedAlert() {
		for (int now : Arrays.asList(2, 1)) {
			CaseResult result = new CaseResult();
			engine.evaluate(current(), null, Arrays.asList(rule("SUSTAINED")), records(2, now), history(), m, result);
			assertTrue(result.outbreakAlerts.isEmpty());
		}
	}
	
	@Test
	public void missingHistoryDoesNotDisableImmediateAlert() {
		CaseRecord r = current();
		r.severity = "SEVERE";
		CaseResult result = new CaseResult();
		engine.evaluate(r, null, Arrays.asList(rule("EPIDEMIC")), records(0, 10),
		    Collections.<ConteoCasosPeriodo> emptyList(), m, result);
		assertTrue(result.outbreakAlerts.isEmpty());
		assertTrue(result.immediateAlerts.contains("SEVERE_CASE"));
		assertTrue(result.warnings.contains("INSUFFICIENT_HISTORY"));
	}
	
	@Test
	public void inactiveRuleDoesNotTrigger() {
		ReglaAlertaBrote rule = rule("ELIMINATED_FOCUS");
		rule.setActiva(false);
		CaseResult result = new CaseResult();
		engine.evaluate(current(), eliminated(), Arrays.asList(rule), records(0, 1), history(), m, result);
		assertTrue(result.outbreakAlerts.isEmpty());
	}
}
