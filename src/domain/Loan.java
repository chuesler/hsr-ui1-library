package domain;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;

public class Loan {

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public static final String DUE_DATE = "dueDate";

	public static final String RETURN_DATE = "returnDate";

	public static final String PICKUP_DATE = "pickupDate";

	private Copy copy;
	private Customer customer;
	private DateTime pickupDate, returnDate, dueDate;
	private final static int DAYS_TO_RETURN_BOOK = 30;

	public Loan(Customer customer, Copy copy) {
		this.copy = copy;
		this.customer = customer;
		pickupDate = DateTime.now();
		dueDate = pickupDate.plusDays(DAYS_TO_RETURN_BOOK).toDateMidnight().toDateTime();
	}

	public boolean isLent() {
		return returnDate == null;
	}

	public boolean returnCopy() {
		try {
			returnCopy(new DateTime());
		} catch (IllegalLoanOperationException e) {
			return false;
		}
		return true;
	}

	public void returnCopy(DateTime returnDate) throws IllegalLoanOperationException {
		if (returnDate.isBefore(pickupDate)) {
			throw new IllegalLoanOperationException("Return Date is before pickupDate");
		}
		DateTime old = this.returnDate;
		this.returnDate = returnDate;
		pcs.firePropertyChange(RETURN_DATE, old, returnDate);
	}

	public void setPickupDate(DateTime pickupDate) throws IllegalLoanOperationException {
		if (!isLent()) {
			throw new IllegalLoanOperationException("Loan is already retuned");
		}
		DateTime old = this.pickupDate;
		this.pickupDate = pickupDate;
		pcs.firePropertyChange(PICKUP_DATE, old, pickupDate);

		old = dueDate;
		dueDate = pickupDate.plusDays(DAYS_TO_RETURN_BOOK).withTime(23, 59, 59, 999);
		pcs.firePropertyChange(DUE_DATE, old, dueDate);
	}

	public DateTime getPickupDate() {
		return pickupDate;
	}

	public String getPickupDateString() {
		return getFormattedDate(pickupDate);
	}

	public DateTime getReturnDate() {
		return returnDate;
	}

	public DateTime getDueDate() {
		return dueDate;
	}

	public String getDueDateString() {
		return getFormattedDate(getDueDate());
	}

	public String getReturnDateString() {
		return getFormattedDate(getReturnDate());
	}

	public Copy getCopy() {
		return copy;
	}

	public Customer getCustomer() {
		return customer;
	}

	@Override
	public String toString() {
		return "Loan of: " + copy.getTitle().getTitle() + "\tFrom: " + customer.getFirstName() + " " + customer.getSurname() + "\tPick up: "
				+ getFormattedDate(pickupDate) + "\tReturn: " + getFormattedDate(returnDate) + "\tDays: " + getDaysOfLoanDuration();
	}

	private String getFormattedDate(DateTime date) {
		if (date != null) {
			return date.toString(DateTimeFormat.mediumDate());
		}
		return "00.00.00";
	}

	public int getDaysOfLoanDuration() {
		if (returnDate != null) {
			return Days.daysBetween(pickupDate, returnDate).getDays();
		}
		return -1;
	}

	public int getDueInDays() {
		return Days.daysBetween(DateTime.now(), getDueDate()).getDays();
	}

	public int getDaysOverdue() {
		if (!isOverdue()) {
			return 0;
		}

		return Days.daysBetween(getDueDate(), DateTime.now()).getDays();
	}

	public boolean isOverdue() {
		return isLent() && getDueDate().isBeforeNow();
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
