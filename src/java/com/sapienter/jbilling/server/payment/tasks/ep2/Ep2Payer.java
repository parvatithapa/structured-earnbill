package com.sapienter.jbilling.server.payment.tasks.ep2;

/**
 * Helper class it  has all  necessary fields to make payment. 
 * 
 * @author krunal bhavsar
 *
 */
public class Ep2Payer {

	private String firstName;
	private String lastName;
	private String creditCardNumber;
	private String email;
	private String expiryMonth;
	private String expiryYear;
	private String merchantId;
	private String transactionType;
	private String tokenId;
	private String paymentMethodName ;
	private String periodicType;
	private String requestedAmount;
	private String requestedId;
	private String currency;
	private String orderNumber;
	private String creditCardType;
	private String parentTransactionId; // used for refund
	
	
	
	public String getParentTransactionId() {
		return parentTransactionId;
	}

	public Ep2Payer addParentTransactionId(String parentTransactionId) {
		this.parentTransactionId = parentTransactionId;
		return this;
	}

	public String getCreditCardType() {
		return creditCardType;
	}

	public Ep2Payer addCreditCardType(String creditCardType) {
		this.creditCardType = creditCardType;
		return this;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public Ep2Payer addOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
		return this;
	}

	public String getCurrency() {
		return currency;
	}

	public Ep2Payer addCurrency(String currency) {
		this.currency = currency;
		return this;
	}

	public String getRequestedId() {
		return requestedId;
	}

	public Ep2Payer addRequestedId(String requestedId) {
		this.requestedId = requestedId;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public String getCreditCardNumber() {
		return creditCardNumber;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getMerchantId() {
		return merchantId;
	}
	
	public String getTransactionType() {
		return transactionType;
	}
	
	public String getTokenId() {
		return tokenId;
	}
	
	public String getPaymentMethodName() {
		return paymentMethodName;
	}
	
	public String getPeriodicType() {
		return periodicType;
	}
	
	public String getRequestedAmount() {
		return requestedAmount;
	}
	
	public Ep2Payer addFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}
	
	public Ep2Payer addLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}
	
	public Ep2Payer addCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
		return this;
	}
	
	public Ep2Payer addEmail(String email) {
		this.email = email;
		return this;
	}
	
	public Ep2Payer addMerchantId(String merchantId) {
		this.merchantId = merchantId;
		return this;
	}
	
	public Ep2Payer addTransactionType(String transactionType) {
		this.transactionType = transactionType;
		return this;
	}
	
	public Ep2Payer addTokenId(String tokenId) {
		this.tokenId = tokenId;
		return this;
	}
	
	public Ep2Payer addPaymentMethodName(String paymentMethodName) {
		this.paymentMethodName = paymentMethodName;
		return this;
	}
	
	public Ep2Payer addPeriodicType(String periodicType) {
		this.periodicType = periodicType;
		return this;
	}
	
	public Ep2Payer addRquestedAmount(String rquestedAmount) {
		this.requestedAmount = rquestedAmount;
		return this;
	}

	public String getExpiryMonth() {
		return expiryMonth;
	}

	public String getExpiryYear() {
		return expiryYear;
	}

	public Ep2Payer addExpiryMonth(String expiryMonth) {
		this.expiryMonth = expiryMonth;
		return this;
	}

	public Ep2Payer addExpiryYear(String expiryYear) {
		this.expiryYear = expiryYear;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Ep2Payer [firstName=");
		builder.append(firstName);
		builder.append(", lastName=");
		builder.append(lastName);
		builder.append(", creditCardNumber=");
		builder.append(creditCardNumber);
		builder.append(", email=");
		builder.append(email);
		builder.append(", expiryMonth=");
		builder.append(expiryMonth);
		builder.append(", expiryYear=");
		builder.append(expiryYear);
		builder.append(", merchantId=");
		builder.append(merchantId);
		builder.append(", transactionType=");
		builder.append(transactionType);
		builder.append(", tokenId=");
		builder.append(tokenId);
		builder.append(", paymentMethodName=");
		builder.append(paymentMethodName);
		builder.append(", periodicType=");
		builder.append(periodicType);
		builder.append(", requestedAmount=");
		builder.append(requestedAmount);
		builder.append(", requestedId=");
		builder.append(requestedId);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", orderNumber=");
		builder.append(orderNumber);
		builder.append("]");
		return builder.toString();
	}
	
}
