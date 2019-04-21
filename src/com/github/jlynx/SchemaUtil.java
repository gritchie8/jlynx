package com.github.jlynx;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Utility class used internally within jLynx to view/build/maintain metadata.
 * Uses JDBC specs at runtime to discover database metadata.
 */
final class SchemaUtil {

    static final int MSSQL = 200;
    static final int MYSQL = 400;
    static final int ORACLE = 100;

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
}