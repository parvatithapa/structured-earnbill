package com.sapienter.jbilling.server.usagePool.task;


import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.event.NewActiveUntilEvent;
import com.sapienter.jbilling.server.order.event.UpdateProrateFlagEvent;
import com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

public class SPCCustomerUsagePoolTask extends PluggableTask implements IInternalEventsTask{
	
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@SuppressWarnings("unchecked")
	private static final Class<Event>[] events = new Class[]{
		NewActiveUntilEvent.class,
		UpdateProrateFlagEvent.class
	};
	
	@Override
	public Class<Event>[] getSubscribedEvents() {
		return events;
	}
	
	@Override
	public void process(Event event) throws PluggableTaskException {
		logger.debug("Entering SPC customer usage pool task when order active until date is update - event: {}", event);
		if (event instanceof NewActiveUntilEvent) {

			NewActiveUntilEvent updateActiveUntilEvent = (NewActiveUntilEvent) event;

			Integer orderId = updateActiveUntilEvent.getOrderId();
			OrderBL orderBl = new OrderBL(orderId);
			OrderDTO order = orderBl.getDTO();
			UserBL user = new UserBL(updateActiveUntilEvent.getUserId());
			CustomerDTO customer = user.getDto().getCustomer();
			try {
				if (null != order && order.getProrateFlag() && null != customer && customer.hasCustomerUsagePools()
				        && !orderBl.isFreeTrialOrder(order.getId())) {
					Date billingCycleStart = CustomerUsagePoolBL.calcCycleStartDateFromMainSubscription(order.getActiveSince(), customer.getMainSubscription());
					Date billingCycleEnd = customer.getNextInvoiceDate();

					MainSubscriptionDTO mainSubscription = customer.getMainSubscription();
					OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
					int periodUnitId = orderPeriodDTO.getUnitId();
					int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
					PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
					Date newActiveSinceDate = new BasicOrderPeriodTask().calculateStart(order);
					if(Constants.ORDER_BILLING_PRE_PAID.equals(order.getBillingTypeId())
							&& order.getNextBillableDay()!=null) {
					newActiveSinceDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(newActiveSinceDate),
					            orderPeriodDTO.getValue() * -1L));
					}
					List<CustomerUsagePoolDTO> customerUsagePoolsBySubscriptionOrder = new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);
					spcCustomerUsagePool(customer,
											 customerUsagePoolsBySubscriptionOrder,
											 newActiveSinceDate,
											 updateActiveUntilEvent.getNewActiveUntil(),
											 order);

					if (billingCycleStart.compareTo(billingCycleEnd) == 0) {
			            billingCycleEnd = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(billingCycleEnd), orderPeriodDTO.getValue()));
				}
	
				}
			} catch(Exception ex) {
				logger.error(ex.getMessage(), ex.getCause());
			    throw new SessionInternalError(ex);
			}
		} else if (event instanceof UpdateProrateFlagEvent) {
			UpdateProrateFlagEvent updateProrateFlagEvent = (UpdateProrateFlagEvent) event;
            OrderDTO newOrder = updateProrateFlagEvent.getNewOrder();
            Integer orderId = newOrder.getId();
            UserBL user = new UserBL(updateProrateFlagEvent.getUserId());
            CustomerDTO customer = user.getDto().getCustomer();

			if(null != customer && customer.hasCustomerUsagePools()) {
				try {
				     Date newActiveSinceDate = new BasicOrderPeriodTask().calculateStart(newOrder);
				     List<CustomerUsagePoolDTO> customerUsagePoolsBySubscriptionOrder = new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);
				     spcCustomerUsagePool(customer,
											 customerUsagePoolsBySubscriptionOrder,
											 newActiveSinceDate,
											 newOrder.getActiveUntil(),
											 newOrder);
                } catch(Exception ex) {
					logger.error(ex.getMessage(), ex.getCause());
					throw new SessionInternalError(ex);
				}
			}
		}
		
	}
	
	private void spcCustomerUsagePool(CustomerDTO customer, List<CustomerUsagePoolDTO> customerUsagePools, Date newActiveSinceDate, Date newActiveUntilDate, OrderDTO order) {
		CustomerUsagePoolBL bl = new CustomerUsagePoolBL();
		CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
		for(CustomerUsagePoolDTO customerUsagePool: customerUsagePools) {
			
			Date cycleEndDate;
			if (!order.getProrateFlag()) {
				 cycleEndDate = bl.getCycleEndDateForPeriod(customerUsagePool.getUsagePool().getCyclePeriodUnit(),
					 									   customerUsagePool.getUsagePool().getCyclePeriodValue(),
						 								   newActiveSinceDate,
						 								   order.getOrderPeriod(),
						 								   newActiveUntilDate);

			} else {
				cycleEndDate = getSPCCycleEndDateForProratePeriod(customer.getNextInvoiceDate(),newActiveUntilDate, order);
			}
			Date epochDate = Util.getEpochDate();
			if (newActiveSinceDate.after(cycleEndDate) || cycleEndDate.before(customerUsagePool.getCycleStartDate())){
				customerUsagePool.setCycleStartDate(epochDate);
				customerUsagePool.setCycleEndDate(epochDate);
				das.save(customerUsagePool);
			}
			if (null != customer.getNextInvoiceDate() &&
					customer.getNextInvoiceDate().before(new Date()) &&
					customerUsagePool.getCycleEndDate().before(customer.getNextInvoiceDate()) &&
					(null != newActiveUntilDate && customerUsagePool.getCycleEndDate().before(newActiveUntilDate))) {
					logger.debug("Cycle end date not updated for old cycle pools : {} Cycle end date : {} Next invoice date : {}",
						customerUsagePool.getId(), customerUsagePool.getCycleEndDate(), customer.getNextInvoiceDate());
			} else if (null == newActiveUntilDate && customerUsagePool.getCycleStartDate().before(customer.getNextInvoiceDate())) {
					if (customerUsagePool.getCycleStartDate().equals(epochDate)) {
						customerUsagePool.setCycleStartDate(newActiveSinceDate);
					}
					customerUsagePool.setCycleEndDate(DateUtils.addMonths(cycleEndDate, -1));
					das.save(customerUsagePool);
			} else if (!customerUsagePool.getCycleEndDate().equals(epochDate)) {
					customerUsagePool.setCycleEndDate(cycleEndDate);
					das.save(customerUsagePool);
			}
		}

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
    public Date getSPCCycleEndDateForProratePeriod(Date nextInvoiceDate, Date orderActiveUntilDate, OrderDTO order) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(nextInvoiceDate);
        Date newCalculatedNID = DateUtils.addMonths(nextInvoiceDate, 1);
        if(order.getActiveSince().compareTo(nextInvoiceDate) >= 0) {
            OrderPeriodDTO orderPeriodDTO = order.getUser().getCustomer().getMainSubscription().getSubscriptionPeriod();
            cal.add(MapPeriodToCalendar.map(orderPeriodDTO.getUnitId()), orderPeriodDTO.getValue());
        }
        if (null == orderActiveUntilDate) {
            if (nextInvoiceDate.compareTo(new Date()) <= 0) {
                cal.setTime(newCalculatedNID);
                cal.add(Calendar.DATE, -1);
            } else {
                cal.add(Calendar.DATE, -1);
        	}
        } else if (orderActiveUntilDate.after(cal.getTime()) && nextInvoiceDate.compareTo(new Date()) <= 0 ) {
                   if (orderActiveUntilDate.before(newCalculatedNID)) {
                       cal.setTime(orderActiveUntilDate);
                   } else {
                       cal.setTime(newCalculatedNID);
                       cal.add(Calendar.DATE, -1);
                   }
        } else {
            cal.setTime(orderActiveUntilDate);
        }
        cal.set(Calendar.MILLISECOND, 999);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        return cal.getTime();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
