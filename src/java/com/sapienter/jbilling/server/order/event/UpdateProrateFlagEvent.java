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

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;

/** UpdateProrateFlagEvent
 * Fire event when order prorate flag get change.
 * @author Ashok Kale
 * @since 13-Feb-2014
 */
public class UpdateProrateFlagEvent implements Event {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UpdateProrateFlagEvent.class)); 
    private Integer entityId;
    private Integer userId;
    private OrderDTO newOrder;
    
    public UpdateProrateFlagEvent(OrderDTO newOrder) {
        try {
            this.entityId = newOrder.getUser().getEntity().getId();
            this.userId = newOrder.getUser().getUserId();
        } catch (Exception e) {
            LOG.error("Handling order in event", e);
        } 
        this.newOrder = newOrder;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Update Order Prorate Flag Event";
    }

    public String toString() {
        return getName();
    }
    public Integer getUserId() {
        return userId;
    }
	public OrderDTO getNewOrder() {
		return newOrder;
	}
    
}
