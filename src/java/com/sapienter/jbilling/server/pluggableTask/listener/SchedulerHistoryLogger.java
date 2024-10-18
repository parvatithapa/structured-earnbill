package com.sapienter.jbilling.server.pluggableTask.listener;

import java.lang.invoke.MethodHandles;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.schedulerhistory.db.ScheduledProcessRunHistoryDAS;
import com.sapienter.jbilling.server.schedulerhistory.db.ScheduledProcessRunHistoryDTO;
import com.sapienter.jbilling.server.schedulerhistory.db.ScheduledProcessRunHistoryDTO.SchedulerStatus;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;


/**
 *
 * @author krunal Bhavsar
 *
 */
public class SchedulerHistoryLogger implements JobListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String HISTORY_OBJ_ID = "historyObjId";

    @Override
    public String getName() {
        return "SchedulerHistoryLogger";
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {
        logger.trace("jobExecutionVetoed");
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {
        try {
            String jobName = jobExecutionContext.getJobDetail().getKey().toString();
            logger.debug("{} has Started!", jobName);
            if(Context.getApplicationContext() == null) {
                logger.warn("Application  context Has not been initialized!");
                return;
            }
            IMethodTransactionalWrapper txWrapper = Context.getBean(IMethodTransactionalWrapper.class);
            txWrapper.execute(()-> doLogErrorAndExecuteOperation(() -> {
                JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
                Integer entityId = map.getInt("entityId");
                String taskName =  jobExecutionContext.getJobDetail().getJobClass().getSimpleName();
                ScheduledProcessRunHistoryDTO schedulerRunHistoryDTO = new ScheduledProcessRunHistoryDTO();
                schedulerRunHistoryDTO.setEntityId(entityId);
                schedulerRunHistoryDTO.setName(taskName);
                schedulerRunHistoryDTO.setStartDatetime(TimezoneHelper.companyCurrentDate(entityId));
                schedulerRunHistoryDTO.setStatus(SchedulerStatus.STARTED);
                schedulerRunHistoryDTO = new ScheduledProcessRunHistoryDAS().save(schedulerRunHistoryDTO);
                logger.debug("Scheduler History saved {}", schedulerRunHistoryDTO);
                jobExecutionContext.put(HISTORY_OBJ_ID, schedulerRunHistoryDTO.getId());
            }));
        } catch(Exception ex) {
            logger.error("Error in before job!", ex);
        }
    }

    @Override
    public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException exception) {
        try {
            String jobName = jobExecutionContext.getJobDetail().getKey().toString();
            logger.debug("{} has finished!", jobName);
            if(exception!=null) {
                logger.error("Job {} finished with error {}", jobName, exception);
            }
            if(Context.getApplicationContext() == null) {
                logger.warn("Application  context Has not been initialized!");
                return;
            }
            IMethodTransactionalWrapper txWrapper = Context.getBean(IMethodTransactionalWrapper.class);
            txWrapper.execute(()-> doLogErrorAndExecuteOperation(() -> {
                ScheduledProcessRunHistoryDAS scheduledRunHistoryDAS = new ScheduledProcessRunHistoryDAS();
                Integer id = (Integer) jobExecutionContext.get(HISTORY_OBJ_ID);
                if( null == id ) {
                    logger.warn("History Object id not found ");
                    return;
                }
                ScheduledProcessRunHistoryDTO schedulerRunHistoryDTO = scheduledRunHistoryDAS.find(id);
                schedulerRunHistoryDTO.setEndDatetime(TimezoneHelper.companyCurrentDate(schedulerRunHistoryDTO.getEntityId()));
                schedulerRunHistoryDTO.setStatus(SchedulerStatus.FINISHED);
                if(null!= exception) {
                    schedulerRunHistoryDTO.setErrorMessage(exception.getMessage());
                }
                schedulerRunHistoryDTO = scheduledRunHistoryDAS.save(schedulerRunHistoryDTO);
                logger.debug("Scheduler History Updated {}", schedulerRunHistoryDTO);
            }));
        } catch(Exception ex) {
            logger.error("Error in after job!", ex);
        }
    }

    private void doLogErrorAndExecuteOperation(Runnable action) {
        try {
            action.run();
        } catch(Exception ex) {
            logger.error("Error During create/update scheduler history", ex);
        }
    }

}
