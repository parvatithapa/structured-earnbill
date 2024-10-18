/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.servicesummary;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

public class PrepaidServiceSummaryDAS extends AbstractDAS<PrepaidServiceSummaryDTO> {
    private static final String DELETE_PREPAID_SERVICE_SUMMARY_BY_SERVICE_SUMMARY_ID_SQL = "DELETE PrepaidServiceSummaryDTO WHERE serviceSummaryId = :serviceSummaryId";
    private static final String DELETE_PREPAID_SERVICE_SUMMARY_BY_INVOICE_ID_SQL = "DELETE FROM prepaid_service_summary WHERE service_summary_id IN (SELECT id FROM service_summary WHERE invoice_id = :invoiceId)";
    private static final String DELETE_PREPAID_SERVICE_SUMMARY_BY_BILLING_PROCESS_ID_SQL =
            "DELETE FROM prepaid_service_summary WHERE service_summary_id IN (SELECT id FROM service_summary WHERE invoice_id IN (SELECT id FROM invoice WHERE billing_process_id = :billingProcessId))";

    public void deleteByServiceSummary(Integer serviceSummaryId) {
        Query query = getSession().createQuery(DELETE_PREPAID_SERVICE_SUMMARY_BY_SERVICE_SUMMARY_ID_SQL);
        query.setParameter("serviceSummaryId", serviceSummaryId);
        query.executeUpdate();
    }
    public void deleteAllByInvoiceId(Integer invoiceId) {
        Query query = getSession().createSQLQuery(DELETE_PREPAID_SERVICE_SUMMARY_BY_INVOICE_ID_SQL);
        query.setParameter("invoiceId", invoiceId);
        query.executeUpdate();
    }

    public void deleteByBillingProcessId(Integer billingProcessId) {
        Query query = getSession().createSQLQuery(DELETE_PREPAID_SERVICE_SUMMARY_BY_BILLING_PROCESS_ID_SQL);
        query.setParameter("billingProcessId", billingProcessId);
        query.executeUpdate();
    }
}
