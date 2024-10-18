/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import org.apache.log4j.Logger;


public class PaymentProvisioningTask extends AbstractProvisioningTask{

    private static final Logger LOG = Logger.getLogger(ExampleProvisioningTask.class);

    @Override
    boolean isActionProvisionable(IProvisionable provisionable) {
        return true;
    }

    @Override
    public void paymentProvisioning(PaymentDTOEx payment, CommandManager c) {
        if (payment != null) {
            c.addCommand("payment_successful_provisioning_command");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");
            LOG.debug("Added command for provisioning when the payment made. Payment " + payment.getId());
        }
    }
}
