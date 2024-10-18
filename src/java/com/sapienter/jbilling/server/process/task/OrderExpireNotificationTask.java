package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by marcolin on 16/06/16.
 */
public class OrderExpireNotificationTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AgeingProcessTask.class));

    public String getTaskName() {
        return "ageing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super.doExecute(context);
        IOrderSessionBean remoteOrder = Context.getBean(Context.Name.ORDER_SESSION);
        LOG.info("Starting order notification at %s", Calendar.getInstance().getTime());
        remoteOrder.reviewNotifications(Calendar.getInstance().getTime());
        LOG.info("Ended order notification at " + Calendar.getInstance().getTime());
    }

}
