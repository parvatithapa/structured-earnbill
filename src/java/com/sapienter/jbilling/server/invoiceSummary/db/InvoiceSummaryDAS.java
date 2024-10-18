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
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
/**
 * @author Ashok Kale
 */
public class InvoiceSummaryDAS extends AbstractDAS<InvoiceSummaryDTO> {

    /**
     * Find Invoice Summary by invoice Id
     * @param invoiceId
     * @return
     */
    @SuppressWarnings("unchecked")
    public InvoiceSummaryDTO findInvoiceSummaryByInvoice(Integer invoiceId) {
        Criteria criteria = getSession().createCriteria(InvoiceSummaryDTO.class)
                .add(Restrictions.eq("creationInvoiceId", invoiceId));
        List<InvoiceSummaryDTO> results = criteria.list();
        // change types are unique by name within entity
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    private static final String DELETE_INVOICE_SUMMARY_BY_INVOICE_ID_SQL = "DELETE InvoiceSummaryDTO WHERE creationInvoiceId = :invoiceId";

    public void deleteByInvoice(Integer invoiceId) {
        Query query = getSession().createQuery(DELETE_INVOICE_SUMMARY_BY_INVOICE_ID_SQL);
        query.setParameter("invoiceId", invoiceId);
        query.executeUpdate();
    }

    private static final String DELETE_INVOICE_SUMMARY_BY_BILLING_PROCESS_ID_SQL =
            "DELETE FROM invoice_summary WHERE creation_invoice_id IN (SELECT id FROM invoice WHERE billing_process_id = :billingProcessId)";

    public void deleteByBillingProcessId(Integer billingProcessId) {
        Query query = getSession().createSQLQuery(DELETE_INVOICE_SUMMARY_BY_BILLING_PROCESS_ID_SQL);
        query.setParameter("billingProcessId", billingProcessId);
        query.executeUpdate();
    }

    /**
     * Returns amount of last statement.
     *
     * @param invoiceId invoice Id
     * @return amount of last statement.
     */
    public BigDecimal getAmountOfLastStatement(Integer invoiceId) {
        String query = "select total_due from invoice_summary where creation_invoice_id =:invoiceId";
        SQLQuery sqlQuery= getSession().createSQLQuery(query);
        sqlQuery.setParameter("invoiceId", invoiceId);
        return (BigDecimal) (null != sqlQuery.uniqueResult() ? sqlQuery.uniqueResult() : BigDecimal.ZERO);
    }
}
