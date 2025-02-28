/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.provisioning.event.SubscriptionActiveEvent;
import com.sapienter.jbilling.server.provisioning.event.SubscriptionInactiveEvent;
import com.sapienter.jbilling.server.rule.RulesBaseTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;

import javax.jms.JMSException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author othman
 * 
 */
@Deprecated
public class ProvisioningCommandsRulesTask extends RulesBaseTask implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ProvisioningCommandsRulesTask.class));
    protected FormatLogger getLog() { return LOG; }

    public static final String ACTIVATED_EVENT_TYPE = "activated";
    public static final String DEACTIVATED_EVENT_TYPE = "deactivated";

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            SubscriptionActiveEvent.class,
            SubscriptionInactiveEvent.class,
            NewQuantityEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sapienter.jbilling.server.system.event.task.IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
     */
    public void process(Event event) throws PluggableTaskException {
        try {

            // the order and the order lines have to be visible to the rules
            KnowledgeBase knowledgeBase;
            try {
                knowledgeBase = readKnowledgeBase();
            } catch (Exception e) {
                throw new TaskException(e);
            }

            session = knowledgeBase.newStatefulKnowledgeSession();

            if (event instanceof SubscriptionActiveEvent) {
                SubscriptionActiveEvent active = (SubscriptionActiveEvent) event;

                LOG.debug("The order " + active.getOrder().getId()
                        + " is active ");
                processRules(active.getOrder(), ACTIVATED_EVENT_TYPE);
            } else if (event instanceof SubscriptionInactiveEvent) {
                SubscriptionInactiveEvent inactive = (SubscriptionInactiveEvent) event;

                LOG.debug("The order " + inactive.getOrder().getId()
                        + " is inactive");
                processRules(inactive.getOrder(), DEACTIVATED_EVENT_TYPE);
            } else if (event instanceof NewQuantityEvent) {
                NewQuantityEvent newQuantity = (NewQuantityEvent) event;

                if (BigDecimal.ZERO.compareTo(newQuantity.getOldQuantity()) != 0
                        && BigDecimal.ZERO.compareTo(newQuantity.getNewQuantity()) != 0) {
                    return;
                }

                Integer orderId = newQuantity.getOrderId();
                OrderDAS orderDb = new OrderDAS();
                OrderDTO order = orderDb.find(orderId);

                if ((order == null) || !checkOrder(order)) {
                    return;
                }

                String typeEvent = "";

                if (BigDecimal.ZERO.compareTo(newQuantity.getOldQuantity()) == 0) {
                    typeEvent = ACTIVATED_EVENT_TYPE;
                }

                if (BigDecimal.ZERO.compareTo(newQuantity.getNewQuantity()) == 0) {
                    typeEvent = DEACTIVATED_EVENT_TYPE;
                }

                LOG.debug("NewQuantityEvent order line "
                        + newQuantity.getOrderLine().getId() + " is "
                        + typeEvent);
                processRules(order, newQuantity.getOrderLine(), typeEvent);
            } else {
                throw new PluggableTaskException("Cant not process event "
                        + event);
            }
        } catch (TaskException e) {
            throw new PluggableTaskException(e);
        }
    }

    /**
     * @param newOrder
     * @param eventType
     * @throws TaskException
     */
    protected void processRules(OrderDTO newOrder, String eventType)
            throws TaskException {
        List<Object> rulesMemoryContext = new ArrayList<Object>();
        CommandManager commandManager = new CommandManager(eventType, newOrder);

        // Add order lines
        for (OrderLineDTO line : newOrder.getLines()) {
            rulesMemoryContext.add(line);
        }

        // add helper command order
        rulesMemoryContext.add(commandManager);

        // then execute the rules
        for (Object o : rulesMemoryContext) {
            LOG.debug("in memory context=" + o);
        }

        // then execute the rules
        LOG.debug("execute provisioning Rules.");
        executeStatefulRules(session, rulesMemoryContext);
        LOG.debug("execute provisioning Rules Done.");

        // send commands queue through JMS
        sendCommandQueue(eventType, commandManager, newOrder);
    }

    /**
     * @param line
     * @param eventType
     * @throws TaskException
     */
    protected void processRules(OrderDTO newOrder, OrderLineDTO line,
            String eventType) throws TaskException {
        List<Object> rulesMemoryContext = new ArrayList<Object>();
        CommandManager commandManager = new CommandManager(eventType, newOrder);

        rulesMemoryContext.add(line);

        // add helper command order
        rulesMemoryContext.add(commandManager);

        // then execute the rules
        for (Object o : rulesMemoryContext) {
            LOG.debug("in memory context=" + o);
        }

        // then execute the rules
        executeStatefulRules(session, rulesMemoryContext);

        // send commands queue through JMS
        sendCommandQueue(eventType, commandManager, newOrder);
    }

    /**
     * sends commands queue through JMS
     * 
     * @param eventType
     * @throws TaskException
     */
    private void sendCommandQueue(String eventType, CommandManager c,
            OrderDTO order) throws TaskException {
        LOG.debug("calling sendCommandQueue()");

        try {
            CommandsQueueSender cmdSender = new CommandsQueueSender(order);

            cmdSender.postCommandsQueue(c.getCommands(), eventType);
        } catch (JMSException e) {
            throw new TaskException(e);
        }
    }

    /**
     * @param order
     * @return
     */
    private boolean checkOrder(OrderDTO order) {
        boolean valid = false;
        Date today = companyCurrentDate();

        if ((order.getOrderStatus() != null)
                && (order.getOrderStatus().getOrderStatusFlag() == OrderStatusFlag.INVOICE)) {
            if (((order.getActiveSince() != null) && order.getActiveSince()
                    .before(today))
                    && ((order.getActiveUntil() != null) && order
                            .getActiveUntil().after(today))) {
                valid = true;
            }
        }

        LOG.debug(" checkOrder(): is order valid ? :" + valid);

        return valid;
    }

    /**
     * Helper class
     * 
     * @author othman
     * 
     */
    public class CommandManager {
        private static final String COMMAND_NAME = "command";
        private static final String ORDER_LINE_ID = "order_line_id";

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private OrderDTO order = null;
        private LinkedList<LinkedList<StringPair>> commands = new LinkedList<LinkedList<StringPair>>();;
        private LinkedList<StringPair> commandQueue;
        private String eventType;

        /**
         * @param eventType
         * @param order
         */
        public CommandManager(String eventType, OrderDTO order) {
            this.eventType = eventType;
            this.order = order;
        }

        public void addCommand(String command, Integer orderLineId) {
            LOG.debug("calling CommandManager.addCommand()");
            LOG.debug("command=" + command);
            LOG.debug("orderLineId=" + orderLineId.intValue());

            // add current command queue to global queue
            if (commandQueue != null) {
                commands.add(commandQueue);
                LOG.debug("added command queue " + commandQueue);
            }

            // create new queue for "command"
            commandQueue = new LinkedList<StringPair>();

            StringPair param = new StringPair(COMMAND_NAME, command);

            commandQueue.add(param);
            param = new StringPair(ORDER_LINE_ID, orderLineId.toString());
            commandQueue.add(param);
            LOG.debug("added command : " + command);
        }

        public void addParameter(String name, String value) {
            StringPair param = new StringPair(name, value);

            commandQueue.add(param);
            LOG.debug("added command parameter: " + param);
        }

        /**
         * @return the order
         */
        public OrderDTO getOrder() {
            return order;
        }

        /**
         * @param order
         *            the order to set
         */
        public void setOrder(OrderDTO order) {
            this.order = order;
        }

        /**
         * @return the eventType
         */
        public String getEventType() {
            return eventType;
        }

        /**
         * @return the commands
         */
        public LinkedList<LinkedList<StringPair>> getCommands() {

            // parameters command queue should be added
            if (commandQueue != null) {
                commands.add(commandQueue);
                LOG.debug("added command queue " + commandQueue);
            }

            return commands;
        }
    }
}
