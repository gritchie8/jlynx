package com.githib.jlynx.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import com.github.jlynx.DAO;
import com.github.jlynx.DAOImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class DAOTest {

  private DAO dao;

  @Before
  public void beforeTestMethod() throws SQLException {

    LoggerFactory.getLogger("jlynx").info("Creating new DAO");
    dao = DAOImpl.newInstance("jdbc:hsqldb:mem:jlynx", null);
    assertNotNull(dao.getConnection());
    String ddl = "CREATE TABLE FRUIT ( ID INT PRIMARY KEY, NAME VARCHAR(20), COLOR VARCHAR(10) )";
    dao.getConnection().setAutoCommit(false);
    int rows = dao.executeSql(ddl, null);
    LoggerFactory.getLogger("jlynx").info("rows = " + rows);
    assertNotNull(dao.getConnection());

  }

  @After
  public void afterTestMethod() throws SQLException {
    LoggerFactory.getLogger("jlynx").info("#afterTestMethod - start");
    dao.executeSql("DROP TABLE FRUIT", null);

  }

  @Test
  public void execSQL() throws SQLException, ReflectiveOperationException {
    assertNotNull(dao);
    assertTrue(dao.getConnection().isValid(6000));
    assertTrue(1 == dao.executeSql("INSERT INTO FRUIT (ID, NAME, COLOR) VALUES (1, 'Apple', 'US')", null));
    assertTrue(1 == dao.executeSql("INSERT INTO FRUIT (ID, NAME, COLOR) VALUES (2, 'Banana', 'MX')", null));
    assertTrue(1 == dao.executeSql("INSERT INTO FRUIT (ID, NAME, COLOR) VALUES (3, 'Orange', 'US-FL')", null));

    Fruit apple = new Fruit();
    apple.id = 1;
    dao.setBean(apple);
    assertTrue(dao.select());
    assertTrue(dao.listOf(Fruit.class, "SELECT * FROM FRUIT", null).size() == 3);

    assertTrue(3 == dao.executeSql("DELETE FROM FRUIT", null));
  }

}
