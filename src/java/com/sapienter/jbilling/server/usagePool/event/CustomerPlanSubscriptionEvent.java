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

package com.sapienter.jbilling.server.usagePool.event;

import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.system.event.Event;

import java.util.Date;

/**
 * CustomerPlanSubscriptionEvent
 * This is a new Event object that represents the successful 
 * subscription of a customer to a plan.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerPlanSubscriptionEvent implements Event {

    private final Integer entityId;
    private final OrderLineDTO line;
    private final OrderDTO order;
    private final PlanDTO plan;
    private final Date endDate;

    /**
     * 
     * @param entityId
     * @param order
     * @param plan
     */
    public CustomerPlanSubscriptionEvent(Integer entityId, OrderDTO order, PlanDTO plan, OrderLineDTO line, Date endDate) {
        this.entityId = entityId;
        this.order = order;
        this.plan = plan;
        this.line = line;
        this.endDate = endDate;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    /**
     *     @return the order
     */
    public OrderLineDTO getOrderLine() {
        return line;
    }
    
    /**
     *     @return the plan
     */
    public PlanDTO getPlan() {
        return plan;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getName() {
        return "Customer Plan Subscription Event - entity " + entityId;
    }
    
    /**
     *     @return the order
     */
    public OrderDTO getOrder() {
		return order;
	}

	public String toString() {
        return getName();
    }

}
