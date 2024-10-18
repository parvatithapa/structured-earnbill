/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionFeeChargingEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

/**
 * UsagePoolConsumptionFeeChargingTask
 * This is an event handler that listens to occurrence of UsagePoolConsumptionFeeChargingEvent.
 * When this event occurs, it picks up the fee product and creates a new one-time post paid 
 * order for this fee product. This fee is thus charged to the customer through one-time order.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

public class UsagePoolConsumptionFeeChargingTask extends PluggableTask implements IInternalEventsTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UsagePoolConsumptionFeeChargingTask.class));

    
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
    	UsagePoolConsumptionFeeChargingEvent.class
    };

    public Class<Event>[] getSubscribedEvents() { return events; }

	@Override
	public void process(Event event) throws PluggableTaskException {
		
		if (!(event instanceof UsagePoolConsumptionFeeChargingEvent))
	            throw new PluggableTaskException("Cannot process event " + event);

		UsagePoolConsumptionFeeChargingEvent feeChargingEvent = (UsagePoolConsumptionFeeChargingEvent) event;
		Integer userId = feeChargingEvent.getUserId();
		Integer entityId = feeChargingEvent.getEntityId();
		
        LOG.debug("Processing event: user id "+ feeChargingEvent.getUserId());
        UserBL userBL;
        try {
        	userBL = new UserBL(userId);
        } catch (Exception e2) {
            throw new PluggableTaskException(e2);
        }
        Integer itemId = feeChargingEvent.getAction().getProductId();

        ItemBL item;
        if (itemId != null) {
            try {
                item = new ItemBL(itemId);
            } catch (SessionInternalError e) {
                throw new PluggableTaskException("Cannot find configured fee charges item: " + itemId, e);
            }
        } else {
            throw new PluggableTaskException("No product id configured for fee charge, for consumption action id:" +
                    feeChargingEvent.getAction().getId() );
        }

        UserDTO user = userBL.getEntity();
        // create the order
        OrderDTO order = new OrderDTO();
        OrderPeriodDTO period = new OrderPeriodDTO();
        period.setId(Constants.ORDER_PERIOD_ONCE);
        order.setOrderPeriod(period);

        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(Constants.ORDER_BILLING_POST_PAID);
        order.setOrderBillingType(type);
        order.setCreateDate(Calendar.getInstance().getTime());
        order.setCurrency(user.getCurrency());

        order.setBaseUserByUserId(user);
        
        Integer languageId = user.getLanguageIdField();
        String description = item.getEntity().getDescription(languageId);
        
        OrderLineDTO line = new OrderLineDTO();
        line.setDescription(description);
        line.setItemId(itemId);
        line.setQuantity(1);
        line.setPrice(item.getPrice(userId, line.getQuantity(), entityId));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setPurchaseOrder(order);
        
        order.getLines().add(line);
        
        // now add the item to the po
        OrderBL orderBL = new OrderBL();
        orderBL.set(order);

        // create the db record
        orderBL.create(entityId, null, order);

        OrderChangeDTO orderChangeDTO = new OrderChangeDTO();
        orderChangeDTO.setOrder(order);
        orderChangeDTO.setQuantity(new BigDecimal(1));
        orderChangeDTO.setItem(new ItemDAS().find(itemId));
        orderChangeDTO.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        orderChangeDTO.setStatus(new OrderChangeStatusDAS().find(Constants.ORDER_CHANGE_STATUS_PENDING));
        orderChangeDTO.setUserAssignedStatus(new OrderChangeStatusDAS().find(Constants.ORDER_CHANGE_STATUS_PENDING));
        orderChangeDTO.setUseItem(itemId);
        orderChangeDTO.setOrderChangeType(new OrderChangeTypeDAS().find(Constants.ORDER_CHANGE_TYPE_DEFAULT));
        orderChangeDTO.setUser(user);
        orderChangeDTO.setStartDate(companyCurrentDate());

        IOrderSessionBean orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        Collection<OrderChangeDTO> orderChangeDTOs = new LinkedList<OrderChangeDTO>();
        orderChangeDTOs.add(orderChangeDTO);
        orderSessionBean.createUpdate(event.getEntityId(), user.getCompany().getId(), user.getLanguageIdField(),
                order, orderChangeDTOs, new LinkedList<Integer>());
	}

}
