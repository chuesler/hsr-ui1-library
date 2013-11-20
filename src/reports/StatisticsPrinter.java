package reports;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.YearMonth;

import domain.Book;
import domain.Library;
import domain.Loan;

public class StatisticsPrinter {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");
	private static StatisticsPrinter instance;

	private JasperReport statisticsReport;

	private StatisticsPrinter() {
	}

	public static StatisticsPrinter getInstance() {
		if (instance == null) {
			synchronized (StatisticsPrinter.class) {
				if (instance == null) {
					instance = new StatisticsPrinter();
				}
			}
		}
		return instance;
	}

	public void exportStatistics(Library lib, File file) throws IOException {
		try {
			JasperExportManager.exportReportToPdfFile(generatePdf(lib), file.getPath());
		} catch (JRException e) {
			throw new ReportExcecutionFailedException(e);
		}
	}

	public void printStatistics(Library lib) throws IOException {
		try {
			JasperPrintManager.printReport(generatePdf(lib), true);
		} catch (JRException e) {
			throw new ReportExcecutionFailedException(e);
		}
	}

	private JasperPrint generatePdf(Library lib) {
		Map<String, Object> reportParameters = new HashMap<>();
		if (statisticsReport == null) {
			try {
				statisticsReport = JasperCompileManager.compileReport("reports/statistics.jrxml");
			} catch (JRException e) {
				throw new ReportCompileFailedException(e);
			}
		}

		List<ReportBean> beans = new ArrayList<>();
		beans.add(new ReportBean(lib));

		try {
			return JasperFillManager.fillReport(statisticsReport, reportParameters, new JRBeanCollectionDataSource(beans));
		} catch (JRException e) {
			throw new ReportExcecutionFailedException(e);
		}
	}

	public static class ReportBean {
		private Library library;

		public ReportBean(Library lib) {
			this.library = lib;
		}

		public int getCurrentLoansCount() {
			return library.getCurrentLoans().size();
		}

		public int getOverdueLoansCount() {
			return library.getOverdueLoans().size();
		}

		public Image getLoansChart() {
			DefaultPieDataset dataset = new DefaultPieDataset();

			dataset.setValue(BUNDLE.getString("Statistics.loans.overdue"), getOverdueLoansCount());
			// current loans include overdue ones, so we subtract them here to not count twice
			dataset.setValue(BUNDLE.getString("Statistics.loans.current"), getCurrentLoansCount() - getOverdueLoansCount());

			JFreeChart loansChart = ChartFactory.createPieChart("", dataset, true, false, false);
			loansChart.setBorderVisible(false);

			PiePlot plot = (PiePlot) loansChart.getPlot();
			plot.setLabelGenerator(null);
			plot.setBackgroundPaint(Color.WHITE);

			return loansChart.createBufferedImage(300, 300);
		}

		public Image getLoanDurationChart() {
			Map<YearMonth, List<Integer>> stats = new TreeMap<>();
			for (Loan loan : library.getLoans()) {
				if (!loan.isLent()) {
					YearMonth month = new YearMonth(loan.getReturnDate());
					if (!stats.containsKey(month)) {
						stats.put(month, new ArrayList<Integer>());
					}

					stats.get(month).add(
							Days.daysBetween(loan.getPickupDate().toDateMidnight(), loan.getReturnDate().toDateMidnight()).getDays());
				}
			}

			DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

			for (Map.Entry<YearMonth, List<Integer>> loansInMonth : stats.entrySet()) {
				double mean = 0;
				double stddev = 0;

				for (Integer value : loansInMonth.getValue()) {
					mean += value;
				}

				mean /= loansInMonth.getValue().size();

				for (Integer value : loansInMonth.getValue()) {
					double difference = mean - value;
					stddev += difference * difference;
				}

				stddev = Math.sqrt(stddev / loansInMonth.getValue().size());

				dataset.add(mean, stddev, BUNDLE.getString("Statistics.loans.averageDuration"), loansInMonth.getKey().toString("MMMM yyyy"));
			}

			CategoryItemRenderer renderer = new StatisticalBarRenderer();
			CategoryPlot plot = new CategoryPlot(dataset, new CategoryAxis(), new NumberAxis(), renderer);
			plot.setOrientation(PlotOrientation.HORIZONTAL);

			JFreeChart chart = new JFreeChart(plot);
			chart.setBackgroundPaint(Color.WHITE);
			plot.setBackgroundPaint(Color.WHITE);

			return chart.createBufferedImage(600, 300);
		}

		public Set<Book> getLentBooksLastMonth() {
			TreeSet<Book> books = new TreeSet<>(new Comparator<Book>() {
				@Override
				public int compare(Book o1, Book o2) {
					return o1.getTitle().compareTo(o2.getTitle());
				}
			});

			Interval currentMonth = YearMonth.now().toInterval();

			for (Loan loan : library.getLoans()) {
				if (currentMonth.contains(loan.getPickupDate())) {
					books.add(loan.getCopy().getTitle());
				}
			}

			return books;
		}
	}
}
