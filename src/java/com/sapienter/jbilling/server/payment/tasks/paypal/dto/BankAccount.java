/**
 * 
 */
package com.sapienter.jbilling.server.payment.tasks.paypal.dto;

/**
 * @author mazhar
 *
 */
public class BankAccount {

	private String customerName;
	private String accountNumber;
	private String routingNumber;
	private String accountType;
	
	public BankAccount(){}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getRoutingNumber() {
		return routingNumber;
	}

	public void setRoutingNumber(String routingNumber) {
		this.routingNumber = routingNumber;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	
	@Override
	public String toString() {
		StringBuilder bankAccount = new StringBuilder("Customer Name: ");
		bankAccount.append(this.customerName);
		bankAccount.append(" Account Number: ");
		bankAccount.append(accountNumber);
		bankAccount.append(" Routing Number: ");
		bankAccount.append(routingNumber);
		bankAccount.append(" Account Type: ");
		bankAccount.append(accountType);
		
		return bankAccount.toString();
	}
}
