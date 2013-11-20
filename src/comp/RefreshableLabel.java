package comp;

import javax.swing.JLabel;

/**
 * Simple label able to recalculate it's text.
 * 
 * @author Ch. Hüsler
 */
public abstract class RefreshableLabel extends JLabel {

	public RefreshableLabel() {
		refresh();
	}

	public abstract void refresh();

}
