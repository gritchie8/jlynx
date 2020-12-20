package com.githib.jlynx.test

import com.github.jlynx.Table
import java.sql.Timestamp

@Table("contact")
data class Contact2(var id: Int? = null) {

    var lastName = ""
    var created: Timestamp? = null
    var active: Boolean? = null
}