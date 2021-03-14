jLynx RELEASE NOTES
-------------------

1. Project hosted @ GitHub (2018).
2. jLynx has 1 runtime dependency... SLF4J (logging).
3. jLynx requires JDK 11 and higher (Dec-2020).

jLynx CHANGE LOG
----------------

##### v2.1.0 (Feb-2020)

    - tweaked getList() to make use of generic & avoid unchecked warnings at compile time
    - java.time.LocalDate/LocalDateTime prioritzed over java.sql.Date/java.sql.Timestamp

###### v2.0.2 (Dec-2020)

    - more bug fixes
    - connection pool connections not closed
    - changes to support Kotlin data classes
    - fields can be private, setters/getters are not used
    - JDK 11 features used
    - save() enhanced
    - save(), insert() return number of rows affected instead of no return

###### v2.0.1 (Nov-2020)

    - bug fixes

###### v2.0.0 (Aug-2020)

    - Fields now instead of getter/setter properties, e.g. public Integer id
    - cleaned up code, removed warnings, etc.
    - source compatibility set to JDK 1.8
    - insert/save method return types are now void; primary keys auto set on 'identity' inserts
    - overloaded setBean method; added a setBean to accept a hash
    - @Column now has option to exclude from database interactions, default is include=true
    - logging is now slf4j (again)

###### v1.8.1 (Mar-2020)

    - added postgresql tests
    - tweaked inserts to support generated keys (removed mysql specific code)
    - insert() will not fail with a table that has no PK

###### v1.8.0 (Apr-2019)

    - fairly drastic cleanup and overhaul
    - removed YAML configuration
    - renamed package to com.github.jlynx
    - added annotations for tables and columns, @Table is required
    - @Column is optional for the property, is necessary when Column and property mismatch

    @Table("T_PERSON")
    public class Person {
      @Column("BIRTHDAY")
      private Date dob;

    - simplified DAO interface removing XML and JSON functionality as there are better marshallers these days
    - added getConnection() so developer can manipulate Connection object directly
    - logging changes

###### v1.7.1 (Oct-2011)

    - improved logging for better performance

###### v1.7.0 (Dec-2010)

    - switched to Java 1.5 (previous versions are 1.3 compatible)
    - switched to Java logging (replaces SLF4J)
    - changed package name from net.sf -> com.github
    - added constructor to DAOImpl, signature permits no configuration usage of jlynx (i.e. no YAML file)
    - jlynx.yaml is optional now (see above)
    - added setBean() method

###### v1.6.3 (Sep-08)

    - removed 'net.sf.jlynx.ajax' package (recommend Spring MVC if you were using this)
    - switched to IntelliJ IDEA (thanks JetBrains!)
    - switched build system to Ant+Ivy
    - jlynx.yaml resource can be now be set in file system using System property 'jlynx.yaml.file' or in classpath at META-INF/jlynx.yaml (default)
    - updated SLF4J to ###### v1.5.3
    - last version available via Maven package manager

###### v1.6.2 (Sep-08)

    - several more JDK 1.3 issues corrected
    - adjusted escape character & sequencing in BeanUtils#toJSON
    - could not deploy to public Maven2 repo

###### v1.6.1 (Sep-08)

    - adjusted primary key initialization to address DERBY bug
    - fixed source code compatibility issue that affected JVM's 1.3/1.4 (goal remains to keep jLynx JDK 1.3 compatible)

###### v1.6.0 (Aug-08)

    - renamed interface and implementation to DAO and DAOImpl
    - removed factory, use new keyword to create DAO instance (i.e. DAO dao = new DAOImpl();)
    - CRUD operations on Maps no longer supported
    - renamed List fetching methods to make it more intuitive
    - added column name mappings to jlynx.yaml for cases when POJO fields do not match column names in database

###### v1.5.2 (Jul-08)

	- fixed bug in YAML config (parsing failed when named-queries was not present)
	- removed XML config option
	- removed sample webapp from zip download on Google (see Wiki for usage and examples)

###### v1.5.1 (Jul-08)

	- added YAML configuration (jlynx.xml is deprecated and will be removed next release!)
	- place jlynx.yaml in META-INF (see project page for sample)
	- updated to most recent release of SLF4J

###### v1.5.0 (May-08)

	- reverted back to JDK 1.3 and SLF4J logging
	- added String[], int[] and Integer[] setter support for POJOs
	- added net.sf.jlynx.ajax package
	- revised sample webapp to use above-noted package
	- added attributes in jlynx.xml for date and timestamp pattern conversion
	- revised DTD is: <!DOCTYPE jlynx PUBLIC "-//TOPMIND//DTD JLYNX 1.5/EN" "http://www.topmind.biz/jlynx/jlynx-1.5.dtd">

###### v1.4.5b (Mar-08)

	- adjusted json output in #jsonArray (removed a JS comment)

###### v1.4.5 (Mar-08)

	- added #jsonArray(List)
	- fixed script tag in sample webapp on index.html

###### v1.4.4 (Jan-08)

	- requires JDK 1.5+ !!!
	- restored jlynx.xml connection configuration and class mappings
	- #setConnection still accepts a Connection object or JNDI datasource, plus now it accepts a named connection
	- if you don't use #setConnection, the named connection 'default' is expected
	- jlynx.xml DTD is now:

	<!DOCTYPE jlynx PUBLIC "-//TOPMIND//DTD JLYNX 1.4/EN" "http://www.topmind.biz/jlynx/jlynx-1.4.4.dtd">

	- fixed toJSON bug (escaped newlines and Strings)
	- improved webapp with heavy use of AJAX/JSON and no JSP
	- add JSON Array method to Relational interface #jsonArray
	- added Blob/Clob select/update support (not super well tested at this point)
	- removed SLF4J dependency in favor of JDK logging
	- POJO java.util.List fields are now skipped

###### v1.4.3 (Sep-07)

	- fixed bug affecting CRUD operations tables with composite primary keys
	- bug appears as a result of ###### v1.4.2 changes

###### v1.4.2 (Aug-07)

	- adjusted / improved save() method by adding a test select query

###### v1.4.1 (Jul-07)

	- bug fix in setConnection(); db server type was not set properly
	- packaging improved for webapp (jLynx Servlet maven project)

###### v1.4.0(b) (Jul-07)

	- improved webapp; introduced an extendable servlet that performs CRUD operations based on HTML input name

###### v1.4.0 (Jul-07)

	- extensive changes to Relational interface and re-factoring of entire implemention
	- jlynx.xml now has query definitions only!
	- connection configuration is now entirely removed from jlynx
	- setConnection accepts an open java.sql.Connection object or a String representing a JNDI DataSource
	- project builds switched from Ant to Maven 2
	- jLynx DTD ###### v1.4 is at http://www.topmind.biz/jlynx/1.4/jlynx.dtd
	- improved webapp and code generation

###### v1.3.8 (Jun-07)

    - added Relational.setConnection(); now you can use jlynx without an XML config file
    - changed Relational interface for setter methods to promote more friendly syntax
        i.e. void setEntity(String e) is now: Relational setEntity(String e)
    - fixed open connection in ConfigParser initializer
    - added Column annotation for JPA bean generation

###### v1.3.7 (Jun-07)

    - added JPA bean generation to code generator (you asked for it)

###### v1.3.6 (Apr-07)

    - fixed bug affecting code generator (detected on SQL Server)
    - added toJSON output method

###### v1.3.5 (Apr-07)

    - fixed AJAX bug in sample web app
    - fixed code generator bug affecting PostgreSQL
    - moved source code from SourceForge to Google

###### v1.3.4 (Mar-07)

    - fixed bug in RelationalFactory affecting entity name auto-mapping to class name
    - add overloaded getResultList method the returns an XML-format String instead of List
    - created junit test class to improve testing of future builds
    - switched to in-memory version of hsql database (for testing)
    - NEW sample web app!

###### v1.3.3 (Mar-07)

    - added toXml() methods to Relational interface and impl
    - changed getResultList() to accept SQL statement that is not configured in jlynx.xml
    - no bug fixes

v.1.3.2 (Feb-07)

    - updated ConfigParser, created protected constructor to replace static init method
    - RelationalFactory throws IllegalArgumentException if namedConnection not present
    - optimized RelationalFactory#getInstance methods
    - simplified how jlynx.xml is discovered, now must be in META-INF i.e. META-INF/jlynx.xml
    - added ConfigParser#getConfigFile()
    - the above changes have yielded performance benefits
    - no bug fixes or API changes

v.1.3.1 (Jan-06)

    - fixed bug affecting case-sensitive table names
    - improved Generator for JSPs
    - improved sample webapp

v.1.3.0 (Dec-06)

    - removed Relational.getPreparedStatement()
    - removed Relational.execute(); replaced with Relational.executeNamedQuery()
    - removed Relational.executeQuery; replaced with Relational.getResultList()
    - no bug fixes

###### v1.2.1 (Nov-06)

    - fixed bug affecting persistence of POJOs with a 'is' getter
    - improved sample web application 

###### v1.2.0 (Oct-06)

    - renamed package biz.topmind.jlynx to net.sf.jlynx
    - removed SqlRunner
    - added static methods to ConfigParser
    - adjusted logging to print better debug information
    - switched SQL Server identity retrieval function from @@IDENTITY to SCOPE_IDENTITY()

###### v1.1 R1 (Sep-06)

    - refactoring to remove all external dependencies from jLynx
    - replaced Jakarta Commons Logging (JCL) w/ Simple Logging Facade for Java (SLF4J)
    - improved robustness of bean introspection
    - removed Bean-to-XML methods from API
    - renamed Relational#preserveNulls to Relational#saveNulls
    - empty Strings are now saved as NULL when saveNulls(true) is invoked
    - fixed bug affecting Map persistence
    - fixed bug redo init() within setEntity()
    - switched license from GPL v2 to BSD-style
    - jlynx.xml: moved from inline DTD to online DTD (http://www.topmind.biz/jlynx/jlynx.dtd)

###### v1.0 R3 (Apr-06)

    - combined sample webapp and jlynx-dev into one project
    - adjusted download format (see above)

###### ###### v1.0 R2 (Mar-06)

    - numerous bug fixes, especially related to jlynx.xml parsing
    - added support for Struts DynaActionForms
    - created sample web application using HSQL DB (removed sample pkg)
    - added SQLRunner class and moved non-creational methods out of RelationalFactory
    - removed Swing GUI generator... replaced with web application

###### ###### ###### v1.0 R1 (Dec-05)

    - moved Relational interface to jlynx pkg
    - added com.topmindsystems.jlynx.sample pkg
    - improved JSP code generation
    - improved bean reflection capabilities
    - added mulitple connection capability
    - improved configuration and added inline DTD
    - added XML output method (bean-to-XML)
    - added prepared statements and named queries in XML
    - removed refresh method from Relational interface and impl
    - fixed namedQuery xml parsing bug (12.21.05 -- DefaultConfig.java)
    - updated JavaDoc (12.28.05)
    - renamed packages to biz.topmind.jlynx (12.29.05)
    - fixed 'jlynx.xml' SAX parser normalization issue (01.19.06)

###### ###### v1.0 (Aug-05)

    - added JSP code generation
    - provided web start capability on public web site

###### earlier release notes unavailable
