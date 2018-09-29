package com.googlecode.jlynx;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * <p>
 * This interface defines methods to persist and retrieve POJOs from relational
 * databases systems (RDBMS), and to fetch results as Java objects. The API
 * currently has been tested on the following databases:
 * </p>
 * 
 * <ul>
 * <li>IBM DB2/UDB
 * <li>Microsoft SQL Server 2000
 * <li>Oracle 9i,10g
 * <li>MySQL
 * <li>HSQL
 * <li>PostGreSQL
 * </ul>
 * 
 * <p>
 * Requires Java 1.5 or higher as of Version 1.7
 * </p>
 * 
 * <p>
 * $Id: DAO.java 545 2011-10-23 20:41:52Z gregrmi@gmail.com $
 * </p>
 * 
 * @since v1.0
 */
public interface DAO extends Serializable {

	/**
	 * Commit database transaction (by default jLynx will auto-commit CRUD
	 * operations).
	 * 
	 * @since v1.0
	 */
	void commit();

	/**
	 * Deletes 1 row from a database.
	 * 
	 * @return boolean - Was the record deleted? Yes or No
	 * @throws java.sql.SQLException
	 *             - a database exception
	 * @since v1.0
	 */
	boolean delete() throws java.sql.SQLException;

	/**
	 * Execute query.
	 * 
	 * @param query
	 *            SQL SELECT statement or named query
	 * @param params
	 *            Parameters in SQL statement
	 * @return boolean
	 * @throws SQLException
	 *             - a database exception
	 * @see java.sql.PreparedStatement#execute()
	 * @since v1.3
	 */
	public boolean exec(String query, Object[] params) throws SQLException;

	/**
	 * Get a JSON Array of objects from database.
	 * 
	 * @param query
	 *            SQL SELECT statement or named query
	 * @param params
	 *            Statement parameters
	 * @return String - JSON
	 * @throws SQLException
	 *             - database exception
	 * @since v1.4.4
	 */
	public String fetchJSON(String query, Object[] params) throws SQLException;

	/**
	 * Returns a list of objects from the database.
	 * 
	 * @param query
	 *            named query in jlynx.yaml or any valid SQL SELECT statement
	 * @param params
	 *            SQL statement parameters (see java.sql.PreparedStatement)
	 * @return List
	 * @throws SQLException
	 *             - database exception
	 * @see java.sql.PreparedStatement#executeQuery()
	 * @since v1.3
	 */
	public List<?> fetchList(String query, Object[] params) throws SQLException;

	/**
	 * Produces a list of database objects in XML format from a SELECT
	 * statement.
	 * 
	 * @param query
	 *            named query from jlynx.yaml or any valid SQL SELECT Statement
	 * @param params
	 *            SQL statement parameters (see java.sql.PreparedStatement)
	 * @param elementName
	 *            XML element name
	 * @return String List as XML
	 * @throws SQLException
	 *             - database exception
	 * @since v1.3.4
	 */
	public String fetchXML(String query, Object[] params, String elementName)
			throws SQLException;

	/**
	 * Introspection of database and generation of Java and HTML code; places
	 * output in <code>user.home</code> folder.
	 * 
	 * @param pkg
	 * @param schema
	 */
	public void generateCode(String pkg, String schema);

	/**
	 * Inserts 1 row into a database.
	 * 
	 * @return int No. of rows inserted -- or -- ID of record inserted (MySQL
	 *         and Microsoft SQL Server only)
	 * @throws java.sql.SQLException
	 *             - database exception
	 * @since v1.0
	 */
	int insert() throws java.sql.SQLException;

	/**
	 * Rollback database transaction if autoCommit is off (by default, jLynx
	 * commits all database transactions).
	 * 
	 * @since v1.0
	 */
	void rollback();

	/**
	 * Saves POJO to database; equivalent to <code>update()</code> if record
	 * exists or <code>insert()</code> if record does not exist. POJO is then
	 * updated with current database values.
	 * 
	 * @return int
	 * @throws SQLException
	 */
	int save() throws SQLException;

	/**
	 * Whether <code>null</code> should be preserved in UPDATE and INSERT
	 * statements; by default NULL property values are removed from the
	 * statement. Empty Strings are treated as NULLs if set to true.
	 * 
	 * @param keepNullsInQuery
	 *            true (nulls are preserved) or false (default - nulls removed
	 *            from SQL queries)
	 * @since v1.0
	 */
	void saveNulls(boolean keepNullsInQuery);

	/**
	 * Select the associated record from the database and populate the POJO's
	 * values; PK value must be set first!
	 * 
	 * @return boolean if records exists returns <code>true</code>
	 * @throws java.sql.SQLException
	 * @since v1.0
	 */
	boolean select() throws java.sql.SQLException;

	/**
	 * Set the connection auto_commit property (by default auto_commit is on
	 * (true)).
	 * 
	 * @param b
	 *            boolean
	 * @return DAO
	 * @since v1.0
	 */
	DAO setAutoCommit(boolean b);

	/**
	 * Sets the object to be used in database transactions.
	 * 
	 * @since v1.7.0
	 * @param bean
	 * @return DAO
	 */
	DAO setBean(Object bean);

	/**
	 * Set the connection when not using the <code>jlynx.yaml</code> defined
	 * connection name 'default'.
	 * 
	 * @param connectionName
	 * @return DAO
	 * @throws SQLException
	 * @since v1.3.8
	 */
	DAO setConnection(String connectionName);

	/**
	 * Set the entity to be used for CRUD operations when YAML mapping is not
	 * present (@deprecated, entity names should be configured).
	 * 
	 * @param entity
	 *            Table in database
	 * @return DAO
	 * @since v1.0 R3
	 */
	@Deprecated
	DAO setEntity(String entity);

	/**
	 * Convert the underlying POJO to a JSON String.
	 * 
	 * @return String
	 * @since v1.3.6
	 */
	String toJSON();

	/**
	 * Get a JSON Array from an existing List (no database operation performed).
	 * 
	 * @param list
	 *            any List of objects
	 * @since v1.4.5
	 */
	public String toJSON(List<?> list);

	/**
	 * Convert the underlying POJO to an XML String.
	 * 
	 * @return String
	 * @since v1.3.3
	 */
	String toXml();

	/**
	 * Convert the underlying POJO to an XML String.
	 * 
	 * @param elementName
	 *            if null defaults to object name
	 * @return String
	 * @since v1.3.3
	 */
	String toXml(String elementName);

	/**
	 * Update 1 row in the database.
	 * 
	 * @return int - No. of rows affected by the update (should return 1)
	 * @throws java.sql.SQLException
	 * @since v1.0
	 */
	int update() throws java.sql.SQLException;

}
