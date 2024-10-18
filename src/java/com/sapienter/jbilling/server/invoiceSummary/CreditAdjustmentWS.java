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
 * This class is used to fetch credit adjustments. There are 3 ways of providing credits.
 * We combined all 3 types in CreditAdjustmentWS object. Below are the 3 type of credits
 * 1. Payment if type Credit
 * 2. Credit Notes
 * 3. Invoice lines of type Adjustments
 * @author Ashok Kale
 *
 */
public class CreditAdjustmentWS implements WSSecured, Serializable{

	//Credit Payment Related information Fields
    private Integer paymentId;
    private Integer paymentMethodId;
    private Integer paymentResultId;
    private Date paymentCreateDatetime;
    private Date paymentDate;
    private int isRefund;
    private String paymentNotes = null;

	//Credit Notes Related information Fields
    private Integer creditNoteId;
    private Integer creditNoteLineId;
	private Integer creditNoteInvoiceId = null;
    private Integer creditNoteInvoiceLineId;
	private Date creditNoteDate;

	//Invoice line Related information Fields
	private Date creditInvoiceLineDate;

	// Credit Adjustment Common fields
    private Integer currencyId;
    private BigDecimal amount;
    private BigDecimal balance;
    private String description;
	private String type;



	public CreditAdjustmentWS() {

	}

	public CreditAdjustmentWS(Integer paymentId, Integer currencyId,
			Integer paymentMethodId, Integer paymentResultId,
			BigDecimal amount, BigDecimal balance, String description, Date paymentCreateDatetime,
			Date paymentDate, int isRefund, String paymentNotes, String type,
			Integer creditNoteId, Integer creditNoteLineId,
			Integer creditNoteInvoiceId, Integer creditNoteInvoiceLineId,
			Date creditNoteDate, Date creditInvoiceLineDate) {
		super();
		this.paymentId = paymentId;
		this.currencyId = currencyId;
		this.paymentMethodId = paymentMethodId;
		this.paymentResultId = paymentResultId;
		this.amount = amount;
		this.balance = balance;
		this.description = description;
		this.paymentCreateDatetime = paymentCreateDatetime;
		this.paymentDate = paymentDate;
		this.isRefund = isRefund;
		this.paymentNotes = paymentNotes;
		this.type = type;
		this.creditNoteId = creditNoteId;
		this.creditNoteLineId = creditNoteLineId;
		this.creditNoteInvoiceId = creditNoteInvoiceId;
		this.creditNoteInvoiceLineId = creditNoteInvoiceLineId;
		this.creditNoteDate = creditNoteDate;
		this.creditInvoiceLineDate = creditInvoiceLineDate;
	}

	public Integer getPaymentId() {
		return paymentId;
	}
	
	public void setPaymentId(Integer paymentId) {
		this.paymentId = paymentId;
	}
	
	public Integer getCurrencyId() {
		return currencyId;
	}
	
	public void setCurrencyId(Integer currencyId) {
		this.currencyId = currencyId;
	}
	
	public Integer getPaymentMethodId() {
		return paymentMethodId;
	}
	
	public void setPaymentMethodId(Integer paymentMethodId) {
		this.paymentMethodId = paymentMethodId;
	}
	
	public Integer getPaymentResultId() {
		return paymentResultId;
	}
	
	public void setPaymentResultId(Integer paymentResultId) {
		this.paymentResultId = paymentResultId;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getPaymentCreateDatetime() {
		return paymentCreateDatetime;
	}

	public void setPaymentCreateDatetime(Date paymentCreateDatetime) {
		this.paymentCreateDatetime = paymentCreateDatetime;
	}

	public Date getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}

	public int getIsRefund() {
		return isRefund;
	}

	public void setIsRefund(int isRefund) {
		this.isRefund = isRefund;
	}

	public String getPaymentNotes() {
		return paymentNotes;
	}

	public void setPaymentNotes(String paymentNotes) {
		this.paymentNotes = paymentNotes;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getCreditNoteId() {
		return creditNoteId;
	}

	public void setCreditNoteId(Integer creditNoteId) {
		this.creditNoteId = creditNoteId;
	}

	public Integer getCreditNoteLineId() {
		return creditNoteLineId;
	}

	public void setCreditNoteLineId(Integer creditNoteLineId) {
		this.creditNoteLineId = creditNoteLineId;
	}

	public Integer getCreditNoteInvoiceId() {
		return creditNoteInvoiceId;
	}

	public void setCreditNoteInvoiceId(Integer creditNoteInvoiceId) {
		this.creditNoteInvoiceId = creditNoteInvoiceId;
	}

	public Integer getCreditNoteInvoiceLineId() {
		return creditNoteInvoiceLineId;
	}

	public void setCreditNoteInvoiceLineId(Integer creditNoteInvoiceLineId) {
		this.creditNoteInvoiceLineId = creditNoteInvoiceLineId;
	}

	public Date getCreditNoteDate() {
		return creditNoteDate;
	}

	public void setCreditNoteDate(Date creditNoteDate) {
		this.creditNoteDate = creditNoteDate;
	}

	public Date getCreditInvoiceLineDate() {
		return creditInvoiceLineDate;
	}

	public void setCreditInvoiceLineDate(Date creditInvoiceLineDate) {
		this.creditInvoiceLineDate = creditInvoiceLineDate;
	}

	public Integer getOwningEntityId() {
		return null;
	}

	public Integer getOwningUserId() {
		return null;
	}
}
