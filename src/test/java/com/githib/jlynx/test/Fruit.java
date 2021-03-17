package com.githib.jlynx.test;

import java.time.LocalDate;

import com.github.jlynx.Column;
import com.github.jlynx.Exclude;
import com.github.jlynx.Table;

@Table("FRUIT")
public class Fruit extends Food {

  public Fruit() {
    vendors = new java.util.ArrayList<>();
    vendors.add("Union Market");
    prefs = new Integer[] { 1, 2 };
  }

  public String origin;

  private String color;

  public void setColor(String colour) {
    this.color = colour;
  }

  public String getColor() {
    return color;
  }

  public java.util.List<String> vendors;
  public Integer[] prefs;
  public java.util.TreeSet<?> things;

  @Exclude
  private String other = "other";

  @Column("PRICE")
  public java.math.BigDecimal price;
  private LocalDate picked;

  public LocalDate getPicked() {
    return picked;
  }

  public void setPicked(LocalDate picked) {
    this.picked = picked;
  }

  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }

  @Exclude
  public int year = -1;
  @Exclude
  public int month = -1;
  @Exclude
  public int day = -1;

}
