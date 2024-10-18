package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.pluggableTask.BasicOrderFilterTask;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.util.Context;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin uses to filter out recurring prorated post-paid orders by considering SPC specific billing delay days.
 */
public class SPCOrderFilterTask extends BasicOrderFilterTask {

    private static final Logger logger = LoggerFactory.getLogger(SPCOrderFilterTask.class);

    @Override
    protected Date calculateBillingUntilWithBillingDelayDays(OrderDTO order, Date billingUntil) {
        logger.debug("Adding SPC specific billing delay days into billing until to filter out prorate post-paid order", order.getId());
        SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
        return order.getProrateFlagValue() ? (spcHelperService.calculateBillingUntilWithDelayDays(order.getUser().getCustomer(), billingUntil, getEntityId())) : billingUntil;
    }
}
