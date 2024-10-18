/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2017] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.invoiceSummary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.sapienter.jbilling.server.security.WSSecured;
/**
 * @author Ashok Kale
 */
public class InvoiceSummaryWS implements WSSecured, Serializable{

	private int id;
	private int creationInvoiceId;
	private Integer userId;
	private BigDecimal monthlyCharges;
	private BigDecimal usageCharges;
	private BigDecimal fees;
	private BigDecimal taxes;	
	private BigDecimal adjustmentCharges;
	private BigDecimal amountOfLastStatement;	
	private BigDecimal paymentReceived;
	private BigDecimal newCharges;
	private BigDecimal totalDue;
	private Date invoiceDate;
	private Date lastInvoiceDate;
	private Date createDatetime;
	
	public InvoiceSummaryWS() {
		
	}
	
	public InvoiceSummaryWS(int id, int creationInvoiceId,Integer userId,
			BigDecimal monthlyCharges, BigDecimal usageCharges,
			BigDecimal fees, BigDecimal taxes, BigDecimal adjustmentCharges,
			BigDecimal amountOfLastStatement, BigDecimal paymentReceived,
			BigDecimal newCharges, BigDecimal totalDue, Date invoiceDate, 
			Date lastInvoiceDate, Date createDatetime) {
		super();
		this.id = id;
		this.creationInvoiceId = creationInvoiceId;
		this.userId = userId;
		this.monthlyCharges = monthlyCharges;
		this.usageCharges = usageCharges;
		this.fees = fees;
		this.taxes = taxes;
		this.adjustmentCharges = adjustmentCharges;
		this.amountOfLastStatement = amountOfLastStatement;
		this.paymentReceived = paymentReceived;
		this.newCharges = newCharges;
		this.totalDue = totalDue;
		this.invoiceDate = invoiceDate;
		this.lastInvoiceDate = lastInvoiceDate;
		this.createDatetime = createDatetime;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getCreationInvoiceId() {
		return creationInvoiceId;
	}
	
	public void setCreationInvoiceId(int creationInvoiceId) {
		this.creationInvoiceId = creationInvoiceId;
	}
	
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public BigDecimal getMonthlyCharges() {
		return monthlyCharges;
	}
	
	public void setMonthlyCharges(BigDecimal monthlyCharges) {
		this.monthlyCharges = monthlyCharges;
	}
	
	public BigDecimal getUsageCharges() {
		return usageCharges;
	}
	
	public void setUsageCharges(BigDecimal usageCharges) {
		this.usageCharges = usageCharges;
	}
	
	public BigDecimal getFees() {
		return fees;
	}
	
	public void setFees(BigDecimal fees) {
		this.fees = fees;
	}
	
	public BigDecimal getTaxes() {
		return taxes;
	}
	
	public void setTaxes(BigDecimal taxes) {
		this.taxes = taxes;
	}
	
	public BigDecimal getAdjustmentCharges() {
		return adjustmentCharges;
	}
	
	public void setAdjustmentCharges(BigDecimal adjustmentCharges) {
		this.adjustmentCharges = adjustmentCharges;
	}
	
	public BigDecimal getAmountOfLastStatement() {
		return amountOfLastStatement;
	}
	
	public void setAmountOfLastStatement(BigDecimal amountOfLastStatement) {
		this.amountOfLastStatement = amountOfLastStatement;
	}
	
	public BigDecimal getPaymentReceived() {
		return paymentReceived;
	}
	
	public void setPaymentReceived(BigDecimal paymentReceived) {
		this.paymentReceived = paymentReceived;
	}
	
	public BigDecimal getNewCharges() {
		return newCharges;
	}
	
	public void setNewCharges(BigDecimal newCharges) {
		this.newCharges = newCharges;
	}
	
	public BigDecimal getTotalDue() {
		return totalDue;
	}

	public void setTotalDue(BigDecimal totalDue) {
		this.totalDue = totalDue;
	}

	public Date getCreateDatetime() {
		return createDatetime;
	}
	
	public void setCreateDatetime(Date createDatetime) {
		this.createDatetime = createDatetime;
	}
	
	public Date getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public Date getLastInvoiceDate() {
		return lastInvoiceDate;
	}

	public void setLastInvoiceDate(Date lastInvoiceDate) {
		this.lastInvoiceDate = lastInvoiceDate;
	}

	public Integer getOwningEntityId() {
		return null;
	}

	public Integer getOwningUserId() {
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InvoiceSummaryWS [id=");
		builder.append(id);
		builder.append(", creationInvoiceId=");
		builder.append(creationInvoiceId);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", monthlyCharges=");
		builder.append(monthlyCharges);
		builder.append(", usageCharges=");
		builder.append(usageCharges);
		builder.append(", fees=");
		builder.append(fees);
		builder.append(", taxes=");
		builder.append(taxes);
		builder.append(", adjustmentCharges=");
		builder.append(adjustmentCharges);
		builder.append(", amountOfLastStatement=");
		builder.append(amountOfLastStatement);
		builder.append(", paymentReceived=");
		builder.append(paymentReceived);
		builder.append(", newCharges=");
		builder.append(newCharges);
		builder.append(", totalDue=");
		builder.append(totalDue);
		builder.append(", invoiceDate=");
		builder.append(invoiceDate);
		builder.append(", lastInvoiceDate=");
		builder.append(lastInvoiceDate);
		builder.append(", createDatetime=");
		builder.append(createDatetime);
		builder.append("]");
		return builder.toString();
	}
	
}
