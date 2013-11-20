package application;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;

import reports.ReminderPrinter;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.beans.PropertyConnector;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import comp.RefreshableLabel;
import comp.UpdateListener;

import domain.Customer;
import domain.Library;
import domain.Loan;

/**
 * Customer tab in the InventoryFrame.
 * 
 * @author Ch. Hüsler
 */
public class CustomerInventoryPanel extends JPanel {

	private static final Logger LOG = Logger.getLogger(CustomerInventoryPanel.class);

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

	private Library library;

	private JTable customerList;

	private PresentationModel<Customer> customerDetailModel = new PresentationModel<>();

	private Action addCustomerAction = new AddCustomerAction();

	private JTextField customerFilter;
	private JTextField customerFirstNameField;
	private JTextField customerSurnameField;
	private JTextField customerStreetField;
	private JTextField customerZipCodeField;
	private JTextField customerCityField;

	private CustomerLoanPanel customerLoanPanel;

	private TableRowSorter<CustomerTableModel> customerSorter;

	private Action printReminderAction = new PrintReminderAction();
	private Action saveCustomerAction = new SaveCustomerAction();

	private CustomerTableModel customerTableModel;

	public CustomerInventoryPanel(Library lib) {
		this.library = lib;

		this.customerLoanPanel = new CustomerLoanPanel(library);

		setLayout(new BorderLayout(0, 0));

		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control N"), "addCustomerAction");
		getActionMap().put("addCustomerAction", addCustomerAction);

		JPanel customerStatisticsPanel = new JPanel();
		customerStatisticsPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 0, 5), new TitledBorder(null, "Kunden-Statistiken",
				TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		add(customerStatisticsPanel, BorderLayout.NORTH);
		FormLayout customerStatisticsPanelLayout =
				new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("max(100dlu;default)"),
						FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("max(100dlu;default)"), }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"), FormFactory.RELATED_GAP_ROWSPEC, });
		customerStatisticsPanelLayout.setColumnGroups(new int[][] { new int[] { 2, 4 } });
		customerStatisticsPanel.setLayout(customerStatisticsPanelLayout);

		final RefreshableLabel totalCustomersLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				setText(String.format(BUNDLE.getString("CustomerInventoryPanel.totalCustomers"), library.getCustomers().size()));
			}
		};
		customerStatisticsPanel.add(totalCustomersLabel, "2, 2");

		final RefreshableLabel overdueCustomersLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				Set<Customer> customers = new HashSet<>();
				for (Loan loan : library.getOverdueLoans()) {
					customers.add(loan.getCustomer());
				}

				setText(String.format(BUNDLE.getString("InventoryFrame.overdueCustomers"), customers.size()));
			}
		};
		customerStatisticsPanel.add(overdueCustomersLabel, "4, 2, fill, fill");

		library.getCustomers().addListEventListener(new ListEventListener<Customer>() {
			@Override
			public void listChanged(ListEvent<Customer> listChanges) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						totalCustomersLabel.refresh();
						overdueCustomersLabel.refresh();
					};
				});
			}
		});

		JPanel customerListPanel = new JPanel();
		customerListPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new TitledBorder(null, "Erfasste Kunden",
				TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		add(customerListPanel, BorderLayout.CENTER);
		customerListPanel.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(250dlu;default):grow"), FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("max(300dlu;default)"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC, },
				new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC,
						RowSpec.decode("default:grow"), FormFactory.LINE_GAP_ROWSPEC, }));

		customerFilter = new JTextField();
		customerListPanel.add(customerFilter, "2, 2, fill, default");
		customerFilter.setColumns(10);

		JScrollPane scrollPane = new JScrollPane();
		customerListPanel.add(scrollPane, "2, 4, fill, fill");

		customerTableModel = new CustomerTableModel(library);
		customerList = new JTable(customerTableModel);
		scrollPane.setViewportView(customerList);

		customerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		customerList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (customerList.getSelectedRow() < 0) {
					return;
				}
				Customer selected = library.getCustomers().get(customerList.convertRowIndexToModel(customerList.getSelectedRow()));

				if (customerDetailModel.getBean() != null && customerDetailModel.isBuffering()) {
					if (selected.equals(customerDetailModel.getBean())) {
						return; // selection is still on the same customer
					}

					if (JOptionPane.showConfirmDialog(CustomerInventoryPanel.this,
							BUNDLE.getString("CustomerInventoryPanel.customer.confirmDiscard.text"),
							BUNDLE.getString("CustomerInventoryPanel.customer.confirmDiscard.title"), JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
						customerDetailModel.triggerFlush();

						customerDetailModel.setBean(selected);
					} else {
						int index = customerList.convertRowIndexToView(library.getCustomers().indexOf(customerDetailModel.getBean()));
						customerList.getSelectionModel().setSelectionInterval(index, index);
					}
				} else {
					customerDetailModel.setBean(selected);
				}
			}
		});

		customerDetailModel.addPropertyChangeListener(PresentationModel.PROPERTY_BEAN, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Customer customer = (Customer) evt.getNewValue();
				customerLoanPanel.setCustomer(customer);

				boolean hasOverdue = false;

				for (Loan loan : library.getCustomerLoans(customer)) {
					if (loan.isOverdue()) {
						hasOverdue = true;
						break;
					}
				}

				printReminderAction.setEnabled(hasOverdue);
			}
		});

		customerList.getColumnModel().getColumn(0).setPreferredWidth(70);
		customerList.getColumnModel().getColumn(1).setPreferredWidth(130);
		customerList.getColumnModel().getColumn(2).setPreferredWidth(130);
		customerList.getColumnModel().getColumn(3).setPreferredWidth(20);
		customerList.getColumnModel().getColumn(4).setPreferredWidth(100);

		customerSorter = new TableRowSorter<CustomerTableModel>(customerTableModel);
		customerList.setRowSorter(customerSorter);

		CustomerRowFilter customerRowFilter = new CustomerRowFilter();
		customerSorter.setRowFilter(customerRowFilter);

		customerSorter.toggleSortOrder(1);

		JPanel customerDetailPanel = new JPanel();
		customerDetailPanel.setBorder(null);
		customerListPanel.add(customerDetailPanel, "6, 2, 1, 3, fill, fill");
		FormLayout customerDetailLayout =
				new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), }, new RowSpec[] { FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.LINE_GAP_ROWSPEC,
						RowSpec.decode("default:grow"), });
		customerDetailLayout.setColumnGroups(new int[][] { new int[] { 4, 8 }, new int[] { 6, 2 } });
		customerDetailPanel.setLayout(customerDetailLayout);

		customerDetailPanel.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control S"), "saveCustomerAction");
		customerDetailPanel.getActionMap().put("saveCustomerAction", saveCustomerAction);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(null);
		customerDetailPanel.add(buttonPanel, "1, 1, 8, 1, fill, fill");
		FormLayout buttonPanelLayout =
				new FormLayout(new ColumnSpec[] { ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("max(68dlu;default)"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC,
						FormFactory.LABEL_COMPONENT_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] { FormFactory.DEFAULT_ROWSPEC, });
		buttonPanelLayout.setColumnGroups(new int[][] { new int[] { 7, 5, 3 } });
		buttonPanel.setLayout(buttonPanelLayout);

		JButton addCustomerButton = new JButton(addCustomerAction);
		buttonPanel.add(addCustomerButton, "3, 1");

		JButton saveCustomerButton = new JButton(saveCustomerAction);
		buttonPanel.add(saveCustomerButton, "5, 1");

		JButton printRemindersButton = new JButton(printReminderAction);
		buttonPanel.add(printRemindersButton, "7, 1, fill, top");

		JLabel customerFirstNameLabel = new JLabel(BUNDLE.getString("CustomerInventoryPanel.customer.firstName")); //$NON-NLS-1$
		customerDetailPanel.add(customerFirstNameLabel, "2, 3, left, default");

		customerFirstNameField = new JTextField();
		customerFirstNameLabel.setLabelFor(customerFirstNameField);
		customerDetailPanel.add(customerFirstNameField, "4, 3, fill, default");
		customerFirstNameField.setColumns(10);

		JLabel customerSurnameLabel = new JLabel(BUNDLE.getString("CustomerInventoryPanel.customer.surname")); //$NON-NLS-1$
		customerDetailPanel.add(customerSurnameLabel, "6, 3, left, default");

		customerSurnameField = new JTextField();
		customerSurnameLabel.setLabelFor(customerSurnameField);
		customerDetailPanel.add(customerSurnameField, "8, 3");
		customerSurnameField.setColumns(10);

		JLabel customerStreetLabel = new JLabel(BUNDLE.getString("CustomerInventoryPanel.customer.street")); //$NON-NLS-1$
		customerDetailPanel.add(customerStreetLabel, "2, 5, left, default");
		customerStreetLabel.setLabelFor(customerStreetField);

		customerStreetField = new JTextField();
		customerStreetLabel.setLabelFor(customerStreetField);
		customerDetailPanel.add(customerStreetField, "4, 5, 5, 1, fill, default");
		customerStreetField.setColumns(10);

		JLabel customerZipCodeLabel = new JLabel(BUNDLE.getString("CustomerInventoryPanel.customer.zip")); //$NON-NLS-1$
		customerDetailPanel.add(customerZipCodeLabel, "2, 7, left, default");

		NumberFormat zipFormat = NumberFormat.getIntegerInstance();
		zipFormat.setMinimumIntegerDigits(0);
		zipFormat.setMaximumIntegerDigits(4);
		zipFormat.setGroupingUsed(false);
		customerZipCodeField = BasicComponentFactory.createIntegerField(customerDetailModel.getBufferedModel(Customer.ZIP), zipFormat);
		customerZipCodeLabel.setLabelFor(customerZipCodeField);
		customerDetailPanel.add(customerZipCodeField, "4, 7, fill, default");
		customerZipCodeField.setColumns(10);

		JLabel customerCityLabel = new JLabel(BUNDLE.getString("CustomerInventoryPanel.customer.city")); //$NON-NLS-1$
		customerDetailPanel.add(customerCityLabel, "6, 7, left, default");

		customerCityField = new JTextField();
		customerCityLabel.setLabelFor(customerCityField);
		customerDetailPanel.add(customerCityField, "8, 7, fill, default");
		customerCityField.setColumns(10);

		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		customerListPanel.add(separator, "4, 2, 1, 3");

		JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
		customerDetailPanel.add(separator2, "2, 9, 7, 1");

		customerList.getSelectionModel().setSelectionInterval(0, 0);

		Bindings.bind(customerFirstNameField, customerDetailModel.getBufferedModel(Customer.FIRSTNAME));
		Bindings.bind(customerSurnameField, customerDetailModel.getBufferedModel(Customer.SURNAME));
		Bindings.bind(customerStreetField, customerDetailModel.getBufferedModel(Customer.STREET));
		Bindings.bind(customerCityField, customerDetailModel.getBufferedModel(Customer.CITY));

		customerDetailPanel.add(customerLoanPanel, "2, 11, 7, 1, fill, fill");

		Bindings.bind(customerFilter, new PropertyAdapter<RowFilter<?, ?>>(customerRowFilter, "stringFilter"));

		PropertyConnector.connect(customerDetailModel, "buffering", saveCustomerAction, "enabled");

		customerLoanPanel.addUpdateListener(new UpdateListener() {
			@Override
			public void update() {
				overdueCustomersLabel.refresh();
			}
		});
	}

	private static class CustomerTableModel extends AbstractTableAdapter<Customer> {
		public CustomerTableModel(Library library) {
			super(new DefaultEventListModel<>(library.getCustomers()), BUNDLE.getString("CustomerInventoryPanel.customer.firstName"), BUNDLE
					.getString("CustomerInventoryPanel.customer.surname"), BUNDLE.getString("CustomerInventoryPanel.customer.street"), BUNDLE
					.getString("CustomerInventoryPanel.customer.zip"), BUNDLE.getString("CustomerInventoryPanel.customer.city"));
		}

		@Override
		public Object getValueAt(int row, int column) {
			Customer customer = getRow(row);

			switch (column) {
				case 0:
					return customer.getFirstName();
				case 1:
					return customer.getSurname();
				case 2:
					return customer.getStreet();
				case 3:
					return customer.getZip();
				case 4:
					return customer.getCity();
			}

			return null;
		}
	}

	public class CustomerRowFilter extends RowFilter<CustomerTableModel, Integer> {
		private String stringFilter;
		private Pattern pattern;
		private Matcher matcher;

		@Override
		public boolean include(javax.swing.RowFilter.Entry<? extends CustomerTableModel, ? extends Integer> entry) {
			if (pattern == null) {
				return true;
			}

			for (int i = 0; i < entry.getValueCount(); i++) {
				if (matcher == null) {
					matcher = pattern.matcher(entry.getStringValue(i));
				} else {
					matcher.reset(entry.getStringValue(i));
				}

				if (matcher.find()) {
					return true;
				}
			}

			return false;
		}

		public String getStringFilter() {
			return stringFilter;
		}

		public void setStringFilter(String stringFilter) {
			this.stringFilter = stringFilter;

			try {
				pattern = Pattern.compile("\\Q" + stringFilter + "\\E", Pattern.CASE_INSENSITIVE);
			} catch (PatternSyntaxException e) {
				pattern = null;
			}

			matcher = null;

			customerSorter.setRowFilter(this);
		}
	}

	private class AddCustomerAction extends AbstractAction {

		public AddCustomerAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("CustomerInventoryPanel.addCustomer.description"));
			putValue(NAME, BUNDLE.getString("CustomerInventoryPanel.addCustomer.name"));
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/silk/add.png")));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (customerDetailModel.isBuffering()
					&& JOptionPane.showConfirmDialog(CustomerInventoryPanel.this,
							BUNDLE.getString("CustomerInventoryPanel.customer.confirmDiscard.text"),
							BUNDLE.getString("CustomerInventoryPanel.customer.confirmDiscard.title"), JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
				return;
			}

			// trigger flush get rid of our listener to avoid duplicate customer inserts due to multiple listeners being registered by spamming the
			// button
			customerDetailModel.triggerFlush();

			Customer c = new Customer("", "");
			customerList.clearSelection();
			customerDetailModel.setBean(c);

			customerDetailModel.getTriggerChannel().addValueChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getNewValue() == null) { // presentation model sets trigger value to null to allow PCL to fire again
						return;
					}

					customerDetailModel.getTriggerChannel().removeValueChangeListener(this);

					if (Boolean.TRUE.equals(evt.getNewValue())) { // true == commit, false == flush
						// TODO (Future): Add customer validation here

						int index = library.addCustomer(customerDetailModel.getBean());
						int row = customerList.convertRowIndexToView(index);

						customerList.getSelectionModel().setSelectionInterval(row, row);
					}
				}
			});
		}
	}

	private class PrintReminderAction extends AbstractAction {
		public PrintReminderAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("CustomerInventoryPanel.printReminder.description"));
			putValue(NAME, BUNDLE.getString("CustomerInventoryPanel.printReminder.name"));
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/silk/printer.png")));
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			Customer customer = library.getCustomers().get(customerList.convertRowIndexToModel(customerList.getSelectedRow()));

			List<Loan> overdueLoans = new ArrayList<>();
			for (Loan loan : library.getCustomerLoans(customer)) {
				if (loan.isOverdue()) {
					overdueLoans.add(loan);
				}
			}

			if (overdueLoans.isEmpty()) {
				LOG.warn("Print reminders button for customer " + customer.getFirstName() + " " + customer.getSurname()
						+ " was active even though there were no overdue loans");
				setEnabled(false);
				return;
			}

			try {
				ReminderPrinter.getInstance().printReminders(overdueLoans);
			} catch (IOException | RuntimeException e1) {
				LOG.error("Error during reminder printing", e1);
				JOptionPane.showMessageDialog(CustomerInventoryPanel.this, BUNDLE.getString("Reminders.error.text"),
						BUNDLE.getString("Reminders.error.title"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private class SaveCustomerAction extends AbstractAction {
		public SaveCustomerAction() {
			putValue(NAME, BUNDLE.getString("Save.name"));
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("Save.description"));
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/silk/disk.png")));
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			customerDetailModel.triggerCommit();

			if (!customerList.getSelectionModel().isSelectionEmpty()) {
				int row = customerList.convertRowIndexToModel(customerList.getSelectedRow());
				customerTableModel.fireTableRowsUpdated(row, row);
			}
		}
	}
}
