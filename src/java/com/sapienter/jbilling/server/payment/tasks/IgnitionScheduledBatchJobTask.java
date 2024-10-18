package com.sapienter.jbilling.server.payment.tasks;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

/**
 * Created by wajeeha on 2/19/18.
 */
public class IgnitionScheduledBatchJobTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_PROCESS_LATEST_INVOICE =
            new ParameterDescription("Process Latest Invoice", true, ParameterDescription.Type.BOOLEAN);

    public IgnitionScheduledBatchJobTask() {
        // Initializer for pluggable params
        descriptions.add(PARAM_PROCESS_LATEST_INVOICE);
    }

    @Override
    public void doExecute (JobExecutionContext context) throws JobExecutionException {
        try {
            Boolean processLatestOrder = getProcessLatestOrder(context);
            Date currentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(this.getEntityId()));
            IMethodTransactionalWrapper txWrapper = Context.getBean(IMethodTransactionalWrapper.class);
            Optional<Integer> actionDateMetafieldId = txWrapper.<Optional<Integer>>execute(()-> {
                MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
                MetaField actionDateMetafield = metaFieldDAS.getFieldByName(getEntityId(), new EntityType[]{EntityType.CUSTOMER},IgnitionConstants.USER_ACTION_DATE);
                return actionDateMetafield!=null ? Optional.of(actionDateMetafield.getId()) : Optional.empty();
            });

            if(!actionDateMetafieldId.isPresent()) {
                logger.debug("Skipping {} for entity {} since customer level meta field {} not found!", getTaskName(),
                        getEntityId(), IgnitionConstants.USER_ACTION_DATE);
                return ;
            }

            JobLauncher jobLauncher = Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);
            Job generateIgnitionPaymentsJob = Context.getBean(Context.Name.BATCH_JOB_GENERATE_IGNITION_PAYMENTS);
            String holidays = loadHolidays(context);
            String debitDateHolidays = loadDebitDateUpdateHolidays(context);

            Map<String, JobParameter> jobParams = new HashMap<>();
            jobParams.put("actionDate", new JobParameter(currentDate));
            jobParams.put("processLatestOrder", new JobParameter(processLatestOrder.toString()));
            jobParams.put("entityId", new JobParameter(getEntityId().toString()));
            jobParams.put("startDate", new JobParameter(new Date()));
            jobParams.put("actionDateMfId", new JobParameter(String.valueOf(actionDateMetafieldId.get())));

            JobExecution paymentsJobExecution = jobLauncher.run(generateIgnitionPaymentsJob, new JobParameters(jobParams));
            logger.debug("Payment Generation Job execution id is {} for entity {}", paymentsJobExecution.getId(), getEntityId());

            Job updateIgnitionCustomerJob = Context.getBean(Context.Name.BATCH_JOB_UPDATE_CUSTOMER_NEXT_ACTION_DATE);

            jobParams = new HashMap<>();
            jobParams.put("holidays", new JobParameter(holidays));
            jobParams.put("debitDateHolidays", new JobParameter(debitDateHolidays));
            jobParams.put("nextPaymentDate", new JobParameter(currentDate));
            jobParams.put("entityId", new JobParameter(getEntityId().toString()));
            jobParams.put("startDate", new JobParameter(new Date()));

            JobExecution updateCustomerJobExecution = jobLauncher.run(updateIgnitionCustomerJob, new JobParameters(jobParams));
            logger.debug("Update customer Job execution id is {} for entity {}", updateCustomerJobExecution.getId(), getEntityId());

        } catch (Exception exception) {
            logger.error("Exception: ", exception);
        }
    }

    @Override
    public String getTaskName() {
        return "Ignition Scheduled Batch Job Task";
    }
    private Boolean getProcessLatestOrder(JobExecutionContext context) {

        String processLatestOrder = context.getJobDetail().getJobDataMap().getString("Process Latest Order");

        if(!StringUtils.isEmpty(processLatestOrder)) {
            return Boolean.valueOf(processLatestOrder);
        }
        else{
            return Boolean.FALSE;
        }
    }

    private String loadHolidays (JobExecutionContext context) {
        StringBuilder stringBuilder = new StringBuilder();
        for(Map.Entry<String, Object> parameterDescription : context.getJobDetail().getJobDataMap().entrySet()){
            if(parameterDescription.getKey().contains(IgnitionConstants.PARAMETER_HOLIDAY)){
                stringBuilder.append(parameterDescription.getValue());
                stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }

    private String loadDebitDateUpdateHolidays(JobExecutionContext context) {
        StringBuilder builder = new StringBuilder();
        context.getJobDetail().getJobDataMap().entrySet().stream().
                filter(parameterDescription -> parameterDescription.getKey().toLowerCase().contains(IgnitionConstants.PARAMETER_DEBIT_DATE_HOLIDAY)).
                forEach(parameterDescription -> {
                            builder.append(parameterDescription.getValue());
                            builder.append(",");
                        }
//                        new IgnitionUtility().calculateDebitDateHolidays(String.valueOf(parameterDescription.getValue()), result)
                );

        return builder.toString();
    }
}
