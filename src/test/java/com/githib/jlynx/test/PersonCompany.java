package com.githib.jlynx.test;

import com.github.jlynx.Column;
import com.github.jlynx.Table;

import java.sql.Date;

@Table("PERSON_COMPANY")
public class PersonCompany {

    @Column("CID")
    private int companyId;

    @Column("PID")
    private int personId;

    private Date endDt;
    private Date startDt;

    public int getCompanyId() {
        return companyId;
    }

    public Date getEndDt() {
        return endDt;
    }

    public int getPersonId() {
        return personId;
    }

    public Date getStartDt() {
        return startDt;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public void setEndDt(Date endDt) {
        this.endDt = endDt;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public void setStartDt(Date startDt) {
        this.startDt = startDt;
    }

}
