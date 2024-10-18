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

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class OrderProvisioningTask extends AbstractProvisioningTask {
    private static final Logger LOG = Logger.getLogger(OrderProvisioningTask.class);
    private static final BigDecimal ONE = new BigDecimal(1);
    private static final BigDecimal TWO = new BigDecimal(2);
    private static final BigDecimal THREE = new BigDecimal(3);

    private static final Integer SETUP_FEE_ITEM_ID = 251;

    @Override
    boolean isActionProvisionable(IProvisionable provisionable) {
        if (provisionable instanceof OrderDTO) {
            return isOrderProvisionable((OrderDTO) provisionable);
        }

        return false;
    }


    private boolean isOrderProvisionable(OrderDTO order) {
        if (order != null) {

            Date today = companyCurrentDate();

            if (order.getActiveSince() != null
                    && order.getActiveSince().before(today)
                    && ( null == order.getActiveUntil() || order.getActiveUntil().after(today) )
                ) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void activate(OrderDTO order, List<OrderLineDTO> lines, CommandManager c) {
        if (order != null) {
            // provisioning activate
            c.addCommand("activate_user");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");

            LOG.debug("Added activation commands for order " + order.getId());
        }

        OrderLineDTO setupFee = findLine(lines, SETUP_FEE_ITEM_ID);
        if (setupFee != null) {
            // external provisioning test
            if (setupFee.getQuantity().compareTo(ONE) == 0) {
                c.addCommand("result_test");
                // returns 'success' then 'unavailable'
                c.addParameter("msisdn", "98765");
                
                c.addCommand("result_test");
                // should return 'fail'
                c.addParameter("msisdn", "54321");

                LOG.debug("Added external provisioning commands for order line " + setupFee.getId());
            }

            // cai test
            if (setupFee.getQuantity().compareTo(TWO) == 0) {
                c.addCommand("cai_test");
                c.addParameter("msisdn", "98765");
                // should be removed from command
                c.addParameter("imsi", "VOID");

                LOG.debug("Added CAI provisioning commands for order line " + setupFee.getId());
            }

            // mmsc test
            if (setupFee.getQuantity().compareTo(THREE) == 0) {
                c.addCommand("mmsc_test");
                c.addParameter("msisdn", "99777");
                c.addParameter("subscriptionType", "HK");

                LOG.debug("Added MMSC provisioning commands for order line " + setupFee.getId());
            }
        }
    }

    @Override
    public void deactivate(OrderDTO order, List<OrderLineDTO> lines, CommandManager c) {
        if (order != null) {
            // provisioning deactivate
            c.addCommand("deactivate_user");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");

            LOG.debug("Added activation commands for order " + order.getId());
        }
    }
}
