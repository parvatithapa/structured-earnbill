package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.spc.billing.SPCUserFilterTask;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;

import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolEvaluationEvent;
import com.sapienter.jbilling.server.usagePool.DefaultUsagePoolEvaluationTask;
import com.sapienter.jbilling.server.usagePool.IUsagePoolEvaluationTask;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

public class SPCCustomerUsagePoolEvaluationTask extends PluggableTask implements IUsagePoolEvaluationTask  {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_SPC_USER_FILTER_TASK_ID =
            new ParameterDescription("Spc User Filter Task Plugin Id", true, Type.INT);

    private CustomerUsagePoolDAS customerUsagePoolDAS;
    private CustomerUsagePoolBL customerUsagePoolBL;


    public SPCCustomerUsagePoolEvaluationTask() {
        customerUsagePoolDAS = new CustomerUsagePoolDAS();
        customerUsagePoolBL = new CustomerUsagePoolBL();
        descriptions.add(PARAM_SPC_USER_FILTER_TASK_ID);
    }

    @Override
    public void evaluateCustomerUsagePool(CustomerUsagePoolEvaluationEvent customerUsagePoolEvaluationEvent) {
        try {
            Map<String, String> params = getSpcUserFilterTaskParams();
            CustomerUsagePoolDTO customerUsagePool = customerUsagePoolDAS.findForUpdate(customerUsagePoolEvaluationEvent.getCustomerUsagePoolId());
            CustomerDTO customer = customerUsagePool.getCustomer();
            DefaultUsagePoolEvaluationTask defaultUsagePoolEvaluationTask = new DefaultUsagePoolEvaluationTask();
            String customerTypeMetaFieldName = params.get(SPCUserFilterTask.PARAM_CUSTOMER_TYPE_MF_NAME.getName());
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> customerTypeMetaFieldValue = customer.getMetaField(customerTypeMetaFieldName);
            if(null == customerTypeMetaFieldValue || customerTypeMetaFieldValue.isEmpty()) {
                logger.debug("customerType not found on user {}", customer.getBaseUser().getId());
                defaultUsagePoolEvaluationTask.evaluateCustomerUsagePool(customerUsagePoolEvaluationEvent);
                return;
            }

            String customerType  = params.get(SPCUserFilterTask.PARAM_CUSTOMER_TYPE.getName());
            if(!customerTypeMetaFieldValue.getValue().equals(customerType)) {
                logger.debug("allowed for customer usage pool evaluation");
                defaultUsagePoolEvaluationTask.evaluateCustomerUsagePool(customerUsagePoolEvaluationEvent);
                return;
            }

            Date runDatePlusOneDay = DateUtils.addDays(customerUsagePoolEvaluationEvent.getRunDate(), 1);

            if (DateUtils.isSameDay(runDatePlusOneDay, customer.getNextInvoiceDate())) {
                renewCustomerUsagePool(customerUsagePool);
            }

            int daysToDelay = Integer.parseInt(params.get(SPCUserFilterTask.PARAM_DAYS_TO_DELAY_BILLING.getName()));
            Date calculatedCycleEndDate = DateUtils.addDays(customerUsagePoolBL.removeTime(customerUsagePool.getCycleEndDate()), ++daysToDelay);

            if(DateUtils.isSameDay(calculatedCycleEndDate, customerUsagePoolEvaluationEvent.getRunDate())) {
                logger.debug("expiring existing {}", customerUsagePool.getId());
                customerUsagePool.expire();
            }

        } catch(Exception ex) {
            logger.error("customer usage pool evaluation failed for CustomerUsagePool {}",
                    customerUsagePoolEvaluationEvent.getCustomerUsagePoolId(), ex);
            throw new SessionInternalError(ex);
        }
    }

    private Map<String, String> getSpcUserFilterTaskParams() throws PluggableTaskException {
        Integer spcUserFilterTaskId = Integer.parseInt(getMandatoryStringParameter(PARAM_SPC_USER_FILTER_TASK_ID.getName()));
        return new PluggableTaskBL<>(spcUserFilterTaskId).getParameters();
    }

    private void  renewCustomerUsagePool(CustomerUsagePoolDTO customerUsagePool) {
        CustomerDTO customer = customerUsagePool.getCustomer();
        UsagePoolDTO usagePool = customerUsagePool.getUsagePool();
        BigDecimal usagePoolQuantity = usagePool.getQuantity();
        OrderDTO subscriptionOrder = customerUsagePool.getOrder();

        Date subscriptionStartDate = DateUtils.addDays(customerUsagePoolBL.removeTime(customerUsagePool.getCycleEndDate()), 1);
        String cyclePeriodUnit = usagePool.getCyclePeriodUnit();
        logger.debug("cyclePeriodUnit: {}", cyclePeriodUnit);
        OrderPeriodDTO subscriptionPeriod = customer.getMainSubscription().getSubscriptionPeriod();

        Date subscriptionEndDate = customerUsagePoolBL.getCycleEndDateForPeriod(cyclePeriodUnit,
                usagePool.getCyclePeriodValue(), subscriptionStartDate, subscriptionPeriod, subscriptionOrder.getActiveUntil());

        CustomerUsagePoolDTO newCustomerUsagePool = new CustomerUsagePoolDTO();
        newCustomerUsagePool.setCustomer(customer);
        newCustomerUsagePool.setInitialQuantity(usagePoolQuantity);
        newCustomerUsagePool.setQuantity(usagePoolQuantity);
        newCustomerUsagePool.setLastRemainingQuantity(BigDecimal.ZERO);
        newCustomerUsagePool.setUsagePool(usagePool);
        newCustomerUsagePool.setPlan(customerUsagePool.getPlan());
        newCustomerUsagePool.setOrder(subscriptionOrder);
        newCustomerUsagePool.setCycleStartDate(subscriptionStartDate);
        newCustomerUsagePool.setCycleEndDate(subscriptionEndDate);

        // save new customer usage pool
        newCustomerUsagePool = customerUsagePoolBL.save(newCustomerUsagePool);
        if (null != newCustomerUsagePool) {
            logger.debug("new usage pool {} created for customer {} from {} to {} period", newCustomerUsagePool.getId(),
                    customer.getBaseUser().getId(), newCustomerUsagePool.getCycleStartDate(),
                    newCustomerUsagePool.getCycleEndDate());
        }
    }
}
