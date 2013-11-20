package test;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.Days;

import domain.Book;
import domain.Copy;
import domain.Customer;
import domain.IllegalLoanOperationException;
import domain.Loan;

public class LoanTest extends TestCase {

	public void testLoanCreation() {
		Loan l = createSampleLoan();
		assertEquals(0, Days.daysBetween(l.getPickupDate(), DateTime.now()).getDays());
		assertEquals("Keller", l.getCustomer().getSurname());
		assertEquals("Design Pattern", l.getCopy().getTitle().getTitle());
	}

	private Loan createSampleLoan() {
		Customer customer = new Customer("Keller", "Peter");
		Book title = new Book("Design Pattern");
		Copy copy = new Copy(title);
		Loan loan = new Loan(customer, copy);
		return loan;
	}

	public void testReturn() {
		Loan l = createSampleLoan();
		assertTrue(l.isLent());
		l.returnCopy();
		assertFalse(l.isLent());
	}

	public void testDurationCalculation() throws IllegalLoanOperationException {
		Loan l = createSampleLoan();
		assertEquals(-1, l.getDaysOfLoanDuration());

		DateTime returnDate = l.getPickupDate().plusDays(12);
		l.returnCopy(returnDate);

		assertEquals(12, l.getDaysOfLoanDuration());
		assertEquals(l.getReturnDate(), returnDate);

	}

	public void testDateConsistency() throws IllegalLoanOperationException {
		Loan l = createSampleLoan();
		l.setPickupDate(new DateTime(2009, 01, 01, 00, 00));
		assertTrue(l.isLent());
		try {
			l.returnCopy(new DateTime(2008, 12, 31, 00, 00));
			fail("Book cannot retuned before the pickup date");
		} catch (IllegalLoanOperationException e) {
			// expected
		}
		assertTrue(l.isLent());
		l.returnCopy(new DateTime(2009, 12, 31, 00, 00));
		assertFalse(l.isLent());
		try {
			l.setPickupDate(new DateTime(2008, 10, 31, 00, 00));
			fail("pickup date cannot be set after the book was returned");
		} catch (IllegalLoanOperationException e) {
			// expected
		}
	}
}
