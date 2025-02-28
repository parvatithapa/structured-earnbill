package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderLinePlanItemDTOEx;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.event.OrderAddedOnInvoiceEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author Khobab
 *
 */
public class CreateOrderForResellerTask extends PluggableTask implements IInternalEventsTask {

	private static final FormatLogger LOG = new FormatLogger(CreateOrderForResellerTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        OrderAddedOnInvoiceEvent.class
    };

	@Override
	public void process(Event event) throws PluggableTaskException {
		LOG.debug("Entering CreateOrderForResellerTask - event: " + event.toString());

		//This handle is only for OrderAddedOnInvoiceEvent
		OrderAddedOnInvoiceEvent invoiceEvent = (OrderAddedOnInvoiceEvent) event;

		CompanyDAS companyDAS = new CompanyDAS();
		CompanyDTO company = companyDAS.find(invoiceEvent.getEntityId());

		if(!(company.getParent() != null)) {
			return;
		}
		LOG.debug("Order belongs to child entity");

		if(!company.isInvoiceAsReseller()) {
			return;
		}
		LOG.debug("Child entity is also a reseller");

		//set base user to reseller customer
		UserDTO reseller = company.getReseller();
		Integer entityId = reseller.getCompany().getId();
		LOG.debug("Entity Id is : " + entityId);
		OrderDTO eventOrder = invoiceEvent.getOrder();

        //we only add products to the parent order which are available to the parent
        List<OrderLineDTO> linesToAdd = new ArrayList<>(eventOrder.getLines().size());
        for(OrderLineDTO line : eventOrder.getLines()) {
            if(line.getItem() == null || line.getItem().isProductAvailableToCompany(company.getParent())) {
                linesToAdd.add(line);
            }
        }

        //if no lines should be added to the new order return
        if(linesToAdd.isEmpty()) {
            return;
        }

        OrderDTO resellerOrder = new OrderDTO(eventOrder);
		resellerOrder.setId(null);
		resellerOrder.setVersionNum(null);
		resellerOrder.setOrderPeriod(new OrderPeriodDAS().find(Constants.ORDER_PERIOD_ONCE));
		resellerOrder.setParentOrder(null);
		resellerOrder.getChildOrders().clear();
		resellerOrder.getOrderProcesses().clear();
		resellerOrder.getLines().clear();
		resellerOrder.setNextBillableDay(null);
		resellerOrder.setActiveSince(invoiceEvent.getStart());
		resellerOrder.setActiveUntil(invoiceEvent.getEnd());
		resellerOrder.setBaseUserByUserId(reseller);
		resellerOrder.setCurrency(new CurrencyDAS().find(reseller.getCurrencyId()));
		resellerOrder.setResellerOrder(invoiceEvent.getOrderId());
		LOG.debug("Active Since: " + resellerOrder.getActiveSince());
		LOG.debug("RESELLER order lines %s", resellerOrder.getLines());

		createLines(linesToAdd, reseller.getUserId(), entityId, resellerOrder);

		LOG.debug("Order copied");
		resellerOrder.setNotes("Automatically created for Reseller. orderId:" + eventOrder.getId() + ", userId:" + eventOrder.getUserId());

		OrderBL orderBL = new OrderBL();
		//process lines to get entity specific prices
		orderBL.processLines(resellerOrder, reseller.getLanguageIdField(), entityId, reseller.getUserId(), reseller.getCurrencyId(), "");
		orderBL.set(resellerOrder);

		try {
			LOG.debug("Processing Lines with - resellerOrder: " + resellerOrder + " , reseller; " + reseller +
					" ,User Id: " + resellerOrder.getUserId() + " ,resellerCurrencyId: " + reseller.getCurrencyId() +
					" ,Pricing Fields: " + resellerOrder.getPricingFields());
            orderBL.recalculate(entityId);
        } catch (ItemDecimalsException e) {
            throw new SessionInternalError("Error when doing credit", RefundOnCancelTask.class, e);
        }

        for (OrderLineDTO line: resellerOrder.getLines()) {
            OrderLineDTO orderLineFromReseller = findMatchingLineOn(eventOrder.getLines(), line);
            if (orderLineFromReseller !=null && orderLineFromReseller.getEvents() != null && !orderLineFromReseller.getEvents().isEmpty()) {
                BigDecimal costAmount = BigDecimal.ZERO;
                BigDecimal quantity = BigDecimal.ZERO;

                //TODO MODULARIZATION: HAndle the cost amount
                for (JbillingMediationRecord eventForLine: orderLineFromReseller.getOrderLineEvents()) {
                    costAmount = costAmount.add(eventForLine.getRatedPrice());
                    quantity = quantity.add(eventForLine.getQuantity());
                }
                line.setAmount(costAmount);
                line.setQuantity(quantity);
                line.setPrice(costAmount.divide(quantity, BigDecimal.ROUND_HALF_EVEN));
            }
        }

		LOG.debug("Reseller Order.Reseller ID %s", resellerOrder.getResellerOrder());

		Integer resellerOrderId= orderBL.create(reseller.getEntity().getId(), null, resellerOrder);
		LOG.debug("Order for reseller created having id %s", resellerOrderId);
		// audit so we know why all these changes happened
        new EventLogger().auditBySystem(entityId, reseller.getId(),
                Constants.TABLE_PUCHASE_ORDER, eventOrder.getId(),
                EventLogger.MODULE_ORDER_MAINTENANCE, EventLogger.ORDER_CREATED_FOR_RESELLER_IN_ROOT,
                resellerOrderId, null, null);
        LOG.debug("REseller Order Created for entityId %d", entityId);

	}

    private OrderLineDTO findMatchingLineOn(List<OrderLineDTO> resellerLines, OrderLineDTO parentLine) {
        Optional<OrderLineDTO> orderLineDTOOptional=resellerLines.stream()
                .filter(rl -> rl.getItemId().equals(parentLine.getItemId()) &&
                                rl.getQuantity().equals(parentLine.getQuantity()) &&
                                rl.getDescription().equals(parentLine.getDescription())
                ).findFirst();

		return orderLineDTOOptional.isPresent()? orderLineDTOOptional.get():null;
    }

    @Override
	public Class<Event>[] getSubscribedEvents() {
		return events;
	}

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
	 * Create order lines for the reseller order using price precedence
	 *
	 * @param orderLines	:	child order lines
	 * @param userId		:	reseller user id
	 * @param entityId		: root entity id
	 * @return
	 */
	private void createLines (List<OrderLineDTO> orderLines, Integer userId, Integer entityId, OrderDTO newOrder) {

		//OrderDTO purchaseOrder = orderLines.iterator().next().getPurchaseOrder();

		for (OrderLineDTO orderLine : orderLines) {
			LOG.debug("Processing Order Line: " + orderLine);

			OrderLineDTO newLine= new OrderLineDTO(orderLine);
            newLine.getAssets().clear();
            newLine.setId(0);
            newLine.setVersionNum(null);
            newLine.setPurchaseOrder(newOrder);

            newOrder.getLines().add(newLine);

            // if order line plan items are not null we have to set them too
            if( orderLine.getOrderLinePlanItems() != null && orderLine.getOrderLinePlanItems().length > 0 ) {
            	newLine.setOrderLinePlanItems(createOrderLinePlanItems(orderLine.getOrderLinePlanItems(), userId, entityId));
            }

		}

	}


	private OrderLinePlanItemDTOEx[] createOrderLinePlanItems(OrderLinePlanItemDTOEx[] orderLinePlanItemDTOs, Integer userId,
			Integer entityId) {

		Set<OrderLinePlanItemDTOEx> planItems = new HashSet<OrderLinePlanItemDTOEx>();

		for (OrderLinePlanItemDTOEx orderLinePlanItem : orderLinePlanItemDTOs) {

			OrderLinePlanItemDTOEx planItem = new OrderLinePlanItemDTOEx();

			planItem.setItemId(orderLinePlanItem.getItemId());
			planItem.setDescription(orderLinePlanItem.getDescription());

			planItems.add(planItem);
		}
		return planItems.toArray(new OrderLinePlanItemDTOEx[planItems.size()]);
	}
}
