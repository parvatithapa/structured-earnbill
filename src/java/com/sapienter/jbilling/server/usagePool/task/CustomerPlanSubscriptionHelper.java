package com.sapienter.jbilling.server.usagePool.task;

import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import org.apache.commons.lang.time.DateUtils;

public abstract interface CustomerPlanSubscriptionHelper {


    public static void createCustomerUsagePool(CustomerPlanSubscriptionEvent customerPlanSubscriptionEvent, boolean prorateFirstPeriod) throws PluggableTaskException {
        OrderDTO subScriptionOrder = customerPlanSubscriptionEvent.getOrder();
        CustomerDTO customer = subScriptionOrder.getBaseUserByUserId().getCustomer();
        PlanDTO plan = customerPlanSubscriptionEvent.getPlan();
        Date lastBillingProcessDate = new BillingProcessDAS().getLastBillingProcessDate(customer.getBaseUser().getEntity().getId());
        Date startDate;
        try {
            startDate = new BasicOrderPeriodTask().calculateStart(customerPlanSubscriptionEvent.getOrder());
        } catch (TaskException e) {
            throw new PluggableTaskException(e);
        }

        if(subScriptionOrder.getNextBillableDay() != null &&
                Constants.ORDER_BILLING_PRE_PAID.equals(subScriptionOrder.getBillingTypeId())) {
            MainSubscriptionDTO mainSubscription = subScriptionOrder.getUser().getCustomer().getMainSubscription();
            OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
            int periodUnitId = orderPeriodDTO.getUnitId();
            int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
            PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
            startDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(startDate),
                    orderPeriodDTO.getValue() * -1L));
        }
        CustomerUsagePoolBL customerUsagePoolBl = new CustomerUsagePoolBL();
        customerUsagePoolBl.setProrateForFirstPeriod(prorateFirstPeriod);
        createCustomerUsagePoolsForPlan(subScriptionOrder, customer, plan, customerUsagePoolBl, startDate, customer.getNextInvoiceDate());

        // JBSPC-1059 - To create next cycle pool when order active since before NID and bill run date is greater than NID
        // JBSPC-1080 - To create next cycle pool when order active since before NID and last Bill Run date is one day before customer NID
        Date customerNIDminusOneDay = DateUtils.addDays(customer.getNextInvoiceDate(), -1);
        if (null != lastBillingProcessDate &&
                subScriptionOrder.getActiveSince().before(customer.getNextInvoiceDate()) &&
                lastBillingProcessDate.compareTo(customerNIDminusOneDay) >= 0) {

            MainSubscriptionDTO mainSubscriptionDTO = customer.getMainSubscription();

            Date nextCycleInvoiceDate = UserBL.checkNIDAgaintMainSubscription(
                    mainSubscriptionDTO.getSubscriptionPeriod().getPeriodUnit().getId(),
                    mainSubscriptionDTO.getNextInvoiceDayOfPeriod(),
                    new UserBL().getCustomerNextInvoiceDate(customer.getBaseUser()));

            createCustomerUsagePoolsForPlan(subScriptionOrder, customer, plan, customerUsagePoolBl, customer.getNextInvoiceDate(), nextCycleInvoiceDate);
        }
    }

    static void createCustomerUsagePoolsForPlan(OrderDTO subscriptionOrder, CustomerDTO customer, PlanDTO plan,
                                                CustomerUsagePoolBL customerUsagePoolBl, Date poolStartDate, Date poolEndDate) {
        for (UsagePoolDTO usagePool : plan.getUsagePools()) {
            CustomerUsagePoolDTO customerUsagePool = customerUsagePoolBl.getCreateCustomerUsagePoolDto(usagePool.getId(),
                    customer.getId(),
                    poolStartDate,
                    subscriptionOrder.getOrderPeriod(),
                    plan,
                    subscriptionOrder.getActiveUntil(),
                    subscriptionOrder.getCreateDate(),
                    subscriptionOrder,
                    poolEndDate);
            customerUsagePoolBl.createOrUpdateCustomerUsagePool(customerUsagePool);
        }
    }

    /**
     * This method fires the customer plan subscription event for the new swapped plan.
     * This will take care of creating all new customer usage pools belonging to the new plan.
     * @param orderDto
     */
    public static void processCustomerPlanSubscription(OrderDTO orderDto) {
        List<CustomerPlanSubscriptionEvent> subscriptionEvents = generateCustomerPlanSubscriptionEvents(orderDto);
        // creating customer usage pool and re rating usage order.
        for(CustomerPlanSubscriptionEvent subscriptionEvent : subscriptionEvents) {
            EventManager.process(subscriptionEvent);
        }
    }

    public static List<CustomerPlanSubscriptionEvent> generateCustomerPlanSubscriptionEvents(OrderDTO order) {
        return new OrderBL().generateCustomerPlanSubscriptionEvents(order.getLines(), order.getUserId(),
                order, null, true, false);
    }
}
