/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

/**
 * CustomerUsagePoolBL
 * Server side code for handling of Customer Usage Pool association.
 * It has functions for CRUD of customer usage pool, calculation of cycle end date,
 * and a business method for evaluation task that updates the customer usage pool
 * from scheduled batch program.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerUsagePoolBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final IUsagePoolEvaluationTask defaultTask = new DefaultUsagePoolEvaluationTask();
    private CustomerUsagePoolDAS customerUsagePoolDas = null;
    private CustomerUsagePoolDTO customerUsagePool = null;
    private boolean prorateForFirstPeriod = true;

    public CustomerUsagePoolBL() {
        init();
    }

    public CustomerUsagePoolBL(Integer customerUsagePoolId) {
        try {
            init();
            set(customerUsagePoolId);
        } catch (Exception e) {
            throw new SessionInternalError("Setting Usage Pool", CustomerUsagePoolBL.class, e);
        }
    }

    private void init() {
        customerUsagePoolDas = new CustomerUsagePoolDAS();
    }

    public void set(Integer customerUsagePoolId) {
        customerUsagePool = customerUsagePoolDas.find(customerUsagePoolId);
    }

    /**
     * Parametrized constructor that returns a CustomerUsagePoolWS
     * object instance from the CustomerUsagePoolDTO parameter given to it.
     * This constructor is useful to return the ws object after converting it from dto.
     * @param dto
     */
    public CustomerUsagePoolWS getWS(CustomerUsagePoolDTO dto) {
        if (customerUsagePool == null) {
            customerUsagePool = dto;
        }
        return getCustomerUsagePoolWS(dto);
    }

    public static CustomerUsagePoolWS getCustomerUsagePoolWS(CustomerUsagePoolDTO dto) {

        CustomerUsagePoolWS ws = new CustomerUsagePoolWS();
        ws.setId(dto.getId());
        ws.setQuantity(null != dto.getQuantity() ? dto.getQuantity().toString() : "");
        if (dto.getCustomer() != null) {
            ws.setCustomerId(dto.getCustomer().getId());
            ws.setUserId(dto.getCustomer().getBaseUser().getId());
        }
        if (dto.getUsagePool() != null) {
            ws.setUsagePoolId(dto.getUsagePool().getId());
        }
        if (dto.getPlan() != null) {
            ws.setPlanId(dto.getPlan().getId());
        }
        ws.setCycleStartDate(dto.getCycleStartDate());
        ws.setCycleEndDate(dto.getCycleEndDate());
        ws.setInitialQuantity(dto.getInitialQuantity());
        ws.setOrderId(dto.getOrder().getId());
        ws.setVersionNum(dto.getVersionNum());
        ws.setUsagePool(UsagePoolBL.getUsagePoolWS(dto.getUsagePool()));
        ws.setLastRemainingQuantity(dto.getLastRemainingQuantity()!=null ?
                dto.getLastRemainingQuantity().toString() : null);
        return ws;
    }

    public CustomerUsagePoolDTO getEntity() {
        return customerUsagePool;
    }

    /**
     * This method converts this CustomerUsagePoolWS object instance
     * into CustomerUsagePoolDTO and returns the same
     * @return CustomerUsagePoolDTO
     */
    public static final CustomerUsagePoolDTO getDTO(CustomerUsagePoolWS ws) {
        CustomerUsagePoolDTO dto = new CustomerUsagePoolDTO();
        if (ws.getId() != null) {
            dto.setId(ws.getId());
        }
        dto.setCustomer(new CustomerDAS().find(ws.getCustomerId()));
        dto.setUsagePool(new UsagePoolDAS().find(ws.getUsagePoolId()));
        dto.setPlan(new PlanDAS().find(ws.getPlanId()));
        dto.setQuantity(null != ws.getQuantity() && !ws.getQuantity().isEmpty() ? new BigDecimal(ws.getQuantity()) : null);
        dto.setCycleEndDate(ws.getCycleEndDate());
        dto.setVersionNum(ws.getVersionNum());
        return dto;
    }

    /**
     * Persists the customer usage pool, after getting the dto object that needs to be saved.
     * The same method can be used to create a new customer usage pool or update an existing one.
     * @param customerUsagePoolDto
     * @return customerUsagePoolDto
     */
    public CustomerUsagePoolDTO createOrUpdateCustomerUsagePool(CustomerUsagePoolDTO customerUsagePoolDto) {

        if( customerUsagePoolDto.getCycleStartDate().after(customerUsagePoolDto.getCycleEndDate())) {
            logger.debug("invalid period dates for customer usage pool: {}", customerUsagePoolDto);
            return null;
        }

        if (customerUsagePoolDto.getId() > 0 ) {
            this.customerUsagePool = new CustomerUsagePoolDAS().findForUpdate(customerUsagePoolDto.getId());
        } else {
            this.customerUsagePool = new CustomerUsagePoolDTO();
        }

        if (null != customerUsagePoolDto.getCustomer()) {
            this.customerUsagePool.setCustomer(customerUsagePoolDto.getCustomer());
        }

        if (null != customerUsagePoolDto.getUsagePool()) {
            this.customerUsagePool.setUsagePool(customerUsagePoolDto.getUsagePool());
        }

        if (null != customerUsagePoolDto.getPlan()) {
            this.customerUsagePool.setPlan(customerUsagePoolDto.getPlan());
        }

        if (null != customerUsagePoolDto.getQuantity()) {
            this.customerUsagePool.setQuantity(customerUsagePoolDto.getQuantity());
        }

        if (null != customerUsagePoolDto.getQuantity()) {
            this.customerUsagePool.setInitialQuantity(customerUsagePoolDto.getInitialQuantity());
        }

        if (null != customerUsagePoolDto.getCycleEndDate()) {
            this.customerUsagePool.setCycleEndDate(customerUsagePoolDto.getCycleEndDate());
        }

        if (null != customerUsagePoolDto.getOrder()) {
            this.customerUsagePool.setOrder(customerUsagePoolDto.getOrder());
        }

        if (null != customerUsagePoolDto.getCycleStartDate()) {
            this.customerUsagePool.setCycleStartDate(customerUsagePoolDto.getCycleStartDate());
        }

        this.customerUsagePool.setVersionNum(customerUsagePoolDto.getVersionNum());

        this.customerUsagePool = save(this.customerUsagePool);

        return this.customerUsagePool != null ? this.customerUsagePool : null;
    }

    /**
     * This method returns a list of CustomerUsagePoolDTO based on customer id.
     * @return List<CustomerUsagePoolDTO>
     */
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByCustomerId() {
        return customerUsagePoolDas.findAllCustomerUsagePoolsByCustomerId(customerUsagePool.getCustomer().getId());
    }

    /**
     * This method returns a list of CustomerUsagePoolDTO based on
     * customer id provided to it as an input parameter.
     * @return List<CustomerUsagePoolDTO>
     */
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByCustomerId(Integer customerId) {
        return new CustomerUsagePoolDAS().getCustomerUsagePoolsByCustomerId(customerId);
    }

    /**
     * This method calculates the cycle end date for a customer usage pool
     * based on cycle period unit and cycle period value specified on the
     * system level usage pool. In case the cycle period unit is specified as
     * 'Billing Periods', then the order period is added to period start date.
     * The cycle end date is calculated from the period start date provided to it.
     * @param cyclePeriodUnit
     * @param cyclePeriodValue
     * @param periodStartDate
     * @param orderPeriod
     * @return Date - cycleEndDate of CustomerUsagePool
     */
    public Date getCycleEndDateForPeriod(String cyclePeriodUnit, Integer cyclePeriodValue, Date periodStartDate, OrderPeriodDTO orderPeriod, Date activeUntilDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodStartDate);
        if (cyclePeriodUnit.equals(Constants.USAGE_POOL_CYCLE_PERIOD_DAYS)) {
            cal.add(Calendar.DATE, cyclePeriodValue);
        } else if (cyclePeriodUnit.equals(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)) {
            cal.add(Calendar.MONTH, cyclePeriodValue);
        } else if (cyclePeriodUnit.equals(Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS) && null != orderPeriod) {
            Integer orderPeriodValue = orderPeriod.getValue();
            cal.add(MapPeriodToCalendar.map(orderPeriod.getUnitId()), orderPeriodValue);
        }

        // Active until date of order less than calculated cycle end date then set cycle end date as active until date.
        if (null != activeUntilDate && activeUntilDate.compareTo(cal.getTime()) <= 0) {
            cal.setTime(activeUntilDate);
            cal.add(Calendar.DATE, 1);
        }

        cal.add(Calendar.DATE, -1);
        return setTimeToEndOfDay(cal.getTime());
    }


    private Date calcCycleDateFromMainSubscription(String cyclePeriodUnit, Integer cyclePeriodValue, Date periodStartDate, MainSubscriptionDTO mainSubscription) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodStartDate);
        OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
        if (cyclePeriodUnit.equals(Constants.USAGE_POOL_CYCLE_PERIOD_DAYS)) {
            cal.add(Calendar.DATE, cyclePeriodValue);
        } else if (cyclePeriodUnit.equals(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)) {
            cal.add(Calendar.MONTH, cyclePeriodValue);
        } else if (cyclePeriodUnit.equals(Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS) && null != orderPeriodDTO) {
            int unit = MapPeriodToCalendar.map(orderPeriodDTO.getUnitId());
            cal.add(unit, orderPeriodDTO.getValue());
            if(unit == Calendar.MONTH) {
                int noOfdays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int daysToAdd = noOfdays < mainSubscription.getNextInvoiceDayOfPeriod().intValue() ? noOfdays :
                    mainSubscription.getNextInvoiceDayOfPeriod();
                cal.set(Calendar.DAY_OF_MONTH, daysToAdd);
            }
        }
        return setTimeToEndOfDay(cal.getTime());
    }

    /**
     * When order prorate flag is ON. This method calculates the cycle end date
     * for a customer usage pool based on customer next invoice date and order active until date
     * If active until date date less than next invoice date then set active until date as cycle end date else
     * set next invoice date as cycle end date
     *
     * @param nextInvoiceDate
     * @param orderActiveUntilDate
     * @param order
     * @return
     */
    public Date getCycleEndDateForProratePeriod(Date nextInvoiceDate, Date orderActiveUntilDate, OrderDTO order) {
    	Calendar cal = Calendar.getInstance();
        cal.setTime(nextInvoiceDate);
        if(order.getActiveSince().compareTo(nextInvoiceDate) >= 0) {
            OrderPeriodDTO orderPeriodDTO = order.getUser().getCustomer().getMainSubscription().getSubscriptionPeriod();
            cal.add(MapPeriodToCalendar.map(orderPeriodDTO.getUnitId()), orderPeriodDTO.getValue());
        }
        if (null == orderActiveUntilDate ||
                orderActiveUntilDate.after(cal.getTime())) {
            cal.add(Calendar.DATE, -1);
        } else {
            cal.setTime(orderActiveUntilDate);
        }
        return setTimeToEndOfDay(cal.getTime());
    }
    
    /**
     * get customer usage pool prorate quantity using following parameters
     * @param nextInvoiceDate
     * @param startPeriod
     * @param orderActiveUntilDate
     * @param quantity
     * @param mainSubscription
     * @param cyclePeriodUnit
     * @param cyclePeriodValue
     * @return
     */
    public BigDecimal getCustomerUsagePoolProrateQuantity(Date nextInvoiceDate, Date startPeriod,
            Date orderActiveUntilDate, BigDecimal quantity, MainSubscriptionDTO mainSubscription,
            String cyclePeriodUnit, Integer cyclePeriodValue) {
        // calculate the days for this cycle
        Date cycleStarts = calcCycleStartDateFromMainSubscription(startPeriod, mainSubscription);
        Calendar cal = new GregorianCalendar();
        cal.setTime(startPeriod);
        int noOfPeriods = 0;

        if(cycleStarts.compareTo(nextInvoiceDate) == 0) {
            OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
            int periodUnitId = orderPeriodDTO.getUnitId();
            int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
            PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
            nextInvoiceDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(nextInvoiceDate),
                    orderPeriodDTO.getValue()));
        }

        Date endOfPeriod;
        if (null == orderActiveUntilDate || (orderActiveUntilDate.compareTo(nextInvoiceDate) >= 0)) {
            endOfPeriod = nextInvoiceDate;
        } else {
            endOfPeriod = new Date(orderActiveUntilDate.getTime()+(24*60*60*1000));
        }

        // Calculated No of billing period in between Subscription start date and end date
        while (cal.getTime().compareTo(nextInvoiceDate) < 0) {
            Date cycleStartDate = calcCycleStartDateFromMainSubscription(cal.getTime(), mainSubscription);
            Date cycleEndDate = calcCycleDateFromMainSubscription(cyclePeriodUnit, cyclePeriodValue, cycleStartDate, mainSubscription);

            cal.setTime(cycleEndDate);
            noOfPeriods++;
        }

        // now create this period
        PeriodOfTime fullBillingCycle = new PeriodOfTime(cycleStarts, nextInvoiceDate, 0);
        PeriodOfTime period = new PeriodOfTime(startPeriod, endOfPeriod, fullBillingCycle.getDaysInPeriod());

        if (noOfPeriods > 1) {
            quantity = quantity.multiply(new BigDecimal(noOfPeriods));
        }

        return calculateProrateQuantityForPeriod(quantity, period);
    }

    /**
     * Calculate prorate quantity using order period
     * @param fullFreeQuantity
     * @param period
     * @return
     */
    private BigDecimal calculateProrateQuantityForPeriod (BigDecimal fullFreeQuantity, PeriodOfTime period) {

        if (period == null || fullFreeQuantity == null) {
            logger.warn("Called with null parameters");
            return null;
        }

        // this is an amount from a one-time order, not a real period of time
        if (period.getDaysInCycle() == 0) {
            return fullFreeQuantity;
        }

        // if this is not a fraction of a period, don't bother making any calculations
        if (period.getDaysInCycle() == period.getDaysInPeriod()) {
            return fullFreeQuantity;
        }

        BigDecimal oneDayQuantity = fullFreeQuantity.divide(new BigDecimal(period.getDaysInCycle()), Constants.BIGDECIMAL_SCALE,
                Constants.BIGDECIMAL_ROUND);

        return oneDayQuantity.multiply(new BigDecimal(period.getDaysInPeriod())).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,
                Constants.BIGDECIMAL_ROUND);
    }

    /**
     * Calculated customer billing cycle star date using customer main subscription.
     * @param periodStart
     * @param mainSubscription
     * @return
     */
    public static Date calcCycleStartDateFromMainSubscription (Date periodStart,
            MainSubscriptionDTO mainSubscription) {
        Date calculatedValue;
        Calendar cal = new GregorianCalendar();

        Integer nextInvoiceDaysOfPeriod = mainSubscription.getNextInvoiceDayOfPeriod();
        Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
        Integer mainSubscriptionPeriodValue = mainSubscription.getSubscriptionPeriod().getValue();

        cal.setTime(periodStart);
        if (Constants.PERIOD_UNIT_WEEK.equals(mainSubscriptionPeriodUnit)) {
            cal.set(Calendar.DAY_OF_WEEK, nextInvoiceDaysOfPeriod);
        } else if (Constants.PERIOD_UNIT_SEMI_MONTHLY.equals(mainSubscriptionPeriodUnit)) {
            Date expectedStartDate = CalendarUtils.findNearestTargetDateInPastForSemiMonthly(cal,
                    nextInvoiceDaysOfPeriod);
            cal.setTime(expectedStartDate);
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }

        if (Constants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
            // consider end of month case
            if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= nextInvoiceDaysOfPeriod
                    && Constants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                cal.add(Calendar.DATE, nextInvoiceDaysOfPeriod - 1);
            }
        }

        if (!Constants.PERIOD_UNIT_SEMI_MONTHLY.equals(mainSubscriptionPeriodUnit)) {
            calculatedValue = CalendarUtils.findNearestTargetDateInPast(cal.getTime(), periodStart,
                    nextInvoiceDaysOfPeriod, mainSubscriptionPeriodUnit, mainSubscriptionPeriodValue);
        } else {
            calculatedValue = cal.getTime();
        }

        return calculatedValue;
    }

    /**
     * This method provides the CustomerUsagePoolDTO based on the various input fields given below.
     * The CustomerUsagePoolDTO returned by this method is to be used for persisting it to the db.
     * @param usagePoolId
     * @param customerId
     * @param subscriptionStartDate
     * @param orderPeriod
     * @return CustomerUsagePoolDTO
     */
    public CustomerUsagePoolDTO getCreateCustomerUsagePoolDto(Integer usagePoolId, Integer customerId,
            Date subscriptionStartDate, OrderPeriodDTO orderPeriod, PlanDTO plan, Date orderActiveUntilDate,
            Date orderCreatedDate, OrderDTO order, Date nextInvoiceDate) {

        Date cycleEndDate;
        CustomerUsagePoolDTO customerUsagePoolDTO = new CustomerUsagePoolDTO();
        UsagePoolDTO usagePoolDto = new UsagePoolDAS().find(usagePoolId);
        CustomerDTO customer = new CustomerDAS().find(customerId);
        customerUsagePoolDTO.setCustomer(customer);
        customerUsagePoolDTO.setUsagePool(usagePoolDto);
        customerUsagePoolDTO.setPlan(plan);

        QuantitySupplier quantitySupplier = new QuantitySupplier(usagePoolDto, nextInvoiceDate, order.getActiveSince());

        if (!order.getProrateFlag()) {
            customerUsagePoolDTO.setQuantity(quantitySupplier.get());
            customerUsagePoolDTO.setInitialQuantity(quantitySupplier.get());
            cycleEndDate = getCycleEndDateForPeriod(usagePoolDto.getCyclePeriodUnit(),
                    usagePoolDto.getCyclePeriodValue(), subscriptionStartDate, orderPeriod, orderActiveUntilDate);
        } else {
            //If Order prorate flag is ON then prorate customer usage pool quantity and calculate expected cycle end date
            //for prorate period using customer next invoice date and order active until date.
            BigDecimal newProrateQuantity = quantitySupplier.get();
            if(prorateForFirstPeriod){
                newProrateQuantity = getCustomerUsagePoolProrateQuantity(nextInvoiceDate,subscriptionStartDate,orderActiveUntilDate, quantitySupplier.get(), customer.getMainSubscription(),
                        usagePoolDto.getCyclePeriodUnit(), usagePoolDto.getCyclePeriodValue());
            }
            customerUsagePoolDTO.setQuantity(newProrateQuantity);
            customerUsagePoolDTO.setInitialQuantity(newProrateQuantity);
            cycleEndDate = getCycleEndDateForProratePeriod(nextInvoiceDate, orderActiveUntilDate, order);
        }

        if (!order.getProrateFlag() && cycleEndDate.compareTo(TimezoneHelper.companyCurrentDateByUserId(customer.getBaseUser().getUserId())) < 0) {
            cycleEndDate = getCycleEndDateForPeriod(usagePoolDto.getCyclePeriodUnit(),
                    usagePoolDto.getCyclePeriodValue(), orderCreatedDate, orderPeriod, orderActiveUntilDate);
        }

        if (null != orderActiveUntilDate && cycleEndDate.compareTo(orderActiveUntilDate) > 0) {
            cycleEndDate = setTimeToEndOfDay(orderActiveUntilDate);
        }

        customerUsagePoolDTO.setCycleEndDate(cycleEndDate);
        customerUsagePoolDTO.setOrder(order);

        customerUsagePoolDTO.setCycleStartDate(removeTime(subscriptionStartDate));
        customerUsagePoolDTO.setVersionNum(1);
        return customerUsagePoolDTO;
    }

    private static final Integer MAX_RESULTS = 1000;
    /**
     * This is a method that gets called from CustomerUsagePoolEvaluationTask.
     * It evaluates and updates the customer usage pools by looking at all customer usage pool records
     * that are eligible for update. The update is done for 2 fields: cycle end date and quantity.
     * @throws PluggableTaskException
     */
    public void triggerCustomerUsagePoolEvaluation(Integer entityId, Date runDate) {
        IUsagePoolEvaluationTask evaluationTask = loadEvaluationTaskForEntity(entityId);

        // Need to set runDate time to the end of day in order to also fetch those CUP's cycle_end_date equals to the runDate
        List<Integer> customerUsagePoolsAllRecords = customerUsagePoolDas.findCustomerUsagePoolsForEvaluation(
                entityId, setTimeToEndOfDay(runDate), null, null);
        List<List<Integer>> subLists = partition(customerUsagePoolsAllRecords, MAX_RESULTS);
        for(final List<Integer> customerUsagePools : subLists) {
            Executor executor = Context.getBean("asyncTaskExecutor");
            IMethodTransactionalWrapper transaction = Context.getBean(IMethodTransactionalWrapper.class);
            executor.execute(()-> {
                logger.debug("customerUsagePools:{}", customerUsagePools);
                for (Integer customerUsagePoolId : customerUsagePools) {
                    transaction.executeInNewTransaction(()-> {
                        try {
                            // process each customer usage pool in new transaction.
                            evaluationTask.evaluateCustomerUsagePool(new CustomerUsagePoolEvaluationEvent(
                                    customerUsagePoolId, entityId, runDate));
                        } catch(Exception ex) {
                            // roll back transaction in exception.
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            logger.error("customer usage pool evaluation failed for customer "
                                    + "usage pool id {}, entity {}, date {}", customerUsagePoolId, entityId, runDate);
                        }
                    });
                }
            });
        }
    }

    /**
     *
     * @param list
     * @param batchSize
     * @return
     */
    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> parts = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            parts.add(new ArrayList<>(list.subList(i, Math.min(size, i + batchSize))));
        }
        return parts;
    }

    private IUsagePoolEvaluationTask loadEvaluationTaskForEntity(Integer entityId) {
        try {
            PluggableTaskManager<IUsagePoolEvaluationTask> taskManager = new PluggableTaskManager<>(entityId,
                    new PluggableTaskTypeCategoryDAS().findByInterfaceName(IUsagePoolEvaluationTask.class.getName()).getId());
            IUsagePoolEvaluationTask task = taskManager.getNextClass();
            if(null == task) {
                logger.debug("No IUsagePoolEvaluationTask configured for entity {}, "
                        + "so using DefaultUsagePoolEvaluationTask", entityId);
                return defaultTask;
            }
            return task;
        } catch(PluggableTaskException pluggableTaskException) {
            throw new SessionInternalError("exception occur during loading plugin for entity "+ entityId, pluggableTaskException);
        }
    }

    /**
     * Calculate expected next invoice date for calculate prorate quantity and cycle end date of customer usage pool.
     * if the customer does not exist
     *
     * @param userDto
     * Commented this test case because it is unused and may be it should use in future.
     */
    /*private Date getExpectedCustomerNextInvoiceDate (CustomerDTO customer) {

        MainSubscriptionDTO mainSubscription = customer.getMainSubscription();
        Date createdDate = customer.getBaseUser().getCreateDatetime();
        GregorianCalendar cal = new GregorianCalendar();

        Date nextInvoiceDate = customer.getNextInvoiceDate();

        createdDate = Util.truncateDate(TimezoneHelper.currentDateForTimezone(customer.getBaseUser().getEntity().getTimezone()));

        logger.debug("Initial run date: {}. Next invoice date for user: {} retrieved from orders is: {} ", createdDate,
                customer.getBaseUser().getUserId(), nextInvoiceDate);

        Integer customerDayOfInvoice = mainSubscription.getNextInvoiceDayOfPeriod();
        Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();

        cal.setTime(nextInvoiceDate == null ? createdDate : Util.truncateDate(nextInvoiceDate));

        // consider end of month case
        if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= customerDayOfInvoice
                && Constants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {

            if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_MONTH)) {
                cal.set(Calendar.DAY_OF_MONTH, customerDayOfInvoice);
            } else if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_WEEK)) {
                cal.set(Calendar.DAY_OF_WEEK, customerDayOfInvoice);
            } else if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_YEAR)) {
                cal.set(Calendar.DAY_OF_YEAR, customerDayOfInvoice);
            } else if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_SEMI_MONTHLY)) {
                cal.setTime(new UserBL().addSemiMonthlyPeriod(cal, customerDayOfInvoice));
            }
        }

        if (!mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_SEMI_MONTHLY)) {
            // if next invoice date exists set the day to next invoice day of period
            // greater than the next invoice date
            LocalDate temporal = DateConvertUtils.asLocalDate(nextInvoiceDate);
            PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(new DateTime((nextInvoiceDate == null ? createdDate.getTime() : nextInvoiceDate.getTime())).getDayOfMonth(), PeriodUnitDTO.SEMI_MONTHLY);
            LocalDate initialNextInvoiceDate = DateConvertUtils.asLocalDate(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, customerDayOfInvoice);
            LocalDate nextRunDate = DateConvertUtils.asLocalDate(cal.getTime());

            while (nextRunDate.isAfter(temporal) || !(temporal.isAfter(initialNextInvoiceDate))) {
                temporal = periodUnit.addTo(temporal, 1);
            }
            nextInvoiceDate = DateConvertUtils.asUtilDate(temporal);
        } else {
            nextInvoiceDate = cal.getTime();
        }

        logger.debug("Final next invoice date for user {} is: {} ", customer.getBaseUser().getUserId(), nextInvoiceDate);
        // user.getCustomer would always update parent customer and hence userDto.getCustomer is used.
        return nextInvoiceDate;
    }*/

    /**
     * This method returns a list of CustomerUsagePoolDTO based on order id.
     *
     * @return List<CustomerUsagePoolDTO>
     */
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByOrderId() {
        return customerUsagePoolDas.findAllCustomerUsagePoolsByOrderId(customerUsagePool.getOrder().getId());
    }

    /**
     * This method returns a list of CustomerUsagePoolDTO based on order id provided to it as an input parameter.
     *
     * @return List<CustomerUsagePoolDTO>
     */
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByOrderId(Integer orderId) {
        return new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);
    }

    /**
     * This method returns a list of CustomerUsagePoolDTO based on user id and asset identifier provided to it as input
     * parameters.
     *
     * @return List<CustomerUsagePoolDTO>
     */
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByUserAndAsset(Integer userId, String assetIdentifier) {
        return new CustomerUsagePoolDAS().getCustomerUsagePoolsByUserAndAssetIdentifier(userId, assetIdentifier);
    }

    public boolean isAssetSubscribedToCustomerUsagePool(Integer userId, String assetIdentifier) {
        List<CustomerUsagePoolDTO> customerUsagePools = getCustomerUsagePoolsByUserAndAsset(userId, assetIdentifier);
        return customerUsagePools.stream().anyMatch(usagePool -> usagePool.getId() == this.customerUsagePool.getId());
    }

    public boolean isProrateForFirstPeriod() {
        return prorateForFirstPeriod;
    }

    public void setProrateForFirstPeriod(boolean prorateForFirstPeriod) {
        this.prorateForFirstPeriod = prorateForFirstPeriod;
    }

    /**
     * Save method that will check first if the customer usage pool is not a duplicate one
     *
     * @param customerUsagePool
     * @return
     */
    public CustomerUsagePoolDTO save(CustomerUsagePoolDTO customerUsagePool) {
        if( customerUsagePool.getCycleStartDate().after(customerUsagePool.getCycleEndDate())) {
            logger.debug("invalid period dates for customer usage pool: {}", customerUsagePool);
            return null;
        }
        if (!customerUsagePoolDas.exist(customerUsagePool)) {
            return customerUsagePoolDas.save(customerUsagePool);
        } else {
            logger.error("Customer Usage Pool Map already exist: {}", customerUsagePool);
            return null;
        }
    }

    public Date removeTime(Date date) {
        return DateUtils.truncate(date, java.util.Calendar.DAY_OF_MONTH);
    }

    public Date setTimeToEndOfDay(Date date) {
        return DateUtils.addMilliseconds(DateUtils.ceiling(date, Calendar.DATE), -1);
    }
}
