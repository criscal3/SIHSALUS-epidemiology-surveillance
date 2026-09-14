package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import java.time.LocalDate;
import java.util.*;

import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class OutbreakEngine {
	
	public void evaluate(CaseRecord current, FocoEpidemiologico focus, List<ReglaAlertaBrote> rules, List<CaseRecord> recent,
	        List<ConteoCasosPeriodo> history, Metadata m, CaseResult result) {
		if ("DISCARDED".equals(current.status))
			return;
		boolean eliminated = focus != null && "eliminado".equals(focus.getClasificacion());
		if ("SEVERE".equals(current.severity))
			result.immediateAlerts.add("SEVERE_CASE");
		if (current.deceased)
			result.immediateAlerts.add("DEATH");
		if (eliminated)
			result.immediateAlerts.add("ELIMINATED_FOCUS");
		if (focus != null && Boolean.FALSE.equals(focus.getReceptivo()))
			result.immediateAlerts.add("NON_RECEPTIVE_FOCUS");
		if (Boolean.TRUE.equals(current.pregnant) && "CONFIRMED".equals(current.status))
			result.immediateAlerts.add("CONFIRMED_PREGNANCY");
		if (focus == null)
			result.warnings.add("FOCUS_UNKNOWN");
		if (current.pregnant == null)
			result.warnings.add("PREGNANCY_UNKNOWN");
		if (!"CONFIRMED".equals(current.status))
			return;
		EpidemiologicalCalendar calendar = new EpidemiologicalCalendar(m);
		LocalDate week = calendar.start(current.onset, "semana");
		EndemicChannel channel = new EndemicChannel();
		for (ReglaAlertaBrote rule : rules) {
			if (!Boolean.TRUE.equals(rule.getActiva()))
				continue;
			if ("ELIMINATED_FOCUS".equals(rule.getTipoCondicion())) {
				if (eliminated && "AUTOCHTHONOUS".equals(current.origin))
					result.outbreakAlerts.add("AUTOCHTHONOUS_ELIMINATED");
			} else if ("EPIDEMIC".equals(rule.getTipoCondicion())) {
				List<Integer> sample = ReportCalculator.history(history, calendar.year(week, "semana"),
				    calendar.number(week, "semana"), m);
				int count = count(recent, current.eventUuid, week, week.plusWeeks(1));
				EndemicChannel.Thresholds thresholds = channel.evaluate(sample, count, m.minimumHistoricalYears);
				if (thresholds.q3 == null)
					warnHistory(result);
				else if (count > thresholds.q3 && count - 1 <= thresholds.q3)
					result.outbreakAlerts.add("EPIDEMIC_THRESHOLD");
			} else if ("SUSTAINED".equals(rule.getTipoCondicion())) {
				int window = rule.getVentanaSemanas() == null ? 2 : rule.getVentanaSemanas();
				if (window < 2 || window > 12)
					throw new SurveillanceException(503, "INVALID_OUTBREAK_RULE");
				boolean sustained = true;
				int previous = -1;
				for (int offset = window - 1; offset >= 0; offset--) {
					LocalDate date = week.minusWeeks(offset);
					int count = count(recent, current.eventUuid, date, date.plusWeeks(1));
					List<Integer> sample = ReportCalculator.history(history, calendar.year(date, "semana"),
					    calendar.number(date, "semana"), m);
					EndemicChannel.Thresholds thresholds = channel.evaluate(sample, count, m.minimumHistoricalYears);
					if (thresholds.q2 == null) {
						sustained = false;
						warnHistory(result);
					} else if (count < thresholds.q2 || count == 0 || (previous >= 0 && count <= previous))
						sustained = false;
					previous = count;
				}
				if (sustained)
					result.outbreakAlerts.add("SUSTAINED_INCREASE");
			} else
				throw new SurveillanceException(503, "INVALID_OUTBREAK_RULE");
		}
	}
	
	private int count(List<CaseRecord> records, String event, LocalDate start, LocalDate end) {
		Set<String> unique = new HashSet<String>();
		for (CaseRecord record : records)
			if (event.equals(record.eventUuid) && "CONFIRMED".equals(record.status) && record.onset != null
			        && !record.onset.isBefore(start) && record.onset.isBefore(end))
				unique.add(record.encounterUuid);
		return unique.size();
	}
	
	private void warnHistory(CaseResult result) {
		if (!result.warnings.contains("INSUFFICIENT_HISTORY"))
			result.warnings.add("INSUFFICIENT_HISTORY");
	}
}
