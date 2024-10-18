/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2017] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.spc;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.STR;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryBL;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

public class SpcServiceSummaryGenerationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ParameterDescription PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME = new ParameterDescription("OrderLine level service identifier mf name", true, STR);
    private static final ParameterDescription PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME = new ParameterDescription("Asset level service identifier mf name", true, STR);
    private static final ParameterDescription PARAM_ZERO_PRICE_EXCULDED_CATEGORIES = new ParameterDescription("zero_price_exculded_categories", false, STR);

    public SpcServiceSummaryGenerationTask() {
        descriptions.add(PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME);
        descriptions.add(PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME);
        descriptions.add(PARAM_ZERO_PRICE_EXCULDED_CATEGORIES);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        InvoicesGeneratedEvent.class,
        InvoiceDeletedEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        ServiceSummaryBL serviceSummaryBL = new ServiceSummaryBL();
        if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;
            for (Integer invoiceId : instantiatedEvent.getInvoiceIds()) {
                logger.debug("Deleting already exist Service summary for given Invoice Id :{}", invoiceId);
                serviceSummaryBL.deleteByInvoice(invoiceId);
                createServiceSummary(invoiceId);
            }
        } else if (event instanceof InvoiceDeletedEvent) {
            InvoiceDeletedEvent invoiceDeletedEvent = (InvoiceDeletedEvent) event;
            serviceSummaryBL.deleteByInvoice(invoiceDeletedEvent.getInvoice().getId());
        } else {
            throw new PluggableTaskException("Unknown event: " + event.getClass());
        }
    }

    /**
     * Validates plugin parameters and return values in Map.
     * @return
     * @throws PluggableTaskException
     */
    private Map<String, Object> validateAndReturnParam() throws PluggableTaskException {
        Integer entityId = getEntityId();
        String excludedParamName = PARAM_ZERO_PRICE_EXCULDED_CATEGORIES.getName();
        String exculdedCategoriesParam = getParameter(excludedParamName, StringUtils.EMPTY);
        Map<String, Object> paramMap = new HashMap<>();
        List<Integer> categories = new ArrayList<>();
        for(String category : exculdedCategoriesParam.split(",")) {
            if(!NumberUtils.isNumber(category)) {
                logger.debug("invalid value {} passed to parameter {} for entity {}", exculdedCategoriesParam, excludedParamName, entityId);
                throw new PluggableTaskException("invalid value " + exculdedCategoriesParam + " passed to "+ excludedParamName);
            }
            categories.add(Integer.parseInt(category));
        }
        paramMap.put(excludedParamName, categories);
        String orderLineLevelMfParamName = PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME.getName();
        String orderLineLevelMfName = getMandatoryStringParameter(orderLineLevelMfParamName);
        MetaField orderLineLevelMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER_LINE }, orderLineLevelMfName);
        if(null == orderLineLevelMf) {
            logger.error("{} not present on order line level metafield for entity {}", orderLineLevelMfName, entityId);
            throw new PluggableTaskException(orderLineLevelMfName + " not found on order line level for entity "+ entityId);
        }
        paramMap.put(orderLineLevelMfParamName, orderLineLevelMfName);

        String assetServiceMfParamName = PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME.getName();
        String assetServiceMfName = getMandatoryStringParameter(assetServiceMfParamName);
        MetaField assetMetaField = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ASSET }, assetServiceMfName);
        if(null == assetMetaField) {
            logger.error("{} not present on asset level metafield for entity {}", assetServiceMfName, entityId);
            throw new PluggableTaskException(assetServiceMfName + " not found on asset level for entity "+ entityId);
        }
        paramMap.put(assetServiceMfParamName, assetServiceMfName);
        return paramMap;
    }

    /**
     * gets {@link ResourceBundle} for userId.
     * @param userId
     * @return
     */
    private ResourceBundle getResourceBundleForUser(Integer userId) {
        UserBL user = new UserBL(userId);
        return ResourceBundle.getBundle("entityNotifications",  user.getLocale());
    }

    /**
     * Creates identifier invoice line map for mediated order.
     * @param invoiceLines
     * @return
     */
    private Map<String, List<InvoiceLineDTO>> createIdentiferMediatedInvoiceLinesMap(List<InvoiceLineDTO> invoiceLines) {
        return invoiceLines.stream()
                .filter(invoiceLine -> invoiceLine.getOrder().getIsMediated())
                .filter(invoiceLine -> StringUtils.isNotEmpty(invoiceLine.getCallIdentifier()))
                .collect(Collectors.groupingBy(InvoiceLineDTO::getCallIdentifier, Collectors.toList()));
    }

    private static final String GET_LAST_ORDER_PROCESS_FOR_ORDER_SQL =
            "SELECT op.* FROM order_process op, invoice i WHERE op.order_id = :orderId AND op.invoice_id = i.id "
                    + "AND op.period_start IS NOT NULL AND op.period_end IS NOT NULL "
                    + "AND op.invoice_id <> :invoiceId ORDER BY i.create_datetime DESC LIMIT 1 ";

    private static final String START_PERIOD = "start";
    private static final String END_PERIOD   = "end";

    /**
     * find usage period based on {@link OrderProcessDTO}.
     * @param orderProcess
     * @return
     */
    private Map<String, Date> getUsagePeriodFromOrderProcess(OrderProcessDTO orderProcess) {
        Map<String, Date> usagePeriod = new HashMap<>();
        // JBSPC-516 : If orderProcess is null, then return null start and end dates.
        if(null == orderProcess) {
            usagePeriod.put(START_PERIOD, null);
            usagePeriod.put(END_PERIOD, null);
            return usagePeriod;
        }
        OrderDTO order = orderProcess.getPurchaseOrder();
        usagePeriod.put(START_PERIOD, orderProcess.getPeriodStart());
        usagePeriod.put(END_PERIOD, orderProcess.getPeriodEnd());
        if(!order.isPostPaid()) {
            SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
            Session session = sf.getCurrentSession();
            Integer invoiceId = orderProcess.getInvoice().getId();
            OrderProcessDTO lastOrderProcess =  (OrderProcessDTO) session.createSQLQuery(GET_LAST_ORDER_PROCESS_FOR_ORDER_SQL)
                    .addEntity(OrderProcessDTO.class)
                    .setParameter("orderId", orderProcess.getPurchaseOrder().getId())
                    .setParameter("invoiceId", invoiceId)
                    .uniqueResult();
            if(null == lastOrderProcess) {
                logger.debug("no period found for order {} on invoice {}", order.getId(), invoiceId);
                return usagePeriod;
            }
            usagePeriod.put(START_PERIOD, lastOrderProcess.getPeriodStart());
            usagePeriod.put(END_PERIOD, lastOrderProcess.getPeriodEnd());
        }
        return usagePeriod;
    }

    /**
     * Create Service summary for generated invoice Invoice summary will not generate for review and zero amount &
     * balance invoices
     *
     * @param invoiceId
     * @throws PluggableTaskException
     */
    private void createServiceSummary(Integer invoiceId) throws PluggableTaskException {
        InvoiceDTO invoiceDTO = new InvoiceBL(invoiceId).getEntity();
        Map<String, Object> paramMap = validateAndReturnParam();
        logger.debug("Creating Service Summary for Invoice Id: {}", invoiceId);
        String assetServiceNumberMfName = (String) paramMap.get(PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME.getName());
        String orderLineServiceNumberMfName = (String) paramMap.get(PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME.getName());
        List<PlanSummaryData> planSummaries = PlanSummaryData.generatePlanSummariesFromInvoiceLines(invoiceDTO.getInvoiceLines(), assetServiceNumberMfName,
                orderLineServiceNumberMfName);
        logger.debug("plan summeries {} section created for invoice {}", planSummaries, invoiceDTO.getId());
        List<Integer> includedInvoiceLineIds = new ArrayList<>();
        ServiceSummaryBL serviceSummaryBL = new ServiceSummaryBL();
        UserDTO user = invoiceDTO.getBaseUser();
        ResourceBundle userResourceBundle = getResourceBundleForUser(user.getId());
        // creating plan summary section.
        for(PlanSummaryData planSummaryData : planSummaries) {
            logger.debug("creating service summary section for plan summary {}", planSummaryData);
            OrderProcessDTO planOrderProcessDTO = invoiceDTO.getOrderProcessForOrder(planSummaryData.getOrderId());
            Date planStartDate = null;
            Calendar planEndDate = Calendar.getInstance();
            if(null != planOrderProcessDTO) {
                planStartDate = planOrderProcessDTO.getPeriodStart();
                planEndDate.setTime(planOrderProcessDTO.getPeriodEnd());
                planEndDate.add(Calendar.DAY_OF_MONTH, -1);
            }
            for(InvoiceLineDTO planInvoiceLine : planSummaryData.getPlanInvoiceLines()) {
                ServiceSummaryDTO planServiceSummary = new ServiceSummaryDTO();
                planServiceSummary.setServiceDescription("Plan Fee");
                planServiceSummary.setPlanId(planSummaryData.getPlanId());
                String planDescription = planInvoiceLine.getItem().getDescription(user.getLanguageIdField());
                planServiceSummary.setServiceId("0");
                if (planSummaryData.getAssetCountOnPlan() == 1) {
                    planServiceSummary.setDisplayIdentifier(planSummaryData.getPlanAssetServiceNumbers().get(0));
                } else {
                    planServiceSummary.setDisplayIdentifier(planDescription);
                }
                planServiceSummary.setItemId(planInvoiceLine.getItem().getId());
                planServiceSummary.setInvoiceLineId(planInvoiceLine.getId());
                planServiceSummary.setIsPlan(true);
                planServiceSummary.setInvoiceId(invoiceId);
                planServiceSummary.setUserId(user.getId());
                planServiceSummary.setPlanDescription(planDescription);
                planServiceSummary.setSubscriptionOrderId(planSummaryData.getOrderId());
                if(planSummaryData.getPlanInvoiceLines().size() == 1) {
                    planServiceSummary.setStartDate(planStartDate);
                    planServiceSummary.setEndDate(planEndDate.getTime());
                } else {
                    List<Date> dates = getStartDateAndEndDateFromLineDescription(userResourceBundle, planStartDate, planEndDate.getTime(),
                            planInvoiceLine.getDescription());
                    planServiceSummary.setStartDate(dates.get(0));
                    planServiceSummary.setEndDate(dates.get(1));
                }
                Integer planServiceSummaryId = serviceSummaryBL.create(planServiceSummary);
                logger.debug("plan service summary created {}", planServiceSummaryId);
            }
            OrderDTO orderDTO = new OrderBL(planSummaryData.getOrderId()).getEntity();
            if(orderDTO.getOrderPeriod().getPeriodUnit().isYearly()) {
                ServiceSummaryDTO planServiceSummary = new ServiceSummaryDTO();
                planServiceSummary.setServiceDescription("Yearly Plan Fee");
                planServiceSummary.setPlanId(planSummaryData.getPlanId());
                ItemDTO item = new PlanBL(planSummaryData.getPlanId()).getEntity().getItem();
                String planDescription = item.getDescription(user.getLanguageIdField());
                planServiceSummary.setServiceId("0");
                if (planSummaryData.getAssetCountOnPlan() == 1) {
                    planServiceSummary.setDisplayIdentifier(planSummaryData.getPlanAssetServiceNumbers().get(0));
                } else {
                    planServiceSummary.setDisplayIdentifier(planDescription);
                }
                planServiceSummary.setItemId(item.getId());
                planServiceSummary.setInvoiceLineId(null);
                planServiceSummary.setIsPlan(true);
                planServiceSummary.setInvoiceId(invoiceId);
                planServiceSummary.setUserId(user.getId());
                planServiceSummary.setPlanDescription(planDescription);
                planServiceSummary.setSubscriptionOrderId(planSummaryData.getOrderId());
                planServiceSummary.setStartDate(null);
                planServiceSummary.setEndDate(null);
                Integer planServiceSummaryId = serviceSummaryBL.create(planServiceSummary);
                logger.debug("yearly plan service summary created {}", planServiceSummaryId);
            }
            includedInvoiceLineIds.addAll(planSummaryData.getAllInvoiceLines());
            List<InvoiceLineDTO> mediatedLines = planSummaryData.getMediatedLines();
            Map<String, List<InvoiceLineDTO>> identifierInvoiceLinesMap = null;
            Map<String, Date> usagePeriod = getUsagePeriodFromOrderProcess(planOrderProcessDTO);
            logger.debug("usage period {} for plan order {} for plan {}", usagePeriod,
                     planSummaryData.getOrderId(), planSummaryData.getPlanId());
            if(CollectionUtils.isNotEmpty(mediatedLines)) {
                identifierInvoiceLinesMap = createIdentiferMediatedInvoiceLinesMap(mediatedLines);
                for(Entry<String, List<InvoiceLineDTO>> identifierInvoiceLinesEntry : identifierInvoiceLinesMap.entrySet()) {
                    int mediatedServiceSummaryCount = -1;
                    for(InvoiceLineDTO mediatedLine : sortMediatedInvoiceLinesBasedOnRollupCodes(identifierInvoiceLinesEntry.getValue())) {
                        mediatedServiceSummaryCount++;
                        ServiceSummaryDTO mediatedServiceSummary = new ServiceSummaryDTO();
                        if(planSummaryData.getAssetCountOnPlan() > 1 ||
                            (orderDTO.getOrderPeriod().getPeriodUnit().isYearly() && null == planOrderProcessDTO)) {
                            String serviceId = getServiceIdForAssetIdentifier(mediatedLine.getCallIdentifier(), assetServiceNumberMfName);
                            mediatedServiceSummary.setServiceId(serviceId);
                            if(mediatedServiceSummaryCount == 0 ) {
                                mediatedServiceSummary.setDisplayIdentifier(serviceId);
                            }
                        }
                        mediatedServiceSummary.setInvoiceId(invoiceId);
                        mediatedServiceSummary.setInvoiceLineId(mediatedLine.getId());
                        mediatedServiceSummary.setIsPlan(false);
                        mediatedServiceSummary.setPlanId(planSummaryData.getPlanId());
                        mediatedServiceSummary.setUserId(user.getId());
                        if(mediatedLine.hasItem()) {
                            mediatedServiceSummary.setItemId(mediatedLine.getItem().getId());
                            mediatedServiceSummary.setServiceDescription(mediatedLine.getItem().getDescription(user.getLanguageIdField()));
                        } else {
                            mediatedServiceSummary.setServiceDescription(mediatedLine.getDescription());
                        }
                        List<Date> minMaxEventDates = findMinMaxDateForMediatedInvoiceLine(usagePeriod.get(START_PERIOD), usagePeriod.get(END_PERIOD), mediatedLine);
                        logger.debug("min and max dates {} for asset identifier {} for period [{} to {}]", minMaxEventDates,
                                mediatedLine.getCallIdentifier(), planStartDate, planEndDate.getTime());

                        mediatedServiceSummary.setStartDate(minMaxEventDates.get(0));
                        mediatedServiceSummary.setEndDate(minMaxEventDates.get(1));
                        mediatedServiceSummary.setSubscriptionOrderId(planSummaryData.getOrderId());
                        Integer mediatedServiceSummaryId = serviceSummaryBL.create(mediatedServiceSummary);
                        logger.debug("mediated service summary created {} for user {}", mediatedServiceSummaryId, user.getId());
                    }
                }

            }

            // populate zero price service summary.
            List<String> assetsWithNoUsage = findAssetsWithNoUsage(planSummaryData.getAssetNumbers(), identifierInvoiceLinesMap);
            AssetDAS assetDAS = new AssetDAS();
            if(CollectionUtils.isNotEmpty(assetsWithNoUsage)) {
                @SuppressWarnings("unchecked")
                List<Integer> zeroPriceExcludedCategories = (List<Integer>) paramMap.get(PARAM_ZERO_PRICE_EXCULDED_CATEGORIES.getName());
                logger.debug("excluded categories {}", zeroPriceExcludedCategories);
                for(String assetWithZeroUsage : assetsWithNoUsage) {
                    String description = StringUtils.EMPTY;
                    if(CollectionUtils.isNotEmpty(zeroPriceExcludedCategories)) {
                        AssetDTO asset = assetDAS.findAssetsByIdentifier(assetWithZeroUsage).get(0);
                        ItemDTO item = asset.getItem();
                        description = item.getDescription(user.getLanguageIdField());
                        boolean shouldSkip = false;
                        for(Integer zeroPriceExeculedCategory : zeroPriceExcludedCategories) {
                            if(item.belongsToCategory(zeroPriceExeculedCategory)) {
                                logger.debug("asset number {} belongs to excluded category {}", assetWithZeroUsage, zeroPriceExeculedCategory);
                                shouldSkip = true;
                                break;
                            }
                        }
                        if (shouldSkip || BigDecimal.ZERO.compareTo(getItemPriceOrZero(item, invoiceDTO.getCreateDatetime())) != 0) {
                        	// If the price of the asset enabled product is non-zero, 
                        	// then skip generating a zero price service summary
                            continue;
                        }
                    }
                    ServiceSummaryDTO zeroPriceServiceSummary = new ServiceSummaryDTO();
                    zeroPriceServiceSummary.setInvoiceId(invoiceId);
                    zeroPriceServiceSummary.setUserId(user.getId());
                    zeroPriceServiceSummary.setIsPlan(false);
                    zeroPriceServiceSummary.setPlanId(planSummaryData.getPlanId());
                    zeroPriceServiceSummary.setStartDate(planStartDate);
                    zeroPriceServiceSummary.setEndDate(planEndDate.getTime());
                    zeroPriceServiceSummary.setServiceDescription(description);
                    String serviceId = getServiceIdForAssetIdentifier(assetWithZeroUsage, assetServiceNumberMfName);
                    zeroPriceServiceSummary.setServiceId("zzzzzzzzzz"+serviceId);
                    zeroPriceServiceSummary.setDisplayIdentifier(serviceId);
                    zeroPriceServiceSummary.setSubscriptionOrderId(planSummaryData.getOrderId());
                    Integer zeroPriceServiceSummaryServiceSummaryId = serviceSummaryBL.create(zeroPriceServiceSummary);
                    logger.debug("zero price mediated usage service summary created {} for user {}",
                            zeroPriceServiceSummaryServiceSummaryId, user.getId());
                }
            }

            List<InvoiceLineDTO> nonMediatedLines = planSummaryData.getNonMediatedLines();
            if(CollectionUtils.isNotEmpty(nonMediatedLines)) {
                for(InvoiceLineDTO nonMediatedLine : nonMediatedLines) {
                    logger.debug("generating non mediated summary for invoice line {}", nonMediatedLine.getId());
                    ServiceSummaryDTO nonMediatedServiceSummary = new ServiceSummaryDTO();
                    nonMediatedServiceSummary.setInvoiceId(invoiceId);
                    nonMediatedServiceSummary.setUserId(user.getId());
                    nonMediatedServiceSummary.setInvoiceLineId(nonMediatedLine.getId());
                    nonMediatedServiceSummary.setIsPlan(false);
                    nonMediatedServiceSummary.setPlanId(planSummaryData.getPlanId());
                    nonMediatedServiceSummary.setServiceId("00");
                    if(planSummaryData.getPlanInvoiceLines().size() == 1) {
                        setNonMediatedLineDates(planStartDate, planEndDate.getTime(), nonMediatedLine, nonMediatedServiceSummary);
                    } else {
                        List<Date> dates = getStartDateAndEndDateFromLineDescription(userResourceBundle, planStartDate, planEndDate.getTime(),
                                nonMediatedLine.getDescription());
                        setNonMediatedLineDates(dates.get(0), dates.get(1), nonMediatedLine, nonMediatedServiceSummary);
                    }
                    if(nonMediatedLine.hasItem()) {
                        nonMediatedServiceSummary.setItemId(nonMediatedLine.getItem().getId());
                        nonMediatedServiceSummary.setServiceDescription(nonMediatedLine.getItem().getDescription(user.getLanguageIdField()));
                    } else {
                        nonMediatedServiceSummary.setServiceDescription(nonMediatedLine.getDescription());
                    }
                    nonMediatedServiceSummary.setSubscriptionOrderId(planSummaryData.getOrderId());
                    Integer nonMediatedServiceSummaryId = serviceSummaryBL.create(nonMediatedServiceSummary);
                    logger.debug("non mediated service summary created {} for user {}", nonMediatedServiceSummaryId, user.getId());
                }
            }
        }

        // populate account charges
        List<Integer> itemsCategoryTypeAdjustment = new ItemDAS().getAllItemsCategoryTypeAdjustment();
        for(InvoiceLineDTO invoiceLine : invoiceDTO.getInvoiceLines()) {
            if(includedInvoiceLineIds.contains(invoiceLine.getId())) {
                logger.debug("plan summary already generated for invoice line {}", invoiceLine.getId());
                continue;
            }
            if(invoiceLine.dueInvoiceLine()) {
                logger.debug("skip plan summary generation for carried invoice line {}", invoiceLine.getId());
                continue;
            }
            if(null != invoiceLine.getItem()
                    && itemsCategoryTypeAdjustment.contains(invoiceLine.getItem().getId())) {
                logger.debug("skip plan summary generation for adjustment invoice line {}", invoiceLine.getId());
                continue;
            }
            logger.debug("generating account charges summary for invoice line {}", invoiceLine.getId());
            ServiceSummaryDTO accountChargesServiceSummary = new ServiceSummaryDTO();
            accountChargesServiceSummary.setInvoiceId(invoiceId);
            accountChargesServiceSummary.setUserId(user.getId());
            accountChargesServiceSummary.setInvoiceLineId(invoiceLine.getId());
            accountChargesServiceSummary.setIsPlan(false);
            OrderDTO invoiceOrder = invoiceLine.getOrder();
            OrderProcessDTO orderProcessDTO = invoiceOrder == null ? null : invoiceDTO.getOrderProcessForOrder(invoiceOrder.getId());
            Date startDate;
            Date endDate;
            if(null!= orderProcessDTO) {
                startDate = orderProcessDTO.getPeriodStart();
                endDate = orderProcessDTO.getPeriodEnd();
            } else if(CollectionUtils.isNotEmpty(planSummaries)) {
                if(planSummaries.size() == 1) {
                    OrderProcessDTO planOrderProcessDTO = invoiceDTO.getOrderProcessForOrder(planSummaries.get(0).getOrderId());
                    startDate = planOrderProcessDTO.getPeriodStart();
                    endDate = planOrderProcessDTO.getPeriodEnd();
                } else {
                    List<OrderProcessDTO> planOrderProcesses = new ArrayList<>();
                    for(PlanSummaryData planSummaryData : planSummaries) {
                        planOrderProcesses.add(invoiceDTO.getOrderProcessForOrder(planSummaryData.getOrderId()));
                    }
                    Collections.sort(planOrderProcesses, (op1, op2) -> op1.getPeriodStart().compareTo(op2.getPeriodStart()));
                    startDate = planOrderProcesses.get(0).getPeriodStart();
                    endDate = planOrderProcesses.get(0).getPeriodEnd();
                }
            } else if( null!= invoiceOrder) {
                startDate = invoiceOrder.getActiveSince();
                endDate = invoiceOrder.getActiveUntil()!=null ? invoiceOrder.getActiveUntil() : startDate;
            } else {
                startDate = invoiceLine.getInvoice().getCreateDatetime();
                endDate = startDate;
            }

            accountChargesServiceSummary.setStartDate(startDate);
            accountChargesServiceSummary.setEndDate(endDate);
            if(invoiceLine.hasItem()) {
                accountChargesServiceSummary.setItemId(invoiceLine.getItem().getId());
                accountChargesServiceSummary.setServiceDescription(invoiceLine.getItem().getDescription(user.getLanguageIdField()));
            } else {
                accountChargesServiceSummary.setServiceDescription(invoiceLine.getDescription());
            }
            Integer accountChargesServiceSummaryId = serviceSummaryBL.create(accountChargesServiceSummary);
            logger.debug("account charges summary created {} for user {}", accountChargesServiceSummaryId, user.getId());
        }
    }
    
    private BigDecimal getItemPriceOrZero(ItemDTO item, Date invoiceDate) {
    	PriceModelDTO priceModel = item.getPrice(invoiceDate);
    	if (null != priceModel) {
    		return priceModel.getRate();
    	}
    	return BigDecimal.ZERO;
    }

    private void setNonMediatedLineDates(Date startDate,
            Date endDate, InvoiceLineDTO nonMediatedLine,
            ServiceSummaryDTO nonMediatedServiceSummary) {
        if (nonMediatedLine.getOrder().getPeriodId() == 1 &&
                !nonMediatedLine.getOrder().getIsMediated()) {
            nonMediatedServiceSummary.setStartDate(nonMediatedLine.getOrder().getActiveSince());
            nonMediatedServiceSummary.setEndDate(null);
        } else {
            nonMediatedServiceSummary.setStartDate(startDate);
            nonMediatedServiceSummary.setEndDate(endDate);
        }
    }
    
    /**
     * Helper method to extract start and end dates from the invoice line description
     * @param user
     * @param planStartDate
     * @param planEndDate
     * @param invoiceLineDescription
     * @return
     * @throws PluggableTaskException
     */
    private List<Date> getStartDateAndEndDateFromLineDescription(ResourceBundle userResourceBundle, Date planStartDate, Date planEndDate,
            String invoiceLineDescription) throws PluggableTaskException {
        List<String> datesStr = new LinkedList<>();
        List<Date> dates = new LinkedList<>();
        Matcher dateMatcher = Pattern.compile(userResourceBundle.getString("format.date.Regex")).matcher(invoiceLineDescription);
        int count = 2;
        while (count-- !=0 && dateMatcher.find()) {
            String date = dateMatcher.group(0);
            logger.debug("parsed date {} from invoice line description {}", date, invoiceLineDescription);
            datesStr.add(date);
        }
        logger.debug("dates constructed {} from invoice line description {}", datesStr, invoiceLineDescription);
        if(datesStr.size() != 2) {
            logger.debug("start and end date not found on invoice line description {}, "
                    + "so taking start and end date from OrderProcessDTO ", invoiceLineDescription);
            dates.add(planStartDate);
            dates.add(planEndDate);
        } else {
            DateFormat dateFormat = new SimpleDateFormat(userResourceBundle.getString("format.date"));
            try {
                dates.add(dateFormat.parse(datesStr.get(0)));
                dates.add(dateFormat.parse(datesStr.get(1)));
            } catch(ParseException ex) {
                throw new PluggableTaskException("date parsing failed from invoice line description", ex);
            }
        }
        return dates;
    }

    private String getServiceIdForAssetIdentifier(String identifier, String assetServiceNumberMfName) {
        AssetDTO asset = new AssetDAS().findAssetsByIdentifier(identifier).get(0);
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> assetServiceId = asset.getMetaField(assetServiceNumberMfName);
        if(null == assetServiceId || StringUtils.isEmpty(assetServiceId.getValue())) {
            return identifier;
        }
        return assetServiceId.getValue();
    }


    private static final String SORT_AND_GET_MEDIATED_LINES_BY_ROLLUP_CODES_SQL =
            "SELECT il.* FROM invoice_line il LEFT JOIN rollup_codes rcodes ON il.description = rcodes.item_type_description "
                    + "AND rcodes.rollup_code_type NOT LIKE 'super%' "
                    + "WHERE il.id IN (:invoiceLineIds) ORDER BY rcodes.itemisation_order;";

    @SuppressWarnings("unchecked")
    private static List<InvoiceLineDTO> sortMediatedInvoiceLinesBasedOnRollupCodes(List<InvoiceLineDTO> mediatedInvoiceLines) {
        SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
        Session session = sf.getCurrentSession();
        return session.createSQLQuery(SORT_AND_GET_MEDIATED_LINES_BY_ROLLUP_CODES_SQL)
                .addEntity(InvoiceLineDTO.class)
                .setParameterList("invoiceLineIds", mediatedInvoiceLines.stream().map(InvoiceLineDTO::getId).collect(Collectors.toList()))
                .list();
    }

    private static List<String> findAssetsWithNoUsage(List<String> assetNumbers, Map<String, List<InvoiceLineDTO>> mediatedLines) {
        if(MapUtils.isEmpty(mediatedLines)) {
            return assetNumbers;
        }
        List<String> assetsWithNoUsage = new ArrayList<>();
        for(String assetNumber : assetNumbers) {
            if(CollectionUtils.isEmpty(mediatedLines.get(assetNumber))) {
                assetsWithNoUsage.add(assetNumber);
            }
        }
        return assetsWithNoUsage;
    }

    private static final String FIND_MIN_MAX_EVENT_DATE_SQL =
            "SELECT MIN(jmr.event_date) AS start, MAX(jmr.event_date) AS end FROM jbilling_mediation_record jmr, order_line ol "
                    + "WHERE jmr.order_line_id = ol.id AND ol.call_identifier = ? "
                    + "AND jmr.user_id = ? AND jmr.item_id = ? AND event_date BETWEEN ? AND ?";
    private static final String FIND_MIN_MAX_EVENT_DATE_USING_ORDER_ID_SQL =
            "SELECT MIN(jmr.event_date) AS start, MAX(jmr.event_date) AS end FROM jbilling_mediation_record jmr, order_line ol "
                    + "WHERE jmr.order_line_id = ol.id AND ol.call_identifier = ? "
                    + "AND jmr.user_id = ? AND jmr.item_id = ? AND jmr.order_id = ?";

    private List<Date> findMinMaxDateForMediatedInvoiceLine(Date start, Date end, InvoiceLineDTO mediatedLine) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        String callIdentifier = mediatedLine.getCallIdentifier();
        List<Map<String, Object>> rows = null;
        Date endDate = null;
        Date startDate = null;
        if(null == start && null == end) {
            rows = jdbcTemplate.queryForList(FIND_MIN_MAX_EVENT_DATE_USING_ORDER_ID_SQL, callIdentifier,
                    mediatedLine.getInvoice().getBaseUser().getId(), mediatedLine.getItem().getId(),mediatedLine.getOrder().getId());
        } else {
        	String companyLevelTimeZone = TimezoneHelper.getCompanyLevelTimeZone(getEntityId());
            endDate = getComanyZonedDate(end, companyLevelTimeZone);
            startDate = getComanyZonedDate(start, companyLevelTimeZone);
            rows = jdbcTemplate.queryForList(FIND_MIN_MAX_EVENT_DATE_SQL, callIdentifier,
                    mediatedLine.getInvoice().getBaseUser().getId(), mediatedLine.getItem().getId(),startDate, endDate);
        }
        if(CollectionUtils.isEmpty(rows)) {
            logger.debug("no event date found for indentifier for identifier {}, so returned default start/end date", callIdentifier);
            return Arrays.asList(startDate, endDate);
        }
        Map<String, Object> row = rows.get(0);
        logger.debug("dates {} found for range [{} to {}] for identifier {} ", row, startDate, endDate, callIdentifier);
        logger.debug("dates fetched {} for identifier {}", rows, callIdentifier);
        return Arrays.asList((Date)row.get(START_PERIOD), (Date)row.get(END_PERIOD));
    }

    private Date getComanyZonedDate(Date date, String companyLevelTimeZone) {
        return Date.from(Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
                .atZone(ZoneId.of(companyLevelTimeZone)).toInstant());
    }
}
