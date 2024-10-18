package com.sapienter.jbilling.server.integration.common.job.processors;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Stopwatch;
import com.sapienter.jbilling.server.integration.ChargeType;
import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.job.model.MeteredUsageItem;
import com.sapienter.jbilling.server.integration.common.job.model.MeteredUsageStepResult;
import com.sapienter.jbilling.server.integration.common.pricing.MeteredUsageCustomUnitBuilder;
import com.sapienter.jbilling.server.integration.common.pricing.MeteredUsageDescriptionBuilder;
import com.sapienter.jbilling.server.integration.common.service.HelperDataAccessService;
import com.sapienter.jbilling.server.integration.common.service.vo.CompanyInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.CustomerInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineInfo;
import com.sapienter.jbilling.server.integration.common.utility.UsageItemHelper;

public class MeteredUsageProcessor implements ItemProcessor<Integer, List<MeteredUsageStepResult>>, StepExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    @Setter
    private HelperDataAccessService dataAccessService;

    @Autowired
    private UsageItemHelper usageItemHelper;

    private int entityId;
    private int activeOrderStatusId;

    // Stats collection variable
    private AtomicLong orders;
    private AtomicLong orderLines;
    private AtomicLong customers;
    private Stopwatch stopwatch;
    private ChargeType chargeType = ChargeType.USAGE;
    private Date lastMediationRunDate = new Date();

    @Override
    public List<MeteredUsageStepResult> process(Integer userId) throws Exception {
        customers.incrementAndGet();
        logger.debug("customerId={}", userId);

        List<MeteredUsageStepResult> meteredUsageStepResultList = new ArrayList<>();

        try {
            Optional<CompanyInfo> companyInfo = dataAccessService.getCompanyInfo(entityId);
            if (!companyInfo.isPresent()) {
                logger.warn("Company={} not found. Skip processing for user={}", entityId, userId);
                return meteredUsageStepResultList;
            }

            Optional<CustomerInfo> customerInfo = dataAccessService.getCustomerInfo(entityId, userId, Constants.CUSTOMER_EXTERNAL_ACCOUNT_IDENTIFIER_MF);

            if (!customerInfo.isPresent()) {
                logger.warn("User={} not found.Skip processing", userId);
                return meteredUsageStepResultList;
            }
            meteredUsageStepResultList = getCustomerUsageItems(companyInfo.get(), customerInfo.get());
            for (MeteredUsageStepResult stepResult : meteredUsageStepResultList) {
                process(stepResult);
            }
        } catch (Exception ex) {
            logger.error("MeteredUsageProcessor-Exception");
            logger.error(ex.getMessage(), ex);
        }
        return meteredUsageStepResultList;
    }

    private void process(MeteredUsageStepResult stepResult) {
        for (MeteredUsageItem usageItem : stepResult.getItems()) {
            String description = buildDescription(stepResult, usageItem);
            usageItem.setFormattedDescription(description);

            String customUnit = buildCustomUnit(stepResult, usageItem);
            usageItem.setCustomUnit(customUnit);
        }
    }

    private List<MeteredUsageStepResult> getCustomerUsageItems(CompanyInfo companyInfo, CustomerInfo customerInfo) {
        logger.debug(" MeteredUsageProcessor: userId={}, ChargeType is {}", customerInfo.getUserId(), chargeType.name());
        List<MeteredUsageStepResult> results = new ArrayList<>();

        List<OrderInfo> orderInfoList = getOrdersByChargeType(companyInfo.getCompanyId(), customerInfo.getUserId(), activeOrderStatusId, chargeType);

        long items = 0;
        for (OrderInfo orderInfo : orderInfoList) {
            orders.incrementAndGet();
            Integer orderId = orderInfo.getOrderId();

            MeteredUsageStepResult stepResult = new MeteredUsageStepResult();
            stepResult.setEntityId(entityId);
            stepResult.setUserId(customerInfo.getUserId());
            stepResult.setAccountIdenfitier(customerInfo.getExternalAccountIdentifier());
            // Note: As a requirement use company level language id and currency
            stepResult.setCurrencyId(companyInfo.getCurrencyId());
            int languageId = companyInfo.getLanguageId();
            stepResult.setLanguageId(languageId);
            stepResult.setOrderId(orderId);
            stepResult.setItems(new ArrayList<>());

            List<OrderLineInfo> orderLinesInfo = dataAccessService.getOrderLines(entityId, orderId);

            List<MeteredUsageItem> usageItems;

            for (OrderLineInfo orderLineInfo : orderLinesInfo) {
                usageItems = getMeteredUsageItemByChargeType(entityId, orderInfo, orderLineInfo, languageId, chargeType);
                if (usageItems.isEmpty()) {
                    continue;
                }
                stepResult.getItems().addAll(usageItems);
            }
            if (!stepResult.getItems().isEmpty()) {
                results.add(stepResult);
            }
        }
        logger.debug(" MeteredUsageProcessor: userId={}, Orders={}, OrderLines={}, ChargeType={}", customerInfo.getUserId(), results.size(), items, chargeType.name());
        return results;
    }

    private String buildCustomUnit(MeteredUsageStepResult stepResult, MeteredUsageItem meteredUsageItem) {
        return MeteredUsageCustomUnitBuilder
                .valueOfIgnoreCase(meteredUsageItem.getPriceModelType())
                .getCustomUnit(stepResult.getLanguageId(), meteredUsageItem.getProductCode(), meteredUsageItem.getProductBillingUnit(), meteredUsageItem.getPriceModelAttributes());
    }

    private String buildDescription(MeteredUsageStepResult stepResult, MeteredUsageItem meteredUsageItem) {
        return MeteredUsageDescriptionBuilder
                .valueOfIgnoreCase(meteredUsageItem.getPriceModelType())
                .getDescription(stepResult.getLanguageId(), meteredUsageItem.getProductDescription(), meteredUsageItem.getProductBillingUnit(), meteredUsageItem.getPriceModelAttributes());
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.debug("StepStarted-Processor");
        String entityIdStr = stepExecution.getJobParameters().getString("entityId");
        entityId = Integer.parseInt(entityIdStr);
        activeOrderStatusId = stepExecution.getJobParameters().getLong(Constants.ORDER_ACTIVE_STATUS_ID).intValue();
        String type = stepExecution.getJobParameters().getString(Constants.CHARGE_TYPE);
        chargeType = ChargeType.valueOf(type);

        if (chargeType.equals(ChargeType.USAGE)) {
            lastMediationRunDate = (Date) stepExecution.getJobParameters().getDate(Constants.LAST_SUCCESS_MEDIATION_RUN_DATE);
        }

        stopwatch = Stopwatch.createStarted();
        orders = new AtomicLong();
        customers = new AtomicLong();
        orderLines = new AtomicLong();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stopwatch.stop();
        logger.debug("StepCompleted-Processor:time={},Customers={},Orders={},OrderLines={}", stopwatch, customers.get(), orders.get(), orderLines.get());
        return ExitStatus.COMPLETED;
    }

    private List<OrderInfo> getOrdersByChargeType(int entityId, int userId, int activeOrderStatusId, ChargeType chargeType) {
        List<OrderInfo> ordersInfo = new ArrayList<>();
        switch (chargeType) {
            case USAGE:
                ordersInfo = dataAccessService.getMediatedOrdersByStatusAndUser(entityId, userId, activeOrderStatusId, lastMediationRunDate);
                break;
            case RESERVED_MONTHLY_PREPAID:
                ordersInfo = dataAccessService.getReservedPlanOrdersByCustomer(entityId, userId);
                break;
        }
        return ordersInfo;
    }

    private List<MeteredUsageItem> getMeteredUsageItemByChargeType(Integer entityId, OrderInfo orderInfo, OrderLineInfo orderLineInfo, Integer languageId, ChargeType chargeType) {
        List<MeteredUsageItem> usageItems = new ArrayList<>();
        switch (chargeType) {
            case USAGE:
                usageItems = usageItemHelper.getUsageItemsForOrderLine(entityId, orderLineInfo, languageId);
                break;
            case RESERVED_MONTHLY_PREPAID:

                usageItems = usageItemHelper.getUsageItemsForReservedMonthlyPlan(entityId, orderInfo, orderLineInfo);
                break;
        }
        return usageItems;
    }
}
