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
import com.sapienter.jbilling.server.item.event.AssetMetaFieldUpdatedEvent;
import com.sapienter.jbilling.server.item.event.SwapAssetsEvent;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.event.OrderMetaFieldUpdateEvent;
import com.sapienter.jbilling.server.order.event.UpgradeOrderEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
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
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] EVENTS = new Class[] {
        PaymentSuccessfulEvent.class,
        NewUserStatusEvent.class,
        SwapAssetsEvent.class,
        UpgradeOrderEvent.class,
        OrderMetaFieldUpdateEvent.class,
        AssetMetaFieldUpdatedEvent.class,
        UpdateCustomerEvent.class,
        ChangeOfPlanEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("Processing sapphire provisioning task!");
        validateRequiredParameters();
        // registering call back which executes after current transaction commits.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(final int status) {
                // only successfully committed transactions will be transfered to sapphire orchestration layer .
                if (TransactionSynchronization.STATUS_COMMITTED == status) {
                    logger.debug("executing event {} for entity {} after commiting transaction", event.getName(), event.getEntityId());
                    SapphireProvisioningHelperService helperService = Context.getBean(SapphireProvisioningHelperService.class);
                    helperService.postEventToProvisioningAsync(event, parameters);
                    return;
                }
                logger.debug("skipping event {} transfer since transaction is roll back for entity {}", event.getName(), event.getEntityId());
            }
        });

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
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return EVENTS;
    }
}
