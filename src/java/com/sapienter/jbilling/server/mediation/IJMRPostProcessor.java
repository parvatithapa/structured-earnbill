package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

public interface IJMRPostProcessor {

    /**
     * add logic after processing of {@link JbillingMediationRecord}.
     * @param jmr
     * @param oldLines
     * @param updatedOrder
     * @param eventResult
     */
    void afterProcessing(JbillingMediationRecord jmr, OrderDTO updatedOrder, MediationEventResult eventResult);

    /**
     * Add logic after undoing {@link JbillingMediationRecord}.
     * @param jmr
     * @param mediatedLine
     */
    void afterProcessingUndo(JbillingMediationRecord jmr, OrderLineDTO mediatedLine);
}
