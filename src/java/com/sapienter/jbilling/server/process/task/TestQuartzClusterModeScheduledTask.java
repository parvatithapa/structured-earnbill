package com.sapienter.jbilling.server.process.task;

import java.lang.invoke.MethodHandles;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

public class TestQuartzClusterModeScheduledTask extends AbstractCronTask {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected static final ParameterDescription PARAM_RUN_DURATION =
    new ParameterDescription("Run Duration(ms)", false, ParameterDescription.Type.STR);

  {
    descriptions.add(PARAM_RUN_DURATION);
  }

  @Override
  public String getTaskName() {
    return "test quartz scheduler: , entity id " + getEntityId() + ", taskId " + getTaskId();
  }
  @Override
  public void doExecute(JobExecutionContext context) throws JobExecutionException {
    super._init(context);

    logger.debug("fired for entity id {}",getEntityId());

    // Determine if we need to simulate processing by just waiting
    String durationStr = getParameterValue(context, PARAM_RUN_DURATION.getName());
    Integer duration = 0;
    if(durationStr != null) {
      duration = Integer.valueOf(durationStr);
    }
    if(duration > 0) {
      try {
        Thread.sleep(duration);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    logger.debug("completed processing");
  }


  private  String getParameterValue(JobExecutionContext context, String parameterName) {
    return  context.getJobDetail().getJobDataMap().getString(parameterName);
  }
}
