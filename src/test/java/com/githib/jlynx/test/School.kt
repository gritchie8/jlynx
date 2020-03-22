package com.githib.jlynx.test

import com.github.jlynx.Column
import com.github.jlynx.Table

@Table("t_school")
class School() {

    var id: Int? = null

    var principal: String? = null

    var name: String = ""

    @Column("address")
    var address: String = "Not set"

}