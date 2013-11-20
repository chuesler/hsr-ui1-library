package reports;

/**
 * Exception to signal mismatching loans.
 * 
 * @author Ch. Hüsler
 */
public class CustomerMismatchException extends RuntimeException {

	public CustomerMismatchException() {
		super();
	}

	public CustomerMismatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CustomerMismatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public CustomerMismatchException(String message) {
		super(message);
	}

	public CustomerMismatchException(Throwable cause) {
		super(cause);
	}

}
