package com.githib.jlynx.test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import junit.framework.TestCase;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    private final String ddl = "CREATE TABLE PERSON (ID INT IDENTITY PRIMARY KEY," +
            " DOB DATE," +
            " MODTIME TIMESTAMP," +
            " RESUME CLOB," +
            " IMAGE BLOB," +
            " AGE INT," +
            " LASTNAME VARCHAR(30)," +
            " firstname varchar(10))";


    @Override
    public void setUp() throws SQLException {
        dao = DAOImpl.newInstance("jdbc:hsqldb:mem:/jlynxdb", null);
        person = new PersonBean();

        try {
            dao.executeSql(ddl, null);
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
        person.setSurName("Smith");
        Calendar cal = Calendar.getInstance();
        cal.set(2000, 3, 14);
        person.setDateOfBirth(new Date(cal.getTimeInMillis()));
        person.setModified(new Timestamp(new java.util.Date().getTime()));
        person.setFirstName("Al");

        try {
            dao.setBean(person).update();
            fail();
        } catch (Throwable t) {
            dao.insert();
        }

        dao.executeSql("UPDATE PERSON SET IMAGE = ? WHERE ID = ?", new Object[]{inputStream, person.getPersonId()});

        assertNull(person.getImage());
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
        person.setFirstName("Taylor");
        dao.setBean(person).insert();
        assertEquals(2, (int) person.getPersonId());
        dao.delete();
    }

    public void test_TableAnnotation() throws SQLException {
        Person person2 = new Person();
        person2.setFirstName("Joe");
        try {
            dao.setBean(person2).insert();
            fail();
        } catch (RuntimeException re) {
            LoggerFactory.getLogger(getName()).info(re.getMessage());
            assertTrue(person2.getPersonId() == null);
        }
    }

    public void test_InsertEmptyRecord() throws SQLException {
        person = new PersonBean();
        assertNull(person.getPersonId());
        dao.setBean(person).insert();
        assertNotNull(person.getPersonId());
    }

    public void test_Update() throws SQLException {
        person = new PersonBean();
        dao.setBean(person).insert();
        person.setFirstName("Lewis");
        dao.update();
    }

    public void test_SetBean2() throws SQLException {
        Map<String, String> params = new HashMap<>();
        params.put("surName", "Hamilton");
        params.put("age", "621");
        params.put("dateOfBirth", "1323412341234");
        params.put("modified", "1523412341234");
        dao.setBean(new PersonBean(), params).insert();
        assertTrue(dao.delete());
    }

    public void test_Col2() throws SQLException {
        person.setDateOfBirth(new java.sql.Date(80, 04, 14));
        person.setSurName("RITC");
        dao.setBean(person).insert();
        person.setSurName("LIZ");
        dao.update();
    }

}
