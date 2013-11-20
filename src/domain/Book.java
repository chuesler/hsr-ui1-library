package domain;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class Book {

	public static final String TITLE = "title";
	public static final String AUTHOR = "author";
	public static final String PUBLISHER = "publisher";
	public static final String SHELF = "shelf";

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	private String title, author, publisher;
	private Shelf shelf;

	public Book(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		String old = this.title;
		this.title = title;
		pcs.firePropertyChange(TITLE, old, title);
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		String old = this.author;
		this.author = author;
		pcs.firePropertyChange(AUTHOR, old, author);
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		String old = this.publisher;
		this.publisher = publisher;
		pcs.firePropertyChange(PUBLISHER, old, publisher);
	}

	public Shelf getShelf() {
		return shelf;
	}

	public void setShelf(Shelf shelf) {
		Shelf old = this.shelf;
		this.shelf = shelf;
		pcs.firePropertyChange(SHELF, old, shelf);
	}

	@Override
	public String toString() {
		return title + ", " + author + ", " + publisher;
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
