package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.server.order.OrderJsonBuilder;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.event.EditOrderEvent;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Created by Leandro Zoi 02/27/2018
 */
public class SendCommandToExternalQueueTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String QUEUE_ERROR = "%s_error";
    private static final String MESSAGE_ERROR = "Error creating/updating orderId: %d \n%s";

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            NewOrderEvent.class,
            EditOrderEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * @param event event to process
     * @throws PluggableTaskException
     * @see IInternalEventsTask#process(Event)
     */
    @Override
    public void process(Event event) {
        if (event instanceof NewOrderEvent || event instanceof EditOrderEvent) {
            OrderDTO order;
            logger.debug("Entering notify with message");
            if (event instanceof NewOrderEvent) {
                order = ((NewOrderEvent) event).getOrder();
            } else {
                order = ((EditOrderEvent) event).getOrder();
            }

            //If order is a credit order, not send it
            if (order.getTotal().compareTo(BigDecimal.ZERO) >= 0) {
                try {
                    RabbitTemplate rabbitTemplate = Context.getBean(Context.Name.AQM_TEMPLATE);
                    Queue queue = Context.getBean(Queue.class);

                    rabbitTemplate.setRoutingKey(queue.getName());
                    rabbitTemplate.setQueue(queue.getName());
                    rabbitTemplate.send(MessageBuilder.withBody(OrderJsonBuilder.build(order).getBytes())
                                                                                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                                                                                .build());
                } catch (Exception e) {
                    sendExceptionToQueueError(e, order.getId());
                }
            }
        }
    }

    private void sendExceptionToQueueError(Throwable t, Integer orderId) {
        RabbitTemplate rabbitTemplate = Context.getBean(Context.Name.AQM_TEMPLATE);
        Queue queue = Context.getBean(Queue.class);

        rabbitTemplate.setRoutingKey(String.format(QUEUE_ERROR, queue.getName()));
        rabbitTemplate.setQueue(String.format(QUEUE_ERROR, queue.getName()));
        rabbitTemplate.send(MessageBuilder.withBody(String.format(MESSAGE_ERROR, orderId,
                                                                  Arrays.stream(t.getStackTrace())
                                                                        .map(StackTraceElement::toString)
                                                                        .collect(Collectors.joining("\n")))
                                                          .getBytes())
                                          .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                                          .build());
    }
}
