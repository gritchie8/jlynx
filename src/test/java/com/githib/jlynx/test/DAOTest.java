package com.githib.jlynx.test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import junit.framework.TestCase;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAOTest extends TestCase {

    private PersonBean person;
    private DAO dao;


    @Override
    public void setUp() {
        dao = DAOImpl.newInstance("jdbc:hsqldb:mem:/jlynxdb", null);
        person = new PersonBean();

        try {
            String ddl = "CREATE TABLE PERSON (ID INT IDENTITY PRIMARY KEY," +
                    " DOB DATE," +
                    " ModTime TIMESTAMP," +
                    " RESUME CLOB," +
                    " IMAGE BLOB," +
                    " AGE INT," +
                    " AGE2 INT," +
                    " AMT DECIMAL(10,2) DEFAULT 0.00," +
                    " LASTNAME VARCHAR(30)," +
                    " firstname varchar(10))";
            assertFalse(dao.executeSql(ddl, null));
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


    public void test_UpdateBLOB() throws Exception {

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
                person.PersonId});
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
        conn.close();
    }

    @SuppressWarnings("unchecked")
    public void test_List() throws Exception {

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
        assertEquals(2, (int) person.PersonId);
        dao.delete();
    }

    public void test_TableAnnotation() throws SQLException {
        Person person2 = new Person();
        person2.FirstName = ("Joe");
        try {
            dao.setBean(person2).insert();
            fail();
        } catch (RuntimeException re) {
            LoggerFactory.getLogger(getName()).info(re.getMessage());
            assertNull(person2.PersonId);
        }
    }

    public void test_InsertEmptyRecord() throws SQLException {
        person = new PersonBean();
        assertNull(person.PersonId);
        dao.setBean(person).insert();
        assertNotNull(person.PersonId);
    }

    public void test_Update() throws SQLException {
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

    public void test_SetBean2() throws SQLException {
        Map<String, String> params = new HashMap<>();
        params.put("surName", "Hamilton");
        params.put("age2", "621");
        params.put("dateOfBirth", "1323412341234");
        params.put("modified", "1523412341234");
        dao.setBean(new PersonBean(), params).insert();
        assertTrue(dao.delete());
    }

    @SuppressWarnings("deprecation")
    public void test_Col2() throws SQLException {
        person.DateOfBirth = (new java.sql.Date(88, Calendar.JULY, 27));
        person.SurName = "Ritc";
        person.Age = 32;
        dao.setBean(person).insert();
        person.SurName = "Alex";
        person.WeightInKg = 70;
        dao.update();
        dao.delete();
        dao.getConnection().close();
    }

    public void test_PKAnnotation() throws SQLException {
        dao.executeSql("CREATE TABLE T (ID VARCHAR(4))", null);
        dao.setBean(new TestPOJO()).select();

    }

}
