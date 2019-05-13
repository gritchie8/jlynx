package com.githib.jlynx.test

import com.github.jlynx.Table

@Table("T_SCHOOL")
data class School(var id: Int) {

    init {
        println("#init School id = $id")
    }

    constructor() : this(0)

    var Principal: String = ""
    var Name: String = ""
    var Address: String = "Not set"

}