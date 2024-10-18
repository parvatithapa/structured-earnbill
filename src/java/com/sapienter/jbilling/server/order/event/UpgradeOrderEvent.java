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

import com.sapienter.jbilling.server.system.event.Event;

/**
 *
 * @author emilc
 */
public class UpgradeOrderEvent implements Event {
    private Integer entityId;
    private Integer oldOrderId;
    private Integer upgradeOrderId;
    private Integer paymentId;

    public UpgradeOrderEvent(Integer oldOrderId, Integer upgradeOrderId, Integer paymentId, Integer entityId) {
        this.oldOrderId = oldOrderId;
        this.upgradeOrderId = upgradeOrderId;
        this.paymentId = paymentId;
        this.entityId = entityId;
    }

    public Integer getOldOrderId() {
        return oldOrderId;
    }

    public Integer getUpgradeOrderId() {
        return upgradeOrderId;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    @Override
    public String getName() {
        return "Order deleted event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return "NewOrderEvent: entityId = " + oldOrderId + " order = " + upgradeOrderId;
    }

}
