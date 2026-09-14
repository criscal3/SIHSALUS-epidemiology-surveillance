package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model;

import java.util.*;

import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.EndemicChannel;

public class SurveillanceReport {
	
	public String generatedAt;
	
	public String eventUuid;
	
	public String from;
	
	public String to;
	
	public String period;
	
	public String population = "CONFIRMED";
	
	public int total;
	
	public List<Point> curve = new ArrayList<Point>();
	
	public List<ChannelPoint> channel = new ArrayList<ChannelPoint>();
	
	public Map<String, Map<String, Integer>> demographics = new LinkedHashMap<String, Map<String, Integer>>();
	
	public List<String> warnings = new ArrayList<String>();
	
	public static class Point {
		
		public String date;
		
		public int cases;
	}
	
	public static class ChannelPoint extends EndemicChannel.Thresholds {
		
		public String date;
		
		public int year;
		
		public int number;
		
		public int cases;
	}
}
