package comp;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Format;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.JTextComponent;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;

/**
 * My stab at a filtering combo box. Note: This is not auto completion. This combo box filters the entries it displays in the popup.
 * 
 * @author Ch. Hüsler
 * 
 * @param <E>
 *            Entry type.
 */
public abstract class FilteringComboBox<E> extends JComboBox<E> {

	private FilterList<E> list;

	/**
	 * Disable filter clearing on popup closing.
	 */
	private AtomicBoolean clearFilterOnPopupClose = new AtomicBoolean(true);

	@SuppressWarnings("unchecked")
	public FilteringComboBox(FilterList<E> l) {
		super(new DefaultEventComboBoxModel<E>(l));
		this.list = l;

		setEditable(true);

		addListener();

		// reset filter if popup closes
		addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				list.setMatcher(null);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				if (clearFilterOnPopupClose.get()) { // clear filter when closing the popup
					list.setMatcher(null);
				}
			}

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

			}
		});
	}

	public FilteringComboBox(FilterList<E> l, Format format) {
		this(l);

		setEditor(new BasicComboBoxEditor() {
			@Override
			protected JTextField createEditorComponent() {
				return new JFormattedTextField(NumberFormat.getIntegerInstance());
			}
		});

	}

	private void addListener() {
		getEditor().getEditorComponent().addKeyListener(new FilterListener());
	}

	public abstract Matcher<E> getMatcher(String text);

	public void resetFilter() {
		list.setMatcher(null);
	}

	@Override
	public void setSelectedItem(Object obj) {
		super.setSelectedItem(obj);

		list.setMatcher(null); // reset filter on selection
	}

	private final class FilterListener extends KeyAdapter {
		@Override
		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE || e.getKeyChar() == KeyEvent.VK_ENTER) { // just close the popup
				hidePopup();
				return;
			}

			// don't try to handle special commands, we'd just break them
			if (e.getKeyChar() == '\b' || !(e.isControlDown() || e.isAltDown() || e.isAltGraphDown() || e.isMetaDown())) {
				SwingUtilities.invokeLater(new Runnable() {
					// needs to be invoked later, otherwise the model messes up because of updates happening too rapidly
					@Override
					public void run() {
						JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();

						String text = editor.getText();
						list.setMatcher(getMatcher(text)); // reset matcher to restart filtering
						// need to set the text again, otherwise we'd just see the first character all the time because the list
						// changes (which gets rid of the editor's input)
						editor.setText(text);

						clearFilterOnPopupClose.set(false);
						hidePopup(); // hide and show the popup to recalculate it's size
						showPopup();
						clearFilterOnPopupClose.set(true);
					}
				});
			}
		}
	}

}
