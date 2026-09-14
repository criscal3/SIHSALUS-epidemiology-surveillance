package org.openmrs.module.sihsalusepidemiologicalsurveillance.web.controller;

import java.util.*;

import org.openmrs.api.context.Context;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/rest/v1/sihsalusepidemiologicalsurveillance")
public class SurveillanceController {
	
	private SurveillanceService service;
	
	public void setService(SurveillanceService value) {
		service = value;
	}
	
	private SurveillanceService service() {
		return service == null ? Context.getService(SurveillanceService.class) : service;
	}
	
	@RequestMapping(value = "/metadata", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> metadata() {
		return service().getMetadata();
	}
	
	@RequestMapping(value = "/cases", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public CaseResult register(@RequestBody CaseRequest body) {
		return service().registerCase(body);
	}
	
	@RequestMapping(value = "/cases/{uuid}", method = RequestMethod.GET)
	@ResponseBody
	public CaseResult get(@PathVariable("uuid") String uuid) {
		return service().getCase(uuid);
	}
	
	@RequestMapping(value = "/reports", method = RequestMethod.GET)
	@ResponseBody
	public SurveillanceReport report(@RequestParam("event") String event, @RequestParam("from") String from,
	        @RequestParam("to") String to, @RequestParam(value = "period", defaultValue = "semana") String period) {
		return service().report(event, from, to, period);
	}
	
	@RequestMapping(value = "/counts/refresh", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> refresh() {
		service().refreshCounts();
		return Collections.<String, Object> singletonMap("refreshed", true);
	}
	
	@RequestMapping(value = "/events", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> event(@RequestBody Map<String, Object> body) {
		return service().saveEvent(body);
	}
	
	@RequestMapping(value = "/rules", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> rule(@RequestBody Map<String, Object> body) {
		return service().saveRule(body);
	}
	
	@ExceptionHandler(SurveillanceException.class)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> error(SurveillanceException error) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", error.getCode());
		body.put("fields", error.getFields());
		return new ResponseEntity<Map<String, Object>>(body, HttpStatus.valueOf(error.getStatus()));
	}
	
	@ExceptionHandler({ org.springframework.http.converter.HttpMessageNotReadableException.class,
	        org.springframework.web.bind.MissingServletRequestParameterException.class })
	@ResponseBody
	public ResponseEntity<Map<String, Object>> malformed(Exception error) {
		return new ResponseEntity<Map<String, Object>>(Collections.<String, Object> singletonMap("code", "INVALID_REQUEST"),
		        HttpStatus.BAD_REQUEST);
	}
	
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> unexpected(Exception error) {
		return new ResponseEntity<Map<String, Object>>(
		        Collections.<String, Object> singletonMap("code", "SERVICE_UNAVAILABLE"), HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
