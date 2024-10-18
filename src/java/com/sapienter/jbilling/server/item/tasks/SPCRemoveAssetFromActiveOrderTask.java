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
package com.sapienter.jbilling.server.item.tasks;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.AssetStatusBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.event.AbstractAssetEvent;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.SPCRemoveAssetFromActiveOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Constants;

public class SPCRemoveAssetFromActiveOrderTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Class<Event> events[] = new Class[] { SPCRemoveAssetFromActiveOrderEvent.class };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("processing event {}", event);
        SPCRemoveAssetFromActiveOrderEvent removeAssetFromActiveOrderEvent = (SPCRemoveAssetFromActiveOrderEvent) event;
        OrderBL orderBL = new OrderBL(removeAssetFromActiveOrderEvent.getOrderId());
        OrderDTO order = orderBL.getDTO();
        Integer userId = order.getUser().getId();
        Date activeUntilDate = null != order.getActiveUntil() ? order.getActiveUntil() : order.getFinishedDate();
        Date companyCurrentDate = companyCurrentDate();

        List<AbstractAssetEvent> assetEvents = new ArrayList<>();

        if (order.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE && null != activeUntilDate && !activeUntilDate.after(companyCurrentDate)) {
            for (OrderLineDTO orderLineDTO : order.getLines()) {
                for (AssetDTO orderLineAsset : orderLineDTO.getAssets()) {
                    AssetStatusDTO defaultAssetStatus = new AssetStatusBL().findDefaultStatusForItem(orderLineAsset.getItem().getId());
                    logger.debug("Removng asset {} from order line {}", orderLineAsset.getIdentifier(), orderLineDTO.getId());
                    orderBL.removeAssetFromOrderLine(orderLineDTO, userId, defaultAssetStatus, orderLineAsset, assetEvents, null);
                }
            }
            logger.debug("Processing events required to complete asset release");
            assetEvents.stream().forEach(EventManager :: process);
        }        
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
