package com.sapienter.jbilling.server.meteredUsage;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.integration.ChargeType;
import com.sapienter.jbilling.server.integration.MeteredUsageContext;
import com.sapienter.jbilling.server.integration.MeteredUsageService;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.ReservedMonthlyChargeEvent;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;
import com.sapienter.jbilling.server.usagePool.event.ReservedUpgradeEvent;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean;


/**
 * Created by tarun.rathor on 12/12/17.
 */

public class MeteredUsageIntegrationTask extends AbstractCronTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final ParameterDescription PARAM_METERED_API_URL =
      new ParameterDescription("metered_api_url", true, ParameterDescription.Type.STR);

    protected static final ParameterDescription PARAM_METERED_API_CONSUMER_KEY =
      new ParameterDescription("metered_api_consumer_key", true, ParameterDescription.Type.STR);

    protected static final ParameterDescription PARAM_METERED_API_CONSUMER_SECRET =
      new ParameterDescription("metered_api_consumer_secret", true, ParameterDescription.Type.STR, true);

    protected static final ParameterDescription PARAM_METERED_API_ASYNC_MODE =
      new ParameterDescription("metered_api_async_mode", false, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_METERED_API_CONNECT_TIMEOUT =
      new ParameterDescription("metered_api_connect_timeout (ms)", false, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_METERED_API_READ_TIMEOUT =
      new ParameterDescription("metered_api_read_timeout (ms)", false, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_METERED_API_RETRIES =
      new ParameterDescription("metered_api_retries", false, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_METERED_API_RETRY_WAIT =
      new ParameterDescription("metered_api_retry_wait (ms)", false, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_UPLOADED_ORDER_STATUS_ID =
      new ParameterDescription("order_status_uploaded", true, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_ACTIVE_ORDER_STATUS_ID =
      new ParameterDescription("order_status_active", true, ParameterDescription.Type.INT);

    protected static final ParameterDescription PARAM_UPLOAD_FAILED_ORDER_STATUS_ID =
      new ParameterDescription("order_status_upload_failed", true, ParameterDescription.Type.INT);


    protected static final ParameterDescription PARAM_LAST_MEDIATION_RUN_LOOKBACK =
      new ParameterDescription("synchronize_mediation_runs_lookback_count", false, ParameterDescription.Type.INT);

    protected static final int DEFAULT_METERED_API_ASYNC_MODE = 0;
    protected static final int DEFAULT_METERED_API_CONNECT_TIMEOUT = 3000;
    protected static final int DEFAULT_METERED_API_READ_TIMEOUT = 3000;
    protected static final int DEFAULT_METERED_API_RETRIES = 2;
    protected static final int DEFAULT_METERED_API_RETRY_WAIT = 2000;
    protected static final int DEFAULT_LAST_MEDIATION_RUN_LOOKBACK = 48;


    //initializer for pluggable params
    {
        descriptions.add(PARAM_METERED_API_URL);
        descriptions.add(PARAM_METERED_API_CONSUMER_KEY);
        descriptions.add(PARAM_METERED_API_CONSUMER_SECRET);
        descriptions.add(PARAM_METERED_API_ASYNC_MODE);
        descriptions.add(PARAM_METERED_API_CONNECT_TIMEOUT);
        descriptions.add(PARAM_METERED_API_READ_TIMEOUT);
        descriptions.add(PARAM_METERED_API_RETRIES);
        descriptions.add(PARAM_METERED_API_RETRY_WAIT);

        descriptions.add(PARAM_UPLOADED_ORDER_STATUS_ID);
        descriptions.add(PARAM_ACTIVE_ORDER_STATUS_ID);
        descriptions.add(PARAM_UPLOAD_FAILED_ORDER_STATUS_ID);
        descriptions.add(PARAM_LAST_MEDIATION_RUN_LOOKBACK);

    }

    private static final String TASK_NAME = "MeteredUsageIntegrationTask";

    public String getTaskName() {
        return TASK_NAME + "entity id:" + getEntityId() + ",:taskId" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {

        Optional<Date> lastSuccessMediationRunDate = getLastSuccessfulMediationRunDate(getEntityId(), getMaxofLastMediationRunLookback(context.getJobDetail()));
        if (!lastSuccessMediationRunDate.isPresent()) {
            logger.warn("Skipping Metered Usage Upload Job for entity id={} , no completed mediation process found",getEntityId());
            return;
        }


        MeteredUsageService meteredUsageService = Context.getBean(MeteredUsageService.BEAN_NAME);
        MeteredUsageContext meteredUsageContext = getMeteredUsageContext(context.getJobDetail());
        meteredUsageContext.setChargeType(ChargeType.USAGE);
        meteredUsageContext.setLastSuccessMediationRunDate(lastSuccessMediationRunDate.get());
        meteredUsageService.runJob(getEntityId(), MeteredUsageService.JOB_NAME, meteredUsageContext);
    }

    private MeteredUsageContext getMeteredUsageContext(JobDetail jobDetail) {

        return MeteredUsageContext.builder()
          .entityId(getEntityId())
          .endpoint(getParameterStringValue(jobDetail, PARAM_METERED_API_URL.getName()).trim())
          .consumerKey(getParameterStringValue(jobDetail, PARAM_METERED_API_CONSUMER_KEY.getName()))
          .consumerSecret(getParameterStringValue(jobDetail, PARAM_METERED_API_CONSUMER_SECRET.getName()))
          .orderStatusUploaded(getParameterIntValue(jobDetail, PARAM_UPLOADED_ORDER_STATUS_ID.getName()))
          .orderStatusUploadFailed(getParameterIntValue(jobDetail, PARAM_UPLOAD_FAILED_ORDER_STATUS_ID.getName()))
          .orderStatusActive(getParameterIntValue(jobDetail, PARAM_ACTIVE_ORDER_STATUS_ID.getName()))
          .connectTimeout(getParameterIntValue(jobDetail, PARAM_METERED_API_CONNECT_TIMEOUT.getName(), DEFAULT_METERED_API_CONNECT_TIMEOUT))
          .readTimeout(getParameterIntValue(jobDetail, PARAM_METERED_API_READ_TIMEOUT.getName(), DEFAULT_METERED_API_READ_TIMEOUT))

          .retries(getParameterIntValue(jobDetail, PARAM_METERED_API_RETRIES.getName(),
            DEFAULT_METERED_API_RETRIES))
          .retryWait(getParameterIntValue(jobDetail, PARAM_METERED_API_RETRY_WAIT.getName(),
            DEFAULT_METERED_API_RETRY_WAIT))
          .asyncMode(getParameterIntValue(jobDetail, PARAM_METERED_API_ASYNC_MODE.getName(), DEFAULT_METERED_API_ASYNC_MODE)).build();




    }

    private String getParameterStringValue(JobDetail jobDetail, String parameterName) {
        return jobDetail.getJobDataMap().getString(parameterName);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
      CustomerPlanSubscriptionEvent.class,
      ReservedMonthlyChargeEvent.class,
      ReservedUpgradeEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        if (event instanceof CustomerPlanSubscriptionEvent) {
            processCustomerPlanSubscriptionEvent((CustomerPlanSubscriptionEvent) event);

        } else if (event instanceof ReservedMonthlyChargeEvent) {
            processReservedMonthlyReportEvent((ReservedMonthlyChargeEvent) event);
        } else if (event instanceof ReservedUpgradeEvent) {
            processReservedUpgradeEvent((ReservedUpgradeEvent) event);
        }

    }

    private void processReservedUpgradeEvent(ReservedUpgradeEvent event) throws PluggableTaskException {
        logger.debug("Executing reserved Upgrade Event");
        WebServicesSessionSpringBean bean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        MeteredUsageContext meteredUsageContext = getMeteredUsageContext(getJobDetail());
        MeteredUsageService meteredUsageService = Context.getBean(MeteredUsageService.BEAN_NAME);
        meteredUsageService.sendPlanUpgradeAdjustment(event.getEntityId(), event.getExistingOrder(), event.getNewOrderId(), event.getUserId(), event.getInitialPriceReported(), event.getPendingAdjustment(), meteredUsageContext);

    }

    private void processReservedMonthlyReportEvent(ReservedMonthlyChargeEvent event) throws PluggableTaskException {

        MeteredUsageContext meteredUsageContext = getMeteredUsageContext(getJobDetail());
        meteredUsageContext.setChargeType(ChargeType.RESERVED_MONTHLY_PREPAID);
        MeteredUsageService meteredUsageService = Context.getBean(MeteredUsageService.BEAN_NAME);
        meteredUsageService.runJob(event.getEntityId(), MeteredUsageService.JOB_NAME, meteredUsageContext);
    }

    private void processCustomerPlanSubscriptionEvent(CustomerPlanSubscriptionEvent event) throws PluggableTaskException {

        WebServicesSessionSpringBean bean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

        OrderDTO orderDTO = event.getOrder();
        OrderLineDTO orderLineDTO = event.getOrderLine();

        OrderBL orderBL = new OrderBL(orderDTO);
        OrderWS orderWS = orderBL.getWS(bean.getCallerLanguageId());
        OrderLineWS orderLineWS = orderBL.getOrderLineWS(orderLineDTO.getId());

        MeteredUsageContext meteredUsageContext = getMeteredUsageContext(getJobDetail());
        meteredUsageContext.setChargeType(ChargeType.RESERVED_PURCHASE);
        MeteredUsageService meteredUsageService = Context.getBean(MeteredUsageService.BEAN_NAME);
        meteredUsageService.sendPlanPurchaseCharge(event.getEntityId(), orderLineWS, orderWS, event.getPlan().getId(), meteredUsageContext);
    }

    private int getParameterIntValue(JobDetail jobDetail, String parameterName, int defaultValue) {
        if (jobDetail.getJobDataMap().containsKey(parameterName))
            return jobDetail.getJobDataMap().getInt(parameterName);

        return defaultValue;
    }

    private int getParameterIntValue(JobDetail jobDetail, String parameterName) {
        return jobDetail.getJobDataMap().getInt(parameterName);
    }

    private int getMaxofLastMediationRunLookback(JobDetail jobDetail) {

        if (!jobDetail.getJobDataMap().containsKey(PARAM_LAST_MEDIATION_RUN_LOOKBACK.getName()))
            return DEFAULT_LAST_MEDIATION_RUN_LOOKBACK;

        int jobParameterValue = jobDetail.getJobDataMap().getInt(PARAM_LAST_MEDIATION_RUN_LOOKBACK.getName());
        return jobParameterValue > DEFAULT_LAST_MEDIATION_RUN_LOOKBACK ? jobParameterValue
          : DEFAULT_LAST_MEDIATION_RUN_LOOKBACK;

    }

    private Optional<Date> getLastSuccessfulMediationRunDate(Integer entityId, int mediationLookback) {
        MediationProcessService mediationProcessService = Context.getBean(MediationProcessService.BEAN_NAME);

        List<MediationProcess> mediationProcesses = mediationProcessService.findMediationProcessByFilters(entityId, 0, mediationLookback, "endDate", "desc", Arrays.asList());

        if (mediationProcesses.isEmpty())
            return Optional.empty();

        Optional<MediationProcess> lastSuccessfulProcess = mediationProcesses.stream()
          .filter(process -> process.getEndDate() != null)
          .findFirst();

        return lastSuccessfulProcess.isPresent() ? Optional.of(lastSuccessfulProcess.get().getEndDate()) : Optional.empty();

     }
}
