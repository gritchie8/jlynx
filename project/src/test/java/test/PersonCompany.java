package test;

import java.sql.Date;

public class PersonCompany implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private int companyId;
	private Date endDt;
	private int personId;
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
