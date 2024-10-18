package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.log4j.Logger;

public class PenaltyCancellationTask extends PluggableTask implements
		IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PenaltyCancellationTask.class));

    private static final Class<Event> events[] = new Class[] { 
            NewUserStatusEvent.class };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

	public void process(Event event) throws PluggableTaskException {
		
		NewUserStatusEvent newUserStatusEvent = (NewUserStatusEvent) event;
		UserDTO user = newUserStatusEvent.getUser();
		
		if (null != user) {
	    
			UserStatusDTO newStatus = user.getUserStatus();
	        if (null == newStatus || (null != newStatus && newStatus.isSuspended())) {
	            return;
	        }
	
	        UserStatusDTO oldStatus = new UserStatusDAS().find(newUserStatusEvent.getOldStatusId());
	        
	        UserBL userbl = new UserBL(newUserStatusEvent.getUserId());
	    	OrderDTO activePenaltyOrder = null;
	        
	        // If user was already suspended or higher, then change the active until date
	        if (oldStatus.isSuspended() && newStatus.getId() == UserDTOEx.STATUS_ACTIVE) {
	        	for (OrderDTO order : userbl.getEntity().getOrders()) {
	                if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.INVOICE)) {
	        			for (OrderLineDTO orderline : order.getLines()) {
	        				if (orderline.getOrderLineType().getId() == Constants.ORDER_LINE_TYPE_PENALTY) {
	        					activePenaltyOrder = order;
	        					break;
	        				}
	        			}
	                }
	        	}
	        	
	        	if (null != activePenaltyOrder) 
	        		activePenaltyOrder.setActiveUntil(companyCurrentDate());
	        }
	        
		}
	}
}
