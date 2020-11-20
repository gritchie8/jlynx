package com.githib.jlynx.test;

import com.github.jlynx.Column;
import com.github.jlynx.Table;

@Table("T")
public class TestPOJO {

    @Column(value = "ID", pk = true)
    public String ID = "001";
}
