package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.event.BulkDownloadEvent;
import com.sapienter.jbilling.server.item.event.BulkUploadEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Taimoor Choudhary on 1/11/18.
 */
public class DTAGBulkLoaderTask  extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Subscribed Events
    private static final Class<Event> events[] = new Class[] {
            BulkUploadEvent.class,
            BulkDownloadEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        if(event instanceof BulkUploadEvent){

            BulkUploadEvent bulkUploadEvent = (BulkUploadEvent) event;

            if(bulkUploadEvent.getEventType().equals(BulkUploadEvent.UploadType.DEFAULT_PRODUCT)){

                Long executionId = uploadDefaultPrices(bulkUploadEvent.getSourceFilePath(), bulkUploadEvent.getErrorFilePath(), bulkUploadEvent.getCallerId(), bulkUploadEvent.getEntityId());

                // Save Execution Id
                bulkUploadEvent.setExecutionId(executionId);

            }else if(bulkUploadEvent.getEventType().equals(BulkUploadEvent.UploadType.ACCOUNT_LEVEL_PRICE)){

                Long executionId = uploadAccountTypePrices(bulkUploadEvent.getSourceFilePath(), bulkUploadEvent.getErrorFilePath(), bulkUploadEvent.getCallerId(), bulkUploadEvent.getEntityId());

                // Save Execution Id
                bulkUploadEvent.setExecutionId(executionId);

            }else if(bulkUploadEvent.getEventType().equals(BulkUploadEvent.UploadType.CUSTOMER_PRICE)){

                Long executionId = uploadCustomerLevelPrices(bulkUploadEvent.getSourceFilePath(), bulkUploadEvent.getErrorFilePath(), bulkUploadEvent.getCallerId(), bulkUploadEvent.getEntityId());

                // Save Execution Id
                bulkUploadEvent.setExecutionId(executionId);

            }else if(bulkUploadEvent.getEventType().equals(BulkUploadEvent.UploadType.PLAN_PRICE)){

                Long executionId = uploadPlanPrices(bulkUploadEvent.getSourceFilePath(), bulkUploadEvent.getErrorFilePath(), bulkUploadEvent.getCallerId(), bulkUploadEvent.getEntityId());

                // Save Execution Id
                bulkUploadEvent.setExecutionId(executionId);

            }
        }else if(event instanceof BulkDownloadEvent){
            BulkDownloadEvent bulkDownloadEvent = (BulkDownloadEvent) event;

            if(bulkDownloadEvent.getEventType().equals(BulkDownloadEvent.DownloadType.DEFAULT_PRODUCT)) {

                Long executionId = downloadDefaultPrices(bulkDownloadEvent.getSourceFilePath(), bulkDownloadEvent.getErrorFilePath(),
                        bulkDownloadEvent.getCallerId(), bulkDownloadEvent.getEntityId(), bulkDownloadEvent.getIdentificationCode());

                // Save Execution Id
                bulkDownloadEvent.setExecutionId(executionId);
            }
            else if(bulkDownloadEvent.getEventType().equals(BulkDownloadEvent.DownloadType.ACCOUNT_LEVEL_PRICE)) {

                Long executionId = downloadAccountLevelPrices(bulkDownloadEvent.getSourceFilePath(), bulkDownloadEvent.getErrorFilePath(),
                        bulkDownloadEvent.getCallerId(), bulkDownloadEvent.getEntityId(), bulkDownloadEvent.getIdentificationCode());

                // Save Execution Id
                bulkDownloadEvent.setExecutionId(executionId);
            }else if(bulkDownloadEvent.getEventType().equals(BulkDownloadEvent.DownloadType.CUSTOMER_PRICE)) {

                Long executionId = downloadCustomerPrices(bulkDownloadEvent.getSourceFilePath(), bulkDownloadEvent.getErrorFilePath(),
                        bulkDownloadEvent.getCallerId(), bulkDownloadEvent.getEntityId(), bulkDownloadEvent.getIdentificationCode());

                // Save Execution Id
                bulkDownloadEvent.setExecutionId(executionId);
            }else if(bulkDownloadEvent.getEventType().equals(BulkDownloadEvent.DownloadType.PLANS)) {

                Long executionId = downloadPlans(bulkDownloadEvent.getSourceFilePath(), bulkDownloadEvent.getErrorFilePath(),
                        bulkDownloadEvent.getCallerId(), bulkDownloadEvent.getEntityId(), bulkDownloadEvent.getIdentificationCode());

                // Save Execution Id
                bulkDownloadEvent.setExecutionId(executionId);
            }
        }
    }

    private Long uploadDefaultPrices(String sourceFilePath, String errorFilePath, Integer callerId, Integer entityId) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productLoadJob = Context.getBean(Context.Name.BATCH_PRODUCT_DEFAULT_PRICES_LOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_INPUT_FILE, new JobParameter(sourceFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productLoadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start default price job", e);
            throw new PluggableTaskException("Unable to start default price job", e);
        }
    }

    private Long uploadAccountTypePrices( String sourceFilePath, String errorFilePath, Integer callerId, Integer entityId) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productLoadJob = Context.getBean(Context.Name.BATCH_PRODUCT_ACCOUNT_PRICES_LOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_INPUT_FILE, new JobParameter(sourceFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productLoadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start account price upload job", e);
            throw new PluggableTaskException("Unable to start account price upload job", e);
        }
    }

    private Long uploadCustomerLevelPrices( String sourceFilePath, String errorFilePath, Integer callerId, Integer entityId) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productLoadJob = Context.getBean(Context.Name.BATCH_PRODUCT_CUSTOMER_LEVEL_PRICES_LOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_INPUT_FILE, new JobParameter(sourceFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productLoadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start customer level price upload job", e);
            throw new PluggableTaskException("Unable to start customer level price upload job", e);
        }
    }

    private Long uploadPlanPrices( String sourceFilePath, String errorFilePath, Integer callerId, Integer entityId) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productLoadJob = Context.getBean(Context.Name.BATCH_PLAN_PRICES_LOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(PlanImportConstants.JOB_PARAM_INPUT_FILE, new JobParameter(sourceFilePath));
        jobParams.put(PlanImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(PlanImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(PlanImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(PlanImportConstants.JOB_PARAM_START, new JobParameter(new Date()));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productLoadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start Plan Price Upload job", e);
            throw new PluggableTaskException("Unable to start Plan Price Upload job", e);
        }
    }

    private Long downloadDefaultPrices(String outputFilePath, String errorFilePath, Integer callerId, Integer entityId,
                                       String productCode) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productDownloadJob = Context.getBean(Context.Name.BATCH_PRODUCT_DEFAULT_PRICES_DOWNLOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_OUTPUT_FILE, new JobParameter(outputFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));
        jobParams.put(ProductImportConstants.JOB_PARAM_IDENTIFICATION_CODE, new JobParameter(productCode));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productDownloadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start Individual Default Price Download job", e);
            throw new PluggableTaskException("Unable to start Individual Default Price Download job", e);
        }
    }

    private Long downloadAccountLevelPrices(String outputFilePath, String errorFilePath, Integer callerId, Integer entityId, String accountId) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productDownloadJob = Context.getBean(Context.Name.BATCH_PRODUCT_ACCOUNT_PRICES_DOWNLOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_OUTPUT_FILE, new JobParameter(outputFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));
        jobParams.put(ProductImportConstants.JOB_PARAM_IDENTIFICATION_CODE, new JobParameter(accountId));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productDownloadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start Default Prices Download job", e);
            throw new PluggableTaskException("Unable to start Default Prices Download job", e);
        }
    }

    private Long downloadCustomerPrices(String outputFilePath, String errorFilePath, Integer callerId, Integer entityId, String customerIdentifier) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productDownloadJob = Context.getBean(Context.Name.BATCH_PRODUCT_CUSTOMER_PRICES_DOWNLOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_OUTPUT_FILE, new JobParameter(outputFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));
        jobParams.put(ProductImportConstants.JOB_PARAM_IDENTIFICATION_CODE, new JobParameter(customerIdentifier));

        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productDownloadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start Default Prices Download job", e);
            throw new PluggableTaskException("Unable to start Default Prices Download job", e);
        }
    }

    private Long downloadPlans(String outputFilePath, String errorFilePath, Integer callerId, Integer entityId, String planNumber) throws PluggableTaskException {

        JobLauncher asyncJobLauncher = Context
                .getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job productDownloadJob = Context.getBean(Context.Name.BATCH_PRODUCT_PLANS_DOWNLOAD_JOB);

        // Job Parameters for Spring Batch
        Map jobParams = new HashMap();

        jobParams.put(ProductImportConstants.JOB_PARAM_OUTPUT_FILE, new JobParameter(outputFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_ERROR_FILE, new JobParameter(errorFilePath));
        jobParams.put(ProductImportConstants.JOB_PARAM_USER_ID, new JobParameter(new Long(callerId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_ENTITY_ID, new JobParameter(new Long(entityId)));
        jobParams.put(ProductImportConstants.JOB_PARAM_START, new JobParameter(new Date()));
        jobParams.put(ProductImportConstants.JOB_PARAM_IDENTIFICATION_CODE, new JobParameter(planNumber));


        try {
            // execute the job asynchronously
            JobExecution execution = asyncJobLauncher.run(productDownloadJob, new JobParameters(jobParams));
            Long executionId = execution.getId();

            return executionId;
        } catch (Exception e) {
            logger.error("Unable to start Default Prices Download job", e);
            throw new PluggableTaskException("Unable to start Default Prices Download job", e);
        }
    }
}
