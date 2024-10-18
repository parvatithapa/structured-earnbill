/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

import java.math.BigDecimal;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionActionDTO;

/**
 * UsagePoolConsumptionNotificationEvent
 * This is a new Event that will be fired when it is defined as 
 * a consumption action on the FUP for a certain consumption %.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

public class UsagePoolConsumptionNotificationEvent implements Event {

	private final Integer entityId;
	private final Integer customerUsagePoolId;
	private final UsagePoolConsumptionActionDTO action;
	
	public UsagePoolConsumptionNotificationEvent(Integer entityId, 
										Integer customerUsagePoolId,
                                        UsagePoolConsumptionActionDTO action) {
		this.entityId = entityId;
		this.customerUsagePoolId = customerUsagePoolId;
		this.action = action;
	}
	
	@Override
	public Integer getEntityId() {
		return entityId;
	}

	public Integer getCustomerUsagePoolId() {
		return customerUsagePoolId;
	}

    public UsagePoolConsumptionActionDTO getAction() {
        return action;
    }

    @Override
	public String getName() {
        return "UsagePoolConsumptionNotificationEvent - entity " + entityId;
    }

	@Override
	public String toString() {
		return "UsagePoolConsumptionNotificationEvent [entityId=" + entityId
				+ ", customerUsagePoolId=" + customerUsagePoolId
				+ ", actionId=" + action.getId()+ "]";
	}
	
}