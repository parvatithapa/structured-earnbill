package com.sapienter.jbilling.server.integration;


import java.math.BigDecimal;

import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;

public interface MeteredUsageService {
    static final String BEAN_NAME = "meteredUsageService";
    static final String JOB_NAME = "meteredUsageUploadJob";

    void runJob(int entityId, String jobName, MeteredUsageContext meteredUsageContext);

    void sendPlanPurchaseCharge(int entityId, OrderLineWS orderLineWS, OrderWS orderWS, Integer planId,
                                 MeteredUsageContext meteredUsageContext);
    void sendPlanUpgradeAdjustment(int entityId, OrderWS initialOrder, Integer newOrderId, Integer userId, BigDecimal initialPriceReported, BigDecimal pendingAdjustment, MeteredUsageContext meteredUsageContext);


}
