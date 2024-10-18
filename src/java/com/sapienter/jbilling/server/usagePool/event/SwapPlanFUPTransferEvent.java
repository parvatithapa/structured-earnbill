package com.sapienter.jbilling.server.usagePool.event;

import java.util.Date;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * SwapPlanFUPTransferEvent
 * This is a new Event object that represents the transfer usage 
 * of existing Free Usage Pool to new a plan.
 * @author Mahesh Shivarkar
 * @since 23-Mar-2015
 */

public class SwapPlanFUPTransferEvent implements Event  {

	private final Integer entityId;
	private final OrderDTO order;
	private final Integer existingPlanItemId; 
	private final Integer swapPlanItemId;
	private final Date effectiveDate;
	
	/**
	 * @param order
	 * @param existingPlanItemId
	 * @param swapPlanItemId
	 */
	public SwapPlanFUPTransferEvent(Integer entityId, OrderDTO order, Integer existingPlanItemId, Integer swapPlanItemId, Date effectiveDate) {
		this.entityId = entityId;
		this.order = order;
		this.existingPlanItemId = existingPlanItemId;
		this.swapPlanItemId = swapPlanItemId;
		this.effectiveDate = effectiveDate;
	}
	
	public OrderDTO getOrder() {
		return order;
	}

	public Integer getExistingPlanItemId() {
		return existingPlanItemId;
	}

	public Integer getSwapPlanItemId() {
		return swapPlanItemId;
	}

	public Date getEffectiveDate() {
        return new Date(effectiveDate.getTime());
    }

    @Override
	public String getName() {
		return "Swap Plan Subscription Event - entity " + entityId;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}
	
	@Override
	public String toString() {
		return "SwapPlanFUPTransferEvent [order: " + order
				+ ", existingPlanItemId: " + existingPlanItemId
				+ ", swapPlanItemId: " + swapPlanItemId + ", " 
				+ ", entityId: " + entityId + "]";
	}
}
