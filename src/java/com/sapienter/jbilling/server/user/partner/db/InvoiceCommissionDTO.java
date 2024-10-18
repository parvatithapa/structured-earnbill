package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Commission paid based on invoices (invoiced amounts or payments received for an invoice).
 */
@Entity
@DiscriminatorValue("INVOICE")
public class InvoiceCommissionDTO extends PartnerCommissionLineDTO {
    private InvoiceDTO invoice;
    private BigDecimal standardAmount = BigDecimal.ZERO;
    private BigDecimal masterAmount = BigDecimal.ZERO;
    private BigDecimal exceptionAmount = BigDecimal.ZERO;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public InvoiceDTO getInvoice () {
        return invoice;
    }

    public void setInvoice (InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    @Column(name="standard_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getStandardAmount () {
        return standardAmount;
    }

    public void setStandardAmount (BigDecimal standardAmount) {
        this.standardAmount = standardAmount;
    }

    @Column(name="master_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getMasterAmount () {
        return masterAmount;
    }

    public void setMasterAmount (BigDecimal masterAmount) {
        this.masterAmount = masterAmount;
    }

    @Column(name="exception_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getExceptionAmount () {
        return exceptionAmount;
    }

    public void setExceptionAmount (BigDecimal exceptionAmount) {
        this.exceptionAmount = exceptionAmount;
    }

    @Transient
    public Type getType() {
        return Type.INVOICE;
    }

    @Override
    public PartnerCommissionLineDTO createReversal() {
        InvoiceCommissionDTO reversal = new InvoiceCommissionDTO();
        if(exceptionAmount != null) {
            reversal.setExceptionAmount(exceptionAmount.negate());
        }
        if(standardAmount != null) {
            reversal.setStandardAmount(standardAmount.negate());
        }
        if(masterAmount != null) {
            reversal.setMasterAmount(masterAmount.negate());
        }
        reversal.setPartner(getPartner());
        reversal.setInvoice(getInvoice());
        return reversal;
    }
}
