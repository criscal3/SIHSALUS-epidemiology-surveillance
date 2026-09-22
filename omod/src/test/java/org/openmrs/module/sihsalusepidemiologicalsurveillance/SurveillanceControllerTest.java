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
	public void eventRoutesDispatchAndDeleteReturnsNoContent() throws Exception {
		SurveillanceService service = mock(SurveillanceService.class);
		SurveillanceController controller = new SurveillanceController();
		controller.setService(service);
		org.springframework.test.web.servlet.MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
		        .standaloneSetup(controller).build();
		String base = "/rest/v1/sihsalusepidemiologicalsurveillance";
		mvc.perform(
		    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(base + "/events?includeRetired=true"))
		        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(base + "/events/event"))
		        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(base + "/events/event")
		        .contentType("application/json").content("{\"name\":\"Updated\"}"))
		        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(base + "/events/event"))
		        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(base + "/healthcheck"))
		        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
		verify(service).getEvents(true);
		verify(service).getEvent("event");
		verify(service).updateEvent(eq("event"), anyMap());
		verify(service).deleteEvent("event");
		verify(service).healthcheck();
	}
	
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
