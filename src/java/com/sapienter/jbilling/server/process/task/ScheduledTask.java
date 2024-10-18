/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.process.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.sapienter.jbilling.client.process.JobScheduler;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask.IPluggableTaskSessionBean;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 * @author Brian Cowdery
 * @since 04-02-2010
 */
public abstract class ScheduledTask extends PluggableTask implements IScheduledTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String JOB_LIST_KEY = "job_chain_ids";
    public static final String PARAM_ENTITY_ID = "entityId";
    public static final String JOB_LIST_SEPARATOR = ",";

    public static final String EXECUTION_DATE_PARAMETER = "executionDate";

    private boolean useTransaction = false;

    protected static final ParameterDescription JOB_CHAIN_IDS =
            new ParameterDescription(JOB_LIST_KEY, false, ParameterDescription.Type.STR);

    protected Date executionDate = TimezoneHelper.serverCurrentDate();

    //initializer for pluggable params
    public ScheduledTask() {
        descriptions.add(JOB_CHAIN_IDS);
    }

    /**
     * Constructs the JobDetail for this scheduled task, and copies the plug-in parameter
     * map into the detail JobDataMap for use when the task is executed by quartz.
     *
     * @return job detail
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException
     *
     */
    @Override
    public JobDetailImpl getJobDetail () throws PluggableTaskException {
        try {
            JobDetailImpl detail = (JobDetailImpl) JobBuilder.newJob(this.getClass())
                    .withIdentity(getTaskName() + " job", Scheduler.DEFAULT_GROUP)
                    .usingJobData(PARAM_ENTITY_ID, getEntityId())
                    .usingJobData("taskId", getTaskId())
                    .build();
            detail.getJobDataMap().putAll(parameters);
            return detail;
        } catch(Exception ex) {
            logger.error("Quartz Job Creation Failed!", ex);
            throw new PluggableTaskException("Quartz Job Creation Failed!", ex);
        }
    }

    /**
     * Copies plug-in parameters from the JobDetail map into the plug-in's working
     * parameter map. This is a compatibility step so that we don't have to write
     * separate parameter handling code specifically for scheduled tasks.
     *
     * @param context executing job context
     * @throws JobExecutionException thrown if an exception occurs while initializing parameters
     */
    protected void _init (JobExecutionContext context) {
        JobDataMap map = context.getJobDetail().getJobDataMap();
        setEntityId(map.getInt(PARAM_ENTITY_ID));

        parameters = new HashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            parameters.put(entry.getKey(), entry.getValue().toString());
        }
    }

    /**
     * Return this plug-ins schedule as a readable string. Can be used as part of
     * {@link IScheduledTask#getTaskName()} to make the task name unique to the schedule
     * allowing multiple plug-ins of the same type to be added with different schedules.
     *
     * @return schedule string
     */
    public abstract String getScheduleString ();

    /**
     * This method is used for the chained scheduledTasks.
     * It executes the current task and calls the following in the list,
     * the actual task logic goes to {@link this#doExecute(JobExecutionContext)}
     *
     * @param context executing job context
     * @throws JobExecutionException
     */
    @Override
    public void execute (JobExecutionContext context) throws JobExecutionException {
        //Create and run a job in spring batch to use the single job launching feature of spring batch
        if (!Util.getSysPropBooleanTrue(Constants.PROPERTY_RUN_API_ONLY_BUT_NO_BATCH)) {
            PlatformTransactionManager transactionManager = null;
            TransactionStatus status = null;
            if(useTransaction) {
                transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
                DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
                status = transactionManager.getTransaction( transactionDefinition );
            }
            try {
                SchedulerContext schedulerContext = context.getScheduler().getContext();
                executionDate = (Date) schedulerContext.get(EXECUTION_DATE_PARAMETER);
                if(executionDate == null) executionDate = TimezoneHelper.serverCurrentDate();
                schedulerContext.put(EXECUTION_DATE_PARAMETER, null);
                schedulerContext.put(PARAM_ENTITY_ID, getEntityId());

                cleanupHibernateSession();

                //Executes current task logic
                doExecute(context);

                if(useTransaction && null!= transactionManager
                        && null!= status) {
                    transactionManager.commit(status);
                }
            } catch (Throwable t) {
                logger.error("Error while executing batch job", t);
                if(useTransaction && null!= transactionManager
                        && null!= status) {
                    transactionManager.rollback(status);
                }
            }
            //Calls the next task in the chain.
            handleChainedTasks(context);
        }
    }

    private void cleanupHibernateSession () {
        SessionFactory hibernateSessionFactory = Context.getBean(Context.Name.HIBERNATE_SESSION);
        hibernateSessionFactory.getCurrentSession().flush();
        hibernateSessionFactory.getCurrentSession().clear();
    }

    /**
     * Here goes the actual task logic to execute for chained tasks.
     *
     * @param context
     * @throws JobExecutionException
     */
    public void doExecute (JobExecutionContext context) throws JobExecutionException {}

    public void executeOn(Date date) throws SchedulerException, PluggableTaskException{
        JobScheduler jobScheduler = Context.getBean(Context.Name.JOB_SCHEDULER);
        jobScheduler.executeJobOn(getJobDetail().getKey(), EXECUTION_DATE_PARAMETER, date);
    }

    /**
     * Handles the call of the following task in the list
     * taken from the JOB_CHAIN_IDS parameter.
     *
     * @param context
     * @throws JobExecutionException
     */
    protected void handleChainedTasks(JobExecutionContext context) throws JobExecutionException{
        JobDataMap jdMap = context.getJobDetail().getJobDataMap();
        String jobList = (String) jdMap.get(JOB_LIST_KEY);
        if (StringUtils.isNotBlank(jobList)) {
            jobList = jobList.trim();
            String[] jobListArr = jobList.split(JOB_LIST_SEPARATOR);
            if (!ArrayUtils.isEmpty(jobListArr)) {
                if (jobList.contains(JOB_LIST_SEPARATOR)) {
                    jobList = jobList.substring(jobList.indexOf(JOB_LIST_SEPARATOR) + 1, jobList.length());
                } else {
                    jobList = "";
                }
                jdMap.put(JOB_LIST_KEY, jobList);
                try {
                    Integer jobId = Integer.parseInt(jobListArr[0].trim());

                    IPluggableTaskSessionBean iPluggableTaskSessionBean = Context.getBean(Context.Name.PLUGGABLE_TASK_SESSION);
                    PluggableTaskDTO pluggableTaskDTO = iPluggableTaskSessionBean.getDTO(jobId, Integer.parseInt(jdMap.get(PARAM_ENTITY_ID).toString()));
                    PluggableTaskBL<ScheduledTask> taskLoader = new PluggableTaskBL<>();
                    taskLoader.set(pluggableTaskDTO);
                    logger.info("Executing task from a chain with puggableTaskTypeId={}", jobId);
                    taskLoader.instantiateTask().execute(context);
                }catch (NumberFormatException e) {
                    logger.error("Error getting the jobId from the {}  parameter.",JOB_LIST_KEY);
                }
                catch (PluggableTaskException e) {
                    logger.error("Error executing a task from a chain.");
                }
            }
        }
    }

    public boolean isUseTransaction() {
        return useTransaction;
    }

    public void setUseTransaction(boolean useTransaction) {
        this.useTransaction = useTransaction;
    }
}
