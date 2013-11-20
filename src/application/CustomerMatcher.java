package application;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ca.odell.glazedlists.matchers.Matcher;
import domain.Customer;

class CustomerMatcher implements Matcher<Customer> {
	private Pattern pattern = null;
	private java.util.regex.Matcher matcher = null;

	public CustomerMatcher(String input) {
		try {
			pattern = Pattern.compile(input, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			pattern = null; // invalid pattern, just accept any item
		}
	}

	@Override
	public boolean matches(Customer customer) {
		if (pattern == null) {
			return true;
		}

		if (matcher == null) {
			matcher = pattern.matcher(customer.toString());
		} else {
			matcher.reset(customer.toString());
		}

		return matcher.find();
	}
}