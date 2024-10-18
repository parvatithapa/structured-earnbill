/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

/*
 * Created on Apr 28, 2003
 *
 */
package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerStatusChangeHistoryDAS;
import com.sapienter.jbilling.server.user.db.CustomerStatusChangeHistoryDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies if the order should be included in a porcess considering its
 * active range dates. It takes the billing period type in consideration
 * as well. This probably would've been easier to do if EJB/QL had support
 * for Date types
 */
public class BasicOrderFilterTask extends PluggableTask implements OrderFilterTask {

    private static final Logger logger = LoggerFactory.getLogger(BasicOrderFilterTask.class);
    protected Date billingUntil = null;
    
    public BasicOrderFilterTask() {
        billingUntil = null;
    }
    
    /* (non-Javadoc)
     * @see com.sapienter.jbilling.server.pluggableTask.OrderFilterTask#isApplicable(com.sapienter.betty.interfaces.OrderEntityLocal)
     */
    public boolean isApplicable(OrderDTO order, BillingProcessDTO process) throws TaskException {
        boolean retValue = true;
        
        GregorianCalendar cal = new GregorianCalendar();

        logger.debug("running isApplicable for order {} billingUntil = {}", order.getId(), billingUntil);
        // some set up to simplify the code
        Date activeUntil = null;
        if (order.getActiveUntil() != null) {
            activeUntil = Util.truncateDate(order.getActiveUntil());
        }

        Date activeSince;
        if (order.getActiveSince() != null) {
            activeSince = Util.truncateDate(order.getActiveSince());
        } else {
            // in fact, an open starting point doesn't make sense (an order reaching
            // inifinitly backwards). So we default to the creation date
            activeSince = Util.truncateDate(order.getCreateDate());
        }
        
        try {
            // calculate how far in time this process applies
        	UserBL userBL = new UserBL(order.getUser());
            Date customerNextInvoiceDate = userBL.getDto().getCustomer().getNextInvoiceDate();

            if (billingUntil == null) {
            	
            	// Get date till which the billing process sees the orders
            	
            	// billingUntil should be customerNextInvoiceDate when :-
            	//	1. Non-prorate Post paid
            	//	2. Prorate Daily Post paid
            	//	3. Non-prorate Daily Pre paid
            	if ((order.getBillingTypeId().compareTo(Constants.ORDER_BILLING_POST_PAID) == 0)) {
	            	billingUntil = customerNextInvoiceDate;
	            } else {
	            	billingUntil = userBL.getBillingUntilDate(customerNextInvoiceDate, process.getBillingDate());
	            }

                logger.debug("Calculating billing until for user, {} is {}", order.getUser(), billingUntil);
            }

            EventLogger eLog = EventLogger.getInstance();
            if (order.getBillingTypeId().compareTo(Constants.ORDER_BILLING_POST_PAID) == 0) {

                if (order.isSuspended()) {
                    CustomerStatusChangeHistoryDTO lastSuspendedHistory = new CustomerStatusChangeHistoryDAS().getLastSuspendedPeriod(userBL.getDto().getUserId());
                    if (null == lastSuspendedHistory ){
                        return false;
                    }
                    retValue = order.getBillingStartDate().before(lastSuspendedHistory.getModifiedAt());
                }

                // check if it is too early 
            	// one time order not picked up when active since date is after billing until.  
                if((!activeSince.before(billingUntil) && !order.getPeriodId().equals(Constants.ORDER_PERIOD_ONCE)) || 
                		(activeSince.after(billingUntil) && order.getPeriodId().equals(Constants.ORDER_PERIOD_ONCE))) {
                    // didn't start yet
                    eLog.info(process.getEntity().getId(), 
                              order.getBaseUserByUserId().getId(), order.getId(),
                              EventLogger.MODULE_BILLING_PROCESS,
                              EventLogger.BILLING_PROCESS_NOT_ACTIVE_YET,
                              Constants.TABLE_PUCHASE_ORDER);

                    retValue = false;
                // One time only orders don't need to check for periods                     
                } else if (!order.getPeriodId().equals(Constants.ORDER_PERIOD_ONCE)) {
                    // check that there's at least one period since this order
                    // started, otherwise it's too early to bill
                    cal.setTime(activeSince);
                    
                    if (CalendarUtils.isSemiMonthlyPeriod(order.getOrderPeriod().getPeriodUnit())) {
                    	cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
                    } else {
                    	cal.add(MapPeriodToCalendar.map(order.getOrderPeriod().getUnitId()), order.getOrderPeriod().getValue());
                    }
                    
                    // When Order pro-rating is on and if cal.getTime() is getter than process billing date 
                    // then use process billing date other wise use cal.getTime()
                    Date firstBillingDate;
                    if (order.getProrateFlag()) {
                    	firstBillingDate = thisOrActiveUntil((cal.getTime().compareTo(process.getBillingDate()) > 0 ? 
                								process.getBillingDate() : cal.getTime()), activeUntil);
                    } else {
                    	firstBillingDate = thisOrActiveUntil(cal.getTime(), activeUntil);
                    }

                    if (firstBillingDate.after(billingUntil)) {
            				eLog.info(process.getEntity().getId(), 
                                      order.getBaseUserByUserId().getId(),
                                      order.getId(),
                                      EventLogger.MODULE_BILLING_PROCESS,
                                      EventLogger.BILLING_PROCESS_ONE_PERIOD_NEEDED,
                                      Constants.TABLE_PUCHASE_ORDER);
                    
                        retValue = false; // gotta wait for the first bill

                    }
                }

                // there must be at least one period after the last paid day
                if (retValue && order.getNextBillableDay() != null) {
                    cal.setTime(order.getNextBillableDay());
                    if (CalendarUtils.isSemiMonthlyPeriod(order.getOrderPeriod().getPeriodUnit())) {
                    	cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
                    } else {
                    	cal.add(MapPeriodToCalendar.map(order.getOrderPeriod().getUnitId()), order.getOrderPeriod().getValue().intValue());
                    }
                    
                    // When Order pro-rating is on and if cal.getTime() is getter than process billing date 
                    // then use process billing date other wise use cal.getTime()
                    Date endOfNextPeriod = null;
                	if (order.getProrateFlag()) {
                		endOfNextPeriod = thisOrActiveUntil((cal.getTime().compareTo(process.getBillingDate()) > 0 ? process.getBillingDate() : cal.getTime()), activeUntil);
                    } else {
                    	endOfNextPeriod = thisOrActiveUntil(cal.getTime(), activeUntil); 
                    }

                    if (endOfNextPeriod.after(billingUntil)) {
                        eLog.info(process.getEntity().getId(), 
                                  order.getBaseUserByUserId().getId(),
                                  order.getId(),
                                  EventLogger.MODULE_BILLING_PROCESS,
                                  EventLogger.BILLING_PROCESS_RECENTLY_BILLED,
                                  Constants.TABLE_PUCHASE_ORDER);
                        
                        retValue = false;
                    }

                    // may be it's actually billed to the end of its life span
                    if (activeUntil != null && //may be it's immortal ;)
                            order.getNextBillableDay().compareTo(activeUntil) > 0) {
                        // this situation shouldn't have happened
                        logger.warn("Order {} should've been flagged out in the previous process", order.getId());
                        eLog.warning(process.getEntity().getId(),
                                     order.getBaseUserByUserId().getId(),
                                     order.getId(),
                                     EventLogger.MODULE_BILLING_PROCESS,
                                     EventLogger.BILLING_PROCESS_WRONG_FLAG_ON,
                                     Constants.TABLE_PUCHASE_ORDER);

                        OrderBL orderBL = new OrderBL(order);
                        orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()));
                        order.setNextBillableDay(null);
                    }
                }
                // post paid orders can't be too late to process 

            } else if (order.getBillingTypeId().compareTo(Constants.ORDER_BILLING_PRE_PAID) == 0) {
                if (order.isSuspended()) {
                    return false;
                }

                //  if it has a billable day
                if (order.getNextBillableDay() != null) {
                    // now check if there's any more time to bill as far as this
                    // process goes
                    logger.debug("order {} nbd = {} bu = {}", order.getId(), order.getNextBillableDay(), billingUntil);
                    if (order.getNextBillableDay().compareTo(billingUntil) >= 0) {
                        retValue = false;
                        eLog.info(process.getEntity().getId(), 
                                  order.getBaseUserByUserId().getId(),
                                  order.getId(),
                                  EventLogger.MODULE_BILLING_PROCESS,
                                  EventLogger.BILLING_PROCESS_RECENTLY_BILLED,
                                  Constants.TABLE_PUCHASE_ORDER);
                        
                    }
                    
                    // check if it is all billed already
                    if (activeUntil != null && order.getNextBillableDay().compareTo(activeUntil) > 0) {
                        retValue = false;
                        logger.warn("Order {} was set to be processed but the next billable date is after the active until", order.getId());
                        eLog.warning(process.getEntity().getId(), 
                                     order.getBaseUserByUserId().getId(),
                                     order.getId(),
                                     EventLogger.MODULE_BILLING_PROCESS,
                                     EventLogger.BILLING_PROCESS_EXPIRED,
                                     Constants.TABLE_PUCHASE_ORDER);

                        OrderBL orderBL = new OrderBL(order);
                        //orderBL.setStatus(null, Constants.DEFAULT_ORDER_FINISHED_STATUS_ID);
                        orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()));
                        order.setNextBillableDay(null);                                   
                    }
                }
                
                // see if it is too early
                if (retValue && activeSince != null) {
                    
                	if ((!activeSince.before(billingUntil))) {
                        // This process is not including the time this order
                        // starts
                        retValue = false;
                        eLog.info(process.getEntity().getId(), 
                                  order.getBaseUserByUserId().getId(),
                                  order.getId(),
                                  EventLogger.MODULE_BILLING_PROCESS,
                                  EventLogger.BILLING_PROCESS_NOT_ACTIVE_YET,
                                  Constants.TABLE_PUCHASE_ORDER);
                        
                    }
                }
                
                // finally if it is too late (would mean a warning)
                if (retValue && activeUntil != null) {
                    if (process.getBillingDate().after(activeUntil)) {
                        // how come this order has some period yet to be billed, but
                        // the active is already history ? It should've been billed
                        // in a previous process
                        eLog.warning(process.getEntity().getId(), 
                                     order.getBaseUserByUserId().getId(),
                                     order.getId(),
                                     EventLogger.MODULE_BILLING_PROCESS,
                                     EventLogger.BILLING_PROCESS_EXPIRED,
                                     Constants.TABLE_PUCHASE_ORDER);

                        logger.warn("Order with time yet to be billed not included!");
                    }
                }
            } else {
                throw new TaskException("Billing type of this order is not supported:" + order.getBillingTypeId());
            }
        } catch (NumberFormatException e) {
            logger.debug("Exception converting types", e);
            throw new TaskException("Exception with type conversions: " + e.getMessage());
        } catch (SessionInternalError e) {
            logger.debug("Internal exception ", e);
            throw new TaskException(e);
        }

        logger.debug("Order {} filter:{}", order.getId(), retValue);
        return retValue;
    }

    private Date thisOrActiveUntil(Date thisDate, Date activeUntil) {
        if (activeUntil == null){
            return thisDate;
        }

        return activeUntil.before(thisDate) ? activeUntil : thisDate;
    }
    
    private boolean isMonthlyCustomerBillingCycle(MainSubscriptionDTO customerBillingCycle) {
    	OrderPeriodDTO billingCyclePeriod = customerBillingCycle.getSubscriptionPeriod();
    	return billingCyclePeriod.getUnitId().equals(Constants.PERIOD_UNIT_MONTH) && 1 == billingCyclePeriod.getValue();
	}
    
    private boolean isSemiMonthlyCustomerBillingCycle(MainSubscriptionDTO customerBillingCycle) {
    	OrderPeriodDTO billingCyclePeriod = customerBillingCycle.getSubscriptionPeriod();
    	return billingCyclePeriod.getUnitId().equals(Constants.PERIOD_UNIT_SEMI_MONTHLY) && 1 == billingCyclePeriod.getValue();
	}
    
    private boolean isWeeklyCustomerBillingCycle(MainSubscriptionDTO customerBillingCycle) {
    	OrderPeriodDTO billingCyclePeriod = customerBillingCycle.getSubscriptionPeriod();
    	return billingCyclePeriod.getUnitId().equals(Constants.PERIOD_UNIT_WEEK) && 1 == billingCyclePeriod.getValue();
	}
    
    private boolean isDailyCustomerBillingCycle(MainSubscriptionDTO customerBillingCycle) {
    	OrderPeriodDTO billingCyclePeriod = customerBillingCycle.getSubscriptionPeriod();
    	return billingCyclePeriod.getUnitId().equals(Constants.PERIOD_UNIT_DAY) && 1 == billingCyclePeriod.getValue();
	}
}
