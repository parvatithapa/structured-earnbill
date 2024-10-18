package com.sapienter.jbilling.server.creditnote;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@SuppressWarnings("serial")
public class CreditNoteInvoiceMapWS implements Serializable {
    private Integer id;
    private Integer creditNoteId;
    private Integer invoiceId;
    private BigDecimal amount;
    private Date createDatetime;

    public CreditNoteInvoiceMapWS(){}

    public CreditNoteInvoiceMapWS(Integer id, Integer creditNoteId, Integer invoiceId, BigDecimal amount, Date createDatetime) {
        this.id = id;
        this.creditNoteId = creditNoteId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.createDatetime = createDatetime;
    }

    public Integer getId() {
        return id;
    }

    public Integer getCreditNoteId() {
        return creditNoteId;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CreditNoteInvoiceMapWS [id=");
        builder.append(id);
        builder.append(", creditNoteId=");
        builder.append(creditNoteId);
        builder.append(", invoiceId=");
        builder.append(invoiceId);
        builder.append(", amount=");
        builder.append(amount);
        builder.append(", createDatetime=");
        builder.append(createDatetime);
        builder.append("]");
        return builder.toString();
    }
}
