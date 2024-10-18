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
package com.sapienter.jbilling.server.servicesummary;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * @author Krunal bhavsar
 * @since 13-May-2019
 */
public class ServiceSummaryDAS extends AbstractDAS<ServiceSummaryDTO> {

    /**
     * Find Service Summary by invoice Id
     *
     * @param invoiceId
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<ServiceSummaryDTO> findServiceSummariesByInvoice(Integer invoiceId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("invoiceId", invoiceId));
        return criteria.list();
    }

    private static final String DELETE_SERVICE_SUMMARY_BY_INVOICE_ID_SQL = "DELETE ServiceSummaryDTO WHERE invoiceId = :invoiceId";

    public void deleteByInvoice(Integer invoiceId) {
        Query query = getSession().createQuery(DELETE_SERVICE_SUMMARY_BY_INVOICE_ID_SQL);
        query.setParameter("invoiceId", invoiceId);
        query.executeUpdate();
    }

    private static final String DELETE_SERVICE_SUMMARY_BY_BILLING_PROCESS_ID_SQL =
            "DELETE FROM service_summary WHERE invoice_id IN (SELECT id FROM invoice WHERE billing_process_id = :billingProcessId)";

    public void deleteByBillingProcessId(Integer billingProcessId) {
        Query query = getSession().createSQLQuery(DELETE_SERVICE_SUMMARY_BY_BILLING_PROCESS_ID_SQL);
        query.setParameter("billingProcessId", billingProcessId);
        query.executeUpdate();
    }

}
