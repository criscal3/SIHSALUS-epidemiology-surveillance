package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import java.time.*;
import java.util.*;

import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class ReportCalculator {
	
	public SurveillanceReport calculate(String eventUuid, LocalDate from, LocalDate to, String period,
	        List<CaseRecord> cases, List<PeriodCaseCount> history, ClinicalCatalog m) {
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		EndemicChannel statistics = new EndemicChannel();
		SurveillanceReport report = new SurveillanceReport();
		report.generatedAt = Instant.now().toString();
		report.eventUuid = eventUuid;
		report.from = from.toString();
		report.to = to.toString();
		report.period = period;
		Map<LocalDate, Integer> daily = new TreeMap<LocalDate, Integer>(), periods = new TreeMap<LocalDate, Integer>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1))
			daily.put(date, 0);
		for (LocalDate date = calendar.start(from, period); !date.isAfter(to); date = calendar.next(date, period))
			periods.put(date, 0);
		for (String dimension : Arrays.asList("age", "ageGroup", "sex", "disease", "ethnicity", "pregnancy", "period"))
			report.demographics.put(dimension, new TreeMap<String, Integer>());
		for (CaseRecord record : cases) {
			if (!eventUuid.equals(record.eventUuid) || !"CONFIRMED".equals(record.status) || record.onset == null
			        || record.onset.isBefore(from) || record.onset.isAfter(to))
				continue;
			report.total++;
			daily.put(record.onset, daily.get(record.onset) + 1);
			LocalDate bucket = calendar.start(record.onset, period);
			periods.put(bucket, periods.get(bucket) + 1);
			Integer age = record.birthDate == null || record.birthDate.isAfter(record.onset) ? null
			        : Period.between(record.birthDate, record.onset).getYears();
			increment(report, "age", age == null ? "UNKNOWN" : age.toString());
			increment(report, "ageGroup", ageGroup(age));
			increment(report, "sex", record.gender);
			increment(report, "disease", record.eventUuid);
			increment(report, "ethnicity", record.ethnicity);
			increment(report, "pregnancy", record.pregnant == null ? "UNKNOWN" : record.pregnant ? "YES" : "NO");
			increment(report, "period", bucket.toString());
		}
		for (Map.Entry<LocalDate, Integer> entry : daily.entrySet()) {
			SurveillanceReport.Point point = new SurveillanceReport.Point();
			point.date = entry.getKey().toString();
			point.cases = entry.getValue();
			report.curve.add(point);
		}
		for (Map.Entry<LocalDate, Integer> entry : periods.entrySet()) {
			SurveillanceReport.ChannelPoint point = new SurveillanceReport.ChannelPoint();
			point.date = entry.getKey().toString();
			point.cases = entry.getValue();
			point.year = calendar.year(entry.getKey(), period);
			point.number = calendar.number(entry.getKey(), period);
			EndemicChannel.Thresholds quartiles = statistics.evaluate(periodHistory(history, entry.getKey(), period, m),
			    point.cases, m.minimumHistoricalYears);
			point.q1 = quartiles.q1;
			point.q2 = quartiles.q2;
			point.q3 = quartiles.q3;
			point.sampleSize = quartiles.sampleSize;
			point.zone = quartiles.zone;
			// Filtered or unfinished buckets are not comparable with complete historical periods.
			if (entry.getKey().isBefore(from) || calendar.next(entry.getKey(), period).isAfter(to.plusDays(1)))
				point.zone = "PARTIAL_PERIOD";
			report.channel.add(point);
		}
		if (report.channel.stream().anyMatch(point -> "INSUFFICIENT_HISTORY".equals(point.zone)))
			report.warnings.add("INSUFFICIENT_HISTORY");
		if (!from.equals(calendar.start(from, period))
		        || !to.plusDays(1).equals(calendar.next(calendar.start(to, period), period)))
			report.warnings.add("PARTIAL_PERIOD");
		return report;
	}
	
	private List<Integer> periodHistory(List<PeriodCaseCount> counts, LocalDate date, String period, ClinicalCatalog m) {
		if (!"dia".equals(period)) {
			EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
			return history(counts, calendar.year(date, period), calendar.number(date, period), m);
		}
		Map<Integer, Integer> years = new TreeMap<Integer, Integer>();
		for (PeriodCaseCount count : counts) {
			if (count.getYear() < date.getYear() && count.getYear() >= date.getYear() - m.historicalYears
			        && java.time.MonthDay.from(date).isValidYear(count.getYear())
			        && count.getPeriodNumber() == date.withYear(count.getYear()).getDayOfYear()) {
				years.put(count.getYear(), count.getCaseCount());
			}
		}
		return new ArrayList<Integer>(years.values());
	}
	
	public static List<Integer> history(List<PeriodCaseCount> values, int year, int number, ClinicalCatalog m) {
		Map<Integer, Integer> samples = new TreeMap<Integer, Integer>();
		for (PeriodCaseCount count : values)
			if (count.getYear() < year && count.getYear() >= year - m.historicalYears && count.getPeriodNumber() == number)
				samples.put(count.getYear(), count.getCaseCount());
		return new ArrayList<Integer>(samples.values());
	}
	
	public static String ageGroup(Integer age) {
		return age == null ? "UNKNOWN"
		        : age < 5 ? "0-4" : age < 12 ? "5-11" : age < 18 ? "12-17" : age < 30 ? "18-29" : age < 60 ? "30-59" : "60+";
	}
	
	private void increment(SurveillanceReport r, String dimension, String key) {
		if (key == null || key.trim().isEmpty())
			key = "UNKNOWN";
		Map<String, Integer> values = r.demographics.get(dimension);
		values.put(key, values.getOrDefault(key, 0) + 1);
	}
	
	public List<PeriodCaseCount> aggregate(NotifiableEvent event, List<CaseRecord> records, ClinicalCatalog m,
	        LocalDate today) {
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		// Start of verified coverage, never before it; absence of records before coverage is unknown.
		LocalDate start = LocalDate.parse(m.surveillanceStartDate);
		List<PeriodCaseCount> result = new ArrayList<PeriodCaseCount>();
		for (String period : Arrays.asList("dia", "semana", "mes", "trimestre", "semestre")) {
			Map<String, PeriodCaseCount> buckets = new LinkedHashMap<String, PeriodCaseCount>();
			for (LocalDate date = calendar.start(start, period); !date.isAfter(today); date = calendar.next(date, period)) {
				// A partially covered first period must not become a historical zero.
				if (date.isBefore(start))
					continue;
				PeriodCaseCount count = new PeriodCaseCount();
				count.setEvent(event);
				count.setPeriodType(period);
				count.setYear(calendar.year(date, period));
				count.setPeriodNumber(calendar.number(date, period));
				count.setCaseCount(0);
				count.setDate("dia".equals(period) ? calendar.date(date) : null);
				buckets.put(count.getYear() + ":" + count.getPeriodNumber(), count);
			}
			for (CaseRecord record : records)
				if (event.getUuid().equals(record.eventUuid) && "CONFIRMED".equals(record.status) && record.onset != null
				        && !record.onset.isBefore(start) && !record.onset.isAfter(today)) {
					PeriodCaseCount count = buckets
					        .get(calendar.year(record.onset, period) + ":" + calendar.number(record.onset, period));
					if (count != null)
						count.setCaseCount(count.getCaseCount() + 1);
				}
			result.addAll(buckets.values());
		}
		return result;
	}
}
