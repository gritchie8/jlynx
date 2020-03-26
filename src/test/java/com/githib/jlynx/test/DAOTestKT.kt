package com.githib.jlynx.test

import com.github.jlynx.DAO
import com.github.jlynx.DAOImpl
import java.util.logging.LogManager

class DAOTestKT : junit.framework.TestCase() {

    private val pgdao: DAO = DAOImpl.newInstance("jdbc:postgresql:test", null)
    private val mydao: DAO = DAOImpl.newInstance("jdbc:mysql://localhost/jlynx_test?" +
            "user=jlynx&password=passwd&serverTimezone=GMT", null)
    var dropDDL = "DROP TABLE T_SCHOOL"

    override fun setUp() {
        val inputStream = this.javaClass.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(inputStream)

        // presumes you have a postgres database created named 'test'
        try {
            pgdao.executeSql(dropDDL, null)
        } catch (ex: Exception) {
            println(ex.message)
        } finally {
            pgdao.executeSql("CREATE TABLE T_SCHOOL (id SERIAL PRIMARY KEY, principal VARCHAR(50)," +
                    " name VARCHAR(30), address VARCHAR(100))", null)
        }

        // presumes mysql database 'jlynx_test'
        try {
            mydao.executeSql(dropDDL, null)
        } catch (ex: Exception) {
            println(ex.message)
        } finally {
            mydao.executeSql("CREATE TABLE T_SCHOOL (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(30)," +
                    " address varchar(100), principal varchar(50))", null)
        }


    }

    override fun tearDown() {
        /*val sql = "DROP TABLE T_SCHOOL"
        dao.executeSql(sql, null);
        dao2.executeSql(sql, null)*/
    }

    fun test_c() {
        var school = School()
        pgdao.setBean(school)
        school.name = "USC"
        school.principal = "Mrs. Smith"
        school.address = "Los Angeles, CA"
        assertTrue(school.id == null)
        school.id = pgdao.insert()

        for (x in 1001..1020) {
            school = School()
            school.address = "Address - $x"
            pgdao.setBean(school)
            school.id = pgdao.insert()
        }

        school.name = "Imperial College"
        pgdao.save()
        assertTrue(true)

        for (x in 1015..1040) {
            school = School()
            school.address = "Address - $x"
            pgdao.setBean(school)
            school.id = pgdao.save()
            assertTrue(x > 21)
        }

        val list = pgdao.getList(school.javaClass, "SELECT * FROM t_school", null)
        assertTrue(list.size > 20)
        list.forEach { c ->
            println((c as School).address)
            assertFalse(c == school)
        }

        //school = School()
        assertTrue(pgdao.setBean(school).delete())

    }

    fun test_b() {
        var s = School()
        s.address = "90 Bowery, New York, NY 10013"
        s.name = "St. Mary's"
        pgdao.setBean(s)
        assertTrue(pgdao.save() == 1)
    }

    fun test_a() {
        assertTrue(true)
        for (x in 0..10) {
            var s = School()
            mydao.setBean(s)
            s.id = mydao.insert()
            s.address = "${s.id} Main St"
            mydao.update()
            mydao.delete()
        }

    }

}