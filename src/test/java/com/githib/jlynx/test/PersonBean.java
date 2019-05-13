package com.githib.jlynx.test;

import com.github.jlynx.Column;
import com.github.jlynx.Table;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Table("PERSON")
public class PersonBean {

    public PersonBean(int pid) {
        setPersonId(pid);
    }

    // must have a public no-arg constructor for DAO#getList
    public PersonBean() {
    }

    private List children;

    @Column("DOB")
    private Date dateOfBirth;

    private Object image;

    @Column("ModTime")
    private Timestamp modified;

    private Integer personId;
    private int[] prefs = new int[]{0, 1, 2};
    private String resume;

    @Column("LastName")
    private String surName;

    public List getChildren() {
        return children;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public Object getImage() {
        return image;
    }

    public Timestamp getModified() {
        return modified;
    }

    public Integer getPersonId() {
        return personId;
    }

    public int[] getPrefs() {
        return prefs;
    }

    public String getResume() {
        return resume;
    }

    public String getSurName() {
        return surName;
    }

    public void setChildren(List children) {
        this.children = children;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setImage(Object image) {
        this.image = image;
    }

    public void setModified(Timestamp modified) {
        this.modified = modified;
    }

    public void setPersonId(Integer id) {
        this.personId = id;
    }

    public void setPrefs(int[] prefs) {
        this.prefs = prefs;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public void setSurName(String name) {
        this.surName = name;
    }

}
