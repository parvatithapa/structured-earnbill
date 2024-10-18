package com.sapienter.jbilling.server.item.tasks;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.NewStatusEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Constants;

/**
 * The purpose of this task is to change the asset status once the order switches to "Finished" status 
 * and thus the given asset cannot be added or used by another customer order
 * 
 * @author Matías Cabezas 
 * @since 20/10/17.
 */
public class SetAssetToOrderFinishedStatusTask extends PluggableTask implements IInternalEventsTask {
    
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            NewStatusEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        NewStatusEvent newStatusEvent = (NewStatusEvent) event;

        OrderDTO order = new OrderBL(newStatusEvent.getOrderId()).getDTO();

        if (order.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE
                && newStatusEvent.getNewStatusId().equals(new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()))) {
            new AssetBL().setAssetToOrderFinishedStatus(newStatusEvent.getOrderId(), newStatusEvent.getExecutorId());
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
