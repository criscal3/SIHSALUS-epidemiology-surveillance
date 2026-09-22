package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.Date;

import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.ClinicalCatalog;

public class EpidemiologicalCalendar {
	
	private final ZoneId zone;
	
	private final WeekFields weeks;
	
	public EpidemiologicalCalendar(ClinicalCatalog catalog) {
		zone = ZoneId.of(catalog.timezone);
		weeks = WeekFields.of(DayOfWeek.valueOf(catalog.firstDayOfWeek), catalog.minimalDaysInFirstWeek);
	}
	
	public Date date(LocalDate date) {
		return Date.from(date.atStartOfDay(zone).toInstant());
	}
	
	public LocalDate local(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
	}
	
	public LocalDate today() {
		return LocalDate.now(zone);
	}
	
	public int year(LocalDate date, String period) {
		return "semana".equals(period) ? date.get(weeks.weekBasedYear()) : date.getYear();
	}
	
	public int number(LocalDate date, String period) {
		switch (period) {
			case "dia":
				return date.getDayOfYear();
			case "semana":
				return date.get(weeks.weekOfWeekBasedYear());
			case "mes":
				return date.getMonthValue();
			case "trimestre":
				return (date.getMonthValue() - 1) / 3 + 1;
			case "semestre":
				return (date.getMonthValue() - 1) / 6 + 1;
			default:
				throw new SurveillanceException(422, "INVALID_PERIOD");
		}
	}
	
	public LocalDate start(LocalDate date, String period) {
		switch (period) {
			case "dia":
				return date;
			case "semana":
				return date.with(weeks.dayOfWeek(), 1);
			case "mes":
				return date.withDayOfMonth(1);
			case "trimestre":
				return LocalDate.of(date.getYear(), (number(date, period) - 1) * 3 + 1, 1);
			case "semestre":
				return LocalDate.of(date.getYear(), (number(date, period) - 1) * 6 + 1, 1);
			default:
				throw new SurveillanceException(422, "INVALID_PERIOD");
		}
	}
	
	public LocalDate next(LocalDate date, String period) {
		switch (period) {
			case "dia":
				return date.plusDays(1);
			case "semana":
				return date.plusWeeks(1);
			case "mes":
				return date.plusMonths(1);
			case "trimestre":
				return date.plusMonths(3);
			case "semestre":
				return date.plusMonths(6);
			default:
				throw new SurveillanceException(422, "INVALID_PERIOD");
		}
	}
}
