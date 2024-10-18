package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;

/**
 * Created by marcolin on 16/06/16.
 */
public class UserCreditCardExpirationNotificationTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AgeingProcessTask.class));

    public String getTaskName() {
        return "ageing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super.doExecute(context);
        IUserSessionBean remoteUser = Context.getBean(Context.Name.USER_SESSION);
        LOG.info("Starting credit card expiration at %s", Calendar.getInstance().getTime());
        remoteUser.notifyCreditCardExpiration(Calendar.getInstance().getTime());
        LOG.info("Ended credit card expiration at %s", Calendar.getInstance().getTime());
    }

}
