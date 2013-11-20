package application;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import comp.FilteringComboBox;
import comp.UpdateListener;

import domain.Copy;
import domain.Copy.Condition;
import domain.Customer;
import domain.Library;
import domain.Loan;

/**
 * Detail frame for loans.
 * 
 * @author Ch. Hüsler
 */
public class LoanDetailFrame extends JFrame {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages"); //$NON-NLS-1$

	private JPanel contentPane;
	private JTable copyList;
	private JLabel returnDateField;

	private CustomerLoanPanel loanPanel;

	private Library library;

	private FilteringComboBox<Customer> customerField;

	private JLabel copyStatusLabel;

	private LendCopyAction lendCopyAction = new LendCopyAction();

	private TableRowSorter<CopyTableModel> copyRowSorter;

	private CopyTableModel copyTableModel;

	private CopyRowFilter copyFilter;

	private JTextField copyFilterField;

	/**
	 * Create the frame.
	 */
	public LoanDetailFrame(Library lib) {
		this.library = lib;

		loanPanel = new CustomerLoanPanel(lib);

		initialize();
	}

	public void createLoan() {
		customerField.resetFilter();
		customerField.getModel().setSelectedItem(null);

		loanPanel.setCustomer(null);

		setTitle(BUNDLE.getString("LoanDetailFrame.title.new"));
	}

	public void showLoan(Loan loan) {
		customerField.resetFilter();
		customerField.getModel().setSelectedItem(loan.getCustomer());

		setTitle(String.format(BUNDLE.getString("LoanDetailFrame.title"), loan.getCustomer().getSurname() + " "
				+ loan.getCustomer().getFirstName()));
	}

	private void initialize() {
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		setMinimumSize(new Dimension(720, 550));
		setPreferredSize(new Dimension(750, 550));

		setLocationRelativeTo(null);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec.decode("default:grow"), }, new RowSpec[] {
				RowSpec.decode("max(150dlu;min)"), RowSpec.decode("min:grow"), }));

		JPanel customerSelectionPanel = new JPanel();
		customerSelectionPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 0, 5), new TitledBorder(null, BUNDLE
				.getString("LoanDetailFrame.customerSelectionTitle"), TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		contentPane.add(customerSelectionPanel, "1, 1, fill, fill");
		customerSelectionPanel
				.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("90px"),
						FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC, },
						new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC,
								RowSpec.decode("default:grow"), }));

		JLabel customerLabel = new JLabel(BUNDLE.getString("LoanDetailFrame.customer"));
		customerSelectionPanel.add(customerLabel, "2, 2");

		Comparator<Customer> customerComparator = new Comparator<Customer>() {
			@Override
			public int compare(Customer o1, Customer o2) {
				int surName = o1.getSurname().compareToIgnoreCase(o2.getSurname());
				return surName == 0 ? o1.getFirstName().compareToIgnoreCase(o2.getFirstName()) : surName;
			}
		};

		customerField = new FilteringComboBox<Customer>(new FilterList<>(new SortedList<Customer>(library.getCustomers(), customerComparator))) {
			@Override
			public Matcher<Customer> getMatcher(String text) {
				return new CustomerMatcher(text);
			}
		};

		customerSelectionPanel.add(customerField, "4, 2, fill, fill");

		customerSelectionPanel.add(loanPanel, "2, 4, 3, 1, fill, fill");

		customerField.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean selected = e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Customer;
				if (selected) {
					Customer customer = (Customer) e.getItem();
					loanPanel.setCustomer(customer);
				}
			}
		});

		JPanel loanCopyPanel = new JPanel();
		loanCopyPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 0, 5), new TitledBorder(null, BUNDLE
				.getString("LoanDetailFrame.lendCopyTitle"), TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		contentPane.add(loanCopyPanel, "1, 2, fill, fill");
		loanCopyPanel.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("90px"),
				FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("min(30dlu;default):grow"), FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
				FormFactory.LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, RowSpec.decode("default:grow"), FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC, }));

		JLabel copyIdLabel = new JLabel(BUNDLE.getString("LoanDetailFrame.copyId"));
		loanCopyPanel.add(copyIdLabel, "2, 2");

		copyFilterField = new JFormattedTextField(NumberFormat.getIntegerInstance());
		copyFilterField.setColumns(10);

		loanCopyPanel.add(copyFilterField, "4, 2, 5, 1, fill, center");

		copyStatusLabel = new JLabel();
		loanCopyPanel.add(copyStatusLabel, "6, 2");

		JLabel returnDateLabel = new JLabel(BUNDLE.getString("LoanDetailFrame.returnDate"));
		loanCopyPanel.add(returnDateLabel, "2, 5");

		returnDateField = new JLabel(DateTime.now().toString(DateTimeFormat.shortDate()));
		loanCopyPanel.add(returnDateField, "4, 5, fill, default");

		JScrollPane copyScrollPane = new JScrollPane();
		loanCopyPanel.add(copyScrollPane, "2, 3, 7, 1");

		copyTableModel = new CopyTableModel(library);
		copyList = new JTable(copyTableModel);
		copyScrollPane.setViewportView(copyList);

		copyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		copyList.getColumnModel().getColumn(0).setPreferredWidth(80);
		copyList.getColumnModel().getColumn(0).setMaxWidth(80);
		copyList.getColumnModel().getColumn(1).setPreferredWidth(50);
		copyList.getColumnModel().getColumn(2).setPreferredWidth(50);

		copyRowSorter = new TableRowSorter<CopyTableModel>(copyTableModel);
		copyList.setRowSorter(copyRowSorter);

		copyFilter = new CopyRowFilter();
		copyRowSorter.setRowFilter(copyFilter);

		copyList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				lendCopyAction.checkStatus();
			}
		});

		Bindings.bind(copyFilterField, new PropertyAdapter<RowFilter<?, ?>>(copyFilter, "filter"));

		JButton lendCopyButton = new JButton(lendCopyAction);
		loanCopyPanel.add(lendCopyButton, "8, 5");

		loanPanel.addStatusListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				copyFilter.setEnabled((boolean) evt.getNewValue());
				copyFilterField.setEnabled((boolean) evt.getNewValue());

				lendCopyAction.checkStatus();
			}
		});

		loanPanel.addUpdateListener(new UpdateListener() {
			@Override
			public void update() {
				copyRowSorter.sort();
			}
		});
	}

	public void addUpdateListener(UpdateListener l) {
		loanPanel.addUpdateListener(l);
	}

	public void removeUpdateListener(UpdateListener l) {
		loanPanel.removeUpdateListener(l);
	}

	private class CopyTableModel extends AbstractTableAdapter<Copy> {

		public CopyTableModel(Library library) {
			super(new DefaultEventListModel<>(library.getCopies()), BUNDLE.getString("LoanDetailFrame.copyList.inventoryNumber"), BUNDLE
					.getString("LoanDetailFrame.copyList.title"), BUNDLE.getString("LoanDetailFrame.copyList.author"), BUNDLE
					.getString("LoanDetailFrame.copyList.condition"));
		}

		@Override
		public Object getValueAt(int row, int col) {
			Copy copy = getRow(row);
			switch (col) {
				case 0:
					return copy.getInventoryNumber();
				case 1:
					return copy.getTitle().getTitle();
				case 2:
					return copy.getTitle().getAuthor();
				case 3:
					return copy.getCondition();
			}
			return null;
		}
	}

	public class CopyRowFilter extends RowFilter<CopyTableModel, Integer> {
		private String filter;
		private Pattern pattern;

		private Copy copy;

		boolean enabled;

		@Override
		public boolean include(Entry<? extends CopyTableModel, ? extends Integer> entry) {
			if (!enabled) {
				return false;
			}

			if (copy != null) {
				return copy.getInventoryNumber() == (long) entry.getValue(0);
			}

			for (Loan loan : library.getLoans()) { // check if loan is lent
				if (loan.isLent() && (long) entry.getValue(0) == loan.getCopy().getInventoryNumber()) {
					return false;
				}
			}

			if (entry.getValue(3) == Condition.WASTE || entry.getValue(3) == Condition.LOST) {
				return false;
			}

			if (pattern == null) {
				return true;
			}

			boolean found = false;
			for (int i = 0; i < entry.getValueCount(); i++) {
				if (pattern.matcher(entry.getStringValue(i)).find()) {
					found = true;
				}
			}

			return found;
		}

		public String getFilter() {
			return filter;
		}

		public void setFilter(String filter) {
			this.filter = filter;

			try {
				pattern = Pattern.compile("\\Q" + filter + "\\E", Pattern.CASE_INSENSITIVE);
			} catch (PatternSyntaxException e) {
				pattern = null;
			}

			copyRowSorter.setRowFilter(this);
		}

		public Copy getCopy() {
			return copy;
		}

		public void setCopy(Copy copy) {
			this.copy = copy;

			copyRowSorter.setRowFilter(this);
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			if (this.enabled != enabled) {
				this.enabled = enabled;
				copyRowSorter.setRowFilter(this);
			}
		}

	}

	private class LendCopyAction extends AbstractAction {
		public LendCopyAction() {
			putValue(NAME, BUNDLE.getString("LoanDetailFrame.lendCopy"));
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/silk/book_add.png")));
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Copy copy = copyTableModel.getRow(copyList.convertRowIndexToModel(copyList.getSelectedRow()));
			if (!library.isCopyLent(copy)) {
				Customer customer = (Customer) customerField.getSelectedItem();
				library.createAndAddLoan(customer, copy);
				copyFilterField.setText(null);
				copyList.clearSelection();
				copyRowSorter.sort();
				setEnabled(false);
			}
		}

		public void checkStatus() {
			if (copyList.getSelectedRow() >= 0) {
				Copy copy = copyTableModel.getRow(copyList.convertRowIndexToModel(copyList.getSelectedRow()));
				setEnabled(!library.isCopyLent(copy) && customerField.getSelectedItem() != null && loanPanel.isLoanStatusOk());
			} else {
				setEnabled(false);
			}
		}
	}
}
