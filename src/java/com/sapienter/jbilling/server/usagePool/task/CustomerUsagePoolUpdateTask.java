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

package com.sapienter.jbilling.server.usagePool.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.usagePool.QuantitySupplier;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDTO;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.order.event.OrderDeletedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerUsagePoolConsumptionEvent;
import org.hibernate.ObjectNotFoundException;

/**
 * CustomerUsagePoolUpdateTask
 * This is an internal events task that subscribes to NewOrderEvent, 
 * NewQuantityEvent and OrderDeletedEvent. Its function is to update 
 * the customer usage pool quantity looking at the usage of the items 
 * for the order being created, updated or deleted.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerUsagePoolUpdateTask extends PluggableTask
implements IInternalEventsTask {

	private static final Logger LOG = Logger.getLogger(CustomerUsagePoolUpdateTask.class);
	
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] { 
        OrderDeletedEvent.class,
        NewOrderEvent.class,
        NewQuantityEvent.class,
    };

	public Class<Event>[] getSubscribedEvents () {
		return events;
	}

	@Override
	public void process(Event event) throws PluggableTaskException {
		
		LOG.debug("Entering Customer Usage Pool Update - event: " + event);
		Integer entityId = event.getEntityId();
		
		if (event instanceof NewOrderEvent) {
			 	
			NewOrderEvent newOrderEvent = (NewOrderEvent) event;
		 			
			Set<OrderLineUsagePoolDTO>  olUsagePools = newOrderEvent.getOrder().getFreeUsagePools();
			
			if (null != olUsagePools) {
			
				Map<Integer, BigDecimal> customerUsagePoolsMap = new HashMap<Integer, BigDecimal>();
				
				for (OrderLineUsagePoolDTO olUsagePool : olUsagePools) {
					BigDecimal customerUsagePoolQuantity = customerUsagePoolsMap.get(olUsagePool.getCustomerUsagePool().getId());
					if (null != customerUsagePoolQuantity) {
						customerUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), customerUsagePoolQuantity.add(olUsagePool.getQuantity()));
					} else {
						customerUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), olUsagePool.getQuantity());
					}
				}
				
				for (Entry<Integer, BigDecimal> customerUsagePoolEntry : customerUsagePoolsMap.entrySet()) {
					CustomerUsagePoolDTO customerUsagePoolDTO = new CustomerUsagePoolDAS().findForUpdate(customerUsagePoolEntry.getKey());
					BigDecimal tierQuantity = customerUsagePoolEntry.getValue();
					if (customerUsagePoolDTO.getQuantity().subtract(tierQuantity).compareTo(BigDecimal.ZERO) < 0) {
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
							new CustomerUsagePoolConsumptionEvent(
									entityId, 
									customerUsagePoolDTO.getId(), 
									customerUsagePoolDTO.getQuantity(), 
									BigDecimal.ZERO,
									null);
						
						customerUsagePoolDTO.setQuantity(BigDecimal.ZERO);
						
						EventManager.process(consumptionEvent);
						
					} else {
						
						BigDecimal newPoolQuantity = customerUsagePoolDTO.getQuantity().subtract(tierQuantity);
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDTO.getId(), 
										customerUsagePoolDTO.getQuantity(), 
										newPoolQuantity,
										null);
						
						customerUsagePoolDTO.setQuantity(newPoolQuantity);
						
						EventManager.process(consumptionEvent);
					}
				}
				
				BigDecimal orderLineUsagePoolQuantitySum = newOrderEvent.getOrder().getFreeUsagePoolsTotalQuantity();
				LOG.debug("orderLineUsagePoolQuantitySum ::"+ orderLineUsagePoolQuantitySum); 
				newOrderEvent.getOrder().setFreeUsageQuantity(orderLineUsagePoolQuantitySum);
			}
		} else if (event instanceof NewQuantityEvent) {
			 
			NewQuantityEvent newQuantityEvent = (NewQuantityEvent) event;
			OrderLineDTO oldLine = newQuantityEvent.getOrderLine();
			OrderLineDTO newLine = newQuantityEvent.getNewOrderLine();
			Map<Integer, BigDecimal> oldCustomerUsagePoolsMap = new HashMap<Integer, BigDecimal>();
			Map<Integer, BigDecimal> newCustomerUsagePoolsMap = new HashMap<Integer, BigDecimal>();

			LOG.debug("oldLine: " + oldLine);
			LOG.debug("newLine: " + newLine);
			
			BigDecimal updateQuantity = BigDecimal.ZERO;
			
			if (null != oldLine && oldLine.hasOrderLineUsagePools()) { 
			
				for (OrderLineUsagePoolDTO olUsagePool : oldLine.getOrderLineUsagePools()) {
					BigDecimal oldCustomerUsagePoolQuantity = oldCustomerUsagePoolsMap.get(olUsagePool.getCustomerUsagePool().getId());
					if (null != oldCustomerUsagePoolQuantity) {
						oldCustomerUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), oldCustomerUsagePoolQuantity.add(olUsagePool.getQuantity()));
					} else {
						oldCustomerUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), olUsagePool.getQuantity());
					}
				}
			}
			
			LOG.debug("oldCustomerUsagePoolsMap: " + oldCustomerUsagePoolsMap);
			 	
			if (null != newLine && newLine.hasOrderLineUsagePools()) {
				
				for (OrderLineUsagePoolDTO olUsagePool : newLine.getOrderLineUsagePools()) {
					BigDecimal newCustomerUsagePoolQuantity = newCustomerUsagePoolsMap.get(olUsagePool.getCustomerUsagePool().getId());
					if (null != newCustomerUsagePoolQuantity) {
						newCustomerUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), newCustomerUsagePoolQuantity.add(olUsagePool.getQuantity()));
					} else {
						newCustomerUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), olUsagePool.getQuantity());
					}
				}
	
				LOG.debug("newCustomerUsagePoolsMap: " + newCustomerUsagePoolsMap);
	
				for (Entry<Integer, BigDecimal> newEntry : newCustomerUsagePoolsMap.entrySet()) {
					Integer newCustomerUsagePoolId = newEntry.getKey();
					BigDecimal newQuantity = newEntry.getValue();
					BigDecimal oldQuantity = oldCustomerUsagePoolsMap.get(newCustomerUsagePoolId);
					if (null != oldQuantity && null != newQuantity){
						updateQuantity = newQuantity.subtract(oldQuantity);
					} else if (null == oldQuantity && null != newQuantity) {
						updateQuantity = newQuantity;
					}
					
					LOG.debug("updateQuantity: " + updateQuantity);
					
					CustomerUsagePoolDTO customerUsagePoolDTO = new CustomerUsagePoolDAS().findForUpdate(newCustomerUsagePoolId);
					if (customerUsagePoolDTO.getQuantity().subtract(updateQuantity).compareTo(BigDecimal.ZERO) < 0) {
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDTO.getId(), 
										customerUsagePoolDTO.getQuantity(), 
										BigDecimal.ZERO,
										null);
						
						customerUsagePoolDTO.setQuantity(BigDecimal.ZERO);
						
						EventManager.process(consumptionEvent);
						
					} else {
						
						BigDecimal newPoolQuantity = customerUsagePoolDTO.getQuantity().subtract(updateQuantity);
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDTO.getId(), 
										customerUsagePoolDTO.getQuantity(), 
										newPoolQuantity,
										null);
						
						customerUsagePoolDTO.setQuantity(newPoolQuantity);
						EventManager.process(consumptionEvent);
					}
				}
				
				OrderDTO order = new OrderDAS().find(newQuantityEvent.getOrderId());
				BigDecimal orderLineUsagePoolQuantitySum = order.getFreeUsagePoolsTotalQuantity();
				LOG.debug("orderLineUsagePoolQuantitySum ::"+ orderLineUsagePoolQuantitySum); 
				order.setFreeUsageQuantity(orderLineUsagePoolQuantitySum);
			}
			
			for (Entry<Integer, BigDecimal> oldEntry : oldCustomerUsagePoolsMap.entrySet()) {
				Integer oldCustomerUsagePoolId = oldEntry.getKey();
				BigDecimal oldQuantity = oldEntry.getValue();
				BigDecimal newQuantity = newCustomerUsagePoolsMap.get(oldCustomerUsagePoolId);
				
				if (null == newQuantity && null != oldQuantity) {
					// order line has been removed, that's why there is no new quantity
					updateQuantity = oldQuantity.negate();
					LOG.debug("2order line has been removed updateQuantity: " + updateQuantity);
					CustomerUsagePoolDTO customerUsagePoolDTO = new CustomerUsagePoolDAS().findForUpdate(oldCustomerUsagePoolId);
					if (customerUsagePoolDTO.getQuantity().subtract(updateQuantity).compareTo(BigDecimal.ZERO) < 0) {
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDTO.getId(), 
										customerUsagePoolDTO.getQuantity(), 
										BigDecimal.ZERO,
										null);
						
						customerUsagePoolDTO.setQuantity(BigDecimal.ZERO);
						
						EventManager.process(consumptionEvent);
						
					} else {
						
						BigDecimal newPoolQuantity = customerUsagePoolDTO.getQuantity().subtract(updateQuantity);
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDTO.getId(), 
										customerUsagePoolDTO.getQuantity(), 
										newPoolQuantity,
										null);
						
						customerUsagePoolDTO.setQuantity(newPoolQuantity);
						
						EventManager.process(consumptionEvent);
					}
				}
			}
			
			if (null != oldLine && null == newLine && oldLine.hasOrderLineUsagePools()) {
				// line has been removed, so remove the order line customer usage pools association
				try {
					OrderLineDTO deletedLine = new OrderLineDAS().find(oldLine.getId());

                    if (deletedLine != null)
					deletedLine.getOrderLineUsagePools().clear();
				} catch (ObjectNotFoundException e) {} catch (Exception e) {
					LOG.debug("Error while fetching deleted Line :::::::::::::::::", e);
					throw new PluggableTaskException("Exception thrown from catch block");
				}
			}
			
		} else if (event instanceof OrderDeletedEvent) {
			OrderDeletedEvent orderDeletedEvent = (OrderDeletedEvent) event;
			OrderDTO deletedOrder = orderDeletedEvent.getOrder();
			if (deletedOrder.hasFreeUsagePools()) {
				CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
				for (Entry<Integer, BigDecimal> freeUsagePoolEntry : deletedOrder.getFreeUsagePoolsMap().entrySet()) {
					CustomerUsagePoolDTO customerUsagePoolDto = das.findForUpdate(freeUsagePoolEntry.getKey());
					QuantitySupplier quantitySupplier = new QuantitySupplier(customerUsagePoolDto.getUsagePool(), customerUsagePoolDto.getCustomer().getNextInvoiceDate(), null);
					// release the quantity back to the usage pool, so add the quantity back.

					BigDecimal customerPoolUpdateQuantity = customerUsagePoolDto.getQuantity().add(freeUsagePoolEntry.getValue());
					BigDecimal newPoolQuantity;

					if (customerPoolUpdateQuantity.compareTo(newPoolQuantity = quantitySupplier.get()) > 0) {
						
						CustomerUsagePoolConsumptionEvent consumptionEvent =
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDto.getId(), 
										customerUsagePoolDto.getQuantity(), 
										newPoolQuantity,
										null);
						
						// customer usage pool quantity cannot be higher than the system defined usage pool quantity
						// so set it to system defined usage pool quantity
						customerUsagePoolDto.setQuantity(newPoolQuantity);
						
						EventManager.process(consumptionEvent);
						
					} else {
						
						CustomerUsagePoolConsumptionEvent consumptionEvent = 
								new CustomerUsagePoolConsumptionEvent(
										entityId, 
										customerUsagePoolDto.getId(), 
										customerUsagePoolDto.getQuantity(), 
										customerPoolUpdateQuantity,
										null);
						
						customerUsagePoolDto.setQuantity(customerPoolUpdateQuantity);
						
						EventManager.process(consumptionEvent);
					}
				}
				
				OrderDTO order = new OrderDAS().find(deletedOrder.getId());
				for (OrderLineDTO line : order.getLines()) {
					line.getOrderLineUsagePools().clear();
				}
				
				deletedOrder.setFreeUsageQuantity(BigDecimal.ZERO);
			}
		}
	
		LOG.debug("Customer Usage Pool Update task completed");
		
	}
}
