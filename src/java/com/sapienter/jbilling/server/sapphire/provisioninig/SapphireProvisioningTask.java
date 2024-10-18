package com.sapienter.jbilling.server.sapphire.provisioninig;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.INT;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.STR;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sapienter.jbilling.server.customer.event.UpdateCustomerEvent;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.event.AssetMetaFieldUpdatedEvent;
import com.sapienter.jbilling.server.item.event.SwapAssetsEvent;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.OrderMetaFieldUpdateEvent;
import com.sapienter.jbilling.server.order.event.UpgradeOrderEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanEvent;
import com.sapienter.jbilling.server.sapphire.NewSaleEvent;
import com.sapienter.jbilling.server.sapphire.NewSaleRequestWS;
import com.sapienter.jbilling.server.sapphire.SapphireSwapAssetEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;

public class SapphireProvisioningTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final ParameterDescription PARAM_API_URL = new ParameterDescription("apiUrl", true, STR);
    static final ParameterDescription PARAM_USER_NAME = new ParameterDescription("userName", true, STR);
    static final ParameterDescription PARAM_PASSWORD = new ParameterDescription("password", true, STR);
    static final ParameterDescription PARAM_TIME_OUT = new ParameterDescription("timeOut", false, INT);
    static final ParameterDescription PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME =
            new ParameterDescription("Customer Provisioning Metafield Name", true, ParameterDescription.Type.STR);
    static final ParameterDescription PARAM_ORDER_PROVISIONING_STATUS_MF_NAME =
            new ParameterDescription("Order Provisioning Metafield Name", true, ParameterDescription.Type.STR);
    static final ParameterDescription PARAM_SUSPENDED_AGEING_STEP_ID =
            new ParameterDescription("Suspended Collection Step Id", true, ParameterDescription.Type.INT);
    static final ParameterDescription PARAM_DISCONNECTED_AGEING_STEP_ID =
            new ParameterDescription("Disconnected Collection Step Id", true, ParameterDescription.Type.INT);
    static final ParameterDescription PARAM_TERMINATED_AGEING_STEP_ID =
            new ParameterDescription("Terminated Collection Step Id", true, ParameterDescription.Type.INT);
    static final ParameterDescription PARAM_DISCONNECTION_FEE_PRODUCT_ID =
            new ParameterDescription("Disconnection Fee Product Id", true, ParameterDescription.Type.INT);
    static final ParameterDescription PARAM_AIT_GROUP_NAME =
            new ParameterDescription("AIT group name", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_EXCLUDE_FROM_PROVISIONING =
            new ParameterDescription("Exclude from Provisioning Metafield Name", false, ParameterDescription.Type.STR);
    static final ParameterDescription PARAM_SERVICE_ORDER_ID_MF_NAME =
            new ParameterDescription("Service Order Id Meta Field Name", false, ParameterDescription.Type.STR);

    public SapphireProvisioningTask() {
        descriptions.add(PARAM_API_URL);
        descriptions.add(PARAM_USER_NAME);
        descriptions.add(PARAM_PASSWORD);
        descriptions.add(PARAM_TIME_OUT);
        descriptions.add(PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME);
        descriptions.add(PARAM_ORDER_PROVISIONING_STATUS_MF_NAME);
        descriptions.add(PARAM_SUSPENDED_AGEING_STEP_ID);
        descriptions.add(PARAM_DISCONNECTED_AGEING_STEP_ID);
        descriptions.add(PARAM_TERMINATED_AGEING_STEP_ID);
        descriptions.add(PARAM_DISCONNECTION_FEE_PRODUCT_ID);
        descriptions.add(PARAM_AIT_GROUP_NAME);
        descriptions.add(PARAM_EXCLUDE_FROM_PROVISIONING);
        descriptions.add(PARAM_SERVICE_ORDER_ID_MF_NAME);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] EVENTS = new Class[] {
        PaymentSuccessfulEvent.class,
        NewUserStatusEvent.class,
        SwapAssetsEvent.class,
        UpgradeOrderEvent.class,
        AssetMetaFieldUpdatedEvent.class,
        UpdateCustomerEvent.class,
        ChangeOfPlanEvent.class,
        SapphireSwapAssetEvent.class,
        NewSaleEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("Processing sapphire provisioning task!");
        validateRequiredParameters();
        handleNewSaleEvent(event);
        if(!isProvisioningEnabledForEvent(event)) {
            logger.debug("Provisioning disabled for event {}", event);
            return;
        }
        // registering call back which executes after current transaction commits.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(final int status) {
                // only successfully committed transactions will be transfered to sapphire orchestration layer .
                if (TransactionSynchronization.STATUS_COMMITTED == status) {
                    logger.debug("executing event {} for entity {} after commiting transaction", event.getName(), event.getEntityId());
                    SapphireHelperService helperService = Context.getBean(SapphireHelperService.class);
                    helperService.postEventToProvisioningAsync(event, parameters);
                    return;
                }
                logger.debug("skipping event {} transfer since transaction is roll back for entity {}", event.getName(), event.getEntityId());
            }
        });

    }

    /**
     * populate service id on newly created order from {@link NewSaleRequestWS}.
     * @param event
     */
    private void handleNewSaleEvent(Event event) {
        if(!(event instanceof NewSaleEvent)) {
            return;
        }
        NewSaleEvent newSaleEvent = (NewSaleEvent) event;
        NewSaleRequestWS newSaleRequest = newSaleEvent.getNewSaleRequest();
        if(null == newSaleRequest.getPlanOrderId()) {
            logger.debug("no plan order id found for order {}", newSaleEvent.getOrderId());
            return;
        }
        String serviceMfName = parameters.get(PARAM_SERVICE_ORDER_ID_MF_NAME.getName());
        OrderDTO order = new OrderDAS().find(newSaleEvent.getOrderId());
        if(order.isPlanOrder()) {
            logger.debug("no need to create {}, on order {} since it is plan order", serviceMfName, order.getId());
            return;
        }
        @SuppressWarnings("unchecked")
        MetaFieldValue<Integer> serviceOrderId = order.getMetaField(serviceMfName);
        if(null == serviceOrderId) {
            MetaField orderServiceMf = MetaFieldBL.getFieldByName(event.getEntityId(), new EntityType[] { EntityType.ORDER }, serviceMfName);
            serviceOrderId = new IntegerMetaFieldValue(orderServiceMf);
        }
        logger.debug("Service Id {} set on order {}", newSaleRequest.getPlanOrderId(), order.getId());
        serviceOrderId.setValue(newSaleRequest.getPlanOrderId());
        order.addMetaField(serviceOrderId);
    }

    /**
     * checks Provisioning enable or not for {@link Event}.
     * @param event
     * @return
     */
    private boolean isProvisioningEnabledForEvent(Event event) {
        String excludeFromProvisioning = getParameter(PARAM_EXCLUDE_FROM_PROVISIONING.getName(), StringUtils.EMPTY);
        if(StringUtils.isEmpty(excludeFromProvisioning)) {
            logger.debug("param {} not configured for entity {}", PARAM_EXCLUDE_FROM_PROVISIONING.getName(), getEntityId());
            return Boolean.TRUE;
        }
        Integer customerId;
        UserDAS userDAS = new UserDAS();
        if(event instanceof PaymentSuccessfulEvent) {
            PaymentSuccessfulEvent paymentSuccessfulEvent = (PaymentSuccessfulEvent) event;
            customerId = userDAS.findNow(paymentSuccessfulEvent.getPayment().getUserId()).getCustomer().getId();
        } else if(event instanceof NewUserStatusEvent) {
            NewUserStatusEvent newUserStatusEvent = (NewUserStatusEvent) event;
            customerId = userDAS.findNow(newUserStatusEvent.getUserId()).getCustomer().getId();
        } else if(event instanceof SwapAssetsEvent) {
            SwapAssetsEvent swapAssetsEvent = (SwapAssetsEvent) event;
            customerId = new OrderDAS().findNow(swapAssetsEvent.getOrderId()).getUser().getCustomer().getId();
        } else if(event instanceof UpgradeOrderEvent) {
            UpgradeOrderEvent upgradeOrderEvent = (UpgradeOrderEvent) event;
            customerId = new OrderDAS().findNow(upgradeOrderEvent.getUpgradeOrderId()).getUser().getCustomer().getId();
        } else if(event instanceof OrderMetaFieldUpdateEvent) {
            OrderMetaFieldUpdateEvent orderMetaFieldUpdateEvent = (OrderMetaFieldUpdateEvent) event;
            customerId = new OrderDAS().findNow(orderMetaFieldUpdateEvent.getOrderId()).getUser().getCustomer().getId();
        } else if(event instanceof UpdateCustomerEvent) {
            UpdateCustomerEvent updateCustomerEvent = (UpdateCustomerEvent) event;
            customerId = updateCustomerEvent.getCustomerId();
        } else if(event instanceof ChangeOfPlanEvent) {
            ChangeOfPlanEvent changeOfPlanEvent = (ChangeOfPlanEvent) event;
            customerId = new OrderDAS().findNow(changeOfPlanEvent.getOrderId()).getUser().getCustomer().getId();
        } else if(event instanceof SapphireSwapAssetEvent) {
            SapphireSwapAssetEvent sapphireSwapAssetEvent = (SapphireSwapAssetEvent) event;
            customerId = new OrderDAS().findNow(sapphireSwapAssetEvent.getNewOrderId()).getUser().getCustomer().getId();
        } else if(event instanceof NewSaleEvent) {
            NewSaleEvent newSaleEvent = (NewSaleEvent) event;
            customerId = new OrderDAS().findNow(newSaleEvent.getOrderId()).getUser().getCustomer().getId();
        } else {
            AssetMetaFieldUpdatedEvent assetMetaFieldUpdatedEvent = (AssetMetaFieldUpdatedEvent) event;
            OrderLineDTO orderLine = new AssetDAS().findNow(assetMetaFieldUpdatedEvent.getAssetId()).getOrderLine();
            if(null == orderLine) {
                return Boolean.FALSE;
            } else {
                customerId = orderLine.getPurchaseOrder().getUser().getCustomer().getId();
            }
        }
        CustomerDTO customer = new CustomerDAS().findNow(customerId);
        Integer userId = customer.getBaseUser().getId();
        @SuppressWarnings("unchecked")
        MetaFieldValue<Boolean> excludeFromProvisioningMfValue = customer.getMetaField(excludeFromProvisioning);
        if(null == excludeFromProvisioningMfValue) {
            logger.debug("{} not defined on user {}", excludeFromProvisioning, userId);
            return Boolean.TRUE;
        }
        if(null == excludeFromProvisioningMfValue.getValue() || Boolean.TRUE.equals(excludeFromProvisioningMfValue.getValue())) {
            logger.debug("{} is unchecked on user {} , so Provisioning disabled", excludeFromProvisioning, userId);
            return Boolean.FALSE;
        }
        logger.debug("provisioning enabled on user {}", userId);
        return Boolean.TRUE;
    }

    private void validateRequiredParameters() throws PluggableTaskException {
        String customerProvisioningParamName = PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME.getName();
        Integer entityId = getEntityId();
        String customerProvisioningMfName = getParameters().get(customerProvisioningParamName);
        if(StringUtils.isEmpty(customerProvisioningMfName)) {
            logger.error("{} parameter not configured for plugin {} for entity {}", customerProvisioningParamName, this.getClass().getSimpleName(), entityId);
            throw new PluggableTaskException("parameter "+ customerProvisioningMfName + " not configured for plugin "+
                    this.getClass().getSimpleName() + " for entity "+ entityId);
        }
        MetaField provisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER }, customerProvisioningMfName);
        if(null == provisioningMf) {
            logger.error("{} not present on customer level metafield for entity {}", customerProvisioningMfName, entityId);
            throw new PluggableTaskException(customerProvisioningMfName + " not found on customer level for entity "+ entityId);
        }

        String orderProvisioningParamName = PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName();
        String orderProvisioningMfName = getParameters().get(orderProvisioningParamName);
        if(StringUtils.isEmpty(orderProvisioningMfName)) {
            logger.error("{} parameter not configured for plugin {} for entity {}", orderProvisioningParamName, this.getClass().getSimpleName(), entityId);
            throw new PluggableTaskException("parameter "+ orderProvisioningMfName + " not configured for plugin "+
                    this.getClass().getSimpleName() + " for entity "+ entityId);
        }
        MetaField orderProvisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, orderProvisioningMfName);
        if(null == orderProvisioningMf) {
            logger.error("{} not present on Order level metafield for entity {}", orderProvisioningMfName, entityId);
            throw new PluggableTaskException(orderProvisioningMfName + " not found on order level for entity "+ entityId);
        }

        String excludeFromProvisioning = getParameter(PARAM_EXCLUDE_FROM_PROVISIONING.getName(), StringUtils.EMPTY);
        if(StringUtils.isNotEmpty(excludeFromProvisioning)) {
            MetaField excludeFromProvisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER }, excludeFromProvisioning);
            if(null == excludeFromProvisioningMf) {
                throw new PluggableTaskException(excludeFromProvisioning + " not found on customer level meta field for entity "+ entityId);
            }
        }

        String orderServiceIdMfParamName = PARAM_SERVICE_ORDER_ID_MF_NAME.getName();
        String orderServiceMfName = getParameter(orderServiceIdMfParamName, StringUtils.EMPTY);
        if(StringUtils.isNotEmpty(orderProvisioningMfName)) {
            MetaField orderServiceMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, orderServiceMfName);
            if(null == orderServiceMf) {
                throw new PluggableTaskException(orderServiceMfName + " not found on order level for entity "+ entityId);
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return EVENTS;
    }
}
