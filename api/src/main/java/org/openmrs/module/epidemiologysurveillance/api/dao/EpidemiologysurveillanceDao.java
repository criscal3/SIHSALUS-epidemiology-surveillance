package org.openmrs.module.epidemiologysurveillance.api.dao;

import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.epidemiologysurveillance.Epidemiologysurveillance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("epidemiologysurveillance.EpidemiologysurveillanceDao")
public class EpidemiologysurveillanceDao {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	public Epidemiologysurveillance getEpidemiologysurveillanceByUuid(String uuid) {
		return (Epidemiologysurveillance) getSession().createCriteria(Epidemiologysurveillance.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public Epidemiologysurveillance saveEpidemiologysurveillance(Epidemiologysurveillance item) {
		getSession().saveOrUpdate(item);
		return item;
	}
}
