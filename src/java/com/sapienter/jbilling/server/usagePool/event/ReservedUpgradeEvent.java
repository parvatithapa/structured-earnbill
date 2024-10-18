package com.sapienter.jbilling.server.usagePool.event;

import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.system.event.Event;

import java.math.BigDecimal;

public class ReservedUpgradeEvent implements Event{

    private String name;
    private Integer entityId;
    private OrderWS existingOrder;
    private Integer userId;
    private Integer newOrderId;
    private BigDecimal initialPriceReported;
    private BigDecimal pendingAdjustment;

    public ReservedUpgradeEvent(){}

    public ReservedUpgradeEvent(String name, Integer entityId, OrderWS existingOrder, Integer userId, Integer newOrderId, BigDecimal initialPriceReported, BigDecimal pendingAdjustment) {

        this.entityId = entityId;
        this.existingOrder = existingOrder;
        this.userId = userId;
        this.newOrderId = newOrderId;
        this.initialPriceReported = initialPriceReported;
        this.pendingAdjustment = pendingAdjustment;
    }

    @Override
    public String getName() {
        return "Reserved upgrade event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public OrderWS getExistingOrder() {
        return existingOrder;
    }

    public void setExistingOrder(OrderWS existingOrder) {
        this.existingOrder = existingOrder;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getNewOrderId() {
        return newOrderId;
    }

    public void setNewOrderId(Integer newOrderId) {
        this.newOrderId = newOrderId;
    }

    public BigDecimal getInitialPriceReported() {
        return initialPriceReported;
    }

    public void setInitialPriceReported(BigDecimal initialPriceReported) {
        this.initialPriceReported = initialPriceReported;
    }

    public BigDecimal getPendingAdjustment() {
        return pendingAdjustment;
    }

    public void setPendingAdjustment(BigDecimal pendingAdjustment) {
        this.pendingAdjustment = pendingAdjustment;
    }
}
