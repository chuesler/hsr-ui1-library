package reports;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import domain.Customer;
import domain.Loan;

/**
 * Helper class to generate the reminders using JasperReports.
 * 
 * @author Ch. Hüsler
 */
public class ReminderPrinter {

	private static ReminderPrinter instance;

	private JasperReport reminderReport;

	private ReminderPrinter() {
	}

	public static ReminderPrinter getInstance() {
		if (instance == null) {
			synchronized (ReminderPrinter.class) {
				if (instance == null) {
					instance = new ReminderPrinter();
				}
			}
		}
		return instance;
	}

	public void exportReminders(List<Loan> loans, File targetFile) throws IOException {
		try {
			JasperExportManager.exportReportToPdfFile(generatePdf(loans), targetFile.getPath());
		} catch (JRException e) {
			throw new ReportExcecutionFailedException(e);
		}
	}

	public void printReminders(List<Loan> loans) throws IOException {

		try {
			JasperPrintManager.printReport(generatePdf(loans), true);
		} catch (JRException e) {
			throw new ReportExcecutionFailedException(e);
		}

	}

	private JasperPrint generatePdf(List<Loan> loans) throws IOException {
		Map<Customer, List<Loan>> loansPerCustomer = new HashMap<>();

		for (Loan loan : loans) {
			if (!loansPerCustomer.containsKey(loan.getCustomer())) {
				loansPerCustomer.put(loan.getCustomer(), new ArrayList<Loan>());
			}
			loansPerCustomer.get(loan.getCustomer()).add(loan);
		}

		// merge reports into one, so we don't get a bajillion separate print jobs
		JasperPrint print = null;
		for (Map.Entry<Customer, List<Loan>> entry : loansPerCustomer.entrySet()) {
			JasperPrint p = generateReminder(entry.getKey(), entry.getValue());

			if (print == null) {
				print = p;
			} else {
				for (JRPrintPage page : p.getPages()) {
					print.addPage(page);
				}
			}
		}
		return print;
	}

	private JasperPrint generateReminder(Customer customer, List<Loan> loans) throws IOException {
		if (customer == null) {
			throw new IllegalArgumentException("no customer");
		}

		if (loans == null || loans.size() == 0) {
			throw new IllegalArgumentException("no loans");
		}

		for (Loan loan : loans) {
			if (!loan.getCustomer().equals(customer)) {
				throw new CustomerMismatchException();
			}
			if (!loan.isOverdue()) {
				throw new NotOverdueException();
			}
		}

		if (reminderReport == null) {
			try {
				reminderReport = JasperCompileManager.compileReport("reports/reminder.jrxml");
			} catch (JRException e) {
				throw new ReportCompileFailedException(e);
			}
		}

		Map<String, Object> reportParameters = new HashMap<>();

		reportParameters.put("customerAddress", String.format("%s %s%n%s%n%d %s", customer.getFirstName(), customer.getSurname(),
				customer.getStreet(), customer.getZip(), customer.getCity()));
		reportParameters.put("currentDate", DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()));

		JasperPrint print;
		try {
			print = JasperFillManager.fillReport(reminderReport, reportParameters, new JRBeanCollectionDataSource(loans));
			return print;
			// JasperExportManager.exportReportToPdfFile(print, "loans" + customer.getFirstName() + customer.getSurname() + ".pdf");
		} catch (JRException e) {
			throw new ReportExcecutionFailedException(e);
		}

	}
}
