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

import java.util.Date;

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
	private final Integer customerUsagePoolId;
    private final UsagePoolConsumptionActionDTO action;
    private final Date activeSince;

    public UsagePoolConsumptionFeeChargingEvent(Integer entityId, Integer customerUsagePoolId, UsagePoolConsumptionActionDTO action,
            Date activeSince) {
		this.entityId = entityId;
		this.customerUsagePoolId = customerUsagePoolId;
        this.action = action;
        this.activeSince = activeSince;
	}
	
    public Integer getCustomerUsagePoolId() {
		return customerUsagePoolId;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

    public UsagePoolConsumptionActionDTO getAction() {
        return action;
    }

    public Date getActiveSince() {
        return activeSince;
    }

    @Override
	public String getName() {
        return "UsagePoolConsumptionFeeChargingEvent - entity " + entityId;
    }

	@Override
	public String toString() {
		return "UsagePoolConsumptionFeeChargingEvent [entityId="
				+ entityId + ", customerUsagePoolId=" + customerUsagePoolId + "]";
	}

}
