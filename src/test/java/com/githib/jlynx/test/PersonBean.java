package com.githib.jlynx.test;

import com.github.jlynx.Column;
import com.github.jlynx.Table;

import java.sql.Date;
import java.sql.Timestamp;

@Table("PERSON")
public class PersonBean {

    @Column("DOB")
    private Date dateOfBirth;
    private Object image;

    @Column("ModTime")
    private Timestamp modified;
    private Integer personId;
    private String resume;

    @Column("LastName")
    private String surName;
    private String firstName;
    private Integer age;

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Object getImage() {
        return image;
    }

    public void setImage(Object image) {
        this.image = image;
    }

    public Timestamp getModified() {
        return modified;
    }

    public void setModified(Timestamp modified) {
        this.modified = modified;
    }

    public Integer getPersonId() {
        return personId;
    }

    public void setPersonId(Integer personId) {
        this.personId = personId;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
