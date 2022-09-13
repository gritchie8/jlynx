package com.githib.jlynx.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.bind.JsonbBuilder;

public class DAOTest {

  private DAO dao;
  private DAO dao2;
  private DAO dao3;
  private Logger logger = LoggerFactory.getLogger("jlynx");

  @Before
  public void beforeTestMethod() throws SQLException {

    logger.info("Creating new DAO");
    dao = DAOImpl.newInstance("jdbc:hsqldb:mem:jlynx", null);
    dao2 = DAOImpl.newInstance("jdbc:postgresql:contact_acme", null);
    dao3 = DAOImpl.newInstance(
        "jdbc:postgresql://korr-dev-3-datadbdbcluster30f691f1-17alffkc749vc.cluster-cgnovxxxt5vj.us-east-1.rds.amazonaws.com/?user=postgres&password=5gY-xKPK=TOGhHsF5-zr8g3_O,0Mjx",
        null);

    assertNotNull(dao.getConnection());
    assertNotNull(dao2.getConnection());
    assertNotNull(dao3.getConnection());

    String ddl = "CREATE TABLE FRUIT ( ID INT PRIMARY KEY, NAME VARCHAR(20), COLOR VARCHAR(10), PRICE DECIMAL(10,2) DEFAULT 10.35,"
        + " PICKED DATE DEFAULT CURRENT_DATE, NOW TIMESTAMP DEFAULT CURRENT_TIMESTAMP )";
    dao.getConnection().setAutoCommit(false);
    dao2.getConnection();
    int rows = dao.executeSql(ddl, null);
    logger.info("rows = " + rows);
    assertNotNull(dao.getConnection());
    ddl = "CREATE TABLE TEST1 (ID SERIAL PRIMARY KEY, DATA JSONB DEFAULT '{}')";
    dao2.executeSql(ddl, null);

  }

  @After
  public void afterTestMethod() throws SQLException {
    logger.info("#afterTestMethod - start");
    dao.executeSql("DROP TABLE FRUIT", null);
    dao2.executeSql("DROP TABLE TEST1", null);
  }

  @Test
  public void execSQL() throws SQLException, ReflectiveOperationException {
    assertNotNull(dao);
    assertTrue(dao.getConnection().isValid(6000));
    assertTrue(1 == dao.executeSql("INSERT INTO FRUIT (ID, NAME, COLOR) VALUES (1, 'Apple', 'red')", null));
    assertTrue(1 == dao.executeSql("INSERT INTO FRUIT (ID, NAME, COLOR) VALUES (2, 'Banana', 'yellow')", null));
    assertTrue(1 == dao.executeSql("INSERT INTO FRUIT (id, name, color) VALUES (3, 'Orange', 'orange')", null));

    Fruit apple = new Fruit();
    apple.id = 1;
    dao.setBean(apple);
    assertTrue(dao.select());
    List<Fruit> fruits = dao.listOf(Fruit.class, "SELECT * FROM FRUIT ORDER BY ID", null);
    assertTrue(fruits.size() == 3);
    assertTrue(fruits.get(2).getColor().equals("orange"));

    apple.setSize(4);
    dao.setBean(apple);
    assertTrue(apple.id == 1);
    apple.now = null;

    dao.update();

    assertTrue(apple.getPicked().getYear() == 2021);
    apple.year = apple.getPicked().getYear();
    // apple.year = 2000;
    logger.info(JsonbBuilder.create().toJson(apple));
    assertTrue(3 == dao.executeSql("DELETE FROM FRUIT", null));

    TestData test = new TestData();
    test.data = "{\"id\": 767}";
    // dao2.setBean(test).insert();
    // test.id = test.id + 1;
    // dao2.setBean(test).insert();

    test.id = 1;
    dao2.setBean(test).insert();
    test.data = null;
    dao2.setBean(test).select();
    logger.info(test.data.toString());

    Contact contact = new Contact();
    contact.id = 1;
    // assertTrue(dao3.setBean(contact).select());
    ResultSet rs = dao3.getConnection().getMetaData().getPrimaryKeys(null, null, "contact");
    assertTrue(rs.next());

  }

}
