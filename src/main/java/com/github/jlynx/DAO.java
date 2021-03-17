package com.github.jlynx;

import java.sql.SQLException;
import java.util.List;

/**
 * This interface defines methods to persist and retrieve POJOs from relational
 * databases systems (RDBMS), and to fetch results as Java objects. Requires
 * Java 1.5 or higher.
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
   * @return returns number of rows affected
   * @throws SQLException database exception
   * @see java.sql.PreparedStatement#executeUpdate()
   * @since v1.3
   */
  int executeSql(String sql, Object[] params) throws SQLException;

  /**
   * Returns a list of objects from the database.
   *
   * @param resultClass class literal to store results
   * @param query       SQL statement
   * @param params      SQL statement parameters (see java.sql.PreparedStatement)
   * @return List
   * @throws ReflectiveOperationException Common superclass of exceptions thrown
   *                                      by reflective operations in core
   *                                      reflection.
   * @throws SQLException                 database exception
   * @see java.sql.PreparedStatement#executeQuery()
   * @since v1.3
   */
  <T> List<T> listOf(Class<T> resultClass, String query, Object[] params)
      throws SQLException, ReflectiveOperationException;

  /**
   * Inserts single row into a database.
   *
   * @return - number of rows added, should be 1
   * @throws SQLException - database exception
   * @since v1.0
   */
  int insert() throws SQLException;

  /**
   * Saves POJO to database; equivalent to <code>update()</code> if record exists
   * or <code>insert()</code> if record does not exist. POJO is then updated with
   * current database values.
   *
   * @return - number of rows affected, should be 1
   * @throws SQLException - database exception
   */
  int save() throws SQLException;

  /**
   * Whether <code>null</code> should be preserved in UPDATE and INSERT
   * statements; by default NULL property values are removed from the statement.
   * Empty Strings are treated as NULLs if set to true.
   *
   * @param keepNullsInQuery true (nulls are preserved) or false (default - nulls
   *                         removed from SQL queries)
   * @return DAO
   * @since v1.0
   */

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
   * 
   * Object MUST have a com.github.jlynx.Table annotation. (v1.8+)
   *
   * @param bean POJO, with a @Table annotation
   * @return DAO
   * @since v1.7.0
   */
  DAO setBean(Object bean);

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
  int update() throws java.sql.SQLException;

}
