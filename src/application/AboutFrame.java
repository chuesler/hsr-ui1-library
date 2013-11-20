package application;

import java.awt.Color;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class AboutFrame extends JFrame {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");
	private JPanel contentPane;

	/**
	 * Create the frame.
	 */
	public AboutFrame() {
		setResizable(false);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		setBounds(100, 100, 624, 295);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("420px"),
				FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("142px"), FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("default:grow"), FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:default:grow"), FormFactory.RELATED_GAP_ROWSPEC, }));

		JLabel aboutLabel = new JLabel(BUNDLE.getString("AboutFrame.aboutText"));
		aboutLabel.setVerticalAlignment(SwingConstants.TOP);
		contentPane.add(aboutLabel, "2, 2, 2, 3, fill, fill");

		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setBackground(Color.LIGHT_GRAY);
		lblNewLabel.setIcon(new ImageIcon(AboutFrame.class.getResource("/large_open_book.png")));
		contentPane.add(lblNewLabel, "4, 2, left, fill");

	}

}
