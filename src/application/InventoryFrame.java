package application;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import reports.ReminderPrinter;
import reports.StatisticsPrinter;
import actions.SaveFileAction;

import comp.TabWithIcon;

import domain.Library;

/**
 * Main frame.
 * 
 * @author Ch. Hüsler
 */
public class InventoryFrame extends JFrame {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages"); //$NON-NLS-1$
	private static final Logger LOG = Logger.getLogger(InventoryFrame.class);

	private Library library;

	private Action printRemindersAction = new PrintRemindersAction();
	private Action exportRemindersAction = new ExportRemindersAction();

	private Action printStatisticsAction = new PrintStatisticsAction();
	private Action exportStatisticsAction = new ExportStatisticsAction();

	private Action showAboutAction = new ShowAboutAction();

	/**
	 * Create the application.
	 */
	public InventoryFrame(Library lib) {
		this.library = lib;

		initialize();
		pack();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1200, 600));
		setMinimumSize(new Dimension(900, 500));
		setTitle(BUNDLE.getString("InventoryFrame.title"));

		setLocationByPlatform(true);

		// Menus
		JMenuBar menuBar = new JMenuBar();
		getContentPane().add(menuBar, BorderLayout.NORTH);

		JMenu remindersMenu = new JMenu(BUNDLE.getString("InventoryFrame.menu.reminders")); //$NON-NLS-1$
		remindersMenu.setMnemonic('M');
		menuBar.add(remindersMenu);

		JMenuItem printRemindersItem = new JMenuItem(printRemindersAction);
		printRemindersItem.setAccelerator(KeyStroke.getKeyStroke("control shift M"));
		remindersMenu.add(printRemindersItem);

		JMenuItem exportRemindersItem = new JMenuItem(exportRemindersAction);
		exportRemindersItem.setAccelerator(KeyStroke.getKeyStroke("control M"));
		remindersMenu.add(exportRemindersItem);

		JMenu statisticsMenu = new JMenu(BUNDLE.getString("InventoryFrame.menu.statistics"));
		statisticsMenu.setMnemonic('T');
		menuBar.add(statisticsMenu);

		JMenuItem printStatisticsItem = new JMenuItem(printStatisticsAction);
		printStatisticsItem.setAccelerator(KeyStroke.getKeyStroke("control shift T"));
		statisticsMenu.add(printStatisticsItem);

		JMenuItem exportStatisticsItem = new JMenuItem(exportStatisticsAction);
		exportStatisticsItem.setAccelerator(KeyStroke.getKeyStroke("control T"));
		statisticsMenu.add(exportStatisticsItem);

		JMenu helpMenu = new JMenu(BUNDLE.getString("InventoryFrame.menu.help")); //$NON-NLS-1$
		helpMenu.setMnemonic('H');
		menuBar.add(helpMenu);

		JMenuItem showAboutItem = new JMenuItem(showAboutAction);
		helpMenu.add(showAboutItem);

		// Tabs
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		BookInventoryPanel bookPanel = new BookInventoryPanel(library);
		tabbedPane.addTab(null, null, bookPanel, null);
		tabbedPane.setTabComponentAt(0,
				new TabWithIcon(BUNDLE.getString("InventoryFrame.books"), new ImageIcon(getClass().getResource("/silk/book_open.png"))));
		tabbedPane.setMnemonicAt(0, 'B');

		LoanInventoryPanel loanPanel = new LoanInventoryPanel(library);
		tabbedPane.addTab(null, null, loanPanel, null);
		tabbedPane.setTabComponentAt(1,
				new TabWithIcon(BUNDLE.getString("InventoryFrame.loans"), new ImageIcon(getClass().getResource("/silk/book_go.png"))));
		tabbedPane.setMnemonicAt(1, 'A');

		CustomerInventoryPanel customerPanel = new CustomerInventoryPanel(library);
		tabbedPane.addTab(null, null, customerPanel, null);
		tabbedPane.setTabComponentAt(2,
				new TabWithIcon(BUNDLE.getString("InventoryFrame.customers"), new ImageIcon(getClass().getResource("/silk/user.png"))));
		tabbedPane.setMnemonicAt(2, 'K');
	}

	private class PrintRemindersAction extends AbstractAction {

		public PrintRemindersAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("Reminders.all.print"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/printer.png")));
			putValue(NAME, BUNDLE.getString("Reminders.all.print"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				InventoryFrame.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				ReminderPrinter.getInstance().printReminders(library.getOverdueLoans());
				InventoryFrame.this.setCursor(Cursor.getDefaultCursor());
			} catch (IOException | RuntimeException e1) {
				LOG.error("Error during reminder printing", e1);
				JOptionPane.showMessageDialog(InventoryFrame.this, BUNDLE.getString("Reminders.error.text"),
						BUNDLE.getString("Reminders.error.title"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private class ExportRemindersAction extends SaveFileAction {

		public ExportRemindersAction() {
			super(InventoryFrame.this);

			putValue(SHORT_DESCRIPTION, BUNDLE.getString("Reminders.export"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/report.png")));
			putValue(NAME, BUNDLE.getString("Reminders.export"));
		}

		@Override
		protected void saveToFile(File file) {
			if (!file.getName().endsWith(".pdf")) {
				file = new File(file.getParent(), file.getName() + ".pdf");
			}
			try {
				ReminderPrinter.getInstance().exportReminders(library.getOverdueLoans(), file);
			} catch (IOException | RuntimeException e) {
				LOG.error("Error during reminder export", e);
				JOptionPane.showMessageDialog(InventoryFrame.this, BUNDLE.getString("Reminders.error.text"),
						BUNDLE.getString("Reminders.error.title"), JOptionPane.ERROR_MESSAGE);
			}

			if (Desktop.isDesktopSupported() && file.exists()) {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					LOG.error("Failed to open generated reminders report using Desktop.open", e);
				}
			}
		}

		@Override
		protected void configureFileChooser(JFileChooser chooser) {
			chooser.setDialogTitle(BUNDLE.getString("Reminders.fileChooser.title"));
			chooser.setFileFilter(new FileNameExtensionFilter("PDFs (*.pdf)", "pdf"));
		}
	}

	private class PrintStatisticsAction extends AbstractAction {

		public PrintStatisticsAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.menu.statistics.print"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/printer.png")));
			putValue(NAME, BUNDLE.getString("InventoryFrame.menu.statistics.print"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				InventoryFrame.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				StatisticsPrinter.getInstance().printStatistics(library);
				InventoryFrame.this.setCursor(Cursor.getDefaultCursor());
			} catch (IOException | RuntimeException e1) {
				LOG.error("Error while printing statistics", e1);
				JOptionPane.showMessageDialog(InventoryFrame.this, BUNDLE.getString("Statistics.error.text"),
						BUNDLE.getString("Statistics.error.title"), JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	private class ExportStatisticsAction extends SaveFileAction {

		public ExportStatisticsAction() {
			super(InventoryFrame.this);

			putValue(SHORT_DESCRIPTION, BUNDLE.getString("Statistics.export"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/chart_pie.png")));
			putValue(NAME, BUNDLE.getString("InventoryFrame.menu.statistics.export"));
		}

		@Override
		protected void saveToFile(File file) {
			if (!file.getName().endsWith(".pdf")) {
				file = new File(file.getParent(), file.getName() + ".pdf");
			}
			try {
				StatisticsPrinter.getInstance().exportStatistics(library, file);
			} catch (IOException | RuntimeException e) {
				LOG.error("Error during reminder export", e);
				JOptionPane.showMessageDialog(InventoryFrame.this, BUNDLE.getString("Statistics.error.text"),
						BUNDLE.getString("Statistics.error.title"), JOptionPane.ERROR_MESSAGE);
			}

			if (Desktop.isDesktopSupported() && file.exists()) {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					LOG.error("Failed to open generated statistics report using Desktop.open", e);
				}
			}
		}

		@Override
		protected void configureFileChooser(JFileChooser chooser) {
			chooser.setDialogTitle(BUNDLE.getString("Statistics.fileChooser.title"));
			chooser.setFileFilter(new FileNameExtensionFilter("PDFs (*.pdf)", "pdf"));
		}

	}

	private static class ShowAboutAction extends AbstractAction {
		private AboutFrame aboutFrame = new AboutFrame();

		public ShowAboutAction() {
			putValue(NAME, BUNDLE.getString("InventoryFrame.menu.help.about"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/information.png")));
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.menu.help.about"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			aboutFrame.setVisible(true);
		}
	}
}
