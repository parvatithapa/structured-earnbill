package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.event.AssetAddedToOrderEvent;
import com.sapienter.jbilling.server.item.event.AssetCreatedEvent;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;
import com.sapienter.jbilling.server.provisioning.db.*;
import com.sapienter.jbilling.server.provisioning.event.OrderChangeStatusTransitionEvent;
import com.sapienter.jbilling.server.provisioning.event.SubscriptionActiveEvent;
import com.sapienter.jbilling.server.provisioning.event.SubscriptionInactiveEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract provisioning plug-in that provides convenience methods and provides processing
 * of internal events to extract relevant provisioning data.
 *
 * @author Brian Cowdery
 * @since 06-Jul-2012
 */
public abstract class AbstractProvisioningTask extends PluggableTask implements IInternalEventsTask {
    public static final String ACTIVATED_EVENT_TYPE = "activated";
    public static final String DEACTIVATED_EVENT_TYPE = "deactivated";
    public static final String ADD_EVENT_TYPE = "add";
    public static final String CREATE_EVENT_TYPE = "create";
    public static final String UPDATE_EVENT_TYPE = "update";
    public static final String PROVISIONING_EVENT_TYPE = "provisioning";
    public static final String PAYMENT_PROVISION_EVENT_TYPE = "payment";
    private static final Logger LOG = Logger.getLogger(AbstractProvisioningTask.class);
    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        SubscriptionActiveEvent.class,
        SubscriptionInactiveEvent.class,
        NewQuantityEvent.class,
        AssetCreatedEvent.class,
        AssetUpdatedEvent.class,
        AssetAddedToOrderEvent.class,
        OrderChangeStatusTransitionEvent.class,
        PaymentSuccessfulEvent.class
    };

    /**
     * Convenience method to find a pricing field by name.
     *
     * @param fields pricing fields
     * @param fieldName name
     * @return found pricing field or null if no field found.
     */
    public static PricingField find(List<PricingField> fields, String fieldName) {
        if (fields != null) {
            for (PricingField field : fields) {
                if (field.getName().equals(fieldName))
                    return field;
            }
        }
        return null;
    }

    /**
     * Convenience method to find a specific order line by item ID.
     *
     * @param lines order lines
     * @param itemId item id
     * @return order line
     */
    public static OrderLineDTO findLine(List<OrderLineDTO> lines, Integer itemId) {
        if (lines != null) {
            for (OrderLineDTO line : lines) {
                if (null != line && line.getItemId()!=null && line.getItemId().equals(itemId)) {
                    return line;
                }
            }
        }
        return null;
    }

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        if (event instanceof SubscriptionActiveEvent) {
            SubscriptionActiveEvent activeEvent = (SubscriptionActiveEvent) event;

            LOG.debug("Processing order " + activeEvent.getOrder().getId() + " subscription activation");
            doActivate(activeEvent.getOrder(), activeEvent.getOrder().getLines());

        } else if (event instanceof SubscriptionInactiveEvent) {
            SubscriptionInactiveEvent inactiveEvent = (SubscriptionInactiveEvent) event;

            LOG.debug("Processing order " + inactiveEvent.getOrder().getId() + " subscription deactivation");
            doDeactivate(inactiveEvent.getOrder(), inactiveEvent.getOrder().getLines());

        } else if (event instanceof NewQuantityEvent) {
            NewQuantityEvent quantityEvent = (NewQuantityEvent) event;

            if (BigDecimal.ZERO.compareTo(quantityEvent.getOldQuantity()) != 0
                    && BigDecimal.ZERO.compareTo(quantityEvent.getNewQuantity()) != 0) {
                LOG.debug("Order line quantities did not change, no provisioning necessary.");
                return;
            }

            OrderDTO order = new OrderBL(quantityEvent.getOrderId()).getDTO();
            if (!isOrderProvisionable(order)) {
                LOG.warn("Order is not active and cannot be provisioned.");
                return;
            }

            if (BigDecimal.ZERO.compareTo(quantityEvent.getOldQuantity()) == 0) {
                LOG.debug("Processing order " + order.getId() + " activation");
                doActivate(order, Arrays.asList(quantityEvent.getOrderLine()));
            }

            if (BigDecimal.ZERO.compareTo(quantityEvent.getNewQuantity()) == 0) {
                LOG.debug("Processing order " + order.getId() + " deactivation");
                doDeactivate(order, Arrays.asList(quantityEvent.getOrderLine()));
            }

        } else if (event instanceof AssetCreatedEvent) {
            AssetCreatedEvent assetCreatedEvent = (AssetCreatedEvent)event;

            LOG.debug("Processing created asset " + assetCreatedEvent.getAsset().getId());
            doCreate(assetCreatedEvent.getAsset());

        } else if (event instanceof AssetUpdatedEvent) {
            AssetUpdatedEvent assetUpdatedEvent = (AssetUpdatedEvent)event;
            LOG.debug("Processing update asset " + assetUpdatedEvent.getAsset().getId());
            doUpdate(assetUpdatedEvent.getAsset());

        } else if (event instanceof AssetAddedToOrderEvent) {
            AssetAddedToOrderEvent assetAddedToOrderEvent = (AssetAddedToOrderEvent)event;

            LOG.debug("Processing adding asset " + assetAddedToOrderEvent.getAsset().getId() + " to order line " + assetAddedToOrderEvent.getAsset().getOrderLine().getId());
            doAdd(assetAddedToOrderEvent.getAsset());

        } else if (event instanceof OrderChangeStatusTransitionEvent) {
            OrderChangeStatusTransitionEvent orderChangeStatusTransitionEvent = (OrderChangeStatusTransitionEvent)event;

            LOG.debug("Processing order status change " + orderChangeStatusTransitionEvent.getOrderChange().getId());
            doProvisioning(orderChangeStatusTransitionEvent.getOrderChange());

        } else if (event instanceof PaymentSuccessfulEvent) {
            PaymentSuccessfulEvent paymentSuccessfulEvent = (PaymentSuccessfulEvent)event;

            LOG.debug("Processing order status change " + paymentSuccessfulEvent.getPayment().getId());
            doPaymentProvisioning(paymentSuccessfulEvent.getPayment());

        } else {
            throw new PluggableTaskException("Cannot process event " + event);
        }
    }
    
    private boolean isOrderProvisionable(OrderDTO order) {
        if (order != null) {

            Date today = companyCurrentDate();

            if (order.getOrderStatus() != null && order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.INVOICE)) {
                if (order.getActiveSince() != null
                        && order.getActiveSince().before(today)
                        && order.getActiveUntil() != null
                        && order.getActiveUntil().after(today)) {

                    return true;
                }
            }
        }

        return false;
    }

    private void sendCommandQueue(CommandManager c, String eventType) throws PluggableTaskException {
        LOG.debug("Publishing command queue to JMS");

        try {
            CommandsQueueSender cmdSender = new CommandsQueueSender();
            cmdSender.postProvisioningCommand(c.getCommands(), eventType);
        } catch (JMSException e) {
            throw new PluggableTaskException(e);
        }
    }

    protected void doActivate(OrderDTO order, List<OrderLineDTO> lines) throws PluggableTaskException {
        if (isActionProvisionable(order)) {
            CommandManager manager = new CommandManager(order);
            activate(order, lines, manager);
            sendCommandQueue(manager, ACTIVATED_EVENT_TYPE);
        }
    }

    protected void doDeactivate(OrderDTO order, List<OrderLineDTO> lines) throws PluggableTaskException {
        if (isActionProvisionable(order)) {
            CommandManager manager = new CommandManager(order);
            deactivate(order, lines, manager);
            sendCommandQueue(manager, DEACTIVATED_EVENT_TYPE);
        }
    }

    protected void doAdd(AssetDTO asset) throws PluggableTaskException {
        if (isActionProvisionable(asset)) {
            CommandManager manager = new CommandManager(asset);
            add(asset, manager);
            sendCommandQueue(manager, ADD_EVENT_TYPE);
        }
    }

    protected void doCreate(AssetDTO asset) throws PluggableTaskException {
        if (isActionProvisionable(asset)) {
            CommandManager manager = new CommandManager(asset);
            create(asset, manager);
            sendCommandQueue(manager, CREATE_EVENT_TYPE);
        }
    }

    protected void doUpdate(AssetDTO asset) throws PluggableTaskException {
        if (isActionProvisionable(asset)) {
            CommandManager manager = new CommandManager(asset);
            update(asset, manager);
            sendCommandQueue(manager, UPDATE_EVENT_TYPE);
        }
    }

    protected void doProvisioning(OrderChangeDTO orderChangeDTO) throws PluggableTaskException {
        if (isActionProvisionable(orderChangeDTO)) {
            CommandManager manager = new CommandManager(orderChangeDTO);
            provisioning(orderChangeDTO, manager);
            sendCommandQueue(manager, PROVISIONING_EVENT_TYPE);
        }
    }

    protected void doPaymentProvisioning(PaymentDTOEx payment) throws PluggableTaskException {
        if (isActionProvisionable(payment)) {
            CommandManager manager = new CommandManager(payment);
            paymentProvisioning(payment, manager);
            sendCommandQueue(manager, PAYMENT_PROVISION_EVENT_TYPE);
        }
    }

    /*
        methods to be implemented to do the actual provisioning work.
     */
    void activate(OrderDTO order, List<OrderLineDTO> lines, CommandManager c) {}

    void deactivate(OrderDTO order, List<OrderLineDTO> lines, CommandManager c) {}

    // Asset related provisioning methods
    void add(AssetDTO asset, CommandManager c) {}

    void create(AssetDTO asset, CommandManager c) {}

    void update(AssetDTO asset, CommandManager c) {}

    // order line/change provisioning
    void provisioning(OrderChangeDTO orderChange, CommandManager c) {}

    // payment proviioning
    void paymentProvisioning(PaymentDTOEx payment, CommandManager c) {}

    abstract boolean isActionProvisionable(IProvisionable provisionable);

    /**
     * Helper that holds the queue of commands to send via JMS to the provisioning message bean.
     *
     * @author othman
     */
    protected static class CommandManager {
        private static final long serialVersionUID = 1L;

        private OrderDTO order = null;

        private LinkedList<ProvisioningCommandDTO> commands = new LinkedList<ProvisioningCommandDTO>();
        private ProvisioningCommandDTO provisioningCommand;
        private ProvisioningCommandDAS provisioningCommandDAS = new ProvisioningCommandDAS();
        private IProvisionable entity;
        private int executionOrder = 1;

        public CommandManager(IProvisionable entity) {
            this.entity = entity;
        }

        public void addCommand(String command) {
            if (provisioningCommand != null){
                commands.add(provisioningCommand);
            }

            if (entity instanceof AssetDTO)
                provisioningCommand = new AssetProvisioningCommandDTO((AssetDTO) entity);
            else if (entity instanceof OrderDTO)
                provisioningCommand = new OrderProvisioningCommandDTO((OrderDTO) entity);
            else if (entity instanceof OrderChangeDTO) {
                provisioningCommand = new OrderLineProvisioningCommandDTO((OrderChangeDTO) entity);
            } else if (entity instanceof PaymentDTO)
                provisioningCommand = new PaymentProvisioningCommandDTO((PaymentDTO) entity);
              else {
                throw new SessionInternalError("Command is not supported for entity" + entity);
            }

            provisioningCommand.setName(command);
            provisioningCommand.setExecutionOrder(executionOrder++);
            provisioningCommand.setCreateDate(TimezoneHelper.serverCurrentDate());
            provisioningCommand.setLastUpdateDate(TimezoneHelper.serverCurrentDate());
            provisioningCommand.setCommandStatus(ProvisioningCommandStatus.IN_PROCESS);
        }

        public void addParameter(String name, String value) {
            provisioningCommand.getCommandParameters().put(name, value);
        }

        public OrderDTO getOrder() {
            return order;
        }

        public void setOrder(OrderDTO order) {
            this.order = order;
        }

        public LinkedList<ProvisioningCommandDTO> getCommands() {
            if (provisioningCommand != null){
                commands.add(provisioningCommand);
            }

            LinkedList<ProvisioningCommandDTO> resultCommands = new LinkedList<ProvisioningCommandDTO>();
            for (ProvisioningCommandDTO command :commands) {
                resultCommands.add(provisioningCommandDAS.save(command));
                provisioningCommandDAS.flush();
            }

            return resultCommands;
        }
    }
}
