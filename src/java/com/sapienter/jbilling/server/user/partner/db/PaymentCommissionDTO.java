/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * This class captures any payment towards an invoice. The amounts are used to calculate payment based commissions.
 */
@Entity
@TableGenerator(
        name="payment_commission_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_commission",
        allocationSize=10
)
@Table(name="payment_commission")
public class PaymentCommissionDTO {
    private int id;
    private InvoiceDTO invoice;
    private BigDecimal paymentAmount;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="payment_commission_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public InvoiceDTO getInvoice () {
        return invoice;
    }

    public void setInvoice (InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    @Column(name="payment_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getPaymentAmount () {
        return paymentAmount;
    }

    public void setPaymentAmount (BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }
}
