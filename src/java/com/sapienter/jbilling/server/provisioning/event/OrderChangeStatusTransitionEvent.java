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

package com.sapienter.jbilling.server.provisioning.event;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class OrderChangeStatusTransitionEvent implements Event {
    private final Integer  entityId;
    private final OrderChangeDTO orderChangeDTO;
    private final Integer oldStatus;
    private final Integer newStatus;

    public OrderChangeStatusTransitionEvent(Integer entityId, OrderChangeDTO orderChangeDTO, Integer oldStatus, Integer newStatus) {
        this.entityId  = entityId;
        this.orderChangeDTO = orderChangeDTO;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Integer getOldStatus() {
        return oldStatus;
    }

    public Integer getNewStatus() {
        return newStatus;
    }

    public OrderChangeDTO getOrderChange() {
        return orderChangeDTO;
    }

    @Override
    public String getName() {
        return "Order Change Status Transaction Event - entity " + entityId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public String toString() {
        return getName() + " - entity " + entityId;
    }
}
