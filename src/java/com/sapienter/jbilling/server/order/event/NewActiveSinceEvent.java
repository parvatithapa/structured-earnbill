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
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * @author Ashok Kale
 * @since 13-Feb-2014
 */
public class NewActiveSinceEvent implements Event {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NewActiveSinceEvent.class)); 
    private Integer entityId;
    private Integer userId;
    private Integer statusId;
    private OrderDTO oldOrder;
    private OrderDTO newOrder;
    
    public NewActiveSinceEvent(Integer orderId, 
            OrderDTO newOrder, OrderDTO oldOrder) {
        try {
            OrderBL order = new OrderBL(orderId);
            
            this.entityId = order.getEntity().getUser().getEntity().getId();
            this.userId = order.getEntity().getUser().getUserId();
            this.statusId = order.getEntity().getOrderStatus().getId();
        } catch (Exception e) {
            LOG.error("Handling order in event", e);
        } 
        this.oldOrder = oldOrder;
        this.newOrder = newOrder;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "New active until";
    }

    public String toString() {
        return getName();
    }
    public Integer getUserId() {
        return userId;
    }
    public Integer getStatusId() {
        return statusId;
    }
	public OrderDTO getOldOrder() {
		return oldOrder;
	}
	public OrderDTO getNewOrder() {
		return newOrder;
	}
    
}
