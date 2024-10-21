package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.ToString;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;
import com.sapienter.jbilling.server.usagePool.event.SwapPlanFUPTransferEvent;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionFeeChargingEvent;
import com.sapienter.jbilling.server.usagePool.task.CustomerPlanSubscriptionHelper;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

@ToString
class FreeUsageQuantityTransferContext {
    CustomerUsagePoolDTO fromCustomerUsagePool;
    BigDecimal quantityToTransfer;
    List<Integer> fromUsagePoolItems;
    Map<CustomerUsagePoolDTO, Integer> fromUsagePoolItemMatchCountNewCustomerUsagePool = new HashMap<>();

    FreeUsageQuantityTransferContext(CustomerUsagePoolDTO fromCustomerUsagePool, BigDecimal addOverage) {
        this.fromCustomerUsagePool = fromCustomerUsagePool;
        quantityToTransfer = fromCustomerUsagePool.getInitialQuantity().subtract(fromCustomerUsagePool.getQuantity())
                .add(addOverage);
        fromUsagePoolItems = fromCustomerUsagePool.getAllItems().stream().map(ItemDTO::getId)
                .collect(Collectors.toList());
    }

    void addNewCustomerUsagePool(CustomerUsagePoolDTO toCustomerUsagePoolDTO) {
        List<Integer> toUsagePoolItems = toCustomerUsagePoolDTO.getAllItems().stream().map(ItemDTO::getId)
                .collect(Collectors.toList());
        List<Integer> commonItems = new ArrayList<>(fromUsagePoolItems);
        commonItems.retainAll(toUsagePoolItems);
        if (CollectionUtils.isNotEmpty(commonItems)) {
            fromUsagePoolItemMatchCountNewCustomerUsagePool.put(toCustomerUsagePoolDTO, commonItems.size());
        }
    }

    SortedMap<Integer, List<CustomerUsagePoolDTO>> getItemMatchCountNewCustomerUsagePoolMap() {
        SortedMap<Integer, List<CustomerUsagePoolDTO>> itemMatchCountCustomerUsagePoolMap = new TreeMap<>(
                Comparator.reverseOrder());
        for (Entry<CustomerUsagePoolDTO, Integer> entry : fromUsagePoolItemMatchCountNewCustomerUsagePool.entrySet()) {
            itemMatchCountCustomerUsagePoolMap.putIfAbsent(entry.getValue(), new LinkedList<>());
            List<CustomerUsagePoolDTO> toCustomerUsagePools = itemMatchCountCustomerUsagePoolMap.get(entry.getValue());
            toCustomerUsagePools.add(entry.getKey());
        }
        return itemMatchCountCustomerUsagePoolMap;
    }

}

public class SPCSwapPlanFUPTransferTask extends PluggableTask implements IInternalEventsTask {

    private static final ParameterDescription PARAM_MOBILE_DATA_CREDIT_PRODUCT_CODE = new ParameterDescription(
            "Mobile Data Credit Product Code", false, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_MOBILE_DATA_DEBIT_PRODUCT_CODE = new ParameterDescription(
            "Mobile Data Debit Product Code", false, ParameterDescription.Type.STR);

    private static final ParameterDescription RERATE_PRODUCT_IDS = new ParameterDescription("Rerate Product Ids",
            false, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_SPC_ITEM_LIST = new ParameterDescription("spc_item_list", true,
            ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_PRODUCT_CODE_SUB_STRING_TO_FETCH_PRICE_FROM_RATE_CARD = new ParameterDescription(
            "Product Code to Fetch Price from Rate Card", true, ParameterDescription.Type.STR);

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String SERVICE_ID = "ServiceId";
    static final String SUBSCRIPTION_ORDER_ID = "Subscription Order Id";

    public SPCSwapPlanFUPTransferTask() {
        descriptions.add(PARAM_MOBILE_DATA_CREDIT_PRODUCT_CODE);
        descriptions.add(PARAM_MOBILE_DATA_DEBIT_PRODUCT_CODE);
        descriptions.add(RERATE_PRODUCT_IDS);
        descriptions.add(PARAM_SPC_ITEM_LIST);
        descriptions.add(PARAM_PRODUCT_CODE_SUB_STRING_TO_FETCH_PRICE_FROM_RATE_CARD);
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        /*String itemListStr = getParameter(PARAM_SPC_ITEM_LIST.getName(), StringUtils.EMPTY);
        List<Integer> itemList = Stream.of(itemListStr.split(",")).map(s -> s = s.trim()).filter(NumberUtils::isDigits)
                .map(Integer::parseInt).collect(Collectors.toList());*/

        SwapPlanFUPTransferEvent swapPlanFUPTransferEvent = (SwapPlanFUPTransferEvent) event;
        logger.debug("Swap Plan Transfer Event {}", swapPlanFUPTransferEvent);
        Integer existingPlanItemId = swapPlanFUPTransferEvent.getExistingPlanItemId();
        OrderDTO planOrder = swapPlanFUPTransferEvent.getOrder();
        PlanDAS planDAS = new PlanDAS();
        PlanDTO existingPlan = planDAS.findPlanByItemId(existingPlanItemId);
        CustomerUsagePoolDAS customerUsagePoolDAS = new CustomerUsagePoolDAS();
        // fetch old customer usage pools.
        List<CustomerUsagePoolDTO> oldCustomerUsagePools = customerUsagePoolDAS.getCustomerUsagePoolsByOrderAndPlanId(
                planOrder.getId(), existingPlan.getId());
        // sort based on preference or created date if preference is same
        Collections.sort(oldCustomerUsagePools,
                CustomerUsagePoolDTO.CustomerUsagePoolsByPrecedenceOrCreatedDateComparator);
        // create new customer usage pool.
        for (CustomerPlanSubscriptionEvent customerPlanSubscriptionEvent : CustomerPlanSubscriptionHelper
                .generateCustomerPlanSubscriptionEvents(planOrder)) {
            CustomerPlanSubscriptionHelper.createCustomerUsagePool(customerPlanSubscriptionEvent, false);
        }

        Integer newPlanItemId = swapPlanFUPTransferEvent.getSwapPlanItemId();
        PlanDTO newPlan = planDAS.findPlanByItemId(newPlanItemId);
        List<CustomerUsagePoolDTO> newCustomerUsagePools = customerUsagePoolDAS.getCustomerUsagePoolsByOrderAndPlanId(
                planOrder.getId(), newPlan.getId());
        // sort based on preference or created date if preference is same
        Collections.sort(newCustomerUsagePools,
                CustomerUsagePoolDTO.CustomerUsagePoolsByPrecedenceOrCreatedDateComparator);
        BigDecimal newCustomerUsagePoolQuantityAvailableForPreviousPeriod = BigDecimal.ZERO;
        BigDecimal newCustomerUsagePoolQuantityAvailableForCurrentPeriod = BigDecimal.ZERO;

        UserDTO user = planOrder.getUser();
        Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();

        renewCustomerUsagePoolsPerNewPlan(user, oldCustomerUsagePools, newCustomerUsagePools);

        newCustomerUsagePools = customerUsagePoolDAS.getCustomerUsagePoolsByOrderAndPlanId(
                planOrder.getId(), newPlan.getId());

        List<CustomerUsagePoolDTO> previousPeriodActivePools = new ArrayList<>();
        previousPeriodActivePools = newCustomerUsagePools.stream().filter(pool ->
            (pool.getCycleStartDate().compareTo(nextInvoiceDate) < 0)).collect(Collectors.toList());
        newCustomerUsagePoolQuantityAvailableForPreviousPeriod = getCustomerPoolTotalQuantity(previousPeriodActivePools);

        List<CustomerUsagePoolDTO> currentPeriodActivePools = new ArrayList<>();
        currentPeriodActivePools = newCustomerUsagePools.stream().filter(pool ->
            (pool.getCycleStartDate().compareTo(nextInvoiceDate) >= 0)).collect(Collectors.toList());
        newCustomerUsagePoolQuantityAvailableForCurrentPeriod = getCustomerPoolTotalQuantity(currentPeriodActivePools);

        // expire old Customer Usage pools.
        for (CustomerUsagePoolDTO oldCustomerUsagePool : oldCustomerUsagePools) {
            // To expire FUP set cycle end date as 1970-01-01
            oldCustomerUsagePool.expire();
        }

        //Commented as per SPC's existing charging system
        /*if (!newCustomerUsagePools.isEmpty()) {
            OrderDAS orderDas = new OrderDAS();
            SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
            List<OrderDTO> activeDataBoostOrders = spcHelperService.findActiveDataBoostFeeOrders(order.getId(), itemList);
            for (OrderDTO orderDto : activeDataBoostOrders) {
                for (OrderLineDTO lineDto : orderDto.getLines()) {
                    lineDto.setDeleted(1);
                }
                orderDto.setDeleted(1);
                orderDas.save(orderDto);
                orderDas.flush();
            }
        }*/

        BigDecimal totalUsageQuantityForPreviousPeriod = getTotalUsageQuantity(planOrder, false);
        if (totalUsageQuantityForPreviousPeriod.compareTo(BigDecimal.ZERO) > 0) {
            if (CollectionUtils.isNotEmpty(previousPeriodActivePools)) {
                tranferUsagetoNewCustomerPool(planOrder, previousPeriodActivePools, false);
            }
            produceAdjustmentLineForDataUsage(planOrder, newCustomerUsagePoolQuantityAvailableForPreviousPeriod, false);
        }

        BigDecimal totalUsageQuantityForCurrentPeriod = getTotalUsageQuantity(planOrder, true);
        if (totalUsageQuantityForCurrentPeriod.compareTo(BigDecimal.ZERO) > 0) {
            if (CollectionUtils.isNotEmpty(currentPeriodActivePools)) {
                tranferUsagetoNewCustomerPool(planOrder, currentPeriodActivePools, true);
            }
            produceAdjustmentLineForDataUsage(planOrder, newCustomerUsagePoolQuantityAvailableForCurrentPeriod, true);
        }
    }

    private void tranferUsagetoNewCustomerPool(OrderDTO planOrder, List<CustomerUsagePoolDTO> newCustomerUsagePools, boolean isNewPeriod) throws PluggableTaskException {
        Collections.sort(newCustomerUsagePools,
                CustomerUsagePoolDTO.CustomerUsagePoolsByPrecedenceOrCreatedDateComparator);
        BigDecimal totalUsageQuantity = getTotalUsageQuantity(planOrder, isNewPeriod);
        for (CustomerUsagePoolDTO customerUsagePoolDTO : newCustomerUsagePools) {
            logger.debug("totalUsageQuantity of mediated Orders{}: " + totalUsageQuantity);
            logger.debug("customerUsagePoolDTO{} : " + customerUsagePoolDTO);
            BigDecimal newCustomerUsagePoolQuantity = customerUsagePoolDTO.getQuantity();
            if (newCustomerUsagePoolQuantity.compareTo(BigDecimal.ZERO) > 0) {
                logger.debug("newCustomerUsagePoolQuantity of customer pool{} : "+newCustomerUsagePoolQuantity);
                if (newCustomerUsagePoolQuantity.compareTo(totalUsageQuantity) >= 0) {
                    newCustomerUsagePoolQuantity = newCustomerUsagePoolQuantity.subtract(totalUsageQuantity);
                    totalUsageQuantity = BigDecimal.ZERO;
                    logger.debug("in if newCustomerUsagePoolQuantity{} : " + newCustomerUsagePoolQuantity);
                } else {
                    totalUsageQuantity = totalUsageQuantity.subtract(newCustomerUsagePoolQuantity);
                    newCustomerUsagePoolQuantity = BigDecimal.ZERO;
                    logger.debug("in else totalUsageQuantity{} : " + totalUsageQuantity);
                }
                customerUsagePoolDTO.setQuantity(newCustomerUsagePoolQuantity);

                UsagePoolDTO usagePoolDTO = customerUsagePoolDTO.getUsagePool();
                if (customerUsagePoolDTO.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    logger.debug("chargeDataBoostFeesPerNewPlan....{}: " + usagePoolDTO);
                    OrderDAS orderDas = new OrderDAS();
                    List<String> assetIdentifiers = planOrder.getAssetIdentifiers();
                    Date activeSince = null;
                    for (String assetIdentifier : assetIdentifiers) {
                        List<OrderDTO> activeMediatedOrders = orderDas.findActiveMediatedOrdersByUserIdAndAssetIdentifier(
                                planOrder.getUserId(), assetIdentifier, planOrder.getUser().getCustomer().getNextInvoiceDate(), isNewPeriod);
                        for (OrderDTO mediatedOrder : activeMediatedOrders) {
                            activeSince = mediatedOrder.getActiveSince();
                            break;
                        }
                    }
                    chargeDataBoostFeesPerNewPlan(customerUsagePoolDTO, activeSince);
                }
                if (totalUsageQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    logger.debug("in break totalUsageQuantity{}: " + totalUsageQuantity);
                    break;
                }
            }
        }
    }

    private void renewCustomerUsagePoolsPerNewPlan(UserDTO user, List<CustomerUsagePoolDTO> oldCustomerUsagePools,
            List<CustomerUsagePoolDTO> newCustomerUsagePools) {
        List<CustomerUsagePoolDTO> currentPeriodPools = oldCustomerUsagePools.stream().filter(pool ->
            (pool.getCycleStartDate().compareTo(user.getCustomer().getNextInvoiceDate()) >= 0)).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(currentPeriodPools)) {
            for (CustomerUsagePoolDTO customerUsagePool : newCustomerUsagePools) {
                // creating new customer usage pool.
                CustomerUsagePoolDTO newCustomerUsagePool = new CustomerUsagePoolDTO();
                CustomerDTO customer = customerUsagePool.getCustomer();
                newCustomerUsagePool.setCustomer(customer);
                UsagePoolDTO usagePool = customerUsagePool.getUsagePool();
                BigDecimal usagePoolQuantity = usagePool.getQuantity();
                OrderDTO subScriptionOrder = customerUsagePool.getOrder();
                newCustomerUsagePool.setInitialQuantity(usagePoolQuantity);
                newCustomerUsagePool.setQuantity(usagePoolQuantity);
                newCustomerUsagePool.setLastRemainingQuantity(BigDecimal.ZERO);
                newCustomerUsagePool.setUsagePool(usagePool);
                newCustomerUsagePool.setPlan(customerUsagePool.getPlan());
                newCustomerUsagePool.setOrder(subScriptionOrder);

                Date cycleEndDate = customerUsagePool.getCycleEndDate();
                Calendar cal = Calendar.getInstance();

                cal.setTime(cycleEndDate);
                cal.add(Calendar.DATE, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                cycleEndDate = cal.getTime();
                Date subscriptionStartDate = cycleEndDate;
                //Set cycle start date
                newCustomerUsagePool.setCycleStartDate(subscriptionStartDate);
                String cyclePeriodUnit = usagePool.getCyclePeriodUnit();

                logger.debug("cyclePeriodUnit: {}", cyclePeriodUnit);

                MainSubscriptionDTO mainSubscriptionDTO = customer.getMainSubscription();
                Date orderActiveUntil = subScriptionOrder.getActiveUntil();
                CustomerUsagePoolBL customerUsagePoolBL = new CustomerUsagePoolBL();
                cycleEndDate = customerUsagePoolBL.getCycleEndDateForPeriod(cyclePeriodUnit, usagePool.getCyclePeriodValue(),
                        subscriptionStartDate, mainSubscriptionDTO.getSubscriptionPeriod(), orderActiveUntil);
                newCustomerUsagePool.setCycleEndDate(cycleEndDate);
                // saving new customer usage pool.
                newCustomerUsagePool = new CustomerUsagePoolDAS().save(newCustomerUsagePool);
                logger.debug("new usage pool {} created for customer {} from {} to {} period", newCustomerUsagePool.getId(), user.getId(),
                        newCustomerUsagePool.getCycleStartDate(), newCustomerUsagePool.getCycleEndDate());
            }
        }
    }

    private BigDecimal getTotalUsageQuantity(OrderDTO order, boolean isNewPeriod) throws PluggableTaskException {
        OrderDAS orderDas = new OrderDAS();
        List<String> assetIdentifiers = order.getAssetIdentifiers();
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (String assetIdentifier : assetIdentifiers) {
            List<OrderDTO> activeMediatedOrders = orderDas.findActiveMediatedOrdersByUserIdAndAssetIdentifier(
                    order.getUserId(), assetIdentifier, order.getUser().getCustomer().getNextInvoiceDate(), isNewPeriod);
            for (OrderDTO mediatedOrder : activeMediatedOrders) {
                for (OrderLineDTO line : mediatedOrder.getLines()) {
                    if(!assetIdentifier.equals(line.getCallIdentifier())) {
                        logger.debug("skipped assetIdentifier in getTotalUsageQuantity{}: " + assetIdentifier);
                        logger.debug("skipped line.getCallIdentifier() in getTotalUsageQuantity{}: " + line.getCallIdentifier());
                        continue;
                    }
                    if (null != line.getItem()
                            && isMobileDataOrGPRSProducts(getRerateProductIds(), new Integer(line.getItem().getId()))) {
                        totalQuantity = totalQuantity.add(line.getQuantity());
                    }
                }
            }
        }
        return totalQuantity;
    }

    private BigDecimal getCustomerPoolTotalQuantity(List<CustomerUsagePoolDTO> customerUsagePools) {
        BigDecimal totalInitialQuantity = BigDecimal.ZERO;
        for (CustomerUsagePoolDTO customerUsagePool : customerUsagePools) {
            totalInitialQuantity = totalInitialQuantity.add(customerUsagePool.getInitialQuantity());
        }
        return totalInitialQuantity;
    }

    private void chargeDataBoostFeesPerNewPlan(CustomerUsagePoolDTO customerUsagePool, Date activeSince) {
        customerUsagePool
                .getUsagePool()
                .getConsumptionActions()
                .stream()
                .filter(consumptionAction -> consumptionAction.getType().equals(Constants.FUP_CONSUMPTION_FEE))
                .forEach(
                        consumptionAction -> EventManager.process(new UsagePoolConsumptionFeeChargingEvent(
                                getEntityId(), customerUsagePool.getId(), consumptionAction, activeSince)));
    }

    private Map<String, Integer> getAdjustmentProductIds(Integer entityId) throws PluggableTaskException {
        String MOBILE_DATA_CREDIT_PRODUCT_CODE = getMandatoryStringParameter(PARAM_MOBILE_DATA_CREDIT_PRODUCT_CODE
                .getName());
        String MOBILE_DATA_DEBIT_PRODUCT_CODE = getMandatoryStringParameter(PARAM_MOBILE_DATA_DEBIT_PRODUCT_CODE
                .getName());
        ItemDTO itemCredit = null;
        ItemDTO itemDebit = null;
        ItemDAS itemDAS = new ItemDAS();
        Map<String, Integer> adjustmentProductIds = new HashMap<>();
        itemCredit = itemDAS.findItemByInternalNumber(MOBILE_DATA_CREDIT_PRODUCT_CODE, entityId);
        itemDebit = itemDAS.findItemByInternalNumber(MOBILE_DATA_DEBIT_PRODUCT_CODE, entityId);

        if (itemCredit == null) {
            logger.error("No Credit product available");
            throw new SessionInternalError("No Credit product available");
        }
        if (itemDebit == null) {
            logger.error("No Debit product available");
            throw new SessionInternalError("No Debit product available");
        }
        adjustmentProductIds.put("Credit Product", itemCredit.getId());
        adjustmentProductIds.put("Debit Product", itemDebit.getId());
        return adjustmentProductIds;
    }
    
    private void deleteExistingAdjustmentOrders(Integer subscriptionOrderId, Map<String, Integer> adjustmentProductIds, Date nextInvoiceDate, boolean isNewPeriod) {
        //delete existing adjustment orders
        OrderDAS orderDas = new OrderDAS();
        SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
        List<OrderDTO> activeAdjustmentOrders = spcHelperService.findActiveDataBoostFeeOrAdjustmentOrders(subscriptionOrderId,
                Arrays.asList(adjustmentProductIds.get("Credit Product"), adjustmentProductIds.get("Debit Product")), nextInvoiceDate, isNewPeriod);
        for (OrderDTO orderDto : activeAdjustmentOrders) {
            logger.error("deleting existing adjustment order {}", orderDto.getId());
            for (OrderLineDTO lineDto : orderDto.getLines()) {
                lineDto.setDeleted(1);
            }
            orderDto.setDeleted(1);
            orderDas.save(orderDto);
            orderDas.flush();
        }
    }

    private String getRerateProductIds() throws PluggableTaskException {
        return getMandatoryStringParameter(RERATE_PRODUCT_IDS.getName());
    }

    private void produceAdjustmentLineForDataUsage(OrderDTO subscriptionOrder, BigDecimal freepoolQuantityAvailable, boolean isNewPeriod)
            throws PluggableTaskException, SessionInternalError {
        OrderDAS orderDas = new OrderDAS();
        List<String> assetIdentifiers = subscriptionOrder.getAssetIdentifiers();
        UserDTO user = subscriptionOrder.getUser();
        Integer userId = user.getId();
        Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();

        Map<String, Integer> adjustmentProductIds = getAdjustmentProductIds(subscriptionOrder.getUser().getEntity().getId());
        deleteExistingAdjustmentOrders(subscriptionOrder.getId(), adjustmentProductIds, nextInvoiceDate, isNewPeriod);

        for (String assetIdentifier : assetIdentifiers) {
            logger.debug("current assetIdentifier: {}", assetIdentifier);
            List<OrderDTO> activeMediatedOrders = orderDas.findActiveMediatedOrdersByUserIdAndAssetIdentifier(
                    userId, assetIdentifier,nextInvoiceDate, isNewPeriod);
            BigDecimal adjustmentAmount = BigDecimal.ZERO;
            BigDecimal newOrderLineTotal = BigDecimal.ZERO;
            BigDecimal lineQuantityConsumption = BigDecimal.ZERO;
            BigDecimal oldOrderLineTotal = BigDecimal.ZERO;
            OrderDTO mediatedOrder = null;
            for (OrderDTO order : activeMediatedOrders) {
                mediatedOrder = order;
                for (OrderLineDTO line : order.getLines()) {
                    if(!assetIdentifier.equals(line.getCallIdentifier())) {
                        logger.debug("skipped assetIdentifier: {}", assetIdentifier);
                        logger.debug("skipped line.getCallIdentifier(): {}", line.getCallIdentifier());
                        continue;
                    }
                    
                    // -- mobile data or GPRS
                    if (null != line.getItem()
                            && isMobileDataOrGPRSProducts(getRerateProductIds(), new Integer(line.getItem().getId()))) {
                        lineQuantityConsumption = lineQuantityConsumption.add(line.getQuantity());
                        oldOrderLineTotal = oldOrderLineTotal.add(line.getAmount());

                        BigDecimal resolvedPrice = getRateForPlanItem(subscriptionOrder.getPlanFromOrder(), user);
                        logger.debug("resolvedPrice {} for ajdustment line {}", resolvedPrice, line.getId());

                        BigDecimal itemUsageQuantityToBeCharged = BigDecimal.ZERO;
                        BigDecimal itemfreepoolRemaining = freepoolQuantityAvailable.subtract(lineQuantityConsumption, MathContext.DECIMAL128);

                        if (itemfreepoolRemaining != null && itemfreepoolRemaining.compareTo(BigDecimal.ZERO) >= 0) {

                            // Since free poll is remaining ,this value is free & not to be charged.
                            newOrderLineTotal = BigDecimal.ZERO;
                            // Recalculate the freepoolQuantityAvailable
                            freepoolQuantityAvailable = itemfreepoolRemaining;

                        } else if (itemfreepoolRemaining != null
                                && itemfreepoolRemaining.compareTo(BigDecimal.ZERO) < 0) {
                            // Recalculate the item usage quantity & Amount as per rate card
                            itemUsageQuantityToBeCharged = itemfreepoolRemaining.negate();
                            newOrderLineTotal = itemUsageQuantityToBeCharged.multiply(resolvedPrice, MathContext.DECIMAL128);
                            // freepoolQuantityAvailable is exhausted
                            freepoolQuantityAvailable = BigDecimal.ZERO;
                        }
                        // Delta Adjustment Amount
                        newOrderLineTotal = newOrderLineTotal.setScale(Constants.BIGDECIMAL_SCALE,
                                Constants.BIGDECIMAL_ROUND);
                        adjustmentAmount = newOrderLineTotal.subtract(oldOrderLineTotal, MathContext.DECIMAL128);
                        adjustmentAmount = adjustmentAmount.setScale(Constants.BIGDECIMAL_SCALE,
                                Constants.BIGDECIMAL_ROUND);
                    }
                }
            }
            String serviceId = collectAssetServiceIDsForOrder(subscriptionOrder.getId());
            if (adjustmentAmount.compareTo(BigDecimal.ZERO) < 0) {
                createOrder(user, adjustmentProductIds.get("Credit Product"), adjustmentAmount, subscriptionOrder.getId(),
                        serviceId, "Mobile Data Credit", mediatedOrder.getActiveSince());
            } else if (adjustmentAmount.compareTo(BigDecimal.ZERO) > 0) {
                createOrder(user, adjustmentProductIds.get("Debit Product"), adjustmentAmount, subscriptionOrder.getId(),
                        serviceId, "Mobile Data Debit", mediatedOrder.getActiveSince());
            }
        }
    }

    private BigDecimal getRateForPlanItem(PlanDTO planDto, UserDTO user) throws PluggableTaskException {
        SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
        String routeName = (String) planDto.getMetaField(SPCConstants.PLAN_RATING).getValue();
        if (StringUtils.isEmpty(routeName)) {
            throw new SessionInternalError("No Plan rating found on plan: " + planDto.getId());
        }
        String tablename = "route_rate_"+user.getEntity().getId()+"_"+routeName.toLowerCase();
        spcHelperService.isTablePresent(tablename);
        String productCode = getMandatoryStringParameter(PARAM_PRODUCT_CODE_SUB_STRING_TO_FETCH_PRICE_FROM_RATE_CARD.getName());
        return spcHelperService.getRateForPlanItem(productCode, tablename);
    }

    private boolean isMobileDataOrGPRSProducts(String rerateProductIds, Integer productId) {
        StringTokenizer st = new StringTokenizer(rerateProductIds, ",");
        while (st.hasMoreTokens()) {  
            String product = st.nextToken();
            if (product.equalsIgnoreCase(productId.toString())) {
                return true;
            }
        }
        return false;
    }

    private Integer createOrder(UserDTO user, Integer itemId,BigDecimal amount, Integer subscriptionOrderId,
            String serviceId, String orderLineDesc, Date activeSince) throws PluggableTaskException {
        OrderDTO adjustmentOrder = new OrderDTO();
        adjustmentOrder.setBaseUserByUserId(user);
        OrderPeriodDTO period = new OrderPeriodDTO();
        period.setId(Constants.ORDER_PERIOD_ONCE);
        adjustmentOrder.setOrderPeriod(period);
        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(Constants.ORDER_BILLING_POST_PAID);
        adjustmentOrder.setOrderBillingType(type);
        adjustmentOrder.setCreateDate(TimezoneHelper.companyCurrentDate(getEntityId()));
        adjustmentOrder.setCurrency(user.getCurrency());
        adjustmentOrder.setNotes("Adjustment Order Created during Plan Swap for Subscription Order "
                + subscriptionOrderId);
        adjustmentOrder.setMetaField(getEntityId(), null, SUBSCRIPTION_ORDER_ID, subscriptionOrderId);
        adjustmentOrder.setActiveSince(activeSince);

        OrderLineDTO adjustmentLine = new OrderLineDTO();

        adjustmentLine.setQuantity(BigDecimal.ONE);
        adjustmentLine.setPrice(amount);
        adjustmentLine.setAmount(amount);

        adjustmentLine.setTypeId(Constants.ORDER_LINE_TYPE_ADJUSTMENT);
        adjustmentLine.setDescription(orderLineDesc);
        adjustmentLine.setUseItem(Boolean.FALSE);
        adjustmentLine.setItemId(itemId);
        MetaFieldHelper.setMetaField(getEntityId(), adjustmentLine, SERVICE_ID, serviceId);

        // Adding Line on Order
        adjustmentOrder.getLines().add(adjustmentLine);
        OrderBL orderBL = new OrderBL(adjustmentOrder);

        // Creating Adjustment Order
        return orderBL.create(getEntityId(), null, adjustmentOrder);

    }

    private String collectAssetServiceIDsForOrder(Integer oldOrderId) {
        OrderDTO oldOrder = new OrderDAS().findNow(oldOrderId);
        String assetServiceID = null;
        for (AssetDTO asset : oldOrder.getAssets()) {
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> assetServiceNumber = asset.getMetaField(SERVICE_ID);
            if (null != assetServiceNumber && StringUtils.isNotEmpty(assetServiceNumber.getValue())) {

                assetServiceID = assetServiceNumber.getValue();
            } else {
                assetServiceID = asset.getIdentifier();
            }
        }
        return assetServiceID;
    }

    private List<String> getPricingFields(Integer orderId, Integer orderLineId) {

        MediationService service = Context.getBean(MediationService.BEAN_NAME);
        return service.getPricingFields(orderId, orderLineId);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] { SwapPlanFUPTransferEvent.class };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

}
