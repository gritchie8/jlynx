package com.githib.jlynx.test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import junit.framework.TestCase;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DAOTest extends TestCase {

    private PersonBean person;
    private PersonCompany personCompany = new PersonCompany();
    private DAO dao;
    private String ddl = "CREATE TABLE PERSON (PERSONID INT IDENTITY PRIMARY KEY," +
            " DOB DATE, MODTIME TIMESTAMP," +
            " RESUME CLOB, IMAGE BLOB," +
            " LASTNAME VARCHAR(30))";


    public DAOTest() {
    }

    @Override
    public void setUp() throws SQLException, IOException {

        dao = DAOImpl.newInstance("jdbc:hsqldb:mem:/jlynxdb", null);
        dao.executeSql(ddl, null);
        LoggerFactory.getLogger(getClass()).info("Testing");

        person = new PersonBean(1);
        try {
            assertNotNull(dao.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Override
    public void tearDown() throws SQLException {
        dao.executeSql("DROP TABLE IF EXISTS PERSON", null);
    }


    public void test_CRUD() throws SQLException {
        LoggerFactory.getLogger(getClass()).info("Starting");
        person.setDateOfBirth(Date.valueOf("2000-04-14"));
        dao.setBean(person);
        assertFalse(dao.select());
        assertEquals(dao.save(), 1);
        assertTrue(dao.delete());
        person.setSurName("$#!@");
        dao.insert();


        PersonBean p2 = new PersonBean();
        dao.setBean(p2);
        Calendar cal = Calendar.getInstance();
        cal.set(1982, 7, 1);
        p2.setDateOfBirth(java.sql.Date.valueOf("1982-1-31"));
        p2.setPersonId((int) dao.save());
        assertTrue(p2.getPersonId() == 2);


        dao.executeSql("CREATE TABLE PERSON_COMPANY (PID INT, CID INT, PRIMARY KEY (PID, CID))", null);
        dao.setBean(personCompany);
        personCompany.setPersonId(person.getPersonId());
        personCompany.setCompanyId(person.getPersonId() + 100);
        dao.insert();
        dao.delete();

        // 2nd part
        person = new PersonBean(1);
        person.setSurName("&^$%@$%");
        person.setModified(new Timestamp(new java.util.Date().getTime()));
        dao.setBean(person);
        assertEquals(dao.update(), 1);

        person = new PersonBean(1);
        assertTrue(dao.setBean(person).select());
        assertNotNull(person.getSurName());
        assertNotNull(person.getModified());

        // test exception is thrown
        person = new PersonBean();
        try {
            dao.setBean(person).select();
            fail();
        } catch (Exception e) {
            assertTrue("Should be a runtime exception", e instanceof RuntimeException);
            person.setPersonId(777);
            dao.save();
        }

        // try invalid object
        try {
            dao.setBean(new Person());
            fail();
        } catch (Throwable t) {
            //t.printStackTrace();
        }

        // make sure bean is not null
        Throwable t = null;
        try {
            dao.setBean(null).select();
            fail();
        } catch (RuntimeException e) {
            t = e;
        }
        assertNotNull(t);

        // make sure insert throws an exception
        try {
            t = null;
            dao.setBean(new PersonBean(1));
            dao.insert();
            dao.insert();
            fail();
        } catch (SQLException e) {
            t = e;
        }
        assertNotNull(t);

        dao.getConnection().setReadOnly(true);
        try {
            DAOImpl.newInstance(dao.getConnection()).setBean(new PersonBean(401)).save();
            fail();
        } catch (SQLException e) {
        } finally {
            dao.getConnection().setReadOnly(false);
        }

        LoggerFactory.getLogger(getClass()).info("Done");
    }

    @SuppressWarnings("unchecked")
    public void test_List() throws SQLException, InstantiationException, IllegalAccessException {
        int i = 0;
        while (i < 25) {
            person = new PersonBean();
            person.setDateOfBirth(Date.valueOf("2010-01-01"));
            person.setSurName("P%$@#$%@# ------ " + i);
            person.setModified(new Timestamp(new java.util.Date().getTime()));
            dao.setBean(person);
            long insert = dao.insert();
            assertTrue(i == person.getPersonId());
            i++;
        }
        java.util.List<PersonBean> list = new ArrayList<PersonBean>();
        try {
            list = (List<PersonBean>) dao.getList(Person.class, "select * from person", null);
            fail();
        } catch (Throwable ex) {
            assertTrue(ex instanceof IllegalArgumentException);
            assertTrue(list.isEmpty());
        } finally {
            list = (List<PersonBean>) dao.getList(PersonBean.class, "select * from person", null);
        }

        assertTrue(list.size() > 10);
        assertTrue(true);

        for (PersonBean p : list) {
            int[] prefs = {1, 2};
            p.setPrefs(prefs);
            dao.setBean(p);
            p.setDateOfBirth(Date.valueOf("1995-06-06"));
            p.setResume("!@#$@#!");
            dao.saveNulls(true);
            dao.update();
            assertTrue(dao.delete());
        }
    }

    public void test_BLOB() throws IOException, SQLException {
        InputStream inputStream = getClass().getResourceAsStream("/Creissels_et_Viaduct_de_Millau.jpg");
        dao.setBean(person);
        dao.save();
        dao.executeSql("UPDATE PERSON SET IMAGE = ? WHERE PERSONID = ?", new Object[]{inputStream, person.getPersonId()});
        person = new PersonBean(1);
        assertNull(person.getImage());
        dao.setBean(person);
        dao.select();
        assertNotNull(person.getImage());
        InputStream is = (InputStream) person.getImage();
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        int x = (int) (10000 * Math.random());
        File file = new File("./test_" + x + ".jpg");
        OutputStream outStream = new FileOutputStream(file);
        outStream.write(buffer);
        assertTrue(file.length() > 5000000);
        assertTrue(file.delete());
    }

}
