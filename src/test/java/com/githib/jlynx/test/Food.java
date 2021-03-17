package com.githib.jlynx.test;

import java.time.LocalDateTime;

import com.github.jlynx.Column;
import com.github.jlynx.Exclude;

public class Food {

  @Column("ID")
  public Integer id;
  public String name;
  public LocalDateTime now; // = LocalDateTime.now();

  @Exclude
  private int size = 1;

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

}
