package domain;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ResourceBundle;

public class Copy {

	public static final String CONDITION = "condition";

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public enum Condition {
		NEW, GOOD, DAMAGED, WASTE, LOST;

		private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

		@Override
		public String toString() {
			return BUNDLE.getString(getClass().getSimpleName() + "." + name());
		};
	}

	public static long nextInventoryNumber = 1;

	private final long inventoryNumber;
	private final Book book;
	private Condition condition;

	public Copy(Book title) {
		this.book = title;
		inventoryNumber = nextInventoryNumber++;
		condition = Condition.NEW;
	}

	public Book getTitle() {
		return book;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		Condition old = this.condition;
		this.condition = condition;
		pcs.firePropertyChange(CONDITION, old, condition);
	}

	public long getInventoryNumber() {
		return inventoryNumber;
	}

	@Override
	public String toString() {
		return getInventoryNumber() + ": " + getTitle().getTitle() + " (" + getTitle().getAuthor() + ")";
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(property, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(property, listener);
	}
}
