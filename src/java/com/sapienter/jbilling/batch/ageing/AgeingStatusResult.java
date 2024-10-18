package com.sapienter.jbilling.batch.ageing;

import java.util.List;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

/**
 * Collects the status of the ageing processing
 *
 * @author Panche Isajeski
 * @since 30-Jan-2014
 */
public class AgeingStatusResult {

    private Integer userId;
    private List<InvoiceDTO> overdueInvoices;

    public AgeingStatusResult(Integer userId, List<InvoiceDTO> overdueInvoices) {
        this.userId = userId;
        this.overdueInvoices = overdueInvoices;
    }

    public Integer getUserId () {
        return userId;
    }

    public List<InvoiceDTO> getOverdueInvoices () {
        return overdueInvoices;
    }

    @Override
    public String toString () {
        return "AgeingStatusResult{" + "userId=" + userId + ", overdueInvoices=" + overdueInvoices + '}';
    }
}
