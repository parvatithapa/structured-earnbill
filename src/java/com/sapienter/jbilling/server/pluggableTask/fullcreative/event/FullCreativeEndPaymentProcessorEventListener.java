package com.sapienter.jbilling.server.pluggableTask.fullcreative.event;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.server.payment.event.EndProcessPaymentEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

/***
 * This listener triggers when back-end payment process completes. Once back-end payemnt process completes, it will make
 * remote server call to full creative
 * 
 * @author Neelmani Gautam - Fullcreative
 * @since 23-Sept-2016
 */

public class FullCreativeEndPaymentProcessorEventListener extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription PARAMETER_WS_URL = new ParameterDescription("Web Service Url", true,
            ParameterDescription.Type.STR);

    {
        descriptions.add(PARAMETER_WS_URL);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] { EndProcessPaymentEvent.class };

    @Override
    public void process (Event event) throws PluggableTaskException {

        FCWebhookAPICallBuilder.validateURL(parameters.get(PARAMETER_WS_URL.getName()));

        EndProcessPaymentEvent endPaymentProcessEvent = (EndProcessPaymentEvent) event;

        logger.debug("Payment Processor id {} ended.", endPaymentProcessEvent.getRunId());
        logger.debug("For Entity ID : {}", endPaymentProcessEvent.getEntityId());

        try {
            Map<String, Object> internalEvent = new HashMap<>();

            internalEvent.put("paymentProcessorId", endPaymentProcessEvent.getRunId());
            internalEvent.put("entityId", endPaymentProcessEvent.getEntityId());

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

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }
}
