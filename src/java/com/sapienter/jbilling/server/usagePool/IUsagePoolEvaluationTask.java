package com.sapienter.jbilling.server.usagePool;

import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;

public interface IUsagePoolEvaluationTask {

    /**
     * Evaluates {@link CustomerUsagePoolDTO} based on user and subscription order.
     * @param customerUsagePoolEvaluationEvent
     */
    public void evaluateCustomerUsagePool(CustomerUsagePoolEvaluationEvent customerUsagePoolEvaluationEvent);


}
