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
import java.util.Date;

import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;

import org.apache.commons.lang.StringUtils;
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
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final String USAGE_POOL_NAME_LABLE = "name";
	
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
							String currentDataBoost = null;
							String currentUsagePoolName = customerUsagePool.getUsagePool()
									.getDescription(customerUsagePool.getCustomer().getBaseUser().getLanguage().getId(), USAGE_POOL_NAME_LABLE);
							if (percentage.compareTo(ONE_HUNDRED) == 0) {
								if (currentUsagePoolName.contains(SPCConstants.DATA_BOOST_NAME)) {
									currentDataBoost = SPCConstants.DATA_BOOST_TOKEN +
											StringUtils.substringAfter(currentUsagePoolName, SPCConstants.DATA_BOOST_NAME);
								}
							}
							fireUsagePoolConsumptionActionEvent(consumptionActionDTO, entityId, userId, customerUsagePool.getId(),
							        currentDataBoost, consumptionEvent.getActiveSince());
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

    private void fireUsagePoolConsumptionActionEvent(UsagePoolConsumptionActionDTO action, Integer entityId,
										Integer userId, Integer customerUsagePoolId,String currentDataBoost, Date activeSince) {
		if (action.getNotificationId() != null) {
			EventManager.process(new UsagePoolConsumptionNotificationEvent(entityId, customerUsagePoolId, action,currentDataBoost));
		} else if (action.getNotificationId() == null) {
			EventManager.process(new UsagePoolConsumptionFeeChargingEvent(entityId, customerUsagePoolId, action, activeSince));
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
			LOG.error("Error while saving the consumption log {}", e);
		}
	}
}
