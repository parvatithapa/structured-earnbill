package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.payment.db.PaymentProcessorUnavailableDAS;
import com.sapienter.jbilling.server.payment.db.PaymentProcessorUnavailableDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * PaymentGatewayMonitoringTask class
 * 
 * If there are registered some payment processor unavailable, this task sends a notification to the email defined and 
 * remove the payments found.
 * 
 * @author Leandro Bagur
 * @since 24/11/17
 */
public class PaymentGatewayMonitoringTask extends AbstractCronTask {
    private static final FormatLogger LOG = new FormatLogger(PaymentGatewayMonitoringTask.class);

    private static final String LOG_MESSAGE_PATTERN = "Total payments found with processor unavailable result for entity id %d: %d";

    public static final ParameterDescription PARAMETER_NOTIFICATION_ID =
        new ParameterDescription("Notification Id", true, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAMETER_EMAILS =
        new ParameterDescription("Emails", true, ParameterDescription.Type.STR);

    {
        descriptions.add(PARAMETER_NOTIFICATION_ID);
        descriptions.add(PARAMETER_EMAILS);
    }

    public PaymentGatewayMonitoringTask() {
        setUseTransaction(true);
    }
    
    @Override
    public String getTaskName() {
        return "payment monitoring process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        PaymentProcessorUnavailableDAS paymentProcessorUnavailableDAS = new PaymentProcessorUnavailableDAS();
        List<PaymentProcessorUnavailableDTO> paymentProcessorUnavailableDTOList = paymentProcessorUnavailableDAS.findByEntity(getEntityId());
        
        if (!paymentProcessorUnavailableDTOList.isEmpty()) {
            LOG.info(String.format(LOG_MESSAGE_PATTERN, getEntityId(), paymentProcessorUnavailableDTOList.size()));
            Integer notificationId = context.getJobDetail().getJobDataMap().getInt(PARAMETER_NOTIFICATION_ID.getName());
            String emails = context.getJobDetail().getJobDataMap().getString(PARAMETER_EMAILS.getName());

            NotificationBL notificationBL = new NotificationBL();
            notificationBL.set(notificationId,
                               SpaImportHelper.getLanguageId(SpaConstants.ENGLISH_LANGUAGE),
                               getEntityId());
            MessageDTO message = notificationBL.getDTO();
            message.getParameters().put(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS, emails);
            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(new UserDAS().findAdminUsers(getEntityId()).get(0), message);

            paymentProcessorUnavailableDTOList.forEach(paymentProcessorUnavailableDAS::delete);
        }
    }
}
