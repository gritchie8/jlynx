package com.github.jlynx;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.TreeMap;

/**
 * Default SQL data type to Java type mappings for object generator. Data types
 * depend on JDBC driver implementation.
 */
class DataTypeMappings {

    static java.util.Map<Integer, String> TYPE_MAPPINGS;

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

    static boolean isNumber(Object obj) {
        return obj instanceof Integer || obj instanceof BigDecimal
                || obj instanceof Double || obj instanceof Long;
    }

}