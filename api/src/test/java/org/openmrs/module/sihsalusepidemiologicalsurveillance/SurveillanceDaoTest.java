package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.*;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.HibernateSurveillanceDao;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.model.*;

public class SurveillanceDaoTest {
	
	@Test
	public void assignsUuidBeforePersisting() {
		DbSessionFactory factory = mock(DbSessionFactory.class);
		DbSession session = mock(DbSession.class);
		when(factory.getCurrentSession()).thenReturn(session);
		HibernateSurveillanceDao dao = new HibernateSurveillanceDao();
		dao.setSessionFactory(factory);
		NotifiableEvent event = new NotifiableEvent();
		dao.save(event);
		assertNotNull(event.getUuid());
		verify(session).saveOrUpdate(event);
		String uuid = event.getUuid();
		dao.save(event);
		assertEquals(uuid, event.getUuid());
	}
	
	@Test
	public void serializesSamePatientWritesWithBoundParameter() {
		DbSessionFactory factory = mock(DbSessionFactory.class);
		DbSession session = mock(DbSession.class);
		SQLQuery query = mock(SQLQuery.class, RETURNS_SELF);
		when(factory.getCurrentSession()).thenReturn(session);
		when(session.createSQLQuery(anyString())).thenReturn(query);
		HibernateSurveillanceDao dao = new HibernateSurveillanceDao();
		dao.setSessionFactory(factory);
		Patient patient = new Patient(17);
		dao.lockPatient(patient);
		verify(query).setInteger("id", 17);
		verify(query).uniqueResult();
		verify(session).createSQLQuery("select patient_id from patient where patient_id = :id for update");
	}
	
	@Test
	public void duplicateQueryExcludesVoidedAndUsesOnsetWindow() {
		DbSessionFactory factory = mock(DbSessionFactory.class);
		DbSession session = mock(DbSession.class);
		Query query = mock(Query.class, RETURNS_SELF);
		when(factory.getCurrentSession()).thenReturn(session);
		when(session.createQuery(anyString())).thenReturn(query);
		when(query.list()).thenReturn(Collections.emptyList());
		HibernateSurveillanceDao dao = new HibernateSurveillanceDao();
		dao.setSessionFactory(factory);
		Date from = new Date(0), to = new Date(1000);
		dao.possibleDuplicates(new Patient(2), Arrays.asList("diagnosis"), from, to, "onset");
		verify(query).setTimestamp("from", from);
		verify(query).setTimestamp("to", to);
		verify(query).setParameterList("diagnoses", Arrays.asList("diagnosis"));
		verify(session).createQuery(contains("d.voided = false"));
	}
}
