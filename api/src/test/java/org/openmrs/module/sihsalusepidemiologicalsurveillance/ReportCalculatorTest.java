package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class ReportCalculatorTest {
	
	final ClinicalCatalog m = new ClinicalCatalog();
	
	final ReportCalculator calculator = new ReportCalculator();
	
	CaseRecord record(String status) {
		CaseRecord r = new CaseRecord();
		r.eventUuid = "event";
		r.status = status;
		r.onset = LocalDate.of(2026, 1, 2);
		r.birthDate = LocalDate.of(2020, 1, 3);
		r.gender = "F";
		r.pregnant = null;
		return r;
	}
	
	@Test
	public void curveUsesOnsetIncludesZeroDaysAndExcludesDiscarded() {
		SurveillanceReport report = calculator.calculate("event", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), "dia",
		    Arrays.asList(record("CONFIRMED"), record("DISCARDED"), record("SUSPECTED")),
		    Collections.<PeriodCaseCount> emptyList(), m);
		assertEquals(1, report.total);
		assertEquals(3, report.curve.size());
		assertEquals(0, report.curve.get(0).cases);
		assertEquals(1, report.curve.get(1).cases);
	}
	
	@Test
	public void demographicAgeIsAtOnsetAndMissingPregnancyIsExplicit() {
		SurveillanceReport report = calculator.calculate("event", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), "dia",
		    Arrays.asList(record("CONFIRMED")), Collections.<PeriodCaseCount> emptyList(), m);
		assertEquals(Integer.valueOf(1), report.demographics.get("age").get("5"));
		assertEquals(Integer.valueOf(1), report.demographics.get("pregnancy").get("UNKNOWN"));
		assertEquals(Integer.valueOf(1), report.demographics.get("ethnicity").get("UNKNOWN"));
	}
	
	@Test
	public void aggregatesAllPeriodsAndExplicitZeroCoverage() {
		m.surveillanceStartDate = "2026-01-01";
		NotifiableEvent event = new NotifiableEvent();
		event.setUuid("event");
		List<PeriodCaseCount> counts = calculator.aggregate(event, Arrays.asList(record("CONFIRMED")), m,
		    LocalDate.of(2026, 1, 3));
		assertEquals(6, counts.size());
		assertTrue(counts.stream()
		        .anyMatch(c -> "dia".equals(c.getPeriodType()) && c.getPeriodNumber() == 1 && c.getCaseCount() == 0));
		assertFalse(counts.stream().anyMatch(c -> "semana".equals(c.getPeriodType()))); // first week is incompletely covered
	}
	
	@Test
	public void dailyHistoryMatchesCalendarDateAcrossLeapYears() {
		List<PeriodCaseCount> history = new ArrayList<PeriodCaseCount>();
		for (int year = 2021; year <= 2025; year++) {
			PeriodCaseCount count = new PeriodCaseCount();
			count.setYear(year);
			count.setPeriodNumber(LocalDate.of(year, 3, 1).getDayOfYear());
			count.setCaseCount(4);
			history.add(count);
		}
		SurveillanceReport report = calculator.calculate("event", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), "dia",
		    Collections.<CaseRecord> emptyList(), history, m);
		assertEquals(5, report.channel.get(0).sampleSize);
		assertEquals(4d, report.channel.get(0).q3, 0d);
	}
	
	@Test
	public void doesNotAssignDefinitiveZoneToPartialWeeks() {
		SurveillanceReport report = calculator.calculate("event", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3),
		    "semana", Arrays.asList(record("CONFIRMED")), Collections.<PeriodCaseCount> emptyList(), m);
		assertEquals("PARTIAL_PERIOD", report.channel.get(0).zone);
		assertTrue(report.warnings.contains("PARTIAL_PERIOD"));
	}
	
	@Test
	public void reportContainsNoPatientIdentifiers() {
		SurveillanceReport report = calculator.calculate("event", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), "dia",
		    Arrays.asList(record("CONFIRMED")), Collections.<PeriodCaseCount> emptyList(), m);
		assertFalse(report.demographics.containsKey("patient"));
		assertFalse(report.demographics.containsKey("encounterUuid"));
	}
}
