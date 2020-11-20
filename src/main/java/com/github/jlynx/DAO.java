package com.github.jlynx;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This interface defines methods to persist and retrieve POJOs from relational
 * databases systems (RDBMS), and to fetch results as Java objects. Requires Java 1.5 or higher.
 *
 * @version 1.8.0
 * @since v1.0
 */
public interface DAO {

    /**
     * Deletes 1 row from a database.
     *
     * @return boolean - Was the record deleted? Yes or No
     * @throws java.sql.SQLException - a database exception
     * @since v1.0
     */
    boolean delete() throws SQLException;

    /**
     * Execute query.
     *
     * @param sql    SQL query
     * @param params parameters in SQL statement
     * @return boolean true if the first result is a ResultSet object; false if the first result is an update count or there is no result
     * @throws SQLException database exception
     * @see java.sql.PreparedStatement#execute()
     * @since v1.3
     */
    @SuppressWarnings("unused")
    boolean executeSql(String sql, Object[] params) throws SQLException;

    /**
     * Returns a list of objects from the database.
     *
     * @param resultClass Class literal to store results
     * @param query       SQL statement
     * @param params      SQL statement parameters (see java.sql.PreparedStatement)
     * @return List
     * @throws SQLException           - database exception
     * @throws IllegalAccessException - exception
     * @throws InstantiationException - exception
     * @see java.sql.PreparedStatement#executeQuery()
     * @since v1.3
     */
    List<?> getList(Class<?> resultClass, String query, Object[] params)
            throws SQLException, IllegalAccessException, InstantiationException;


    /**
     * Inserts single row into a database.
     *
     * @throws SQLException - database exception
     * @since v1.0
     */
    void insert() throws SQLException;

    /**
     * Saves POJO to database; equivalent to <code>update()</code> if record
     * exists or <code>insert()</code> if record does not exist. POJO is then
     * updated with current database values.
     *
     * @throws SQLException - database exception
     */
    void save() throws SQLException;

    /**
     * Whether <code>null</code> should be preserved in UPDATE and INSERT
     * statements; by default NULL property values are removed from the
     * statement. Empty Strings are treated as NULLs if set to true.
     *
     * @param keepNullsInQuery true (nulls are preserved) or false (default - nulls removed
     *                         from SQL queries)
     * @return DAO
     * @since v1.0
     */
    @SuppressWarnings("unused")
    DAO saveNulls(boolean keepNullsInQuery);

    /**
     * Select the associated record from the database and populate the POJO's
     * values; PK value must be set first!
     *
     * @return boolean if records exists returns <code>true</code>
     * @throws java.sql.SQLException database exception
     * @since v1.0
     */
    boolean select() throws java.sql.SQLException;

    /**
     * Sets the object to be used in database transactions.
     * <p>
     * Object MUST have a com.github.jlynx.Table annotation. (v1.8+)
     *
     * @param bean POJO, with a @Table annotation
     * @return DAO
     * @since v1.7.0
     */
    DAO setBean(Object bean);

    /**
     * Sets the object to be used in database transactions.
     * <p>
     * Parameters will be set on the properties or at least attempted.
     * <p>
     * Object MUST have a @Table annotation.
     *
     * @param bean       POJO, with a @Table annotation
     * @param parameters Parameters key/value pairs as Strings
     * @return DAO
     * @since v1.9.0
     */
    DAO setBean(Object bean, Map<String, String> parameters);

    /**
     * The current connection.
     *
     * @return Connection - jdbc connection
     * @throws SQLException - database exception
     * @since v1.7.2
     */
    java.sql.Connection getConnection() throws SQLException;

    /**
     * Update 1 row in the database.
     *
     * @return int - No. of rows affected by the update (should return 1)
     * @throws java.sql.SQLException database exception
     * @since v1.0
     */
    @SuppressWarnings("unused")
    int update() throws java.sql.SQLException;

}
