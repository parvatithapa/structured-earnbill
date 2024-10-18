package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.mediation.JMRPostProcessorTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineItemizedUsageDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;

public class SpcJMRPostProcessorTask extends JMRPostProcessorTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription PARAM_CREDIT_POOL_TABLE_NAME =
            new ParameterDescription("Credit pool table name", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE =
        new ParameterDescription("Reduce pricing_field in JMR table", false, ParameterDescription.Type.BOOLEAN, "true");

    private static final String COLON = ":";
    private static final String COMMA = ",";
    public SpcJMRPostProcessorTask() {
        super();
        descriptions.add(PARAM_CREDIT_POOL_TABLE_NAME);
        descriptions.add(PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE);
    }

    @Override
    public void afterProcessing(JbillingMediationRecord jmr, OrderDTO updatedOrder, MediationEventResult eventResult) {
        boolean shouldReducePricingFields = getParameter(PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE.getName(), Boolean.parseBoolean(PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE.getDefaultValue()));
        try {
            OrderLineDTO updatedMediatedLine = updatedOrder.getLineById(eventResult.getOrderLinedId());
            if(null == updatedMediatedLine) {
                logger.debug("mediated line {} not found on order {}", jmr.getOrderLineId(), updatedOrder.getId());
                return;
            }
            // applying rounding and scale to mediated line.
            BigDecimal orderLineAmount = updatedMediatedLine.getAmount();
            if ((updatedMediatedLine.hasOrderLineUsagePools() &&
                    BigDecimal.ZERO.compareTo(orderLineAmount) == 0) ||
                    BigDecimal.ZERO.compareTo(eventResult.getAmountForChange()) == 0) {
                jmr.setTaxAmount(BigDecimal.ZERO);
                jmr.setRatedPriceWithTax(BigDecimal.ZERO);
                return;
            }
            BigDecimal chargeOfJMR = eventResult.getAmountForChange();
            orderLineAmount = orderLineAmount.subtract(chargeOfJMR);
            UserDTO user = new UserDAS().find(jmr.getUserId());
            String taxTableName = getParameter(PARAM_TAX_TABLE_NAME.getName(), "");
            if(StringUtils.isEmpty(taxTableName)) {
                logger.debug("tax calculation skipping, since tax table name param not configured for plugin");
                return;
            }
            Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();
            String taxDateFormat = getParameter(PARAM_TAX_DATE_FORMAT.getName(), PARAM_TAX_DATE_FORMAT.getDefaultValue());
            BigDecimal itemTaxRate = new ItemBL(jmr.getItemId()).getTaxRate(nextInvoiceDate, taxTableName, taxDateFormat);
            logger.debug("tax rate {} found for user {} for item {} for date {}", itemTaxRate, jmr.getUserId(), jmr.getItemId(), nextInvoiceDate);
            BigDecimal unRoundedTaxAmount = chargeOfJMR.multiply(itemTaxRate.divide(new BigDecimal(100)));
            logger.debug("unRoundedTaxAmount {} for jmr {}", unRoundedTaxAmount, jmr.getRecordKey());
            BigDecimal inclusiveRoundedJMRAmount = chargeOfJMR.add(unRoundedTaxAmount, MathContext.DECIMAL128)
                    .setScale(scale(), roundingMode());
            logger.debug("inclusiveRoundedJMRAmount {} for jmr {}", inclusiveRoundedJMRAmount, jmr.getRecordKey());
            jmr.setRatedPriceWithTax(inclusiveRoundedJMRAmount);
            BigDecimal excludingAmountRate = itemTaxRate.divide(new BigDecimal("100")).add(BigDecimal.ONE);
            logger.debug("excludingAmountRate {} for jmr {}", excludingAmountRate, jmr.getRecordKey());
            BigDecimal unRoundedExcludingJMRCharge = inclusiveRoundedJMRAmount.divide(excludingAmountRate, MathContext.DECIMAL128);
            logger.debug("unRoundedExcludingJMRCharge {} for jmr {}", unRoundedExcludingJMRCharge, jmr.getRecordKey());
            BigDecimal taxUnRoundedAmount = inclusiveRoundedJMRAmount.subtract(unRoundedExcludingJMRCharge, MathContext.DECIMAL128);
            logger.debug("taxUnRoundedAmount {} for jmr {}", taxUnRoundedAmount, jmr.getRecordKey());
            jmr.setTaxAmount(taxUnRoundedAmount);
            eventResult.setAmountForChange(unRoundedExcludingJMRCharge);
            orderLineAmount = orderLineAmount.add(unRoundedExcludingJMRCharge, MathContext.DECIMAL128);
            updatedMediatedLine.setAmount(orderLineAmount);
            updatedMediatedLine.setPrice(orderLineAmount.divide(updatedMediatedLine.getQuantity(), MathContext.DECIMAL128));
            // create or add OrderLineItemizedUsageDTO details on order line.
            String tariffCode = jmr.getPricingFieldValueByName(SPCConstants.TARIFF_CODE);
            if(StringUtils.isEmpty(tariffCode)) {
                logger.debug("no tariff code found for jmr {}", jmr.getRecordKey());
                return;
            }

            String planOrderId = jmr.getPricingFieldValueByName(SPCConstants.PURCHASE_ORDER_ID);
            String callIdentifier = updatedMediatedLine.getCallIdentifier();
            if(StringUtils.isEmpty(planOrderId)) {
                logger.debug("plan order not found for asset identifier {}", callIdentifier);
                return;
            }
            OrderDTO planOrder = new OrderDAS().find(Integer.parseInt(planOrderId));
            PlanDTO plan = planOrder.getPlanFromOrder();
            if(null == plan) {
                logger.debug("plan not found for asset identifier {}", callIdentifier);
                return;
            }
            String tableName;
            try {
                tableName = getMandatoryStringParameter(PARAM_CREDIT_POOL_TABLE_NAME.getName());
            } catch (PluggableTaskException pluggableTaskException) {
                shouldReducePricingFields = false;
                throw new SessionInternalError("Credit pool table name param not configured in plugin!");
            }
            // add or update usage details on line.
            updatedMediatedLine.addOrderLineItemizedUsage(tariffCode, unRoundedExcludingJMRCharge, jmr.getQuantity());
            SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
            List<SpcCreditPoolInfo> creditPools = spcHelperService.getCreditPoolsForPlan(plan.getId(), tableName);
            logger.debug("credit pools {} found for plan {}", creditPools, plan.getId());
            if(CollectionUtils.isEmpty(creditPools)) {
                return;
            }
            SpcCreditPoolInfo creditPoolToUse = null;
            for(SpcCreditPoolInfo creditPool : creditPools) {
                if(creditPool.getTariffCodeList().contains(tariffCode)) {
                    creditPoolToUse = creditPool;
                    break;
                }
            }
            List<String> callIdentifiers = new ArrayList<>();
            callIdentifiers.add(callIdentifier);
            if (null != creditPoolToUse) {
                logger.debug("callIdentifiers {} for jmr {}", callIdentifiers, jmr.getRecordKey());
                logger.debug("tariffcodes {} for jmr {}", creditPoolToUse.getTariffCodeList(), jmr.getRecordKey());
                logger.debug("NextInvoiceDate() {} for jmr {}", user.getCustomer().getNextInvoiceDate(), jmr.getRecordKey());
                List<Map<String, Object>> usageAmountsDataList = spcHelperService.getUsageForAssetsAndTariffCodes(callIdentifiers, creditPoolToUse.getTariffCodeList(), user.getCustomer().getNextInvoiceDate(), jmr.getEventDate(), user.getId());
                BigDecimal oldAmountWithTax = getUsageAmount(usageAmountsDataList, nextInvoiceDate, taxTableName, taxDateFormat);
                logger.debug("oldAmountWithTax {} for jmr {}", oldAmountWithTax, jmr.getRecordKey());
                BigDecimal newAmount = inclusiveRoundedJMRAmount.add(oldAmountWithTax, MathContext.DECIMAL128);
                logger.debug("newAmount {} for jmr {}", newAmount, jmr.getRecordKey());
                BigDecimal freeAmountWithoutTax = creditPoolToUse.getFreeAmountAsDecimal();
                logger.debug("freeAmountWithoutTax {} for jmr {}", freeAmountWithoutTax, jmr.getRecordKey());
                BigDecimal inclusiveRoundedFreeAmount = addTaxComponent(freeAmountWithoutTax, itemTaxRate);
                logger.debug("inclusiveRoundedFreeAmount {} for jmr {}", inclusiveRoundedFreeAmount, jmr.getRecordKey());
                List<BigDecimal> utilizedPercentages = findNotificationPercentages(creditPoolToUse.getConsumptionPercentageList(), inclusiveRoundedFreeAmount, oldAmountWithTax, newAmount);
                if (CollectionUtils.isEmpty(utilizedPercentages)) {
                    logger.debug("utilized percentage not found!");
                    return;
                }
                logger.debug("utilized percentages {}", utilizedPercentages);
                EventManager.process(new SpcCreditPoolNotificationEvent(getEntityId(), oldAmountWithTax, newAmount, creditPoolToUse, utilizedPercentages, jmr, planOrder.getId()));
            }
        } finally {
            if(shouldReducePricingFields) {
                reducePricingFields(jmr);
            }
        }
    }

    @Override
    public void afterProcessingUndo(JbillingMediationRecord jmr, OrderLineDTO mediatedLine) {
    	
        logger.debug("undo {} jmr on mediated line {}", jmr, mediatedLine.getId());
        String tariffCode = jmr.getPricingFieldValueByName(SPCConstants.TARIFF_CODE);
        if(StringUtils.isEmpty(tariffCode)) {
            return;
        }

        OrderLineItemizedUsageDTO usageInfo = mediatedLine.getOrderLineItemizedUsageDTO(mediatedLine.getId(), tariffCode);
        if(null == usageInfo) {
            return;
        }
        // remove amount and quantity.
        usageInfo.addAmountAndQuantity(jmr.getRatedPrice().negate(MathContext.DECIMAL128),
                jmr.getQuantity().negate(MathContext.DECIMAL128));
        logger.debug("amount {} and quantity {} removed for tariff code {} from order {}", jmr.getRatedPrice(), jmr.getQuantity(),
                tariffCode, mediatedLine.getPurchaseOrder().getId());

    }

    private List<BigDecimal> findNotificationPercentages(List<BigDecimal> notificationPercentages,
            BigDecimal freeAmount, BigDecimal oldAmount, BigDecimal newAmount) {
        List<BigDecimal> applicablePercentages = new ArrayList<>();
        BigDecimal oldAmountPercentage = (oldAmount.multiply(new BigDecimal("100"))).divide(freeAmount, CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
        BigDecimal newAmountPercentage = (newAmount.multiply(new BigDecimal("100"))).divide(freeAmount, CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
        for (BigDecimal percentage : notificationPercentages) {
            if (oldAmountPercentage.compareTo(percentage) >= 0) {
                continue;
            }

            if (newAmountPercentage.compareTo(percentage) >= 0) {
                applicablePercentages.add(percentage);
            }
        }
        return applicablePercentages;
    }
    
    private BigDecimal getUsageAmount(List<Map<String, Object>> usageDataMapList, Date nextInvoiceDate, String taxTableName, String taxDateFormat) {
        BigDecimal amount = new BigDecimal("0.0");
        if(CollectionUtils.isEmpty(usageDataMapList)) {
            logger.debug("Usage data map list is empty");
            return amount;
        }
        for(Map<String, Object> usageMap : usageDataMapList) {
            Integer itemId = (Integer)usageMap.get("item_id");
            logger.debug("Item id {}",itemId);
            BigDecimal itemLevelAmount = (BigDecimal)usageMap.get("usage");
            logger.debug("itemLevelAmount {}",itemLevelAmount);
            BigDecimal itemTaxRate = new ItemBL(itemId).getTaxRate(nextInvoiceDate, taxTableName, taxDateFormat);
            logger.debug("itemTaxRate {}",itemTaxRate);
            BigDecimal inclusiveItemLevelAmount = addTaxComponent(itemLevelAmount, itemTaxRate);
            logger.debug("inclusiveItemLevelAmount {}",inclusiveItemLevelAmount);
            amount = amount.add(inclusiveItemLevelAmount, MathContext.DECIMAL128)
                    .setScale(scale(), roundingMode());
            logger.debug("amount {}",amount);
        }
        return amount;
    }
    private BigDecimal addTaxComponent(BigDecimal originalAmount, BigDecimal itemTaxRate) {
        logger.debug("originalAmount {}", originalAmount);
        logger.debug("itemTaxRate {}", itemTaxRate);
        BigDecimal taxComponent = originalAmount.multiply(itemTaxRate.divide(new BigDecimal(100)));
        logger.debug("taxOriginalAmount {}", taxComponent);
        return originalAmount.add(taxComponent, MathContext.DECIMAL128)
                .setScale(scale(), roundingMode());
    }

    protected void reducePricingFields(JbillingMediationRecord record) {
        List<String> reducedPricingFields = Arrays.asList(record.getPricingFields().split(COMMA)).stream()
                                                .filter(pricingField -> StringUtils.containsIgnoreCase(
                                                    SPCConstants.SPC_JMR_PRICING_FIELDS, pricingField.split(COLON)[0]))
                                                .collect(Collectors.toList());
        record.setPricingFields(String.join(COMMA, reducedPricingFields));
    }
}
