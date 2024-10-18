package com.sapienter.jbilling.server.usagePool.task;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
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
import com.sapienter.jbilling.server.usagePool.db.SwapPlanHistoryDAS;
import com.sapienter.jbilling.server.usagePool.db.SwapPlanHistoryDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;
import com.sapienter.jbilling.server.usagePool.event.SwapPlanFUPTransferEvent;
import com.sapienter.jbilling.server.usagePool.task.UsageOrderReRater.UsagePeriod;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;

/**
 * This plugin processes SwapPlanFUPTransferEvent and transfers quantity utilised from old plan to new plan.
 *
 * @author Mahesh Shivarkar
 * @since 23-Mar-2015
 */

public class SwapPlanFUPTransferTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_CREDIT_PRODUCT =
            new ParameterDescription("credit_product_id", false, ParameterDescription.Type.INT);

    private static final ParameterDescription PARAM_DEBIT_PRODUCT =
            new ParameterDescription("debit_product_id", false, ParameterDescription.Type.INT);

    private static final String STR_SPACE = " ";

    public SwapPlanFUPTransferTask() {
        descriptions.add(PARAM_CREDIT_PRODUCT);
        descriptions.add(PARAM_DEBIT_PRODUCT);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {SwapPlanFUPTransferEvent.class};

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private Optional<OrderLineDTO> getSubScriptionLineByItemId(OrderDTO order, Integer itemId) {
        return order.getLines()
                .stream()
                .filter(line -> itemId.equals(line.getItemId()))
                .findFirst();
    }

    private BigDecimal getAmountFromOrderByPlan(OrderDTO orderDTO, Integer planId) throws PluggableTaskException {
        Optional<OrderLineDTO> subscriptionLine = getSubScriptionLineByItemId(orderDTO, planId);
        if(!subscriptionLine.isPresent()) {
            for(OrderDTO childOrder : orderDTO.getChildOrders()) {
                if(childOrder.getOrderPeriod().getId()!= Constants.ORDER_PERIOD_ONCE) {
                    logger.debug("Fetching subscription line from child order {}", childOrder.getId());
                    subscriptionLine = getSubScriptionLineByItemId(childOrder, planId);
                    if(subscriptionLine.isPresent()) {
                        break;
                    }
                }
            }
        }
        if(!subscriptionLine.isPresent()) {
            throw new PluggableTaskException("subscription item "+ planId + " not found on order "+ orderDTO.getId());
        }
        return subscriptionLine.get().getAmount();
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        SwapPlanFUPTransferEvent swapPlanFUPTransferEvent = (SwapPlanFUPTransferEvent) event;
        Integer existingPlanItemId = swapPlanFUPTransferEvent.getExistingPlanItemId();
        Integer swapPlanItemId = swapPlanFUPTransferEvent.getSwapPlanItemId();
        OrderDTO orderDto = new OrderDAS().find(swapPlanFUPTransferEvent.getOrder().getId());
        Integer userId = orderDto.getUserId();
        CustomerDTO customerDto = new UserBL(userId).getEntity().getCustomer();
        logger.debug("Swap Plan Transfer Event {}", swapPlanFUPTransferEvent);

        PlanDTO existingPlan = new PlanDAS().findPlanByItemId(existingPlanItemId);
        BigDecimal oldPlanAmount = getAmountFromOrderByPlan(orderDto, existingPlanItemId);
        // Expire customer usage pools from existing plan
        expireOldCustomerUsagePools(customerDto, existingPlan, orderDto.getId());

        // Call the subscription events to create new customer usage pools
        processCustomerPlanSubscription(orderDto);

        BigDecimal newPlanAmount = getAmountFromOrderByPlan(orderDto, swapPlanItemId);

        // get the new customer usage pools
        List<CustomerUsagePoolDTO> newCustomerUsagePools = new CustomerUsagePoolBL().getCustomerUsagePoolsByCustomerId(customerDto.getId());
        CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
        for(CustomerUsagePoolDTO freeUsagePool: newCustomerUsagePools) {
            freeUsagePool.setQuantity(freeUsagePool.getInitialQuantity());
            das.save(freeUsagePool);
        }

        Map<UsagePeriod, Date> periodMap = UsageOrderReRater.getBillablePeriodFromSubscriptionOrder(orderDto);

        Date billingCycleStart = periodMap.get(UsagePeriod.CYCLE_START_DATE);
        Date billingCycleEnd = periodMap.get(UsagePeriod.CYCLE_END_DATE);
        reRateUsageOrderAndCreateSwapPlanHistory(userId, billingCycleStart, billingCycleEnd, orderDto,
                swapPlanFUPTransferEvent);

        // create adjustment order
        Date effectiveDate = swapPlanFUPTransferEvent.getEffectiveDate();
        Date nextBillableDay = orderDto.getNextBillableDay();
        if(Constants.ORDER_BILLING_PRE_PAID.equals(orderDto.getBillingTypeId())
                && nextBillableDay!=null && effectiveDate.before(nextBillableDay)) {
            Integer adjustmentOrderId = createAdjustMentDebitOrCreditOrder(userId, billingCycleStart, oldPlanAmount.subtract(newPlanAmount).negate(),
                    orderDto.getId(),swapPlanFUPTransferEvent);

            logger.debug("Adjustment Order created {}", adjustmentOrderId);
        }
    }

    private SwapPlanHistoryDTO reRateUsageOrderAndCreateSwapPlanHistory(Integer userId, Date billingCycleStart, Date billingCycleEnd,
            OrderDTO orderDto, SwapPlanFUPTransferEvent event) {
        PlanDTO existingPlan = new PlanDAS().findPlanByItemId(event.getExistingPlanItemId());
        PlanDTO swapPlan = new PlanDAS().findPlanByItemId(event.getSwapPlanItemId());
        Integer entityId = new UserDAS().find(userId).getEntity().getId();
        SwapPlanHistoryDTO swapPlanHistory = new SwapPlanHistoryDTO(existingPlan.getId(), swapPlan.getId(), orderDto.getId());
        BigDecimal oldPlanOverageQuantity = BigDecimal.ZERO;
        BigDecimal oldPlanOverageAmount = BigDecimal.ZERO;
        BigDecimal oldPlanUsedfreeQuantity = BigDecimal.ZERO;

        OrderDAS orderDAS = new OrderDAS();
        for (Integer orderId : orderDAS.getCustomersAllOneTimeUsageOrdersInCurrentBillingCycle(userId, billingCycleStart, billingCycleEnd,
        		OrderStatusFlag.INVOICE)) {
            OrderDTO usageOrder = orderDAS.find(orderId);
            oldPlanOverageQuantity = oldPlanOverageQuantity.add(usageOrder.getTotalOrderLineQuantity());
            oldPlanOverageAmount = oldPlanOverageAmount.add(usageOrder.getTotal());
            oldPlanUsedfreeQuantity = oldPlanUsedfreeQuantity.add(usageOrder.getFreeUsagePoolsTotalQuantity());
            List<OrderLineDTO> oldLines = OrderHelper.copyOrderLinesToDto(usageOrder.getLines());
            OrderBL orderBL = new OrderBL(usageOrder);

            for(OrderLineDTO orderLine : usageOrder.getLines()) {
                ItemDTO itemDto = orderLine.getItem();

                if (null != itemDto && !swapPlan.doesPlanHaveItem(itemDto.getId())
                        && !orderLine.isLineTypeItem() && !orderLine.getUseItem()) {
                    continue;
                }
                orderLine.clearOrderLineUsagePools();
                orderLine.setQuantity(orderLine.getQuantity().add(new BigDecimal(com.sapienter.jbilling.common.Constants.NEW_QUANTITY)));
                orderBL.processLine(orderLine, orderDto.getUser().getLanguageIdField(), entityId, userId, orderDto.getCurrencyId(), null);
                orderBL.checkOrderLineQuantities(Arrays.asList(getLineById(oldLines, orderLine.getId())), Arrays.asList(orderLine), entityId, orderId, true, false);
                orderBL.recalculate(entityId);
            }

        }

        swapPlanHistory.setOldPlanOverageAmount(oldPlanOverageAmount);
        swapPlanHistory.setOldPlanOverageQuantity(oldPlanOverageQuantity.subtract(oldPlanUsedfreeQuantity));
        swapPlanHistory.setOldPlanUsedfreeQuantity(oldPlanUsedfreeQuantity);
        swapPlanHistory = new SwapPlanHistoryDAS().save(swapPlanHistory);
        logger.debug("Saved SwapPlanHistory {}", swapPlanHistory);
        return swapPlanHistory;
    }

    private ResourceBundle getUserResourceBundle(UserDTO user) {
        try {
            return ResourceBundle.getBundle("entityNotifications", user.getLanguage().asLocale());
        } catch (Exception e) {
            throw new SessionInternalError("Error ", SwapPlanFUPTransferTask.class, e);
        }
    }

    /**
     * Creates Adjustment Debit/Credit Order for pre-paid subscription
     * @param userId
     * @param billingCycleStart
     * @param billingCycleEnd
     */
    private Integer createAdjustMentDebitOrCreditOrder(Integer userId, Date billingCycleStart, BigDecimal changedAmount, Integer subscriptionOrderId, SwapPlanFUPTransferEvent event) {

        logger.debug("Creating Adjustment Order For user {} for  ActiveSinceDate {}", userId, billingCycleStart);

        PlanDTO existingPlan = new PlanDAS().findPlanByItemId(event.getExistingPlanItemId());
        PlanDTO swapPlan = new PlanDAS().findPlanByItemId(event.getSwapPlanItemId());

        OrderDTO adjustmentOrder = new OrderDTO();
        UserDTO user = new UserDAS().find(userId);
        adjustmentOrder.setBaseUserByUserId(user);
        adjustmentOrder.setActiveSince(billingCycleStart);

        OrderPeriodDTO period = new OrderPeriodDTO();
        period.setId(Constants.ORDER_PERIOD_ONCE);
        adjustmentOrder.setOrderPeriod(period);

        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(Constants.ORDER_BILLING_POST_PAID);
        adjustmentOrder.setOrderBillingType(type);
        adjustmentOrder.setCreateDate(TimezoneHelper.companyCurrentDate(getEntityId()));
        adjustmentOrder.setCurrency(user.getCurrency());

        adjustmentOrder.setNotes("Adjustment Order Created during Plan Swap for Subscription Order "+ subscriptionOrderId);

        OrderLineDTO adjustmentLine = new OrderLineDTO();
        adjustmentLine.setQuantity(BigDecimal.ONE);

        StringBuilder lineDescriptionBuilder = new StringBuilder();

        ResourceBundle resourceBundle = getUserResourceBundle(user);

        if(changedAmount.compareTo(BigDecimal.ZERO) < 0) {
            lineDescriptionBuilder.append(resourceBundle.getString("order.credit.adjustment.note"));
            adjustmentLine.setItemId(getItemId(PARAM_CREDIT_PRODUCT));
        } else {
            lineDescriptionBuilder.append(resourceBundle.getString("order.debit.adjustment.note"));
            adjustmentLine.setItemId(getItemId(PARAM_DEBIT_PRODUCT));
        }

        String oldPlanDescription = existingPlan.getItem().getDescription(user.getLanguageIdField());
        String newPlanDescription = swapPlan.getItem().getDescription(user.getLanguageIdField());

        lineDescriptionBuilder
        .append(", ")
        .append(resourceBundle.getString("order.oldPlan"))
        .append(STR_SPACE)
        .append(oldPlanDescription)
        .append(STR_SPACE)
        .append(resourceBundle.getString("order.newPlan"))
        .append(STR_SPACE)
        .append(newPlanDescription);

        String lineDescription = lineDescriptionBuilder.toString();
        logger.info(lineDescription);
        adjustmentLine.setDescription(lineDescription);
        adjustmentLine.setPrice(changedAmount);
        adjustmentLine.setAmount(adjustmentLine.getPrice().multiply(adjustmentLine.getQuantity()));
        adjustmentLine.setTypeId(Constants.ORDER_LINE_TYPE_ADJUSTMENT);
        adjustmentLine.setUseItem(Boolean.FALSE);

        // Adding Line on Order
        adjustmentOrder.getLines().add(adjustmentLine);
        OrderBL orderBL = new OrderBL(adjustmentOrder);

        // Creating Adjustment Order
        return orderBL.create(getEntityId(), null, adjustmentOrder);
    }

    private Integer getItemId(ParameterDescription param) {
        String itemId = getParameters().get(param.getName());
        if(Objects.isNull(itemId)) {
            throw new SessionInternalError(String.format("Please Enter [%s] for SwapPlanFUPTransferTask plugin ", param.getName()));
        }
        return Integer.valueOf(itemId);
    }

    /**
     * This method expires all the old customer usage pools by setting their cycle end date.
     * @param customerDto
     * @param existingPlan
     */
    private void expireOldCustomerUsagePools(CustomerDTO customerDto, PlanDTO existingPlan, Integer orderId) {
        List<CustomerUsagePoolDTO> oldCustomerUsagePools = customerDto.getCustomerUsagePools();
        if (CollectionUtils.isNotEmpty(oldCustomerUsagePools)) {
            for (CustomerUsagePoolDTO oldCustomerUsagePool: oldCustomerUsagePools) {
                if (oldCustomerUsagePool.getCycleEndDate().after(Util.getEpochDate())
                        && oldCustomerUsagePool.getPlan().getId().equals(existingPlan.getId())
                        && oldCustomerUsagePool.getOrder().getId().equals(orderId)) {
                    //To expire FUP set cycle end date as 1970-01-01
                    oldCustomerUsagePool.setCycleEndDate(Util.getEpochDate());
                    oldCustomerUsagePool.setCycleStartDate(Util.getEpochDate());
                    oldCustomerUsagePool.setQuantity(BigDecimal.ZERO);
                }
            }
        }
    }

    /**
     * This method fires the customer plan subscription event for the new swapped plan.
     * This will take care of creating all new customer usage pools belonging to the new plan.
     * @param orderDto
     */
    private void processCustomerPlanSubscription(OrderDTO orderDto) {
        List<CustomerPlanSubscriptionEvent> subscriptionEvents = new ArrayList<>();
        subscriptionEvents.addAll(new OrderBL().generateCustomerPlanSubscriptionEvents(orderDto.getLines(),
                orderDto.getUserId(), orderDto, null, true, false));

        // creating customer usage pool and re rating usage order.
        for(CustomerPlanSubscriptionEvent subscriptionEvent : subscriptionEvents) {
            EventManager.process(subscriptionEvent);
        }
    }

    private OrderLineDTO getLineById(List<OrderLineDTO> lines, int id) {
        OrderLineDTO line = null;
        for(OrderLineDTO orderLine: lines) {
            if(orderLine.getId()==id) {
                orderLine.clearOrderLineUsagePools();
                line = orderLine;
                break;
            }
        }
        return line;
    }
}