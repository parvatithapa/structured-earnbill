package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.provisioning.IProvisioningProcessSessionBean;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;

/**
 * Created by marcolin on 16/06/16.
 */
public class ProvisioningTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AgeingProcessTask.class));

    public String getTaskName() {
        return "ageing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super.doExecute(context);
        IProvisioningProcessSessionBean remoteProvisioningProcess = Context.getBean(
                Context.Name.PROVISIONING_PROCESS_SESSION);
        LOG.info("Starting provisioning process at %s", Calendar.getInstance().getTime());
        remoteProvisioningProcess.trigger(getEntityId());
        LOG.info("Ended provisioning process at %s", Calendar.getInstance().getTime());
    }

}
