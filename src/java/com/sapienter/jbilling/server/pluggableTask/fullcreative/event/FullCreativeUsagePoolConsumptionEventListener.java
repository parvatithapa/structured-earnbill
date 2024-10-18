package com.sapienter.jbilling.server.pluggableTask.fullcreative.event;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerUsagePoolConsumptionEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.DTOFactory;

/***
 * 	The listener triggers when usage pool consumption exceeds configured percentage,
 *  it should trigger remote server call to full creative.
 *	@author Neelmani Gautam - Fullcreative
 * 	@since 23-Sept-2016
 */

public class FullCreativeUsagePoolConsumptionEventListener extends PluggableTask
implements IInternalEventsTask{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription PARAMETER_WS_URL =
            new ParameterDescription("Web Service Url", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_CONSUMPTION_PERCENTAGE =
            new ParameterDescription("Consumption %", true, ParameterDescription.Type.INT);

    // initializer for pluggable parameters
    {
        descriptions.add(PARAMETER_WS_URL);
        descriptions.add(PARAMETER_CONSUMPTION_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        CustomerUsagePoolConsumptionEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        logger.debug("Entering Customer Usage Pool Consumption - event: ", event);

        FCWebhookAPICallBuilder.validateURL(parameters.get(PARAMETER_WS_URL.getName()));

        if (event instanceof CustomerUsagePoolConsumptionEvent) {

            Integer customerUsagePoolId = null;
            CustomerUsagePoolConsumptionEvent consumptionEvent = (CustomerUsagePoolConsumptionEvent) event;
            customerUsagePoolId = consumptionEvent.getCustomerUsagePoolId();
            BigDecimal oldQuantity = consumptionEvent.getOldQuantity();
            if (null != customerUsagePoolId) {
                CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
                CustomerUsagePoolDTO customerUsagePool = das.findCustomerUsagePoolsById(customerUsagePoolId);

                // Calculate Total percentage including current usage quantity.
                if (null != customerUsagePool && null != customerUsagePool.getInitialQuantity() && customerUsagePool.getInitialQuantity().compareTo(BigDecimal.ZERO) > 0) {

                    BigDecimal consumptionPercentage = ((customerUsagePool.getInitialQuantity().subtract(customerUsagePool.getQuantity()))
                            .divide(customerUsagePool.getInitialQuantity(), MathContext.DECIMAL128))
                            .multiply(new BigDecimal(100), MathContext.DECIMAL128);

                    // Calculate Total Consumption percentage excluding current usage quantity.
                    BigDecimal oldConsumptionPercentage = ((customerUsagePool.getInitialQuantity().subtract(oldQuantity))
                            .divide(customerUsagePool.getInitialQuantity(), MathContext.DECIMAL128))
                            .multiply(new BigDecimal(100), MathContext.DECIMAL128);

                    logger.debug("Initial Quantity : {}", customerUsagePool.getInitialQuantity());
                    logger.debug("Old Consumption Percentage : {}, Old Consumption : {}", oldConsumptionPercentage, oldQuantity);
                    logger.debug("Current Consumption Percentage : {}, Current Consumption : {}", consumptionPercentage, customerUsagePool.getQuantity());

                    logger.debug("Percentage " + parameters.get(PARAMETER_CONSUMPTION_PERCENTAGE.getName()));
                    BigDecimal percentage = new BigDecimal(parameters.get(PARAMETER_CONSUMPTION_PERCENTAGE.getName()));                    

                    if (percentage.compareTo(oldConsumptionPercentage) > 0 && percentage.compareTo(consumptionPercentage) <= 0) {
                        fireUsagePoolConsumptionActionEvent(consumptionEvent, customerUsagePool);                        
                    }
                }
            }
        }
        logger.debug("Customer Usage Pool Consumption task completed");
    }

    private void fireUsagePoolConsumptionActionEvent(Event consumptionEvent, CustomerUsagePoolDTO customerUsagePool) {

        logger.debug("Making consumpiton exceeded WS call.5");

        try{
            logger.debug("Order Id : " + customerUsagePool.getOrder().getId());

            Map<String, Object> internalEvent = new HashMap<>();

            internalEvent.put("eventType", "usagepoolConsumptionEvent");
            internalEvent.put("event", consumptionEvent);
            internalEvent.put("user", UserBL.getWS(DTOFactory.getUserDTOEx(customerUsagePool.getCustomer().getBaseUser())));
            internalEvent.put("FUPQuantity", customerUsagePool.getInitialQuantity());
            internalEvent.put("monthlyOrderId", customerUsagePool.getOrder().getId());
            internalEvent.put("entityId", consumptionEvent.getEntityId());
            internalEvent.put("threshold_percentage", parameters.get(PARAMETER_CONSUMPTION_PERCENTAGE.getName()));

            Map<String, Object> response = FCWebhookAPICallBuilder.makeApiCall(
                    parameters.get(PARAMETER_WS_URL.getName()),
                            new ObjectMapper().writeValueAsString(internalEvent), 20);

            logger.debug("Is Call Successfull : {}", response.get("success"));

            if (!(Boolean)response.get("success")) {
                logger.debug("Message : {}", response.get("message"));
            }
        }catch(Exception e){
            logger.error("Couldn't make call to fullcreative event listener.", e);
        }
    }
}
