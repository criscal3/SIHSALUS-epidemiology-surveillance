package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.*;
import java.util.*;

import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db.RecoverEmptySurveillanceSchema;

public class RecoverEmptySurveillanceSchemaTest {
	
	private final String[] tables = { "notifiable_event", "outbreak_alert_rule", "outbreak_alert", "period_case_count",
	        "epidemiological_focus", "surveillance_case", "individual_record" };
	
	private Connection connection() throws SQLException {
		return DriverManager.getConnection("jdbc:h2:mem:recovery-" + UUID.randomUUID(), "sa", "");
	}
	
	private Database database(Connection connection, boolean retired, boolean completed) throws Exception {
		Database database = spy(
		    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
		List<RanChangeSet> history = new ArrayList<>();
		if (retired)
			history.add(ran("13-retire-historical-surveillance-tables"));
		if (completed)
			history.add(ran("14-create-current-surveillance-schema"));
		doReturn(history).when(database).getRanChangeSetList();
		return database;
	}
	
	private RanChangeSet ran(String id) {
		RanChangeSet result = mock(RanChangeSet.class);
		when(result.getId()).thenReturn(id);
		when(result.getAuthor()).thenReturn("sihsalus");
		when(result.getChangeLog()).thenReturn("liquibase-surveillance-case.xml");
		return result;
	}
	
	private void schema(Connection connection) throws SQLException {
		try (Statement sql = connection.createStatement()) {
			for (String table : tables)
				sql.execute("create table " + table + " (id int primary key)");
			sql.execute("create table encounter_diagnosis (diagnosis_id int primary key)");
			sql.execute("insert into encounter_diagnosis values (1)");
			sql.execute("alter table surveillance_case add event_id int references notifiable_event(id)");
			sql.execute("alter table outbreak_alert add rule_id int references outbreak_alert_rule(id)");
			sql.execute("alter table epidemiological_focus add case_id int references surveillance_case(id)");
		}
	}
	
	private boolean exists(Connection connection, String name) throws SQLException {
		try (ResultSet result = connection.getMetaData().getTables(null, "PUBLIC", name.toUpperCase(Locale.ROOT), null)) {
			return result.next();
		}
	}
	
	private void refusesWithoutDropping(Connection connection, Database database) throws Exception {
		try {
			new RecoverEmptySurveillanceSchema().execute(database);
			fail("Unsafe recovery must halt");
		}
		catch (CustomChangeException expected) {
			assertTrue(expected.getMessage().contains("Recovery halted"));
		}
		for (String table : tables)
			assertTrue(table, exists(connection, table));
	}
	
	@Test
	public void removesOnlyEmptyModuleTablesInDependencyOrder() throws Exception {
		try (Connection connection = connection()) {
			schema(connection);
			new RecoverEmptySurveillanceSchema().execute(database(connection, true, false));
			for (String table : tables)
				assertFalse(table, exists(connection, table));
			assertTrue(exists(connection, "encounter_diagnosis"));
			try (Statement sql = connection.createStatement();
			        ResultSet rows = sql.executeQuery("select count(*) from encounter_diagnosis")) {
				rows.next();
				assertEquals(1, rows.getInt(1));
			}
		}
	}
	
	@Test
	public void refusesIfAnyOfTheSevenTablesContainsData() throws Exception {
		for (String populated : tables) {
			try (Connection connection = connection()) {
				schema(connection);
				try (Statement sql = connection.createStatement()) {
					sql.execute("insert into " + populated + " (id) values (1)");
				}
				refusesWithoutDropping(connection, database(connection, true, false));
			}
		}
	}
	
	@Test
	public void refusesExternalForeignKeysEvenFromAnotherSchema() throws Exception {
		for (String prefix : Arrays.asList("PUBLIC", "OTHER")) {
			try (Connection connection = connection()) {
				schema(connection);
				try (Statement sql = connection.createStatement()) {
					sql.execute("create schema if not exists OTHER");
					sql.execute(
					    "create table " + prefix + ".external_consumer (id int references PUBLIC.notifiable_event(id))");
				}
				refusesWithoutDropping(connection, database(connection, true, false));
			}
		}
	}
	
	@Test
	public void refusesWhenRetirementIsNotRecorded() throws Exception {
		try (Connection connection = connection()) {
			schema(connection);
			refusesWithoutDropping(connection, database(connection, false, false));
		}
	}
	
	@Test
	public void skipsCompletedMigrationAndCleanInstallation() throws Exception {
		try (Connection connection = connection()) {
			new RecoverEmptySurveillanceSchema().execute(database(connection, true, false));
			schema(connection);
			try (Statement sql = connection.createStatement()) {
				sql.execute("insert into notifiable_event values (1)");
			}
			new RecoverEmptySurveillanceSchema().execute(database(connection, true, true));
			for (String table : tables)
				assertTrue(exists(connection, table));
		}
	}
	
	@Test
	public void refusesIncompleteTableSet() throws Exception {
		try (Connection connection = connection()) {
			try (Statement sql = connection.createStatement()) {
				sql.execute("create table notifiable_event(id int)");
			}
			try {
				new RecoverEmptySurveillanceSchema().execute(database(connection, true, false));
				fail("Incomplete schema must halt");
			}
			catch (CustomChangeException expected) {
				assertTrue(exists(connection, "notifiable_event"));
			}
		}
	}
}
