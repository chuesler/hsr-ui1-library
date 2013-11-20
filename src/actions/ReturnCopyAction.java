package actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import domain.Loan;

public abstract class ReturnCopyAction extends AbstractAction {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

	public ReturnCopyAction() {

		putValue(SHORT_DESCRIPTION, BUNDLE.getString("InventoryFrame.returnCopy.description"));
		putValue(NAME, BUNDLE.getString("InventoryFrame.returnCopy.name"));
		putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/silk/arrow_undo.png")));
		setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		List<Loan> loans = getSelectedLoans();
		if (loans != null && loans.size() > 0) {
			for (Loan loan : loans) {
				if (loan.isOverdue()) {
					int answer =
							JOptionPane.showConfirmDialog(JOptionPane.getRootFrame(),
									String.format(BUNDLE.getString("InventoryFrame.overdueMessage.text"), loan.getCopy().getInventoryNumber()),
									BUNDLE.getString("InventoryFrame.overdueMessage.title"), JOptionPane.OK_CANCEL_OPTION,
									JOptionPane.INFORMATION_MESSAGE);
					if (answer != JOptionPane.OK_OPTION) {
						return;
					}
				}

				loan.returnCopy();
			}

			finishedOk();
		}
	}

	protected abstract List<Loan> getSelectedLoans();

	protected abstract void finishedOk();
}