package test;

import junit.framework.TestCase;
import domain.Book;
import domain.Shelf;

public class BookTest extends TestCase {

	public void testBookCreation() {
		Book b = new Book("The Definitive ANTLR Reference");
		b.setTitle("The Definitive ANTLR Reference");
		b.setAuthor("Terence Parr");
		b.setPublisher("The Pragmatic Programmers");
		b.setShelf(Shelf.A1);

		assertEquals("The Definitive ANTLR Reference", b.getTitle());
		b.setTitle("NewName");
		assertEquals("NewName", b.getTitle());
		assertEquals("Terence Parr", b.getAuthor());
		assertEquals("The Pragmatic Programmers", b.getPublisher());

	}
}
