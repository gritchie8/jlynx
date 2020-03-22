package com.githib.jlynx.test

import com.github.jlynx.DAO
import com.github.jlynx.DAOImpl
import java.util.logging.LogManager

class DAOTestKT : junit.framework.TestCase() {

    var dao: DAO = DAOImpl.newInstance("jdbc:h2:mem:testdb", null)

    override fun setUp() {
        val inputStream = this.javaClass.getResourceAsStream("/logging.properties")
        // presumes you have a postgres database created named 'test'
        dao = DAOImpl.newInstance("jdbc:postgresql:test", null)
        LogManager.getLogManager().readConfiguration(inputStream)
        dao.executeSql("CREATE TABLE T_SCHOOL (id SERIAL PRIMARY KEY, principal VARCHAR(50)," +
                " name VARCHAR(30), address VARCHAR(100))", null)
    }

    override fun tearDown() {
        dao.executeSql("DROP TABLE T_SCHOOL", null);
    }

    fun test1() {
        var school = School()
        dao.setBean(school)
        school.name = "USC"
        school.principal = "Mrs. Smith"
        school.address = "Los Angeles, CA"
        assertTrue(school.id == null)
        school.id = dao.insert()

        for (x in 1001..1020) {
            school = School()
            school.address = "Address - $x"
            dao.setBean(school)
            school.id = dao.insert()
        }

        school.name = "Imperial College"
        dao.save()
        assertTrue(true)

        for (x in 1015..1040) {
            school = School()
            school.address = "Address - $x"
            dao.setBean(school)
            school.id = dao.save()
            assertTrue(x > 21)
        }

        val list = dao.getList(school.javaClass, "SELECT * FROM t_school", null)
        assertTrue(list.size > 20)
        list.forEach { c ->
            println((c as School).address)
            assertTrue(c is School)
            assertFalse(c == school)
        }

        //school = School()
        assertTrue(dao.setBean(school).delete())
    }

    fun test2() {
        var s = School()
        s.address = "90 Bowery, New York, NY 10013"
        dao.setBean(s)
        assertTrue(dao.save() == 1)
    }

    fun test3() {
        assertTrue(true)
    }

}