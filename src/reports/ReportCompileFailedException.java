package reports;

/**
 * Exception to signal an error while trying to compile a report.
 * 
 * @author Ch. Hüsler
 */
public class ReportCompileFailedException extends RuntimeException {

	public ReportCompileFailedException() {
	}

	public ReportCompileFailedException(String message) {
		super(message);
	}

	public ReportCompileFailedException(Throwable cause) {
		super(cause);
	}

	public ReportCompileFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReportCompileFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
