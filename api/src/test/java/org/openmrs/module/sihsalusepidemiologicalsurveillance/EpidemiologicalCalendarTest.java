package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.EpidemiologicalCalendar;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog;

public class EpidemiologicalCalendarTest {
	
	@Test
	public void newYearUsesWeekYearNotCalendarYear() {
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(new ClinicalCatalog());
		LocalDate date = LocalDate.of(2021, 1, 1);
		assertEquals(2020, calendar.year(date, "semana"));
		assertEquals(53, calendar.number(date, "semana"));
		assertEquals(LocalDate.of(2020, 12, 27), calendar.start(date, "semana"));
	}
	
	@Test
	public void handlesLeapDaysAndTimezoneRoundtrip() {
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(new ClinicalCatalog());
		LocalDate date = LocalDate.of(2024, 2, 29);
		assertEquals(date, calendar.local(calendar.date(date)));
		assertEquals(60, calendar.number(date, "dia"));
		assertEquals(1, calendar.number(date, "trimestre"));
		assertEquals(1, calendar.number(date, "semestre"));
	}
}
