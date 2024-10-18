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

package com.sapienter.jbilling.server.usagePool.task;

import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionActionDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionLogDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionLogDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerUsagePoolConsumptionEvent;
import com.sapienter.jbilling.server.usagePool.event.FreeTrialConsumptionEvent;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionFeeChargingEvent;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionNotificationEvent;

/**
 * CustomerUsagePoolConsumptionActionTask
 * This is an event handler that listens to CustomerUsagePoolConsumptionEvent.
 * When this event occurs, it calculates the consumption % for Customer's FUP 
 * and fires appropriate action as defined on the FUP. 
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

public class CustomerUsagePoolConsumptionActionTask extends PluggableTask 
implements IInternalEventsTask {

	private static final Logger LOG = Logger.getLogger(CustomerUsagePoolConsumptionActionTask.class);
	
	@SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] { 
		CustomerUsagePoolConsumptionEvent.class
    };

	public Class<Event>[] getSubscribedEvents () {
		return events;
	}
	
	public void process(Event event) throws PluggableTaskException {
		LOG.debug("Entering Customer Usage Pool Consumption - event: " + event);
		if (event instanceof CustomerUsagePoolConsumptionEvent) {
			
			Integer customerUsagePoolId = null;
			CustomerUsagePoolConsumptionEvent consumptionEvent = (CustomerUsagePoolConsumptionEvent) event;
			Integer entityId = consumptionEvent.getEntityId();
			customerUsagePoolId = consumptionEvent.getCustomerUsagePoolId();
			BigDecimal oldQuantity = consumptionEvent.getOldQuantity();
			BigDecimal newQuantity = consumptionEvent.getNewQuantity();
			
			if (null != customerUsagePoolId) {
				CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
				CustomerUsagePoolDTO customerUsagePool = das.findCustomerUsagePoolsById(customerUsagePoolId);
                
                // Calculate Total percentage including current usage quantity.
                if (null != customerUsagePool && null != customerUsagePool.getInitialQuantity() && customerUsagePool.getInitialQuantity().compareTo(BigDecimal.ZERO) > 0) {
                	
                	Integer userId = customerUsagePool.getCustomer().getBaseUser().getId();
                	 UsagePoolDTO usagePool = customerUsagePool.getUsagePool();
                     
                	 BigDecimal consumptionPercentage = ((customerUsagePool.getInitialQuantity().subtract(customerUsagePool.getQuantity()))
							.divide(customerUsagePool.getInitialQuantity(), MathContext.DECIMAL128))
							.multiply(new BigDecimal(100), MathContext.DECIMAL128);

	                // Calculate Total Consumption percentage excluding current usage quantity.
	                BigDecimal oldConsumptionPercentage = ((customerUsagePool.getInitialQuantity().subtract(oldQuantity))
                            .divide(customerUsagePool.getInitialQuantity(), MathContext.DECIMAL128))
                            .multiply(new BigDecimal(100), MathContext.DECIMAL128);


	                for (UsagePoolConsumptionActionDTO consumptionActionDTO: usagePool.getConsumptionActions()) {
	                    String actionType = consumptionActionDTO.getType();
	                    BigDecimal percentage = new BigDecimal(consumptionActionDTO.getPercentage());
	                    LOG.debug("Percent :: "+ consumptionActionDTO.getPercentage() + " Type :: " + actionType +
	                            " Id :: " + consumptionActionDTO.getId());
	                    if(consumptionPercentage.compareTo(new BigDecimal("100")) >= 0) {
	                        LOG.debug("firing free trial consumption action event!");
	                        EventManager.process(new FreeTrialConsumptionEvent(entityId, userId));
                        }

	                    if (percentage.compareTo(oldConsumptionPercentage) > 0 && percentage.compareTo(consumptionPercentage) <= 0) {
	                        fireUsagePoolConsumptionActionEvent(consumptionActionDTO, entityId, userId, customerUsagePool.getId());
	                        saveUsagePoolConsumptionLog(oldQuantity, newQuantity,
	                                customerUsagePool, "" + consumptionActionDTO.getId(), consumptionPercentage,
	                                consumptionActionDTO.getId());
	                    }
	                }
                }
			}
		}
		
		LOG.debug("Customer Usage Pool Consumption task completed");
	}

    private boolean needToBeLaunched(BigDecimal consumptionPercentage, Integer customerUsagePoolId,
                                     UsagePoolConsumptionActionDTO consumptionActionDTO) {
        BigDecimal percentage = new BigDecimal(consumptionActionDTO.getPercentage());
        UsagePoolConsumptionLogDAS consumptionLogDas = new UsagePoolConsumptionLogDAS();
        BigDecimal percentageConsumptionLog = consumptionLogDas.findPercentageConsumptionBycustomerUsagePoolId(customerUsagePoolId, consumptionActionDTO.getId());
        LOG.debug("percentageConsumptionLog ::::::::: "+percentageConsumptionLog);
        return  null != percentageConsumptionLog &&
                percentage.compareTo(percentageConsumptionLog) > 0 &&
                percentage.compareTo(consumptionPercentage) <= 0;
    }

    private void fireUsagePoolConsumptionActionEvent(UsagePoolConsumptionActionDTO action, Integer entityId,
										Integer userId, Integer customerUsagePoolId) {
		if (action.getNotificationId() != null) {
			EventManager.process(new UsagePoolConsumptionNotificationEvent(entityId, customerUsagePoolId, action));
		} else if (action.getNotificationId() == null) {
			EventManager.process(new UsagePoolConsumptionFeeChargingEvent(entityId, userId, action));
		} else {
            LOG.error("The action with id: " + action.getId() + " don't have any product Id or notification Id setted");
        }
	}

	private void saveUsagePoolConsumptionLog(BigDecimal oldQuantity,
                                             BigDecimal newQuantity,
                                             CustomerUsagePoolDTO customerUsagePool,
                                             String actionExecuted, BigDecimal consumptionPercentage, Integer id) {
		UsagePoolConsumptionLogDTO consumptionLog = new UsagePoolConsumptionLogDTO();
		
		consumptionLog.setOldQuantity(oldQuantity);
		consumptionLog.setNewQuantity(newQuantity);
		consumptionLog.setCustomerUsagePool(customerUsagePool);
		consumptionLog.setConsumptionDate(TimezoneHelper.serverCurrentDate());
		consumptionLog.setActionExecuted(actionExecuted);
		consumptionLog.setPercentageConsumption(consumptionPercentage);

		try {
			new UsagePoolConsumptionLogDAS().save(consumptionLog);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
