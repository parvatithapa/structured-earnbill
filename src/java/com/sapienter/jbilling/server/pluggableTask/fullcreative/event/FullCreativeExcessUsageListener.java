package com.sapienter.jbilling.server.pluggableTask.fullcreative.event;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.DTOFactory;

/***
 * FullcreativeExcessUsageListener
 * 
 * The listener triggers when usage pool consumption in amount reaches or exceeds the predefined amount, it should
 * trigger remote server call to full creative.
 * 
 * @author Neelmani Gautam - Fullcreative
 * @since 23-Sept-2016
 *
 */
public class FullCreativeExcessUsageListener extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription PARAMETER_WS_URL = new ParameterDescription("Web Service Url", true,
            ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_EXCESS_USAGE_AMOUNT = new ParameterDescription(
            "Excess Usage Amount", true, ParameterDescription.Type.FLOAT);

    // initializer for pluggable params
    {
        descriptions.add(PARAMETER_WS_URL);
        descriptions.add(PARAMETER_EXCESS_USAGE_AMOUNT);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] { NewQuantityEvent.class };

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    @Override
    public void process (Event event) throws PluggableTaskException {

        FCWebhookAPICallBuilder.validateURL(parameters.get(PARAMETER_WS_URL.getName()));

        logger.debug("Entering Customer Usage Pool Consumption - event: {}", event);

        if (event instanceof NewQuantityEvent) {
            NewQuantityEvent newQuantityEvent = (NewQuantityEvent) event;

            logger.info("New Quantity : Order Id : {}", newQuantityEvent.getOrderId());

            OrderDAS das = new OrderDAS();
            OrderDTO order = das.findNow(newQuantityEvent.getOrderId());

            logger.debug("Get Total Amount {}", order.getTotal());

            BigDecimal excessUsageLimit = new BigDecimal(parameters.get(PARAMETER_EXCESS_USAGE_AMOUNT.getName()));

            logger.debug("excessUsageLimit {}", excessUsageLimit);
            logger.debug("Comparision : {}", (order.getTotal().compareTo(excessUsageLimit) >= 0));

            if (order.getTotal().compareTo(excessUsageLimit) >= 0) {
                UserWS user = UserBL.getWS(DTOFactory.getUserDTOEx(order.getBaseUserByUserId()));

                logger.debug("Usser Balance : {}", user.getDynamicBalance());

                fireExcessUsageEvent(newQuantityEvent, order, user);
            }
        }
    }

    private void fireExcessUsageEvent (NewQuantityEvent newQuantityEvent, OrderDTO order, UserWS user) {

        logger.debug("Making consumpiton exceeded WS call.");

        try {
            Map<String, Object> internalEvent = new HashMap<>();

            internalEvent.put("eventType", "excessUsageEvent");
            internalEvent.put("entityId", newQuantityEvent.getEntityId());
            internalEvent.put("user", UserBL.getWS(DTOFactory.getUserDTOEx(user.getId())));
            internalEvent.put("onetimeOrderId", order.getId());
            internalEvent.put("onetimeOrderAmount", order.getTotal());
            internalEvent.put("threshold_excessusage_amount", parameters.get(PARAMETER_EXCESS_USAGE_AMOUNT.getName()));

            Map<String, Object> response = FCWebhookAPICallBuilder.makeApiCall(
                    parameters.get(PARAMETER_WS_URL.getName()), new ObjectMapper().writeValueAsString(internalEvent),
                    20);

            logger.debug("Is Call Successfull : {}", response.get("success"));

            if (!(Boolean) response.get("success")) {
                logger.debug("Message : {}", response.get("message"));
            }
        } catch (Exception e) {
            logger.error("Couldn't make call to fullcreative event listener.", e);
        }
    }
}
