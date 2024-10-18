package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.ToString;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.AssetAssignmentDAS;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.util.Constants;

@ToString
class PlanSummaryData {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Integer planId;
    private Integer orderId;
    private Integer assetCountOnPlan;
    private List<InvoiceLineDTO> mediatedLines;
    private List<InvoiceLineDTO> nonMediatedLines;
    private List<InvoiceLineDTO> planInvoiceLines;
    private List<String> assetServiceNumbers;
    private List<String> planAssetServiceNumbers;
    private List<String> assetNumbers;

    private PlanSummaryData() {}


    /**
     * Create Order id plan id {@link Map} from {@link InvoiceLineDTO}.
     * @param invoiceLines
     * @param subscriptionOrderIdMfName
     * @return
     */
    private static Map<Integer, Integer> orderPlanMapFromInvoiceLines(Set<InvoiceLineDTO> invoiceLines, String subscriptionOrderIdMfName) {
        Map<Integer, Integer> orderPlanMap = new HashMap<>();
        for(InvoiceLineDTO invoiceLine : invoiceLines) {
            if(invoiceLine.isPlanLine()) {
                orderPlanMap.put(invoiceLine.getOrder().getId(), invoiceLine.getItem().firstPlan().getId());
            } else {
                // JBSPC-516 : if the invoice does not have a subscription order,
                // then retrieve the subscription order using the mediated order
                // orderPlanMap will only be populated if the subscription order is Yearly.
                OrderDTO order = invoiceLine.getOrder();
                Integer userId = order.getUserId();
                MetaFieldValue<Integer> subscriptionOrderIdValue = StringUtils.isNotBlank(subscriptionOrderIdMfName) ? order.getMetaField(subscriptionOrderIdMfName) : null;

                if (order.getIsMediated()) {
                    String assetIdentifier = invoiceLine.getCallIdentifier();
                    OrderDTO assetOrder = new OrderDAS().findOrderByUserAssetIdentifierEffectiveDate(userId, assetIdentifier, order.getActiveSince());
                    if (assetOrder.getOrderPeriod().getPeriodUnit().isYearly() || 
                    		(assetOrder.isPrePaid() && assetOrder.isFinished())) {
                        orderPlanMap.put(assetOrder.getId(), findPlanId(assetOrder, assetIdentifier));
                    }
                }
                // JBAGL-140 : if the invoice does not have the cancelled subscription order,
                // then try to retreive the subscription order from the credit order created
                // after the cancellation of subscription order.
                else if(null != subscriptionOrderIdValue) {
                    OrderDTO subscriptionOrder = new OrderBL(subscriptionOrderIdValue.getValue()).getEntity();
                        for (OrderLineDTO orderLine : subscriptionOrder.getLines()) {
                            ItemDTO item = orderLine.getItem();
                            if (0 == orderLine.getDeleted() && null != item && item.hasPlans()) {
                                orderPlanMap.put(subscriptionOrder.getId(), item.firstPlan().getId());
                                break;
                            }
                    }
                }
            }
        }
        return orderPlanMap;
    }

    private static Integer findPlanId(OrderDTO order, String identifier) {
        Integer plan = null;
        for(OrderLineDTO orderLine : order.getLines()) {
            ItemDTO item = orderLine.getItem();
            if(null!= item && item.hasPlans() ) {
                plan = item.firstPlan().getId();
                break;
            }
        }
        OrderDTO parentOrder = order.getParentOrder();
        if(null == plan && null!= parentOrder) {
            plan = findPlanId(parentOrder, identifier);
        }
        if(null == plan) {
            throw new SessionInternalError("no plan found for asset identifier "+ identifier);
        }
        return plan;
    }

    /**
     * Generates {@link PlanSummaryData} from {@link InvoiceLineDTO}.
     * @param invoiceDTO
     * @param assetServiceNumberMfName
     * @param orderLineServiceNumberMfName
     * @return
     */
    static List<PlanSummaryData> generatePlanSummariesFromInvoiceLines(InvoiceDTO invoiceDTO,
            String assetServiceNumberMfName, String orderLineServiceNumberMfName, String subscriptionOrderIdMfName) {
        Set<InvoiceLineDTO> invoiceLines = invoiceDTO.getInvoiceLines();
        if(CollectionUtils.isEmpty(invoiceLines)) {
            return Collections.emptyList();
        }
        invoiceLines = invoiceLines.stream()
                .filter(invoiceLine -> null!=invoiceLine.getOrder())
                .filter(invoiceLine -> !invoiceLine.dueInvoiceLine())
                .filter(invoiceLine -> !invoiceLine.adjustmentInvoiceLine())
                .collect(Collectors.toSet());

        Map<Integer, Integer> orderPlanMap = orderPlanMapFromInvoiceLines(invoiceLines, subscriptionOrderIdMfName);
        if(MapUtils.isEmpty(orderPlanMap)) {
            return Collections.emptyList();
        }
        List<PlanSummaryData> planSummaries = new ArrayList<>();
        Set<Integer> processedInvoiceLineIds = new HashSet<>();
        for(Entry<Integer, Integer> orderPlanEntry : orderPlanMap.entrySet()) {
            Integer orderId = orderPlanEntry.getKey();
            PlanSummaryData planSummaryData = new PlanSummaryData();
            Integer planId = orderPlanEntry.getValue();
            planSummaryData.setPlanId(planId);
            planSummaryData.setOrderId(orderId);
            planSummaryData.setNonMediatedLines(collectNonMediatedInvoiceLines(orderId, invoiceLines, assetServiceNumberMfName, orderLineServiceNumberMfName, subscriptionOrderIdMfName, processedInvoiceLineIds));
            OrderDTO planOrder = new OrderDAS().findNow(orderId);
            OrderProcessDTO planOrderProcessDTO = invoiceDTO.getOrderProcessForOrder(planSummaryData.getOrderId());
            Date periodStart = null != planOrderProcessDTO ? planOrderProcessDTO.getPeriodStart() : planOrder.getActiveSince();
            Set<String> assetNumbers = collectPlanOrderAssetsFromAssetAssignment(planOrder, periodStart);
            Set<String> planAssetServiceNumbers = collectAssetServiceNumbersForOrder(orderId, assetServiceNumberMfName);
            for(OrderDTO childOrder : planOrder.getChildOrders()) {
                planAssetServiceNumbers.addAll(collectAssetServiceNumbersForOrder(childOrder.getId(), assetServiceNumberMfName));
                assetNumbers.addAll(childOrder.getAssetIdentifiers());
            }
            planSummaryData.setPlanAssetServiceNumbers(new ArrayList<>(planAssetServiceNumbers));
            Map<String, String> assetIdentiferAndServiceIdMap = collectAssetNumberAndServiceNumberFromInvoiceLine(planSummaryData.getNonMediatedLines(), orderId, assetServiceNumberMfName);
            assetNumbers.addAll(assetIdentiferAndServiceIdMap.keySet());
            logger.debug("assets numbers {} found on order {}", assetNumbers, orderId);
            planSummaryData.setMediatedLines(collectMediatedInvoiceLines(assetNumbers, invoiceLines, planId, processedInvoiceLineIds));
            planSummaryData.setAssetCountOnPlan(planAssetServiceNumbers.size());
            planSummaryData.setPlanInvoiceLines(invoiceLines
                    .stream()
                    .filter(InvoiceLineDTO::isPlanLine)
                    .filter(invoiceLine -> null!= invoiceLine.getOrder())
                    .filter(invoiceLine -> invoiceLine.getOrder().getId().equals(orderId))
                    .collect(Collectors.toList()));
            Set<String> assetServiceNumbers = collectAssetServiceNumbersForOrder(orderId, assetServiceNumberMfName);
            assetServiceNumbers.addAll(assetIdentiferAndServiceIdMap.values());
            planSummaryData.setAssetServiceNumbers(new ArrayList<>(assetServiceNumbers));
            planSummaryData.setAssetNumbers(new ArrayList<>(assetNumbers));
            planSummaries.add(planSummaryData);
        }
        processedInvoiceLineIds.clear();
        return planSummaries;
    }

    private static Map<String,String> collectAssetNumberAndServiceNumberFromInvoiceLine(List<InvoiceLineDTO> invoiceLines,
            Integer planOrderId, String assetServiceNumberMfName) {
        Map<String, String> assetIdentifierAndServiceIdMap = new HashMap<>();
        for(InvoiceLineDTO invoiceLine : invoiceLines) {
            OrderDTO order = invoiceLine.getOrder();
            if(order.getId().equals(planOrderId) || CollectionUtils.isEmpty(order.getAssets())) {
                continue;
            }
            for(AssetDTO asset : order.getAssets()) {
                @SuppressWarnings("unchecked")
                MetaFieldValue<String> assetServiceNumber = asset.getMetaField(assetServiceNumberMfName);
                String identifier = asset.getIdentifier();
                if(null!= assetServiceNumber && StringUtils.isNotEmpty(assetServiceNumber.getValue())) {
                    assetIdentifierAndServiceIdMap.put(identifier, assetServiceNumber.getValue());
                } else {
                    logger.debug("{}'s value not set on  asset {}", assetServiceNumberMfName, asset.getId());
                    assetIdentifierAndServiceIdMap.put(identifier, identifier);
                }
            }
        }
        return assetIdentifierAndServiceIdMap;
    }

    private static List<InvoiceLineDTO> getInvoiceLinesForOrder(Set<InvoiceLineDTO> invoiceLines, Integer orderId) {
        return invoiceLines.stream()
                .filter(invoiceLine -> invoiceLine.getOrder().getId().equals(orderId))
                .collect(Collectors.toList());
    }

    /**
     * Collects non mediated invoice lines.
     * @param planOrderId
     * @param invoiceLines
     * @param assetServiceNumberMfName
     * @param orderLineServiceNumberMfName
     * @param processedInvoiceLineIds
     * @return
     */
    private static List<InvoiceLineDTO> collectNonMediatedInvoiceLines(Integer planOrderId, Set<InvoiceLineDTO> invoiceLines, String assetServiceNumberMfName, String orderLineServiceNumberMfName, String subscriptionOrderIdMfName, Set<Integer> processedInvoiceLineIds) {
        Map<Integer, InvoiceLineDTO> nonMediatedInvoiceIdInvoiceLineMap = new HashMap<>();
        nonMediatedInvoiceIdInvoiceLineMap.putAll(getInvoiceLinesForOrder(invoiceLines, planOrderId).stream()
                .filter(invoiceLine -> !invoiceLine.isPlanLine())
                .collect(Collectors.toMap(InvoiceLineDTO::getId, invoiceLine -> invoiceLine)));
        Set<String> assetServiceNumbers = collectAssetServiceNumbersForOrder(planOrderId, assetServiceNumberMfName);
        for(OrderDTO childOrder : new OrderDAS().findNow(planOrderId).getChildOrders()) {
            assetServiceNumbers.addAll(collectAssetServiceNumbersForOrder(childOrder.getId(), assetServiceNumberMfName));
            nonMediatedInvoiceIdInvoiceLineMap.putAll(getInvoiceLinesForOrder(invoiceLines, childOrder.getId())
                    .stream()
                    .collect(Collectors.toMap(InvoiceLineDTO::getId, invoiceLine -> invoiceLine)));
        }

        for(InvoiceLineDTO invoiceLine : invoiceLines) {
            if(nonMediatedInvoiceIdInvoiceLineMap.containsKey(invoiceLine.getId()) || invoiceLine.isPlanLine() || processedInvoiceLineIds.contains(invoiceLine.getId())) {
                continue;
            }
            OrderDTO order = invoiceLine.getOrder();
            if(isServiceNumberPresentOnOrder(order, assetServiceNumbers, orderLineServiceNumberMfName) &&
                    isSubscriptionOrderIdPresetnOnOrder(order, planOrderId, subscriptionOrderIdMfName)) {
                nonMediatedInvoiceIdInvoiceLineMap.put(invoiceLine.getId(), invoiceLine);
                processedInvoiceLineIds.add(invoiceLine.getId());
            }
        }
        return new ArrayList<>(nonMediatedInvoiceIdInvoiceLineMap.values());
    }

    private static boolean isSubscriptionOrderIdPresetnOnOrder(OrderDTO order, Integer subscriptionOrderId,
            String subscriptionOrderIdMfName) {
        @SuppressWarnings("unchecked")
        MetaFieldValue<Integer> subscriptionOrderIdValue = order.getMetaField(subscriptionOrderIdMfName);
        if(null == subscriptionOrderIdValue || subscriptionOrderIdMfName.isEmpty()) {
            return false;
        }
        return subscriptionOrderIdValue.getValue().equals(subscriptionOrderId);
    }

    /**
     * checks service number on order's order line meta field.
     * @param order
     * @param serviceNumbers
     * @param orderLineServiceNumberMfName
     * @return
     */
    private static boolean isServiceNumberPresentOnOrder(OrderDTO order, Set<String> serviceNumbers, String orderLineServiceNumberMfName) {
        for(OrderLineDTO line : order.getLines()) {
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> orderLineServiceNumber = line.getMetaField(orderLineServiceNumberMfName);
            if(null == orderLineServiceNumber || StringUtils.isEmpty(orderLineServiceNumber.getValue())) {
                continue;
            }
            if(serviceNumbers.contains(orderLineServiceNumber.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects mediated invoice lines from invoice lines.
     * @param assetNumbers
     * @param invoiceLines
     * @param planId
     * @param processedInvoiceLineIds
     * @return
     */
    private static List<InvoiceLineDTO> collectMediatedInvoiceLines(Set<String> assetNumbers, Set<InvoiceLineDTO> invoiceLines, Integer planId, Set<Integer> processedInvoiceLineIds) {
        return invoiceLines
                .stream()
                .filter(invoiceLine -> assetNumbers.contains(invoiceLine.getCallIdentifier()))
                .filter(invoiceLine -> invoiceLine.getOrder().getIsMediated())
                .filter(invoiceLine -> isPlanIdMatched(invoiceLine, planId))
                .filter(invoiceLine -> !processedInvoiceLineIds.contains(invoiceLine.getId()))
                .map(invoiceLine -> addInProcessedInvoiceLinesId(invoiceLine, processedInvoiceLineIds))
                .collect(Collectors.toList());
    }

    private static InvoiceLineDTO addInProcessedInvoiceLinesId(InvoiceLineDTO invoiceLine, Set<Integer> processedInvoiceLineIds) {
        processedInvoiceLineIds.add(invoiceLine.getId());
        return invoiceLine;
    }

    private static boolean isPlanIdMatched(InvoiceLineDTO invoiceLine, Integer planId) {
        Integer invoicePlanId = getPlanIdFromInvoiceLine(invoiceLine);
        return null != invoicePlanId ? invoicePlanId.equals(planId) : false;
    }

    private static Integer getPlanIdFromInvoiceLine(InvoiceLineDTO invoiceLine) {
        String assetIdentifier = invoiceLine.getCallIdentifier();
        OrderDTO order = invoiceLine.getOrder();
        Integer userId = order.getUserId();
        OrderDTO planOrder = new OrderDAS().findOrderByUserAssetIdentifierEffectiveDate(userId, assetIdentifier, order.getActiveSince());
        if (null != planOrder) {
            PlanDTO chargingPlan = planOrder.getPlanFromOrder();
            if (null == chargingPlan && null != planOrder.getParentOrder() && planOrder.getParentOrder().getPeriodId() != Constants.ORDER_PERIOD_ONCE) {
                chargingPlan = planOrder.getParentOrder().getPlanFromOrder();
            }
            if (null != chargingPlan) {
                return chargingPlan.getId();
            }
        }
        return null;
    }

    void setPlanId(Integer planId) {
        this.planId = planId;
    }

    void setAssetCountOnPlan(Integer assetCountOnPlan) {
        this.assetCountOnPlan = assetCountOnPlan;
    }

    void setMediatedLines(List<InvoiceLineDTO> mediatedLines) {
        this.mediatedLines = mediatedLines;
    }

    void setNonMediatedLines(List<InvoiceLineDTO> nonMediatedLines) {
        this.nonMediatedLines = nonMediatedLines;
    }

    Integer getPlanId() {
        return planId;
    }

    List<InvoiceLineDTO> getPlanInvoiceLines() {
        return planInvoiceLines;
    }


    void setPlanInvoiceLines(List<InvoiceLineDTO> planInvoiceLines) {
        this.planInvoiceLines = planInvoiceLines;
    }


    Integer getAssetCountOnPlan() {
        return assetCountOnPlan;
    }

    List<InvoiceLineDTO> getMediatedLines() {
        return mediatedLines;
    }

    List<InvoiceLineDTO> getNonMediatedLines() {
        return nonMediatedLines;
    }

    Integer getOrderId() {
        return orderId;
    }

    void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    List<String> getAssetServiceNumbers() {
        return assetServiceNumbers;
    }

    void setAssetServiceNumbers(List<String> assetServiceNumbers) {
        this.assetServiceNumbers = assetServiceNumbers;
    }

    List<String> getPlanAssetServiceNumbers() {
        return planAssetServiceNumbers;
    }

    void setPlanAssetServiceNumbers(List<String> planAssetServiceNumbers) {
        this.planAssetServiceNumbers = planAssetServiceNumbers;
    }

    List<String> getAssetNumbers() {
        return assetNumbers;
    }

    void setAssetNumbers(List<String> assetNumbers) {
        this.assetNumbers = assetNumbers;
    }

    List<Integer> getAllInvoiceLines() {
        List<InvoiceLineDTO> invoiceLines = new ArrayList<>();
        invoiceLines.addAll(planInvoiceLines);
        invoiceLines.addAll(nonMediatedLines);
        invoiceLines.addAll(mediatedLines);
        return invoiceLines.stream()
                .map(InvoiceLineDTO::getId)
                .collect(Collectors.toList());
    }

    /**
     * Collects Asset service id meta fields for orderId.
     * @param orderId
     * @param assetServiceNumberMfName
     * @return
     */
    private static Set<String> collectAssetServiceNumbersForOrder(Integer orderId, String assetServiceNumberMfName) {
        Set<String> assetServiceNumbers = new HashSet<>();
        OrderDTO order = new OrderDAS().findNow(orderId);
        List<AssetDTO> assets = order.getAssets();
        if(order.isFinished()) { // get assets from asset assignment for the finished order
            for(AssetAssignmentDTO assetAssignmentDTO : order.getAssetAssignments()) {
                assets.add(assetAssignmentDTO.getAsset());
            }
        }
        for(AssetDTO asset : assets) {
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> assetServiceNumber = asset.getMetaField(assetServiceNumberMfName);
            if(null!= assetServiceNumber && StringUtils.isNotEmpty(assetServiceNumber.getValue())) {
                assetServiceNumbers.add(assetServiceNumber.getValue());
            } else {
                logger.debug("{}'s value not set on  asset {}", assetServiceNumberMfName, asset.getId());
                assetServiceNumbers.add(asset.getIdentifier());
            }
        }
        logger.debug("asset service numbers {} for order {}", assetServiceNumbers, orderId);
        return assetServiceNumbers;
    }

    /**
     * Collects Assets from Plan Order and also from Asset Assignments whose assets are released from the Plan Order
     * @param planOrder
     * @param startDate
     * @return set of all the Asset Numbers which are present or released from the orders for the period
     */
    private static Set<String> collectPlanOrderAssetsFromAssetAssignment(OrderDTO planOrder, Date startDate) {
        Set<String> assetNumbers = new HashSet<>(planOrder.getAssetIdentifiers());
        List<AssetAssignmentDTO> assignments = new AssetAssignmentDAS().getAssignmentsForOrder(planOrder.getId());
        Set<String> assetIdentifiers = assignments.stream()
            .filter(aa -> aa.getStartDatetime().compareTo(startDate) <= 0 && (aa.getEndDatetime() == null || aa.getEndDatetime().compareTo(startDate) >= 0))
            .map(AssetAssignmentDTO::getAsset)
            .map(AssetDTO::getIdentifier)
            .collect(Collectors.toSet());
        assetNumbers.addAll(assetIdentifiers);
        logger.debug("asset service numbers {} for finished order {}", assetNumbers, planOrder.getId());
        return assetNumbers;
    }
}
