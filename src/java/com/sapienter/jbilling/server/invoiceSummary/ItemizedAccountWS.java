package com.sapienter.jbilling.server.invoiceSummary;

import java.io.Serializable;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.security.WSSecured;

public class ItemizedAccountWS implements WSSecured, Serializable{

	
    private InvoiceSummaryWS invoiceSummary;
    private InvoiceLineDTO monthlyCharges[] = new InvoiceLineDTO[0];
    private InvoiceLineDTO usageCharges[] = new InvoiceLineDTO[0];
    private InvoiceLineDTO fees[] = new InvoiceLineDTO[0];
    private InvoiceLineDTO taxes[] = new InvoiceLineDTO[0];
    private PaymentWS paymentsAndRefunds[] = new PaymentWS[0];
    private CreditAdjustmentWS creditAdjustments[] = new CreditAdjustmentWS[0];
	
	public ItemizedAccountWS() {	
	}

	public InvoiceSummaryWS getInvoiceSummary() {
		return invoiceSummary;
	}

	public void setInvoiceSummary(InvoiceSummaryWS invoiceSummary) {
		this.invoiceSummary = invoiceSummary;
	}

	public InvoiceLineDTO[] getMonthlyCharges() {
		return monthlyCharges;
	}

	public void setMonthlyCharges(InvoiceLineDTO[] monthlyCharges) {
		this.monthlyCharges = monthlyCharges;
	}

	public InvoiceLineDTO[] getUsageCharges() {
		return usageCharges;
	}

	public void setUsageCharges(InvoiceLineDTO[] usageCharges) {
		this.usageCharges = usageCharges;
	}

	public InvoiceLineDTO[] getFees() {
		return fees;
	}

	public void setFees(InvoiceLineDTO[] fees) {
		this.fees = fees;
	}

	public InvoiceLineDTO[] getTaxes() {
		return taxes;
	}

	public void setTaxes(InvoiceLineDTO[] taxes) {
		this.taxes = taxes;
	}

	public PaymentWS[] getPaymentsAndRefunds() {
		return paymentsAndRefunds;
	}

	public void setPaymentsAndRefunds(PaymentWS[] paymentsAndRefunds) {
		this.paymentsAndRefunds = paymentsAndRefunds;
	}

	public CreditAdjustmentWS[] getCreditAdjustments() {
		return creditAdjustments;
	}

	public void setCreditAdjustments(CreditAdjustmentWS[] creditAdjustments) {
		this.creditAdjustments = creditAdjustments;
	}

	public Integer getOwningEntityId() {
		return null;
	}

	public Integer getOwningUserId() {
		return null;
	}
	
}
