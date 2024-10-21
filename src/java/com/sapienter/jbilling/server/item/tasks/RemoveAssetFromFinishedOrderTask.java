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

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.event.NewStatusEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;


/**
 * Listens for {@link com.sapienter.jbilling.server.order.event.NewStatusEvent} events.
 * If the new status is FINISHED, the task will unlink all assets from the order and assign them the default
 * status.
 *
 * @author Gerhard
 * @since 13/5/2013
 */
public class RemoveAssetFromFinishedOrderTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(Logger.getLogger(RemoveAssetFromFinishedOrderTask.class));

    public RemoveAssetFromFinishedOrderTask () {
        descriptions.add(NUMBER_OF_DAYS);
    }

    protected static final ParameterDescription NUMBER_OF_DAYS =
            new ParameterDescription("Number of Days", false, ParameterDescription.Type.INT);

    private static final Class<Event> events[] = new Class[] {
            NewStatusEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        NewStatusEvent newStatusEvent = (NewStatusEvent) event;
        Integer  numberOfDays = getParameter(NUMBER_OF_DAYS.getName(),0);
        Calendar cal = Calendar.getInstance();
        //load the order
        OrderDTO order = new OrderBL(newStatusEvent.getOrderId()).getDTO();
        Date activeUntilDate = null != order.getActiveUntil() ? order.getActiveUntil() : order.getFinishedDate();
        Date companyCurrentDate = companyCurrentDate();
        //When number of days parameter value not present and Active Until Date before current date then remove all assets from order.
        //orders with a period of once will immediately go to a finished state after billing, ignore them
        if (order.getOrderPeriod().getId() != com.sapienter.jbilling.server.util.Constants.ORDER_PERIOD_ONCE
                && newStatusEvent.getNewStatusId().equals(new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()))) {
            if (null == activeUntilDate && numberOfDays!= 0) {
                logger.debug("Order don't have Active Until/Finished Date: ", activeUntilDate);
                throw new SessionInternalError("Order {} don't have Active Until/Finished Date"+ order.getId());
            }
            if(numberOfDays == 0) {
                new AssetBL().unlinkAssets(newStatusEvent.getOrderId(), newStatusEvent.getExecutorId());
            } else {
                cal.setTime(activeUntilDate);
                cal.add(Calendar.DATE,numberOfDays);
                if(cal.getTime().before(companyCurrentDate)){
                    new AssetBL().unlinkAssets(newStatusEvent.getOrderId(), newStatusEvent.getExecutorId());
            }
          }
      }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
