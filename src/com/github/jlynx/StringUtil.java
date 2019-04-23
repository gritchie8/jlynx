package com.github.jlynx;

/**
 * Utility class used internally by jLynx
 */
final class StringUtil {


    static String escapeQuotes(String str) {

        if (str != null) {
            return replace(str, "'", "''");
            // jdk 1.3 issue str.replaceAll("'", "''");
        } else {
            return null;
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


    static String fixNulls(String sqlIn) {
        return replace(sqlIn, "'null'", "NULL");
    }

}