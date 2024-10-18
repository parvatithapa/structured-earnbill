package com.sapienter.jbilling.server.pluggableTask;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;	
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.CustomEmailTokenEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * This task subscribes to the {@link CustomEmailTokenEvent} 
 *
 * @author Mahesh Shivarkar
 * @since  09-06-2016
 */

public class FullCreativeCustomEmailTokenTask extends PluggableTask
implements IInternalEventsTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FullCreativeCustomEmailTokenTask.class));

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
    	CustomEmailTokenEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
	
	public void process(Event event) throws PluggableTaskException {
		
		if (!(event instanceof CustomEmailTokenEvent)) {
            throw new PluggableTaskException("Cannot process event " + event);
        }
		
		LOG.debug("Processing " + event);
		
		CustomEmailTokenEvent customEmailTokenEvent = (CustomEmailTokenEvent) event;
        UserDTO user = new UserBL(customEmailTokenEvent.getUserId()).getDto();
        String primaryAccountNumber =
        		getPrimaryAccountNumberByCustomerIdAndEntityId(customEmailTokenEvent.getUserId(),customEmailTokenEvent.getEntityId());
        
        try {
	        MessageDTO message = customEmailTokenEvent.getMessage();
	        if (null != message) {
	        	message.addParameter("accountNumber", primaryAccountNumber);
	        }
	        
	        Map<String, Object> parameters = customEmailTokenEvent.getParameters();
	        if (null != parameters) {
		        parameters.put("accountNumber", primaryAccountNumber);
	        }
        } catch (Exception e){
        	throw new PluggableTaskException("Message or parameters may be null");
        }
	}

	private static String getPrimaryAccountNumberByCustomerIdAndEntityId(Integer userId, Integer entityId) {
    	if(null == userId || null == entityId) {
    		return null;
    	}
    	return new CustomerDAS().getPrimaryAccountNumberByUserAndEntityId(userId, entityId);
    }
}
