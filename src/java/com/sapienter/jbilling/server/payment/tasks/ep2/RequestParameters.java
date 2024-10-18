package com.sapienter.jbilling.server.payment.tasks.ep2;

public enum RequestParameters {
	MERCHANT_ID ("merchant-account-id"),
	PAYMENT_METHOD ("payment-method"),
	REQUEST_ID ("request-id"),
	ORDER_NUMBER ("order-number"),
	TRANSACTION_TYPE ("transaction-type"),
	REQUESTED_AMOUNT ("requested-amount"),
	CURRENCY ("currency"),
	FIRST_NAME ("first-name"),
	LAST_NAME ("last-name"),
	EMAIL_ID ("email"),
	TOKEN_ID ("token-id"),
	PERIODIC_TYPE ("periodic-type"), 
	ACCOUNT_NUMBER("account-number"), 
	CARD_SECURITY_CODE("card-security-code") ,
	CARD_TYPE("card-type"),
	EXPIRATION_MONTH("expiration-month"),
	EXPIRATION_YEAR("expiration-year"),
	PARENT_TRANSACTION_ID("parent-transaction-id");
	
	private final String name;

	private RequestParameters(String name) {
		this.name = name;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}
}
