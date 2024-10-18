package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.event.PeriodCancelledEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.strategy.CycleStrategy;
import com.sapienter.jbilling.server.provisioning.event.OrderChangeStatusTransitionEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.CustomerPriceBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountTypePriceBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Created by Leandro Zoi on 9/20/17.
 */
public class CreateCyclesTask extends PluggableTask implements IInternalEventsTask {
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            OrderChangeStatusTransitionEvent.class,
            PeriodCancelledEvent.class
    };

    private static final SortedMap<Date, PriceModelDTO> EMPTY_MODELS = new TreeMap<>();
    private static final String BREAK_LINE = "\n";
    private static final String NOTES = "order.should.not.be.modified";

    /**
     * @param event event to process
     * @throws PluggableTaskException
     * @see IInternalEventsTask#process(Event)
     */
    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof OrderChangeStatusTransitionEvent) {
            OrderChangeDTO orderChangeDTO = ((OrderChangeStatusTransitionEvent) event).getOrderChange();
            OrderDTO order = orderChangeDTO.getOrder();
            PlanDAS planDas = new PlanDAS();
            Integer planId = planDas.findPlanIdByOrderId(order.getId());
            PriceModelDTO priceModel = getPriceModelByHierarchy(order.getUser().getId(),
                    orderChangeDTO.getItem().getId(),
                    orderChangeDTO.getStartDate(), planId);
            if (priceModel == null) {
                priceModel = orderChangeDTO.getItem().getPrice(orderChangeDTO.getStartDate(), order.getUser().getEntity().getId());
            }

            boolean hasTeaser = false; 
            
            while (priceModel != null) {
                if (priceModel.getStrategy() instanceof CycleStrategy) {
                    hasTeaser = true;
                    List<OrderChangeDTO> orderChanges = ((CycleStrategy) priceModel.getStrategy()).generateOrderChangesByCycles(orderChangeDTO,
                                                                                                                                order,
                                                                                                                                priceModel);
                    if (!CollectionUtils.isEmpty(orderChanges)) {
                        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();

                        orderChangeDTO.setEndDate(orderChanges.get(0).getStartDate());
                        orderChangeDTO.setNextBillableDate(orderChangeDTO.getStartDate());
                        orderChangeDAS.save(orderChangeDTO);
                        orderChanges.forEach(orderChangeDAS::save);
                    }
                }

                priceModel = priceModel.getNext();
            }

            if(hasTeaser) {
                setTeaserNote(order);
                
                OrderDAS orderDAS = new OrderDAS();
                orderDAS.save(order);
                orderDAS.flush();
            }
            
        } else if (event instanceof PeriodCancelledEvent) {
            OrderDTO order = ((PeriodCancelledEvent) event).getOrder();
            Date activeUntil = order.getActiveUntil();
            OrderChangeDAS orderChangeDAS = new OrderChangeDAS();

            order.getLines().stream()
                            .flatMap(orderLine -> orderLine.getOrderChanges().stream())
                            .filter(changeDTO -> !changeDTO.getStartDate().before(activeUntil) &&
                                                  changeDTO.getStatus().getApplyToOrder().equals(ApplyToOrder.NO))
                            .forEach(orderChangeDAS::delete);
        }
    }

    private void setTeaserNote(OrderDTO order) {
        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", new UserBL(order.getUserId()).getLocale());
        String teaserNote = bundle.getString(NOTES);
        if (StringUtils.isBlank(order.getNotes())) {
            order.setNotes(teaserNote);
        } else {
            if(!order.getNotes().contains(teaserNote)) {
                order.setNotes(order.getNotes()
                        .concat(BREAK_LINE)
                        .concat(bundle.getString(NOTES)));
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private PriceModelDTO getPriceModelByHierarchy(Integer userId, Integer itemId, Date pricingDate, Integer planId) {
        SortedMap<Date, PriceModelDTO> models;
        PriceModelDTO model = null;
        CustomerDTO customer = new UserBL(userId).getEntity().getCustomer();

        while (customer.getUseParentPricing()) {
            customer = customer.getParent();
            model = getPriceModelByHierarchy(customer.getBaseUser().getId(), itemId, pricingDate, planId);
        }

        if (model != null) {
            return model;
        }

        // 1. Customer pricing resolution
        models = collectPriceModels(new CustomerPriceBL(userId).getAllCustomerPricesForDate(itemId, Boolean.FALSE, pricingDate, null));
        if (PriceModelBL.containsEffectiveModel(models, pricingDate)) {
            return PriceModelBL.getPriceForDate(models, pricingDate);
        }

        // 2. Account Type pricing resolution
        models = collectPriceModels(new AccountTypePriceBL(customer.getAccountType()).getPricesForItemAndPricingDate(itemId, pricingDate));
        if (PriceModelBL.containsEffectiveModel(models, pricingDate)) {
            return PriceModelBL.getPriceForDate(models, pricingDate);
        }

        // 3. Plan Pricing resolution - consider only the plan prices from the customer pricing
        PlanItemDTO item = new CustomerPriceBL(userId).getPriceForDate(itemId, true, pricingDate,planId);
        if (item != null && PriceModelBL.containsEffectiveModel(item.getModels(), pricingDate)) {
            return PriceModelBL.getPriceForDate(item.getModels(), pricingDate);
        }

        return null;
    }

    private SortedMap<Date, PriceModelDTO> collectPriceModels(List<PlanItemDTO> items) {
        if (!CollectionUtils.isEmpty(items)) {
            return items.stream()
                        .flatMap(item -> item.getModels().entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> {
                                    throw new IllegalStateException();
                                },
                                TreeMap::new
                        ));
        }

        return EMPTY_MODELS;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
