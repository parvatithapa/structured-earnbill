package com.sapienter.jbilling.server.sapphire.provisioninig;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchangeDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;

/**
 *
 * @author Ashwinkumar Patra
 *
 */
public class SapphireProvisioningRetryTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_SAPPHIRE_PROVISIONING_PLUGIN_ID =
            new ParameterDescription("sapphire_provisioning_task_id", true, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_MAX_RETRY =
            new ParameterDescription("max_retry", true, ParameterDescription.Type.INT);

    public SapphireProvisioningRetryTask() {
        descriptions.add(PARAM_SAPPHIRE_PROVISIONING_PLUGIN_ID);
        descriptions.add(PARAM_MAX_RETRY);
    }

    @Override
    public String getTaskName() {
        return "SapphireProvisioningRetryTask: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        synchronized (SapphireProvisioningRetryTask.class) {
            try {
                _init(context);
                String pluginId = parameters.get(PARAM_SAPPHIRE_PROVISIONING_PLUGIN_ID.getName());
                if(StringUtils.isEmpty(pluginId)) {
                    logger.debug("{} not configured for task {}", PARAM_SAPPHIRE_PROVISIONING_PLUGIN_ID.getName(), getTaskName());
                    return;
                }
                if(!NumberUtils.isDigits(pluginId)) {
                    logger.debug("{} is not number ", pluginId);
                    return;
                }
                String maxRetry = parameters.get(PARAM_MAX_RETRY.getName());
                if(StringUtils.isEmpty(maxRetry)) {
                    logger.error("{} not configured for task {}", PARAM_MAX_RETRY.getName(), getTaskName());
                    return;
                }
                if(!NumberUtils.isDigits(maxRetry)) {
                    logger.error("{} is not number  for task {}", maxRetry, getTaskName());
                    return;
                }
                IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
                SapphireHelperService helperService = Context.getBean(SapphireHelperService.class);
                OutBoundInterchangeDAS outBoundInterchangeDAS = Context.getBean(OutBoundInterchangeDAS.class);
                List<Integer> failedRequestIds = txAction.execute(() ->
                outBoundInterchangeDAS.findAllFailedOutBoundInterchangeRequestIdsForEntity(getEntityId(), Integer.parseInt(maxRetry)));
                logger.debug("retrying {} for entity {}", failedRequestIds, getEntityId());
                for(Integer failedRequestId : failedRequestIds) {
                    helperService.retryOutBoundInterchangeRequest(failedRequestId, Integer.parseInt(pluginId),
                            getEntityId(), Integer.parseInt(maxRetry));
                }
            } catch(Exception ex) {
                logger.error("tasked failed for entity {} ", getEntityId(), ex);
            }
        }
    }

}
