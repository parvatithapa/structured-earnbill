package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.lang.invoke.MethodHandles;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.util.Constants;

public class SPCMediationEvaluationStrategyTask extends AssetMediationEvaluationStrategyTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected Date calculateActualEventDate() {
        logger.debug("SPCMediationEvaluationStrategyTask.calculateActualEventDate()");
        if(null == data.getSubscriptionOrder()) {
            throw new SessionInternalError(Constants.SUBSCRIPTION_ORDER_NOT_FOUND);
        }
        OrderProcessDTO orderProcess = new OrderProcessDAS().getOrderProcessByOrderId(data.getSubscriptionOrder().getId());
        if(null != orderProcess) {
            Date compareDate = getComparingDate(orderProcess, data.getSubscriptionOrder().getOrderBillingType().getId());
            java.time.LocalDate localEventDate = null != data.getEventDate() ?
                    data.getEventDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
                    if(null != localEventDate && localEventDate.isBefore(null != compareDate ?
                            compareDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)) {
                        return compareDate;
                    } else {
                        return data.getEventDate();
                    }
        }
        return data.getEventDate();
    }

    protected Date getComparingDate(OrderProcessDTO orderProcess, int billingType) {
        logger.debug("getting compaire dates");
        if(Constants.ORDER_BILLING_PRE_PAID == billingType){
            return orderProcess.getPeriodStart();
        }
        return orderProcess.getPeriodEnd();
    }
}
