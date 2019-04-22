package test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import junit.framework.TestCase;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DAOTest extends TestCase {

    private PersonBean person;
    private PersonCompany personCompany = new PersonCompany();
    private DAO dao;
    private Logger logger = Logger.getLogger(DAO.class.getName());
    private String ddl = "CREATE TABLE PERSON (PERSONID INT PRIMARY KEY, DOB DATE, MODTIME TIMESTAMP, " +
            "RESUME CLOB, IMAGE BLOB, LASTNAME VARCHAR(30))";
    private String h2 = "jdbc:h2:mem:jlynxdb";

    @Override
    public void setUp() throws SQLException {
        dao = DAOImpl.newInstance("jdbc:hsqldb:mem:/jlynxdb", null);
        dao.executeSql(ddl, null);
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
        dao.executeSql("DROP TABLE PERSON", null);
    }


    public void test_CRUD() throws SQLException {
        person.setDateOfBirth(Date.valueOf("2000-04-14"));
        dao.setBean(person);
        assertFalse(dao.select());
        assertEquals(dao.save(), 1);
        assertTrue(dao.delete());
        person.setSurName("$#!@");
        dao.insert();
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
        dao.setBean(person);
        assertTrue(dao.select());
        assertNotNull(person.getSurName());
        assertNotNull(person.getModified());
    }

    @SuppressWarnings("unchecked")
    public void test_List() throws SQLException, InstantiationException, IllegalAccessException {
        int i = 90;
        while (i < 105) {
            person = new PersonBean(++i);
            person.setDateOfBirth(Date.valueOf("2010-01-01"));
            person.setSurName("P%$@#$%@# ------ " + i);
            person.setModified(new Timestamp(new java.util.Date().getTime()));
            dao.setBean(person);
            dao.insert();
        }
        java.util.List<PersonBean> list;
        try {
            list = (List<PersonBean>) dao.getList(Person.class, "select * from person", null);
            fail();
        } catch (Throwable ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        } finally {
            list = (List<PersonBean>) dao.getList(PersonBean.class, "select * from person", null);
        }

        assertTrue(list.size() > 10);

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

    public void test_LoggingLevel() {
        assertTrue(logger.isLoggable(Level.FINE));
    }

    public void test_BLOB() throws IOException, SQLException {
        File file = new File("./test/resources/Creissels_et_Viaduct_de_Millau.jpg");
        InputStream is = new FileInputStream(file);
        dao.setBean(person);
        dao.save();
        dao.executeSql("UPDATE PERSON SET IMAGE = ? WHERE PERSONID = ?", new Object[]{is, person.getPersonId()});
        person = new PersonBean(1);
        assertNull(person.getImage());
        dao.setBean(person);
        dao.select();
        assertNotNull(person.getImage());
        is = (InputStream) person.getImage();
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        Double x = 10000 * Math.random();
        file = new File("/tmp/test_" + x.intValue() + ".jpg");
        OutputStream outStream = new FileOutputStream(file);
        outStream.write(buffer);
    }
}
