package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional( propagation = Propagation.REQUIRED )
public class JobExecutionSessionBean implements IJobExecutionSessionBean {

    @Override
    public JobExecutionHeaderDTO startJob(long jobExecutionId, String type, Date startDate, int entityId) {
        return new JobExecutionBL().startJob(jobExecutionId, type, startDate, entityId);
    }

    @Override
    public void endJob(long jobExecutionId, Date endDate) {
        new JobExecutionBL().forExecution(jobExecutionId).endJob(endDate);
    }

    @Override
    public void endJob(long jobExecutionId, Date endDate, JobExecutionHeaderDTO.Status status) {
        new JobExecutionBL().forExecution(jobExecutionId).endJob(endDate, status);
    }

    @Override
    public void addLine(long jobExecutionId, String type, String name, String value) {
        new JobExecutionBL().forExecution(jobExecutionId).addLine(type, name, value);
    }

    @Override
    public void updateLine(long jobExecutionId, String type, String name, String value) {
        new JobExecutionBL().forExecution(jobExecutionId).updateLine(type, name, value);
    }

    @Override
    public void incrementLine(long jobExecutionId, String type, String name) {
        new JobExecutionBL().forExecution(jobExecutionId).incrementLine(type, name);
    }
}
