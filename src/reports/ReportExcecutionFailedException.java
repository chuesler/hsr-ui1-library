package reports;

/**
 * Exception to signal an error while trying to generate a report.
 * 
 * @author Ch. Hüsler
 */
public class ReportExcecutionFailedException extends RuntimeException {

	public ReportExcecutionFailedException() {
	}

	public ReportExcecutionFailedException(String message) {
		super(message);
	}

	public ReportExcecutionFailedException(Throwable cause) {
		super(cause);
	}

	public ReportExcecutionFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReportExcecutionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
