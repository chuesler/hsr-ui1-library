package application;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.validation.Validatable;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.view.ValidationComponentUtils;

import domain.Book;
import domain.Copy;
import domain.Copy.Condition;
import domain.Library;
import domain.Loan;
import domain.Shelf;

/**
 * Detail frame for books.
 * 
 * @author Ch. Hüsler
 */
public class BookDetailFrame extends JFrame {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages"); //$NON-NLS-1$

	private Library lib;

	private BookModel bookModel = new BookModel();

	private JPanel contentPane;
	private JTextArea titleField;
	private JTextField authorField;
	private JTextField publisherField;
	private JComboBox<Shelf> shelfField;

	private JTable copyList;

	private Action saveAction = new SaveAction();
	private Action addCopyAction = new AddCopyAction();

	private TableRowSorter<CopyTableModel> copySorter;
	private CopyRowFilter copyFilter;

	/**
	 * Create the frame.
	 */
	public BookDetailFrame(Library lib) {
		this.lib = lib;

		initialize();
	}

	public void showBook(Book book) {

		if (book == null) {
			book = new Book("");
			setTitle(BUNDLE.getString("BookDetailFrame.title.new"));
		} else {
			setTitle(String.format(BUNDLE.getString("BookDetailFrame.title.existing"), book.getTitle()));
		}
		bookModel.triggerFlush();
		bookModel.setBean(book);

		copyFilter.setBook(book);
		copyList.getSelectionModel().setSelectionInterval(0, 0);

		bookModel.validate();
	}

	private void initialize() {
		setTitle(BUNDLE.getString("BookDetailFrame.title.existing")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		setMinimumSize(new Dimension(500, 550));
		setPreferredSize(new Dimension(500, 450));

		setLocationRelativeTo(null);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!bookModel.isBuffering()
						|| JOptionPane.showConfirmDialog(BookDetailFrame.this, BUNDLE.getString("BookDetailFrame.confirmClose"),
								BUNDLE.getString("BookDetailFrame.confirmClose.title"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
					setVisible(false);
				}
			}
		});

		contentPane = new JPanel();

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec.decode("default:grow"), }, new RowSpec[] {
				FormFactory.DEFAULT_ROWSPEC, FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("fill:max(125dlu;min)"),
				RowSpec.decode("default:grow"), }));

		contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control S"), "saveBookAction");
		contentPane.getActionMap().put("saveBookAction", saveAction);

		JPanel detailPanel = new JPanel();
		detailPanel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new TitledBorder(UIManager.getBorder("TitledBorder.border"),
				"Buch-Informationen", TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		contentPane.add(detailPanel, "1, 3, fill, fill");
		detailPanel.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:max(31dlu;default)"),
				FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(70dlu;pref)"), FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:max(38dlu;default):grow"), FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.LINE_GAP_ROWSPEC, }));

		JLabel titleLabel = new JLabel(BUNDLE.getString("BookDetailFrame.book.title")); //$NON-NLS-1$
		detailPanel.add(titleLabel, "2, 2");

		JScrollPane titleScrollPane = new JScrollPane();
		titleScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		detailPanel.add(titleScrollPane, "4, 2, 3, 1, fill, fill");

		titleField = new JTextArea();
		titleField.setLineWrap(true);
		titleField.setWrapStyleWord(true);
		titleScrollPane.setViewportView(titleField);
		titleField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				// override textarea's default tab behavior - we don't need tab chars inside the title, but would like to be able to switch focus by
				// using the keyboard
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					getFocusOwner().transferFocus();
				}
			}
		});
		ValidationComponentUtils.setMandatory(titleField, true);
		ValidationComponentUtils.setMessageKey(titleField, "book.title");

		Bindings.bind(titleField, bookModel.getBufferedModel(Book.TITLE));

		JLabel authorLabel = new JLabel(BUNDLE.getString("BookDetailFrame.book.author")); //$NON-NLS-1$
		detailPanel.add(authorLabel, "2, 4");

		authorField = new JTextField();
		detailPanel.add(authorField, "4, 4, 3, 1, fill, default");
		authorField.setColumns(10);
		ValidationComponentUtils.setMandatory(authorField, true);
		ValidationComponentUtils.setMessageKey(authorField, "book.author");

		JLabel publisherLabel = new JLabel(BUNDLE.getString("BookDetailFrame.book.publisher")); //$NON-NLS-1$
		detailPanel.add(publisherLabel, "2, 6");

		publisherField = new JTextField();
		detailPanel.add(publisherField, "4, 6, 3, 1, fill, default");
		publisherField.setColumns(10);
		ValidationComponentUtils.setMandatory(publisherField, true);
		ValidationComponentUtils.setMessageKey(publisherField, "book.publisher");

		JLabel shelfLabel = new JLabel(BUNDLE.getString("BookDetailFrame.book.shelf")); //$NON-NLS-1$
		detailPanel.add(shelfLabel, "2, 8");

		shelfField = new JComboBox<Shelf>(new DefaultComboBoxModel<Shelf>(Shelf.values()));
		detailPanel.add(shelfField, "4, 8, 3, 1, fill, default");
		ValidationComponentUtils.setMandatory(shelfField, true);
		ValidationComponentUtils.setMessageKey(shelfField, "book.shelf");

		JButton saveButton = new JButton(saveAction);
		detailPanel.add(saveButton, "6, 10");

		JPanel copiesPanel = new JPanel();
		copiesPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 5, 5, 5), new TitledBorder(UIManager.getBorder("TitledBorder.border"),
				"Exemplare", TitledBorder.LEADING, TitledBorder.TOP, null, null)));
		contentPane.add(copiesPanel, "1, 4, fill, fill");
		copiesPanel.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC, FormFactory.MIN_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("max(70dlu;pref)"), FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),
				FormFactory.LINE_GAP_ROWSPEC, }));

		JButton addCopyButton = new JButton(addCopyAction);
		copiesPanel.add(addCopyButton, "8, 2");

		JScrollPane scrollPane = new JScrollPane();
		copiesPanel.add(scrollPane, "2, 4, 7, 1, fill, fill");

		final CopyTableModel copyTableModel = new CopyTableModel(lib);
		copyList = new JTable(copyTableModel);
		scrollPane.setViewportView(copyList);

		copyList.getColumnModel().removeColumn(copyList.getColumnModel().getColumn(0));

		copySorter = new TableRowSorter<CopyTableModel>(copyTableModel);
		copyList.setRowSorter(copySorter);

		copyFilter = new CopyRowFilter();
		copySorter.setRowFilter(copyFilter);

		TableCellRenderer renderer = new DefaultTableCellRenderer() {
			private Font normalFont = getFont();
			private Font lostOrWasteFont = getFont().deriveFont(Font.ITALIC);

			@Override
			public Component
					getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				Condition condition = copyTableModel.getRow(copyList.convertRowIndexToModel(row)).getCondition();

				if (condition == Condition.WASTE || condition == Condition.LOST) {
					label.setFont(lostOrWasteFont);
					label.setForeground(isSelected ? Color.LIGHT_GRAY : Color.DARK_GRAY);
				} else {
					label.setFont(normalFont);
					label.setForeground(isSelected ? copyList.getSelectionForeground() : copyList.getForeground());
				}

				return label;
			}
		};

		copyList.getColumnModel().getColumn(0).setPreferredWidth(40);
		copyList.getColumnModel().getColumn(0).setCellRenderer(renderer);
		copyList.getColumnModel().getColumn(1).setPreferredWidth(300);
		copyList.getColumnModel().getColumn(1).setCellRenderer(renderer);
		copyList.getColumnModel().getColumn(2).setPreferredWidth(80);
		copyList.getColumnModel().getColumn(2).setCellRenderer(renderer);

		copyList.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(Condition.values())));

		Bindings.bind(authorField, bookModel.getBufferedModel(Book.AUTHOR));
		Bindings.bind(publisherField, bookModel.getBufferedModel(Book.PUBLISHER));
		Bindings.bind(shelfField, new SelectionInList<Shelf>(Shelf.values(), bookModel.getBufferedModel(Book.SHELF)));

	}

	private class SaveAction extends AbstractAction {
		public SaveAction() {
			putValue(SMALL_ICON, new ImageIcon(BookDetailFrame.class.getResource("/silk/disk.png")));
			putValue(NAME, BUNDLE.getString("Save.name"));
			putValue(SHORT_DESCRIPTION, BUNDLE.getString("Save.description"));
			putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 0);
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!bookModel.validate().hasErrors()) {
				bookModel.triggerCommit();

				if (!lib.getBooks().contains(bookModel.getBean())) {
					lib.getBooks().add(bookModel.getBean());
				}
			}
			setEnabled(false);
		}
	}

	private class AddCopyAction extends AbstractAction {
		public AddCopyAction() {
			putValue(SMALL_ICON, new ImageIcon(BookDetailFrame.class.getResource("/silk/add.png")));
			putValue(NAME, BUNDLE.getString("BookDetailFrame.addCopy"));
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			lib.createAndAddCopy(bookModel.getBean());
		}
	}

	private class BookModel extends PresentationModel<Book> implements Validatable {

		private ValidationResultModel validationResultModel = new DefaultValidationResultModel();

		private PropertyChangeListener valueChangedListener;

		public BookModel() {
			valueChangedListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {
					ValidationResult result = validate();
					validationResultModel.setResult(result);

					ValidationComponentUtils.updateComponentTreeMandatoryAndBlankBackground(contentPane);
					ValidationComponentUtils.updateComponentTreeSeverityBackground(contentPane, result);

					saveAction.setEnabled(bookModel.isBuffering() && !bookModel.getValidationResultModel().hasErrors());
				}
			};

			addBeanPropertyChangeListener(valueChangedListener);
			getBeanChannel().addValueChangeListener(valueChangedListener);
		}

		@Override
		public BufferedValueModel getBufferedModel(String propertyName) {
			BufferedValueModel model = super.getBufferedModel(propertyName);

			// make sure we don't register the listener multiple times
			for (PropertyChangeListener pcl : model.getPropertyChangeListeners("value")) {
				if (pcl == valueChangedListener) {
					return model;
				}
			}

			// make sure we know when the value changes
			model.addPropertyChangeListener("value", valueChangedListener);
			return model;
		}

		public ValidationResultModel getValidationResultModel() {
			return validationResultModel;
		}

		@Override
		public ValidationResult validate() {
			PropertyValidationSupport support = new PropertyValidationSupport(this, "book");

			String title = (String) getBufferedModel("title").getValue();
			String author = (String) getBufferedModel("author").getValue();
			String publisher = (String) getBufferedModel("publisher").getValue();
			Shelf shelf = (Shelf) getBufferedModel("shelf").getValue();

			if (title == null || title.isEmpty()) {
				support.addError("title", BUNDLE.getString("Book.missingTitle"));
			}

			if (author == null || author.isEmpty()) {
				support.addError("author", BUNDLE.getString("Book.missingAuthor"));
			}

			if (publisher == null || publisher.isEmpty()) {
				support.addError("publisher", BUNDLE.getString("Book.missingPublisher"));
			}

			if (shelf == null) {
				support.addError("shelf", BUNDLE.getString("Book.missingShelf"));
			}

			return support.getResult();
		}
	}

	private class CopyTableModel extends AbstractTableAdapter<Copy> {
		public CopyTableModel(Library library) {
			super(new DefaultEventListModel<>(library.getCopies()), "INVISIBLE-Book", BUNDLE
					.getString("BookDetailFrame.copyList.inventoryNumber"), BUNDLE.getString("BookDetailFrame.copyList.state"), BUNDLE
					.getString("BookDetailFrame.copyList.condition"));
		}

		@Override
		public Object getValueAt(int row, int column) {
			Copy copy = getRow(row);

			switch (column) {
				case 0:
					return copy.getTitle();
				case 1:
					return copy.getInventoryNumber();
				case 2:
					Loan loan = lib.getLoan(copy);

					String statusText = "";
					if (loan != null && loan.isLent()) {
						if (loan.isOverdue()) {
							statusText = String.format(BUNDLE.getString("Copy.status.overdue"), loan.getDueDateString(), loan.getDaysOverdue());
						} else if (loan.getDueInDays() == 0) {
							statusText = String.format(BUNDLE.getString("Copy.status.dueToday"), loan.getDueDateString());
						} else {
							statusText = String.format(BUNDLE.getString("Copy.status.dueIn"), loan.getDueDateString(), loan.getDueInDays());
						}
					} else {
						if (EnumSet.of(Condition.NEW, Condition.GOOD, Condition.DAMAGED).contains(copy.getCondition())) {
							statusText = BUNDLE.getString("Copy.status.available");
						} else {
							statusText = copy.getCondition().toString();
						}
					}
					return statusText;
				case 3:
					return copy.getCondition();
			}

			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 3;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 3) {
				Condition condition = (Condition) aValue;
				getRow(rowIndex).setCondition(condition);
			} else {
				super.setValueAt(aValue, rowIndex, columnIndex);
			}
		}
	}

	private class CopyRowFilter extends RowFilter<CopyTableModel, Integer> {
		private Book book;

		@Override
		public boolean include(javax.swing.RowFilter.Entry<? extends CopyTableModel, ? extends Integer> entry) {
			return book != null && book.equals(entry.getValue(0));
		}

		public void setBook(Book book) {
			this.book = book;
			copySorter.sort();
		}

	}
}
