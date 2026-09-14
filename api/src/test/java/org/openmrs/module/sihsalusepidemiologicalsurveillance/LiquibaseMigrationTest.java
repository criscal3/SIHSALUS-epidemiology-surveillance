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
				sql.execute("create table concept(concept_id int primary key)");
				sql.execute("create table users(user_id int primary key)");
				sql.execute("create table location(location_id int primary key)");
				sql.execute("create table encounter(encounter_id int primary key)");
				sql.execute(
				    "create table scheduler_task_config(task_config_id int auto_increment primary key, name varchar(255), description varchar(1024), schedulable_class varchar(1024), repeat_interval bigint, start_on_startup boolean, started boolean, uuid varchar(38) unique, created_by int)");
				sql.execute("insert into concept values (1)");
			}
			Database database = DatabaseFactory.getInstance()
			        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
			Liquibase liquibase = new Liquibase("liquibase.xml", new ClassLoaderResourceAccessor(), database);
			liquibase.update("");
			liquibase.update("");
			try (Statement sql = connection.createStatement()) {
				ResultSet tasks = sql.executeQuery("select count(*) from scheduler_task_config where start_on_startup=true");
				tasks.next();
				assertEquals(1, tasks.getInt(1));
				tasks.close();
				ResultSet tables = connection.getMetaData().getTables(null, null, "EVENTO_NOTIFICABLE", null);
				assertTrue(tables.next());
				sql.execute(
				    "insert into evento_notificable(evento_notificable_id,uuid,concept_id,nombre,periodicidad,plazo_dias) values(1,'event',1,'Synthetic','semanal',7)");
				fails(sql,
				    "insert into regla_alerta_brote(uuid,evento_notificable_id,tipo_condicion,activa) values('orphan',99,'EPIDEMIC',true)");
				sql.execute(
				    "insert into conteo_casos_periodo(uuid,evento_notificable_id,tipo_periodo,anio,numero_periodo,numero_casos) values('count',1,'semana',2026,1,2)");
				fails(sql,
				    "insert into conteo_casos_periodo(uuid,evento_notificable_id,tipo_periodo,anio,numero_periodo,numero_casos) values('duplicate',1,'semana',2026,1,3)");
				fails(sql, "delete from evento_notificable where evento_notificable_id=1");
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
