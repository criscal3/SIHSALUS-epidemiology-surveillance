package org.openmrs.module.sihsalusepidemiologicalsurveillance.api;

import org.openmrs.User;
import org.openmrs.api.context.Context;

/** Also called explicitly: authorization does not depend on controller annotation interception. */
public class SurveillanceAccess {
	
	public User require(String privilege) {
		if (!Context.isAuthenticated())
			throw new SurveillanceException(401, "AUTHENTICATION_REQUIRED");
		if (!Context.hasPrivilege(privilege))
			throw new SurveillanceException(403, "ACCESS_DENIED");
		return Context.getAuthenticatedUser();
	}
}
