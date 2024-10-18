package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Northern911FailureProcessorTask
 * 
 * This task sends an email to the addresses are defined in the task with the error list.
 * 
 * @author Leandro Bagur
 * @since 23/10/17.
 */
public class Northern911FailureProcessorTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger LOG = Logger.getLogger(Northern911FailureProcessorTask.class);
    
    public static final ParameterDescription PARAMETER_NOTIFICATION_ID =
        new ParameterDescription("notification_id", true, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAMETER_EMAILS =
        new ParameterDescription("emails", true, ParameterDescription.Type.STR);

    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_NOTIFICATION_ID);
        descriptions.add(PARAMETER_EMAILS);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        Northern911FailureEvent.class,
    };
    
    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof Northern911FailureEvent) {
            Northern911FailureEvent northernEvent = (Northern911FailureEvent) event; 
            List<String> errors = northernEvent.getErrors();

            try {
                MessageDTO message = new NotificationBL().getCustomNotificationMessage(Integer.valueOf(parameters.get(PARAMETER_NOTIFICATION_ID.getName())), northernEvent.getEntityId(), northernEvent.getUserId(), SpaImportHelper.getLanguageId(SpaConstants.ENGLISH_LANGUAGE));
                message.getParameters().put(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS, parameters.get(PARAMETER_EMAILS.getName()));
                message.getParameters().put(SpaConstants.ERROR_LIST, String.join("\n", errors));
                message.getParameters().put("accountNumber", northernEvent.getUserId());
                INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
                notificationSess.notify(northernEvent.getUserId(), message);    
            } catch (NotificationNotFoundException e) {
                LOG.error("Error sending message to Northern 911 " + e.getMessage());
            }
        }
        
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
