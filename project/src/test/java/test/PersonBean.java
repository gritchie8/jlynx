package test;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@SuppressWarnings("rawtypes")
public class PersonBean implements Serializable {

	private static final long serialVersionUID = -4546373603361788645L;

	private List children;
	private Date dateOfBirth;
	private Object image;
	private Timestamp modified;
	private Integer personId;
	private int[] prefs = new int[] { 0, 1, 2 };
	private String resume;
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
