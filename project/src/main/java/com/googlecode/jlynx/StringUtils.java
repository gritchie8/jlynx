package com.googlecode.jlynx;

import java.util.StringTokenizer;

/**
 * Utility class used internally by jLynx
 */
final class StringUtils {

	private static String capitalize(String str) {
		return capitalize(str, true);
	}

	private static String capitalize(String str, boolean simpleMode) {

		if (str == null) {
			return null;
		} else if (simpleMode) {
			return Character.toUpperCase(str.charAt(0)) + str.substring(1);
		}
		StringTokenizer st = new StringTokenizer(str, "_");
		String result = "";
		while (st.hasMoreElements()) {
			String s = st.nextToken();
			result += Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}

		return result;

	}

	protected static String createAccessor(String field, String type) {

		if (field == null || type == null) {
			return null;
		} else {
			field = field.toLowerCase();
			field = replace(field, " ", "");
			// jdk 1.3 issue .replaceAll(" ", "");
			return "\n" + get(field, type) + set(field, type);
		}

	}

	protected static String escapeQuotes(String str) {

		if (str != null) {
			return replace(str, "'", "''");
			// jdk 1.3 issue str.replaceAll("'", "''");
		} else {
			return null;
		}

	}

	protected static String mixedCase(String str) {

		str = str.toLowerCase();
		String result = "";

		StringTokenizer st = new StringTokenizer(str, "_");
		while (st.hasMoreElements()) {
			String s = st.nextToken();
			result += Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}
		return result;

	}

	private static String get(String field, String type) {

		if (field == null || type == null) {
			return null;
		} else {

			field = field.toLowerCase();
			field = replace(field, " ", "");
			// jdk 1.3 issue .replaceAll(" ", "");
			return "\n    public " + type + " get" + capitalize(field)
					+ "() {\n        return " + field + ";\n    }";
		}

	}

	static String replace(String source, String pattern, String replace) {
		if (source != null) {
			final int len = pattern.length();
			StringBuffer sb = new StringBuffer();
			int found = -1;
			int start = 0;

			while ((found = source.indexOf(pattern, start)) != -1) {
				sb.append(source.substring(start, found));
				sb.append(replace);
				start = found + len;
			}

			sb.append(source.substring(start));

			return sb.toString();
		} else
			return "";
	}

	private static String set(String field, String type) {

		if (field == null) {
			return null;
		} else {
			field = field.toLowerCase();
			field = replace(field, " ", "");
			// jdk 1.3 issue .replaceAll(" ", "");
			return "\n\n    public void set" + capitalize(field) + "(" + type
					+ " in) {\n        this." + field + " = in;\n    }";
		}

	}

	static String[] splitAtFirstColon(String input) {

		// System.out.println(input);
		int size = (input.indexOf(":") > 0) ? 2 : 1;

		String[] output = new String[size];

		if (size == 1) {
			output[0] = input;
		} else {
			int fc = input.indexOf(":");
			output[0] = input.substring(0, fc);
			output[1] = input.substring(fc + 1, input.length());
		}

		return output;

	}

	static String fixNulls(String sqlIn) {
		return replace(sqlIn, "'null'", "NULL");
	}

}