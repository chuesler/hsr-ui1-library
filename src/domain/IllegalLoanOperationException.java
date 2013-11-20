package domain;

public class IllegalLoanOperationException extends Exception {

	private static final long serialVersionUID = 4722101048344940400L;

	public IllegalLoanOperationException(String string) {
		super(string);
	}

}
