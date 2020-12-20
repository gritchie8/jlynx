package com.githib.jlynx.test;

import com.github.jlynx.Column;
import com.github.jlynx.Table;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Table("PERSON")
public class PersonBean {

    @Column("ID")
    private Integer personId;

    public Integer getPersonId() {
        return this.personId;
    }

    @SuppressWarnings("unused")
    protected String Resume;

    public String FirstName;
    public Integer Age;
    public Object Image;
    public BigDecimal Amt;// = BigDecimal.valueOf(88545.65);

    @Column("DOB")
    public Date DateOfBirth;

    @Column("lastName")
    public String SurName;

    @Column(value = "Age2")
    public Integer age2;

    @Column("ModTime")
    public Timestamp Modified;

    @Column(include = false)
    @SuppressWarnings("unused")
    public boolean Hidden = true;

    @Column(include = false)
    public Integer WeightInKg = 79;
}
