package com.sapienter.jbilling.server.integration.common.job.writers;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Stopwatch;
import com.sapienter.jbilling.appdirect.vo.UsageBean;
import com.sapienter.jbilling.server.integration.ChargeType;
import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.appdirect.client.AppDirectIntegrationClientImpl;
import com.sapienter.jbilling.server.integration.common.appdirect.client.IntegrationClient;
import com.sapienter.jbilling.server.integration.common.appdirect.client.Uploader;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.FreeSubscriptionExpiredException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionNotFoundException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionUsageNotAllowed;
import com.sapienter.jbilling.server.integration.common.job.model.Converter;
import com.sapienter.jbilling.server.integration.common.job.model.MeteredUsageStepResult;
import com.sapienter.jbilling.server.integration.common.service.HelperDataAccessService;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.util.IJobExecutionSessionBean;
import io.vavr.control.Try;

/**
 * Created by tarun.rathor on 12/18/17.
 */
public class MeteredUsageWriter implements ItemWriter<List<MeteredUsageStepResult>>, StepExecutionListener {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Getter
  @Setter
  private HelperDataAccessService dataAccessService;
  @Getter
  @Setter
  private OrderService orderService;
  @Autowired
  IJobExecutionSessionBean jobExecutionService;

  private IntegrationClient integrationClient;
  private Integer entityId;
  private int uploadedOrderStatusId;
  private int activeOrderStatusId;
  private int uploadFailedOrderStatusId;
  private int retries;
  private long retryWait;


  private AtomicLong orders;
  private AtomicLong totalSubscriptions;
  private AtomicLong failedSubscriptions;
  private Stopwatch stopwatch;
  private int partition;
  private ChargeType chargeType;

  public void write(List<? extends List<MeteredUsageStepResult>> results) throws Exception {

    totalSubscriptions.addAndGet(results.size());

    try {


      for (List<MeteredUsageStepResult> result : results) {
        Optional<UsageBean> usageBean = Converter.convert(result);

        orders.addAndGet(result.size());
        if (usageBean.isPresent()) {
          Try.of(() -> usageBean.get())
            .mapTry(Uploader.builder()
              .retries(retries)
              .retryWait(retryWait)
              .integrationClient(integrationClient)
              .build()
              .getUploadFunction())
            .onFailure(exception ->
              updateOrderStatus(result, translateToOrderStatus(exception))
            )
            .onSuccess(success ->
              markOrderReported(result, chargeType, success )
            )
            .getOrElse(false);
        }
      }
    } catch (Exception e) {

      logger.error("MeteredUsageWriter-Exception");
      logger.error(e.getMessage(), e);
    }
  }

  private void updateOrderStatus(List<MeteredUsageStepResult> stepResults, int orderStatus) {
    // Add Stats here
    for (MeteredUsageStepResult stepResult : stepResults) {
      orderService.updateCustomOrderStatus(entityId,
        stepResult.getUserId(),
        stepResult.getOrderId(),
        orderStatus);
    }
  }

  private void markOrderReported(List<MeteredUsageStepResult> stepResults, ChargeType chargeType, boolean success) {

    switch (chargeType) {
      case USAGE:
        int orderStatus = success ? uploadedOrderStatusId : activeOrderStatusId;
        updateOrderStatus(stepResults, orderStatus);
        break;
      case RESERVED_MONTHLY_PREPAID:
        if (success)
        updateOrderReservedMonthlyReportLastRunMetafield(stepResults);
        break;
    }
  }

  private void updateOrderReservedMonthlyReportLastRunMetafield(List<MeteredUsageStepResult> stepResults) {

    for (MeteredUsageStepResult stepResult : stepResults) {
      if (!stepResult.getItems().isEmpty()) {
        orderService.updateOrderMetafield(entityId,
          stepResult.getOrderId(),
          Constants.ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF, new Date());
        orderService.updateOrderMetafield(entityId, stepResult.getOrderId(), Constants.ADJUSTMENT, BigDecimal.ZERO);
      }
    }

  }

  private int translateToOrderStatus(Throwable throwable) {
    failedSubscriptions.incrementAndGet();
    if (throwable instanceof SubscriptionNotFoundException ||
      throwable instanceof FreeSubscriptionExpiredException ||
      throwable instanceof SubscriptionUsageNotAllowed) {
      // Mark these as not valid orders for sending to marketplace
      return uploadFailedOrderStatusId;
    } else {
      // Any temporary problem , keep it active for next run of metered usage
      // May be a transient issue, not enough knowledge to disregard this
      return activeOrderStatusId;
    }
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    logger.debug("StepStarted-Writer");
    orders = new AtomicLong();
    totalSubscriptions = new AtomicLong();
    failedSubscriptions = new AtomicLong();

    stopwatch = Stopwatch.createStarted();

    String entityIdString = stepExecution.getJobParameters().getString(Constants.ENTITY_ID);
    entityId = Integer.parseInt(entityIdString);

    partition = stepExecution.getExecutionContext().getInt(Constants.PARM_CURRENT_PARTITION);

    String endpointUrl = stepExecution.getJobParameters().getString(Constants.METERED_USAGE_API_ENDPOINT);
    long asyncMode = stepExecution.getJobParameters().getLong(Constants.METERED_USAGE_API_ASYNC);
    if (asyncMode == 1) {
      endpointUrl = new StringBuilder().append(endpointUrl).append("?async=true").toString();
    }
    String consumerKey = stepExecution.getJobParameters().getString(Constants.METERED_USAGE_API_CONSUMER_KEY);
    String consumerSecret = stepExecution.getJobParameters().getString(Constants.METERED_USAGE_API_CONSUMER_SECRET);
    int connectTimeout = stepExecution.getJobParameters().getLong(Constants.METERED_USAGE_API_CONNECT_TIMEOUT).intValue();
    int readTimeout = stepExecution.getJobParameters().getLong(Constants.METERED_USAGE_API_READ_TIMEOUT).intValue();

    integrationClient = new AppDirectIntegrationClientImpl(endpointUrl, consumerKey, consumerSecret, connectTimeout, readTimeout);

    retries = stepExecution.getJobParameters().getLong(Constants.METERED_USAGE_API_RETRIES).intValue();
    retryWait = stepExecution.getJobParameters().getLong(Constants.METERED_USAGE_API_RETRY_WAIT);

    uploadedOrderStatusId = stepExecution.getJobParameters().getLong(Constants.ORDER_UPLOADED_STATUS_ID).intValue();
    activeOrderStatusId = stepExecution.getJobParameters().getLong(Constants.ORDER_ACTIVE_STATUS_ID).intValue();
    uploadFailedOrderStatusId = stepExecution.getJobParameters().getLong(Constants.ORDER_UPLOAD_FAILED_STATUS_ID).intValue();
    String type = stepExecution.getJobParameters().getString(Constants.CHARGE_TYPE);
    chargeType = ChargeType.valueOf(type);
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    stopwatch.stop();
    logger.debug("StepCompleted-Writer: time={}, subscriptions={}, orders={}", stopwatch, totalSubscriptions.get(), orders.get());
    String totalSubscriptionsHeader = String.format("Partition-#%d - Total Subscriptions", partition);
    String failedSubscriptionsHeader = String.format("Partition-#%d - Failed Subscriptions", partition);
    String ordersHeader = String.format("Partition-#%d - Orders", partition);

    jobExecutionService
      .addLine(stepExecution.getJobExecution().getId(), IJobExecutionSessionBean.LINE_TYPE_HEADER, totalSubscriptionsHeader, Long.toString(totalSubscriptions.get()));
    jobExecutionService
      .addLine(stepExecution.getJobExecution().getId(), IJobExecutionSessionBean.LINE_TYPE_HEADER, failedSubscriptionsHeader, Long.toString(failedSubscriptions.get()));
    jobExecutionService
      .addLine(stepExecution.getJobExecution().getId(), IJobExecutionSessionBean.LINE_TYPE_HEADER, ordersHeader, Long.toString(orders.get()));
    return ExitStatus.COMPLETED;
  }
}
