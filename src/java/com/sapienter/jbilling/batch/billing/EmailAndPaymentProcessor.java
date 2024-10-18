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

package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;

/**
 * @author Igor Poteryaev
 */
public class EmailAndPaymentProcessor implements ItemProcessor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean local;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Override
    public Integer process (Integer userId) {
        long enteringTime = System.currentTimeMillis();
        logger.trace("Billing process ID: {}, User ID: {} +++ Entering process(userId)", billingProcessId, userId);

        logger.debug("Sending email and processing payments for UserId # {}", userId);
        for (Integer invoiceId : local.getInvoiceIdsByBillingProcessAndByUser(billingProcessId, userId)) {
            logger.debug("Sending email for invoice {} for UserId {} ", invoiceId, userId);
            local.email(entityId, invoiceId, billingProcessId);
        }
        logger.debug("Billing process ID: {}, User ID: {}  +++ done email & payment.", billingProcessId, userId);

        logger.trace("Billing process ID: {}, User ID: {} processed in {} secs. +++ Leaving process(userId)",
                billingProcessId, userId, (System.currentTimeMillis() - enteringTime) / 1000);
        return userId;
    }
}
