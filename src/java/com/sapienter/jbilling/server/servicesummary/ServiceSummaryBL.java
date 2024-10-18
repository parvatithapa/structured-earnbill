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

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Krunal Bhavsar
 *
 */
public class ServiceSummaryBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ServiceSummaryDAS serviceSummaryDas = null;
    private ServiceSummaryDTO serviceSummaryDto = null;

    public ServiceSummaryBL(Integer serviceSummaryId) {
        init();
        set(serviceSummaryId);
    }

    public ServiceSummaryBL() {
        init();
    }

    public void set(Integer serviceSummaryId) {
        serviceSummaryDto = serviceSummaryDas.find(serviceSummaryId);
    }

    private void init() {
        serviceSummaryDas = new ServiceSummaryDAS();
        serviceSummaryDto = new ServiceSummaryDTO();
    }

    public ServiceSummaryDTO getServiceSummaryDTO() {
        return this.serviceSummaryDto;
    }

    public Integer create(ServiceSummaryDTO serviceSummary) {
        serviceSummaryDto = serviceSummaryDas.save(serviceSummary);
        logger.debug("service summary saved {}", serviceSummary);
        return serviceSummaryDto.getId();
    }

    /**
     * Delete Service summary by invoice ID
     *
     * @param invoiceId
     */
    public void deleteByInvoice(Integer invoiceId) {
        serviceSummaryDas.deleteByInvoice(invoiceId);
    }

    /**
     * Delete Service summary by billingProcessId
     *
     * @param billingProcessId
     */
    public void deleteByBillingProcess(Integer billingProcessId) {
        serviceSummaryDas.deleteByBillingProcessId(billingProcessId);
    }

    /**
     * Delete Service summary by service ID
     *
     * @param serviceId
     */
    public void delete(Integer serviceId) {
        serviceSummaryDto = serviceSummaryDas.find(serviceId);
        if (null != serviceSummaryDto) {
            serviceSummaryDas.delete(serviceSummaryDto);
        }
    }

    public ServiceSummaryDTO save(ServiceSummaryDTO serviceSummaryDTO) {
        return serviceSummaryDas.save(serviceSummaryDTO);
    }

}
