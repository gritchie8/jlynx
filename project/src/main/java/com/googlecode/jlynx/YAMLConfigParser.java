package com.googlecode.jlynx;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class YAMLConfigParser {

	public static final String file = "META-INF/jlynx.yaml";

	private static Logger logger = LoggerFactory.getLogger();
	private static List<String> lines = new ArrayList<String>();
	private static List<Integer> indents = new ArrayList<Integer>();
	private static String[] cnAttribs = new String[] { "name", "datasource",
			"url", "driver", "uid", "passwd" };
	List<ConnectionData> holders = new ArrayList<ConnectionData>();

	void init() throws Throwable {
		try {
			parse();
			filterComments();

			for (int i = 0; i < lines.size(); i++) {
				logger.finer("line#" + (i) + ": (" + indents.get(i) + ") "
						+ lines.get(i));
			}
			parseConnections();
			parseDatePatterns();
			parseClassEntityHash();
			parseNamedQueries();
			parseColMappings();
			parseLoggingLevel();

		} catch (RuntimeException re) {
			re.printStackTrace();
		} catch (Exception e) {
			if (System.getProperty("jlynx.yaml.file") == null)
				logger.warning("jlynx.yaml file not found at META-INF/jlynx.yaml");
			else
				logger.warning("jlynx.yaml file not found at "
						+ System.getProperty("jlynx.yaml.file"));
		}

	}

	private void parseLoggingLevel() {
		for (String line : lines) {
			if (line.indexOf("java-logging-level:") != -1) {
				String level = line
						.substring(line.indexOf(":") + 1, line.length()).trim()
						.toUpperCase();
				logger.info("Logging level set to: Level."
						+ level.toUpperCase());
				Config.loggingLevel = level.toUpperCase();
				break;
			}
		}
		if (Config.loggingLevel == null)
			logger.severe("### You should set the logging level in jlynx.yaml ### e.g. java-logging-level: finer");

	}

	private void addConnections() {
		for (int i = 0; i < holders.size(); i++) {
			if (holders.get(i) instanceof ConnectionData) {
				ConnectionData conn = (ConnectionData) holders.get(i);
				if (conn.getName() != null) {
					if (conn.getDatasource() == null) {
						List<String> connParams = new ArrayList<String>();
						connParams.add(conn.getDriver());
						connParams.add(conn.getUrl());
						connParams.add(conn.getUid());
						String pwd = (conn.getPasswd() == null) ? "" : conn
								.getPasswd();
						connParams.add(pwd);
						DAOImpl.connMap.put(conn.getName(), connParams);
						logger.info("JDBC connection added: " + conn.getName());
					} else {
						DAOImpl.connMap.put(conn.getName(),
								conn.getDatasource());
						logger.info("Datasourse connection added: "
								+ conn.getName());
					}
				}
			}
		}
	}

	private void addNamedQry(String name, String qry) {
		if (name != null && qry != null) {
			qry = qry.trim();
			Config.NAMED_QUERY_MAPPING.put(name, qry);
			logger.fine("query name '" + name + "' = '" + qry + "'");
		}
	}

	private void filterComments() {
		List<String> l = new ArrayList<String>();
		List<Integer> indented = new ArrayList<Integer>();
		for (int i = 0; i < lines.size(); i++) {
			String str = lines.get(i).toString();
			logger.finest("(" + i + ") " + str);
			int index = str.indexOf("#");
			if (index > 1) {
				l.add(str.substring(0, index).trim());
				indented.add(indents.get(i));
			} else if (index == -1) {
				l.add(str);
				indented.add(indents.get(i));
			}
		}
		lines = l;
		indents = indented;
	}

	private void parse() throws IOException {

		String yaml = (System.getProperty("jlynx.yaml.file") == null) ? file
				: System.getProperty("jlynx.yaml.file");
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(yaml);
		if (!yaml.equals(file))
			is = new FileInputStream(yaml);
		int blen = is.available();
		blen++;
		byte[] bytes = new byte[blen];
		if (blen > 0)
			logger.info("jLynx configuration file is " + yaml);
		is.read(bytes);
		bytes[blen - 1] = '\n';

		StringBuffer line = new StringBuffer();
		int indented = 0;
		boolean txtStarted = false;
		for (int i = 0; i < blen; i++) {
			char c = (char) bytes[i];
			// System.out.print(c);
			boolean isSpace = Character.isWhitespace(c);
			boolean isNewline = (c == '\n' || c == '\r');
			if (isNewline) {

				// add non-empty lines
				if (line.toString().trim().length() > 0) {
					String linestr = line.toString().trim();
					lines.add(linestr);
					indents.add(new Integer(indented));
				}
				txtStarted = false;
				indented = 0;
				line = new StringBuffer();
			} else {
				if (isSpace && !txtStarted)
					indented++;
				else {
					if (!txtStarted)
						txtStarted = true;
					if (c != '\t')
						line.append(c);
				}
			}
		}
	}

	private void parseClassEntityHash() {
		int startAt = -1;
		boolean found = false;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equals("class-entity-mappings:")) {
				logger.finest("starting class-entity-mappings at non-empty line#"
						+ i);
				startAt = i + 1;
				found = true;
			}
		}

		try {
			while (found) {

				String str;
				try {
					str = lines.get(startAt).toString();
				} catch (Exception e) {
					break;
				}

				found = str.indexOf("- ") == 0;
				if (found) {
					startAt++;

					String[] split = StringUtils.splitAtFirstColon(str);
					if (split.length == 2) {
						String cls = split[0].substring(str.indexOf("- ") + 2)
								.trim();
						String entity = split[1].trim();
						logger.info(cls + ".class maps to entity '" + entity
								+ "'");
						DAOImpl.entityMap.put(cls, entity);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseColMappings() {
		try {
			int startAt = 0;
			int endAt = 0;
			boolean colOk = false;
			for (int i = 0; i < lines.size(); i++) {
				String str = lines.get(i).toString();
				if (startAt == 0 && str.indexOf("column-mappings:") != -1) {
					colOk = true;
					startAt = i + 1;
					endAt = i + 1;

				} else if (startAt > 0
						&& (str.indexOf("pattern:") > 0
								|| str.indexOf("ed-queries") > 0
								|| str.indexOf("-mappings:") > 0 || str
								.indexOf("nnections:") > 0))
					break;
				else
					endAt++;
			}

			while (colOk && (startAt < endAt)) {
				String str = lines.get(startAt++).toString();
				String col = str.substring(str.indexOf(":") + 1, str.length())
						.trim();
				String prop = str.substring(str.indexOf("- ") + 1,
						str.indexOf(":")).trim();
				if (prop != null && col != null) {
					int lastDot = prop.lastIndexOf(".");
					String cls = prop.substring(0, lastDot);
					prop = prop.substring(lastDot + 1);// .toLowerCase();
					logger.fine(cls + "->" + prop);
					Class<?> toAdd = Class.forName(cls);
					if (Config.COL_MAPPING.get(toAdd) == null)
						Config.COL_MAPPING.put(toAdd,
								new LinkedHashMap<String, String>());
					Map<String, String> colMap = Config.COL_MAPPING.get(toAdd);
					colMap.put("p:" + prop.toLowerCase(), col);
					colMap.put("c:" + col.toLowerCase(), prop);
					// Config.COL_MAPPING.put(prop.toLowerCase(), col);
					// Config.COL_MAPPING.put(revCol, prop.substring(lastDot +
					// 1));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Class<?> key : Config.COL_MAPPING.keySet()) {
			logger.info("Column mapping: " + key + "\n -> "
					+ Config.COL_MAPPING.get(key));
		}
	}

	private void parseConnections() {
		int startAt = 0;
		int endAt = 0;
		boolean found;

		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equals("connections:")) {
				startAt = i;
				found = true;
				endAt = startAt;
			}
		}

		do {
			String str;
			try {
				str = lines.get(++endAt).toString();
			} catch (IndexOutOfBoundsException e) {
				break;
			}
			found = false;
			for (int j = 0; j < cnAttribs.length; j++) {
				if (str.indexOf(cnAttribs[j]) != -1) {
					found = true;
					// endAt++;
					break;
				}
			}
		} while (found);

		ConnectionData ch = null;
		for (int i = startAt; i < endAt; i++) {
			String str = lines.get(i).toString();

			if (str.indexOf("-") == 0) {
				ch = new ConnectionData();
				holders.add(ch);
			}

			logger.finest(str + " ch=" + ch);

			String[] split = StringUtils.splitAtFirstColon(str);
			if (split.length == 2 && split[1] != null) {

				for (int j = 0; j < cnAttribs.length; j++) {
					if (split[0].indexOf(cnAttribs[j]) != -1) {
						String value = split[1].trim();
						BeanUtils.setValue(cnAttribs[j], ch, value);
					}
				}
			}
		}

		addConnections();

	}

	private void parseDatePatterns() {
		for (int i = 0; i < lines.size(); i++) {
			String str = lines.get(i).toString();
			if (str.indexOf("date-pattern:") != -1) {
				String dp = str.substring(str.indexOf(":") + 1, str.length())
						.trim();
				logger.info("Date format pattern for java.sql.Date objects: "
						+ dp);
				Config.datePattern = dp;

			} else if (str.indexOf("timestamp-pattern:") != -1) {
				String tsp = str.substring(str.indexOf(":") + 1, str.length())
						.trim();
				logger.info("Date format pattern for java.sql.Timestamp objects: "
						+ tsp);

				Config.tsPattern = tsp;
			}
		}
	}

	private void parseNamedQueries() {
		int startAt = -1;
		int endAt = -1;
		for (int i = 0; i < lines.size(); i++) {
			String str = lines.get(i).toString();
			if (str.indexOf("named-queries:") == 0) {
				startAt = i + 1;
				endAt = startAt;
			} else if (startAt > 0
					&& (str.indexOf("pattern:") > 0
							|| str.indexOf("-mappings:") > 0 || str
							.indexOf("nnections:") > 0)) {
				break;
			} else {
				endAt++;
			}

		}
		if (startAt > 0) {
			logger.fine("loading named queries from line#" + startAt + "-"
					+ endAt);

			String name = null;
			String qry = null;

			for (int i = startAt; i < endAt; i++) {
				String str = lines.get(i).toString();
				if (str.indexOf("- ") == 0) {

					addNamedQry(name, qry);

					name = str.substring(2, str.indexOf(":")).trim();
					qry = "";

					if ((str.indexOf(">") > str.indexOf(":"))
							|| (str.indexOf("|") > str.indexOf(":"))) {
						logger.fine("block for query name '" + name + "'");
					} else {
						qry = str.substring(str.indexOf(":") + 1).trim();
						addNamedQry(name, qry);
						name = null;
						qry = null;
					}
				} else if (name != null) {
					qry += str + ' ';
					// logger.fine(qry.trim());
				}
			}
			addNamedQry(name, qry);
		}
	}
}
