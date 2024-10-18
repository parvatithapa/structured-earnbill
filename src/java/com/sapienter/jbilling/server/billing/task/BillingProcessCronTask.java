package com.sapienter.jbilling.server.billing.task;

import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.IPluggableTaskSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.process.task.ScheduledTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

public class BillingProcessCronTask extends AbstractCronTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BillingProcessCronTask.class));
	// This default cron expression will never run as it is in past.
	private static final String defaultCronExpression = "0 0 0 1 1 ? 1970";  
	
	@Override
	public String getTaskName() {
		return this.getClass().getName() + "-" + getEntityId();
	}

	public void doExecute(JobExecutionContext context) throws JobExecutionException {
		 //super.execute(context);//_init(context);

	        IBillingProcessSessionBean billing = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

	        LOG.info("Starting billing at " + TimezoneHelper.serverCurrentDate() + " for " + getEntityId());
	        billing.trigger(companyCurrentDate(), getEntityId());
	        LOG.info("Ended billing at " + TimezoneHelper.serverCurrentDate());
	        
	        handleChainedTasks(context);
	}
	
	@Override
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
                for (int i=0;i<jobListArr.length;i++) {
	                try {
	                    Integer jobId = Integer.parseInt(jobListArr[i].trim());
	
	                    IPluggableTaskSessionBean iPluggableTaskSessionBean = (IPluggableTaskSessionBean) Context.getBean(Context.Name.PLUGGABLE_TASK_SESSION);
	                    PluggableTaskDTO pluggableTaskDTO = iPluggableTaskSessionBean.getDTO(jobId, Integer.parseInt(jdMap.get("entityId").toString()));
	                    PluggableTaskBL<ScheduledTask> taskLoader = new PluggableTaskBL<ScheduledTask>();
	                    taskLoader.set(pluggableTaskDTO);
	                    System.out.println("");
	                    LOG.info("Executing task from a chain with puggableTaskTypeId=" + jobId);
	                    taskLoader.instantiateTask().execute(context);
	                }catch (NumberFormatException e) {
	                    LOG.error("Error getting the jobId from the " + JOB_LIST_KEY + " parameter.");
	                    e.printStackTrace();
	                }
	                catch (PluggableTaskException e) {
	                    LOG.error("Error executing a task from a chain.");
	                    e.printStackTrace();
	                }
                }
            }
        }
	}
	
	// Overriding default cron expression to avoid executing every day at 12 noon.
	@Override
	protected String getParameter(String key, String defaultValue) {
        Object value = parameters.get(key);
        return value != null ? (String) value : defaultCronExpression;
    }
}
