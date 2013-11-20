package comp;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Label to display icons on JTabbedPane tabs. This component is needed to control the icon position.
 * 
 * @author Ch. Hüsler
 */
public class TabWithIcon extends JLabel {

	public TabWithIcon(String text, Icon icon) {
		super(text);
		setIcon(icon);

		setIconTextGap(5);
		setHorizontalTextPosition(SwingConstants.RIGHT);

		// Display the mnemonic "_". First char is fine in this project, but this would have to be changed for broader use.
		setDisplayedMnemonic(text.charAt(0));
	}

}
