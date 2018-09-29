package com.googlecode.jlynx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Parses jLynx configuration at META-INF/jlynx.yaml
 */
class Config implements Serializable {

	private static final String $version = "v1.7.1 (SVN $Revision: 498 $)";

	private final static Logger logger = Logger.getLogger("jlynx");

	private static final long serialVersionUID = 3246587199368951463L;

	static final Map<String, String> NAMED_QUERY_MAPPING = new TreeMap<String, String>();

	static final Map<Class<?>, Map<String, String>> COL_MAPPING = new HashMap<Class<?>, Map<String, String>>();

	static String datePattern = "yyyy-MM-dd";

	static String tsPattern = "yyyy-MM-dd HH:mm:ss.SSS";

	static String loggingLevel = null;

	/**
	 * Returns SQL statement as configured in jlynx.xml.
	 * 
	 * @param namedQuery
	 *            - named query
	 * @return String - SQL statement
	 */
	static String getQuery(String namedQuery) {

		String q = (String) Config.NAMED_QUERY_MAPPING.get(namedQuery);
		if (q == null)
			return "";
		else
			return q;

	}

	Config() {

		try {

			new YAMLConfigParser().init();
			String vendor = (System.getProperty("java.vendor") == null) ? "--"
					: System.getProperty("java.vendor");
			logger.info("jLynx initializing : JVM " + vendor + " version "
					+ System.getProperty("java.version"));

			// boolean parsed = false;

		} catch (Throwable e) {

			e.printStackTrace();
			logger.severe("jLynx YAML config file Error!");
			logger.severe(e.getMessage());

		}

		logger.info("jLynx " + $version + " initialization complete");

	}

}
