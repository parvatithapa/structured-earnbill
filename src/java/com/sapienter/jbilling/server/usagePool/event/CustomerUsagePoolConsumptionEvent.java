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

/**
 * CustomerUsagePoolConsumptionEvent
 * This is a new Event that will be fired every time 
 * there is a change in customer usage pool quantity.
 * @author Amol Gadre
 * @since 24-Jan-2014
 */

public class CustomerUsagePoolConsumptionEvent implements Event {
	
	private final Integer entityId;
	private final Integer customerUsagePoolId;
	private final BigDecimal oldQuantity;
	private final BigDecimal newQuantity;
	
	public CustomerUsagePoolConsumptionEvent(Integer entityId, 
			Integer customerUsagePoolId, BigDecimal oldQuantity, BigDecimal newQuantity) {
		this.entityId = entityId;
		this.customerUsagePoolId = customerUsagePoolId;
		this.oldQuantity = oldQuantity;
		this.newQuantity = newQuantity;	
	}
	
	@Override
	public Integer getEntityId() {
		return entityId;
	}

	public Integer getCustomerUsagePoolId() {
		return customerUsagePoolId;
	}
	
	public BigDecimal getOldQuantity() {
		return oldQuantity;
	}
	
	public BigDecimal getNewQuantity() {
		return newQuantity;
	}
	
	@Override
	public String getName() {
        return "CustomerUsagePoolConsumptionEvent - entity " + entityId;
    }

	@Override
    public String toString() {
        return getName() +  
        	", customer usage pool: " + customerUsagePoolId + 
        	", old quantity: " + oldQuantity +
        	", new quantity: " + newQuantity;
    }
	
}
