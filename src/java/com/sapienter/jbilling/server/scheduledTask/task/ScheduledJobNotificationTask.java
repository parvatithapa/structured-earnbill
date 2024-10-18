package com.sapienter.jbilling.server.scheduledTask.task;

import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.scheduledTask.event.ScheduledJobNotificationEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Util;
import grails.util.Holders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Sends Notifications upon trigger events for Scheduled Tasks / Batch Jobs.
 *
 * @author Aadil Nazir
 * @since 04/26/18
 */
public class ScheduledJobNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
            ScheduledJobNotificationEvent.class
    };

    // task / job started notification id
    public static final ParameterDescription PARAMETER_TASK_JOB_STARTED_NOTIFICATION_ID =
            new ParameterDescription("Task/Job started Notification ID", true, ParameterDescription.Type.INT);

    // task / job ended notification id
    public static final ParameterDescription PARAMETER_TASK_JOB_COMPLETED_NOTIFICATION_ID =
            new ParameterDescription("Task/Job completed Notification ID", true, ParameterDescription.Type.INT);

    // Additional Email address
    public static final ParameterDescription PARAMETER_ADDITIONAL_EMAIL_ADDRESS =
            new ParameterDescription("Additional Email Address (comma separated)", true, ParameterDescription.Type.STR);

    {
        descriptions.add(PARAMETER_TASK_JOB_STARTED_NOTIFICATION_ID);
        descriptions.add(PARAMETER_TASK_JOB_COMPLETED_NOTIFICATION_ID);
        descriptions.add(PARAMETER_ADDITIONAL_EMAIL_ADDRESS);
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        if (event instanceof ScheduledJobNotificationEvent) {

            ScheduledJobNotificationEvent scheduledJobNotificationEvent = (ScheduledJobNotificationEvent) event;
            logger.debug("ScheduledJobNotificationTask : process");
            logger.debug("scheduledJobNotificationEvent.getJobExecutionContext() : {}", scheduledJobNotificationEvent.getJobExecutionContext());
            logger.debug("scheduledJobNotificationEvent.getJobExecution() : {}", scheduledJobNotificationEvent.getJobExecution());

            String jobStartedNotificationIdString = parameters.get(PARAMETER_TASK_JOB_STARTED_NOTIFICATION_ID.getName());
            String jobCompletedNotificationIdString = parameters.get(PARAMETER_TASK_JOB_COMPLETED_NOTIFICATION_ID.getName());
            String additionalEmailAddresses = parameters.get(PARAMETER_ADDITIONAL_EMAIL_ADDRESS.getName()).trim();

            if(additionalEmailAddresses.isEmpty()) {
                logger.warn("No email address configured in ScheduledJobNotificationTask");
                return;
            }

            Integer jobStartedNotificationId = getNotificationId(jobStartedNotificationIdString);
            Integer jobCompletedNotificationId = getNotificationId(jobCompletedNotificationIdString);
            Integer notificationId = null;
            Integer entityId = scheduledJobNotificationEvent.getEntityId();
            String jobName = scheduledJobNotificationEvent.getJobName();


            logger.debug("Event Obtained : {}", scheduledJobNotificationEvent.getTaskEventType());

            ScheduledJobNotificationEvent.TaskEventType taskEventType = scheduledJobNotificationEvent.getTaskEventType();

            if (taskEventType.equals(ScheduledJobNotificationEvent.TaskEventType.TRIGGER_FIRED) ||
                    taskEventType.equals(ScheduledJobNotificationEvent.TaskEventType.BEFORE_JOB)) {
                notificationId = jobStartedNotificationId;
            }
            else if (taskEventType.equals(ScheduledJobNotificationEvent.TaskEventType.TRIGGER_COMPLETED) ||
                    taskEventType.equals(ScheduledJobNotificationEvent.TaskEventType.AFTER_JOB)) {
                notificationId = jobCompletedNotificationId;
            }

            logger.debug("Notification Id for the obtained event : {}", notificationId);

            if (null == notificationId) {
                logger.warn("Appropriate notification ID is not configured in ScheduledJobNotificationTask");
                return;
            }

            try {

                IUserSessionBean userSessionBean = (IUserSessionBean) Context.getBean(
                        Context.Name.USER_SESSION);
                Integer adminUserId = userSessionBean.findAdminUserIds(entityId).get(0);
                UserDTOEx userDTOEx = userSessionBean.getUserDTOEx(adminUserId);
                logger.debug("userDTOEx {}",userDTOEx);

                MessageDTO message = new NotificationBL().getJobEventNotification(
                        entityId,
                        userDTOEx.getLanguageId(),
                        notificationId);

                message.addParameter("url", Holders.getFlatConfig().get("grails.serverURL"));
                message.addParameter("company_id", entityId.toString());
                message.addParameter("plugin_name", jobName);
                message.addParameter("start_time", Util.getCurrentDateInUTC("h:mm a E zz"));
                message.addParameter("end_time", Util.getCurrentDateInUTC("h:mm a E zz"));

                for (String email : additionalEmailAddresses.split(",") ) {
                    message.addParameter("specificEmailAddress", email.trim());
                    // notify the user with the notification message
                    INotificationSessionBean notification = (INotificationSessionBean) Context.getBean(
                            Context.Name.NOTIFICATION_SESSION);
                    notification.notify(userDTOEx.getUserId(), message);
                }

            } catch (NotificationNotFoundException e) {
                logger.warn("Failed to send Job Trigger Event notification. Entity {}", entityId);
            }

        }

    }

    /**
     *  Retrieves the notification message id provided in the parameter of plugin
     *
     * @param notificationIdString
     * @return notification message id; null if no message is applicable
     */
    protected Integer getNotificationId(String notificationIdString) {

        if (null != notificationIdString  && !notificationIdString.trim().isEmpty()) {
            try {
                return Integer.valueOf(notificationIdString);
            } catch (NumberFormatException e) {
                logger.warn("Cannot parse attribute value {} as an integer.", notificationIdString);
            }
        }
        return null;
    }
}
