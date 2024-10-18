package com.sapienter.jbilling.server.user;

import javax.validation.constraints.NotNull;

import lombok.ToString;

import org.hibernate.validator.constraints.NotEmpty;

@ToString
public class CreateUserRequestWS {

	@NotNull(message = "validation.error.notnull")
	@NotEmpty(message = "validation.error.notempty")
	private String accountNumber;
	private UserBillingPeriod billingPeriod;
	private Integer nextInvoiceDayOfPeriod;
	// can be used for passing different values
	private CustomUserFieldWS[] customUserFields;

	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public UserBillingPeriod getBillingPeriod() {
		return billingPeriod;
	}

	public void setBillingPeriod(UserBillingPeriod billingPeriod) {
		this.billingPeriod = billingPeriod;
	}

	public int getNextInvoiceDayOfPeriod() {
		return nextInvoiceDayOfPeriod;
	}

	public void setNextInvoiceDayOfPeriod(int nextInvoiceDayOfPeriod) {
		this.nextInvoiceDayOfPeriod = nextInvoiceDayOfPeriod;
	}

	public CustomUserFieldWS[] getCustomUserFields() {
		return customUserFields;
	}

	public void setCustomUserFields(CustomUserFieldWS[] customUserFields) {
		this.customUserFields = customUserFields;
	}
}