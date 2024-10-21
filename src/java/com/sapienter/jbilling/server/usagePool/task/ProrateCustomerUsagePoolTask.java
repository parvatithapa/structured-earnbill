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

package com.sapienter.jbilling.server.usagePool.task;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.event.NewActivateOrderEvent;
import com.sapienter.jbilling.server.order.event.NewActiveSinceEvent;
import com.sapienter.jbilling.server.order.event.NewActiveUntilEvent;
import com.sapienter.jbilling.server.order.event.UpdateProrateFlagEvent;
import com.sapienter.jbilling.server.order.event.NewSuspendOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.QuantitySupplier;
import com.sapienter.jbilling.server.usagePool.UsagePoolResetValueEnum;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProrateCustomerUsagePoolTask
 * This is an internal events task that subscribes to NewActiveUntilEvent.
 * Prorate customer usage pool quantity and cycle end date when a order active until date is update
 * @author Ashok Kale
 * @since 11-Feb-2014
 */

public class ProrateCustomerUsagePoolTask extends PluggableTask implements IInternalEventsTask {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@SuppressWarnings("unchecked")
	private static final Class<Event>[] events = new Class[]{
		NewActiveSinceEvent.class,
		NewActiveUntilEvent.class,
		NewActivateOrderEvent.class,
		NewSuspendOrderEvent.class,
		UpdateProrateFlagEvent.class
	};

	public Class<Event>[] getSubscribedEvents () {
		return events;
	}

	@Override
	public void process(Event event) throws PluggableTaskException {

		logger.debug("Entering Prorate customer usage pool when order active until date is update - event: {}", event);
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
					prorateCustomerUsagePool(customer,
											 customerUsagePoolsBySubscriptionOrder,
											 newActiveSinceDate,
											 updateActiveUntilEvent.getNewActiveUntil(),
											 order);

					if (billingCycleStart.compareTo(billingCycleEnd) == 0) {
			            billingCycleEnd = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(billingCycleEnd), orderPeriodDTO.getValue()));
				}

					reRateUsageOrder(updateActiveUntilEvent.getUserId(),
									 updateActiveUntilEvent.getEntityId(),
									 billingCycleStart,
									 billingCycleEnd,
									 OrderStatusFlag.INVOICE);
				}
			} catch(Exception ex) {
				logger.error(ex.getMessage(), ex.getCause());
			    throw new SessionInternalError(ex);
			}
	} else if (event instanceof NewActiveSinceEvent) {
		NewActiveSinceEvent updateActiveSinceEvent = (NewActiveSinceEvent) event;
		Integer orderId = updateActiveSinceEvent.getNewOrder().getId();
		UserBL user = new UserBL(updateActiveSinceEvent.getUserId());
		CustomerDTO customer = user.getDto().getCustomer();
		OrderDTO newOrder = updateActiveSinceEvent.getNewOrder();
		try {
			if (null != newOrder && newOrder.getProrateFlag() && null != customer && customer.hasCustomerUsagePools()) {

				Date billingCycleStart = CustomerUsagePoolBL.calcCycleStartDateFromMainSubscription(updateActiveSinceEvent.getOldOrder().getActiveSince(), customer.getMainSubscription());
				Date billingCycleEnd = customer.getNextInvoiceDate();

				Date newActiveSinceDate = new BasicOrderPeriodTask().calculateStart(newOrder);
					List<CustomerUsagePoolDTO> customerUsagePoolsBySubscriptionOrder = new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);
					prorateCustomerUsagePool(customer,
											 customerUsagePoolsBySubscriptionOrder,
											 newActiveSinceDate,
											 newOrder.getActiveUntil(),
											 newOrder);

				reRateUsageOrder(updateActiveSinceEvent.getUserId(),
									 updateActiveSinceEvent.getEntityId(),
									 billingCycleStart,
									 billingCycleEnd,
									 OrderStatusFlag.INVOICE);
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
				Date billingCycleStart = CustomerUsagePoolBL.calcCycleStartDateFromMainSubscription(newOrder.getActiveSince(), customer.getMainSubscription());
				Date billingCycleEnd = customer.getNextInvoiceDate();

				Date newActiveSinceDate = new BasicOrderPeriodTask().calculateStart(newOrder);
				List<CustomerUsagePoolDTO> customerUsagePoolsBySubscriptionOrder = new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);
					prorateCustomerUsagePool(customer,
											 customerUsagePoolsBySubscriptionOrder,
											 newActiveSinceDate,
											 newOrder.getActiveUntil(),
											 newOrder);

					reRateUsageOrder(updateProrateFlagEvent.getUserId(),
									 updateProrateFlagEvent.getEntityId(),
									 billingCycleStart,
									 billingCycleEnd,
									 OrderStatusFlag.INVOICE);

				} catch(Exception ex) {
					logger.error(ex.getMessage(), ex.getCause());
					throw new SessionInternalError(ex);
				}
			}
		} else if (event instanceof NewSuspendOrderEvent) {
			NewSuspendOrderEvent newSuspendOrderEvent = (NewSuspendOrderEvent) event;

			Integer orderId = newSuspendOrderEvent.getOrderId();
			OrderDTO order = new OrderBL(orderId).getDTO();
			boolean isPostPaid = Constants.ORDER_BILLING_POST_PAID.equals(order.getBillingTypeId());
			CustomerDTO customer = new UserBL(newSuspendOrderEvent.getUserId()).getDto().getCustomer();

			try {
				if (customer != null && customer.hasCustomerUsagePools() && isPostPaid) {
					Date billingCycleStart = CustomerUsagePoolBL.calcCycleStartDateFromMainSubscription(order.getActiveSince(), customer.getMainSubscription());
					Date newActiveSinceDate = new BasicOrderPeriodTask().calculateStart(order);
					List<CustomerUsagePoolDTO> customerUsagePoolsBySubscriptionOrder = new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);

					prorateCustomerUsagePool(customer,
											 customerUsagePoolsBySubscriptionOrder,
											 newActiveSinceDate,
											 newSuspendOrderEvent.getSuspendedDate(),
											 order);

					reRateUsageOrder(newSuspendOrderEvent.getUserId(),
									 newSuspendOrderEvent.getEntityId(),
									 billingCycleStart,
									 newSuspendOrderEvent.getSuspendedDate(),
									 OrderStatusFlag.INVOICE,
									 OrderStatusFlag.SUSPENDED_AGEING);
				}
			} catch(Exception ex) {
				logger.error(ex.getMessage(), ex.getCause());
				throw new SessionInternalError(ex);
			}
		} else if (event instanceof NewActivateOrderEvent) {
			NewActivateOrderEvent newActivateOrderEvent = (NewActivateOrderEvent) event;

			Integer orderId = newActivateOrderEvent.getOrderId();
			OrderDTO order = new OrderBL(orderId).getDTO();
			CustomerDTO customer = new UserBL(newActivateOrderEvent.getUserId()).getDto().getCustomer();
			PlanDTO plan = newActivateOrderEvent.getPlan();
			Date startDate = newActivateOrderEvent.getStartDate();
			boolean isPostPaid = Constants.ORDER_BILLING_POST_PAID.equals(order.getBillingTypeId());
			try {
				if(isPostPaid){
					CustomerUsagePoolBL customerUsagePoolBL = new CustomerUsagePoolBL();
					Date billingCycleStart = CustomerUsagePoolBL.calcCycleStartDateFromMainSubscription(order.getActiveSince(), customer.getMainSubscription());

					for (CustomerUsagePoolDTO customerUsagePool: new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId)) {
						CustomerUsagePoolDTO newCustomerUsagePool = customerUsagePoolBL.getCreateCustomerUsagePoolDto(customerUsagePool.getUsagePool().getId(),
																													  customer.getId(),
																													  startDate,
																													  order.getOrderPeriod(),
																													  plan,
																													  order.getActiveUntil(),
																													  order.getCreateDate(),
																													  order,
																													  customer.getNextInvoiceDate());

						Date endDate = customerUsagePoolBL.getCycleEndDateForPeriod(customerUsagePool.getUsagePool().getCyclePeriodUnit(),
																					customerUsagePool.getUsagePool().getCyclePeriodValue(),
																					customerUsagePool.getCycleStartDate(),
																					order.getOrderPeriod(),
																					order.getActiveUntil());

						if (endDate.after(newCustomerUsagePool.getCycleStartDate())) {
							customerUsagePool.setInitialQuantity(customerUsagePool.getInitialQuantity().add(newCustomerUsagePool.getInitialQuantity()));
							customerUsagePool.setQuantity(customerUsagePool.getInitialQuantity());
							customerUsagePool.setCycleEndDate(newCustomerUsagePool.getCycleEndDate());
							customerUsagePoolBL.createOrUpdateCustomerUsagePool(customerUsagePool);

							reRateUsageOrder(newActivateOrderEvent.getUserId(),
											 newActivateOrderEvent.getEntityId(),
											 billingCycleStart,
											 customerUsagePool.getCycleEndDate(),
											 OrderStatusFlag.INVOICE,
											 OrderStatusFlag.SUSPENDED_AGEING);
						} else {
							LocalDate newStartDate = DateConvertUtils.asLocalDate(newCustomerUsagePool.getCycleStartDate());
							customerUsagePool.setCycleStartDate(DateConvertUtils.asUtilDate(newStartDate.withDayOfMonth(1)));
							customerUsagePool.setInitialQuantity(newCustomerUsagePool.getInitialQuantity());
							customerUsagePool.setQuantity(newCustomerUsagePool.getQuantity());
							customerUsagePool.setCycleEndDate(newCustomerUsagePool.getCycleEndDate());
							customerUsagePoolBL.createOrUpdateCustomerUsagePool(customerUsagePool);
						}
					}
				}
			} catch(Exception ex) {
				logger.error(ex.getMessage(), ex.getCause());
				throw new SessionInternalError(ex);
			}
		}
	}

	private void prorateCustomerUsagePool(CustomerDTO customer, List<CustomerUsagePoolDTO> customerUsagePools, Date newActiveSinceDate, Date newActiveUntilDate, OrderDTO order) {
		CustomerUsagePoolBL bl = new CustomerUsagePoolBL();
		CustomerUsagePoolDAS das = new CustomerUsagePoolDAS();
		for(CustomerUsagePoolDTO customerUsagePool: customerUsagePools) {
			QuantitySupplier quantitySupplier = new QuantitySupplier(customerUsagePool.getUsagePool(),customerUsagePool.getCustomer().getNextInvoiceDate(), null);
			BigDecimal newProrateQuantity = bl.
						getCustomerUsagePoolProrateQuantity(
						        customer.getNextInvoiceDate(),
								newActiveSinceDate, newActiveUntilDate,
								quantitySupplier.get(),
								customer.getMainSubscription(),
								customerUsagePool.getUsagePool().getCyclePeriodUnit(),
								customerUsagePool.getUsagePool().getCyclePeriodValue());
			Date cycleEndDate;
			if (!order.getProrateFlag()) {
				 cycleEndDate = bl.getCycleEndDateForPeriod(customerUsagePool.getUsagePool().getCyclePeriodUnit(),
					 									   customerUsagePool.getUsagePool().getCyclePeriodValue(),
						 								   newActiveSinceDate,
						 								   order.getOrderPeriod(),
						 								   newActiveUntilDate);

				 newProrateQuantity = customerUsagePool.getUsagePool().getQuantity();
			} else {
				cycleEndDate = bl.getCycleEndDateForProratePeriod(customer.getNextInvoiceDate(),newActiveUntilDate, order);
			}

			if(UsagePoolResetValueEnum.ADD_THE_INITIAL_VALUE.equals(customerUsagePool.getUsagePool().getUsagePoolResetValue())) {
			    BigDecimal lastRemainingQuantity = customerUsagePool.getLastRemainingQuantity();
			    if(lastRemainingQuantity!=null) {
			        newProrateQuantity = newProrateQuantity.add(lastRemainingQuantity);
			        logger.debug("Added Last Remaiming Qunatity {} in Prorated Qnatity and Total Qunatity is {}", lastRemainingQuantity,
			                newProrateQuantity);
			    } else {
			        logger.warn("Last Remaining Qunaity is null!");
			    }
			}
			customerUsagePool.setInitialQuantity(newProrateQuantity);
			customerUsagePool.setQuantity(newProrateQuantity);
			customerUsagePool.setCycleStartDate(newActiveSinceDate);
			customerUsagePool.setCycleEndDate(cycleEndDate);
			das.save(customerUsagePool);
		}

		List<Integer> proratedCustomerUsagePoolIds = getCustomerUsagePoolId(customerUsagePools);
		for(CustomerUsagePoolDTO customerUsagePool: customer.getCustomerUsagePoolList()) {
			if(!proratedCustomerUsagePoolIds.contains(customerUsagePool.getId())) {
				customerUsagePool.setQuantity(customerUsagePool.getInitialQuantity());
				das.save(customerUsagePool);
			}
		}
	}

	private void reRateUsageOrder(int userId, int entityId, Date billingCycleStart, Date billingCycleEnd, OrderStatusFlag... flags) {
	    OrderDAS orderDAS = new OrderDAS();
	    List<Integer> orders = orderDAS.getCustomersAllOneTimeUsageOrdersInCurrentBillingCycle(userId, billingCycleStart,
                billingCycleEnd, flags);
	    UsageOrderReRater.reRateUsageOrder(entityId, orders);
	}

	private List<Integer> getCustomerUsagePoolId(List<CustomerUsagePoolDTO> customerUsagePools) {
		return customerUsagePools.stream()
								 .map(CustomerUsagePoolDTO::getId)
								 .collect(Collectors.toList());
	}

    @Override
    public boolean isSingleton() {
        return true;
    }
}
