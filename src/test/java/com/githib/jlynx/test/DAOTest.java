package com.githib.jlynx.test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import data.Contact2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.Assert.*;

public class DAOTest {

    private PersonBean person;
    private static DAO dao;

    @Before
    public void beforeTestMethod() {

        if (dao == null)
            try {
                LoggerFactory.getLogger("jlynx").info("Creating new DAO");
                HikariConfig config = new HikariConfig();
                Properties dsProps = new Properties();
                dsProps.put("url", "jdbc:hsqldb:mem:jlynx_db");
                config.setDataSourceProperties(dsProps);
                config.setUsername("");
                config.setPassword("");
                config.setDataSourceClassName("org.hsqldb.jdbc.JDBCDataSource");
                HikariDataSource ds = new HikariDataSource(config);
                dao = DAOImpl.newInstance(ds.getConnection());
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                fail();
            }
        else {
            try {
                assertNotNull(dao.getConnection());
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                fail();
            }
        }

        person = new PersonBean();

        try {
            String ddl = "CREATE TABLE PERSON (" +
                    " ID INT GENERATED BY DEFAULT AS IDENTITY (START WITH 1001, INCREMENT BY 1) PRIMARY KEY," +
                    " DOB DATE," +
                    " ModTime TIMESTAMP," +
                    " RESUME CLOB," +
                    " IMAGE BLOB," +
                    " AGE INT," +
                    " AGE2 INT," +
                    " AMT DECIMAL(10,2) DEFAULT 0.00," +
                    " LASTNAME VARCHAR(30)," +
                    " firstname varchar(10))";

            assertNotNull(dao.getConnection());
            dao.getConnection().setAutoCommit(false);
            assertFalse(dao.executeSql(ddl, null));

        } catch (SQLException e) {
            LoggerFactory.getLogger("jlynx").warn(e.getMessage());
        }
    }


    @After
    public void afterTestMethod() {
        LoggerFactory.getLogger("jlynx").info("#afterTestMethod - start");
        if (dao != null)
            try {
                assertNotNull(dao.getConnection());
                dao.executeSql("DROP TABLE IF EXISTS PERSON", null);
                // dao.getConnection().close();
            } catch (SQLException exception) {
                exception.printStackTrace();
                fail();
            } finally {
                // dao = null;
                LoggerFactory.getLogger("jlynx").info("#afterTestMethod - done");
            }
    }

    @Test
    public void updateBLOB() throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("/Creissels_et_Viaduct_de_Millau.jpg");
        person.SurName = "Smith";
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.APRIL, 14);
        person.DateOfBirth = (new Date(cal.getTimeInMillis()));
        person.Modified = (new Timestamp(new java.util.Date().getTime()));
        person.FirstName = "Al";
        Connection conn = dao.getConnection();

        try {
            conn.setAutoCommit(false);
            assertEquals(1, dao.setBean(person).update());
            fail();
        } catch (Throwable t) {
            dao.insert();
        }

        dao.executeSql("UPDATE PERSON SET IMAGE = ? WHERE ID = ?", new Object[]{inputStream,
                person.getPersonId()});
        conn.commit();
        assertNull(person.Image);
        assertTrue(dao.select());

        assertNotNull(person.Image);
        InputStream is = (InputStream) person.Image;
        byte[] buffer = new byte[is.available()];
        assertTrue(is.read(buffer) > 8000);
        int x = (int) (10000 * Math.random());
        File file = new File("./test_" + x + ".jpg");
        OutputStream outStream = new FileOutputStream(file);
        outStream.write(buffer);
        assertTrue(file.length() > 5000000);
        assertTrue(file.delete());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void list() throws Throwable {

        dao.setBean(person).save();

        person = new PersonBean();
        dao.setBean(person).save();


        List<PersonBean> list = (List<PersonBean>) dao.getList(PersonBean.class, "SELECT * FROM person", null);
        assertEquals(list.size(), 2);

        dao.setBean(list.get(0)).delete();
        dao.setBean(list.get(1)).delete();

        list = (List<PersonBean>) dao.getList(PersonBean.class, "SELECT * FROM person", null);
        assertTrue(list.isEmpty());
        person = new PersonBean();
        person.FirstName = "Taylor";
        dao.setBean(person).insert();
        assertTrue(person.getPersonId() > 1000);
        dao.delete();
    }

    @Test
    public void tableAnnotation() throws SQLException {
        Person person2 = new Person();
        person2.FirstName = ("Joe");
        try {
            dao.setBean(person2).insert();
            fail();
        } catch (RuntimeException re) {
            LoggerFactory.getLogger(getClass()).info(re.getMessage());
            assertNull(person2.getPersonId());
        }
    }

    @Test
    public void insertEmptyRecord() throws SQLException {
        person = new PersonBean();
        assertNull(person.getPersonId());
        dao.setBean(person).insert();
        assertNotNull(person.getPersonId());
    }

    @Test
    public void update() throws SQLException {
        person = new PersonBean();
        dao.setBean(person).insert();
        person.FirstName = ("Lewis");
        person.age2 = 34;
        assertEquals(1, dao.update());
        assertNull(person.Amt);
        dao.select();
        assertNotNull(person.Amt);
        LoggerFactory.getLogger(DAOTest.class).info("Amt = " + person.Amt);
    }

    @Test
    public void setBean2() throws SQLException {
        Map<String, String> params = new HashMap<>();
        params.put("surName", "Hamilton");
        params.put("age2", "621");
        params.put("dateOfBirth", "1323412341234");
        params.put("modified", "1523412341234");
        dao.setBean(new PersonBean(), params).insert();
        assertTrue(dao.delete());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_Col2() throws SQLException {
        person.DateOfBirth = (new java.sql.Date(88, Calendar.JULY, 27));
        person.SurName = "Ritc";
        person.Age = 32;
        dao.setBean(person).insert();
        person.SurName = "Alex";
        person.WeightInKg = 70;
        dao.update();
        dao.delete();
    }

    @Test
    public void primaryKeyAnnotation() throws SQLException {
        dao.executeSql("CREATE TABLE T (ID VARCHAR(4))", null);
        dao.setBean(new TestPOJO()).select();
    }

    @Test
    public void postgre() {
        dao = DAOImpl.newInstance("jdbc:postgresql:gregoryritchie", null);
        Contact contact = new Contact();
        //contact.id = 1;
        dao.setBean(contact);
        contact.lastname = "Ritc";
        try {
            dao.insert();
            assertTrue(dao.select());
            assertTrue(contact.lastname.length() > 2);
            assertTrue(contact.active);
            assertTrue(contact.getId() > 0);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            fail();
        }

        Contact2 c2 = new Contact2();
        assertNull(c2.getId());
        c2.setSurName("Smith");
        dao.setBean(c2);
        try {
            dao.insert();
            assertTrue(c2.getId() > 0);
            assertTrue(dao.select());
            assertNotNull(c2.getCreated());
            assertTrue(c2.getActive());
            assertNotNull(c2.getSurName());
            dao.getConnection().close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            fail();
        } finally {
            dao = null;
        }
    }

}
