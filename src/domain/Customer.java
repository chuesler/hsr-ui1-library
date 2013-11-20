package domain;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class Customer {

	public static final String FIRSTNAME = "firstName";
	public static final String SURNAME = "surname";
	public static final String STREET = "street";
	public static final String ZIP = "zip";
	public static final String CITY = "city";

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	private String firstName, surname, street, city;
	private int zip;

	public Customer(String surname, String firstName) {
		this.firstName = firstName;
		this.surname = surname;
	}

	public void setAdress(String street, int zip, String city) {
		setStreet(street);
		setZip(zip);
		setCity(city);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String name) {
		String old = this.firstName;
		this.firstName = name;
		pcs.firePropertyChange(FIRSTNAME, old, name);
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		String old = this.surname;
		this.surname = surname;
		pcs.firePropertyChange(SURNAME, old, surname);
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		String old = this.street;
		this.street = street;
		pcs.firePropertyChange(STREET, old, street);
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		String old = this.city;
		this.city = city;
		pcs.firePropertyChange(CITY, old, city);
	}

	public int getZip() {
		return zip;
	}

	public void setZip(int zip) {
		int old = this.zip;
		this.zip = zip;
		pcs.firePropertyChange(ZIP, old, zip);
	}

	@Override
	public String toString() {
		return surname + " " + firstName + ", " + street + ", " + zip + " " + city;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(property, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(property, listener);
	}
}
