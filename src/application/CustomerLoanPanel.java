package application;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import actions.ReturnCopyAction;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import comp.UpdateListener;

import domain.Customer;
import domain.Library;
import domain.Loan;

/**
 * Loan history panel for a customer.
 * 
 * @author Ch. Hüsler
 */
public class CustomerLoanPanel extends JPanel {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

	private Library library;

	private JTable loanList;
	private TableRowSorter<LoanTableModel> loanSorter;
	private LoanRowFilter loanFilter;

	private ValueHolder customerLoanStatus = new ValueHolder(false);
	private Customer customer = null;
	private CustomerErrorLabel customerErrorLabel;

	private List<UpdateListener> updateListeners = new ArrayList<UpdateListener>();

	private ReturnCopyAction returnCopyAction = new ReturnCopyAction() {
		@Override
		protected void finishedOk() {
			loanList.clearSelection();
			checkCustomerLoanStatus();

			for (UpdateListener l : updateListeners) {
				l.update();
			}
		}

		@Override
		protected List<Loan> getSelectedLoans() {
			List<Loan> loans = new ArrayList<Loan>();

			for (int row : loanList.getSelectedRows()) {
				Loan loan = library.getLoans().get(loanList.convertRowIndexToModel(row));
				if (loan.isLent()) {
					loans.add(loan);
				}
			}

			return loans;
		}
	};

	public CustomerLoanPanel(Library lib) {
		setBorder(new EmptyBorder(5, 0, 5, 0));
		this.library = lib;

		final LoanTableModel loanTableModel = new LoanTableModel(library);
		setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.BUTTON_COLSPEC, }, new RowSpec[] { FormFactory.DEFAULT_ROWSPEC, FormFactory.MIN_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("fill:default:grow"), }));

		JLabel loanHistoryLabel = new JLabel(BUNDLE.getString("InventoryFrame.customer.loanHistory"));
		add(loanHistoryLabel, "1, 2");

		JButton returnCopyButton = new JButton(returnCopyAction);
		add(returnCopyButton, "3, 2");

		customerErrorLabel = new CustomerErrorLabel();
		customerErrorLabel.setVisible(false);
		add(customerErrorLabel, "1, 1, 3, 1");
		loanList = new JTable(loanTableModel);

		JScrollPane scrollPane = new JScrollPane(loanList);
		add(scrollPane, "1, 4, 3, 1, fill, fill");

		loanSorter = new TableRowSorter<LoanTableModel>(loanTableModel);
		loanList.setRowSorter(loanSorter);

		loanFilter = new LoanRowFilter();
		loanSorter.setRowFilter(loanFilter);

		loanSorter.setComparator(0, new Comparator<Loan>() {
			@Override
			public int compare(Loan o1, Loan o2) {
				if (o1.isLent() != o2.isLent()) {
					return o1.isLent() ? -1 : 1;
				} else if (o1.getReturnDate() == null) {
					if (o2.getReturnDate() == null) {
						return o2.getDueDate().compareTo(o1.getDueDate());
					} else {
						return -1;
					}
				} else {
					if (o2.getReturnDate() == null) {
						return 1;
					} else {
						return o2.getReturnDate().compareTo(o1.getReturnDate());
					}
				}
			}
		});
		loanSorter.toggleSortOrder(0);

		addUpdateListener(new UpdateListener() {
			@Override
			public void update() {
				loanSorter.sort();
			}
		});

		loanList.getColumnModel().getColumn(0).setMinWidth(220);
		loanList.getColumnModel().getColumn(1).setMinWidth(80);
		loanList.getColumnModel().getColumn(2).setMinWidth(50);
		loanList.getColumnModel().getColumn(3).setMinWidth(300);
		loanList.getColumnModel().getColumn(3).setPreferredWidth(500);

		loanList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				Loan selected = null;
				if (!loanList.getSelectionModel().isSelectionEmpty()) {
					selected = library.getLoans().get(loanList.convertRowIndexToModel(loanList.getSelectedRow()));
				}
				returnCopyAction.setEnabled(selected != null && selected.isLent());
			}
		});

		LoanTableCellRenderer renderer = new LoanTableCellRenderer();

		for (TableColumn col : Collections.list(loanList.getColumnModel().getColumns())) {
			col.setCellRenderer(renderer);
		}

		library.getLoans().addListEventListener(new ListEventListener<Loan>() {
			@Override
			public void listChanged(ListEvent<Loan> e) {
				while (e.next()) {
					if (library.getLoans().get(e.getIndex()).getCustomer().equals(customer)) {
						checkCustomerLoanStatus();
						break;
					}
				}
			}
		});
	}

	private void checkCustomerLoanStatus() {

		if (customer == null) {
			customerErrorLabel.setVisible(false);
			customerLoanStatus.setValue(false);
		} else {
			List<Loan> loans = library.getCustomerLoans(customer);
			int current = 0;
			boolean overdue = false;

			for (Loan loan : loans) {
				if (loan.isLent()) {
					current++;
					if (loan.isOverdue()) {
						overdue = true;
						break;
					}
				}
			}

			customerLoanStatus.setValue(!overdue && current < 3);

			if (overdue) {
				customerErrorLabel.showOverdueError();
			} else if (current >= 3) {
				customerErrorLabel.showCountError();
			} else {
				customerErrorLabel.setVisible(false);
			}
		}
	}

	public void addStatusListener(PropertyChangeListener l) {
		customerLoanStatus.addPropertyChangeListener(l);
	}

	public void removeStatusListener(PropertyChangeListener l) {
		customerLoanStatus.removePropertyChangeListener(l);
	}

	public void addUpdateListener(UpdateListener l) {
		updateListeners.add(l);
	}

	public void removeUpdateListener(UpdateListener l) {
		updateListeners.remove(l);
	}

	public boolean isLoanStatusOk() {
		return customerLoanStatus.booleanValue();
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;

		loanFilter.setCustomer(customer);
		loanSorter.sort();

		checkCustomerLoanStatus();
	}

	private final class LoanTableCellRenderer extends DefaultTableCellRenderer {
		private Color overdue = new Color(0xFFB0B0);
		private Color overdueSelected = new Color(0xFF7474);
		private Color lent = new Color(0x79FFA3);
		private Color lentSelected = new Color(0x3DE06F);

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			Loan loan = (Loan) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);

			if (loanList.convertColumnIndexToModel(column) == 0) {
				if (loan.isOverdue()) {
					label.setText(String.format(BUNDLE.getString("Loan.state.overdue"), loan.getDueDateString()));
				} else if (loan.isLent()) {
					label.setText(String.format(BUNDLE.getString("Loan.state.lent"), loan.getDueDateString()));
				} else {
					label.setText(String.format(BUNDLE.getString("Loan.state.returned"), loan.getReturnDateString()));
				}
			}

			label.setForeground(isSelected ? table.getSelectionForeground() : loan.isLent() ? Color.BLACK : Color.DARK_GRAY);

			if (loan.isOverdue()) {
				label.setBackground(isSelected ? overdueSelected : overdue);
			} else if (loan.isLent()) {
				label.setBackground(isSelected ? lentSelected : lent);
			} else {
				label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			}

			return label;
		}
	}

	private class LoanTableModel extends AbstractTableAdapter<Loan> {
		public LoanTableModel(Library library) {
			super(new DefaultEventListModel<>(library.getLoans()), BUNDLE.getString("LoanDetailFrame.loanList.state"), BUNDLE
					.getString("LoanDetailFrame.loanList.condition"), BUNDLE.getString("LoanDetailFrame.loanList.inventoryNumber"), BUNDLE
					.getString("LoanDetailFrame.loanList.title"));
		}

		@Override
		public Object getValueAt(int row, int col) {
			Loan loan = getRow(row);

			switch (col) {
				case 0:
					return loan;
				case 1:
					return loan.getCopy().getCondition();
				case 2:
					return loan.getCopy().getInventoryNumber();
				case 3:
					return loan.getCopy().getTitle().getTitle();
			}

			return null;
		}
	}

	public class LoanRowFilter extends RowFilter<LoanTableModel, Integer> {
		private Customer customer;

		@Override
		public boolean include(Entry<? extends LoanTableModel, ? extends Integer> entry) {
			return customer != null && customer == ((Loan) entry.getValue(0)).getCustomer();
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
			loanSorter.setRowFilter(this);
		}
	}

	private static class CustomerErrorLabel extends JLabel {
		private static final String COUNT_ERROR_MESSAGE = BUNDLE.getString("LoanDetailFrame.errorCustomerCopyCount");
		private static final String OVERDUE_ERROR_MESSAGE = BUNDLE.getString("LoanDetailFrame.errorCustomerOverdueLoan");

		public CustomerErrorLabel() {
			super(COUNT_ERROR_MESSAGE, new ImageIcon(LoanDetailFrame.class.getResource("/silk/cancel.png")), JLabel.LEADING);
		}

		public void showCountError() {
			setText(COUNT_ERROR_MESSAGE);
			setVisible(true);
		}

		public void showOverdueError() {
			setText(OVERDUE_ERROR_MESSAGE);
			setVisible(true);
		}
	}
}
