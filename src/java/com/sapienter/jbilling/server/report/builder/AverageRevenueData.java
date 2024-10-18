package com.sapienter.jbilling.server.report.builder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @author Nader Mirzadeh
 * @since 2019-Feb-11
 */
public class AverageRevenueData {

    private int userId;
    private String customerName;
    private int customerAccountId;
    private String customerAccountName;
    private String customerAccountStatus;
    private int invoiceId;
    private Date invoiceDate;
    private BigDecimal invoiceAmount;
    private int invoiceLineId;
    private BigDecimal invoiceLineAmount;
    private int invoiceLineTypeId;
    private String invoiceLineType;
    
    
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public int getCustomerAccountId() {
		return customerAccountId;
	}
	public void setCustomerAccountId(int customerAccountId) {
		this.customerAccountId = customerAccountId;
	}
	public String getCustomerAccountName() {
		return customerAccountName;
	}
	public void setCustomerAccountName(String customerAccountName) {
		this.customerAccountName = customerAccountName;
	}
	public String getCustomerAccountStatus() {
		return customerAccountStatus;
	}
	public void setCustomerAccountStatus(String customerAccountStatus) {
		this.customerAccountStatus = customerAccountStatus;
	}
	public int getInvoiceId() {
		return invoiceId;
	}
	public void setInvoiceId(int invoiceId) {
		this.invoiceId = invoiceId;
	}
	public Date getInvoiceDate() {
		return invoiceDate;
	}
	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}
	public BigDecimal getInvoiceAmount() {
		return invoiceAmount;
	}
	public void setInvoiceAmount(BigDecimal invoiceAmount) {
		this.invoiceAmount = invoiceAmount;
	}
	public int getInvoiceLineId() {
		return invoiceLineId;
	}
	public void setInvoiceLineId(int invoiceLineId) {
		this.invoiceLineId = invoiceLineId;
	}
	public BigDecimal getInvoiceLineAmount() {
		return invoiceLineAmount;
	}
	public void setInvoiceLineAmount(BigDecimal invoiceLineAmount) {
		this.invoiceLineAmount = invoiceLineAmount;
	}
	public int getInvoiceLineTypeId() {
		return invoiceLineTypeId;
	}
	public void setInvoiceLineTypeId(int invoiceLineTypeId) {
		this.invoiceLineTypeId = invoiceLineTypeId;
	}
	public String getInvoiceLineType() {
		return invoiceLineType;
	}
	public void setInvoiceLineType(String invoiceLineType) {
		this.invoiceLineType = invoiceLineType;
	}
	
    
}
