package com.github.jlynx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

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
     * Create a <code>com.github.jlynx.DAO</code> instance with an existing Connection.
     *
     * @param connection JDBC database connection
     * @return DAO
     */
    @SuppressWarnings("unused")
    public static DAO newInstance(Connection connection) {
        DAOImpl dao = new DAOImpl();
        dao._conn = connection;
        dao._managedConnection = true;
        return dao;
    }

    /**
     * Create a <code>com.github.jlynx.DAO</code> instance with database URL and connection parameters.
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
    @SuppressWarnings("unused")
    public static DAO newInstance(String dataSourceName) {
        DAOImpl dao = new DAOImpl();
        dao._dsName = dataSourceName;
        return dao;
    }

    // pre-condition: ResultSet next() called
    private void setValues(ResultSet rs, Object object) throws SQLException {

        for (int colIndex = 1; colIndex <= rs.getMetaData().getColumnCount(); colIndex++) {

            String colName = rs.getMetaData().getColumnName(colIndex);
            Object value;
            //String propName = colName;
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
                    value = rs.getTimestamp(colIndex);
                    break;
                case Types.DATE:
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

        } // end for-loop of


    }

    private void setClass(Class<?> aClass) throws IllegalAccessException, InstantiationException {
        if (aClass.isAnnotationPresent(Table.class)) {
            Table table = aClass.getAnnotation(Table.class);
            _entityName = table.value();
            _bean = aClass.newInstance();
        } else
            throw new IllegalArgumentException(Table.class.getName() + " annotation is missing");

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
                for (Field f : BeanUtil.getFields(_bean.getClass())) {
                    if (f.isAnnotationPresent(Column.class) && f.getAnnotation(Column.class).pk())
                        _keys.add(f.getAnnotation(Column.class).value());
                }
                if (_keys.isEmpty()) {
                    _logger.error(e.getMessage());
                    throw new IllegalStateException("Primary key not on the table, or annotated correctly on class "
                            + this._bean.getClass().getName());
                }
                _logger.warn("Using column annotation on table without a PK");
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
                    String message = "Primary key value empty for database column: " + key + ", object: " + _bean.getClass().getName();
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
                sql = new StringBuffer(StringUtil.replace(sql.toString(), end,
                        ""));
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

            sql.append(oracleDate1).append(delimiter).append(StringUtil.escapeQuotes(val.toString()))
                    .append(delimiter).append(oracleDate2);

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

                sql.append(getDbColumn(colName)).append(" = ").append(oracleDate1).append(delimiter)
                        .append(value).append(delimiter).append(oracleDate2);

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
    public boolean executeSql(String sql, Object[] p) throws SQLException {

        try {
            connect();
            _ps = _conn.prepareStatement(sql);
            setParams(p);
            return _ps.execute();
        } finally {
            cleanup();
        }
    }

    private List<Object> executeQuery() throws SQLException {

        List<Object> result = new ArrayList<>();

        if (_ps != null && _bean != null)
            _rs = _ps.executeQuery();
        else {
            throw new IllegalArgumentException("#executeQuery - result bean not found!");
        }

        while (_rs != null && _rs.next()) {
            try {
                Object obj;
                if (_bean instanceof Class)
                    obj = ((Class<?>) _bean).newInstance();
                else
                    obj = _bean.getClass().newInstance();

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
    public List<?> getList(Class<?> resultClass, String sql, Object[] p)
            throws SQLException, InstantiationException, IllegalAccessException {
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
                return field.isAnnotationPresent(Column.class) ?
                        field.getAnnotation(Column.class).value() : prop.toUpperCase();

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
    public final void insert() throws SQLException {

        connect();
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
                        result = _stmt.executeUpdate(sql, new String[]{getDbColumn(pk)});
                    } else
                        result = _stmt.executeUpdate(sql);
                } else
                    result = _stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            else
                result = _stmt.executeUpdate(sql);

            if (result == 1 && supportsGetGeneratedKeys) {

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
            }

        } catch (SQLException e) {
            _logger.error(e.getMessage(), e);
            throw e;
        } finally {
            cleanup();
        }
    }

    private void cleanup() throws SQLException {

        if (_managedConnection || (_conn != null && !_conn.isClosed()))
            return;

        if (_conn != null && !_conn.getAutoCommit()) {
            _logger.warn("#cleanup not executed - auto commit is off");
            return;
        }
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

        if (_conn != null) {
            _conn.close();
            _conn = null;
        }

        if (_logger.isTraceEnabled())
            _logger.trace("#cleanup executed, connection closed");

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
    public void save() throws SQLException {

        connect();
        String filter;
        try {
            filter = createFilterStmt();
        } catch (IllegalArgumentException e) {
            _logger.trace("#save - insert new record");
            insert();
            return;
        }
        // 1. find out if object exists in DB first
        // 2. if(exists) update else insert
        StringBuilder select = new StringBuilder("SELECT ");
        select.append(getDbColumn(_keys.iterator().next()));
        select.append(" FROM ").append(getEntity()).append(filter);
        _stmt = _conn.createStatement();
        _rs = _stmt.executeQuery(select.toString());
        if (_logger.isTraceEnabled())
            _logger.trace(select.toString());
        boolean doUpdate = _rs.next();

        if (doUpdate) {
            if (_logger.isDebugEnabled())
                _logger.debug("#save - updating existing record");
            update();
        } else {
            if (_logger.isTraceEnabled())
                _logger.trace("#save - insert new record");
            insert();
        }
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

    /**
     * java.sql.Date and java.sql.Timestamp must be converted to longs (time in millis) to be set
     *
     * @param bean       POJO, with a @Table annotation
     * @param parameters Parameters key/value pairs as Strings
     * @return DAO       DAO interface
     */
    @Override
    public DAO setBean(Object bean, Map<String, String> parameters) {
        this._bean = bean;
        setEntity(bean.getClass());

        for (String key : parameters.keySet())
            BeanUtil.setValueFromString(_bean, key, parameters.get(key));

        return this;
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
                    _ps.setInt(++i, (Integer) o);
                else if (o instanceof InputStream)
                    try {
                        _ps.setBinaryStream(++i, (InputStream) o, ((InputStream) o).available());
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new SQLException("Error setting BinaryStream value in PreparedStatement");
                    }
                else
                    _ps.setObject(++i, o);
            }
        }
    }

    @Override
    public final int update() throws SQLException {
        connect();
        try {
            String sql = createUpdateStmt();
            _stmt = _conn.createStatement();
            if (_logger.isDebugEnabled())
                _logger.debug("#update - " + sql);
            return _stmt.executeUpdate(sql);
        } finally {
            cleanup();
        }
    }

}