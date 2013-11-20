package actions;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

/**
 * Abstract action to help with file saving.
 * 
 * @author Ch. Hüsler
 */
public abstract class SaveFileAction extends AbstractAction {
	private Component parent;

	public SaveFileAction(Component parent) {
		this.parent = parent;
	}

	@Override
	public final void actionPerformed(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();

		configureFileChooser(fileChooser);

		fileChooser.setMultiSelectionEnabled(false);

		if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			saveToFile(fileChooser.getSelectedFile());

			parent.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Configure file chooser settings. The default implementation is empty. Note: Multi selection is not supported.
	 * 
	 * @param chooser
	 *            The file chooser.
	 */
	protected void configureFileChooser(JFileChooser chooser) {
	}

	/**
	 * Called when the user accepts a file.
	 * 
	 * @param file
	 *            The accepted file.
	 */
	protected abstract void saveToFile(File file);

}