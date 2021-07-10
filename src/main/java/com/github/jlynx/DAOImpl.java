package com.github.jlynx;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DAO interface.
 *
 * @see com.github.jlynx.DAO
 */
public class DAOImpl implements DAO {

  private final static Map<String, String> entityMap = new TreeMap<>();
  private final static Logger _logger = LoggerFactory.getLogger("jlynx");

  private Object _bean;
  private Connection _conn;
  private Properties _cnProps;
  private String _dsName;
  private int _dbVendor;
  private String _entityName;
  private boolean _keepNullsInQuery;
  private Set<String> _keys;
  private PreparedStatement _ps;
  private ResultSet _rs;
  private Statement _stmt;

  private String _cnUrl;
  private boolean _managedConnection;

  private DAOImpl() {
    if (_logger.isTraceEnabled())
      _logger.trace("#DAOImpl - creating new instance");
  }

  /**
   * Create a <code>com.github.jlynx.DAO</code> instance with an existing
   * Connection.
   *
   * @param connection JDBC database connection
   * @return DAO
   */
  public static DAO newInstance(Connection connection) {
    DAOImpl dao = new DAOImpl();
    dao._conn = connection;
    dao._managedConnection = true;
    return dao;
  }

  /**
   * Create a <code>com.github.jlynx.DAO</code> instance with database URL and
   * connection parameters.
   *
   * @param databaseUrl      JDBC database connection String
   * @param connectionParams JDBC properties
   * @return DAO
   */
  public static DAO newInstance(String databaseUrl, Properties connectionParams) {
    DAOImpl dao = new DAOImpl();
    dao._cnUrl = databaseUrl;
    dao._cnProps = connectionParams;
    return dao;
  }

  /**
   * Create a <code>com.github.jlynx.DAO</code> instance with a DataSource name.
   *
   * @param dataSourceName JNDI
   * @return DAO
   */
  public static DAO newInstance(String dataSourceName) {
    DAOImpl dao = new DAOImpl();
    dao._dsName = dataSourceName;
    return dao;
  }

  // pre-condition: ResultSet next() called
  private void setValues(ResultSet rs, Object object) throws SQLException {

    for (int colIndex = 1; colIndex <= rs.getMetaData().getColumnCount(); colIndex++) {

      String colName = rs.getMetaData().getColumnName(colIndex);
      Object value = null;
      int type = rs.getMetaData().getColumnType(colIndex);

      switch (type) {
        case Types.BLOB:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
          value = rs.getBlob(colIndex);
          if (value != null)
            value = ((Blob) value).getBinaryStream();
          break;
        case Types.VARCHAR:
        case Types.CHAR:
        case Types.CLOB:
        case Types.LONGVARCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
          value = rs.getString(colIndex);
          break;
        case Types.TIMESTAMP:
        case Types.TIMESTAMP_WITH_TIMEZONE:
          value = rs.getObject(colIndex, LocalDateTime.class);
          if (value == null)
            value = rs.getTimestamp(colIndex);
          break;
        case Types.DATE:
          value = rs.getObject(colIndex, LocalDate.class);
          if (value == null)
            value = rs.getDate(colIndex);
          break;
        case Types.INTEGER:
        case Types.TINYINT:
        case Types.SMALLINT:
          value = rs.getInt(colIndex);
          break;
        case Types.BIGINT:
          value = rs.getLong(colIndex);
          break;
        case Types.NUMERIC:
        case Types.DECIMAL:
          value = rs.getBigDecimal(colIndex);
          break;
        case Types.FLOAT:
          value = rs.getFloat(colIndex);
          break;
        case Types.BIT:
        case Types.BOOLEAN:
          value = rs.getBoolean(colIndex);
          break;
        default:
          value = rs.getObject(colIndex);
      }

      if (!entityMap.containsKey(object.getClass().getName()) && object.getClass().isAnnotationPresent(Table.class))
        entityMap.put(object.getClass().getCanonicalName(), object.getClass().getAnnotation(Table.class).value());

      BeanUtil.setValue(colName, object, value);

    }

  }

  private void setClass(Class<?> aClass) throws ReflectiveOperationException {

    Table table = aClass.getAnnotation(Table.class);
    _entityName = table.value();
    _bean = aClass.getConstructor().newInstance();

    if (!aClass.isAnnotationPresent(Table.class))
      _logger.debug(Table.class.getName() + " annotation is missing! " + aClass.getName());

  }

  @Override
  public Connection getConnection() throws SQLException {
    connect();
    return _conn;
  }

  private void connect() throws SQLException {
    if (!_managedConnection && (_conn == null || _conn.isClosed())) {
      if (_cnUrl != null && _cnUrl.length() > 1)
        _conn = DriverManager.getConnection(_cnUrl, _cnProps);
      else if (_dsName != null)
        connectDS();
      else
        throw new RuntimeException("Connection not valid");

      _dbVendor = SchemaUtil.findDbVendor(_conn.getMetaData());
    }
  }

  private void connectDS() throws SQLException {

    Context ctx = null;
    DataSource ds = null;
    try {
      ctx = new InitialContext();
      ds = (DataSource) ctx.lookup("java:comp/env/" + this._dsName);
      if (ds == null)
        throw new NamingException();

    } catch (NamingException e) {
      try {
        if (ctx != null)
          ds = (DataSource) ctx.lookup(_dsName);
      } catch (NamingException e1) {
        e1.printStackTrace();
      }
    } finally {
      if (ds != null)
        _conn = ds.getConnection();
      else
        throw new SQLException("Could not lookup connection by name: " + _dsName);
    }
  }

  private synchronized String createFilterStmt() {

    if (_bean == null)
      throw new UnsupportedOperationException("Call #setBean() before performing database operations.");

    StringBuilder sql = new StringBuilder();
    final String and = " AND ";

    if (_keys == null) {
      try {
        initPK();
      } catch (SQLException e) {
        // check for Column pk annotation
        _keys = new HashSet<>();
        _logger.error(e.getMessage());
        throw new IllegalStateException("Primary key not found " + this._entityName);
      }
    }

    for (String key : _keys) {

      sql.append(and).append(getDbColumn(key));
      Object partKeyValue = null;
      try {
        partKeyValue = BeanUtil.getValue(key, _bean);
      } catch (IllegalAccessException e) {
        _logger.error(e.getMessage());
      } finally {

        if (partKeyValue == null) {
          String message = "Primary key value empty for database column: " + key + ", object: "
              + _bean.getClass().getName();
          throw new IllegalArgumentException(message);
        }
      }

      String delimiter = SchemaUtil.isNumber(partKeyValue) ? "" : "'";
      sql.append(" = ").append(delimiter).append(partKeyValue.toString()).append(delimiter);
    }

    return " WHERE " + sql.substring(and.length());
  }

  private String createInsertStmt() {

    StringBuffer sql = new StringBuffer();

    Map<String, Object> props = BeanUtil.describe(_bean);
    String delimiter;
    Object obj = _bean;

    removeNulls(props);

    if (props.isEmpty())
      return "INSERT INTO " + _entityName + " DEFAULT VALUES";

    String[] fields = new String[props.size()];
    int j = 0;
    for (String prop : props.keySet()) {
      fields[j++] = prop;

      sql.append(getDbColumn(prop));

      if (j < fields.length)
        sql.append(",");
      else {
        // fix for configured column mappings
        String end = ",$END-OF-COLUMN65$";
        sql.append(end);
        sql = new StringBuffer(StringUtil.replace(sql.toString(), end, ""));
        sql.append(") VALUES (");
      }
    }

    Iterator<Object> i = props.values().iterator();
    j = 0;
    while (i.hasNext()) {

      Object val = i.next();
      String oracleDate1 = "";
      String oracleDate2 = "";

      if (_dbVendor == SchemaUtil.ORACLE) {
        // Oracle fix for Dates
        if (java.sql.Timestamp.class.equals(BeanUtil.getType(fields[j], obj))) {
          oracleDate1 = "to_date(";
          oracleDate2 = ",'yyyy-mm-dd hh24:mi:ss\".999\"')";
        } else if (java.sql.Date.class.equals(BeanUtil.getType(fields[j], obj))) {
          oracleDate1 = "to_date(";
          oracleDate2 = ",'yyyy-mm-dd')";
        }
      }

      if (_dbVendor == SchemaUtil.MSSQL) {

        // MSSQL fix for Bits/Boolean
        Class<?> cls = BeanUtil.getType(fields[j], obj);

        if (Boolean.class.equals(cls)) {

          Boolean valB = (Boolean) val;
          val = valB ? "1" : "0";

        } else if (cls == boolean.class) {

          if ("true".equals(val.toString()))
            val = "1";
          else
            val = "0";
        }

      }

      Object field = null;
      try {
        field = BeanUtil.getValue(fields[j], obj);
      } catch (IllegalAccessException e) {
        _logger.error(e.getMessage());
      }
      delimiter = (SchemaUtil.isNumber(field)) ? "" : "'";

      j++;

      sql.append(oracleDate1).append(delimiter).append(StringUtil.escapeQuotes(val.toString())).append(delimiter)
          .append(oracleDate2);

      if (i.hasNext())
        sql.append(",");
      else
        sql.append(")");
    }

    String stmt = "INSERT INTO " + _entityName + " (" + StringUtil.fixNulls(sql.toString());
    if (_logger.isDebugEnabled())
      _logger.debug("#insert - " + stmt);
    return stmt;
  }

  private String createSelectStmt() {
    String stmt = "SELECT * FROM " + _entityName + createFilterStmt();
    if (_logger.isDebugEnabled())
      _logger.debug("#select - " + stmt);
    return stmt;
  }

  private String createUpdateStmt() {

    StringBuilder sql = new StringBuilder("UPDATE ").append(_entityName).append(" SET ");

    String where = createFilterStmt();

    Map<String, Object> map = new TreeMap<>(BeanUtil.describe(_bean));

    Set<String> remove = new HashSet<>(_keys);

    for (String prop : map.keySet())
      for (String pk : _keys)
        if (pk.equalsIgnoreCase(prop))
          remove.add(prop);

    for (String prop : remove)
      map.remove(prop);

    // remove null values from the bean
    removeNulls(map);

    if (map.isEmpty())
      throw new RuntimeException("No values to update for " + _bean.toString());

    for (String colName : map.keySet()) {
      String value = null;
      Object oValue = map.get(colName);

      if (oValue != null)
        value = StringUtil.escapeQuotes(oValue.toString());

      if (value != null) {

        String oracleDate1 = "";
        String oracleDate2 = "";

        if (_dbVendor == SchemaUtil.ORACLE) {

          // Oracle fix for Dates
          Class<?> cls = BeanUtil.getType(colName, _bean);
          if (java.sql.Timestamp.class.equals(cls)) {
            oracleDate1 = "to_date(";
            oracleDate2 = ",'yyyy-mm-dd hh24:mi:ss\".999\"')";
          } else if (java.sql.Date.class.equals(cls)) {
            oracleDate1 = "to_date(";
            oracleDate2 = ",'yyyy-mm-dd')";
          }
        }

        if (_dbVendor == SchemaUtil.MSSQL) {

          Class<?> cls = BeanUtil.getType(colName, _bean);

          // MSSQL fix for Bits/Boolean
          if (Boolean.class.equals(cls)) {

            Boolean valB = null;
            try {
              valB = (Boolean) BeanUtil.getValue(colName, _bean);
            } catch (IllegalAccessException e) {
              _logger.error(e.getMessage());
            }

            if (valB == null)
              value = null;
            else if (valB)
              value = "1";
            else
              value = "0";

          } else if (cls == boolean.class) {
            if ("true".equals(value))
              value = "1";
            else
              value = "0";
          }
        }

        // do fix here for DB2 numeric types
        String delimiter;
        try {
          if (SchemaUtil.isNumber(BeanUtil.getValue(colName, _bean))) {
            delimiter = "";
          } else
            delimiter = "'";
        } catch (IllegalAccessException e) {
          delimiter = "'";
        }

        sql.append(getDbColumn(colName)).append(" = ").append(oracleDate1).append(delimiter).append(value)
            .append(delimiter).append(oracleDate2);

        sql.append(", ");
      }

    }

    String sql2 = sql.substring(0, sql.length() - 2) + where;

    sql2 = StringUtil.fixNulls(sql2);
    return sql2;
  }

  @Override
  public final boolean delete() throws SQLException {

    connect();

    try {
      String sql = "DELETE FROM " + _entityName + createFilterStmt();
      if (_logger.isDebugEnabled())
        _logger.debug("#delete - " + sql);
      _stmt = _conn.createStatement();
      int result = _stmt.executeUpdate(sql);
      return result == 1;
    } finally {
      cleanup();
    }
  }

  @Override
  public int executeSql(String sql, Object[] p) throws SQLException {

    try {
      connect();
      _ps = _conn.prepareStatement(sql);
      setParams(p);
      return _ps.executeUpdate();
    } finally {
      cleanup();
      if (_logger.isDebugEnabled())
        _logger.debug("#executeSql - " + sql);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> executeQuery() throws SQLException {

    List<T> result = new ArrayList<>();

    if (_ps != null && _bean != null)
      _rs = _ps.executeQuery();
    else {
      throw new IllegalArgumentException("#executeQuery - result bean not found!");
    }

    while (_rs != null && _rs.next()) {
      try {
        T obj;
        if (_bean instanceof Class)
          obj = ((Class<T>) _bean).getConstructor().newInstance();
        else
          obj = (T) _bean.getClass().getConstructor().newInstance();

        setValues(_rs, obj);
        result.add(obj);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    cleanup();
    return result;

  }

  @Override
  public <T> List<T> listOf(Class<T> resultClass, String sql, Object[] p)
      throws SQLException, ReflectiveOperationException {
    connect();
    _ps = this._conn.prepareStatement(sql);
    setParams(p);
    setClass(resultClass);
    return executeQuery();
  }

  private String getEntity() {
    return _entityName;
  }

  private void setEntity(Class<?> cls) {
    if (entityMap.containsKey(cls.getName()))
      _entityName = entityMap.get(cls.getName());
    else {
      if (!cls.isAnnotationPresent(Table.class))
        throw new RuntimeException(Table.class.getName() + " annotation missing from " + cls.getName());
      _entityName = cls.getAnnotation(Table.class).value();
      entityMap.put(cls.getName(), _entityName);

    }
    _keys = null; // v202
  }

  private String getDbColumn(String prop) {

    for (Field field : BeanUtil.getFields(_bean.getClass()))
      if (field.getName().equalsIgnoreCase(prop))
        return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).value() : prop.toUpperCase();

    return prop.toUpperCase();
  }

  // initialize; setup primary keys
  private void initPK() throws SQLException {
    _keys = null;
    if (getEntity() != null)
      _keys = SchemaUtil.getPK(_conn, getEntity());
    else
      _logger.warn("Call DAO#setBean() first");
  }

  @Override
  public final int insert() throws SQLException {

    connect();
    int recordsAffected = 0;
    try {
      if (_keys == null)
        initPK();
    } catch (SQLException e) {
      _logger.warn(e.getMessage());
    }

    long result;
    try {
      String sql = createInsertStmt();
      _logger.debug("#insert - " + sql);
      _stmt = _conn.createStatement();
      boolean supportsGetGeneratedKeys = _conn.getMetaData().supportsGetGeneratedKeys();
      if (supportsGetGeneratedKeys)
        if (_dbVendor == SchemaUtil.ORACLE) {
          if (_keys != null && _keys.size() == 1) {
            String pk = _keys.iterator().next();
            result = _stmt.executeUpdate(sql, new String[] { getDbColumn(pk) });
          } else
            result = _stmt.executeUpdate(sql);
        } else
          result = _stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      else
        result = _stmt.executeUpdate(sql);

      if (result == 1 && supportsGetGeneratedKeys && _keys != null) {

        recordsAffected = 1;
        _rs = _stmt.getGeneratedKeys();
        if (_rs.next()) {

          final String pkProperty = _keys.iterator().next();

          if (_logger.isTraceEnabled())
            _logger.trace("#insert - attempting to set identity value " + pkProperty);

          Object keyVal = _rs.getObject(1);
          if (keyVal != null) {
            if (keyVal instanceof Long) {
              if ((Long) keyVal > 0)
                BeanUtil.setValue(pkProperty, _bean, keyVal);
            } else if (keyVal instanceof Integer) {
              if ((Integer) keyVal > 0)
                BeanUtil.setValue(pkProperty, _bean, keyVal);
            } else
              BeanUtil.setValue(pkProperty, _bean, keyVal);
          }

        }
      } else
        recordsAffected = result == 1 ? 1 : -1;

    } catch (SQLException e) {
      _logger.error(e.getMessage(), e);
      throw e;
    } finally {
      cleanup();
    }
    return recordsAffected;
  }

  private void cleanup() throws SQLException {

    if (_conn == null || !_conn.getAutoCommit())
      return;
    else
      _conn.close();

    if (_rs != null)
      _rs.close();

    if (_stmt != null)
      _stmt.close();

    if (_ps != null)
      _ps.close();

    if (_logger.isTraceEnabled())
      _logger.info("#cleanup executed, connection closed");

  }

  private void removeNulls(Map<String, Object> map) {

    Set<String> keysToRemove = new HashSet<>();

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

  @Override
  public int save() throws SQLException {

    connect();
    int updatedRecords = 0;
    final boolean autoCommit = _conn.getAutoCommit();
    if (autoCommit)
      _conn.setAutoCommit(false);
    try {
      // String filter = createFilterStmt();
      if (_logger.isDebugEnabled())
        _logger.debug("#save - update existing record");
      updatedRecords = update();

      if (updatedRecords != 1)
        throw new RuntimeException();

    } catch (RuntimeException e) {
      if (_logger.isDebugEnabled())
        _logger.debug("#save - insert new record");
      updatedRecords = insert();
    } finally {
      if (autoCommit && !_conn.isClosed()) {
        _conn.setAutoCommit(true);
        cleanup();
      }
    }
    return updatedRecords;
  }

  @Override
  public DAO saveNulls(boolean updateNulls) {
    _keepNullsInQuery = updateNulls;
    return this;
  }

  @Override
  public boolean select() throws SQLException {

    connect();

    try {
      boolean result;
      String sql = createSelectStmt();
      _stmt = _conn.createStatement();
      if (_logger.isDebugEnabled())
        _logger.debug("#update - " + sql);
      ResultSet resultset = _stmt.executeQuery(sql);

      if ((result = resultset.next()))
        setValues(resultset, _bean);

      return result;

    } finally {
      cleanup();
    }
  }

  @Override
  public DAO setBean(Object bean) {
    _bean = bean;
    setEntity(bean.getClass());
    return this;
  }

  private void setParams(Object[] params) throws SQLException {
    if (params != null && params.length > 0) {
      int i = 0;
      for (Object o : params) {
        if (o instanceof Integer)
          _ps.setObject(++i, (Integer) o, Types.INTEGER);
        else if (o instanceof InputStream)
          try {
            _ps.setBinaryStream(++i, (InputStream) o, ((InputStream) o).available());
          } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Error setting BinaryStream value in PreparedStatement");
          }
        else if (o instanceof Timestamp)
          _ps.setObject(++i, (Timestamp) o, Types.TIMESTAMP);
        else
          _ps.setObject(++i, o);
      }
    }
  }

  @Override
  public final int update() throws SQLException {
    connect();
    String sql = createUpdateStmt();
    try {

      _stmt = _conn.createStatement();
      if (_logger.isDebugEnabled())
        _logger.debug("#update - " + sql);
      return _stmt.executeUpdate(sql);
    } catch (SQLException sqle) {
      _logger.error(sql);
      throw sqle;
    } finally {
      cleanup();
    }
  }

}