package test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import junit.framework.TestCase;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DAOTest extends TestCase {

    private PersonBean person = new PersonBean(1);
    private PersonCompany personCompany = new PersonCompany();
    private DAO dao;
    private Logger logger = Logger.getLogger(DAO.class.getName());

    @Override
    public void setUp() {
        dao = DAOImpl.newInstance("jdbc:hsqldb:mem:/jlynxdb", null);
        assertNotNull(dao);
        try {
            assertNotNull(dao.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void test_CRUD() throws SQLException {
        dao.executeSql("CREATE TABLE PERSON (PERSONID INT PRIMARY KEY, DOB DATE)", null);
        person.setDateOfBirth(Date.valueOf("2000-04-14"));
        dao.setBean(person);
        assertFalse(dao.select());
        assertEquals(dao.save(), 1);
        assertTrue(dao.delete());
        dao.executeSql("ALTER TABLE PERSON ADD COLUMN LASTNAME VARCHAR(40)", null);
        person.setSurName("$#!@");
        dao.insert();
        dao.executeSql("CREATE TABLE PERSON_COMPANY (PID INT, CID INT, PRIMARY KEY (PID, CID))", null);
        dao.setBean(personCompany);
        personCompany.setPersonId(person.getPersonId());
        personCompany.setCompanyId(person.getPersonId() + 100);
        dao.insert();
        dao.delete();

        // 2nd part
        dao.executeSql("ALTER TABLE PERSON ADD COLUMN MODTIME TIMESTAMP", null);
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

    public void test_InsertMany() throws SQLException {
        int i = 90;
        while (i < 105) {
            person = new PersonBean(++i);
            person.setDateOfBirth(Date.valueOf("2010-01-01"));
            person.setSurName("P%$@#$%@# ------ " + i);
            person.setModified(new Timestamp(new java.util.Date().getTime()));
            dao.setBean(person);
            dao.insert();

        }
        assertNotNull(dao.getConnection());
    }

    @SuppressWarnings("unchecked")
    public void test_List() throws SQLException, InstantiationException, IllegalAccessException {
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
        dao.executeSql("ALTER TABLE PERSON ADD COLUMN RESUME CLOB", null);
        dao.executeSql("ALTER TABLE PERSON ADD COLUMN IMAGE BLOB", null);

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

    public void test_H2SQL() throws SQLException {
        dao = DAOImpl.newInstance("jdbc:h2:mem:jlynxdb", null);
        dao.executeSql("CREATE TABLE PERSON (PERSONID INT PRIMARY KEY, IMAGE BLOB, RESUME CLOB, LASTNAME VARCHAR(40))",
                null);
        person = new PersonBean(1005);
        person.setSurName("$#!@#$#-#$!@$");
        dao.setBean(person);
        dao.save();
        dao.delete();
        dao.executeSql("DROP TABLE PERSON", null);
    }

    public void test_LoggingLevel() {
        assertTrue(logger.isLoggable(Level.FINE));
    }
}
