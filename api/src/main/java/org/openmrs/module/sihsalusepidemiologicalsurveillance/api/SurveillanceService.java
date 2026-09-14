package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import java.util.Map;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.SurveillanceConstants;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface SurveillanceService extends OpenmrsService {
	
	@Authorized(SurveillanceConstants.ACCESS)
	@Transactional(readOnly = true)
	Map<String, Object> getMetadata();
	
	@Authorized(SurveillanceConstants.REGISTER)
	@Transactional(isolation = Isolation.READ_COMMITTED)
	CaseResult registerCase(CaseRequest request);
	
	@Authorized(SurveillanceConstants.VIEW)
	CaseResult getCase(String uuid);
	
	@Authorized(SurveillanceConstants.REPORT)
	SurveillanceReport report(String eventUuid, String from, String to, String period);
	
	@Authorized(SurveillanceConstants.MANAGE)
	void refreshCounts();
	
	@Authorized(SurveillanceConstants.MANAGE)
	Map<String, Object> saveEvent(Map<String, Object> body);
	
	@Authorized(SurveillanceConstants.MANAGE)
	Map<String, Object> saveRule(Map<String, Object> body);
}
