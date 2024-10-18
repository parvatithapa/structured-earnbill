package com.sapienter.jbilling.server.integration;


import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.sapienter.jbilling.appdirect.vo.UsageBean;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.integration.common.appdirect.client.AppDirectIntegrationClientImpl;
import com.sapienter.jbilling.server.integration.common.appdirect.client.IntegrationClient;
import com.sapienter.jbilling.server.integration.common.appdirect.client.Uploader;
import com.sapienter.jbilling.server.integration.common.job.model.Converter;
import com.sapienter.jbilling.server.integration.common.job.model.MeteredUsageStepResult;
import com.sapienter.jbilling.server.integration.common.service.vo.ReservedPlanInfo;
import com.sapienter.jbilling.server.integration.common.utility.ReservedInstanceHelper;
import com.sapienter.jbilling.server.integration.common.utility.UsageItemHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.order.OrderWS;
import io.vavr.control.Try;

public class MeteredUsageServiceImpl implements MeteredUsageService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ApplicationContext applicationContext;

    @Autowired
    private ReservedInstanceHelper reservedInstanceHelper;

    @Autowired
    private UsageItemHelper usageItemHelper;

    @Getter @Setter
    private OrderService orderService;

    @Override
    public void runJob(int entityId, String jobName, MeteredUsageContext meteredUsageContext) {

        runSpringBatchJob(entityId, jobName, meteredUsageContext);
    }

    private Job getJob(String name) {
        return (Job) applicationContext.getBean(name);
    }

    private void runSpringBatchJob(int entityId, String jobName, MeteredUsageContext meteredUsageContext) {

        Job meteredUsageJob = getJob(jobName);
        if (meteredUsageJob != null) {
            JobLauncher jobLauncher = (JobLauncher) applicationContext.getBean(Constants.BATCH_ASYNC_JOB_LAUNCHER);

            Map<String, JobParameter> parametersMap = new HashMap<>();
            parametersMap.put("datetime", new JobParameter(new Date()));
            parametersMap.put(Constants.ENTITY_ID, new JobParameter(Integer.toString(entityId)));

            parametersMap.put(Constants.ORDER_UPLOADED_STATUS_ID,
                    new JobParameter((long) meteredUsageContext.getOrderStatusUploaded()));
            parametersMap.put(Constants.ORDER_UPLOAD_FAILED_STATUS_ID,
                    new JobParameter((long) meteredUsageContext.getOrderStatusUploadFailed()));
            parametersMap.put(Constants.ORDER_ACTIVE_STATUS_ID,
                    new JobParameter((long) meteredUsageContext.getOrderStatusActive()));

            // Populate API parameters and configuration
            parametersMap.put(Constants.METERED_USAGE_API_ENDPOINT,
                    new JobParameter(meteredUsageContext.getEndpoint()));
            parametersMap.put(Constants.METERED_USAGE_API_CONSUMER_KEY,
                    new JobParameter(meteredUsageContext.getConsumerKey()));
            parametersMap.put(Constants.METERED_USAGE_API_CONSUMER_SECRET,
                    new JobParameter(meteredUsageContext.getConsumerSecret()));
            parametersMap.put(Constants.METERED_USAGE_API_CONNECT_TIMEOUT,
                    new JobParameter(meteredUsageContext.getConnectTimeout()));
            parametersMap.put(Constants.METERED_USAGE_API_READ_TIMEOUT,
                    new JobParameter(meteredUsageContext.getReadTimeout()));
            parametersMap.put(Constants.METERED_USAGE_API_RETRIES,
                    new JobParameter(meteredUsageContext.getRetries()));
            parametersMap.put(Constants.METERED_USAGE_API_RETRY_WAIT,
                    new JobParameter(meteredUsageContext.getRetryWait()));
            parametersMap.put(Constants.METERED_USAGE_API_ASYNC,
                    new JobParameter((long) meteredUsageContext.getAsyncMode()));
            parametersMap.put(Constants.CHARGE_TYPE,
                    new JobParameter(meteredUsageContext.getChargeType().name()));
            if (meteredUsageContext.getChargeType().equals(ChargeType.USAGE))
                parametersMap.put(Constants.LAST_SUCCESS_MEDIATION_RUN_DATE,
                        new JobParameter(meteredUsageContext.getLastSuccessMediationRunDate()));

            JobParameters jobParameters = new JobParameters(parametersMap);

            try {
                jobLauncher.run(meteredUsageJob, jobParameters);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void sendPlanPurchaseCharge(int entityId, OrderLineWS orderLineWS, OrderWS orderWS, Integer planId,
                                       MeteredUsageContext meteredUsageContext) {

        String endpointUrl = meteredUsageContext.getEndpoint();
        String consumerKey = meteredUsageContext.getConsumerKey();
        String consumerSecret = meteredUsageContext.getConsumerSecret();
        int connectTimeout = (int) meteredUsageContext.getConnectTimeout();
        int readTimeout = (int) meteredUsageContext.getReadTimeout();
        int retries = (int) meteredUsageContext.getRetries();
        long retryWait = meteredUsageContext.getRetryWait();
        if (reservedInstanceHelper.isReservedInstancePlan(entityId, planId)) {
            boolean foundMetaFields = validateMetafields(orderWS, Constants.ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF) && validateMetafields(orderWS, Constants.ADJUSTMENT) && validateMetafields(orderWS, Constants.UPGRADED_TO);
            if (foundMetaFields) {
                logger.debug("Reserved Instance plan Purchased : Plan id is {}, order line description is  {}", planId, orderLineWS.getDescription());
                try {
                    int userId = orderWS.getUserId();

                    ReservedPlanInfo reservedPlanInfo = reservedInstanceHelper.getReservedPlanInfo(entityId, planId, orderLineWS.getPriceAsDecimal());
                    BigDecimal quantityToBeCharged = orderLineWS.getQuantityAsDecimal();
                    if (reservedPlanInfo.getPaymentOption().equalsIgnoreCase(Constants.PLAN_PAYMENT_OPTION_MONTHLY)) {

                        quantityToBeCharged = reservedInstanceHelper.prorateMonthlyReservedPurchaseQuantity(orderLineWS.getQuantityAsDecimal(), orderWS.getActiveSince());
                    }

                    if (BigDecimal.ZERO.equals(quantityToBeCharged)) {
                        logger.warn("Order not active for current month.");
                        logger.warn("Quantity ZERO, not need to report the purchase order.");
                        return;
                    }

                    List<MeteredUsageStepResult> results = usageItemHelper.getUsageStepResultForReservedPlanPurchase(entityId, userId, quantityToBeCharged, reservedPlanInfo);

                    Optional<UsageBean> usageBean = Converter.convert(results);


                    if (usageBean.isPresent()) {
                        IntegrationClient integrationClient = new AppDirectIntegrationClientImpl(endpointUrl, consumerKey, consumerSecret, connectTimeout, readTimeout);

                        try {
                            Try.of(() -> usageBean.get())
                                    .mapTry(Uploader.builder()
                                            .retries(retries)
                                            .retryWait(retryWait)
                                            .integrationClient(integrationClient)
                                            .build()
                                            .getUploadFunction())
                                    .onSuccess(success -> updateLastReportMonthlyDate(entityId, orderWS.getId(), success)

                                    )
                                    .getOrElse(false);

                        } catch (Exception e) {

                            logger.error("MeteredUsageWriter-Exception");
                            logger.error(e.getLocalizedMessage(), e);
                        }

                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.error("Metafield missing");
                throw new SessionInternalError("Metafield missing");
            }
        }
    }

    @Override
    public void sendPlanUpgradeAdjustment(int entityId, OrderWS initialOrder, Integer newOrderId, Integer userId, BigDecimal initialPriceReported, BigDecimal pendingAdjustment, MeteredUsageContext meteredUsageContext) {
        String endpointUrl = meteredUsageContext.getEndpoint();
        String consumerKey = meteredUsageContext.getConsumerKey();
        String consumerSecret = meteredUsageContext.getConsumerSecret();
        int connectTimeout = (int) meteredUsageContext.getConnectTimeout();
        int readTimeout = (int) meteredUsageContext.getReadTimeout();
        int retries = (int) meteredUsageContext.getRetries();
        long retryWait = meteredUsageContext.getRetryWait();
        BigDecimal adjustment = reservedInstanceHelper.getAdjustment(initialPriceReported, initialOrder.getActiveUntil());
        try {
            List<MeteredUsageStepResult> results = usageItemHelper.getAdjustmentForPlanUpgrade(entityId, userId, adjustment, initialOrder.getId(), newOrderId);

            Optional<UsageBean> usageBean = Converter.convert(results);

            if (usageBean.isPresent()) {
                IntegrationClient integrationClient = new AppDirectIntegrationClientImpl(endpointUrl, consumerKey, consumerSecret, connectTimeout, readTimeout);

                Try.of(() -> usageBean.get())
                        .mapTry(Uploader.builder()
                                .retries(retries)
                                .retryWait(retryWait)
                                .integrationClient(integrationClient)
                                .build()
                                .getUploadFunction())
                        .onFailure(x -> orderMetafieldUpdate(entityId, newOrderId, initialOrder.getId(), adjustment.add(pendingAdjustment))
                        ).onSuccess(x -> orderMetafieldUpdate(entityId, newOrderId, initialOrder.getId(), pendingAdjustment)

                )
                        .getOrElse(false);
            }
        } catch (Exception e) {

            logger.error("MeteredUsageWriter-Exception");
            logger.error(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)  {
        this.applicationContext = applicationContext;
    }

    private void orderMetafieldUpdate(int entityId, Integer newOrderId, Integer initialOrderId, BigDecimal adjustment){
        orderService.updateOrderMetafield(entityId, newOrderId, Constants.ADJUSTMENT, adjustment);
        orderService.updateOrderMetafield(entityId, initialOrderId, Constants.ADJUSTMENT, BigDecimal.ZERO);
    }

    private boolean validateMetafields(OrderWS order, String metafieldName){
        MetaFieldValueWS[] metafields = order.getMetaFields();
        boolean found = false;
        for(MetaFieldValueWS metafield : metafields){
            if(metafield.getFieldName().equals(metafieldName)){
                found = true;
                break;
            }

        }
        return found;
    }

    private void updateLastReportMonthlyDate(Integer entityId, Integer orderId, boolean success) {

        if (success) {
            orderService.updateOrderMetafield(entityId, orderId, Constants.ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF, new Date());
        }
    }
}
