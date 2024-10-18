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

package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.process.task.ProRateOrderPeriodUtil;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

public class BasicOrderPeriodTask extends PluggableTask implements OrderPeriodTask {
    protected Date viewLimit = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BasicOrderPeriodTask.class));
    private List<PeriodOfTime> periods = new ArrayList<>();
    private int backdatedPeriods;

    
    public BasicOrderPeriodTask() {
        viewLimit = null;
    }

    /**
     * Calculates the date that the invoice about to be generated is
     * going to start cover from this order. This IS NOT the invoice
     * date, since an invoice is composed by (potentially) several orders and
     * other invoices
     * @param order
     * @return
     */
     public Date calculateStart(OrderDTO order) throws TaskException {
        Date retValue;
        OrderPeriodDTO orderPeriod = order.getOrderPeriod();

        try {
            if (orderPeriod.getId() == Constants.ORDER_PERIOD_ONCE) {
                // this should be irrelevant, and could be either the order date
                // or this process date ...
                return null;
            }

            if (order.getNextBillableDay() == null) {
                // never been processed
                // If it is open started (with no start date), we assume that
                // it started when it was created
                retValue = order.getActiveSince() == null ?
                            order.getCreateDate() :
                            order.getActiveSince();
            
            } else {
                // the process date means always which day has not been paid for yet.
                Date orderNextBillableDate = order.getNextBillableDay();
                Date orderChangesNextBillableDate = order.calcNextBillableDayFromChanges();

                if (orderChangesNextBillableDate != null && orderNextBillableDate.after(orderChangesNextBillableDate)) {
                    // we have a back-dated order change(s).
                    int multiplicator = orderPeriod.getValue();
                    PeriodUnit period = order.valueOfPeriodUnit();
                    LocalDate startDate = DateConvertUtils.asLocalDate(orderNextBillableDate);
                    LocalDate orderChangesDate = DateConvertUtils.asLocalDate(orderChangesNextBillableDate);
                    int addedPeriods = 0;
                    while (startDate.isAfter(orderChangesDate)) {
                        startDate = period.addTo(startDate, -multiplicator);
                        addedPeriods++;
                    }
                    backdatedPeriods = addedPeriods;
                    retValue = DateConvertUtils.asUtilDate(startDate);

                } else {
                    retValue = orderNextBillableDate;
                }
            }

            if (retValue == null) {
                throw new TaskException("Missing some date fields on " +
                        "order " + order.getId());
        }

        } catch(Exception e) {
            throw new TaskException("Missing some date fields on " +
                    "order " + order.getId());
        }

        // it's important to truncate this date
        return Util.truncateDate(retValue);
    }

    /**
     * This methods takes and order and calculates the end date that is 
     * going to be covered cosidering the starting date and the dates
     * of this process. 
     *
     *
     * @param order
     * @param processDate
     * @param maxPeriods
     * @param startOfBillingPeriod
     * @param inViewLimit
     * @return
     * @throws SessionInternalError
     */
    public Date calculateEnd(OrderDTO order, Date processDate, int maxPeriods,
                             Date startOfBillingPeriod, Date inViewLimit) throws TaskException {

        if (order.getOrderPeriod().getId() ==  Constants.ORDER_PERIOD_ONCE) {
            periods.add(PeriodOfTime.OneTimeOrderPeriodOfTime);
            return null;
        }

        int maxPeriodsUsed = maxPeriods + backdatedPeriods;
        if (order.getProrateFlag()) {
            startOfBillingPeriod = ProRateOrderPeriodUtil.calculateCycleStarts(order, startOfBillingPeriod);
        }

        if(inViewLimit != null) {
            viewLimit = inViewLimit;
        }

        Date endOfPeriod = null;
        final Date firstBillableDate = calculateStart(order);
        GregorianCalendar cal = new GregorianCalendar();
        try {
            // calculate the how far we can see in the future
            // get the period of time from the customer main subscription
            if (viewLimit == null) {
            	if (order.getBillingTypeId().compareTo(Constants.ORDER_BILLING_POST_PAID) == 0 ) { 
            		viewLimit = order.getUser().getCustomer().getNextInvoiceDate();
            	} else {
            		 viewLimit = getViewLimit(order.getUser().getId(), processDate);
            	}
            }
            
            cal.setTime(startOfBillingPeriod);
        
            LOG.debug("Calculating ebp for order %s sbp: %s process date: %s viewLimit:%s", 
                    order.getId(), startOfBillingPeriod, processDate, viewLimit);
            
            //Removing this check as a fix for 5789. This method is not just used in invoice generation.
            //This gets triggered as part of a itemBL.getDTO aswell. So this validation here is counterproductive.
            //There is validation on UI and in billing process to not generate invoices for orders unless they are in INVOICE status}

            if (order.getBillingTypeId().compareTo(
                    Constants.ORDER_BILLING_POST_PAID) == 0 ) {
                // this will move on time from the start of the billing period
                // to the closest multiple period that doesn't go beyond the 
                // visibility date
                
                while (cal.getTime().compareTo(viewLimit) < 0
                        && (order.getActiveUntil() == null || cal.getTime().compareTo(order.getActiveUntil()) <= 0)
                        && periods.size() < maxPeriodsUsed) {
                    Date cycleStarts = cal.getTime();
                    if (CalendarUtils.isSemiMonthlyPeriod(order.getOrderPeriod().getPeriodUnit())) {
                    	cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
                    } else {
                    	cal.add(MapPeriodToCalendar.map(order.getOrderPeriod().getUnitId()), order.getOrderPeriod().getValue());
                    }

                    Date cycleEnds = cal.getTime();
                    cycleEnds = consistentEndOfMonth(order, cycleEnds);
                    cal.setTime(cycleEnds);
                    
                    if (cal.getTime().after(firstBillableDate) &&
                            (!cal.getTime().after(viewLimit) || (order.getActiveUntil() != null && !order.getActiveUntil().after(viewLimit)))) {

                        // calculate the days for this cycle
                        PeriodOfTime cycle = new PeriodOfTime(cycleStarts, cycleEnds, 0);
                        // now create this period
                        PeriodOfTime pt = new PeriodOfTime((periods.size() == 0) ? firstBillableDate : endOfPeriod, cal.getTime(), cycle.getDaysInPeriod());
                        periods.add(pt);
                        endOfPeriod = cal.getTime();
                        LOG.debug("added period %s", pt);
                    }

                    LOG.debug("post paid, now testing:%s(eop) = %s compare %s", cal.getTime(), endOfPeriod, cal.getTime().compareTo(viewLimit));
                }
            } else if (order.getBillingTypeId().compareTo(Constants.ORDER_BILLING_PRE_PAID) == 0) {
                /* here the end of the period will be after the start of the billing
                 * process. This means that is NOT taking ALL the periods that are
                 * visible to this process, just the first one after the start of the
                 * process
                 */
                 
                // bring the date until it goes over the view limit
                // (or it reaches the expiration).
                // This then takes all previous periods that should've been billed
                // by previous processes
                Date myStart = firstBillableDate;
                while (cal.getTime().compareTo(viewLimit) < 0
                        && (order.getActiveUntil() == null || cal.getTime().compareTo(order.getActiveUntil()) <= 0)
                        && periods.size() < maxPeriodsUsed) {
                    Date cycleStarts = cal.getTime();
                    if (CalendarUtils.isSemiMonthlyPeriod(order.getOrderPeriod().getPeriodUnit())) {
                    	cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
                    } else {
                    	cal.add(MapPeriodToCalendar.map(order.getOrderPeriod().getUnitId()), order.getOrderPeriod().getValue());
                    }

                    Date cycleEnds = cal.getTime();
                    cycleEnds = consistentEndOfMonth(order, cycleEnds);
                    cal.setTime(cycleEnds);
                    
                    if (cal.getTime().after(firstBillableDate)) {
                        // calculate the days for this cycle
                        PeriodOfTime cycle = new PeriodOfTime(cycleStarts, cycleEnds, 0);
                        periods.add(new PeriodOfTime(myStart, cal.getTime(), cycle.getDaysInPeriod()));
                        myStart = cal.getTime();
                    }

                    LOG.debug("pre paid, now testing:%s (eop) = %s compare ", cal.getTime(), endOfPeriod, cal.getTime().compareTo(viewLimit));
                }
                
                endOfPeriod = cal.getTime();
                        
            } else {
                throw new TaskException("Order billing type " + order.getBillingTypeId() + " is not supported");
            }
        } catch (Exception e) {
            throw new TaskException(e);
        }

        // verify End of month day when order prorate flag is false.
        if (!order.getProrateFlagValue()) {
            endOfPeriod = verifyEndOfMonthDay(order, endOfPeriod);
        }

        if (endOfPeriod == null) {
            throw new OrderPeriodCalcException("Error calculating for order " + order.getId());
        } else if (order.getActiveUntil() != null && endOfPeriod.after(order.getActiveUntil())) {
            // make sure this date is not beyond the expiration date
            cal.setTime(order.getActiveUntil());
            cal.add(Calendar.DATE, 1);
            endOfPeriod = cal.getTime();
        }
        
        // make sure the last period actually reflects the last adjustments
        if (periods.size() > 0) {
            PeriodOfTime lastOne = periods.get(periods.size() - 1);
            periods.remove(lastOne);

            periods.add(new PeriodOfTime(lastOne.getStart(), endOfPeriod, lastOne.getDaysInCycle()));
        }
        LOG.debug("ebp: %s", endOfPeriod);
        
        return endOfPeriod;
    }

    protected Date getViewLimit(Integer userId, Date processDate) {
        try {
        	UserBL userBL = new UserBL(userId);
            Date viewLimit = userBL.getBillingUntilDate(
            		userBL.getDto().getCustomer().getNextInvoiceDate(),
            		processDate);
            LOG.debug("Calculating view limit for user, " + userId + " is " + viewLimit);
            return viewLimit;
        } catch (Exception e) {
            throw new SessionInternalError("Calculating view limit", BasicOrderPeriodTask.class, e);
        }
    }

    private Date consistentEndOfMonth(OrderDTO order, Date cycleEnds) throws TaskException {
		LOG.debug("Input Cycle End Date %s", cycleEnds);

		MainSubscriptionDTO mainSubscription = order.getBaseUserByUserId().getCustomer().getMainSubscription();
		Integer customersDayOfInvoice = mainSubscription.getNextInvoiceDayOfPeriod();
		Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
		LocalDate date = DateConvertUtils.asLocalDate(cycleEnds);
        LocalDate orderDate = DateConvertUtils.asLocalDate(order.getNextBillableDay() != null ? order.getNextBillableDay() :
                                                                                                order.getActiveSince());

		// consider end of month
		if (Constants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit) &&
                order.getOrderPeriod().getPeriodUnit().getId() == Constants.PERIOD_UNIT_MONTH) {
            if (order.getProrateFlag() && date.getDayOfMonth() < customersDayOfInvoice &&
                    date.lengthOfMonth() > date.getDayOfMonth()) {
                date = date.withDayOfMonth(customersDayOfInvoice);
            } else if (!order.getProrateFlag() && date.getDayOfMonth() < orderDate.getDayOfMonth() &&
                            date.lengthOfMonth() > date.getDayOfMonth()) {
                date = date.withDayOfMonth(orderDate.getDayOfMonth());
            }
        }

		LOG.debug("Adjusted Cycle Ends for order id period %s is: %s ", order.getId(), date);
		return Util.truncateDate(DateConvertUtils.asUtilDate(date));
	}
    
    /*
     * 
    // Last day of the month validation
    // If the current date is the last day of a month, the next date
    // might have to as well.
    */
    protected Date verifyEndOfMonthDay(OrderDTO order, Date periodEndDate) throws TaskException {
        if (periodEndDate == null || order == null) return null;
        
        GregorianCalendar current = new GregorianCalendar();
        // this makes only sense when the order is on monthly periods
        if (order.getOrderPeriod().getUnitId().equals(Constants.PERIOD_UNIT_MONTH)) {
            // the current next invoice date has to be the last day of that month, and not a 31
            current.setTime(calculateStart(order));
            
            if (current.get(Calendar.DAY_OF_MONTH) == current.getActualMaximum(Calendar.DAY_OF_MONTH) &&
                    current.get(Calendar.DAY_OF_MONTH) < 31) {
                // set the end date proposed
                GregorianCalendar edp = new GregorianCalendar();
                GregorianCalendar firstDate = new GregorianCalendar();
                edp.setTime(periodEndDate);
                // the proposed end date should not be the end of the month
                if (edp.get(Calendar.DAY_OF_MONTH) != edp.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    // set the first invoice-able day
                    firstDate.setTime(order.getActiveSince() == null ? order.getCreateDate() : order.getActiveSince());
                    if (firstDate.get(Calendar.DAY_OF_MONTH) > edp.get(Calendar.DAY_OF_MONTH)) {
                        LOG.debug("Order %s.Adjusting next invoice date because end of the month from %s to %s", 
                                order.getId(), edp.get(Calendar.DAY_OF_MONTH), firstDate.get(Calendar.DAY_OF_MONTH));
                        edp.set(Calendar.DAY_OF_MONTH, firstDate.get(Calendar.DAY_OF_MONTH));
                        return edp.getTime();   
                    } else {
                        // In case of semi-monthly 15 -End of month condition 
                    	MainSubscriptionDTO mainSubscription = order.getBaseUserByUserId().getCustomer().getMainSubscription();
                    	Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId(); 
                    	if (Constants.PERIOD_UNIT_SEMI_MONTHLY.equals(mainSubscriptionPeriodUnit)) {
	                    	Integer firstMonth = firstDate.get(Calendar.MONTH);
	                		Integer customersDayOfInvoice = mainSubscription.getNextInvoiceDayOfPeriod();
	                    	if (firstMonth.equals(Calendar.FEBRUARY) && (firstDate.get(Calendar.DAY_OF_MONTH) == edp.get(Calendar.DAY_OF_MONTH)) &&
	                    			 customersDayOfInvoice.equals(Constants.SEMI_MONTHLY_END_OF_MONTH)) {
	                    		edp.set(Calendar.DAY_OF_MONTH, edp.getActualMaximum(Calendar.DAY_OF_MONTH));
	                    		
	                    		return edp.getTime(); 
	                    	}
                    	}
                    	
                    	// the first date of invoice has to be greater than the day being proposed, otherwise
                        // there isn't anything to fix (the fix is to increase the edp by a few days)
                        return periodEndDate;
                    }
                } else {
                    // if the proposed end date is the end of the month, it can't be corrected, since
                    // the correction means adding days.
                    return periodEndDate;
                }
            } else { 
                // if the last next billing date is the 31, adding a month can't be problematic
                // if the last next billing date is not the last day of the month, it can't come from
                // a higher end date
                return periodEndDate;
            }
        } else {
            return periodEndDate;
        }
        
    }

    public List<PeriodOfTime> getPeriods() {
        return periods;
    }
}
