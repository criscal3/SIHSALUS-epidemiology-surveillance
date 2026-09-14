package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.model.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.web.controller.SurveillanceController;

public class SurveillanceControllerTest {
	
	@Test
	public void delegatesWritesToProtectedService() {
		SurveillanceService service = mock(SurveillanceService.class);
		SurveillanceController controller = new SurveillanceController();
		controller.setService(service);
		CaseRequest request = new CaseRequest();
		controller.register(request);
		verify(service).registerCase(request);
	}
	
	@Test
	public void preservesValidationAndPermissionStatuses() {
		SurveillanceController controller = new SurveillanceController();
		for (int status : Arrays.asList(401, 403, 409, 422, 503)) {
			assertEquals(status, controller.error(new SurveillanceException(status, "SAFE_CODE")).getStatusCodeValue());
		}
	}
	
	@Test
	public void neverSerializesExceptionMessagesOrStackTraces() {
		SurveillanceController controller = new SurveillanceController();
		Map<String, Object> body = controller.unexpected(new RuntimeException("sensitive SQL")).getBody();
		assertEquals(Collections.<String, Object> singletonMap("code", "SERVICE_UNAVAILABLE"), body);
	}
}
