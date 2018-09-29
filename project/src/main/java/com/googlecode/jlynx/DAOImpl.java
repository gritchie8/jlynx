package com.googlecode.jlynx;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Implementation of DAO interface.
 * 
 * @see com.googlecode.jlynx.DAO
 */
public class DAOImpl implements DAO {

	private final Logger _logger = LoggerFactory.getLogger();
	private final boolean _finestOk = _logger.isLoggable(Level.FINEST);
	private final boolean _fineOk = _logger.isLoggable(Level.FINE);

	static final Map<String, Object> connMap = new TreeMap<String, Object>();
	static final Map<String, String> entityMap = new TreeMap<String, String>();
	private static final long serialVersionUID = 1L;

	static {

		Config cp = new Config();

		if (connMap.keySet().size() > 0) {
			LoggerFactory
					.getLogger()
					.info(cp.getClass().getCanonicalName()
							+ " >> connections configured: " + connMap.keySet());
		}
		Iterator<String> iter = entityMap.keySet().iterator();
		while (iter.hasNext()) {
			String o = iter.next();
			LoggerFactory.getLogger().info(
					"class/entity pairing :: " + o + " maps to "
							+ entityMap.get(o));
		}
	}

	// pre-condition: ResultSet next() called
	private Object setValues(ResultSet rs, Object object) throws Exception {

		if (object == null)
			return null;

		try {

			int colCount = rs.getMetaData().getColumnCount();
			for (int i = 1; i <= colCount; i++) {

				String colName = rs.getMetaData().getColumnName(i);
				Object value;
				int type = rs.getMetaData().getColumnType(i);
				if (type == Types.BLOB || type == Types.BINARY
						|| type == Types.LONGVARBINARY) {
					value = rs.getBlob(i);
					if (value != null) {
						value = ((Blob) value).getBinaryStream();
					}
				} else if (type == Types.VARCHAR || type == Types.CLOB
						|| type == Types.LONGVARCHAR) {
					value = rs.getString(i);
				} else
					value = rs.getObject(i);

				BeanUtils.setValue(getProp(colName), object, value);

			}// end for

		} catch (SQLException e) {
			// logger.severe("Oops! Database error: " + e.getMessage());
			throw e;
		}
		return object;
	}

	// private member fields
	private boolean _autoCommit = true;
	private Object _bean;
	private Connection _conn;
	private Properties _cnProps;
	private String _dsName;
	private int _dbVendor;
	private String _entityName;
	private boolean _initialized = false;
	private boolean _keepNullsInQuery = false;
	private Set<String> _keys;
	private PreparedStatement _ps;
	private ResultSet _rs;
	private Statement _stmt;
	private Map<String, String> _colMap = null;
	private String _cnUrl;

	/**
	 * Create a <code>com.googlecode.jlynx.DAO</code> instance.
	 */
	public DAOImpl() {
	}

	/**
	 * Create a <code>com.googlecode.jlynx.DAO</code> instance without using
	 * jlynx.yaml configured <code>java.sql.Connection</code>.
	 * 
	 * @param url
	 *            JDBC database connection String
	 * @param connectionProps
	 *            JDBC properties
	 * @throws SQLException
	 */
	public DAOImpl(String url, Properties connectionProps) throws SQLException {
		this._cnUrl = url;
		this._cnProps = connectionProps;
	}

	/**
	 * Create a <code>com.googlecode.jlynx.DAO</code> instance using provided
	 * POJO.
	 * 
	 * @param pojo
	 *            a POJO or Class
	 */
	@SuppressWarnings("unchecked")
	public DAOImpl(Object pojo) {

		if (pojo instanceof Class<?>) {
			setEntity((Class<Object>) pojo.getClass());
		} else if (pojo instanceof Collection || pojo instanceof Map) {
			throw new UnsupportedOperationException();
		} else {
			setEntity((Class<Object>) pojo.getClass());
		}

		this._bean = pojo;

		if (_fineOk) {
			_logger.fine("Creating instance of " + DAO.class.getName()
					+ " :: using object of type: " + pojo.getClass().getName());
			_logger.fine("Current DB entity is: '" + getEntity()
					+ "' :: Switch entity using #setEntity()");
		}
	}

	public void commit() {
		if (_conn != null) {
			try {
				_conn.commit();
				this._autoCommit = true;
				release();
			} catch (SQLException e) {
				_logger.severe(e.getMessage());
				e.printStackTrace();
			} finally {
				this._autoCommit = true;
				_conn = null;
			}
		} else if (_fineOk)
			_logger.fine("Connection already closed!");
	}

	final void connect() throws SQLException {
		if (_finestOk)
			_logger.finest("#connect() " + _initialized);
		if (_conn != null) {
			if (_finestOk)
				_logger.finest("Connection is not null " + _conn);
		} else if (_cnUrl != null)
			this._conn = DriverManager.getConnection(_cnUrl, _cnProps);
		else if (_dsName == null)
			setConnection("default");
		else if (_dsName != null)
			connectDS();

		if (_conn != null) {
			this._dbVendor = SchemaUtil.findDbVendor(_conn.getMetaData());
			_conn.setAutoCommit(_autoCommit);
		} else {
			throw new SQLException("Connection not configured!");
		}

	}

	private void connectDS() throws SQLException {

		if (_finestOk)
			_logger.finest("#connectDS() " + _dsName);

		Context ctx = null;
		DataSource ds = null;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/" + this._dsName);
			if (ds == null)
				throw new NamingException();

		} catch (NamingException e) {
			try {
				if (ctx != null) {
					ds = (DataSource) ctx.lookup(this._dsName);
				}
			} catch (NamingException e1) {
				// e1.printStackTrace();
				_logger.severe(e.getMessage());
			}
		} finally {
			if (ds != null) {
				_conn = ds.getConnection();
				_conn.setAutoCommit(_autoCommit);
			} else
				throw new SQLException("Could not lookup connection by name: "
						+ _dsName);
		}

		// return this;
	}

	private synchronized String createFilterStmt() throws SQLException {

		StringBuffer sql = new StringBuffer();
		final String and = " AND ";

		Object obj = this._bean;
		if (_keys == null)
			initPK();

		for (String key : _keys) {

			String prop = getProp(key);

			sql.append(and).append(getDbColumn(key));

			final Object partKeyValue = BeanUtils.getValue(prop, obj);

			if (partKeyValue == null)
				throw new SQLException("Primary Key value is missing! col="
						+ key + " " + obj + " prop=" + prop + "");

			String delimiter = DataTypeMappings.isNumber(partKeyValue) ? ""
					: "'";

			sql.append(" = ").append(delimiter).append(partKeyValue.toString())
					.append(delimiter);
		}

		return " WHERE " + sql.substring(and.length());
	}

	private String createInsertStmt() {

		StringBuffer sql = new StringBuffer();

		Map<String, Object> props = (Map<String, Object>) BeanUtils
				.describe(_bean);
		String delimiter;
		Object obj = _bean;

		// TODO remove transient properties from this object
		removeNulls(props);

		String[] fields = new String[props.size()];
		int j = 0;
		for (String prop : props.keySet()) {
			fields[j++] = prop;
			sql.append(getDbColumn(prop));
			if (j < fields.length) {
				sql.append(",");
			} else {
				// fix for configured column mappings
				String end = ",$END-OF-COLUMN65$";
				sql.append(end);
				sql = new StringBuffer(StringUtils.replace(sql.toString(), end,
						""));
				sql.append(") VALUES (");
			}
		}

		Iterator<Object> i = props.values().iterator();
		j = 0;
		while (i.hasNext()) {

			Object val = i.next();
			// logger.fine(val);
			String oracleDate1 = "";
			String oracleDate2 = "";

			if (_dbVendor == SchemaUtil.ORACLE) {

				// Oracle fix for Dates
				if (java.sql.Timestamp.class.equals(BeanUtils.getType(
						fields[j], obj))) {
					oracleDate1 = "to_date(";
					oracleDate2 = ",'yyyy-mm-dd hh24:mi:ss\".999\"')";
				}
			}

			if (_dbVendor == SchemaUtil.MSSQL) {

				// MSSQL fix for Bits/Boolean
				Class<?> cls = BeanUtils.getType(fields[j], obj);

				if (Boolean.class.equals(cls)) {

					Boolean valB = (Boolean) val;

					if (valB.booleanValue())
						val = "1";
					else
						val = "0";

					if (_finestOk)
						_logger.finest("SQL bit fix :: " + cls);

				} else if (cls == boolean.class) {
					// boolean bit = ((Boolean) val).booleanValue();
					if ("true".equals(val.toString()))
						val = "1";
					else
						val = "0";

					if (_finestOk)
						_logger.finest("SQL bit fix :: " + cls);
				}

			}

			Object field;

			// field = PropertyUtils.getProperty(obj, fields[j]);
			field = BeanUtils.getValue(fields[j], obj);

			if (DataTypeMappings.isNumber(field)) {
				delimiter = "";
			} else
				delimiter = "'";

			j++;

			sql.append(oracleDate1).append(delimiter)
					.append(StringUtils.escapeQuotes(val.toString()))
					.append(delimiter).append(oracleDate2);
			if (i.hasNext()) {
				sql.append(",");
			} else {
				sql.append(")");
			}
		}

		String result = "INSERT INTO " + _entityName + " ("
				+ StringUtils.fixNulls(sql.toString());

		if (_fineOk)
			_logger.fine("INSERT stmt: " + result);

		return result;
	}

	private String createSelectStmt() throws NoSuchMethodException,
			SQLException {

		String sql = "SELECT * FROM " + _entityName + createFilterStmt();
		if (_fineOk)
			_logger.fine("SELECT SQL statement: " + sql);
		return sql;
	}

	private String createUpdateStmt() throws Exception {

		String delimiter;

		String sql = "UPDATE " + _entityName + " SET ";

		String where = createFilterStmt();

		if (_finestOk)
			_logger.finest("WHERE clause: " + where);

		Map<String, Object> map = new TreeMap<String, Object>();
		map.putAll(BeanUtils.describe(_bean));
		Set<String> remove = new HashSet<String>();
		remove.addAll(_keys);

		for (String prop : remove)
			map.remove(getProp(prop));

		// TODO remove useless properties from this object
		// remove null values from the bean
		removeNulls(map);

		for (String colName : map.keySet()) {
			String value = null;
			Object oValue = map.get(colName);

			if (oValue != null) {
				value = StringUtils.escapeQuotes(oValue.toString());

				if (this._keys != null) {
					for (String key : _keys) {
						if (colName.equalsIgnoreCase(key))
							value = null;
					}
				} else
					throw new SQLException(
							"Primary key(s) is not set for the UPDATE query");

			}

			if (value != null) {

				String oracleDate1 = "";
				String oracleDate2 = "";

				if (_dbVendor == SchemaUtil.ORACLE) {

					// Oracle fix for Dates
					Class<?> cls = BeanUtils.getType(colName, _bean);
					if (java.sql.Timestamp.class.equals(cls)) {
						// System.out.println("Oracle Date");
						oracleDate1 = "to_date(";
						oracleDate2 = ",'yyyy-mm-dd hh24:mi:ss\".999\"')";

					}
				}

				if (_dbVendor == SchemaUtil.MSSQL) {

					Class<?> cls = BeanUtils.getType(colName, _bean);

					// MSSQL fix for Bits/Boolean
					if (Boolean.class.equals(cls)) {
						Boolean valB = (Boolean) BeanUtils.getValue(colName,
								_bean);

						if (_finestOk)
							_logger.finest("SQL Server boolean/bit! value="
									+ valB);

						if (valB == null)
							value = null;
						else if (valB.booleanValue())
							value = "1";
						else
							value = "0";

						// logger.fine("SQL bit fix :: " + cls);

					} else if (cls == boolean.class) {

						if ("true".equals(value))
							value = "1";
						else
							value = "0";

						if (_finestOk)
							_logger.finest("SQL bit fix :: " + cls);
					}
				}

				// do fix here for DB2 numeric types
				if (DataTypeMappings.isNumber(BeanUtils
						.getValue(colName, _bean))) {
					delimiter = "";
				} else
					delimiter = "'";

				sql += getDbColumn(colName) + "=" + oracleDate1 + delimiter
						+ value + delimiter + oracleDate2 + ", ";
				// logger.fine(sql);
			}

		}

		sql = sql.substring(0, sql.length() - 2) + where;

		sql = StringUtils.fixNulls(sql);

		if (_fineOk)
			_logger.fine("UPDATE stmt: " + sql);

		return sql;
	}

	Connection defaultConnection() throws SQLException {
		connect();
		return _conn;
	}

	public final boolean delete() throws SQLException {

		if (_bean instanceof Map)
			throw new UnsupportedOperationException();

		connect();
		String sql = null;

		try {
			sql = "DELETE FROM " + _entityName + createFilterStmt();

			// logger.fine(sql);
			_stmt = _conn.createStatement();
			int result = _stmt.executeUpdate(sql);
			return result == 1;
		} catch (Exception e) {
			_logger.severe(e.getMessage());
			e.printStackTrace();
			if (e instanceof SQLException) {
				throw (SQLException) e;
			} else
				throw new SQLException(e.getMessage());
		} finally {
			if (_fineOk)
				_logger.fine(sql);
			release();
		}
	}

	public boolean exec(String query, Object[] p) throws SQLException {

		String q = Config.getQuery(query);
		String sql = (q == null || "".equals(q)) ? query : q;

		if (_fineOk)
			_logger.fine("SQL stmt = " + sql);

		boolean result;
		connect();

		_ps = _conn.prepareStatement(sql);
		if (_finestOk)
			_logger.finest("conn = " + _conn + ":" + _autoCommit);

		setParams(p);
		result = _ps.execute();
		if (this._autoCommit)
			release();
		if (_finestOk)
			_logger.finest("Exiting");
		return result;

	}

	private List<?> executeQuery() throws SQLException {

		List<Object> result = new ArrayList<Object>();

		if (_ps != null && _bean != null)
			_rs = _ps.executeQuery();
		else {
			throw new SQLException("result bean not found!");
		}

		while (_rs != null && _rs.next()) {
			try {
				Object obj;
				if (_bean instanceof Class)
					obj = ((Class<?>) _bean).newInstance();
				else
					obj = _bean.getClass().newInstance();
				obj = setValues(_rs, obj);
				result.add(obj);

			} catch (Exception e) {
				e.printStackTrace();
				_logger.severe("Problem setting values from ResultSet :: "
						+ _bean);
			}
		}

		release();
		return result;

	}

	public String fetchJSON(String query, Object[] params) throws SQLException {

		List<?> list = fetchList(query, params);
		return toJSON(list);

	}

	public List<?> fetchList(String query, Object[] p) throws SQLException {

		connect();

		String sql = (Config.getQuery(query) == null || "".equals(Config
				.getQuery(query))) ? query : Config.getQuery(query);

		if (_fineOk)
			_logger.fine("SQL stmt = " + sql);

		this._ps = this._conn.prepareStatement(sql);

		setParams(p);
		return executeQuery();
	}

	public String fetchXML(String namedQuery, Object[] params,
			String elementName) throws SQLException {
		List<?> list = this.fetchList(namedQuery, params);
		return BeanUtils.toXml(list, elementName);
	}

	public void generateCode(String pkg, String schema) {
		try {
			connect();
			Generator.generateCode(pkg, schema, null, null, _conn);
		} catch (Exception e) {
			if (!(e instanceof NullPointerException))
				_logger.severe(e.getMessage());
			e.printStackTrace();
		}
	}

	// Returns the entityName.
	private String getEntity() {
		return _entityName;
	}

	private String getDbColumn(String prop) {
		if (_finestOk)
			_logger.finest("colMap " + _colMap + " " + _bean);
		String prop2 = "p:" + prop.toLowerCase();
		return _colMap != null && _colMap.containsKey(prop2) ? _colMap
				.get(prop2) : prop;
	}

	private String getProp(String col) {
		if (_finestOk)
			_logger.finest("colMap " + _colMap);
		String col2 = "c:" + col.toLowerCase();
		return _colMap != null && _colMap.containsKey(col2) ? _colMap.get(col2)
				: col;
	}

	// initialize; setup primary keys
	private void initPK() throws SQLException {
		_initialized = false;
		_keys = null;
		if (_fineOk)
			_logger.fine(_conn + " entity=" + getEntity());
		if (_conn != null && getEntity() != null) {
			try {
				_keys = PK.getPK(_conn, getEntity());
				setAutoCommit(_conn.getAutoCommit());
				if (_finestOk)
					_logger.finest("Primary key map... " + _keys);
			} catch (SQLException e) {
				_initialized = false;
				throw e;
			}
		}

		if (Config.COL_MAPPING.containsKey(_bean.getClass())) {
			_colMap = Config.COL_MAPPING.get(_bean.getClass());
		} else
			_colMap = null;

		// exit if bean is a Map and setEntity() has not been called yet
		if (_bean == null || (_bean instanceof Map && getEntity() == null))
			return;

		if (_finestOk)
			_logger.finest("entity = " + getEntity() + " object class = "
					+ _bean.getClass());

		_initialized = true;
	}

	public final int insert() throws SQLException {

		connect();
		if (_keys == null)
			initPK();

		int result;
		try {
			String sql = createInsertStmt();
			_stmt = _conn.createStatement();
			if (_fineOk)
				_logger.fine(sql);
			result = _stmt.executeUpdate(sql);
			if (_dbVendor == SchemaUtil.MSSQL && result == 1) {
				try {
					int ident = 1;
					String identitySql = "SELECT SCOPE_IDENTITY()";

					_rs = _stmt.executeQuery(identitySql);
					if (_rs.next())
						ident = _rs.getInt(1);

					if (_fineOk)
						_logger.fine(_dbVendor + " : " + identitySql + " = "
								+ ident);

					result = ident;
				} catch (Exception e) {
					result = 1;
				}
			} else if (_dbVendor == SchemaUtil.MYSQL && result == 1) {

				// use mysql last_insert_id() to return the auto_increment value
				// if it returns 0, return 1 instead

				String ident = "";

				_rs = _stmt.executeQuery("select LAST_INSERT_ID() as ident");
				if (_rs.next())
					ident = _rs.getString(1);

				if (_finestOk)
					_logger.finest("mysql LAST_INSERT_ID() = " + ident);

				try {
					result = Integer.parseInt(ident);
					if (result == 0)
						result = 1;
				} catch (NumberFormatException e) {
					result = 1;
				}
			}

		} catch (Exception e) {
			_logger.severe(e.getMessage());
			// e.printStackTrace();
			if (e instanceof SQLException) {
				throw (SQLException) e;
			} else
				throw new SQLException(e.getMessage());
		} finally {
			release();
		}
		return result;
	}

	private void release() throws SQLException {

		if (_finestOk)
			_logger.finest("AutoCommit = " + this._autoCommit);

		try {
			if (_rs != null) {
				_rs.close();
				_rs = null;
			}
			if (_stmt != null) {
				_stmt.close();
				_stmt = null;
			}
			if (_ps != null) {
				_ps.close();
				_ps = null;
			}
		} catch (SQLException e) {
			_logger.severe(e.getMessage());
		} finally {

			if (_finestOk)
				_logger.finest("conn=" + _conn);

			if (_conn != null && _autoCommit) {
				_conn.close();
				_conn = null;
			}
		}
		if (_finestOk)
			_logger.finest("conn=" + _conn);
	}

	private void removeNulls(Map<String, Object> map) {

		if (_finestOk)
			_logger.finest("Keep nulls in SQL query :: "
					+ this._keepNullsInQuery);

		Set<String> keysToRemove = new HashSet<String>();

		for (String key : map.keySet()) {

			if (_keepNullsInQuery) {
				// for empty Strings put null
				if (map.get(key) == null || "".equals(map.get(key)))
					map.put(key, "null");
			} else {
				if (map.get(key) == null)
					keysToRemove.add(key);
			}
		}
		// remove keys from map
		for (String key : keysToRemove)
			map.remove(key);

	}

	public void rollback() {

		if (_conn != null) {

			try {

				_conn.rollback();
				this._autoCommit = true;
				release();

			} catch (SQLException e) {
				_logger.severe(e.getMessage());
				e.printStackTrace();
			} finally {
				this._autoCommit = true;
			}

		} else if (_fineOk)
			_logger.fine("Connection already closed!");

	}

	public int save() throws SQLException {

		if (_finestOk)
			_logger.finest("Entering save()");
		connect();
		int result = 0;

		if (!_autoCommit) {
			throw new SQLException(
					"save() not supported in transaction, use insert() or update()!");
		} else {
			String where;
			try {
				where = createFilterStmt();
			} catch (Exception e1) {
				e1.printStackTrace();
				if (_fineOk)
					_logger.fine("Primary key value not found! Trying insert()");
				result = insert();
				return result;
			}

			setAutoCommit(false);
			try {

				// 1. find out if object exists in DB first
				// 2. if(exists) update else insert

				StringBuffer select = new StringBuffer("SELECT ");
				select.append(getDbColumn(_keys.iterator().next()));
				select.append(" FROM ").append(getEntity()).append(where);

				if (_fineOk)
					_logger.fine("Does database record exist? test qry = "
							+ select);

				_stmt = _conn.createStatement();
				ResultSet resultset = _stmt.executeQuery(select.toString());
				boolean doUpdate = resultset.next();
				resultset.close();
				// setAutoCommit(true);
				if (doUpdate) {
					if (_fineOk)
						_logger.fine("Yes. Perform update()");
					result = update();
				} else {
					if (_fineOk)
						_logger.fine("Not found. Perform insert()");
					result = insert();
				}

			} catch (Exception e) {
				result = 0;
				_logger.severe(e.getMessage());
				// if (e instanceof SQLException)
				throw new SQLException(e.getMessage());
			} finally {
				setAutoCommit(true);
				release();
			}

		}
		if (_finestOk)
			_logger.finest("Exiting save()");
		return result;
	}

	public void saveNulls(boolean updateNulls) {
		this._keepNullsInQuery = updateNulls;
	}

	public final boolean select() throws SQLException {

		if (_bean instanceof Map)
			throw new UnsupportedOperationException();

		connect();

		String sql;

		try {
			// connect();
			boolean result;
			sql = createSelectStmt();

			_stmt = _conn.createStatement();
			ResultSet resultset = _stmt.executeQuery(sql);

			if ((result = resultset.next()))
				this._bean = setValues(resultset, this._bean);
			if (_fineOk)
				_logger.fine("SELECT stmt: " + sql + " :: result >> "
						+ ((result) ? "success" : "failed"));

			return result;

		} catch (Exception e) {
			_logger.severe(e.getMessage());
			e.printStackTrace();
			if (e instanceof SQLException) {
				throw (SQLException) e;
			} else
				throw new SQLException(e.getMessage());

		} finally {

			release();

		}
	}

	public DAO setAutoCommit(boolean b) {

		if (_conn != null) {
			try {
				this._conn.setAutoCommit(b);
				this._autoCommit = b;
			} catch (SQLException e) {
				_logger.severe("Problem with autoCommit!");
				e.printStackTrace();
			}
		} else {
			this._autoCommit = b;
		}

		return this;
	}

	public DAO setConnection(String cn) {
		if (_finestOk)
			_logger.finest("#setConnection " + cn); // check connMap for
													// connection

		Object cfgConn = connMap.get(cn);

		if (cfgConn == null) {
			_dsName = cn.toString();
		} else {
			if (cfgConn instanceof List) {
				try {
					List<?> cfgList = (List<?>) cfgConn;
					Class.forName(cfgList.get(0).toString());
					// TODO check for NPE
					if (cfgList.get(2) == null) {
						_conn = DriverManager.getConnection(cfgList.get(1)
								.toString());
						if (_fineOk)
							_logger.fine("No user configured!");
					} else
						_conn = DriverManager.getConnection(cfgList.get(1)
								.toString(), cfgList.get(2).toString(), cfgList
								.get(3).toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else
				_dsName = cfgConn.toString();
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	public DAO setBean(Object bean) {
		this._bean = bean;
		setEntity((Class<Object>) bean.getClass());
		try {
			initPK();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (DAO) this;
	}

	private void setEntity(Class<Object> class1) {
		if (_finestOk)
			_logger.finest("Class name = " + class1.getName());
		if (DAOImpl.entityMap.containsKey(class1.getName())) {
			this._entityName = (String) DAOImpl.entityMap.get(class1.getName());
		} else {
			String clsName = class1.getName().toUpperCase();
			String pkgName = (class1.getPackage() == null) ? "" : class1
					.getPackage().getName().toUpperCase()
					+ ".";

			this._entityName = clsName.substring(pkgName.length());
		}
		if (_fineOk)
			_logger.fine("Entity set as: " + this._entityName);
	}

	public DAO setEntity(String entity) {
		this._entityName = entity;
		return this;
	}

	private void setParams(Object[] params) throws SQLException {
		if (params != null && params.length > 0) {
			// int i = 0;
			for (int i = 0; i < params.length;) {
				Object o = params[i];
				if (o instanceof Integer) {
					_ps.setInt(++i, ((Integer) o).intValue());
				} else if (o instanceof InputStream) {
					try {
						if (_finestOk)
							_logger.finest("Setting BLOB param");
						_ps.setBinaryStream(++i, (InputStream) o,
								((InputStream) o).available());

					} catch (IOException e) {
						e.printStackTrace();
						throw new SQLException(
								"Error setting BinaryStream value in PreparedStatement");
					}
				} else
					_ps.setObject(++i, o);
			}
		}
	}

	public String toJSON() {
		return BeanUtils.toJSON(_bean);
	}

	public String toJSON(List<?> list) {
		StringBuffer json = new StringBuffer(
				"{\n  \"Info\": \"jlynx Relational#jsonArray() generated\",\n  \"QueryTime\": \"");
		json.append(new Timestamp(System.currentTimeMillis())).append("\",");
		json.append("\n  \"QueryResults\": {\n");
		json.append("    \"ResultSize\": ").append(list.size()).append(",\n");

		if (list.size() == 0)
			return json.toString() + "    \"Results\": null\n}\n}";

		boolean addClass = true;
		for (int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			if (addClass) {
				json.append("    \"ResultClass\": \"")
						.append(obj.getClass().getName())
						.append("\",\n    \"Results\": [");
				addClass = false;
			}
			String j = BeanUtils.toJSON(obj);
			// int start = j.indexOf("{", 1);
			json.append(j).append(",\n");
		}
		String result = json.toString().substring(0, json.length() - 2);
		return result + "]}}";
	}

	public String toXml() {
		return toXml(null);
	}

	public String toXml(String element) {
		if (_fineOk)
			_logger.fine("Entering method toXml");
		return BeanUtils.toXml(_bean, element);
	}

	public final int update() throws SQLException {

		if (_bean instanceof Map)
			throw new UnsupportedOperationException();

		connect();

		String sql;
		try {
			int result;
			sql = createUpdateStmt();

			_stmt = _conn.createStatement();

			if (_fineOk)
				_logger.fine(sql);

			result = _stmt.executeUpdate(sql);
			return result;

		} catch (Exception e) {
			_logger.severe(e.getMessage());
			if (e instanceof SQLException) {
				throw (SQLException) e;
			} else
				throw new SQLException(e.getMessage());
		} finally {
			release();
		}
	}

}// end class
