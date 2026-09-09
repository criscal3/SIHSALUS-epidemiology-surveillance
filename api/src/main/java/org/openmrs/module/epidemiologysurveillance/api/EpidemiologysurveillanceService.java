package org.openmrs.module.epidemiologysurveillance.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.epidemiologysurveillance.EpidemiologysurveillanceConfig;
import org.openmrs.module.epidemiologysurveillance.Epidemiologysurveillance;

public interface EpidemiologysurveillanceService extends OpenmrsService {
	
	/**
	 * Returns an item by uuid. It can be called by any authenticated user. It is fetched in read
	 * only transaction.
	 * 
	 * @param uuid
	 * @return
	 * @throws APIException
	 */
	@Authorized()
	Epidemiologysurveillance getEpidemiologysurveillanceByUuid(String uuid) throws APIException;
	
	/**
	 * Saves an item. Sets the owner to superuser, if it is not set. It can be called by users with
	 * this module's privilege. It is executed in a transaction.
	 * 
	 * @param item
	 * @return
	 * @throws APIException
	 */
	@Authorized(EpidemiologysurveillanceConfig.MODULE_PRIVILEGE)
	Epidemiologysurveillance saveEpidemiologysurveillance(Epidemiologysurveillance item) throws APIException;
}
