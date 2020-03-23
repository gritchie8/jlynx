package com.githib.jlynx.test

import com.github.jlynx.DAO
import com.github.jlynx.DAOImpl
import java.util.logging.LogManager

class DAOTestKT : junit.framework.TestCase() {

    private val dao: DAO = DAOImpl.newInstance("jdbc:postgresql:test", null)
    private val dao2: DAO = DAOImpl.newInstance("jdbc:mysql://localhost/jlynx_test?" +
            "user=jlynx&password=passwd&serverTimezone=GMT", null)

    override fun setUp() {
        val inputStream = this.javaClass.getResourceAsStream("/logging.properties")
        // presumes you have a postgres database created named 'test'
        LogManager.getLogManager().readConfiguration(inputStream)
        dao.executeSql("CREATE TABLE T_SCHOOL (id SERIAL PRIMARY KEY, principal VARCHAR(50)," +
                " name VARCHAR(30), address VARCHAR(100))", null)
        dao2.executeSql("CREATE TABLE T_SCHOOL (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(30)," +
                " address varchar(100), principal varchar(50))", null)
    }

    override fun tearDown() {
        val sql = "DROP TABLE T_SCHOOL"
        dao.executeSql(sql, null);
        dao2.executeSql(sql, null)
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
        for (x in 0..10) {
            var s = School()
            //s.id = x
            dao2.setBean(s)
            s.id = dao2.insert()
            s.address = "${s.id} Main St"
            dao2.update()
            dao2.delete()
            //println("ID = ${s.id}")
        }

    }

}