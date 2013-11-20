package application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import actions.ReturnCopyAction;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import comp.RefreshableLabel;
import comp.UpdateListener;

import domain.Library;
import domain.Loan;

/**
 * Loan tab in the InventoryFrame.
 * 
 * 
 * @author Ch. Hüsler
 */
public class LoanInventoryPanel extends JPanel {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

	private Library library;

	private JTable loanList;

	private LoanDetailFrame loanDetailFrame;

	private JTextField loanFilter;

	private TableRowSorter<LoanTableModel> loanRowSorter;

	private Action showLoanDetailAction = new ShowLoanDetailAction();

	private Action addLoanAction = new AddLoanAction();

	private Action returnCopyAction = new ReturnCopyAction() {
		@Override
		protected void finishedOk() {
			loanRowSorter.sort();
			loanList.clearSelection();
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

	public LoanInventoryPanel(Library lib) {
		library = lib;
		loanDetailFrame = new LoanDetailFrame(lib);

		setLayout(new BorderLayout(0, 0));

		JPanel loanStatisticsPanel = new JPanel();
		loanStatisticsPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 0, 5), new TitledBorder(null, "Ausleih-Statistiken",
				TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		add(loanStatisticsPanel, BorderLayout.NORTH);
		FormLayout loanStatisticsPanelLayout =
				new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("max(100dlu;default)"),
						FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC, });
		loanStatisticsPanelLayout.setColumnGroups(new int[][] { new int[] { 2, 6, 4 } });
		loanStatisticsPanel.setLayout(loanStatisticsPanelLayout);

		final RefreshableLabel totalLoansLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				setText(String.format(BUNDLE.getString("InventoryFrame.totalLoans"), library.getLoans().size()));
			}
		};

		loanStatisticsPanel.add(totalLoansLabel, "2, 2");

		final RefreshableLabel currentLoansLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				setText(String.format(BUNDLE.getString("InventoryFrame.currentLoans"), library.getCurrentLoans().size()));
			}
		};
		loanStatisticsPanel.add(currentLoansLabel, "4, 2");

		final RefreshableLabel overdueLoansLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				setText(String.format(BUNDLE.getString("InventoryFrame.overdueLoans"), library.getOverdueLoans().size()));
			}
		};
		loanStatisticsPanel.add(overdueLoansLabel, "6, 2");

		library.getLoans().addListEventListener(new ListEventListener<Loan>() {
			@Override
			public void listChanged(ca.odell.glazedlists.event.ListEvent<Loan> listChanges) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						totalLoansLabel.refresh();
						currentLoansLabel.refresh();
						overdueLoansLabel.refresh();
					}
				});
			};
		});

		JPanel loanListPanel = new JPanel();
		loanListPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new TitledBorder(null, BUNDLE
				.getString("InventoryFrame.enteredLoans"), TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		add(loanListPanel, BorderLayout.CENTER);
		FormLayout loanListPanelLayout =
				new FormLayout(new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("max(250dlu;min)"),
						FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
						ColumnSpec.decode("max(68dlu;min)"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC,
						FormFactory.LABEL_COMPONENT_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, FormFactory.LABEL_COMPONENT_GAP_COLSPEC, },
						new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.LINE_GAP_ROWSPEC,
								RowSpec.decode("fill:default:grow"), FormFactory.LINE_GAP_ROWSPEC, });
		loanListPanelLayout.setColumnGroups(new int[][] { new int[] { 6, 8, 10 } });
		loanListPanel.setLayout(loanListPanelLayout);

		loanFilter = new JTextField();
		loanListPanel.add(loanFilter, "2, 2, fill, default");
		loanFilter.setColumns(10);

		JCheckBox showOverdueLoansOnly = new JCheckBox(BUNDLE.getString("InventoryFrame.showOverdueLoansOnly")); //$NON-NLS-1$
		loanListPanel.add(showOverdueLoansOnly, "4, 2");

		JButton returnCopyButton = new JButton(returnCopyAction);
		loanListPanel.add(returnCopyButton, "6, 2");

		JButton showSelectedLoanButton = new JButton(showLoanDetailAction);
		loanListPanel.add(showSelectedLoanButton, "8, 2");

		JButton addLoanButton = new JButton(addLoanAction);
		loanListPanel.add(addLoanButton, "10, 2");

		JScrollPane scrollPane = new JScrollPane();
		loanListPanel.add(scrollPane, "2, 4, 9, 1, fill, fill");

		LoanTableModel loanTableModel = new LoanTableModel(library);
		loanList = new JTable(loanTableModel);

		loanList.getColumnModel().removeColumn(loanList.getColumnModel().getColumn(0)); // isLent column

		loanList.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component
					getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				boolean overdue = (Boolean) value;

				ImageIcon icon = new ImageIcon(BookDetailFrame.class.getResource("/silk/" + (overdue ? "error" : "tick") + ".png"));
				String text = BUNDLE.getString("InventoryFrame.loanList.state." + (overdue ? "overdue" : "ok"));

				label.setText(text);
				label.setIcon(icon);

				return label;
			}
		});

		loanList.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component
					getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				label.setText(((DateTime) value).toString(DateTimeFormat.mediumDate()));
				return label;
			}
		});

		loanList.getColumnModel().getColumn(0).setPreferredWidth(80);
		loanList.getColumnModel().getColumn(0).setMaxWidth(80);
		loanList.getColumnModel().getColumn(1).setPreferredWidth(100);
		loanList.getColumnModel().getColumn(1).setMaxWidth(100);
		loanList.getColumnModel().getColumn(2).setPreferredWidth(600);
		loanList.getColumnModel().getColumn(3).setPreferredWidth(100);
		loanList.getColumnModel().getColumn(4).setPreferredWidth(150);

		scrollPane.setViewportView(loanList);

		loanList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean enabled = !loanList.getSelectionModel().isSelectionEmpty();
				showLoanDetailAction.setEnabled(enabled && loanList.getSelectedRowCount() == 1); // single selection only
				returnCopyAction.setEnabled(enabled);
			}
		});

		loanList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					showLoanDetailAction.actionPerformed(null);
				}
			}
		});

		loanRowSorter = new TableRowSorter<LoanTableModel>(loanTableModel);
		loanRowSorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(4, SortOrder.ASCENDING), new RowSorter.SortKey(2, SortOrder.ASCENDING)));

		loanList.setRowSorter(loanRowSorter);

		LoanTableRowFilter rowFilter = new LoanTableRowFilter();
		loanRowSorter.setRowFilter(rowFilter);

		loanDetailFrame.addUpdateListener(new UpdateListener() {
			@Override
			public void update() {
				loanRowSorter.sort(); // sort list when loans get updated (i.e. returned)

				currentLoansLabel.refresh();
				overdueLoansLabel.refresh();
				totalLoansLabel.refresh();
			}
		});

		Bindings.bind(loanFilter, new PropertyAdapter<RowFilter<?, ?>>(rowFilter, "stringFilter"));
		Bindings.bind(showOverdueLoansOnly, new PropertyAdapter<RowFilter<?, ?>>(rowFilter, "showOverdueOnly"));

	}

	private static class LoanTableModel extends AbstractTableAdapter<Loan> {
		public LoanTableModel(Library library) {
			super(new DefaultEventListModel<>(library.getLoans()), "INVISIBLE-isLent", BUNDLE.getString("InventoryFrame.loanList.state"), BUNDLE
					.getString("InventoryFrame.loanList.copyId"), BUNDLE.getString("InventoryFrame.loanList.title"), BUNDLE
					.getString("InventoryFrame.loanList.dueDate"), BUNDLE.getString("InventoryFrame.loanList.customerName"));
		}

		@Override
		public Object getValueAt(int rowIndex, int colIndex) {
			Loan loan = getRow(rowIndex);

			switch (colIndex) {
				case 0:
					return loan.isLent(); // used for filtering
				case 1:
					return loan.isOverdue();
				case 2:
					return loan.getCopy().getInventoryNumber();
				case 3:
					return loan.getCopy().getTitle().getTitle();
				case 4:
					return loan.getDueDate();
				case 5:
					return String.format("%s %s", loan.getCustomer().getSurname(), loan.getCustomer().getFirstName());
			}

			return null;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
				case 1:
					return Boolean.class;
				case 2:
					return Integer.class;
				case 4:
					return DateTime.class;
				default:
					return String.class;
			}
		}
	}

	public class LoanTableRowFilter extends RowFilter<LoanTableModel, Integer> {

		private String stringFilter;
		private boolean showOverdueOnly;
		private Pattern pattern;

		@Override
		public boolean include(Entry<? extends LoanTableModel, ? extends Integer> entry) {
			if (!(boolean) entry.getValue(0)) { // book is not lent
				return false;
			}
			if (showOverdueOnly && !(boolean) entry.getValue(1)) {
				return false;
			}

			if (pattern != null) {
				boolean found = false;
				for (int i = 2; i < entry.getValueCount(); i++) { // don't need 0 or 1, that's covered above
					if (pattern.matcher(entry.getStringValue(i)).find()) {
						found = true;
					}
				}
				return found;
			}

			return true;
		}

		public String getStringFilter() {
			return stringFilter;
		}

		public void setStringFilter(String stringFilter) {
			this.stringFilter = stringFilter;

			try {
				pattern = Pattern.compile("(?i:\\Q" + stringFilter + "\\E)");
			} catch (PatternSyntaxException e) {
				pattern = null;
			}
			loanRowSorter.setRowFilter(this); // needed to refilter the list
		}

		public boolean isShowOverdueOnly() {
			return showOverdueOnly;
		}

		public void setShowOverdueOnly(boolean showOverdueOnly) {
			this.showOverdueOnly = showOverdueOnly;
			loanRowSorter.setRowFilter(this); // needed to refilter the list
		}

	}

	private class ShowLoanDetailAction extends AbstractAction {
		public ShowLoanDetailAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.showLoanDetail.description"));
			putValue(NAME, BUNDLE.getString("InventoryFrame.showLoanDetail.name"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/zoom.png")));
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			loanDetailFrame.showLoan(library.getLoans().get(loanList.convertRowIndexToModel(loanList.getSelectedRow())));
			loanDetailFrame.setVisible(true);
		}
	}

	private class AddLoanAction extends AbstractAction {
		public AddLoanAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.addLoan.description"));
			putValue(NAME, BUNDLE.getString("InventoryFrame.addLoan.name"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/add.png")));
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			loanDetailFrame.createLoan();
			loanDetailFrame.setVisible(true);
		}
	}

}
