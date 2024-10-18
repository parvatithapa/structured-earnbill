package com.sapienter.jbilling.server.distributel;

import org.apache.commons.lang.StringUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class DistributelStepDecider implements JobExecutionDecider {

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        String tableName = jobParameters.getString(DistributelPriceJobConstants.PARAM_PRICE_REVERSAL_DATA_TABLE_NAME);
        if(StringUtils.isEmpty(tableName)) {
            // means job done.
            return new FlowExecutionStatus("NO");
        }
        // if parameter found, means need to execute price removal step.
        return new FlowExecutionStatus("YES");
    }

}
