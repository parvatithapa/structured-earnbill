package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.server.company.event.NewAdminEvent;
import com.sapienter.jbilling.server.notification.*;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Map;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.INT;

/**
 * Created by vivek on 10/8/15.
 */
public class AdminCredentialNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger LOG = Logger.getLogger(AdminCredentialNotificationTask.class);

    private static final Class<Event> events[] = new Class[]{
            NewAdminEvent.class
    };

    private static final ParameterDescription NOTIFICATION_ID = new ParameterDescription("notification_id", true, INT);


    {
        descriptions.add(NOTIFICATION_ID);
    }

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) {

        LOG.debug("Process newAdminEvent  " + event.getName());
        if (event instanceof NewAdminEvent) {
            LOG.debug("Process NewAdminEvent Here");
            sendNotificationToAdmin((NewAdminEvent) event);
        }

    }

    private boolean sendNotificationToAdmin(NewAdminEvent adminEvent) {
        LOG.debug("Try to send notification to admin " + adminEvent.getleadUsername());
        if (StringUtils.trimToNull(parameters.get(NOTIFICATION_ID.getName())) == null) {
            return false;
        }
        MessageDTO message = null;
        UserDTO userDTO = new UserDAS().findByUserName(adminEvent.getleadUsername(), adminEvent.getTargetEntityId());
        UserBL userBL = new UserBL(userDTO.getUserId());
        Integer notificationMessageTypeId = Integer.parseInt(parameters.get(NOTIFICATION_ID.getName()));
        try {
            message = new NotificationBL().getCustomNotificationMessage(notificationMessageTypeId, adminEvent.getEntityId(), userDTO.getId(), userBL.getLanguage());
            MessageSection messageSection = message.getContent()[1];
            String additionalMessage = "#[[\\nCredentials for company " + adminEvent.getTargetEntityId() + "\\n]]#\\n";
            for (Map.Entry<String, String> entry : adminEvent.getLoginCredentials().entrySet()) {
                additionalMessage += "#[[\\nUsername: " + entry.getKey() + " password is: " + entry.getValue() + "\\n]]#\\n";
            }

            messageSection.setContent(messageSection.getContent().concat(additionalMessage));
        } catch (NotificationNotFoundException nnfe) {
            LOG.debug("Notification not found while trying to send notification to admin " + adminEvent.getleadUsername());
        }
        if (message == null) {
            return false;
        }
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        LOG.debug(String.format("Notifying user: %s for a creating new Admin", userDTO.getId()));
        notificationSession.notify(userDTO.getId(), message);


        return true;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
