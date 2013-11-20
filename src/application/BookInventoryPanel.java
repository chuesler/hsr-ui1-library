package application;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import ca.odell.glazedlists.event.ListEvent;
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

import domain.Book;
import domain.Copy;
import domain.Library;
import domain.Shelf;

/**
 * Books tab in the InventoryFrame.
 * 
 * @author Ch. Hüsler
 */
public class BookInventoryPanel extends JPanel {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

	private Library library;
	private JTable bookList;

	private TableRowSorter<BookTableModel> bookRowSorter;

	private Action addBookAction = new AddBookAction();
	private Action showBookDetailAction = new ShowBookDetailAction();

	private JTextField bookFilter;

	private BookDetailFrame bookDetailFrame;

	private BookTableModel bookTableModel;

	private BookUpdateListener updateListener;

	public BookInventoryPanel(Library lib) {
		library = lib;
		bookDetailFrame = new BookDetailFrame(lib);

		updateListener = new BookUpdateListener();

		setLayout(new BorderLayout());

		JPanel bookStatisticsPanel = new JPanel();
		bookStatisticsPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 0, 5), new TitledBorder(UIManager
				.getBorder("TitledBorder.border"), BUNDLE.getString("InventoryFrame.statistics"), TitledBorder.LEADING, TitledBorder.TOP, null,
				null)));
		add(bookStatisticsPanel, BorderLayout.NORTH);
		FormLayout statisticsPanelLayout =
				new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("max(77dlu;default)"),
						FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("100dlu"), }, new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC, });
		statisticsPanelLayout.setColumnGroups(new int[][] { new int[] { 2, 4 } });
		bookStatisticsPanel.setLayout(statisticsPanelLayout);

		final RefreshableLabel totalBooksLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				setText(String.format(BUNDLE.getString("InventoryFrame.totalBooks"), library.getBooks().size()));
			}
		};
		bookStatisticsPanel.add(totalBooksLabel, "2, 2");

		final RefreshableLabel totalCopiesLabel = new RefreshableLabel() {
			@Override
			public void refresh() {
				setText(String.format(BUNDLE.getString("InventoryFrame.totalCopies"), library.getCopies().size()));
			}
		};
		bookStatisticsPanel.add(totalCopiesLabel, "4, 2");

		JPanel bookListPanel = new JPanel();
		bookListPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new TitledBorder(UIManager.getBorder("TitledBorder.border"),
				BUNDLE.getString("InventoryFrame.bookInventory"), TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		add(bookListPanel, BorderLayout.CENTER);
		FormLayout bookListPanelLayout =
				new FormLayout(new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("max(250dlu;min)"),
						FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
						ColumnSpec.decode("max(68dlu;default)"), FormFactory.LABEL_COMPONENT_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC,
						FormFactory.LABEL_COMPONENT_GAP_COLSPEC, }, new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("fill:23px:grow"),
						FormFactory.LINE_GAP_ROWSPEC, });
		bookListPanelLayout.setColumnGroups(new int[][] { new int[] { 8, 6 } });
		bookListPanel.setLayout(bookListPanelLayout);

		JButton showSelectedBookButton = new JButton(showBookDetailAction);
		bookListPanel.add(showSelectedBookButton, "6, 2, fill, fill");

		bookFilter = new JTextField();
		bookListPanel.add(bookFilter, "2, 2, fill, default");
		bookFilter.setColumns(10);
		bookFilter.requestFocusInWindow();

		JCheckBox showAvailableBooksOnly = new JCheckBox(BUNDLE.getString("InventoryFrame.showAvailableBooksOnly")); //$NON-NLS-1$
		bookListPanel.add(showAvailableBooksOnly, "4, 2");

		JButton addBookButton = new JButton(addBookAction);
		bookListPanel.add(addBookButton, "8, 2, fill, center");

		bookTableModel = new BookTableModel(library);
		bookList = new JTable(bookTableModel);

		bookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// set column widths
		bookList.getColumnModel().getColumn(0).setPreferredWidth(80);
		bookList.getColumnModel().getColumn(0).setMaxWidth(80);
		bookList.getColumnModel().getColumn(1).setPreferredWidth(500);
		bookList.getColumnModel().getColumn(2).setPreferredWidth(100);
		bookList.getColumnModel().getColumn(3).setPreferredWidth(100);

		bookRowSorter = new TableRowSorter<>(bookTableModel);
		bookList.setRowSorter(bookRowSorter);

		bookList.getRowSorter().toggleSortOrder(1);

		RowFilter<BookTableModel, Integer> rowFilter = new BookTableRowFilter();

		// table filter
		Bindings.bind(bookFilter, new PropertyAdapter<RowFilter<?, ?>>(rowFilter, "stringFilter"));
		Bindings.bind(showAvailableBooksOnly, new PropertyAdapter<RowFilter<?, ?>>(rowFilter, "showAvailableOnly"));

		JScrollPane scrollPane = new JScrollPane(bookList);
		bookListPanel.add(scrollPane, "2, 4, 7, 1, fill, fill");

		// only enable the showSelected button if there actually is a selection
		bookList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean hasSelection = !bookList.getSelectionModel().isSelectionEmpty();
				showBookDetailAction.setEnabled(hasSelection);
			}
		});

		bookList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					showBookDetailAction.actionPerformed(null);
				}
			}
		});

		library.getBooks().addListEventListener(new ListEventListener<Book>() {
			@Override
			public void listChanged(ListEvent<Book> e) {
				totalBooksLabel.refresh();
			}
		});

		library.getCopies().addListEventListener(new ListEventListener<Copy>() {
			@Override
			public void listChanged(ListEvent<Copy> e) {
				totalCopiesLabel.refresh();

				// handle "copy" field update
				if (!bookList.getSelectionModel().isSelectionEmpty()) {
					int selectedModelIndex = bookList.convertRowIndexToModel(bookList.getSelectedRow());
					Book selected = bookTableModel.getRow(selectedModelIndex);
					while (e.next()) {
						if (library.getCopies().get(e.getIndex()).getTitle().equals(selected)) {
							bookTableModel.fireTableCellUpdated(selectedModelIndex, 0);
						}
					}
				}
			}
		});

	}

	private class BookUpdateListener implements PropertyChangeListener {
		private Book book;
		private int index;

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			bookTableModel.fireTableRowsUpdated(index, index);
		}

		public void reattach(Book book, int index) {
			if (this.book != null) {
				this.book.removePropertyChangeListener(this);
			}
			this.book = book;
			this.index = index;
			book.addPropertyChangeListener(this);
		}
	}

	private static class BookTableModel extends AbstractTableAdapter<Book> {
		private Library library;

		public BookTableModel(Library library) {
			super(new DefaultEventListModel<Book>(library.getBooks()), new String[] { BUNDLE.getString("InventoryFrame.bookList.available"),
					BUNDLE.getString("InventoryFrame.bookList.title"), BUNDLE.getString("InventoryFrame.bookList.author"),
					BUNDLE.getString("InventoryFrame.bookList.publisher"), BUNDLE.getString("InventoryFrame.bookList.shelf") });
			this.library = library;

			library.getCopies().addListEventListener(new ListEventListener<Copy>() {
				@Override
				public void listChanged(ListEvent<Copy> e) {

				}
			});
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Book book = getRow(rowIndex);

			switch (columnIndex) {
				case 0:
					return library.getAvailableCopiesOfBook(book).size();
				case 1:
					return book.getTitle();
				case 2:
					return book.getAuthor();
				case 3:
					return book.getPublisher();
				case 4:
					return book.getShelf();
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return Integer.class;
				case 4:
					return Shelf.class;
				default:
					return String.class;
			}
		}
	}

	public class BookTableRowFilter extends RowFilter<BookTableModel, Integer> {
		private String stringFilter;
		private boolean showAvailableOnly;
		private Pattern pattern;

		@Override
		public boolean include(Entry<? extends BookTableModel, ? extends Integer> entry) {
			if (showAvailableOnly && (Integer) entry.getValue(0) == 0) {
				return false;
			}

			if (pattern != null) {
				boolean found = false;
				for (int i = 1; i < entry.getValueCount(); i++) { // don't need 0, that's covered above
					if (pattern.matcher(entry.getStringValue(i)).find()) {
						found = true;
					}
				}
				return found;
			}

			return true;
		}

		public boolean isShowAvailableOnly() {
			return showAvailableOnly;
		}

		public void setShowAvailableOnly(boolean showAvailableOnly) {
			this.showAvailableOnly = showAvailableOnly;
			bookRowSorter.setRowFilter(this); // refilter
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
			bookRowSorter.setRowFilter(this); // needed to refilter the list
		}
	}

	private class AddBookAction extends AbstractAction {
		public AddBookAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.addBook.description"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/book_add.png")));
			putValue(NAME, BUNDLE.getString("InventoryFrame.addBook.name"));
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			bookDetailFrame.showBook(null);
			bookDetailFrame.setVisible(true);
		}
	}

	private class ShowBookDetailAction extends AbstractAction {
		public ShowBookDetailAction() {
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.showBookDetail.description"));
			putValue(NAME, BUNDLE.getString("InventoryFrame.showBookDetail.name"));
			putValue(SMALL_ICON, new ImageIcon(InventoryFrame.class.getResource("/silk/book_edit.png")));
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = bookList.convertRowIndexToModel(bookList.getSelectedRow());
			Book selectedBook = library.getBooks().get(index);
			updateListener.reattach(selectedBook, index);
			bookDetailFrame.showBook(selectedBook);
			bookDetailFrame.setVisible(true);
		}
	}
}
