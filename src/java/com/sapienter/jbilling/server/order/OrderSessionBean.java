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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.event.NewOrderAndChangeEvent;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.spa.Distributel911AddressUpdateEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * This is the session facade for the orders in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 */
@Transactional(propagation = Propagation.REQUIRED)
public class OrderSessionBean implements IOrderSessionBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public void reviewNotifications(Date today)
            {

        try {
            OrderBL order = new OrderBL();
            order.reviewNotifications(today);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO getOrder(Integer orderId) {
        try {
            OrderDAS das = new OrderDAS();
            OrderDTO order = das.findNow(orderId);
            order.touch();
            return order;

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO getOrderEx(Integer orderId, Integer languageId) {
        try {
            OrderDAS das = new OrderDAS();
            OrderDTO order = das.find(orderId);
            order.addExtraFields(languageId);
            order.touch();
            das.detach(order);
            Collections.sort(order.getLines(), new OrderLineComparator());
            return order;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO setStatus(Integer orderId, Integer statusId,
            Integer executorId, Integer languageId)
            {
        try {
            OrderBL order = new OrderBL(orderId);
            order.setStatus(executorId, statusId);
            OrderDTO dto = order.getDTO();
            dto.addExtraFields(languageId);
            dto.touch();
            return dto;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

     public void delete(Integer id, Integer executorId)
            {
        try {
            // now get the order
            OrderBL bl = new OrderBL(id);
            bl.delete(executorId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

    }

    public OrderPeriodDTO[] getPeriods(Integer entityId, Integer languageId)
            {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            return bl.getPeriods(entityId, languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderPeriodDTO getPeriod(Integer languageId, Integer id)
            {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            OrderPeriodDTO dto =  bl.getPeriod(languageId, id);
            dto.touch();

            return dto;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void setPeriods(Integer languageId, OrderPeriodDTO[] periods)
            {
        OrderBL bl = new OrderBL();
        bl.updatePeriods(languageId, periods);
    }

    public void addPeriod(Integer entityId, Integer languageId)
            {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            bl.addPeriod(entityId, languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Boolean deletePeriod(Integer periodId)
            {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            return bl.deletePeriod(periodId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO addItem(Integer itemID, BigDecimal quantity, OrderDTO order, Integer languageId, Integer userId,
                            Integer entityId) {

        logger.debug("Adding item {} quantity:{}", itemID, quantity);

        OrderBL bl = new OrderBL(order);
        bl.addItem(itemID, quantity, languageId, userId, entityId, order.getCurrencyId(), TimezoneHelper.companyCurrentDate(entityId));
        return order;
    }

    public OrderDTO addItem(Integer itemID, Integer quantity, OrderDTO order, Integer languageId, Integer userId,
                            Integer entityId) {

        return addItem(itemID, new BigDecimal(quantity), order, languageId, userId, entityId);
    }

    public OrderDTO recalculate(OrderDTO modifiedOrder, Integer entityId) {
        OrderBL bl = new OrderBL();
        bl.set(modifiedOrder);
        bl.recalculate(entityId);
        return bl.getDTO();
    }

    public Integer createUpdate(Integer entityId, Integer executorId, Integer languageId,
                                OrderDTO order, Collection<OrderChangeDTO> orderChanges, Collection<Integer> deletedChanges) {
        Integer retValue;
        try {
            OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(order);
            // linked set to preserve hierarchy order in collection, from root to child
            LinkedHashSet<OrderDTO> ordersForUpdate = OrderHelper.findOrdersInHierarchyFromRootToChild(rootOrder);

            OrderDTO persistedOrder = null;
            for (OrderDTO updatedOrder : ordersForUpdate) {
                if (updatedOrder.getId() != null) {
                    persistedOrder = new OrderBL(updatedOrder.getId()).getDTO();
                    break;
                }
            }
            List<Integer> ordersForDelete = new LinkedList<>();
            if (persistedOrder != null) {
                for (OrderDTO existedOrder : OrderHelper.findOrdersInHierarchyFromRootToChild(OrderHelper.findRootOrderIfPossible(persistedOrder))) {
                    boolean found = false;
                    for (OrderDTO updatedOrder : ordersForUpdate) {
                    	if (existedOrder.getId().equals(updatedOrder.getId())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // add in reverse order: from child to parent
                        ordersForDelete.add(0, existedOrder.getId());
                    }
                }
            }
            Date onDate = com.sapienter.jbilling.common.Util.truncateDate(TimezoneHelper.companyCurrentDate(entityId));
            if (persistedOrder != null) {
                // evict orders hierarchy to update as transient entities
                new OrderDAS().detachOrdersHierarchy(persistedOrder);
            }
            
            Integer swapPlanItemId = null;
            Integer existingPlanItemId = null;
            Date effectiveDate = null;
            for (OrderChangeDTO orderChangeDTO : orderChanges) {
            	if (null == swapPlanItemId && null != orderChangeDTO.getItem() && orderChangeDTO.getItem().isPlan() && orderChangeDTO.getQuantity().compareTo(BigDecimal.ZERO) > 0) { 
            		swapPlanItemId = orderChangeDTO.getItem().getId();
                    effectiveDate = orderChangeDTO.getStartDate();
            	} else if (null == existingPlanItemId && null != orderChangeDTO.getItem() && orderChangeDTO.getItem().isPlan() && orderChangeDTO.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            		existingPlanItemId = orderChangeDTO.getItem().getId();
            	}
            	
            	if (null != swapPlanItemId && null != existingPlanItemId) {
            		break;
            	}
            }
            
            OrderDTO targetOrder = OrderBL.updateOrdersFromDto(persistedOrder, rootOrder);
            Map<OrderLineDTO, OrderChangeDTO> appliedChanges = OrderChangeBL.applyChangesToOrderHierarchy(targetOrder, orderChanges, onDate, true, entityId);

            // validate final hierarchy
            OrderHierarchyValidator hierarchyValidator = new OrderHierarchyValidator();
            hierarchyValidator.buildHierarchy(targetOrder);
            String error = hierarchyValidator.validate(entityId);
            if (error != null) {
                throw new SessionInternalError("Incorrect orders hierarchy: " + error, new String[]{error});
            }

            // linked set to preserve hierarchy order in collection, from root to child
            ordersForUpdate = OrderHelper.findOrdersInHierarchyFromRootToChild(targetOrder);
            // update from root order to child orders
            for (OrderDTO updatedOrder : ordersForUpdate) {
                if (updatedOrder.getId() == null) {
                    OrderBL bl = new OrderBL();
                    List<PricingField> pricingFields = updatedOrder.getPricingFields();
                    bl.processLines(updatedOrder, languageId, entityId, updatedOrder.getBaseUserByUserId().getId(),
                            updatedOrder.getCurrencyId(),
                            updatedOrder.getPricingFields() != null ? PricingField.setPricingFieldsValue(pricingFields.toArray(new PricingField[pricingFields.size()])) : null);
                    retValue = bl.createSingleOrder(entityId, executorId, updatedOrder, appliedChanges);
                    updatedOrder.setId(retValue);

                    EventManager.process(new NewOrderAndChangeEvent(entityId, order, orderChanges));

                } else {
                    recalculateAndUpdateOrder(updatedOrder, languageId, entityId, executorId, appliedChanges);
                }

                // Create Request to Update 911 Emergency Address
                Distributel911AddressUpdateEvent addressUpdateEvent = Distributel911AddressUpdateEvent.
                        createEventForAssetUpdateOnOrder(entityId, updatedOrder.getUserId(), updatedOrder);
                EventManager.process(addressUpdateEvent);
            }

            for (Integer orderForDeleteId : ordersForDelete) {
                OrderDTO orderForDelete = new OrderDAS().find(orderForDeleteId);
                if (orderForDelete.getDeleted() > 0) continue;
                // soft delete of order: delete only if hierarchy will not have errors
                String err = hierarchyValidator.deleteOrder(orderForDelete.getId());
                if (err == null) {
                    err = hierarchyValidator.validate();
                    if (err == null) {
                        OrderBL bl = new OrderBL();
                        bl.setForUpdate(orderForDelete.getId());
                        bl.delete(executorId);
                    } else {
                        // add order back to hierarchy in validator
                        hierarchyValidator.updateOrdersInfo(Arrays.asList(orderForDelete));
                    }
                }
            }
            OrderChangeBL orderChangeBL = new OrderChangeBL();
            if (appliedChanges != null) {
                orderChanges.addAll(orderChangesFromBundledItems(orderChanges, appliedChanges.values()));
            }

            //synchronize order changes with database state
            orderChangeBL.updateOrderChanges(entityId, orderChanges, deletedChanges, onDate);
            logger.debug(" ExistingPlanItemId: {} swapPlanItemId: {}", existingPlanItemId, swapPlanItemId);
            if(existingPlanItemId != null && swapPlanItemId != null && existingPlanItemId.intValue() != swapPlanItemId.intValue()) {
                new OrderBL().processSwapPlanFUPTrasfer(entityId, targetOrder, existingPlanItemId, swapPlanItemId, effectiveDate);
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return order.getId();
    }

    private List<OrderChangeDTO> orderChangesFromBundledItems(Collection<OrderChangeDTO> orderChanges, Collection<OrderChangeDTO> appliedChanges) {
        return appliedChanges.stream().filter(oc -> !orderChanges.contains(oc)).collect(Collectors.toList());
    }

    public Long getCountWithDecimals(Integer itemId)
            {
        try {
            return new OrderLineDAS().findLinesWithDecimals(itemId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Apply order changes group on given date. This method will select order changes from db and try to apply them to orders.
     * If success changes of orders will be saved. Otherwise orderChanges will be marked as APPLY_ERROR with appropriate error message
     * @param orderChangeIdsForHierarchy Order change ids for orders hierarchy
     * @param onDate application date
     * @param entityId target entity id
     * @throws SessionInternalError Exception is thrown if error was found during changes apply
     */
    public void applyChangesToOrders(Collection<Integer> orderChangeIdsForHierarchy, Date onDate, Integer entityId)
            {
        List<OrderChangeDTO> orderChanges = new LinkedList<>();
        Set<OrderDTO> ordersForUpdate = new HashSet<>();
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        // select order changes by ids from db, add to list only applicable on given date
        for (Integer changeId : orderChangeIdsForHierarchy) {
            OrderChangeDTO change = orderChangeDAS.find(changeId);
            if (change.getStatus().getId() == Constants.ORDER_CHANGE_STATUS_PENDING
                    && OrderChangeBL.isApplicable(change, onDate)) {
                orderChanges.add(change);
                OrderDTO order = change.getOrder();
                if(!CollectionUtils.isEmpty(order.getChildOrders())){
                    for(OrderDTO orderChild: order.getChildOrders()){
                        applyChangesToOrders(orderChangeDAS.findByOrderAndStatus(orderChild.getId(), Constants.ORDER_CHANGE_STATUS_PENDING), onDate, entityId);
                    }
                }
                ordersForUpdate.add(order);
            }
        }

        applyOrderChangesToOrders(orderChanges, ordersForUpdate, onDate, entityId, false);
    }

    public void applyOrderChangesToOrders(Collection<OrderChangeDTO> orderChanges, Collection<OrderDTO> ordersForUpdate, Date onDate, Integer entityId,
                                          boolean throwOnError) {

        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();

        if (orderChanges.isEmpty() || ordersForUpdate.isEmpty()) return;

        OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(ordersForUpdate.iterator().next());
        if (rootOrder == null) return;

        // make input entities transient
        new OrderDAS().detachOrdersHierarchy(rootOrder);
        for (OrderChangeDTO change : orderChanges) {
            change.touch();
            orderChangeDAS.detach(change);
            Set<AssetDTO> assets = new HashSet<>();
            for (AssetDTO persistedAsset : change.getAssets()) {
                assets.add(new AssetDTO(persistedAsset));
            }
            change.setAssets(assets);
        }

        OrderChangeBL orderChangeBL = new OrderChangeBL();

        Map<OrderLineDTO, OrderChangeDTO> appliedChanges =
		        OrderChangeBL.applyChangesToOrderHierarchy(rootOrder, orderChanges, onDate, throwOnError, entityId);
        // validate final hierarchy
        OrderHierarchyValidator hierarchyValidator = new OrderHierarchyValidator();
        hierarchyValidator.buildHierarchy(rootOrder);
        String error = hierarchyValidator.validate(entityId);
        if (error != null) {
            throw new SessionInternalError("Error in final orders hierarchy after changes apply: " + error, new String[]{error});
        } else {
            LinkedHashSet<OrderDTO> updatedOrders = OrderHelper.findOrdersInHierarchyFromRootToChild(rootOrder);
            updatedOrders.retainAll(ordersForUpdate);
            // find only really changed order, recalculate and update them
            for (OrderDTO order : updatedOrders) {
                boolean reallyUpdated = false;
                for (OrderChangeDTO change : orderChanges) {
                    if (change.getOrder().getId().equals(order.getId())
                            && change.getStatus().getId() != Constants.ORDER_CHANGE_STATUS_APPLY_ERROR
                            && change.getStatus().getId() != Constants.ORDER_CHANGE_STATUS_PENDING) {
                        reallyUpdated = true;
                        break;
                    }
                }
                if (reallyUpdated) {
                    recalculateAndUpdateOrder(
		                    order, com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID,
		                    entityId, null, appliedChanges);
                }
            }
            //synchronize order changes with database state
            orderChangeBL.updateOrderChanges(entityId, orderChanges, new HashSet<Integer>(), onDate);
        }

    }

    /**
     * Log the error during changes apply to orderChange objects
     * @param entityId Entity id
     * @param orderChangeIds Target orderChange Ids
     * @param onDate Changes Application date
     * @param errorCode Error Code
     * @param errorMessage Error Message
     */
    public void markOrderChangesAsApplyError(Integer entityId, Collection<Integer> orderChangeIds, Date onDate, String errorCode, String errorMessage) {
        List<OrderChangeDTO> orderChanges = new LinkedList<>();
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        for (Integer changeId : orderChangeIds) {
            orderChanges.add(orderChangeDAS.find(changeId));
        }
        new OrderChangeBL().updateOrderChangesAsApplyError(entityId, orderChanges, onDate, errorCode, errorMessage);
    }

    /**
     * Recalculate order and update persisted one
     * @param updatedOrder Input order dto
     * @param languageId Language Id
     * @param entityId Entity Id
     * @param executorId Executor Id
     */
    private void recalculateAndUpdateOrder(
		    OrderDTO updatedOrder, Integer languageId, Integer entityId,
		    Integer executorId, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        // start by locking the order
        OrderBL oldOrder = new OrderBL();
        oldOrder.setForUpdate(updatedOrder.getId());
        OrderBL orderBL = new OrderBL();
        // see if the related items should provide info
        List<PricingField> pricingFields = updatedOrder.getPricingFields();
        orderBL.processLines(updatedOrder, languageId, entityId, updatedOrder.getBaseUserByUserId().getId(),
                updatedOrder.getCurrency().getId(),
                updatedOrder.getPricingFields() != null ? PricingField.setPricingFieldsValue(pricingFields.toArray(new PricingField[pricingFields.size()])) : null);

        // recalculate
        orderBL.set(updatedOrder);
        orderBL.recalculate(entityId);

        // update
        oldOrder.update(executorId, updatedOrder, appliedChanges);
        if (updatedOrder.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
            for (OrderDTO childOrder: updatedOrder.getChildOrders()) {
                childOrder.setOrderStatus(updatedOrder.getOrderStatus());
                recalculateAndUpdateOrder(childOrder, languageId, entityId, executorId, appliedChanges);
            }
        }
    }
    
    public void save(OrderDTO order) {
        new OrderDAS().save(order);
    }
}
