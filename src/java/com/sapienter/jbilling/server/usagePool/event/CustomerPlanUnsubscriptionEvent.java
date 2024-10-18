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

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * CustomerPlanUnsubscriptionEvent
 * This is a new Event object that represents event of customer unsubscribing from the plan,
 * this is either done by deleting the plan order, or by setting active until date on the plan order.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerPlanUnsubscriptionEvent implements Event {
   
	private final Integer  entityId;
    private final OrderDTO order;
    private final String triggeringAction;
    private final boolean isPlanSwap; 

    /**
     *    @param entityId
     *    @param order
     */
    public CustomerPlanUnsubscriptionEvent(Integer entityId, OrderDTO order, String triggeringAction, boolean isPlanSwap) {
        this.entityId = entityId;
        this.order    = order;
        this.triggeringAction = triggeringAction;
        this.isPlanSwap = isPlanSwap;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    /**
     *     @return the order
     */
    public OrderDTO getOrder() {
        return order;
    }
    
    public String getTriggeringAction() {
		return triggeringAction;
	}

	public String getName() {
        return "Customer Plan Unsubscription Event - entity " + entityId;
    }

    public boolean isPlanSwap() {
        return isPlanSwap;
    }

    public String toString() {
        return getName() + " - entity " + entityId + 
        	", order: " + order.getId() + 
        	", triggeringAction: " + triggeringAction;
    }

}
