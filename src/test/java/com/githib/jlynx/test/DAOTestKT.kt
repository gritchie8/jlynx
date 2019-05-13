package com.githib.jlynx.test

import com.github.jlynx.DAO
import com.github.jlynx.DAOImpl
import java.util.logging.LogManager

class DAOTestKT : junit.framework.TestCase() {

    var dao: DAO = DAOImpl.newInstance("jdbc:h2:mem:testdb", null)

    init {
        val inputStream = this.javaClass.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(inputStream)
        dao.executeSql("CREATE TABLE T_SCHOOL (ID INT PRIMARY KEY, PRINCIPAL VARCHAR(50), NAME VARCHAR(30), ADDRESS VARCHAR(100))", null)
    }

    fun test_CreateDelete() {
        var school = School(1)
        dao.setBean(school)
        school.Name = "USC"
        school.Principal = "Mrs. Smith"
        school.Address = "LA, Cali"
        assertTrue(school.id == 1)
        dao.insert()

        school = school.copy()
        school.id = 2
        dao.setBean(school)
        dao.save()

        val list = dao.getList(school.javaClass, "SELECT * FROM t_school", null)
        assertTrue(list.size == 2)
        list.forEach { c ->
            println(c)
            assertTrue(c is School)
            println(c?.equals(school))
            println(c == school)
        }

        school = School(2)
        assertTrue(dao.setBean(school).delete())
    }

}