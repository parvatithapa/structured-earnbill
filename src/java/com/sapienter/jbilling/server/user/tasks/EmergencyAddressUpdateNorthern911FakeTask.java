package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.event.EmergencyAddressUpdateEvent;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

/**
 * Created by Wajeeha on 4/10/2017.
 */
public class EmergencyAddressUpdateNorthern911FakeTask  extends PluggableTask implements IInternalEventsTask {

    private static final Logger LOG = Logger.getLogger(EmergencyAddressUpdateNorthern911FakeTask.class);

    private static final Class<Event>[] events = new Class[]{
            EmergencyAddressUpdateEvent.class,
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if(event instanceof EmergencyAddressUpdateEvent) {
            EmergencyAddressUpdateEvent emergencyAddressUpdateEvent = ((EmergencyAddressUpdateEvent) event);
            ContactDTO contactDTO = emergencyAddressUpdateEvent.getContactDto();

            if(!NumberUtils.isNumber(contactDTO.getPhoneNumber())){
                emergencyAddressUpdateEvent.setUpdated(false);
                emergencyAddressUpdateEvent.setErrorResponse("200:Phone number contains non-numeric characters.");
            }else{
                emergencyAddressUpdateEvent.setUpdated(true);
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
