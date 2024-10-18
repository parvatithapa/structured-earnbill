package com.sapienter.jbilling.server.meteredUsage;

import com.sapienter.jbilling.server.process.event.ReservedMonthlyChargeEvent;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ReservedMonthlySubscriptionReportTask extends AbstractCronTask  {

    private static final String TASK_NAME = "ReservedMonthlySubscriptionReportTask";

    public String getTaskName() {
        return TASK_NAME + "entity id:" + getEntityId() + ",:taskId" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {

        EventManager.process(new ReservedMonthlyChargeEvent(getEntityId()));
    }


}
