package test;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;
import junit.framework.TestCase;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class DAOTest extends TestCase {

    private PersonBean person = new PersonBean(1);
    private PersonCompany personCompany = new PersonCompany();
    private DAO dao;

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
        int i = 50;
        while (i < 100) {
            person = new PersonBean(++i);
            person.setDateOfBirth(Date.valueOf("2010-01-01"));
            person.setSurName("P%$@#$%@# ------ " + i);
            dao.setBean(person);
            dao.insert();
            Logger.getAnonymousLogger().info(person.getSurName());
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

        assertTrue(list.size() > 50);
        Logger.getLogger("jlynx").info("List");
    }
}
