package com.googlecode.jlynx;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.TreeMap;

/**
 * Default SQL data type to Java type mappings for object generator. Data types
 * depend on JDBC driver implementation.
 */
class DataTypeMappings {

	public static java.util.Map<Integer, String> TYPE_MAPPINGS;

	protected static boolean isNumber(Object obj) {

		return obj instanceof Integer || obj instanceof BigDecimal
				|| obj instanceof Double || obj instanceof Long;

	}

	static {

		TYPE_MAPPINGS = new TreeMap<Integer, String>();
		TYPE_MAPPINGS.put(new Integer(Types.BIT), "boolean");

		TYPE_MAPPINGS.put(new Integer(Types.BLOB), "java.sql.Blob");
		TYPE_MAPPINGS.put(new Integer(Types.CLOB), "java.sql.Clob");

		TYPE_MAPPINGS.put(new Integer(Types.DATE), "java.sql.Date");
		TYPE_MAPPINGS.put(new Integer(Types.TIME), "java.sql.Time");
		TYPE_MAPPINGS.put(new Integer(Types.TIMESTAMP), "java.sql.Timestamp");

		TYPE_MAPPINGS.put(new Integer(Types.VARCHAR), "String");
		TYPE_MAPPINGS.put(new Integer(Types.CHAR), "String");
		TYPE_MAPPINGS.put(new Integer(Types.LONGVARCHAR), "String");

		TYPE_MAPPINGS.put(new Integer(Types.INTEGER), "Integer");
		TYPE_MAPPINGS.put(new Integer(Types.TINYINT), "Integer");
		TYPE_MAPPINGS.put(new Integer(Types.SMALLINT), "Integer");

		TYPE_MAPPINGS.put(new Integer(Types.BIGINT), "long");

		TYPE_MAPPINGS.put(new Integer(Types.NUMERIC), "java.math.BigDecimal");
		TYPE_MAPPINGS.put(new Integer(Types.DECIMAL), "java.math.BigDecimal");

		TYPE_MAPPINGS.put(new Integer(Types.REAL), "float");

		TYPE_MAPPINGS.put(new Integer(Types.FLOAT), "double");
		TYPE_MAPPINGS.put(new Integer(Types.DOUBLE), "double");

	}

}