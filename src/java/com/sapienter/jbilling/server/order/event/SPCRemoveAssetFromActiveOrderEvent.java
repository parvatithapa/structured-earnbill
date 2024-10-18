package com.sapienter.jbilling.server.order.event;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.system.event.Event;

public class SPCRemoveAssetFromActiveOrderEvent implements Event {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Integer entityId;
    private Integer orderId;

    public SPCRemoveAssetFromActiveOrderEvent() {
    }

    public SPCRemoveAssetFromActiveOrderEvent(Integer orderId) {
        try {
            this.orderId = orderId;
            OrderBL order = new OrderBL(orderId);
            this.entityId = order.getDTO().getUser().getEntity().getId();
        } catch(Exception e) {
            logger.error("Handling order in event", e);
        }
    }

    public Integer getOrderId() {
        return orderId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "remove asset";
    }

}
