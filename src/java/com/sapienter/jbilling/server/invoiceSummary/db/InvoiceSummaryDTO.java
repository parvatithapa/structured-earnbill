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
package com.sapienter.jbilling.server.invoiceSummary.db;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

@Entity
@TableGenerator(
        name = "invoice_summary_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "invoice_summary",
        allocationSize = 100)
@Table(name="invoice_summary")
public class InvoiceSummaryDTO extends AbstractDescription implements Exportable {
	
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
	
	public InvoiceSummaryDTO() {
	}

	public InvoiceSummaryDTO(int id, int creationInvoiceId, Integer userId,
			BigDecimal monthlyCharges, BigDecimal usageCharges,
			BigDecimal fees, BigDecimal taxes, BigDecimal adjustmentCharges,
			BigDecimal amountOfLastStatement, BigDecimal paymentReceived,
			BigDecimal newCharges, BigDecimal totalDue, Date invoiceDate, 
			Date lastInvoiceDate,  Date createDatetime) {
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

	@Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "invoice_summary_GEN")
    @Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    @Column(name = "creation_invoice_id", nullable = false)
	public int getCreationInvoiceId() {
		return creationInvoiceId;
	}

	public void setCreationInvoiceId(int creationInvoiceId) {
		this.creationInvoiceId = creationInvoiceId;
	}

    @Column(name = "user_id", nullable = false)
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	@Column(name = "monthly_charges", nullable = false, precision = 17, scale = 17)
	public BigDecimal getMonthlyCharges() {
		return monthlyCharges;
	}

	public void setMonthlyCharges(BigDecimal monthlyCharges) {
		this.monthlyCharges = monthlyCharges;
	}

	@Column(name = "usage_charges", nullable = false, precision = 17, scale = 17)	
	public BigDecimal getUsageCharges() {
		return usageCharges;
	}

	public void setUsageCharges(BigDecimal usageCharges) {
		this.usageCharges = usageCharges;
	}

	@Column(name = "fees", nullable = false, precision = 17, scale = 17)
	public BigDecimal getFees() {
		return fees;
	}

	public void setFees(BigDecimal fees) {
		this.fees = fees;
	}

	@Column(name = "taxes", nullable = false, precision = 17, scale = 17)
	public BigDecimal getTaxes() {
		return taxes;
	}

	public void setTaxes(BigDecimal taxes) {
		this.taxes = taxes;
	}

	@Column(name = "adjustment_charges", nullable = false, precision = 17, scale = 17)
	public BigDecimal getAdjustmentCharges() {
		return adjustmentCharges;
	}

	public void setAdjustmentCharges(BigDecimal adjustmentCharges) {
		this.adjustmentCharges = adjustmentCharges;
	}

	@Column(name = "amount_of_last_statement", nullable = false, precision = 17, scale = 17)
	public BigDecimal getAmountOfLastStatement() {
		return amountOfLastStatement;
	}

	public void setAmountOfLastStatement(BigDecimal amountOfLastStatement) {
		this.amountOfLastStatement = amountOfLastStatement;
	}

	@Column(name = "payment_received", nullable = false, precision = 17, scale = 17)
	public BigDecimal getPaymentReceived() {
		return paymentReceived;
	}
	
	public void setPaymentReceived(BigDecimal paymentReceived) {
		this.paymentReceived = paymentReceived;
	}

	@Column(name = "new_charges", nullable = false, precision = 17, scale = 17)
	public BigDecimal getNewCharges() {
		return newCharges;
	}

	public void setNewCharges(BigDecimal newCharges) {
		this.newCharges = newCharges;
	}
	
	@Column(name = "total_due", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalDue() {
		return totalDue;
	}

	public void setTotalDue(BigDecimal totalDue) {
		this.totalDue = totalDue;
	}

	@Column(name = "invoice_date", nullable = false)
	public Date getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	@Column(name = "last_invoice_date", nullable = true)
	public Date getLastInvoiceDate() {
		return lastInvoiceDate;
	}

	public void setLastInvoiceDate(Date lastInvoiceDate) {
		this.lastInvoiceDate = lastInvoiceDate;
	}

	@Column(name = "create_datetime", nullable = false)
	public Date getCreateDatetime() {
		return createDatetime;
	}

	public void setCreateDatetime(Date createDatetime) {
		this.createDatetime = createDatetime;
	}

	
    @Override
	public String toString() {
		return "InvoiceSummaryDTO [id=" + id + ", userId=" + userId 
				+ ", creationInvoiceId=" + creationInvoiceId 
				+ ", monthlyCharges=" + monthlyCharges
				+ ", usageCharges=" + usageCharges + ", fees=" + fees
				+ ", taxes=" + taxes + ", adjustmentCharges="
				+ adjustmentCharges + ", amountOfLastStatement="
				+ amountOfLastStatement + ", paymentReceived="
				+ paymentReceived + ", newCharges=" + newCharges 
				+ ", totalDue=" + totalDue
				+ ", invoiceDate=" + invoiceDate
				+ ", lastInvoiceDate=" + lastInvoiceDate
				+ ", createDatetime=" + createDatetime + "]";
	}

	@Transient
    public String[] getFieldNames() {
        return new String[] {
                "id",
                "userId",
                "creationInvoiceId",
                "monthlyCharges",
                "usageCharges",
                "fees",
                "taxes",
                "adjustmentCharges",
                "amountOfLastStatement",
                "paymentReceived",
                "newCharges",
                "totalDue",
                "invoiceDate",
                "createDatetime",
        };
    }

    @Transient
    public Object[][] getFieldValues() {
          return null;
        }

	@Transient
	protected String getTable() {
		return Constants.TABLE_INVOICE_SUMMARY;
	}

}
