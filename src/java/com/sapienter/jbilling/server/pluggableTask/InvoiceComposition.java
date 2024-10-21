/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pluggableTask;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.discount.db.DiscountLineDTO;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTierDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.process.event.ApplySuspendedPeriods;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Holder;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.util.Context;
/**
 * @author Leandro Zoi
 * @since 01/15/18
 */

public abstract class InvoiceComposition extends PluggableTask implements InvoiceCompositionTask {

    private BigDecimal etalonZero = new BigDecimal(0).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);

    protected static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DateTimeFormatter dateFormatter;
    private String invoiceLineDelegated;
    private String invoiceLineDelegatedDue;
    private String invoiceLineTo;
    private String invoiceLinePeriod;
    private String invoiceLineOrderNumber;
    protected String dateFormat;
    private Locale locale;
    private Integer invoiceLinePrecisionPrefValue = null;
    private JdbcTemplate jdbcTemplate = null;
    protected boolean resourceBundleInitialized = false;

    /**
     * Composes the actual invoice line description based off of set entity preferences and the order period being
     * processed.
     *
     * @param orderLine
     *            order line being processed
     * @param period
     *            period of time being processed
     * @return invoice line description
     */
    protected String composeDescription (OrderLineDTO orderLine, PeriodOfTime period) {
        OrderDTO order = orderLine.getPurchaseOrder();
        // initialize resource bundle once, if not initialized
        if (!resourceBundleInitialized) {
            initializeResourceBundleProperties(order.getBaseUserByUserId().getUserId());
        }
        StringBuilder lineDescription = new StringBuilder(1000).append(orderLine.getDescription());

        /*
         * append the billing period to the order line for non one-time orders
         */
        if (order.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE) {
            // period ends at midnight of the next day (E.g., Oct 1 00:00, effectivley end-of-day Sept 30th).
            // subtract 1 day from the end so the period print out looks human readable
            LocalDate start = period.getDateMidnightStart();
            LocalDate end = period.getDateMidnightEnd().minusDays(1);

            logger.debug("Composing for period {} to {}. Using date format: {}", start, end, dateFormat);

            // now add this to the line
            lineDescription.append(' ').append(invoiceLinePeriod).append(' ');
            lineDescription.append(dateFormatter.print(start)).append(' ');
            lineDescription.append(invoiceLineTo).append(' ');
            lineDescription.append(dateFormatter.print(end));
        }

        /*
         * optionally append the order id if the entity has the preference set
         */
        if (needAppendOrderId(order.getBaseUserByUserId().getCompany().getId())) {
            lineDescription.append(invoiceLineOrderNumber);
            lineDescription.append(' ');
            lineDescription.append(order.getId().toString());
        }

        return lineDescription.toString();
    }

    protected void initializeResourceBundleProperties (Integer userId) {
        logger.debug("Initializing resource bundle properties");
        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", getLocale(userId));

        setDateFormater(bundle.getString("format.date"));
        invoiceLineTo = bundle.getString("invoice.line.to");
        invoiceLinePeriod = bundle.getString("invoice.line.period");
        invoiceLineOrderNumber = bundle.getString("invoice.line.orderNumber");
        invoiceLineDelegated = bundle.getString("invoice.line.delegated");
        invoiceLineDelegatedDue = bundle.getString("invoice.line.delegated.due");

        dateFormatter = DateTimeFormat.forPattern(dateFormat);

        resourceBundleInitialized = true;
    }

    protected BigDecimal getAmountByType(OrderLineDTO orderLine, PeriodOfTime period) {
        switch (orderLine.getTypeId()) {
            case Constants.ORDER_LINE_TYPE_ITEM:
            case Constants.ORDER_LINE_TYPE_DISCOUNT:
            case Constants.ORDER_LINE_TYPE_ADJUSTMENT:
                return calculateAmountForPeriod(orderLine, period);
            case Constants.ORDER_LINE_TYPE_TAX:
            case Constants.ORDER_LINE_TYPE_PENALTY:
                return orderLine.getAmount();
            default:
                logger.debug("Unsupported order line type id: {}", orderLine.getTypeId());
                return BigDecimal.ZERO;
        }
    }

    protected void delegateInvoices(NewInvoiceContext invoiceCtx) {
        for (InvoiceDTO invoice : invoiceCtx.getInvoices()) {
            // the whole invoice will be added as a single line
            // The text of this line has to be i18n
            String delegatedLine = new StringBuilder(100).append(invoiceLineDelegated)
                                                         .append(' ')
                                                         .append(getUseInvocieIdAsInvoiceNumberPreferenceValue(invoiceCtx.getEntityId()) ? invoice.getId() : invoice.getPublicNumber())
                                                         .append(' ')
                                                         .append(invoiceLineDelegatedDue)
                                                         .append(' ')
                                                         .append(dateFormatter.print(invoice.getDueDate().getTime()))
                                                         .toString();

            invoiceCtx.addResultLine(new InvoiceLineDTO.Builder()
                                                       .description(delegatedLine)
                                                       .amount(invoice.getBalance())
                                                       .type(Constants.INVOICE_LINE_TYPE_DUE_INVOICE)
                                                       .build());
        }
    }

    protected BigDecimal composeInvoiceLine(NewInvoiceContext invoiceCtx, Integer userId, BigDecimal amount,
                                            OrderLineDTO orderLine, PeriodOfTime period, Holder<InvoiceLineDTO> holder,
                                            String callIdentifier, Long callCounter, String assetIdentifier,
                                            Integer usagePlanId, boolean allowLinesWithZero) {

        List<ActivePeriodChargingTask.SuspendedCycle> cycles = new ArrayList<>();
        EventManager.process(new ApplySuspendedPeriods(cycles, invoiceCtx.getEntityId(), userId, period.getStart(), period.getEnd()));
        if (cycles.isEmpty()) {
            EventManager.process(new ApplySuspendedPeriods(cycles, invoiceCtx.getEntityId(), userId, null, null));
        }
        setInvoiceLinePrecisionPreferenceValue(invoiceCtx.getEntityId());

        if (!cycles.isEmpty() && !period.equals(PeriodOfTime.OneTimeOrderPeriodOfTime)) {
            BigDecimal periodAmount = new BigDecimal("0.00");
            for (ActivePeriodChargingTask.SuspendedCycle cycle: cycles) {
                for (PeriodOfTime periodOfTime : cycle.splitPeriods(period)) {
                    if (periodOfTime.getDaysInPeriod() > 0) {
                        BigDecimal proratedAmount = calculateProRatedAmountForPeriod(orderLine.getAmount(), periodOfTime);

                        periodAmount = periodAmount.add(composeInvoiceLineForPeriod(invoiceCtx, userId,
                                                                                    proratedAmount, orderLine,
                                                                                    periodOfTime, holder,
                                                                                    callIdentifier, callCounter,
                                                                                    assetIdentifier, usagePlanId,
                                                                                    allowLinesWithZero));
                    }
                }
            }

            return periodAmount;
        } else {
            return composeInvoiceLineForPeriod(invoiceCtx, userId, amount, orderLine, period, holder, callIdentifier,
                                               callCounter, assetIdentifier, usagePlanId, allowLinesWithZero);
        }
    }

    /**
     * for each order line tier value, based on order line type:
     *
     * 1. determine invoice line type (specific for ITEM, DISCOUNT)
     *
     * 2. compose description (specific for ITEM, DISCOUNT)
     *
     * 3. calculate amount for period (specific for ITEM, DISCOUNT)
     *
     * 4. apply amount to invoice line (specific to TAX)
     *
     * 5. fill other invoice line fields
     *
     * 6. apply amount to order totals
     *
     * @param invoiceCtx
     * @param userId
     * @param orderLine
     * @param period
     *
     * @return calclulated amount this line is contributed to invoice
     */
    private BigDecimal composeInvoiceLineForPeriod (NewInvoiceContext invoiceCtx, Integer userId, BigDecimal amount,
                                                    OrderLineDTO orderLine, PeriodOfTime period, Holder<InvoiceLineDTO> holder,
                                                    String callIdentifier, Long callCounter, String assetIdentifier,
                                                    Integer usagePlanId, boolean allowLinesWithZero) {

        OrderDTO order = orderLine.getPurchaseOrder();
        Integer entityId = order.getBaseUserByUserId().getCompany().getId();
        setInvoiceLinePrecisionPreferenceValue(entityId);
        logger.debug("getting the invoice line precision preference value for entity = {}, invoiceLinePrecisionPrefValue = {}", entityId, invoiceLinePrecisionPrefValue);
        boolean taxFlag = getInvocieLineTaxPreferenceValue(invoiceCtx.getEntityId());
        OrderDTO parentOrder = order.getParentOrder();
        BigDecimal taxRate = !orderLine.isDiscount() ? getTaxRate(entityId, new ItemBL(orderLine.getItemId()).getEntity(), invoiceCtx.getBillingDate())
                : getWeightedTaxRate(entityId, invoiceCtx.getBillingDate(), parentOrder, orderLine);

        logger.debug("Adding order line from {}, quantity {}, price {}, typeid {}. Period: {}", order.getId(),
                                                                                             orderLine.getQuantity(),
                                                                                             orderLine.getPrice(),
                                                                                             orderLine.getTypeId(),
                                                                                             period);

        BigDecimal periodAmount = amount != null ? getInvoiceLinePrecisionBasedAmount(amount, entityId) : calculateAmountForPeriod(orderLine, period);

        if (!allowLinesWithZero && etalonZero.compareTo(periodAmount) == 0) {
            return BigDecimal.ZERO;
        }
        if(null!=orderLine.getOrderLineType() && orderLine.getOrderLineType().getId() == Constants.ORDER_LINE_TYPE_ITEM
                && PreferenceBL.isTierCreationAllowed(entityId) &&
                orderLine.hasOrderLineTiers() && !orderLine.getPurchaseOrder().getProrateFlag()) {
            for (OrderLineTierDTO oltier : orderLine.getOrderLineTiers())
            {
                InvoiceLineDTO.Builder newLine = new InvoiceLineDTO.Builder()
                        .description(getOrderLineTierDescription(composeDescription(orderLine, period), oltier, orderLine.getQuantity()))
                        .sourceUserId(order.getUser().getId())
                        .itemId(orderLine.getItemId())
                        .amount(taxFlag ? calculateAmountWithTax(oltier.getAmount(), taxRate) : getInvoiceLinePrecisionBasedAmount(oltier.getAmount(), entityId))
                        .price(oltier.getPrice())
                        .quantity(oltier.getQuantity())
                        .type(determineInvoiceLineType(userId, order))
                        .callIdentifier(callIdentifier)
                        .assetIdentifier(assetIdentifier)
                        .callCounter(callCounter)
                        .usagePlanId(usagePlanId)
                        .isPercentage(orderLine.isPercentage())
                        .order(order)
                        .grossAmount(taxFlag ? getInvoiceLinePrecisionBasedAmount(oltier.getAmount(), entityId) : BigDecimal.ZERO)
                        .taxAmount(taxFlag ? getTaxAmount(oltier.getAmount(), taxRate) : BigDecimal.ZERO)
                        .taxRate(taxRate);
                if (newLine != null) {
                    InvoiceLineDTO newInvoiceLine = newLine.build();
                    newInvoiceLine.setIsOrderLineTier(true);
                    invoiceCtx.addResultLine(newInvoiceLine);
                    if (holder != null) {
                        holder.setTarget(newInvoiceLine);
                    }
                }
            }
        } else {
                InvoiceLineDTO.Builder newLine = new InvoiceLineDTO.Builder();
                 if(orderLine.getPurchaseOrder().getIsMediated()){
                     BigDecimal usageAmountWithTax = taxFlag ? getUsagOrderLineAmountWithTax(orderLine.getId()) : periodAmount;
                     logger.debug("Amount with Tax for mediated order {} ", usageAmountWithTax);
                     BigDecimal excludingAmountRate = taxRate.divide(new BigDecimal("100")).add(BigDecimal.ONE);
                     logger.debug("Excluding amount rate : {} ", excludingAmountRate);
                     BigDecimal grossAmount = usageAmountWithTax.divide(excludingAmountRate, MathContext.DECIMAL128)
                             .setScale(invoiceLinePrecisionPrefValue, Constants.BIGDECIMAL_ROUND);
                     logger.debug("Gross Amount : {} ", grossAmount);
                     newLine
                      .amount(usageAmountWithTax)
                      .grossAmount(taxFlag ? grossAmount : BigDecimal.ZERO)
                      .taxAmount(taxFlag ? usageAmountWithTax.subtract(grossAmount) : BigDecimal.ZERO);
                 } else {
                     newLine
                     .amount(taxFlag ? calculateAmountWithTax(periodAmount, taxRate) : periodAmount)
                     .taxAmount(taxFlag ? getTaxAmount(periodAmount, taxRate) : BigDecimal.ZERO)
                     .grossAmount(taxFlag ? periodAmount : BigDecimal.ZERO);
                 }
                 newLine.description(orderLine.getDescription())
                     .sourceUserId(order.getUser().getId())
                     .itemId(orderLine.getItemId())
                     .price(orderLine.getPrice())
                     .callIdentifier(callIdentifier)
                     .assetIdentifier(assetIdentifier)
                     .callCounter(callCounter)
                     .usagePlanId(usagePlanId)
                     .isPercentage(orderLine.isPercentage())                     
                     .taxRate(taxRate);

            switch (orderLine.getTypeId()) {
                case Constants.ORDER_LINE_TYPE_ITEM:
                case Constants.ORDER_LINE_TYPE_DISCOUNT:
                case Constants.ORDER_LINE_TYPE_SUBSCRIPTION:
                case Constants.ORDER_LINE_TYPE_ADJUSTMENT:
                    //adjustment order
                    if (Constants.INVOICE_LINE_TYPE_ADJUSTMENT.equals(orderLine.getTypeId())) {
                        newLine.type(Constants.INVOICE_LINE_TYPE_ADJUSTMENT);
                    } else {
                        newLine.type(determineInvoiceLineType(userId, order));
                    }

                    newLine.description(composeDescription(orderLine, period))
                            .quantity(orderLine.getQuantity())
                            .order(order); // link invoice line to the order that originally held the charge
                    break;

                // tax items
                case Constants.ORDER_LINE_TYPE_TAX:

                    newLine.type(Constants.INVOICE_LINE_TYPE_TAX);

                    InvoiceLineDTO taxLine = findTaxLine(invoiceCtx.getResultLines(), orderLine.getDescription());
                    if (taxLine != null) {
                        // tax already exists, add the total
                        taxLine.setAmount(getInvoiceLinePrecisionBasedAmount(taxLine.getAmount(), entityId).add(periodAmount));
                        // not need to add a new invoice line
                        newLine = null;
                    }
                    break;

                // penalty items
                case Constants.ORDER_LINE_TYPE_PENALTY:
                    newLine.type(Constants.INVOICE_LINE_TYPE_PENALTY);
                    break;

                default:
                    logger.debug("Unsupported order line type id: {}", orderLine.getTypeId());
                    break;
            }

            if (newLine != null) {
                InvoiceLineDTO newInvoiceLine = newLine.build();
                invoiceCtx.addResultLine(newInvoiceLine);
                if (holder != null) {
                    holder.setTarget(newInvoiceLine);
                }
            }
        }

        return periodAmount;
    }

    private String getOrderLineTierDescription(String initialDescription, OrderLineTierDTO orderLineTierDTO, BigDecimal totalQuantity){
        StringBuilder builder = new StringBuilder();
        BigDecimal to = null != orderLineTierDTO.getTierTo() ? orderLineTierDTO.getTierTo() : totalQuantity;
        if(StringUtils.isNotBlank(initialDescription)){
            builder.append(initialDescription);
        }

        return builder.append(" Tier ")
                .append(orderLineTierDTO.getTierNumber())
                .append(" From: ").append(orderLineTierDTO.getTierFrom().setScale(2, BigDecimal.ROUND_HALF_UP))
                .append(" To: ").append(to.setScale(2, BigDecimal.ROUND_HALF_UP))
                .toString();

    }

    /**
     * Gets the locale for the given user.
     *
     * @param userId
     *            user to get locale for
     * @return users locale
     */
    protected Locale getLocale (Integer userId) {
        if (locale == null) {
            try {
                UserBL user = new UserBL(userId);
                locale = user.getLocale();
            } catch (Exception e) {
                throw new SessionInternalError("Exception occurred determining user locale for composition.", e);
            }
        }
        return locale;
    }

    /**
     * Returns true if the given entity wants the order ID appended to the invoice line description.
     *
     * @param entityId
     *            entity id
     * @return true if order ID should be appended, false if not.
     */
    protected boolean needAppendOrderId (Integer entityId) {
        int preferenceOrderIdInInvoiceLine = 0;
        try {
            preferenceOrderIdInInvoiceLine = PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_ORDER_IN_INVOICE_LINE);
        } catch (Exception e) {
            /* use default value */
        }
        return preferenceOrderIdInInvoiceLine == 1;
    }

    private BigDecimal calculateProRatedAmountForPeriod(BigDecimal fullPrice, PeriodOfTime period) {

        if (period == null || fullPrice == null) {
            logger.warn("Called with null parameters");
            return null;
        }

        // this is an amount from a one-time order, not a real period of time
        if (period == PeriodOfTime.OneTimeOrderPeriodOfTime) {
            return fullPrice;
        }

        // if this is not a fraction of a period, don't bother making any calculations
        if (period.getDaysInCycle() == period.getDaysInPeriod()) {
            return fullPrice;
        }

        BigDecimal oneDayPrice = fullPrice.divide(new BigDecimal(period.getDaysInCycle()), invoiceLinePrecisionPrefValue,
                Constants.BIGDECIMAL_ROUND);

        return oneDayPrice.multiply(new BigDecimal(period.getDaysInPeriod())).setScale(invoiceLinePrecisionPrefValue,
                Constants.BIGDECIMAL_ROUND);
    }

    private BigDecimal calculateAmountForPeriod(OrderLineDTO orderLine, PeriodOfTime period) {
        if (orderLine.getPurchaseOrder().getProrateFlag()) {
            if (null == invoiceLinePrecisionPrefValue)
                setInvoiceLinePrecisionPreferenceValue(orderLine.getPurchaseOrder().getUser().getEntity().getId());
            return calculateProRatedAmountForPeriod(orderLine.getAmount(), period);
        }
        return getInvoiceLinePrecisionBasedAmount(orderLine.getAmount(), orderLine.getPurchaseOrder().getUser().getEntity().getId());
    }

    private Integer determineInvoiceLineType (Integer userId, OrderDTO order) {
        if (userId.equals(order.getUser().getId())) {
            return (Constants.ORDER_PERIOD_ONCE.equals(order.getPeriodId())) ? Constants.INVOICE_LINE_TYPE_ITEM_ONETIME :
                    Constants.INVOICE_LINE_TYPE_ITEM_RECURRING;
        }

        return Constants.INVOICE_LINE_TYPE_SUB_ACCOUNT;
    }

    /**
     * Returns the index of a tax line with the matching description. Used to find an existing tax line so that similar
     * taxes can be consolidated;
     *
     * @param lines
     *            invoice lines
     * @param desc
     *            tax line description
     * @return index of tax line
     */
    private InvoiceLineDTO findTaxLine (List<InvoiceLineDTO> lines, String desc) {
        for (InvoiceLineDTO line : lines) {
            if (line.getTypeId() == Constants.ORDER_LINE_TYPE_TAX && line.getDescription().equals(desc)) {
                return line;
            }
        }

        return null;
    }

    protected void setDateFormater(String invoiceDateFormat) {
		dateFormat = invoiceDateFormat;
	}

    /**
     * Invoice line will show Invoice ID if parameter is true.
     * else invoice line will show Invoice Number in description
     * @return boolean
     */

    protected boolean getUseInvocieIdAsInvoiceNumberPreferenceValue(Integer entityId){
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_USE_INVOICE_ID_AS_INVOICE_NUMBER_IN_INVOICE_LINE_DESCRIPTIONS);
        return (prefValue != null && prefValue == 1);
    }

    /**
     * If an order line contains one or more asset Identifiers, this method return comma separated joined string of asset identifiers.
     * If no assets are present on the line, then it return empty string.
     * returns asset identifiers from order line in string format
     * @param orderLine
     * @return
     */
    protected String getAssetIdentifiers(OrderLineDTO orderLine) {
    	if(CollectionUtils.isEmpty(orderLine.getAssets())) {
    		return StringUtils.EMPTY;
    	}
    	return orderLine.getAssets()
    			.stream()
    			.map(AssetDTO::getIdentifier)
    			.collect(Collectors.joining(","));
    }

    private BigDecimal calculateAmountWithTax(BigDecimal amount, BigDecimal taxRate) {
        logger.debug("calculating amount with tax for the amount {} and with the tax rate of {}", amount, taxRate);
        return amount != null && taxRate != null ? amount.add(getTaxAmount(amount, taxRate)) : amount;
    }

    private BigDecimal getTaxAmount(BigDecimal grossAmount, BigDecimal taxRate) {
        logger.debug("getting tax amount for grossAmount = {} with taxRate = {}", grossAmount, taxRate);
        return grossAmount != null && taxRate != null ? grossAmount.multiply(taxRate).divide(BigDecimal.valueOf(100L),
                invoiceLinePrecisionPrefValue, Constants.BIGDECIMAL_ROUND) : BigDecimal.ZERO;
    }
    private BigDecimal getUsagOrderLineAmountWithTax(Integer  orderLineId) {
        logger.debug("calculating amount with tax for the order line id {}", orderLineId);
        JMRRepository jmrRepository = Context.getBean(JMRRepository.class);
        return jmrRepository.findAmountWithTaxForMediatedOrderLine(orderLineId);
    }
    
    private BigDecimal getTaxRate(Integer entityId, ItemDTO item, Date invoiceGenerationDate) {
        logger.debug("getting tax rate for {} and {}", item, invoiceGenerationDate);
        String taxTableName = new MetaFieldDAS().getComapanyLevelMetaFieldValue(CommonConstants.TAX_TABLE_NAME, entityId);
        String taxDateFormat = new MetaFieldDAS().getComapanyLevelMetaFieldValue(CommonConstants.TAX_DATE_FORMAT, entityId);
        if(null == item || StringUtils.isEmpty(taxTableName) || StringUtils.isEmpty(taxDateFormat)) {
            return BigDecimal.ZERO;
        } else if(item.isPlan()) {
            return new PlanBL(item.getPlans().iterator().next()).getTaxRate(invoiceGenerationDate, taxTableName, taxDateFormat);
        } else {
            return new ItemBL(item).getTaxRate(invoiceGenerationDate, taxTableName, taxDateFormat);
        }
    }

    private BigDecimal getWeightedTaxRate(Integer entityId, Date billingDate, OrderDTO parentOrder, OrderLineDTO discountOrderLine) {

        BigDecimal hundred = new BigDecimal(100);
        if (null == parentOrder) {
            return BigDecimal.ZERO;
        }

        List<DiscountLineDTO> discountLines = parentOrder.getDiscountLines();
        for (DiscountLineDTO discountLineDTO : discountLines) {
            OrderLineDTO parentLine = discountOrderLine.getParentLine();
            if (null != parentLine) {
                if (null != discountLineDTO.getItem() && discountLineDTO.getItem().getId() == parentLine.getItemId()) {
                    logger.debug("Returning product level tax..");
                    return getTaxRate(entityId,
                            new ItemBL(discountLineDTO.getItem().getId()).getEntity(), billingDate);
                }

                if (null != discountLineDTO.getPlanItem() && parentLine.getItem().isPlan()) {
                    for (PlanDTO planDTO : parentLine.getItem().getPlans()) {
                        //Plan level tax
                        if (planDTO.getItem().getId() == discountLineDTO.getPlanItem().getItem().getId()) {
                            logger.debug("Returning plan level tax..");
                            return getTaxRate(entityId,
                                    new ItemBL(discountLineDTO.getPlanItem().getItem().getId()).getEntity(), billingDate);
                        }
                        //Plan Item level tax
                        for (PlanItemDTO planItem : planDTO.getPlanItems()) {
                            if (discountLineDTO.getPlanItem().getItem().getId() == planItem.getItem().getId()) {
                                logger.debug("Returning plan item level tax..");
                                return getTaxRate(entityId,
                                        new ItemBL(discountLineDTO.getPlanItem().getItem().getId()).getEntity(), billingDate);
                            }
                        }
                    }
                }
            }
        }

        logger.debug("Calculating weighted average tax..");
        Map<Integer, WeightedAverage> weightedAverages = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalWeightedTax = BigDecimal.ZERO;
        for (OrderLineDTO orderLine : parentOrder.getLines()) {
            weightedAverages.put(
                    orderLine.getId(),
                    new WeightedAverage(orderLine.getAmount(), getTaxRate(entityId,
                            new ItemBL(orderLine.getItemId()).getEntity(), billingDate)));
            total = total.add(orderLine.getAmount());
        }

        for (Entry<Integer, WeightedAverage> entry : weightedAverages.entrySet()) {
            WeightedAverage weightedAverage = entry.getValue();
            weightedAverage.setWeightedAmount(weightedAverage.getAmount().divide(total, MathContext.DECIMAL64).multiply(hundred));
            weightedAverage.setWeightedTax(weightedAverage.getWeightedAmount().multiply(weightedAverage.getTax()).divide(hundred, MathContext.DECIMAL64));
            weightedAverages.put(entry.getKey(), weightedAverage);
            totalWeightedTax = totalWeightedTax.add(weightedAverage.getWeightedTax());
        }
        logger.debug("Weighted average tax calculated {}", totalWeightedTax);
        return totalWeightedTax;
    }


    private class WeightedAverage {
        private BigDecimal amount;
        private BigDecimal tax;
        private BigDecimal weightedAmount;
        private BigDecimal weightedTax;

        public WeightedAverage(BigDecimal amount,BigDecimal tax) {
            this.setAmount(amount);
            this.setTax(tax);
        }

        public BigDecimal getWeightedAmount() {
            return weightedAmount;
        }

        public void setWeightedAmount(BigDecimal weightedAmount) {
            this.weightedAmount = weightedAmount;
        }

        public BigDecimal getWeightedTax() {
            return weightedTax;
        }

        public void setWeightedTax(BigDecimal weightedTax) {
            this.weightedTax = weightedTax;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getTax() {
            return tax;
        }

        public void setTax(BigDecimal tax) {
            this.tax = tax;
        }
    }

    protected boolean getInvocieLineTaxPreferenceValue(Integer entityId){
        logger.debug("getting the invoice tax preference value for entity = {}", entityId);
        try {
            Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_INVOICE_LINE_TAX);
            return (prefValue != null && prefValue == 1);
        } catch(Exception e) {
            logger.error("errror occurred while getting the invoice tax preference value : {}", e);
            return false;
        }
    }

    private void setInvoiceLinePrecisionPreferenceValue(Integer entityId) {
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_INVOICE_LINE_PRECISION);
        if (null != prefValue) {
            //if value is 0 || 1 || >10, set default precision
            if (0 == prefValue || 1 == prefValue || 10 < prefValue) {
                invoiceLinePrecisionPrefValue = Constants.BIGDECIMAL_SCALE;
            } else {
                //if 2 to 10, then apply precision
                invoiceLinePrecisionPrefValue = prefValue;
            }
        }
    }

    private BigDecimal getInvoiceLinePrecisionBasedAmount (BigDecimal amount, Integer entityId) {
        if (null == invoiceLinePrecisionPrefValue)
            setInvoiceLinePrecisionPreferenceValue(entityId);
        BigDecimal retAmount = amount.setScale(invoiceLinePrecisionPrefValue, Constants.BIGDECIMAL_ROUND);
        return retAmount;
    }
}
