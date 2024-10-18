package com.sapienter.jbilling.server.invoiceSummary;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ItemizedAccountTester {
	private ItemizedAccountWS itemizedAccountWS;
	private BigDecimal monthlyCharges;
	private BigDecimal usageCharges;
	private BigDecimal fees;
	private BigDecimal taxes;	
	private BigDecimal adjustmentCharges;
	private BigDecimal amountOfLastStatement;	
	private BigDecimal paymentReceived;
	private BigDecimal newCharges;
	private BigDecimal totalDue;
	private Date lastInvoiceDate;
	
	public ItemizedAccountTester(ItemizedAccountWS itemizedAccountWS) {
		this.itemizedAccountWS = itemizedAccountWS;
	}
	
	public ItemizedAccountTester addExpectedMonthlyCharges(BigDecimal monthlyCharges)  {
		this.monthlyCharges = monthlyCharges;
		return this;
	}
	
	public ItemizedAccountTester addExpectedUsageCharges(BigDecimal usageCharges)  {
		this.usageCharges = usageCharges;
		return this;
	}
	
	public ItemizedAccountTester addExpectedFeesCharges(BigDecimal fees)  {
		this.fees = fees;
		return this;
	}
	
	public ItemizedAccountTester addExpectedTaxesCharges(BigDecimal taxes)  {
		this.taxes = taxes;
		return this;
	}
	
	public ItemizedAccountTester addExpectedAdjustmentCharges(BigDecimal adjustmentCharges)  {
		this.adjustmentCharges = adjustmentCharges;
		return this;
	}
	
	public ItemizedAccountTester addExpectedPaymentReceived(BigDecimal paymentReceived)  {
		this.paymentReceived = paymentReceived;
		return this;
	}
	
	public ItemizedAccountTester addExpectedNewCharges(BigDecimal newCharges)  {
		this.newCharges = newCharges;
		return this;
	}
	
	public ItemizedAccountTester addExpectedTotalDue(BigDecimal totalDue)  {
		this.totalDue = totalDue;
		return this;
	}
	
	public ItemizedAccountTester addExpectedLastInvoiceDate(Date lastInvoiceDate)  {
		this.lastInvoiceDate = lastInvoiceDate;
		return this;
	}
	
	public ItemizedAccountTester addExpectedAmountOfLastStatement(BigDecimal amountOfLastStatement)  {
		this.amountOfLastStatement = amountOfLastStatement;
		return this;
	}
	
	public void  validate() {
		assertNotNull("ItemizedAccountWS Required ", itemizedAccountWS);
		InvoiceSummaryWS invoiceSummaryWS = itemizedAccountWS.getInvoiceSummary();
		
        assertEquals("Monthly Charges Should be ", monthlyCharges, invoiceSummaryWS.getMonthlyCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Usage Charges Should be ", usageCharges, invoiceSummaryWS.getUsageCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Fees Should be ", fees, invoiceSummaryWS.getFees().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Taxes Should be ", taxes, invoiceSummaryWS.getTaxes().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Adjustment Charges Should be ", adjustmentCharges, invoiceSummaryWS.getAdjustmentCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Amount Of Last Statement Should be ", amountOfLastStatement, invoiceSummaryWS.getAmountOfLastStatement().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Payment Received Should be ", paymentReceived, invoiceSummaryWS.getPaymentReceived().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("New Charges Should be ", newCharges, invoiceSummaryWS.getNewCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Total Due Should be ", totalDue, invoiceSummaryWS.getTotalDue().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals("Last Invoice Date Should be ", parseDate(lastInvoiceDate), parseDate(invoiceSummaryWS.getLastInvoiceDate()));
        
	}
	
	private String parseDate(Date date) {
		if(date == null)
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		return sdf.format(date);
	}
	
}
