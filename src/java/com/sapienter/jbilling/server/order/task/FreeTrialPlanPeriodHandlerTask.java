package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.server.item.db.FreeTrialPeriod;
import com.sapienter.jbilling.server.item.db.ItemDAS;

import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.task.SwapPlanFUPTransferTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDecimalsException;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;

import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;


import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;


public class FreeTrialPlanPeriodHandlerTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription FREE_TRIAL_SUBSCRIPTION_ORDER_ID = new ParameterDescription("Free Trial Subscription Order Meta Field", true, ParameterDescription.Type.STR);
    public static final ParameterDescription FREE_TRIAL_DISCOUNT_PRODUCT_ID = new ParameterDescription("Free Trial Discount Product ID", true, ParameterDescription.Type.INT);

    {
        descriptions.add(FREE_TRIAL_SUBSCRIPTION_ORDER_ID);
        descriptions.add(FREE_TRIAL_DISCOUNT_PRODUCT_ID);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            NewOrderEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("Processing FreeTrialPlanPeriodHandlerTask");

        if(event instanceof NewOrderEvent){
            logger.debug("Processing FreeTrialPlanPeriodHandlerTask for NewOrderEvent");
            NewOrderEvent orderEvent = (NewOrderEvent) event;
            OrderDTO order = orderEvent.getOrder();

            PlanDTO plan = order.getLines().stream()
                    .filter(line -> line.getItem() != null && line.getItem().isPlan())
                    .flatMap(line -> line.getItem().getPlans().stream())
                    .findFirst().orElse(null);
            if (plan == null || !plan.isFreeTrial()) {
                logger.debug("Plan is not have free trial option");
                return;
            } else {
                List<OrderDTO> subscriptions = new OrderDAS().findByPlanUserFreeTrialSubscription(order.getUserId(), plan.getId())
                        .stream().filter(o -> !o.getId().equals(order.getId())).collect(Collectors.toList());
                if (!subscriptions.isEmpty()) {
                    subscriptions.stream().forEach(o -> logger.debug("Order={}", o));
                    logger.debug("Customer have been subscribed to this plan before");
                    return;
                }
            }

            logger.debug("Order have plan with trial period");
            logger.debug("Order={}", order);
            logger.debug("Plan={}", plan);

            UserBL userBL = new UserBL(order.getBaseUserByUserId());
            CustomerDTO customerDTO = order.getUser().getCustomer();
            ResourceBundle bundle = getUserResourceBundle(userBL.getEntity());
            Integer entityId = order.getBaseUserByUserId().getCompany().getId();
            Integer languageId = userBL.getEntity().getLanguageIdField();

            OrderDTO newOrder = new OrderDTO(order);
            // reset the ids, so it is a new order
            newOrder.setId(null);
            newOrder.setVersionNum(null);
            newOrder.setParentOrder(null);
            newOrder.setCreateDate(new Date());
            newOrder.setNextBillableDay(null);
            newOrder.setParentOrder(null);
            newOrder.setNotesInInvoice(1);

            newOrder.getLines().clear();
            newOrder.getMetaFields().clear();
            newOrder.getChildOrders().clear();
            newOrder.getDiscountLines().clear();
            newOrder.getOrderProcesses().clear();

            //calculate active until date from period/period value
            FreeTrialPeriod periodUnit = plan.getFreeTrialPeriodUnit();
            Integer periodValue = plan.getFreeTrialPeriodValue();
            Date activeUntilDate = freeTrialPlanEndDate(periodUnit, periodValue, order.getActiveSince(), customerDTO.getMainSubscription());
            newOrder.setActiveUntil(activeUntilDate);
            logger.debug("Free trial period={}, value={}", periodUnit, periodValue);
            logger.debug("Discount order activeUntilDate={}", activeUntilDate);

            if (!isMatchPeriod(periodUnit, order.getOrderPeriod())) {
                logger.debug("Free trial period and order period not matched");
                newOrder.setProrateFlag(true);
                newOrder.setProrateAdjustmentFlag(true);
            }

            // order lines:
            Integer discountProductId =  Integer.valueOf(parameters.get(FREE_TRIAL_DISCOUNT_PRODUCT_ID.getName()));
            String productDescription = new ItemDAS().findNow(discountProductId).getDescription(languageId);
            String planDescription = new ItemDAS().findNow(plan.getItemId()).getDescription(languageId);
            String customDescription = productDescription + " - " + planDescription;

            PeriodOfTime cycle = new PeriodOfTime(newOrder.getActiveSince(), activeUntilDate, 0);
            newOrder.getLines().add(createLine(newOrder, cycle, newOrder.getOrderPeriod(), order.getTotal(), languageId, customDescription));
            newOrder.setNotes(bundle.getString("order.free.trial.notes") + " " + order.getId());

            //Set metafield
            MetaField fieldName = new MetaFieldDAS().getFieldByName(entityId, new EntityType[]{EntityType.ORDER}, parameters.get(FREE_TRIAL_SUBSCRIPTION_ORDER_ID.getName()));
            MetaFieldValue field = fieldName.createValue();
            field.setValue(order.getId());
            newOrder.setMetaField(field, null);

            if (!newOrder.getLines().isEmpty()) {
                // do the maths
                try {
                    OrderBL orderBl = new OrderBL(newOrder);
                    orderBl.recalculate(entityId);
                    orderBl.create(entityId, null, newOrder);
                } catch (ItemDecimalsException e) {
                    throw new SessionInternalError("Error when doing credit", FreeTrialPlanPeriodHandlerTask.class, e);
                }

                // save
                try {
                    order.setActiveUntil(null);
                    new OrderDAS().save(order);
                } catch (Exception e) {
                    throw new SessionInternalError("Error when creating order", FreeTrialPlanPeriodHandlerTask.class, e);
                }
            }
        }
    }

    private Date freeTrialPlanEndDate(FreeTrialPeriod cyclePeriodUnit, Integer cyclePeriodValue, Date periodStartDate, MainSubscriptionDTO mainSubscription) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodStartDate);
        OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
        switch (cyclePeriodUnit) {
            case MONTHS:
                cal.add(Calendar.MONTH, cyclePeriodValue);
                break;
            case YEARS:
                cal.add(Calendar.YEAR, cyclePeriodValue);
            case BILLING_CYCLE:
                int unit = MapPeriodToCalendar.map(orderPeriodDTO.getUnitId());
                cal.add(unit, orderPeriodDTO.getValue() * cyclePeriodValue);
                if (unit == Calendar.MONTH) {
                    int noOfdays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    int daysToAdd = noOfdays < mainSubscription.getNextInvoiceDayOfPeriod().intValue() ? noOfdays : mainSubscription.getNextInvoiceDayOfPeriod();
                    cal.set(Calendar.DAY_OF_MONTH, daysToAdd);
                }
                break;
            case DAYS:
                cal.add(Calendar.DATE, cyclePeriodValue);
                break;
            default:
                break;
        }

        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.MILLISECOND, 999);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        return cal.getTime();
    }

    private OrderLineDTO createLine(OrderDTO order, PeriodOfTime cycle, OrderPeriodDTO orderPeriodDTO, BigDecimal amount, Integer languageId, String description) {
        OrderLineDTO newLine = new OrderLineDTO();
        // reset so it gets inserted
        newLine.setId(0);
        newLine.setUseItem(false);
        newLine.setVersionNum(null);
        newLine.setPurchaseOrder(order);
        newLine.getAssets().clear();
        newLine.setCreateDatetime(TimezoneHelper.companyCurrentDate(getEntityId()));

        // make the order negative (credit)
        newLine.setPrice(amount.negate());
        newLine.setQuantity(BigDecimal.ONE);
        newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        newLine.setAmount(newLine.getPrice().multiply(newLine.getQuantity()));

        Integer discountProductId = Integer.valueOf(parameters.get(FREE_TRIAL_DISCOUNT_PRODUCT_ID.getName()));
        newLine.setItemId(discountProductId);
        newLine.setDescription(description);

        /*int noOfDays = cycle.getDaysInPeriod() + 1;
        int maxDaysOfMonth = calculateDaysInPeriod(order);
        BigDecimal singleDayPrice = calculateSingleDayPrice(newLine.getAmount(), maxDaysOfMonth);
        BigDecimal calculatedPrice = singleDayPrice.multiply(new BigDecimal(noOfDays)).negate();
        logger.debug("noOfDays={}, maxDaysOfMonth={}, singleDayPrice={}, calculatedPrice={}", noOfDays, maxDaysOfMonth, singleDayPrice, calculatedPrice);*/
        return newLine;
    }

    /*
    Calculate max days of in period:
    1. If month, get days in month
    2. If year, get days in year
    */
    private Integer calculateDaysInPeriod(OrderDTO orderDTO) {
        OrderPeriodDTO orderPeriodDTO = orderDTO.getOrderPeriod();
        int unit = MapPeriodToCalendar.map(orderPeriodDTO.getUnitId());
        switch (unit) {
            case GregorianCalendar.DAY_OF_YEAR:
            case GregorianCalendar.WEEK_OF_YEAR:
            case GregorianCalendar.MONTH:
                if (orderPeriodDTO.getValue() > 1) {
                    return (int) DAYS.between(DateConvertUtils.asLocalDate(orderDTO.getActiveSince()),
                            DateConvertUtils.asLocalDate(orderDTO.getActiveSince()).plusMonths(orderPeriodDTO.getValue()));
                }
                return DateConvertUtils.asLocalDate(orderDTO.getActiveSince()).lengthOfMonth();
            default:
                return (int) DAYS.between(DateConvertUtils.asLocalDate(orderDTO.getActiveSince()),
                        DateConvertUtils.asLocalDate(orderDTO.getActiveSince()).plusYears(1));
        }
    }

    private BigDecimal calculateSingleDayPrice(BigDecimal amount, int maxNoOfDays) {
        return amount.divide(new BigDecimal(maxNoOfDays), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
    }

    private boolean isMatchPeriod(FreeTrialPeriod freeTrialPeriod, OrderPeriodDTO orderPeriodDTO) {
        int unit = MapPeriodToCalendar.map(orderPeriodDTO.getUnitId());
        switch (unit) {
            case GregorianCalendar.DAY_OF_YEAR:
            case GregorianCalendar.WEEK_OF_YEAR:
                return FreeTrialPeriod.DAYS.equals(freeTrialPeriod);
            case GregorianCalendar.MONTH:
                return FreeTrialPeriod.MONTHS.equals(freeTrialPeriod) && orderPeriodDTO.getValue().equals(1); // not matched for 3 month period, it have same unit period
            case GregorianCalendar.YEAR:
                return FreeTrialPeriod.YEARS.equals(freeTrialPeriod);
            default:
                return false;
        }
    }

    private ResourceBundle getUserResourceBundle(UserDTO user) {
        try {
            return ResourceBundle.getBundle("entityNotifications", user.getLanguage().asLocale());
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new SessionInternalError("Error ", SwapPlanFUPTransferTask.class, e);
        }
    }
}
