package com.github.jlynx;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of DAO interface.
 *
 * @see com.github.jlynx.DAO
 */
public class DAOImpl implements DAO {

    static final Map<String, Object> connMap = new TreeMap<String, Object>();
    private static final Map<String, String> entityMap = new TreeMap<String, String>();
    private Logger _logger;
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
    private Map<String, String> _colMap = null;
    private String _cnUrl;
    //private String _driver;

    private DAOImpl() {
        try {
            _logger = Logger.getLogger(getClass().getName(), "jlynx-logging");
        } catch (MissingResourceException t) {
            _logger = Logger.getLogger(getClass().getName());
            _logger.severe(t.getMessage());
        }
        _logger.log(Level.INFO, "Created");
    }

    /**
     * Create a <code>com.github.jlynx.DAO</code> instance.
     *
     * @param databaseUrl     JDBC database connection String
     * @param connectionProps JDBC properties
     * @throws SQLException - database exception
     */
    public static DAO newInstance(String databaseUrl, Properties connectionProps) {
        DAOImpl dao = new DAOImpl();
        dao._cnUrl = databaseUrl;
        dao._cnProps = connectionProps;
        return dao;
    }

    /**
     * Create a <code>com.github.jlynx.DAO</code> instance.
     *
     * @param dataSourceName JNDI
     * @return
     */
    public static DAO newInstance(String dataSourceName) {
        DAOImpl dao = new DAOImpl();
        dao._dsName = dataSourceName;
        return dao;
    }

    // pre-condition: ResultSet next() called
    private void setValues(ResultSet rs, Object object) throws Exception {

        try {

            int colCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= colCount; i++) {

                String colName = rs.getMetaData().getColumnName(i);
                Object value;
                String propName = getProp(colName);
                int type = rs.getMetaData().getColumnType(i);
                if (type == Types.BLOB || type == Types.BINARY || type == Types.LONGVARBINARY) {
                    value = rs.getBlob(i);
                    if (value != null)
                        value = ((Blob) value).getBinaryStream();

                } else if (type == Types.VARCHAR || type == Types.CLOB || type == Types.LONGVARCHAR) {
                    value = rs.getString(i);
                } else
                    value = rs.getObject(i);

                if (!entityMap.containsKey(object.getClass().getName()) && object.getClass().isAnnotationPresent(Table.class))
                    entityMap.put(object.getClass().getCanonicalName(), object.getClass().getAnnotation(Table.class).value());

                for (Field field : object.getClass().getDeclaredFields())
                    if (field.isAnnotationPresent(Column.class) && colName.equalsIgnoreCase(field.getAnnotation(Column.class).value())) {
                        propName = field.getName(); //todo - cache?
                        break;
                    }

                BeanUtils.setValue(propName, object, value);

            }// end for

        } catch (SQLException e) {
            throw e;
        }
    }

    private void setClass(Class<?> aClass) throws IllegalAccessException, InstantiationException {
        if (aClass.isAnnotationPresent(Table.class)) {
            Table table = (Table) aClass.getAnnotation(Table.class);
            _entityName = table.value();
            _bean = aClass.newInstance();
        } else
            throw new IllegalArgumentException(Table.class.getName() + " annotation is missing");

    }

    public Connection getConnection() throws SQLException {
        connect();
        return _conn;
    }

    private void connect() throws SQLException {
        if (_conn == null || _conn.isClosed()) {
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
                throw new SQLException("Primary Key value is missing! col=" + key + " " + obj + " prop=" + prop + "");

            String delimiter = DataTypeMappings.isNumber(partKeyValue) ? "" : "'";

            sql.append(" = ").append(delimiter).append(partKeyValue.toString()).append(delimiter);
        }

        return " WHERE " + sql.substring(and.length());
    }

    private String createInsertStmt() {

        StringBuffer sql = new StringBuffer();

        Map<String, Object> props = BeanUtils.describe(_bean);
        String delimiter;
        Object obj = _bean;

        // TODO remove transient properties from this object
        removeNulls(props);

        String[] fields = new String[props.size()];
        int j = 0;
        for (String prop : props.keySet()) {
            fields[j++] = prop;

            // handle Column annotation
            String column = null;
            for (Field field : _bean.getClass().getDeclaredFields())
                if (field.getName().equalsIgnoreCase(prop) && field.isAnnotationPresent(Column.class)) {
                    column = field.getAnnotation(Column.class).value();
                    break;
                }

            sql.append(getDbColumn(column == null ? prop : column));
            if (j < fields.length)
                sql.append(",");
            else {
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
                    val = valB ? "1" : "0";

                } else if (cls == boolean.class) {

                    if ("true".equals(val.toString()))
                        val = "1";
                    else
                        val = "0";
                }

            }

            Object field = BeanUtils.getValue(fields[j], obj);

            delimiter = (DataTypeMappings.isNumber(field)) ? "" : "'";

            j++;

            sql.append(oracleDate1).append(delimiter).append(StringUtils.escapeQuotes(val.toString()))
                    .append(delimiter).append(oracleDate2);

            if (i.hasNext())
                sql.append(",");
            else
                sql.append(")");
        }

        String result = "INSERT INTO " + _entityName + " (" + StringUtils.fixNulls(sql.toString());
        _logger.info(result);
        return result;
    }

    private String createSelectStmt() throws NoSuchMethodException, SQLException {
        return "SELECT * FROM " + _entityName + createFilterStmt();
    }

    private String createUpdateStmt() throws Exception {

        String delimiter;

        StringBuffer sql = new StringBuffer("UPDATE ").append(_entityName).append(" SET ");

        String where = createFilterStmt();

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
                        oracleDate1 = "to_date(";
                        oracleDate2 = ",'yyyy-mm-dd hh24:mi:ss\".999\"')";

                    }
                }

                if (_dbVendor == SchemaUtil.MSSQL) {

                    Class<?> cls = BeanUtils.getType(colName, _bean);

                    // MSSQL fix for Bits/Boolean
                    if (Boolean.class.equals(cls)) {

                        Boolean valB = (Boolean) BeanUtils.getValue(colName, _bean);

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
                if (DataTypeMappings.isNumber(BeanUtils
                        .getValue(colName, _bean))) {
                    delimiter = "";
                } else
                    delimiter = "'";

                sql.append(getDbColumn(colName)).append("=").append(oracleDate1).append(delimiter)
                        .append(value).append(delimiter).append(oracleDate2);

                sql.append(", ");
            }

        }

        String sql2 = sql.toString().substring(0, sql.length() - 2) + where;

        sql2 = StringUtils.fixNulls(sql2);
        return sql2;
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
            e.printStackTrace();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else
                throw new SQLException(e.getMessage());
        } finally {
            cleanup();
        }
    }

    public boolean executeSql(String sql, Object[] p) throws SQLException {

        boolean result;
        connect();

        _ps = _conn.prepareStatement(sql);
        setParams(p);
        result = _ps.execute();
        if (_conn.getAutoCommit())
            cleanup();

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

                setValues(_rs, obj);
                result.add(obj);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        cleanup();
        return result;

    }

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
            _entityName = cls.getAnnotation(Table.class).value();
            entityMap.put(cls.getName(), _entityName);
        }
    }

    private String getDbColumn(String prop) {

        String prop2 = "p:" + prop.toLowerCase();

        // handle Column annotation; here new to 1.8.0 todo cache?
        for (Field field : _bean.getClass().getDeclaredFields())
            if (field.getName().equalsIgnoreCase(prop) && field.isAnnotationPresent(Column.class))
                return field.getAnnotation(Column.class).value();


        return _colMap != null && _colMap.containsKey(prop2) ? _colMap.get(prop2) : prop;
    }

    private String getProp(String col) {

        String col2 = "c:" + col.toLowerCase();
        return _colMap != null && _colMap.containsKey(col2) ? _colMap.get(col2) : col;
    }

    // initialize; setup primary keys
    private void initPK() throws SQLException {
        _keys = null;
        if (_conn != null && getEntity() != null) {
            try {
                _keys = PK.getPK(_conn, getEntity());
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public final int insert() throws SQLException {

        connect();
        if (_keys == null)
            initPK();

        int result;
        try {
            String sql = createInsertStmt();
            _stmt = _conn.createStatement();
            result = _stmt.executeUpdate(sql);
            if (_dbVendor == SchemaUtil.MSSQL && result == 1) {
                try {
                    int ident = 1;
                    String identitySql = "SELECT SCOPE_IDENTITY()";

                    _rs = _stmt.executeQuery(identitySql);
                    if (_rs.next())
                        ident = _rs.getInt(1);

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

                try {
                    result = Integer.parseInt(ident);
                    if (result == 0)
                        result = 1;
                } catch (NumberFormatException e) {
                    //result = 1;
                }
            }

        } catch (SQLException e) {
            //_logger.severe(e.getMessage());
            throw e;
        } finally {
            cleanup();
        }
        return result;
    }

    private void cleanup() throws SQLException {
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
    }

    private void removeNulls(Map<String, Object> map) {

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

    public int save() throws SQLException {

        connect();
        int result = 0;

        if (!_conn.getAutoCommit()) {
            throw new SQLException(
                    "save() not supported in transaction, use insert() or update()!");
        } else {
            String where;
            try {
                where = createFilterStmt();
            } catch (Exception e1) {
                e1.printStackTrace();
                result = insert();
                return result;
            }

            //setAutoCommit(false);
            try {

                // 1. find out if object exists in DB first
                // 2. if(exists) update else insert

                StringBuffer select = new StringBuffer("SELECT ");
                select.append(getDbColumn(_keys.iterator().next()));
                select.append(" FROM ").append(getEntity()).append(where);

                _stmt = _conn.createStatement();
                ResultSet resultset = _stmt.executeQuery(select.toString());
                boolean doUpdate = resultset.next();
                resultset.close();

                if (doUpdate)
                    result = update();
                else
                    result = insert();

            } catch (Exception e) {
                //_logger.severe(e.getMessage());
                throw new SQLException(e.getMessage());

            } finally {
                cleanup();
            }

        }

        return result;
    }

    public void saveNulls(boolean updateNulls) {
        _keepNullsInQuery = updateNulls;
    }

    public boolean select() throws SQLException {

        connect();

        try {
            boolean result;
            String sql = createSelectStmt();

            _stmt = _conn.createStatement();
            ResultSet resultset = _stmt.executeQuery(sql);

            if ((result = resultset.next()))
                setValues(resultset, _bean);

            return result;

        } catch (Exception e) {

            e.printStackTrace();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else
                throw new SQLException(e.getMessage());

        } finally {

            cleanup();

        }
    }

    public DAO setBean(Object bean) {
        _bean = bean;
        setEntity(bean.getClass());
        try {
            initPK();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public final int update() throws SQLException {
        connect();
        try {
            int result;
            String sql = createUpdateStmt();
            _stmt = _conn.createStatement();
            result = _stmt.executeUpdate(sql);
            return result;
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else
                throw new SQLException(e.getMessage());
        } finally {
            cleanup();
        }
    }

}