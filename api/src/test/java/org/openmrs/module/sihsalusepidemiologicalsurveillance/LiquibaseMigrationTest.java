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
				sql.execute("create table notification_alert(alert_id int primary key)");
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
				sql.execute("insert into notification_alert values (1)");
				sql.execute("insert into address_hierarchy_entry values (1)");
			}
			Database database = DatabaseFactory.getInstance()
			        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
			Liquibase liquibase = new Liquibase("liquibase.xml", new ClassLoaderResourceAccessor(), database);
			liquibase.update(8, "");
			liquibase.update("");
			liquibase.update("");
			try (Statement sql = connection.createStatement()) {
				for (String table : new String[] { "NOTIFIABLE_EVENT", "OUTBREAK_ALERT_RULE", "OUTBREAK_ALERT",
				        "PERIOD_CASE_COUNT", "EPIDEMIOLOGICAL_FOCUS", "SURVEILLANCE_CASE", "INDIVIDUAL_RECORD" }) {
					assertTrue("Expected current table " + table,
					    connection.getMetaData().getTables(null, null, table, null).next());
				}
				for (String retired : new String[] { "SURVEILLANCE_NOTIFIABLE_EVENT", "SURVEILLANCE_OUTBREAK_ALERT_RULE",
				        "SURVEILLANCE_PERIOD_CASE_COUNT", "SURVEILLANCE_EPIDEMIOLOGICAL_FOCUS", "SURVEILLANCE_AUDIT",
				        "SURVEILLANCE_NOTI_NOTIFICATION" }) {
					assertFalse("Historical table must be retired: " + retired,
					    connection.getMetaData().getTables(null, null, retired, null).next());
				}
				ResultSet tasks = sql
				        .executeQuery("select count(*) from scheduler_task_config where start_on_startup=false");
				tasks.next();
				assertEquals(1, tasks.getInt(1));
				tasks.close();
				sql.execute(
				    "insert into notifiable_event(notifiable_event_id,uuid,concept_id,periodicity,reference_regulation,valid_from,creator,date_created) values(10,'event',1,'SEMANAL','test',CURRENT_DATE,1,CURRENT_TIMESTAMP)");
				fails(sql,
				    "insert into outbreak_alert_rule(uuid,notifiable_event_id,condition_type,version,valid_from,active,creator,date_created) values('orphan',99,'UMBRAL_CANAL',1,CURRENT_DATE,true,1,CURRENT_TIMESTAMP)");
				sql.execute(
				    "insert into period_case_count(uuid,notifiable_event_id,address_hierarchy_entry_id,zone_level,period_type,year,period_number,start_date,end_date,diagnosis_type,case_count,calculation_date,creator,date_created) values('count',10,1,'DISTRITO','SEMANA',2026,1,CURRENT_DATE,CURRENT_DATE,'CONFIRMADO',2,CURRENT_TIMESTAMP,1,CURRENT_TIMESTAMP)");
				fails(sql,
				    "insert into period_case_count(uuid,notifiable_event_id,address_hierarchy_entry_id,zone_level,period_type,year,period_number,start_date,end_date,diagnosis_type,case_count,calculation_date,creator,date_created) values('duplicate',10,1,'DISTRITO','SEMANA',2026,1,CURRENT_DATE,CURRENT_DATE,'CONFIRMADO',3,CURRENT_TIMESTAMP,1,CURRENT_TIMESTAMP)");
				fails(sql, "delete from notifiable_event where notifiable_event_id=10");
				fails(sql,
				    "insert into outbreak_alert(uuid,rule_id,address_hierarchy_entry_id,year,epidemiological_week,generation_date,alert_id,creator,date_created) values('orphan-alert',1,1,2026,1,CURRENT_TIMESTAMP,99,1,CURRENT_TIMESTAMP)");
				fails(sql,
				    "insert into individual_record(uuid,year,epidemiological_week,format_version,content_hash,download_date,creator,date_created) values('orphan-creator',2026,1,'v1','0123456789012345678901234567890123456789012345678901234567890123',CURRENT_TIMESTAMP,99,CURRENT_TIMESTAMP)");
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
