package com.sapienter.jbilling.server.usagePool;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerUsagePoolConsumptionEvent;

public class DefaultUsagePoolEvaluationTask extends PluggableTask implements IUsagePoolEvaluationTask {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void evaluateCustomerUsagePool(CustomerUsagePoolEvaluationEvent customerUsagePoolEvaluationEvent) {
        Integer entityId = customerUsagePoolEvaluationEvent.getEntityId();
        CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
        CustomerUsagePoolDTO custUsagePool = das.findForUpdate(customerUsagePoolEvaluationEvent.getCustomerUsagePoolId());
        Date runDate = customerUsagePoolEvaluationEvent.getRunDate();
        logger.debug("evaluating {} customer usage pool for subscription order {}", custUsagePool.getId(), custUsagePool.getOrder().getId());

        if(custUsagePool.getCycleEndDate().after(runDate)) {
            logger.debug("Skipping Customer [{}] because customer usage pool [{}]'s end date [{}] is after run date [{}], only Post Paid customer type should evaluate CUPs one day earlier", custUsagePool.getCustomer().getId(),
                    custUsagePool.getId(), custUsagePool.getCycleEndDate(), runDate);
            return;
        }

        if(custUsagePool.getCycleEndDate().before(custUsagePool.getCycleStartDate())) {
            logger.warn("Skipping Customer [{}] because customer usage pool [{}]'s end date [{}] is before start date [{}]", custUsagePool.getCustomer().getId(),
                    custUsagePool.getId(), custUsagePool.getCycleEndDate(), custUsagePool.getCycleStartDate());
            return;
        }

        if (null != custUsagePool.getOrder() && custUsagePool.getOrder().getId().intValue() != 0 &&
                custUsagePool.getOrder().getDeleted() != 1 &&
                !custUsagePool.getOrder().getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {

            logger.debug("customerUsagePool quantity: {}", custUsagePool.getQuantity());
            String resetValue = custUsagePool.getUsagePool().getUsagePoolResetValue().getResetValue();
            logger.debug("resetValue: {}", resetValue);

            if (resetValue.equals(UsagePoolResetValueEnum.ZERO.toString())) {
                if(!custUsagePool.getQuantity().equals(BigDecimal.ZERO)) {
                    custUsagePool.setLastRemainingQuantity(custUsagePool.getQuantity());
                    CustomerUsagePoolConsumptionEvent qtyChangeEvent = new CustomerUsagePoolConsumptionEvent(
                            entityId,
                            custUsagePool.getId(),
                            custUsagePool.getQuantity(),
                            BigDecimal.ZERO,
                            null);

                    custUsagePool.setQuantity(BigDecimal.ZERO);
                    EventManager.process(qtyChangeEvent);
                    logger.debug("Quantity: {}", custUsagePool.getQuantity());
                    das.save(custUsagePool);
                }
            } else  {

                custUsagePool.setLastRemainingQuantity(custUsagePool.getQuantity());

                //Set cycle start date
                Date subscriptionStartDate = DateUtils.addDays(DateUtils.truncate(custUsagePool.getCycleEndDate(), Calendar.DAY_OF_MONTH), 1);
                custUsagePool.setCycleStartDate(subscriptionStartDate);

                String cyclePeriodUnit = custUsagePool.getUsagePool().getCyclePeriodUnit();

                logger.debug("cyclePeriodUnit: {}", cyclePeriodUnit);
                OrderDTO subscriptionOrder = custUsagePool.getOrder();
                Date orderActiveUntil = subscriptionOrder.getActiveUntil();
                Date nextInvoiceDate = custUsagePool.getCustomer().getNextInvoiceDate();
                QuantitySupplier quantitySupplier = new QuantitySupplier(custUsagePool.getUsagePool(), nextInvoiceDate, null);
                BigDecimal usagePoolQuantity = quantitySupplier.get();
                CustomerUsagePoolBL customerUsagePoolBL = new CustomerUsagePoolBL(custUsagePool.getId());
                if (!subscriptionOrder.getProrateFlag()) {
                    custUsagePool.setCycleEndDate(customerUsagePoolBL.getCycleEndDateForPeriod(cyclePeriodUnit,
                            custUsagePool.getUsagePool().getCyclePeriodValue(), subscriptionStartDate,
                            custUsagePool.getCustomer().getMainSubscription().getSubscriptionPeriod(), orderActiveUntil));
                } else {
                    BigDecimal prorateQuantity = customerUsagePoolBL.getCustomerUsagePoolProrateQuantity(nextInvoiceDate,subscriptionStartDate,
                            orderActiveUntil, usagePoolQuantity, custUsagePool.getCustomer().getMainSubscription(),
                            cyclePeriodUnit, custUsagePool.getUsagePool().getCyclePeriodValue());

                    usagePoolQuantity = prorateQuantity;
                    logger.debug("prorateQuantity: {}", prorateQuantity);
                    custUsagePool.setInitialQuantity(prorateQuantity);
                    custUsagePool.setCycleEndDate(customerUsagePoolBL.getCycleEndDateForProratePeriod(nextInvoiceDate, orderActiveUntil, subscriptionOrder));
                }

                logger.debug("CycleEndDate: {}", custUsagePool.getCycleEndDate());

                if (resetValue.equals(UsagePoolResetValueEnum.ADD_THE_INITIAL_VALUE.toString())) {

                    BigDecimal newPoolQuantity = custUsagePool.getQuantity().add(usagePoolQuantity);

                    CustomerUsagePoolConsumptionEvent qtyChangeEvent =
                            new CustomerUsagePoolConsumptionEvent(
                                    entityId,
                                    custUsagePool.getId(),
                                    custUsagePool.getQuantity(),
                                    newPoolQuantity,
                                    null);

                    custUsagePool.setQuantity(newPoolQuantity);
                    custUsagePool.setInitialQuantity(newPoolQuantity);

                    EventManager.process(qtyChangeEvent);

                } else if (resetValue.equals(UsagePoolResetValueEnum.RESET_TO_INITIAL_VALUE.toString()) || resetValue.equals(UsagePoolResetValueEnum.HOURS_PER_CALENDER_MONTH.toString())) {

                    CustomerUsagePoolConsumptionEvent qtyChangeEvent =
                            new CustomerUsagePoolConsumptionEvent(
                                    entityId,
                                    custUsagePool.getId(),
                                    custUsagePool.getQuantity(),
                                    usagePoolQuantity,
                                    null);

                    custUsagePool.setQuantity(usagePoolQuantity);

                    EventManager.process(qtyChangeEvent);
                }
                logger.debug("Quantity: {}", custUsagePool.getQuantity());
                das.save(custUsagePool);
            }
        }
    }

}
