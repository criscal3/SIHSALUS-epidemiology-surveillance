/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.epidemiologysurveillance.api.impl;

import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.epidemiologysurveillance.Epidemiologysurveillance;
import org.openmrs.module.epidemiologysurveillance.api.EpidemiologysurveillanceService;
import org.openmrs.module.epidemiologysurveillance.api.dao.EpidemiologysurveillanceDao;
import org.springframework.transaction.annotation.Transactional;

public class EpidemiologysurveillanceServiceImpl extends BaseOpenmrsService implements EpidemiologysurveillanceService {
	
	EpidemiologysurveillanceDao dao;
	
	UserService userService;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(EpidemiologysurveillanceDao dao) {
		this.dao = dao;
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	
	@Override
	@Transactional(readOnly = true)
	public Epidemiologysurveillance getEpidemiologysurveillanceByUuid(String uuid) throws APIException {
		return dao.getEpidemiologysurveillanceByUuid(uuid);
	}
	
	@Override
	@Transactional
	public Epidemiologysurveillance saveEpidemiologysurveillance(Epidemiologysurveillance item) throws APIException {
		if (item.getOwner() == null) {
			item.setOwner(userService.getUser(1));
		}
		
		return dao.saveEpidemiologysurveillance(item);
	}
}
