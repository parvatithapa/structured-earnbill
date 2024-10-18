/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.order.event;

import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.system.event.Event;

import java.math.BigDecimal;
import java.util.*;

/**
 * This event is triggered when an order is updated.
 *
 * @author Gerhard Maree
 * 
 */
public class OrderUpdatedEvent implements Event {

    private final Integer entityId;
    private final List<OrderLineDTO> oldLines;
    private final List<OrderLineDTO> newLines;
    private final Integer orderId;

    public OrderUpdatedEvent(Integer entityId, Integer orderId, List<OrderLineDTO> oldOrderLines,
                             List<OrderLineDTO> newOrderLines) {
        this.entityId = entityId;
        this.oldLines = oldOrderLines;
        this.newLines = newOrderLines;
        this.orderId = orderId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public List<OrderLineDTO> getOldLines() {
        return oldLines;
    }

    public List<OrderLineDTO> getNewLines() {
        return newLines;
    }

    public Integer getOrderId() {
        return orderId;
    }

    /**
     * Returns a list of all order lines removed in this update.
     * @return
     */
    public List<OrderLineDTO> findLinesRemoved() {
        List<OrderLineDTO> linesRemoved = new ArrayList<>();

        //We are returning the deleted lines from the list of newLines. We do this because
        //the old lines do not have order changes attached to them.
        Set<Integer> newProductIds = new HashSet<>();
        Map<Integer, OrderLineDTO> newProductIdLineMap = new HashMap<>();

        for(OrderLineDTO newLine : newLines) {
            if(newLine.getDeleted() == 0) {
                newProductIds.add(newLine.getItemId());
            }
            newProductIdLineMap.put(newLine.getId(), newLine);
        }

        for(OrderLineDTO oldLine : oldLines) {
            if(oldLine.getDeleted() == 0 && !newProductIds.contains(oldLine.getItemId())) {
                linesRemoved.add(newProductIdLineMap.get(oldLine.getId()));
            }
        }
        return linesRemoved;
    }

    @Override
    public String toString() {
        return "OrderUpdatedEvent{" +
                "entityId=" + entityId +
                ", orderId=" + orderId +
                '}';
    }

    public String getName() {
        return "Order Updated Event - entity " + entityId;
    }
}
