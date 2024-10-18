package com.sapienter.jbilling.server.order.event;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;

import java.util.Collection;

/**
 * Created by Taimoor Choudhary on 1/4/18.
 */
public class NewOrderAndChangeEvent implements Event {

    private Integer entityId;
    private OrderDTO order;
    private Collection<OrderChangeDTO> orderChanges;

    public NewOrderAndChangeEvent(Integer entityId, OrderDTO order, Collection<OrderChangeDTO> orderChanges) {
        this.entityId = entityId;
        this.order = order;
        this.orderChanges = orderChanges;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Order and OrderChange created event";
    }

    public OrderDTO getOrder() {
        return order;
    }

    public Collection<OrderChangeDTO> getOrderChanges() {
        return orderChanges;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NewOrderAndChangeEvent{");
        sb.append("entityId=").append(entityId);
        sb.append(", order=").append(order);
        sb.append(", orderChanges=").append(orderChanges);
        sb.append('}');
        return sb.toString();
    }
}
