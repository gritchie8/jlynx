package test;

public class Person extends PersonBean {

	private static final long serialVersionUID = 1L;

	public Person(int pid) {
		setPersonId(new Integer(pid));
	}

	public Person() {
	}

}
