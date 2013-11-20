package domain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;
import domain.Copy.Condition;

public class Library {

	private BasicEventList<Copy> copies;
	private BasicEventList<Customer> customers;
	private BasicEventList<Loan> loans;
	private BasicEventList<Book> books;

	private static final EnumSet<Condition> ACCEPTABLE_CONDITIONS = EnumSet.of(Condition.NEW, Condition.GOOD, Condition.DAMAGED);

	public Library() {
		copies = new BasicEventList<Copy>();
		customers = new BasicEventList<Customer>();
		loans = new BasicEventList<Loan>();
		books = new BasicEventList<Book>();
	}

	public Loan createAndAddLoan(Customer customer, Copy copy) {
		if (!isCopyLent(copy)) {
			Loan l = new Loan(customer, copy);
			loans.add(l);
			return l;
		} else {
			return null;
		}
	}

	public int addCustomer(Customer customer) {
		int probableIndex = customers.size(); // to avoid looping through the whole list if not necessary

		customers.add(customer);

		return customers.get(probableIndex).equals(customer) ? probableIndex : customers.indexOf(customer);
	}

	public Customer createAndAddCustomer(String name, String surname) {
		Customer c = new Customer(name, surname);
		customers.add(c);
		return c;
	}

	public Book createAndAddBook(String name) {
		Book b = new Book(name);
		books.add(b);
		return b;
	}

	public Copy createAndAddCopy(Book title) {
		Copy c = new Copy(title);
		copies.add(c);
		return c;
	}

	public Book findByBookTitle(String title) {
		for (Book b : books) {
			if (b.getTitle().equals(title)) {
				return b;
			}
		}
		return null;
	}

	public boolean isCopyLent(Copy copy) {
		Loan l = getLoan(copy);

		return l != null;
	}

	public Loan getLoan(Copy copy) {
		if (copy != null) {
			for (Loan l : loans) {
				if (l.getCopy().equals(copy) && l.isLent()) {
					return l;
				}
			}
		}
		return null;
	}

	public EventList<Copy> getCopiesOfBook(final Object book) {
		FilterList<Copy> filtered = new FilterList<>(copies);

		filtered.setMatcher(new Matcher<Copy>() {
			@Override
			public boolean matches(Copy item) {
				return item.getTitle().equals(book);
			}
		});

		return filtered;
	}

	public List<Loan> getLentCopiesOfBook(Book book) {
		List<Loan> lentCopies = new ArrayList<Loan>();
		for (Loan l : loans) {
			if (l.getCopy().getTitle().equals(book) && l.isLent()) {
				lentCopies.add(l);
			}
		}
		return lentCopies;
	}

	public List<Copy> getAvailableCopiesOfBook(Book book) {
		List<Copy> availableCopies = new ArrayList<>();
		availableCopies.addAll(getCopiesOfBook(book));
		for (Loan l : loans) {
			if (l.getCopy().getTitle().equals(book) && l.isLent()) {
				availableCopies.remove(l.getCopy());
			}
		}

		for (Iterator<Copy> it = availableCopies.iterator(); it.hasNext();) {
			if (!ACCEPTABLE_CONDITIONS.contains(it.next().getCondition())) {
				it.remove();
			}
		}

		return availableCopies;
	}

	public FilterList<Loan> getCustomerLoans(final Customer customer) {
		return new FilterList<>(loans, new Matcher<Loan>() {
			@Override
			public boolean matches(Loan item) {
				return item.getCustomer().equals(customer);
			}
		});
	}

	public FilterList<Loan> getOverdueLoans() {
		return new FilterList<>(loans, new Matcher<Loan>() {
			@Override
			public boolean matches(Loan item) {
				return item.isLent() && item.isOverdue();
			}
		});
	}

	public List<Copy> getAvailableCopies() {
		return getCopies(false);
	}

	public List<Copy> getLentOutBooks() {
		return getCopies(true);
	}

	private List<Copy> getCopies(final boolean isLent) {
		FilterList<Copy> retCopies = new FilterList<>(copies, new Matcher<Copy>() {
			@Override
			public boolean matches(Copy item) {
				return isLent == isCopyLent(item);
			}
		});

		return retCopies;
	}

	public EventList<Copy> getCopies() {
		return copies;
	}

	public EventList<Loan> getLoans() {
		return loans;
	}

	public FilterList<Loan> getCurrentLoans() {
		return new FilterList<>(loans, new Matcher<Loan>() {
			@Override
			public boolean matches(Loan item) {
				return item.isLent();
			};
		});
	}

	public EventList<Book> getBooks() {
		return books;
	}

	public EventList<Customer> getCustomers() {
		return customers;
	}

	public Copy getCopyForInventoryNumber(long inventoryNumber) {
		for (Copy copy : copies) {
			if (copy.getInventoryNumber() == inventoryNumber) {
				return copy;
			}
		}
		return null;
	}

}
