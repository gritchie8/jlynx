package com.googlecode.jlynx;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class used internally within jLynx to view/build/maintain metadata.
 * Uses JDBC specs at runtime to discover database metadata.
 */
final class SchemaUtil {

	static final int MSSQL = 200;
	static final int MYSQL = 400;
	static final int ORACLE = 100;

	private static Connection conn;
	private static String entTypes[] = { "TABLE" };

	private static java.util.logging.Logger logger = LoggerFactory.getLogger();

	static int findDbVendor(DatabaseMetaData dm) throws SQLException {
		String dbName = dm.getDatabaseProductName();

		if ("MySQL".equalsIgnoreCase(dbName))
			return MYSQL;
		else if ("Microsoft SQL Server".equalsIgnoreCase(dbName))
			return MSSQL;
		else if ("Oracle".equalsIgnoreCase(dbName))
			return ORACLE;

		return 0;
	}

	static Map<String, String> getColumns(String table) throws SQLException {

		Map<String, String> result = new TreeMap<String, String>();
		DatabaseMetaData dmd = conn.getMetaData();

		ResultSet rs = dmd.getColumns(null, null, table.toUpperCase(), null);
		// NPE 4.12.2007

		logger.fine("rs=" + rs);
		if (rs != null && !rs.next())
			throw new SQLException("No columns");

		do {

			logger.finest("col=" + rs.getObject(4));
			String col = rs.getObject(4).toString().toLowerCase();
			String type = getDataType(rs.getInt(5));
			result.put(col, type);

		} while (rs.next());

		return result;

	}

	private static String getDataType(int type) {

		Integer jType = new Integer(type);
		String result = (String) DataTypeMappings.TYPE_MAPPINGS.get(jType);

		if (result == null)
			result = "Object";

		return result;

	}

	static Map<String, String> getPrimaryKeys(String table) {
		Map<String, String> result = new TreeMap<String, String>();
		try {
			// connect();
			DatabaseMetaData dmd = conn.getMetaData();

			ResultSet rs = dmd.getPrimaryKeys(null, null, table);
			while (rs.next()) {
				String col = rs.getString(4);
				result.put(col, "Primary Key");
			}
		} catch (SQLException e) {
			logger.severe(e.getMessage());
		}

		logger.fine("Entity Name = " + table + " Primary Key(s) = " + result);

		return result;

	}

	static Map<String, String> getTables(Connection cn, String schema)
			throws SQLException {

		logger.fine("Entering cn=" + cn);
		conn = cn;

		Map<String, String> result = new TreeMap<String, String>();
		if (cn == null) {
			conn = null;
			// connect();
			cn = conn;
		}
		DatabaseMetaData dmd = cn.getMetaData();
		ResultSet rs = null;
		rs = dmd.getTables(null, schema, null, entTypes);

		// removed schema 3.22.05
		while (rs.next()) {
			String tbl = rs.getString(3);
			logger.fine("getTables :: " + tbl);
			result.put(tbl, rs.getString(4));
		}

		// MS SQL Server system tables
		result.remove("syssegments");
		result.remove("sysconstraints");
		return result;
	}

}