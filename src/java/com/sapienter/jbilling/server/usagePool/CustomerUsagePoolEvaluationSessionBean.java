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
package com.sapienter.jbilling.server.usagePool;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomerUsagePoolEvaluationSessionBean
 * This is the Spring session bean that implements
 * IcustomerUsagePoolEvaluationSessionBean interface. It is used for triggering
 * the customer usage pool evaluation task in a Spring transaction context.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */
@Transactional(propagation = Propagation.REQUIRED)
public class CustomerUsagePoolEvaluationSessionBean implements ICustomerUsagePoolEvaluationSessionBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    /* (non-Javadoc)
     * @see com.sapienter.jbilling.server.usagePool.ICustomerUsagePoolEvaluationSessionBean#trigger()
     */
    public void trigger(Integer entityId, Date runDate) {
        logger.debug("executing CustomerUsagePoolEvaluation for entity {} on {}", entityId, runDate);
        new CustomerUsagePoolBL().triggerCustomerUsagePoolEvaluation(entityId, runDate);
    }

}
