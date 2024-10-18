package com.sapienter.jbilling.server.movius;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * @author Manish Bansod
 * @since 01-04-2018
 */
public class OrderChangeUpdatePositionEvent implements Event {

	private final OrderDTO orderDTO;
	private final OrderLineDTO existingOrderLine;
	private final Integer entityId;
	private final BigDecimal newQuantity;
	private final static String EVENT_NAME = "Order Change Update Position Event";
	
	public OrderChangeUpdatePositionEvent(BigDecimal newQuantity,
			OrderDTO orderDTO, OrderLineDTO existingOrderLine, Integer entityId) {
		this.newQuantity = newQuantity;
		this.orderDTO = orderDTO;
		this.existingOrderLine = existingOrderLine;
		this.entityId = entityId;
	}

	public BigDecimal getNewQuantity() {
		return newQuantity;
	}

	public OrderDTO getOrderDTO() {
		return orderDTO;
	}

	public OrderLineDTO getExistingOrderLine() {
		return existingOrderLine;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

	@Override
	public String getName() {
		return EVENT_NAME;
	}

}
