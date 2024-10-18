package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.util.Constants;

public class SPCMediationCurrentPeriodEvaluationStrategyTask extends SPCMediationEvaluationStrategyTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected Date getComparingDate(OrderProcessDTO orderProcess, int billingType) {
        logger.debug("getting compared date");
        if(Constants.ORDER_BILLING_PRE_PAID == billingType){
            Date lastPeriodStartDate = orderProcess.getLastPeriodStartDate();
            return null != lastPeriodStartDate ? lastPeriodStartDate : orderProcess.getPeriodStart();
        }
        return orderProcess.getPeriodEnd();
    }

}
