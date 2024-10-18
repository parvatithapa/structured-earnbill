package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.IInvoiceSessionBean;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.util.Calendar;

/**
 * Created by marcolin on 16/06/16.
 */
public class InvoiceReminderTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AgeingProcessTask.class));

    public String getTaskName() {
        return "ageing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super.doExecute(context);
        IInvoiceSessionBean remoteInvoice = Context.getBean(Context.Name.INVOICE_SESSION);
        LOG.info("Starting invoice reminders at %s", Calendar.getInstance().getTime());
        remoteInvoice.sendReminders(Calendar.getInstance().getTime());
        LOG.info("Ended invoice reminders at %s", Calendar.getInstance().getTime());
    }

}
