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

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionActionDTO;

/**
 * UsagePoolConsumptionFeeChargingEvent
 * This is a new Event that will be fired when it is defined 
 * on the FUP as a consumption action for a certain consumption %.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

public class UsagePoolConsumptionFeeChargingEvent implements Event {
	
	private final Integer entityId;
	private final Integer userId;
    private final UsagePoolConsumptionActionDTO action;


    public UsagePoolConsumptionFeeChargingEvent(Integer entityId, Integer userId, UsagePoolConsumptionActionDTO action) {
		this.entityId = entityId;
		this.userId = userId;
        this.action = action;
	}
	
	public Integer getUserId() {
		return userId;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

    public UsagePoolConsumptionActionDTO getAction() {
        return action;
    }

    @Override
	public String getName() {
        return "UsagePoolConsumptionFeeChargingEvent - entity " + entityId;
    }

	@Override
	public String toString() {
		return "UsagePoolConsumptionFeeChargingEvent [entityId="
				+ entityId + ", userId=" + userId + "]";
	}

}
