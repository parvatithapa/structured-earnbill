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

import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.system.event.Event;

import java.util.Date;

/**
 * This event is triggered when a order is reactivated.
 *
 * @author Leandro Zoi
 * @since 01/15/18
 */
public class NewActivateOrderEvent implements Event {
    private Date startDate;
    private Integer entityId;
    private Integer orderId;
    private Integer userId;
    private PlanDTO plan;

    public NewActivateOrderEvent(Integer orderId, PlanDTO plan, Date startDate) {
        OrderBL order = new OrderBL(orderId);
        this.entityId = order.getEntity().getUser().getEntity().getId();
        this.userId = order.getEntity().getUser().getUserId();
        this.orderId = orderId;
        this.plan = plan;
        this.startDate = startDate;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "New Suspendend Order Event " + entityId;
    }

    public String toString() {
        return getName();
    }

	public Integer getOrderId() {
        return orderId;
    }

    public Integer getUserId() {
        return userId;
    }

    public PlanDTO getPlan() {
        return plan;
    }

    public Date getStartDate() {
        return startDate;
    }
}
