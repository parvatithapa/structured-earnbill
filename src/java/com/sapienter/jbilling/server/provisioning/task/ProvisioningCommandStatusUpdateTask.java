/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;
import com.sapienter.jbilling.server.provisioning.db.*;
import com.sapienter.jbilling.server.provisioning.event.CommandStatusUpdateEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class ProvisioningCommandStatusUpdateTask extends PluggableTask implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ProvisioningCommandStatusUpdateTask.class));

    private ProvisioningStatusDAS provisioningStatusDas = new ProvisioningStatusDAS();

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof CommandStatusUpdateEvent) {
            CommandStatusUpdateEvent commandStatusEvent = (CommandStatusUpdateEvent)event;

            LOG.debug("Processing command status change " + commandStatusEvent.getProvisioningCommand().getId());

            doProcessEvent(commandStatusEvent.getProvisioningCommand());
        } else {
            throw new PluggableTaskException("Cannot process event " + event);
        }
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            CommandStatusUpdateEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    protected void doProcessEvent(ProvisioningCommandDTO command) throws PluggableTaskException {

        switch (command.getCommandType()) {
            case ORDER:

                IOrderProvisioningCommandDTO orderProvisioningCommand = (IOrderProvisioningCommandDTO)command;
                processOrderProvisioningCommandUpdate(orderProvisioningCommand);
                break;

            case ORDER_LINE:

                IOrderLineProvisioningCommandDTO orderLineProvisioningCommand = (IOrderLineProvisioningCommandDTO)command;
                processOrderLineProvisioningCommandUpdate(orderLineProvisioningCommand);
                break;

            case ASSET:

                IAssetProvisioningCommandDTO assetProvisioningCommand = (IAssetProvisioningCommandDTO)command;
                processAssetProvisioningCommandUpdate(assetProvisioningCommand);
                break;

            case PAYMENT:

                IPaymentProvisioningCommandDTO paymentProvisioningCommand = (IPaymentProvisioningCommandDTO)command;
                processPaymentProvisioningCommandUpdate(paymentProvisioningCommand);
                break;

            default: throw new SessionInternalError("Command type not supported: " + command.getCommandType());
        }
    }

    protected void processPaymentProvisioningCommandUpdate(IPaymentProvisioningCommandDTO paymentProvisioningCommand) {

        PaymentDTO payment = paymentProvisioningCommand.getPayment();
        updateProvisionableStatus(paymentProvisioningCommand.getCommandStatus(), payment);
    }

    protected void processAssetProvisioningCommandUpdate(IAssetProvisioningCommandDTO assetProvisioningCommand) {

        AssetDTO asset = assetProvisioningCommand.getAsset();
        updateProvisionableStatus(assetProvisioningCommand.getCommandStatus(), asset);
    }

    protected void processOrderLineProvisioningCommandUpdate(IOrderLineProvisioningCommandDTO orderLineProvisioningCommand) {

        OrderChangeDTO orderChange = orderLineProvisioningCommand.getOrderChange();
        OrderLineDTO orderLine = orderLineProvisioningCommand.getOrderLine();
        OrderDTO order = orderChange.getOrder();

        if (orderChange != null) {

            if (orderLineProvisioningCommand.getCommandStatus().equals(ProvisioningCommandStatus.SUCCESSFUL)) {
                OrderChangeStatusDTO appliedStatus = new OrderChangeStatusDAS().findApplyStatus(getEntityId());

                OrderChangeDTO newChangeDTO = buildFromLine(orderChange, appliedStatus);

                Date onDate = Util.truncateDate(companyCurrentDate());

                Collection<OrderChangeDTO> orderChanges = Arrays.asList(newChangeDTO);
                Collection<OrderDTO> ordersForUpdate = Arrays.asList(order);

                IOrderSessionBean orderSessionBean = (IOrderSessionBean) Context.getBean(Context.Name.ORDER_SESSION);
                orderSessionBean.applyOrderChangesToOrders(orderChanges, ordersForUpdate, onDate, getEntityId(), true);
            }

            if (orderLine != null) {

                OrderLineDTO updateOrderLineDTO = new OrderLineDAS().findForUpdate(orderLine.getId());
                // update the provisionable status based on the command status
                updateProvisionableStatus(orderLineProvisioningCommand.getCommandStatus(),
                        updateOrderLineDTO);
                new OrderLineDAS().save(updateOrderLineDTO);
            }
        }
    }

    public OrderChangeDTO buildFromLine(OrderChangeDTO oldChangeDTO, OrderChangeStatusDTO status) {

        OrderChangeDTO newChangeDTO = new OrderChangeDTO();
        newChangeDTO.setId(oldChangeDTO.getId());
        newChangeDTO.setParentOrderChange(oldChangeDTO.getParentOrderChange());
        newChangeDTO.setParentOrderLine(oldChangeDTO.getParentOrderLine());
        newChangeDTO.setOrderLine(oldChangeDTO.getOrderLine());
        newChangeDTO.setOrder(oldChangeDTO.getOrder());
        newChangeDTO.setItem(oldChangeDTO.getItem());
        newChangeDTO.setQuantity(oldChangeDTO.getQuantity());
        newChangeDTO.setPrice(oldChangeDTO.getPrice());
        newChangeDTO.setDescription(oldChangeDTO.getDescription());
        newChangeDTO.setUseItem(oldChangeDTO.getUseItem());
        newChangeDTO.setUser(oldChangeDTO.getUser());
        newChangeDTO.setCreateDatetime(oldChangeDTO.getCreateDatetime());
        newChangeDTO.setStartDate(oldChangeDTO.getStartDate());
        newChangeDTO.setApplicationDate(oldChangeDTO.getApplicationDate());
        newChangeDTO.setStatus(oldChangeDTO.getStatus());
        newChangeDTO.setAssets(oldChangeDTO.getAssets());
        newChangeDTO.setErrorMessage(oldChangeDTO.getErrorMessage());
        newChangeDTO.setErrorCodes(oldChangeDTO.getErrorCodes());
        newChangeDTO.setOptLock(oldChangeDTO.getOptLock());

        newChangeDTO.setUserAssignedStatus(status);

        return newChangeDTO;
    }



    protected void processOrderProvisioningCommandUpdate(IOrderProvisioningCommandDTO orderProvisioningCommand) {

        OrderDTO order = orderProvisioningCommand.getOrder();
        updateProvisionableStatus(orderProvisioningCommand.getCommandStatus(), order);

    }

    private void updateProvisionableStatus(ProvisioningCommandStatus provisioningCommandStatus,
                                           IProvisionable provisionable) {

        switch (provisioningCommandStatus) {
            case IN_PROCESS:
                LOG.warn("The command status is still in process");
                throw new SessionInternalError("The command status is still in process");

            case SUCCESSFUL:

                if (provisionable.getProvisioningStatusId().equals(
                        Constants.PROVISIONING_STATUS_PENDING_ACTIVE)) {

                    provisionable.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_ACTIVE));

                } else if (provisionable.getProvisioningStatusId().equals(
                        Constants.PROVISIONING_STATUS_PENDING_INACTIVE)) {

                    provisionable.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_INACTIVE));

                } else {
                    LOG.warn("The provisionable %s has been already updated to status %s ", provisionable,
                            provisionable.getProvisioningStatusId());
                }

                break;

            case FAILED:
            case CANCELLED:
                provisionable.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_FAILED));
                break;

            case UNAVAILABLE:
                provisionable.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_UNAVAILABLE));
                break;

        }

        LOG.debug("Provisioning status set to %s for entity %s", provisionable.getProvisioningStatus(),
                provisionable);
    }
}
