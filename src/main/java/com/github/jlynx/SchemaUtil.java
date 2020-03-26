package com.github.jlynx;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Utility class used internally within jLynx to view/build/maintain metadata.
 * Uses JDBC specs at runtime to discover database metadata.
 */
final class SchemaUtil {

    static final int ORACLE = 100;
    static final int MSSQL = 200;

    static java.util.Map<Integer, String> TYPE_MAPPINGS;
    private static Map<String, Set<String>> primaryKeys = new TreeMap<String, Set<String>>();

    static {

        TYPE_MAPPINGS = new TreeMap<Integer, String>();
        TYPE_MAPPINGS.put(Types.BIT, "boolean");

        TYPE_MAPPINGS.put(Types.BLOB, "java.sql.Blob");
        TYPE_MAPPINGS.put(Types.CLOB, "java.sql.Clob");

        TYPE_MAPPINGS.put(Types.DATE, "java.sql.Date");
        TYPE_MAPPINGS.put(Types.TIME, "java.sql.Time");
        TYPE_MAPPINGS.put(Types.TIMESTAMP, "java.sql.Timestamp");

        TYPE_MAPPINGS.put(Types.VARCHAR, "String");
        TYPE_MAPPINGS.put(Types.CHAR, "String");
        TYPE_MAPPINGS.put(Types.LONGVARCHAR, "String");

        TYPE_MAPPINGS.put(Types.INTEGER, "Integer");
        TYPE_MAPPINGS.put(Types.TINYINT, "Integer");
        TYPE_MAPPINGS.put(Types.SMALLINT, "Integer");

        TYPE_MAPPINGS.put(Types.BIGINT, "long");

        TYPE_MAPPINGS.put(Types.NUMERIC, "java.math.BigDecimal");
        TYPE_MAPPINGS.put(Types.DECIMAL, "java.math.BigDecimal");

        TYPE_MAPPINGS.put(Types.REAL, "float");

        TYPE_MAPPINGS.put(Types.FLOAT, "double");
        TYPE_MAPPINGS.put(Types.DOUBLE, "double");

    }

    static int findDbVendor(DatabaseMetaData dm) throws SQLException {
        String dbName = dm.getDatabaseProductName();

        if ("Microsoft SQL Server".equalsIgnoreCase(dbName))
            return MSSQL;
        else if ("Oracle".equalsIgnoreCase(dbName))
            return ORACLE;

        return 0;
    }

    static Set<String> getPK(Connection conn, String table) throws SQLException {

        String url = conn.getMetaData().getURL();
        String key = url + "|" + table;
        Set<String> pks = primaryKeys.get(key);

        if (pks == null) {

            ResultSet resultSet = conn.getMetaData().getPrimaryKeys(null, null, table);

            pks = new HashSet<String>();
            String pk = null;
            while (resultSet.next()) {
                pk = resultSet.getString(4);
                pks.add(pk);
            }

            if (pk == null)
                throw new SQLException("Primary key missing for table: " + table);

            pks.remove("");
            pks.remove(null);
            if (!pks.isEmpty())
                primaryKeys.put(key, pks);

        }

        return primaryKeys.get(key);
    }

    static boolean isNumber(Object obj) {
        return obj instanceof Integer || obj instanceof BigDecimal
                || obj instanceof Double || obj instanceof Long;
    }
}