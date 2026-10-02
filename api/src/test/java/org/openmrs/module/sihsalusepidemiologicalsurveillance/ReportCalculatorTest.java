package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.Test;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.Diagnosis;
import org.openmrs.User;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class ReportCalculatorTest {
	
	final ClinicalCatalog m = new ClinicalCatalog();
	
	final ReportCalculator calculator = new ReportCalculator();
	
	@Test
	public void sqlOnsetSurvivesAggregationAndReloadWithoutLosingTwoDays() {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			NotifiableEvent event = new NotifiableEvent();
			event.setUuid("synthetic-event");
			Concept concept = new Concept(1);
			event.setConcept(concept);
			SurveillanceCase value = surveillanceCase(concept, "CONFIRMADO", 100);
			value.setOnsetDate(java.sql.Date.valueOf("2026-09-11"));
			List<PeriodCaseCount> counts = calculator.aggregateSurveillanceCases(event, Collections.singletonList(value),
			    Collections.singletonMap(100, 10), m, new User(1), LocalDate.of(2026, 9, 12));
			for (PeriodCaseCount count : counts) {
				assertTrue(count.getStartDate() instanceof java.sql.Date);
				count.setStartDate(java.sql.Date.valueOf(count.getStartDate().toString()));
			}
			LocalDate from = LocalDate.of(2026, 9, 9), to = LocalDate.of(2026, 9, 11);
			SurveillanceReport report = new AggregateReportCalculator().calculate(event.getUuid(), from, to, "dia",
			    "CENTRO_POBLADO", 100, "CONFIRMADO", counts, Collections.<PeriodCaseCount> emptyList(), m);
			assertEquals(1, report.total);
			assertEquals(0, report.curve.get(0).cases);
			assertEquals(0, report.curve.get(1).cases);
			assertEquals("2026-09-11", report.curve.get(2).date);
			assertEquals(1, report.curve.get(2).cases);
			assertEquals(1, report.channel.get(2).cases);
			assertEquals("2026-09-11", report.channel.get(2).date);
		}
		finally {
			TimeZone.setDefault(original);
		}
	}
	
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
	
	@Test
	public void surveillanceAggregationCountsEachEligibleCaseForItsCenterAndDistrict() {
		NotifiableEvent event = new NotifiableEvent();
		Concept eventConcept = new Concept(1);
		Concept diagnosisConcept = new Concept(2);
		eventConcept.addSetMember(diagnosisConcept);
		event.setConcept(eventConcept);
		SurveillanceCase eligible = surveillanceCase(diagnosisConcept, "CONFIRMADO", 100);
		SurveillanceCase discarded = surveillanceCase(diagnosisConcept, "DESCARTADO", 100);
		Map<Integer, Integer> districts = Collections.singletonMap(100, 10);
		
		List<PeriodCaseCount> counts = calculator.aggregateSurveillanceCases(event, Arrays.asList(eligible, discarded),
		    districts, m, new User(1), LocalDate.of(2026, 1, 20));
		
		assertEquals(10, counts.size());
		assertTrue(counts.stream()
		        .anyMatch(c -> c.getAddressHierarchyEntryId() == 100 && "CENTRO_POBLADO".equals(c.getZoneLevel())
		                && "SEMANA".equals(c.getPeriodType()) && "CONFIRMADO".equals(c.getDiagnosisType())
		                && c.getCaseCount() == 1));
		assertTrue(counts.stream().anyMatch(c -> c.getAddressHierarchyEntryId() == 10 && "DISTRITO".equals(c.getZoneLevel())
		        && "SEMANA".equals(c.getPeriodType()) && c.getCaseCount() == 1));
	}
	
	private SurveillanceCase surveillanceCase(Concept diagnosisConcept, String type, int address) {
		Diagnosis diagnosis = new Diagnosis();
		diagnosis.setDiagnosis(new CodedOrFreeText(diagnosisConcept, null, null));
		SurveillanceCase result = new SurveillanceCase();
		result.setDiagnosis(diagnosis);
		result.setDiagnosisType(type);
		result.setInfectionAddress(address);
		result.setOnsetDate(new EpidemiologicalCalendar(m).date(LocalDate.of(2026, 1, 19)));
		return result;
	}
}
