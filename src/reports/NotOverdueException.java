package reports;

/**
 * Exception to complain about a loan not being overdue while printing reminders.
 * 
 * @author Ch. Hüsler
 */
public class NotOverdueException extends RuntimeException {

	public NotOverdueException() {
	}

	public NotOverdueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotOverdueException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotOverdueException(String message) {
		super(message);
	}

	public NotOverdueException(Throwable cause) {
		super(cause);
	}

}
