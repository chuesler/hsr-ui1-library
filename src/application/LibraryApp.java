package application;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import domain.Book;
import domain.Copy;
import domain.Customer;
import domain.IllegalLoanOperationException;
import domain.Library;
import domain.Loan;
import domain.Shelf;

/**
 * Main class provided for this project, adapted to add a GUI and splash screen.
 * 
 * @author Ch. Hüsler
 * @author HSR UInt1
 */
public class LibraryApp {
	private static final Logger LOG = Logger.getLogger(LibraryApp.class);

	public static void main(String[] args) throws Exception {
		SplashScreen splash;
		try {
			splash = SplashScreen.getSplashScreen();
		} catch (Exception e) {
			splash = null;
		}

		Library library = new Library();
		initLibrary(library, new ProgressMonitor(10) {
			private Graphics2D g;

			@Override
			protected void progressUpdated() {
				SplashScreen splash = SplashScreen.getSplashScreen();
				if (g == null && splash != null) {
					g = splash.createGraphics();
				}
				if (g != null) {
					g.setColor(Color.BLACK);
					g.drawRect(28, 320, 584, 7); // splash image is 640x400

					int width = 21 + 560 / getMax() * getProgress();
					g.fillRect(30, 322, width, 4);

					splash.update();
				}
			}
		});

		UIManager.setLookAndFeel(new NimbusLookAndFeel());

		InventoryFrame libraryFrame = new InventoryFrame(library);

		libraryFrame.setVisible(true);

		if (splash != null && splash.isVisible()) {
			splash.close();
		}
	}

	private static void initLibrary(Library library, ProgressMonitor monitor) throws ParserConfigurationException, SAXException, IOException,
			IllegalLoanOperationException {

		Locale.setDefault(new Locale("de", "CH"));

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		loadCustomersFromXml(library, builder, new File("data/customers.xml"));

		monitor.updateProgress(2);

		loadBooksFromXml(library, builder, new File("data/books.xml"));

		monitor.updateProgress(4);

		// create pseudo random books and loans
		createBooksAndLoans(library, monitor);

		LOG.trace("Initialisation of the library was successful!\n");
		LOG.trace("Books in library: " + library.getBooks().size());
		LOG.trace("Customers: " + library.getCustomers().size() + "\n");
		LOG.trace("Copies in library: " + library.getCopies().size());
		LOG.trace("Copies currently on loan: " + library.getLentOutBooks().size());
		int lentBooksPercentage = (int) ((double) library.getLentOutBooks().size() / library.getCopies().size() * 100);
		LOG.trace("Percent copies on loan: " + lentBooksPercentage + "%");
		LOG.trace("Copies currently overdue: " + library.getOverdueLoans().size());

		for (Loan l : library.getLoans()) {
			if (l.isLent()) {
				LOG.trace(l.getCopy().getTitle() + ":" + l.getCopy().getInventoryNumber() + ":" + l);
			}
		}

		for (Loan l : library.getOverdueLoans()) {
			LOG.trace(l.getDaysOverdue());
		}
	}

	private static void createBooksAndLoans(Library library, ProgressMonitor monitor) throws IllegalLoanOperationException {
		int progressSteps = library.getBooks().size() / 6;

		for (int i = 0; i < library.getBooks().size(); i++) {

			if (i > 0 && i % progressSteps == 0) {
				monitor.updateProgress(4 + i / progressSteps);
			}

			switch (i % 4) {
				case 0:
					Copy c1 = library.createAndAddCopy(library.getBooks().get(i));
					c1.setCondition(Copy.Condition.GOOD);
					createLoansForCopy(library, c1, i, 5);
					Copy c2 = library.createAndAddCopy(library.getBooks().get(i));
					c2.setCondition(Copy.Condition.DAMAGED);
					createLoansForCopy(library, c2, i, 2);
					Copy c3 = library.createAndAddCopy(library.getBooks().get(i));
					c3.setCondition(Copy.Condition.WASTE);
					break;
				case 1:
					Copy c4 = library.createAndAddCopy(library.getBooks().get(i));
					createLoansForCopy(library, c4, i, 4);
					library.createAndAddCopy(library.getBooks().get(i));
					break;
				case 2:
					Copy c5 = library.createAndAddCopy(library.getBooks().get(i));
					createLoansForCopy(library, c5, i, 2);
					break;
				case 3:
					Copy c6 = library.createAndAddCopy(library.getBooks().get(i));
					createOverdueLoanForCopy(library, c6, i);
					break;
			}
		}
	}

	private static void loadBooksFromXml(Library library, DocumentBuilder builder, File file) throws SAXException, IOException {
		Document doc2 = builder.parse(file);
		NodeList titles = doc2.getElementsByTagName("title");
		for (int i = 0; i < titles.getLength(); i++) {
			Node title = titles.item(i);
			Book b = library.createAndAddBook(getTextContentOf(title, "name"));
			b.setAuthor(getTextContentOf(title, "author"));
			b.setPublisher(getTextContentOf(title, "publisher"));
			b.setShelf(Shelf.getRandomShelf());
		}
	}

	private static void loadCustomersFromXml(Library library, DocumentBuilder builder, File file) throws SAXException, IOException {
		Document doc = builder.parse(file);
		NodeList customers = doc.getElementsByTagName("customer");
		for (int i = 0; i < customers.getLength(); i++) {
			Node customer = customers.item(i);

			Customer c = library.createAndAddCustomer(getTextContentOf(customer, "name"), getTextContentOf(customer, "surname"));
			c.setAdress(getTextContentOf(customer, "street"), Integer.parseInt(getTextContentOf(customer, "zip")),
					getTextContentOf(customer, "city"));
		}
	}

	private static void createLoansForCopy(Library library, Copy copy, int position, int count) throws IllegalLoanOperationException {
		// Create Loans in the past
		for (int i = count; i > 1; i--) {
			Loan l = library.createAndAddLoan(getCustomer(library, position + i), copy);
			DateTime pickup = l.getPickupDate().minusMonths(i).plusDays(position % 10);
			l.setPickupDate(pickup);
			l.returnCopy(pickup.plusDays(position % 10 + i * 2));
		}
		// Create actual open loans
		if (position % 2 == 0) {
			Loan l = library.createAndAddLoan(getCustomer(library, position), copy);
			DateTime pickup = l.getPickupDate();
			l.setPickupDate(pickup.minusDays(position % 10));
		}
	}

	private static void createOverdueLoanForCopy(Library library, Copy copy, int position) throws IllegalLoanOperationException {
		Loan l = library.createAndAddLoan(getCustomer(library, position), copy);
		l.setPickupDate(l.getPickupDate().minusMonths(2));
	}

	private static Customer getCustomer(Library library, int position) {
		return library.getCustomers().get(position % library.getCustomers().size());
	}

	private static String getTextContentOf(Node element, String name) {
		NodeList attributes = element.getChildNodes();
		for (int r = 0; r < attributes.getLength(); r++) {
			if (attributes.item(r).getNodeName().equals(name)) {
				return attributes.item(r).getTextContent();
			}
		}
		return "";
	}

	/**
	 * Simple progress monitor to update splash screen during init. The swing version of this opens a dialog itself, which would a) close the splash
	 * screen and b) defeat it's purpose.
	 * 
	 * @author chuesler
	 */
	private static abstract class ProgressMonitor {
		private int max, progress;

		public ProgressMonitor(int max) {
			this.max = max;
		}

		public void updateProgress(int progress) {
			if (progress > max) {
				progress = max;
			}
			this.progress = progress;
			progressUpdated();
		}

		protected abstract void progressUpdated();

		public int getMax() {
			return max;
		}

		public int getProgress() {
			return progress;
		}

	}
}
