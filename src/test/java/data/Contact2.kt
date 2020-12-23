package data

import com.github.jlynx.Column
import com.github.jlynx.Table
import java.sql.Timestamp

@Table("contact")
data class Contact2(var id: Int? = null) {

    @field:Column("lastname")
    var surName = ""
    var created: Timestamp? = null
    var active: Boolean? = null
}