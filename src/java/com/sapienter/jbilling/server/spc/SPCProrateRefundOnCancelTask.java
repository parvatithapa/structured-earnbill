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
package com.sapienter.jbilling.server.spc;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.PeriodCancelledEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import org.apache.commons.lang.StringUtils;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.server.integration.common.utility.DateUtility;

public class SPCProrateRefundOnCancelTask extends PluggableTask implements
IInternalEventsTask {

	private static final FormatLogger logger = new FormatLogger(MethodHandles
			.lookup().lookupClass());
	static final String SUBSCRIPTION_ORDER_ID = "Subscription Order Id";

	static final String SERVICE_ID = "ServiceId";
	static final String CUSTOMER_TYPE = "Customer Type";
	static final String PRE_PAID = "Pre Paid";

	@SuppressWarnings("unchecked")
	private static final Class<Event> events[] = new Class[] { PeriodCancelledEvent.class };

	public Class<Event>[] getSubscribedEvents() {
		return events;
	}

	public void process(Event event) {

		// First check if at least 1 Product ID mapping is provided in the Plugin configuration. If not, throw exception.
		String itemId = null;
		Map<String, String> mapParameters = getParameters();

		if(mapParameters == null || mapParameters.isEmpty()){
			throw new SessionInternalError("No credit product mapping provided. At least one credit product mapping should present.");
		}
		for (String propertyKey : mapParameters.keySet()) {

			itemId = mapParameters.get(propertyKey);
			if(itemId != null && !itemId.isEmpty()){
				break;
			}
		}
		if (itemId == null || itemId.isEmpty()) {
			throw new SessionInternalError("No credit product mapping provided. At least one credit product mapping should present.");
		}

		OrderDTO order;

		PeriodCancelledEvent periodCancelledEvent;
		// validate the type of the event
		if (event instanceof PeriodCancelledEvent) {
			periodCancelledEvent = (PeriodCancelledEvent) event;
			order = periodCancelledEvent.getOrder();
			logger.debug(
					"Plug in processing period cancelled event for order {}",
					order.getId());
		} else {
			throw new SessionInternalError(
					"Can't process anything but a period cancel event");
		}
		// local variables
		Integer userId = new OrderDAS().find(order.getId())
				.getBaseUserByUserId().getUserId(); // the order might not be in
		// the session
		Integer entityId = event.getEntityId();
		UserBL userBL;
		ResourceBundle bundle;
		Integer languageId;

		UserDTO user = new UserDAS().findNow(userId);
		CustomerDTO customer =  user.getCustomer();
		//JBSPC-855: Do not generate the credit cancellation order if the "Customer Type" meta field value on Customer is "Pre Paid" & just return.
		MetaFieldValue<String> customerTypeMetaFieldValue = customer.getMetaField(CUSTOMER_TYPE);
		if(null != customerTypeMetaFieldValue &&  
				null != customerTypeMetaFieldValue.getValue() &&  
				((String)customerTypeMetaFieldValue.getValue()).equalsIgnoreCase(PRE_PAID)){
			return;
		}
		// Order is non pro-rated then no need to create an order
		if (!order.getProrateFlagValue()) {
			return;
		}
		if(isOrderCancelledDateLastDateOfBillingCycle(order)){
			return;
		}
		try {
			userBL = new UserBL(userId);
			bundle = ResourceBundle.getBundle("entityNotifications",
					userBL.getLocale());
			languageId = userBL.getEntity().getLanguageIdField();
		} catch (Exception e) {
			throw new SessionInternalError("Error when doing credit",
					SPCProrateRefundOnCancelTask.class, e);
		}

		// create a new order that is the same as the original one, but all
		// negative prices
		OrderBL orderBL = new OrderBL(order);
		OrderDTO newOrder = new OrderDTO(order);
		// reset the ids, so it is a new order
		newOrder.setId(null);
		newOrder.setVersionNum(null);
		newOrder.setParentOrder(null);
		newOrder.getOrderProcesses().clear(); // no invoices created for a new
		// order
		newOrder.getLines().clear();
		newOrder.getChildOrders().clear();
		//Setting Prorate Flag to false so that it does not prorate again on invoice
		//newOrder.setProrateFlag(new Boolean(false));
		// starts where the cancellation starts
		Calendar activeSinceDate = Calendar.getInstance();
		activeSinceDate
		.setTime(order.getActiveUntil() != null ? order
				.getActiveUntil() : TimezoneHelper
				.companyCurrentDate(entityId));
		activeSinceDate.add(Calendar.DATE, 1);

		Calendar activeUntilDate = Calendar.getInstance();
		activeUntilDate.setTime(order.getNextBillableDay());
		activeUntilDate.add(Calendar.DATE, -1);

		newOrder.setActiveSince(activeSinceDate.getTime());
		// ends where the original would invoice next
		newOrder.setActiveUntil(activeUntilDate.getTime());
		newOrder.setNextBillableDay(null);
		OrderStatusDAS orderStatusDAS = new OrderStatusDAS();
		newOrder.setOrderStatus(orderStatusDAS.find(orderStatusDAS.
				getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId)));
		// add some clarification notes
		newOrder.setNotes(bundle.getString("order.credit.notes") + " "
				+ order.getId());
		newOrder.setNotesInInvoice(0);
		newOrder.setCreateDate(new Date());
		newOrder.getDiscountLines().clear();
		//
		// order lines:
		//
		OrderLineDTO newLine = new OrderLineDTO();
		newLine.getAssets().clear();
		Integer inAdvancePlanCreditItemId = getInAdvancePlanCreditItemId(order);

		// Create credit Order only if a valid credit order ID is available
		if (inAdvancePlanCreditItemId != null) {
			newLine.setItemId(inAdvancePlanCreditItemId);

			ItemDTO item = new ItemDAS().findNow(newLine.getItemId());
			newLine.setDescription(item.getDescription(languageId));
			// reset so they get inserted
			newLine.setId(0);
			newLine.setVersionNum(null);
			newLine.setUseItem(false);
			newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			newLine.setPurchaseOrder(newOrder);

			//newLine.setPrice(order.getTotal());
			// make the order negative (refund/credit)
			if(order.getTotal() != null){
				newLine.setPrice(order.getTotal().negate());
			}

			newLine.setQuantity(BigDecimal.ONE);
			//Prorating amount calculation is not required .Will be done automatically. Hence next 2 lines are commented.  
			/*
			BigDecimal newLinePrice = newLine.getPrice().subtract(
					orderBL.prorateTotalAmount(newLine.getPrice(),
							order.getActiveUntil()));
			newLine.setPrice(newLinePrice.negate());
			 */
			List<OrderLineDTO> oldLines = order.getLines();

			String finalServiceId = null;
			//Fetch the service id from the asset
			String assetServiceId = collectAssetServiceIDsForOrder(order.getId());
			// works
			for (OrderLineDTO oldLine : oldLines) {
				for(MetaFieldValue oldLineMetaValue: oldLine.getMetaFields()){

					if(oldLineMetaValue != null && oldLineMetaValue.getField() != null && oldLineMetaValue.getField().getName().equalsIgnoreCase(SERVICE_ID)){
						finalServiceId = (String)oldLineMetaValue.getValue();
					}
				}
				if(finalServiceId != null && !finalServiceId.isEmpty()){
					MetaFieldHelper.setMetaField(getEntityId(), newLine,
							SERVICE_ID, finalServiceId);
				}else if(assetServiceId != null && !assetServiceId.isEmpty()){
					MetaFieldHelper.setMetaField(getEntityId(), newLine,
							SERVICE_ID, assetServiceId);
				}
			}// for


			newOrder.getLines().add(newLine);
	        newOrder.setMetaField(getEntityId(), null, SUBSCRIPTION_ORDER_ID, order.isPlanOrder() ? order.getId() : getSubscriptionOrderId(order));
			if (!order.getLines().isEmpty()) {

				// do the maths
				orderBL.set(newOrder);
				try {
					orderBL.recalculate(entityId);
				} catch (ItemDecimalsException e) {
					throw new SessionInternalError("Error when doing credit",
							SPCProrateRefundOnCancelTask.class, e);
				}

				// save
				Integer newCreditOrderId = orderBL.create(entityId, null,
						newOrder, new HashMap<>());

				// audit so we know why all these changes happened
				new EventLogger().auditBySystem(entityId, userId,
						Constants.TABLE_PUCHASE_ORDER, order.getId(),
						EventLogger.MODULE_ORDER_MAINTENANCE,
						EventLogger.ORDER_CANCEL_AND_CREDIT, newCreditOrderId,
						null, null);

				order.setNotes(order.getNotes() + " - "
						+ bundle.getString("order.cancelled.notes") + " "
						+ newCreditOrderId);
				logger.debug("Credit done with new order {}", newCreditOrderId);
			}// if
		}// if
	}

	private Integer getSubscriptionOrderId(OrderDTO order) {
		MetaFieldValue subscriptionOrderIdMFV = order.getMetaField(SUBSCRIPTION_ORDER_ID);
	    if(null != subscriptionOrderIdMFV && !subscriptionOrderIdMFV.isEmpty()) {
				return (Integer) subscriptionOrderIdMFV.getValue();
		}
		return null;
	}

	public String toString() {
		return "SPCProrateRefundOnCancelTask for events "
				+ Arrays.toString(events);
	}

	private Integer getInAdvancePlanCreditItemId(OrderDTO order) {

		Map<String, String> mapParameters = getParameters();
		String itemId = null;

		for (OrderLineDTO orderLine : order.getLines()) {
			for (Integer itemTypeId : orderLine.getItem().getTypes()) {
				ItemTypeDTO itemType = new ItemTypeDAS().find(itemTypeId);

				for (String propertyKey : mapParameters.keySet()) {

					if (itemType.getDescription().equalsIgnoreCase(propertyKey)) {
						itemId = mapParameters.get(propertyKey);
						break;
					}
				}
			}
		}
		if ((itemId == null) || (itemId != null && itemId.isEmpty())) {
			return null;
		}

		try {
			int productID = Integer.parseInt(itemId);
		} catch (NumberFormatException e) {
			throw new SessionInternalError(""
					+ "Integer expected for Product ID"
					+ ", Actual value is: " + itemId);
		}

		return Integer.valueOf(itemId);
	}

	private static String collectAssetServiceIDsForOrder(Integer oldOrderId) {
		Set<String> assetServiceNumbers = new HashSet<>();
		OrderDTO oldOrder = new OrderDAS().findNow(oldOrderId);
		for(AssetDTO asset : oldOrder.getAssets()) {
			@SuppressWarnings("unchecked")
			MetaFieldValue<String> assetServiceNumber = asset.getMetaField(SERVICE_ID);
			if(null != assetServiceNumber && StringUtils.isNotEmpty(assetServiceNumber.getValue())) {

				String assetServiceID = assetServiceNumber.getValue();
				return assetServiceID;
			} 
		}

		return null;
	}

    private boolean isOrderCancelledDateLastDateOfBillingCycle(OrderDTO order){
		Date periodEndDate = DateUtility.addDaysToDate(order.getNextBillableDay() ,-1);
		if(order.getActiveUntil().equals(periodEndDate)){
			return true;
		}
		return false;
	}
	
}
