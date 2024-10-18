package com.sapienter.jbilling.server.payment.tasks.paysafe;

import java.math.BigDecimal;


public class PaySafePayerInfo {
	
	private String firstName;
	private String lastName;
	private String creditCardNumber;
	private String email;
	private String expiryMonth;
	private String expiryYear;
	private String tokenId;
	private BigDecimal amount;
	private String parentPaymentTransactionId; // used for refund
	private String parentPaymentMerchantRefNumber; // used for refund
	private String merchantRefNumber; // unique for each payment 
	private String merchantCustomerId; // unique for each customer
	private String city;
	private String zip;
	private String countryCode;
	private String state;
	private String street;
	private String cardHolderName;
	
	
	public String getCardHolderName() {
		return cardHolderName;
	}

	public PaySafePayerInfo addCardHolderName(String cardHolderName) {
		this.cardHolderName = cardHolderName;
		return this;
	}

	public String getParentPaymentTransactionId() {
		return parentPaymentTransactionId;
	}

	public String getParentPaymentMerchantRefNumber() {
		return parentPaymentMerchantRefNumber;
	}

	public String getMerchantRefNumber() {
		return merchantRefNumber;
	}

	public String getCity() {
		return city;
	}

	public String getZip() {
		return zip;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public PaySafePayerInfo addParentPaymentTransactionId(String parentPaymentTransactionId) {
		this.parentPaymentTransactionId = parentPaymentTransactionId;
		return this;
	}

	public PaySafePayerInfo addParentPaymentMerchantRefNumber(String parentPaymentMerchantRefNumber) {
		this.parentPaymentMerchantRefNumber = parentPaymentMerchantRefNumber;
		return this;
	}

	public PaySafePayerInfo addMerchantRefNumber(String merchantRefNumber) {
		this.merchantRefNumber = merchantRefNumber;
		return this;
	}

	public PaySafePayerInfo addCity(String city) {
		this.city = city;
		return this;
	}

	public PaySafePayerInfo addZip(String zip) {
		this.zip = zip;
		return this;
	}

	public PaySafePayerInfo addCountryCode(String countryCode) {
		this.countryCode = countryCode;
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
	
	public String getTokenId() {
		return tokenId;
	}
	
	public PaySafePayerInfo addFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}
	
	public PaySafePayerInfo addLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}
	
	public PaySafePayerInfo addCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
		return this;
	}
	
	public PaySafePayerInfo addEmail(String email) {
		this.email = email;
		return this;
	}
	
	public PaySafePayerInfo addTokenId(String tokenId) {
		this.tokenId = tokenId;
		return this;
	}

	public String getExpiryMonth() {
		return expiryMonth;
	}

	public String getExpiryYear() {
		return expiryYear;
	}

	public PaySafePayerInfo addExpiryMonth(String expiryMonth) {
		this.expiryMonth = expiryMonth;
		return this;
	}

	public PaySafePayerInfo addExpiryYear(String expiryYear) {
		this.expiryYear = expiryYear;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public PaySafePayerInfo addAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getMerchantCustomerId() {
		return merchantCustomerId;
	}

	public PaySafePayerInfo addMerchantCustomerId(String merchantCustomerId) {
		this.merchantCustomerId = merchantCustomerId;
		return this;
	}

	public String getState() {
		return state;
	}

	public String getStreet() {
		return street;
	}

	public PaySafePayerInfo addState(String state) {
		this.state = state;
		return this;
	}

	public PaySafePayerInfo addStreet(String street) {
		this.street = street;
		return this;
	}
	

}
