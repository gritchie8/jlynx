package com.githib.jlynx.test;

import com.github.jlynx.Table;

@Table("contact")
public class Contact {

    private Integer id;
    public String lastname;
    public Boolean active;

    public Integer getId() {
        return this.id;
    }

}
