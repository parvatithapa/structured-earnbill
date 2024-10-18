package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.UpgradeOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Leandro Zoi on 3/3/18.
 */
public class UpgradePlanTask extends PluggableTask implements IInternalEventsTask {
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            UpgradeOrderEvent.class
    };

    /**
     * @param event event to process
     * @throws PluggableTaskException
     * @see IInternalEventsTask#process(Event)
     */
    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof UpgradeOrderEvent) {
            WebServicesSessionSpringBean api = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            UpgradeOrderEvent upgradeOrderEvent = (UpgradeOrderEvent) event;
            Date today = new Date();
            Integer categoryId = MetaFieldBL.getMetaFieldIntegerValueNullSafety(new EntityBL(getEntityId()).getEntity().getMetaField(Constants.SETUP_META_FIELD));
            OrderBL oldOrderBL = new OrderBL(upgradeOrderEvent.getOldOrderId());
            OrderBL newOrderBL = new OrderBL(upgradeOrderEvent.getUpgradeOrderId());
            OrderStatusDAS statusDAS = new OrderStatusDAS();

            OrderDTO oldOrder = new OrderDTO(oldOrderBL.getEntity());
            OrderDTO newOrder = new OrderDTO(newOrderBL.getEntity());

            oldOrder.setUpgradeOrderId(newOrder.getId());
            oldOrderBL.update(oldOrder.getBaseUserByUserId().getId(), oldOrder, Collections.emptyMap());

            oldOrder.setActiveUntil(today);
            oldOrderBL.update(oldOrder.getBaseUserByUserId().getId(), oldOrder, Collections.emptyMap());

            newOrder.getMetaFields().clear();
            newOrder.getMetaFields().addAll(oldOrder.getMetaFields());
            newOrder.setParentUpgradeOrderId(oldOrder.getId());
            newOrder.setOrderStatus(statusDAS.find(statusDAS.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, upgradeOrderEvent.getEntityId())));
            newOrder.setLines(newOrder.getLines()
                                      .stream()
                                      .filter(line -> line.getItem()
                                                          .getItemTypes()
                                                          .stream()
                                                          .noneMatch(itemType -> categoryId != null && itemType.getId() == categoryId))
                                      .collect(Collectors.toList()));

            List<OrderDTO> childOrdersToRemove = new LinkedList<OrderDTO>(); 
            newOrder.getChildOrders().forEach(childOrder -> {
                childOrder.setLines(childOrder.getLines()
                                              .stream()
                                              .filter(line -> line.isDiscount() ||
                                                              line.getItem()
                                                                  .getItemTypes()
                                                                  .stream()
                                                                  .noneMatch(itemType -> categoryId != null && itemType.getId() == categoryId))
                                              .collect(Collectors.toList()));
                if (childOrder.getLines().isEmpty()) {
                    childOrdersToRemove.add(childOrder);
                }
            });

            childOrdersToRemove.forEach(childOrder -> {
                newOrder.getChildOrders().remove(childOrder);
                api.deleteOrder(childOrder.getId());
            });

            List<MetaFieldValue> oldMetaFields = oldOrder.getLines().stream()
                                                                    .filter(line -> line.getItem().isPlan())
                                                                    .flatMap(line -> line.getMetaFields().stream())
                                                                    .map(metafield -> MetaFieldBL.createValueFromDataType(metafield.getField(),
                                                                                                                          metafield.getValue(),
                                                                                                                          metafield.getField().getDataType()))
                                                                    .collect(Collectors.toList());

            newOrder.getLines().stream()
                               .filter(line -> line.getItem().isPlan())
                               .forEach(line -> line.getMetaFields().addAll(oldMetaFields));

            newOrderBL.update(newOrder.getBaseUserByUserId().getId(), newOrder, Collections.emptyMap());

            InvoiceDTO invoice;
            try {
                BillingProcessBL process = new BillingProcessBL();
                invoice = process.generateInvoice(newOrder.getId(), null, null,
                        api.getCallerId(), companyCurrentDate());
            } catch (Exception e) {
                throw new SessionInternalError(
                        "Error while generating a new invoice");
            }
            InvoiceWS invoiceWS = api.getInvoiceWS(invoice.getId());
            newOrder.getChildOrders().forEach( childOrder ->
                    api.applyOrderToInvoice(childOrder.getId(), invoiceWS)
            );
            
            Integer lastOrderId = api.getLastOrders(oldOrder.getBaseUserByUserId().getId(), 1)[0];
            String orderNotes = new OrderBL(lastOrderId).getDTO().getNotes();
            if (StringUtils.isNotEmpty(orderNotes) && orderNotes.contains(oldOrder.getId().toString())) {
                api.applyOrderToInvoice(lastOrderId, api.getInvoiceWS(invoice.getId()));
            }

            api.createPaymentLink(invoice.getId(), upgradeOrderEvent.getPaymentId());
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
