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

public class PrepaidServiceSummaryBL {

    private PrepaidServiceSummaryDAS prepaidServiceSummaryDAS = null;
    private PrepaidServiceSummaryDTO prepaidServiceSummaryDTO = null;

    public PrepaidServiceSummaryBL(Integer prepaidServiceSummaryId) {
        init();
        set(prepaidServiceSummaryId);
    }

    public PrepaidServiceSummaryBL() {
        init();
    }

    public void set(Integer prepaidServiceSummaryId) {
        prepaidServiceSummaryDTO = prepaidServiceSummaryDAS.find(prepaidServiceSummaryId);
    }

    private void init() {
        prepaidServiceSummaryDAS = new PrepaidServiceSummaryDAS();
        prepaidServiceSummaryDTO = new PrepaidServiceSummaryDTO();
    }

    public PrepaidServiceSummaryDTO prepaidServiceSummaryDTO() {
        return this.prepaidServiceSummaryDTO;
    }

    public Integer create(PrepaidServiceSummaryDTO prepaidServiceSummary) {
        prepaidServiceSummaryDTO = prepaidServiceSummaryDAS.save(prepaidServiceSummary);
        return prepaidServiceSummaryDTO.getId();
    }

    public PrepaidServiceSummaryDTO save(PrepaidServiceSummaryDTO prepaidServiceSummary) {
        return prepaidServiceSummaryDAS.save(prepaidServiceSummary);
    }

    /**
     * Delete Prepaid service summary by service summary ID
     *
     * @param serviceSummaryId
     */
    public void deleteByServiceSummaryId(Integer serviceSummaryId) {
        prepaidServiceSummaryDAS.deleteByServiceSummary(serviceSummaryId);
    }

    /**
     * Delete Prepaid service summary by invoice ID
     *
     * @param invoiceId
     */
    public void deleteAllByInvoiceId(Integer invoiceId) {
        prepaidServiceSummaryDAS.deleteAllByInvoiceId(invoiceId);
    }
    /**
     * Delete Prepaid service summary by billing process ID
     *
     * @param billingProcessId
     */
    public void deleteAllByBillingProcessId(Integer billingProcessId) {
        prepaidServiceSummaryDAS.deleteByBillingProcessId(billingProcessId);
    }
}
