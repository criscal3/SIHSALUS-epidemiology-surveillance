package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.sql.*;
import java.util.UUID;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;

/** Synthetic native-key stubs exercise migration integrity; this is not a MySQL acceptance test. */
public class LiquibaseMigrationTest {
	
	@Test
	public void migrationsAreRepeatableAndEnforceForeignKeysAndUniquePeriods() throws Exception {
		try (Connection connection = DriverManager
		        .getConnection("jdbc:h2:mem:surveillance-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "")) {
			try (Statement sql = connection.createStatement()) {
				sql.execute("create table concept(concept_id int primary key, uuid varchar(38) unique)");
				sql.execute("create table users(user_id int primary key)");
				sql.execute("create table patient(patient_id int primary key)");
				sql.execute("create table location(location_id int primary key)");
				sql.execute("create table encounter(encounter_id int primary key)");
				sql.execute("create table provider(provider_id int primary key)");
				sql.execute("create table diagnosis(diagnosis_id int primary key)");
				sql.execute("create table orders(order_id int primary key)");
				sql.execute("create table address_hierarchy_entry(address_hierarchy_entry_id int primary key)");
				sql.execute(
				    "create table scheduler_task_config(task_config_id int auto_increment primary key, name varchar(255), description varchar(1024), schedulable_class varchar(1024), repeat_interval bigint, start_on_startup boolean, started boolean, uuid varchar(38) unique, created_by int)");
				sql.execute("insert into concept values (1, 'synthetic-concept')");
				sql.execute("insert into concept values (3, '41cb3fbf-f50c-4d7d-87bc-1b5dd963f8d0')");
				sql.execute("insert into concept values (4, 'fae143be-b2c2-4d55-86de-09cf708911d6')");
				sql.execute("insert into users values (1)");
				sql.execute("insert into patient values (1)");
				sql.execute("insert into location values (1)");
				sql.execute("insert into encounter values (1)");
				sql.execute("insert into provider values (1)");
				sql.execute("insert into diagnosis values (1)");
				sql.execute("insert into orders values (1)");
				sql.execute("insert into address_hierarchy_entry values (1)");
			}
			Database database = DatabaseFactory.getInstance()
			        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
			Liquibase liquibase = new Liquibase("liquibase.xml", new ClassLoaderResourceAccessor(), database);
			liquibase.update(8, "");
			try (Statement sql = connection.createStatement()) {
				sql.execute("update evento_notificable set nombre='Previously deployed event' where concept_id=3");
				connection.commit();
			}
			liquibase.update("");
			liquibase.update("");
			try (Statement sql = connection.createStatement()) {
				ResultSet preserved = sql
				        .executeQuery("select name, retired from surveillance_notifiable_event where concept_id=3");
				assertTrue(preserved.next());
				assertEquals("Previously deployed event", preserved.getString(1));
				assertFalse(preserved.getBoolean(2));
				preserved.close();
				ResultSet tasks = sql
				        .executeQuery("select count(*) from scheduler_task_config where start_on_startup=false");
				tasks.next();
				assertEquals(1, tasks.getInt(1));
				tasks.close();
				ResultSet tables = connection.getMetaData().getTables(null, null, "SURVEILLANCE_NOTIFIABLE_EVENT", null);
				assertTrue(tables.next());
				ResultSet seeded = sql
				        .executeQuery("select count(*) from surveillance_notifiable_event where concept_id in (3,4)");
				seeded.next();
				assertEquals(2, seeded.getInt(1));
				seeded.close();
				sql.execute(
				    "insert into surveillance_notifiable_event(event_id,uuid,concept_id,name,periodicity,deadline_days) values(10,'event',1,'Synthetic','semanal',7)");
				fails(sql,
				    "insert into surveillance_outbreak_alert_rule(uuid,event_id,condition_type,active) values('orphan',99,'EPIDEMIC',true)");
				sql.execute(
				    "insert into surveillance_period_case_count(uuid,event_id,period_type,calendar_year,period_number,case_count) values('count',10,'semana',2026,1,2)");
				fails(sql,
				    "insert into surveillance_period_case_count(uuid,event_id,period_type,calendar_year,period_number,case_count) values('duplicate',10,'semana',2026,1,3)");
				fails(sql, "delete from surveillance_notifiable_event where event_id=10");
				sql.execute(
				    "insert into surveillance_case(surveillance_case_id,uuid,patient_id,encounter_id,provider_id,location_id,diagnosis_id,diagnosis_type,creator,date_created,voided) values(1,'case',1,1,1,1,1,'PROBABLE',1,CURRENT_TIMESTAMP,false)");
				fails(sql,
				    "insert into surveillance_case(surveillance_case_id,uuid,patient_id,encounter_id,provider_id,location_id,diagnosis_id,diagnosis_type,creator,date_created,voided) values(2,'same-diagnosis',1,1,1,1,1,'PROBABLE',1,CURRENT_TIMESTAMP,false)");
			}
		}
	}
	
	private void fails(Statement sql, String statement) throws Exception {
		try {
			sql.execute(statement);
			fail("Constraint must reject invalid data");
		}
		catch (SQLException expected) {
			assertNotNull(expected.getSQLState());
		}
	}
}
