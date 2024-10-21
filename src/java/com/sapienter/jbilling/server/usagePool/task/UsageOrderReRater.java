package com.sapienter.jbilling.server.usagePool.task;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.DbConnectionUtil;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

import static com.sapienter.jbilling.server.usagePool.task.UsageOrderReRater.UsagePeriod.*;

public interface UsageOrderReRater {

    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    enum UsagePeriod {
        CYCLE_START_DATE, CYCLE_END_DATE;
    }

    /**
     * Calculates cycle start and end date based on given subscription order
     * @param subscriptionOrder
     * @return
     */
    public static Map<UsagePeriod, Date> getBillablePeriodFromSubscriptionOrder(OrderDTO subscriptionOrder) {
        Date newActiveSince = subscriptionOrder.getActiveSince();
        Date newActiveUntil = subscriptionOrder.getActiveUntil();

        CustomerDTO customerDto = subscriptionOrder.getBaseUserByUserId().getCustomer();
        Date nextBillableDate = null == subscriptionOrder.getNextBillableDay() ? subscriptionOrder.getActiveSince() : subscriptionOrder.getNextBillableDay();
        Date billingCycleStart = CustomerUsagePoolBL.calcCycleStartDateFromMainSubscription(nextBillableDate,
                customerDto.getMainSubscription());
        Date billingCycleEnd = customerDto.getNextInvoiceDate();

        MainSubscriptionDTO mainSubscription = customerDto.getMainSubscription();
        OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
        int periodUnitId = orderPeriodDTO.getUnitId();
        int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
        PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);

        if (newActiveSince.after(billingCycleStart)) {
            billingCycleStart = newActiveSince;
        }
        if (!subscriptionOrder.getProrateFlag()) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(billingCycleStart), orderPeriodDTO.getValue())));
            if (null != newActiveUntil && newActiveUntil.before(cal.getTime())) {
                billingCycleEnd = newActiveUntil;
            } else {
                billingCycleEnd = cal.getTime();
            }
        } else {
            Date newActiveUntilDate = (null != newActiveUntil) ? newActiveUntil : customerDto.getNextInvoiceDate();
            if (newActiveUntilDate.before(billingCycleEnd)) {
                billingCycleEnd = newActiveUntil;
            }
        }
        if(Constants.ORDER_BILLING_PRE_PAID.equals(subscriptionOrder.getBillingTypeId()) && null != subscriptionOrder.getNextBillableDay()) {
              billingCycleStart = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(billingCycleStart), orderPeriodDTO.getValue() * -1L));
        }

        if (billingCycleStart.compareTo(billingCycleEnd) == 0) {
            billingCycleEnd = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(billingCycleEnd), orderPeriodDTO.getValue()));
        }

        Calendar calculatedBillingCycleStart = Calendar.getInstance();
        calculatedBillingCycleStart.setTime(billingCycleStart);
        Map<UsagePeriod, Date> result = new EnumMap<>(UsagePeriod.class);
        result.put(CYCLE_START_DATE, calculatedBillingCycleStart.getTime());
        result.put(CYCLE_END_DATE, billingCycleEnd);
        logger.debug("Usage Period {}", result);
        return result;
    }

    /**
     * Re Rates usage orders if usage order has used customer usage pool which is being subscribed/unsubscribed and reset customer usagepool quantity
     * based on resetCustomerUsagePool flag
     * @param entityId
     * @param subscriptionOrder
     * @param resetCustomerUsagePool
     */
    public static void reRateUsageOrder(Integer entityId, OrderDTO subscriptionOrder, boolean resetCustomerUsagePool) {
        Map<UsagePeriod, Date> periodMap = getBillablePeriodFromSubscriptionOrder(subscriptionOrder);
        Date billingCycleStart = periodMap.get(CYCLE_START_DATE);
        Date billingCycleEnd = periodMap.get(CYCLE_END_DATE);
        if(resetCustomerUsagePool) {
            Integer customerId = subscriptionOrder.getBaseUserByUserId().getCustomer().getId();
            List<CustomerUsagePoolDTO> activePools = new CustomerUsagePoolBL().getCustomerUsagePoolsByCustomerId(customerId);
            if(CollectionUtils.isNotEmpty(activePools)) {
                CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
                for(CustomerUsagePoolDTO customerUsagePool : activePools) {
                        logger.debug("Re setting Customer {} customer usagepool {} to quantity {}", customerId, customerUsagePool.getId(),
                                customerUsagePool.getInitialQuantity());
                        customerUsagePool.setQuantity(customerUsagePool.getInitialQuantity());
                        das.save(customerUsagePool);
                }
            }
        }
        OrderDAS orderDAS = new OrderDAS();
        List<Integer> usageOrderIds = orderDAS.getCustomersAllOneTimeUsageOrdersInCurrentBillingCycle(subscriptionOrder.getUserId(), billingCycleStart, billingCycleEnd, OrderStatusFlag.INVOICE);
        reRateUsageOrder(entityId, usageOrderIds);
    }

    /**
     * Re Rates usage orders if usage order has used customer usage pool which is being subscribed/unsubscribed
     * @param entityId
     * @param subscriptionOrder
     * @param resetCustomerUsagePool
     */
    public static void reRateUsageOrder(Integer entityId, OrderDTO subscriptionOrder) {
        reRateUsageOrder(entityId, subscriptionOrder, true);
    }

    /**
     * Re Rates usage orders if usage order has used customer usage pool which is being subscribed/unsubscribed
     * @param entityId
     * @param userId
     * @param billingCycleStart
     * @param billingCycleEnd
     */
    public static void reRateUsageOrder(Integer entityId, Integer userId, Date billingCycleStart, Date billingCycleEnd) {
        OrderDAS orderDAS = new OrderDAS();
        List<Integer> usageOrderIds = orderDAS.getCustomersAllOneTimeUsageOrdersInCurrentBillingCycle(userId, billingCycleStart, billingCycleEnd, OrderStatusFlag.INVOICE);
        reRateUsageOrder(entityId, usageOrderIds);
    }

    /**
     * Re Rates usage orders if usage order has used customer usage pool which is being subscribed/unsubscribed
     * @param entityId
     * @param orders
     */
    public static void reRateUsageOrder(Integer entityId, List<Integer> orders) {
        OrderDAS orderDAS = new OrderDAS();
        orders.stream()
        .filter(id -> DbConnectionUtil.isEntityPersisted("purchase_order", id))
        .forEach(id -> {
            OrderDTO usageOrder = orderDAS.find(id);
            List<OrderLineDTO> oldLines = OrderHelper.copyOrderLinesToDto(usageOrder.getLines());
            OrderBL orderBL = new OrderBL(usageOrder);

            for(OrderLineDTO orderLine : usageOrder.getLines()) {
                boolean skipLine = false;
                if (!orderLine.isLineTypeItem() && !orderLine.getUseItem()) {
                    skipLine = true;
                }

                ItemDTO item = orderLine.getItem();
                if(null!=item && item.getHasDecimals() == 0) {
                    skipLine = true;
                }

                if(skipLine) {
                    continue;
                }
                orderLine.clearOrderLineUsagePools();
                orderLine.setQuantity(orderLine.getQuantity().add(new BigDecimal(com.sapienter.jbilling.common.Constants.NEW_QUANTITY)));
                orderBL.processLine(orderLine, usageOrder.getUser().getLanguageIdField(), entityId, usageOrder.getUserId(), usageOrder.getCurrencyId(), null);
                orderBL.checkOrderLineQuantities(Arrays.asList(getLineById(oldLines, orderLine.getId())), Arrays.asList(orderLine), entityId, id, true, false);
                orderBL.recalculate(entityId);
                usageOrder.setFreeUsageQuantity(usageOrder.getFreeUsagePoolsTotalQuantity());
            }
        });
    }

    public static OrderLineDTO getLineById(List<OrderLineDTO> lines, int id) {
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
