package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.googlecode.jlynx.DAO;
import com.googlecode.jlynx.DAOImpl;

public class DAOTest extends TestCase {

	static Logger logger = Logger.getAnonymousLogger();

	final static String cn = "default";
	private DAO dao;
	PersonBean person;

	// int test = 40;

	protected void resetDAO() {
		person = new Person();
		person.setPersonId(990);
		dao = new DAOImpl(person).setConnection(cn);
	}

	@Override
	protected void setUp() {

		String ddl = "ddl_HSQL";
		if (!cn.equals("default"))
			ddl = "ddl_DERBY";

		logger.info("----- setUp() starting ----- using DDL statement: " + ddl);

		dao = new DAOImpl().setConnection(cn);
		try {
			dao.exec(ddl, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		resetDAO();
		logger.info("----- setUp() finished -----");

	}

	@Override
	protected void tearDown() {
		logger.info("----- tearDown() started -----");
		dao = new DAOImpl().setConnection(cn);
		try {
			dao.exec("DROP TABLE person", null);
			dao.exec("DROP TABLE company", null);
			dao.exec("DROP TABLE personcompany", null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("----- tearDown() finished -----");
	}

	public void testArrays() {
		String intArray = person.getPrefs().getClass().getName();
		assertTrue("[I".equals(intArray));
		Integer[] ints = new Integer[] { 0, 99 };
		assertTrue("[Ljava.lang.Integer;".equals(ints.getClass().getName()));
		try {
			dao.save();
		} catch (SQLException e) {
			fail();
			e.printStackTrace();
		}
	}

	public void testBean() {
		try {
			person.setSurName("Smith");
			dao.save();
			Company c = new Company();
			c.setId(100);
			int i = dao.setBean(c).save();
			assertTrue(i == 1);
			c.setName("General Motors");
			i = dao.update();
			assertTrue(i == 1);
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testBlobUpdate() {
		try {
			InputStream isImage = getClass().getClassLoader()
					.getResourceAsStream("jlynx.gif");
			InputStream isResume = getClass().getClassLoader()
					.getResourceAsStream("resume.txt");
			StringBuffer sb = new StringBuffer();
			BufferedReader d = new BufferedReader(new InputStreamReader(
					isResume));
			String line = null;
			while ((line = d.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}
			String txt = sb.toString();
			person.setSurName("TEST");
			dao.save();
			resetDAO();
			dao.exec(
					"UPDATE PERSON SET IMAGE = ?, RESUME = ? WHERE PERSONID = ?",
					new Object[] { isImage, txt, person.getPersonId() });
			resetDAO();
			dao.select();
			logger.fine("====Image==== " + person.getImage());
			assertNotNull(person.getImage());
			String resume = person.getResume();
			assertNotNull(resume);
			logger.fine("====Resume====\n\n" + resume.substring(0, 50)
					+ "\n===== rest not shown");

			if (person.getImage() instanceof InputStream) {
				isImage = (InputStream) person.getImage();
				assertTrue(isImage.available() > 0);
				String file = "/tmp/" + System.currentTimeMillis() + ".gif";
				FileOutputStream fos = new FileOutputStream(file);
				int c;
				while ((c = isImage.read()) != -1) {
					fos.write(c);
				}
				isImage = new FileInputStream(new File(file));
				assertNotNull(isImage);
			} else
				fail();

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	public void testCRUD() throws Exception {

		person.setSurName("Test2");
		person.setDateOfBirth(new java.sql.Date(System.currentTimeMillis()));
		dao.save();

		resetDAO();
		try {
			person.setSurName("Test3");
			dao.save();
		} catch (Exception e) {
			fail();
			assertTrue(e instanceof SQLException);
		}

		resetDAO();
		assertTrue(dao.select());
		logger.info("Person last name: " + person.getSurName());
		assertTrue("Test3".equals(person.getSurName()));

		resetDAO();
		try {
			dao.insert();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SQLException);
			resetDAO();
			dao.delete();
			resetDAO();
			dao.insert();
		}

		resetDAO();
		assertTrue(dao.select());

		PersonCompany pc = new PersonCompany();
		dao = new DAOImpl(pc);
		pc.setPersonId(person.getPersonId());
		assertFalse(dao.select());
		dao.save();
		assertTrue(dao.select());

	}

	// public void testDateBinding() {
	// PersonBean p = new PersonBean();
	// BeanUtils.setValue("dateOFBIRTH", p, "2/26/2020");
	// BeanUtils.setValue("Modified", p, "2/28/2020 15:30:00");
	// logger.info("dob=" + p.getDateOfBirth());
	// logger.info("mod=" + p.getModified());
	// assertTrue(p.getDateOfBirth().getTime() > System.currentTimeMillis());
	// assertTrue(p.getModified().getTime() > System.currentTimeMillis());
	// }

	public void testEntityNameResolution() {
		dao = new DAOImpl(new Person(999)).setConnection(cn);
		try {
			dao.save();
			resetDAO();
			person.setPersonId(new Integer(999));
			assertTrue(dao.select());
			person.setPersonId(new Integer(998));
			assertFalse(dao.select());
		} catch (SQLException e) {
			fail();
		}
	}

	public void testGen() throws Exception {
		dao.generateCode("test.model", null);
	}

	@SuppressWarnings("unchecked")
	public void testList() throws Exception {

		try {

			dao.setAutoCommit(false);

			for (int i = 0; i < 40; i++) {
				person.setPersonId(new Integer(i));
				person.setSurName("Test__" + i);
				person.setModified(new java.sql.Timestamp(System
						.currentTimeMillis()));
				int k = dao.insert();
				if (i == 0)
					logger.fine("insert = " + k);
			}
			dao.commit();
			dao = null;
			dao = new DAOImpl().setBean(person).setConnection(cn);
			List<Person> list = (List<Person>) dao.fetchList("selectAll", null);

			dao = new DAOImpl(Person.class).setConnection(cn);
			List<Person> plist = (List<Person>) dao
					.fetchList("selectAll", null);
			logger.info(plist.size() + " persons in list");
			assertTrue(plist.size() > 0);

			String json = dao.toJSON(list);
			logger.info(json + "\n===== end json");

			logger.info("List size = " + list.size());
			assertEquals(40, list.size());
			assertTrue(list.get(5) instanceof PersonBean);

			int low = 22;
			int high = 26;
			Object[] p = new Object[] { new Integer(low), new Integer(high) };

			dao = new DAOImpl(Person.class).setConnection(cn);
			String xml = dao.fetchXML("selectSome", p, "person");
			logger.info("\n\nXML output:\n\n" + xml + "\n===== XML done");

		} catch (Exception e) {
			// fail();
			e.printStackTrace();
			throw e;
		}

	}

	public void testToJSON() throws Exception {
		logger.fine("Entering");
		person.setModified(new Timestamp(System.currentTimeMillis()));
		person.setDateOfBirth(new Date(System.currentTimeMillis()));
		person.setSurName("Greg");
		person.setResume("Hello...\nMy name is... \n\tit's 'nice' out\r\"Quote\"\nIsn't it? yes \\ no");

		if (dao.insert() == 1) {
			resetDAO();
			assertTrue(dao.select());
			logger.fine("\n" + dao.toJSON());
			// logger.fine(r.toJavascript("test"));
		} else
			assertTrue(false);

		logger.info("Testing #selectJSON");

		for (int i = 10; i < 5; i++) {
			resetDAO();
			person.setPersonId(new Integer(i));
			person.setSurName("Testing Fetch JSON method");
			dao.insert();
		}
		resetDAO();
		String json = dao.fetchJSON("select * from person", null);
		logger.info(json + "\n===== end json");

	}
}
