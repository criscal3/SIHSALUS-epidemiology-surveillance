package org.openmrs.module.sihsalusepidemiologicalsurveillance.api.db;

import java.sql.*;
import java.util.*;

import liquibase.change.custom.CustomTaskChange;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/** One-time recovery only. All safety checks precede the first DROP; no CASCADE or FK disabling. */
public class RecoverEmptySurveillanceSchema implements CustomTaskChange {
	
	private static final List<String> TABLES = Arrays.asList("epidemiological_focus", "outbreak_alert", "period_case_count",
	    "surveillance_case", "outbreak_alert_rule", "individual_record", "notifiable_event");
	
	@Override
	public void execute(Database database) throws CustomChangeException {
		try {
			boolean retired = false;
			for (RanChangeSet change : database.getRanChangeSetList()) {
				if (!"sihsalus".equals(change.getAuthor())
				        || !"liquibase-surveillance-case.xml".equals(change.getChangeLog()))
					continue;
				if ("14-create-current-surveillance-schema".equals(change.getId()))
					return; // Never alter a schema recorded as completed.
				if ("13-retire-historical-surveillance-tables".equals(change.getId()))
					retired = true;
			}
			Connection connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
			String catalog = connection.getCatalog();
			String schema = database.getDefaultSchemaName();
			Map<String, String> physical = new HashMap<>();
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet tables = metadata.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
				while (tables.next()) {
					String name = tables.getString("TABLE_NAME");
					if (TABLES.contains(name.toLowerCase(Locale.ROOT)))
						physical.put(name.toLowerCase(Locale.ROOT), name);
				}
			}
			if (physical.isEmpty())
				return; // Normal clean installation.
			if (!retired || physical.size() != TABLES.size())
				throw new CustomChangeException(
				        "Recovery halted: expected changeset 13 and all seven surveillance tables. Ask the administrator to inspect the partial schema.");
			for (String table : TABLES) {
				String actual = physical.get(table);
				try (Statement statement = connection.createStatement();
				        ResultSet rows = statement
				                .executeQuery("SELECT COUNT(*) FROM " + database.escapeTableName(catalog, schema, actual))) {
					rows.next();
					if (rows.getLong(1) != 0)
						throw new CustomChangeException(
						        "Recovery halted: " + table + " contains data. No tables were removed.");
				}
				try (ResultSet keys = metadata.getExportedKeys(catalog, schema, actual)) {
					while (keys.next()) {
						String child = keys.getString("FKTABLE_NAME").toLowerCase(Locale.ROOT);
						if (!Objects.equals(keys.getString("PKTABLE_CAT"), keys.getString("FKTABLE_CAT"))
						        || !Objects.equals(keys.getString("PKTABLE_SCHEM"), keys.getString("FKTABLE_SCHEM"))
						        || !TABLES.contains(child) || TABLES.indexOf(child) >= TABLES.indexOf(table))
							throw new CustomChangeException(
							        "Recovery halted: an external or unexpected foreign key references " + table
							                + ". No tables were removed.");
					}
				}
			}
			// Use only the allowlisted tables, in child-before-parent order. Module must remain stopped.
			for (String table : TABLES) {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate("DROP TABLE " + database.escapeTableName(catalog, schema, physical.get(table)));
				}
			}
		}
		catch (CustomChangeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CustomChangeException(
			        "Surveillance schema recovery failed. Keep the module stopped and inspect the schema before retrying.",
			        ex);
		}
	}
	
	@Override
	public String getConfirmationMessage() {
		return "Guarded surveillance recovery completed or not required.";
	}
	
	@Override
	public void setUp() {
	}
	
	@Override
	public void setFileOpener(ResourceAccessor accessor) {
	}
	
	@Override
	public ValidationErrors validate(Database database) {
		return new ValidationErrors();
	}
}
