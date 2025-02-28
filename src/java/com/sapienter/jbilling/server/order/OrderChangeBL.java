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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeBL;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDependencyDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.movius.OrderChangeUpdatePositionEvent;
import com.sapienter.jbilling.server.order.event.OrderChangeAppliedEvent;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangePlanItemDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.validator.OrderAssetsValidator;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.PricingStrategy;
import com.sapienter.jbilling.server.provisioning.event.OrderChangeStatusTransitionEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Constants;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import static com.sapienter.jbilling.common.CommonConstants.INTEGER_TRUE;

/**
 * @author Alexander Aksenov
 * @since 09.07.13
 */
public class OrderChangeBL {
    private final static int          ERROR_CODE_LINE_NOT_FOUND         = 100;
    private final static int          ERROR_CODE_NEGATIVE_QUANTITY      = 101;
    private final static int          ERROR_CODE_INVALID_HIERARCHY      = 102;
    private final static int          ERROR_CODE_INVALID_ASSETS         = 103;
    private final static int          ERROR_CODE_UNEXPECTED_APPLY_ERROR = 104;

    private OrderLineDAS              orderLineDAS                      = new OrderLineDAS();
    private OrderChangeDAS            orderChangeDAS                    = new OrderChangeDAS();
    private OrderChangeStatusDAS      orderChangeStatusDAS              = new OrderChangeStatusDAS();
    private OrderDAS                  orderDas                          = new OrderDAS();
    private AssetDAS                  assetDAS                          = new AssetDAS();

    private static final FormatLogger LOG                               = new FormatLogger(
                                                                                Logger.getLogger(OrderChangeBL.class));

    /**
     * Save updates in orderChanges. This method should be called after persist all orders and lines
     *
     * @param entityId
     *            Company for orders
     * @param orderChanges
     *            updated changes list
     * @param deletedChanges
     *            deleted changes ids
     * @param onDate
     *            current date (to validate order change
     */
    public void updateOrderChanges (Integer entityId, Collection<OrderChangeDTO> orderChanges,
            Collection<Integer> deletedChanges, Date onDate) {

        OrderChangeStatusDTO appliedStatus = orderChangeStatusDAS.findApplyStatus(entityId);
        OrderChangeStatusDTO pendingStatus = orderChangeStatusDAS.find(Constants.ORDER_CHANGE_STATUS_PENDING);
        for (Integer deletedId : deletedChanges) {
            OrderChangeDTO persistedOrderChange = orderChangeDAS.find(deletedId);
            if (persistedOrderChange != null && !persistedOrderChange.getStatus().equals(appliedStatus)) {
                orderChangeDAS.delete(persistedOrderChange);
            }
        }
        // sort collection to persist parent changes first
        List<OrderChangeDTO> sortedChanges = new LinkedList<>(orderChanges);
        Collections.sort(sortedChanges, (left, right) -> {
            if (left.getParentOrderChange() == null && right.getParentOrderChange() != null)
                return -1;
            if (left.getParentOrderChange() != null && right.getParentOrderChange() == null)
                return 1;
            return 0;
        });
        OrderLineDTO orderLine = null;

        for (OrderChangeDTO orderChange : sortedChanges) {
            if (orderChange.getOrder() == null || orderChange.getOrder().getId() == null) {
                continue;
            }
            if (orderChange.getId() != null && deletedChanges.contains(orderChange.getId())) {
                continue;
            }
            Integer languageId = new CompanyDAS().find(entityId).getLanguageId();
            OrderChangeDTO persistedOrderChange;
            if (orderChange.getId() != null && orderChange.getId().intValue() > 0) {
                persistedOrderChange = orderChangeDAS.find(orderChange.getId());
                OrderChangeStatusDTO oldStatus = persistedOrderChange.getUserAssignedStatus();
                if (orderChange.getAppliedManually() != null && orderChange.getAppliedManually().intValue() == 1) {
                    persistedOrderChange.setStatus(appliedStatus);
                    persistedOrderChange.setUserAssignedStatus(appliedStatus);
                } else {
                    persistedOrderChange.setStatus(orderChange.getStatus() == null ? pendingStatus
                            : orderChangeStatusDAS.find(orderChange.getStatus().getId()));
                    persistedOrderChange.setUserAssignedStatus(orderChangeStatusDAS.find(orderChange
                            .getUserAssignedStatus()
                            .getId()));
                }
                persistedOrderChange.setPrice(orderChange.getPrice());
                persistedOrderChange.setQuantity(orderChange.getQuantity());
                persistedOrderChange.setDescription(orderChange.getDescription());
                persistedOrderChange.setStartDate(Util.truncateDate(orderChange.getStartDate()));
                persistedOrderChange.setApplicationDate(orderChange.getApplicationDate());
                persistedOrderChange.setUseItem(orderChange.getUseItem());
                persistedOrderChange.setOptLock(orderChange.getOptLock());
                persistedOrderChange.setErrorCodes(orderChange.getErrorCodes());
                persistedOrderChange.setErrorMessage(orderChange.getErrorMessage());
	            persistedOrderChange.setOrderStatusToApply(orderChange.getOrderStatusToApply());
                orderLine = orderChange.getOrderLine();

                if (orderLine != null) {
                    if (orderLine.getId() != 0) {
                        persistedOrderChange.setOrderLine(orderLineDAS.find(orderLine.getId()));
                    } else {
                        orderLine = orderLineDAS.save(orderLine);
                        orderLineDAS.flush();
                        persistedOrderChange.setOrderLine(orderLine);
                    }
                }

                synchronizeAssets(persistedOrderChange.getAssets(), orderChange.getAssets());
                Set<AssetDTO> assets = new HashSet<AssetDTO>(orderChange.getAssets());
                orderChange.getAssets().clear();
                for (AssetDTO asset : assets) {
                    orderChange.getAssets().add(assetDAS.find(asset.getId()));
                }
                // update order change plan items
                if (persistedOrderChange.getOrderChangePlanItems() != null) {
                    Iterator<OrderChangePlanItemDTO> changePlanItemIterator = persistedOrderChange
                            .getOrderChangePlanItems()
                            .iterator();
                    while (changePlanItemIterator.hasNext()) {
                        OrderChangePlanItemDTO changePlanItem = changePlanItemIterator.next();
                        boolean found = false;
                        for (OrderChangePlanItemDTO dtoChangePlanItem : orderChange.getOrderChangePlanItems()) {
                            found = dtoChangePlanItem.getId() != null
                                    && dtoChangePlanItem.getId().equals(changePlanItem.getId());
                            if (found) {
                                break;
                            }
                        }
                        if (!found) {
                            changePlanItemIterator.remove();
                        }
                    }
                } else {
                    persistedOrderChange.setOrderChangePlanItems(new HashSet<OrderChangePlanItemDTO>());
                }
                for (OrderChangePlanItemDTO dtoChangePlanItem : orderChange.getOrderChangePlanItems()) {
                    OrderChangePlanItemDTO persistedChangePlanItem = null;
                    if (dtoChangePlanItem.getId() != null) {
                        for (OrderChangePlanItemDTO persistedDTO : persistedOrderChange.getOrderChangePlanItems()) {
                            if (dtoChangePlanItem.getId().equals(persistedDTO.getId())) {
                                persistedChangePlanItem = persistedDTO;
                                break;
                            }
                        }
                    }
                    if (persistedChangePlanItem == null) {
                        persistedChangePlanItem = dtoChangePlanItem;
                        persistedOrderChange.getOrderChangePlanItems().add(persistedChangePlanItem);
                        MetaFieldHelper.updateMetaFieldsWithValidation(
                                languageId,
                                persistedChangePlanItem
                                .getItem()
                                .getOrderLineMetaFields(), persistedChangePlanItem, dtoChangePlanItem);
                        persistedChangePlanItem.setOrderChange(persistedOrderChange);
                    } else {
                        persistedChangePlanItem.setOptLock(dtoChangePlanItem.getOptLock());
                        persistedChangePlanItem.setDescription(dtoChangePlanItem.getDescription());
                        persistedChangePlanItem.setAssets(dtoChangePlanItem.getAssets());
                        persistedChangePlanItem.getMetaFields().clear();
                        persistedChangePlanItem.getMetaFields().addAll(dtoChangePlanItem.getMetaFields());
                    }
                }

                MetaFieldHelper.updateMetaFieldsWithValidation(languageId, persistedOrderChange.getItem().getOrderLineMetaFields(),
                        persistedOrderChange, orderChange);

                persistedOrderChange.setAppliedManually(orderChange.getAppliedManually());
                persistedOrderChange.setRemoval(orderChange.getRemoval());
                persistedOrderChange.setNextBillableDate(null != orderChange.getNextBillableDate() ?
                        orderChange.getNextBillableDate() : orderChange.getOrder().getNextBillableDay());
                persistedOrderChange.setEndDate(orderChange.getEndDate());
                updateOrderChangeFromItem(persistedOrderChange, orderChange.getOrderLine());

                validateOrderChange(persistedOrderChange, onDate);
                persistedOrderChange = orderChangeDAS.save(persistedOrderChange);
                orderChangeDAS.flush();

                if (oldStatus != null && (oldStatus.getId() != persistedOrderChange.getUserAssignedStatus().getId())) {
                    OrderChangeStatusTransitionEvent event = new OrderChangeStatusTransitionEvent(entityId, persistedOrderChange,
                            oldStatus.getId(), persistedOrderChange.getUserAssignedStatus().getId());
                    EventManager.process(event);


                }
            } else {
                orderChange.setOrder(orderDas.find(orderChange.getOrder().getId()));
                if (orderChange.getOrderLine() != null) {
                    orderChange.setOrderLine(orderLineDAS.find(orderChange.getOrderLine().getId()));
                }
                if (orderChange.getParentOrderLine() != null) {
                    orderChange.setParentOrderLine(orderLineDAS.find(orderChange.getParentOrderLine().getId()));
                }
                if (!CollectionUtils.isEmpty(orderChange.getOrderChangePlanItems())
                        && isNonEditablePlanOrderChange(orderChange)) {
                    for (OrderChangePlanItemDTO changePlanItem : orderChange.getOrderChangePlanItems()) {
                        changePlanItem.setOrderChange(orderChange);
                        MetaFieldHelper.updateMetaFieldDefaultValuesWithValidation(languageId, changePlanItem
                                .getItem()
                                .getOrderLineMetaFields(), changePlanItem);
                    }
                } else {
                    orderChange.getOrderChangePlanItems().clear();
                }
                if (orderChange.getParentOrderChange() != null) {
                    orderChange.setParentOrderChange(orderChangeDAS.find(orderChange.getParentOrderChange().getId()));
                }
                if (orderChange.getAppliedManually() != null && orderChange.getAppliedManually().intValue() == 1) {
                    orderChange.setStatus(appliedStatus);
                    orderChange.setUserAssignedStatus(appliedStatus);
                } else {
                    orderChange.setStatus(orderChange.getStatus() == null ? pendingStatus : orderChangeStatusDAS
                            .find(orderChange.getStatus().getId()));
                    orderChange.setUserAssignedStatus(orderChangeStatusDAS.find(orderChange
                            .getUserAssignedStatus()
                            .getId()));
                }
                orderChange.setStartDate(Util.truncateDate(orderChange.getStartDate()));
                orderChange.setCreateDatetime(TimezoneHelper.serverCurrentDate());
                Set<AssetDTO> assets = new HashSet<AssetDTO>(orderChange.getAssets());
                orderChange.getAssets().clear();
                for (AssetDTO asset : assets) {
                    orderChange.getAssets().add(assetDAS.find(asset.getId()));
                }

                MetaFieldHelper.updateMetaFieldDefaultValuesWithValidation(languageId, orderChange
                        .getItem()
                        .getOrderLineMetaFields(), orderChange);

                if (null != orderChange.getOrderLine() && !orderChange.getOrderLine().getItem().isPlan()) {
                    boolean isTeaserPricing = OrderHelper.itemHasTeaserPricing(orderChange.getUser().getId(), 
                            orderChange.getOrderLine().getItem().getId(), 
                            new Date(), orderChange.getUser().getCompany().getId());
                    if (isTeaserPricing) {
                        orderChange.getOrderLine().setUseItem(Boolean.FALSE);
                        orderChange.setDescription(orderChange.getOrderLine().getDescription());
                    }
                }
                updateOrderChangeFromItem(orderChange, orderChange.getOrderLine());
                // When OrderChange NextBillableDate is null, set the Order NextBillableDay
                if(null == orderChange.getNextBillableDate())
                    orderChange.setNextBillableDate(orderChange.getOrder().getNextBillableDay());

                validateOrderChange(orderChange, onDate);
                orderChange = orderChangeDAS.save(orderChange);
                orderChangeDAS.flush();

                OrderChangeStatusTransitionEvent event = new OrderChangeStatusTransitionEvent(entityId, orderChange,
                        null, orderChange.getUserAssignedStatus().getId());
                EventManager.process(event);
            }
        }
    }

    private static void updateOrderChangeFromItem (OrderChangeDTO change, OrderLineDTO line) {
        if (line != null && line.getUseItem()) {
            change.setPrice(line.getPrice());
            change.setDescription(line.getDescription());
        }
    }

    /**
     * This method will apply of order changes if possible. Inapplicable changes (by date, for example) will be ignored
     * For changes application they will be splited to groups: by dependencies and orders. After changes apply
     * validation for Orders Hierarchy and orderLine assets will be performed
     *
     * @param rootOrder
     *            root order for hierarchy updated
     * @param orderChanges
     *            target changes
     * @param onDate
     *            date for changes apply
     * @param throwOnError
     *            flag is throw error if change apply is impossible, or only fill error field in orderChange object
     */
    public static Map<OrderLineDTO, OrderChangeDTO> applyChangesToOrderHierarchy (OrderDTO rootOrder, Collection<OrderChangeDTO> orderChanges,
            Date onDate, boolean throwOnError, Integer entityId) {
        Set<OrderChangeDTO> unAppliedChanges = new HashSet<OrderChangeDTO>();
        for (OrderChangeDTO orderChange : orderChanges) {
            if (isApplicable(orderChange, onDate)) {
                unAppliedChanges.add(orderChange);
            }
        }

        Map<Integer, OrderDTO> ordersMap = new HashMap<Integer, OrderDTO>();
        Map<Integer, OrderLineDTO> orderLinesMap = new HashMap<Integer, OrderLineDTO>();
        Map<Integer, AssetDTO> assetsMap = new HashMap<Integer, AssetDTO>();
        LinkedHashSet<OrderDTO> orders = OrderHelper.findOrdersInHierarchyFromRootToChild(rootOrder);
        for (OrderDTO order : orders) {
            if (order.getId() != null) {
                ordersMap.put(order.getId(), order);
                for (OrderLineDTO line : order.getLines()) {
                    if (line.getId() > 0) {
                        orderLinesMap.put(line.getId(), line);
                        for (AssetDTO asset : line.getAssets()) {
                            assetsMap.put(asset.getId(), asset);
                            if (asset.getPrevOrderLine() == null) {
                                asset.setPrevOrderLine(line);
                            }
                        }
                    }
                }
            }
        }
        // propagate actual orders to changes instead of ws dtos
        // this is needed for apply and revert opportunity
        // if order or line is new, order change already contain actual ref
        for (OrderChangeDTO orderChange : orderChanges) {
            if (orderChange.getOrder() != null && orderChange.getOrder().getId() != null) {
                orderChange.setOrder(ordersMap.get(orderChange.getOrder().getId()));
            }
            if (orderChange.getOrderLine() != null && orderChange.getOrderLine().getId() > 0) {
                orderChange.setOrderLine(orderLinesMap.get(orderChange.getOrderLine().getId()));
            }
            // propagate actual assets to changes, otherwise multiple dtos is possible for same asset
            if (orderChange.getAssets() != null) {
                Set<AssetDTO> oldAssets = new HashSet<AssetDTO>(orderChange.getAssets());
                orderChange.getAssets().clear();
                for (AssetDTO oldAsset : oldAssets) {
                    AssetDTO newAsset = assetsMap.get(oldAsset.getId());
                    if (newAsset == null) {
                        newAsset = oldAsset;
                        if (newAsset.getPrevOrderLine() == null) {
                            newAsset.setPrevOrderLine(newAsset.getOrderLine());
                        }
                    }
                    orderChange.getAssets().add(newAsset);
                }
            }
        }

		/* used for preserving successfully applied changes throughout the order hierarchy */
	    LinkedList<ChangeApplyInfo> successChanges = new LinkedList<ChangeApplyInfo>();

        // apply changes from parent order to child, group linked changes together for apply
        for (OrderDTO order : orders) {
            Collection<OrderChangeDTO> applicableChanges = findChangesForOrder(unAppliedChanges, order);
            unAppliedChanges.removeAll(applicableChanges);
            // find dependencies and group changes
            List<OrderChangeDTO> nextGroup = selectNextChangesGroup(applicableChanges, unAppliedChanges);
            sortOrderChangesBeforeApplication(nextGroup);
            while (!CollectionUtils.isEmpty(nextGroup)) {
                LinkedList<ChangeApplyInfo> applyInfos = new LinkedList<ChangeApplyInfo>();
                boolean success = true;
                String errorCode = null;
                String error = null;
                for (OrderChangeDTO orderChangeDTO : nextGroup) {
                    ChangeApplyInfo applyInfo = apply(orderChangeDTO, onDate);
                    if (applyInfo.success) {
                        applyInfos.add(applyInfo);
                    } else {
                        success = false;
                        errorCode = String.valueOf(applyInfo.errorCode);
                        error = findErrorMessageForCode(applyInfo.errorCode);
                        break;
                    }
                }
                if (success && !applyInfos.isEmpty()) {
                    OrderHierarchyValidator validator = new OrderHierarchyValidator();
                    validator.buildHierarchy(rootOrder);
                    error = validator.validate(entityId);
                    if (error != null) {
                        success = false;
                        errorCode = String.valueOf(ERROR_CODE_INVALID_HIERARCHY);
                    } else {
                        // perform additional validation for order line in each order change applyed
                        for (ChangeApplyInfo applyInfo : applyInfos) {
                            OrderLineDTO line = applyInfo.orderChange.getOrderLine();
                            if (line == null) {
                                line = applyInfo.orderChange.getLineCreated();
                            }
                            if (line != null) {
                                error = OrderAssetsValidator.validateAssetsForOrderChangesApply(line);
                                if (error != null) {
                                    errorCode = String.valueOf(ERROR_CODE_INVALID_ASSETS);
                                    success = false;
                                    break;
                                }
                                if (line.getQuantity() != null && line.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                                    errorCode = String.valueOf(ERROR_CODE_NEGATIVE_QUANTITY);
                                    error = findErrorMessageForCode(ERROR_CODE_NEGATIVE_QUANTITY);
                                    success = false;
                                    break;
                                }
                            }
                        }
                        if (error == null) {
                            // check that asset is not linked twice
                            Set<AssetDTO> newAssets = new HashSet<AssetDTO>();
                            for (OrderChangeDTO orderChangeDTO : nextGroup) {
                                OrderLineDTO line = orderChangeDTO.getOrderLine();
                                if (line == null) {
                                    line = orderChangeDTO.getLineCreated();
                                }
                                for (AssetDTO assetDTO : line.getAssets()) {
                                    if (newAssets.contains(assetDTO)) {
                                        error = OrderAssetsValidator.ERROR_ASSET_ALREADY_LINKED;
                                        errorCode = String.valueOf(ERROR_CODE_INVALID_ASSETS);
                                        success = false;
                                        break;
                                    } else {
                                        newAssets.add(assetDTO);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!success) {
                    if (!throwOnError) {
                        OrderChangeStatusDTO applyErrorStatus = new OrderChangeStatusDAS()
                                .find(Constants.ORDER_CHANGE_STATUS_APPLY_ERROR);
                        for (ChangeApplyInfo applyInfo : applyInfos) {
                            applyInfo.orderChange.setErrorCodes(errorCode);
                            applyInfo.orderChange.setErrorMessage(error);
                            applyInfo.orderChange.setStatus(applyErrorStatus);
                            applyInfo.orderChange.setApplicationDate(TimezoneHelper.companyCurrentDate(entityId));
                        }
                    }
                    // revert changes in reverse order
                    while (!applyInfos.isEmpty()) {
                        ChangeApplyInfo infoForRevert = applyInfos.removeLast();
                        revert(infoForRevert);
                    }
                    if (throwOnError) {
                        throw new SessionInternalError("Error during order change apply: " + error,
                                new String[] { error });
                    }
                } else {
                    for (ChangeApplyInfo applyInfo : applyInfos) {
                        applyInfo.orderChange.setStatus(applyInfo.orderChange.getUserAssignedStatus());
                        if (applyInfo.orderChange.getApplicationDate() == null) {
                            applyInfo.orderChange.setApplicationDate(TimezoneHelper.companyCurrentDate(entityId));
                        }
                        OrderChangeAppliedEvent event = new OrderChangeAppliedEvent(entityId, applyInfo.orderChange);
                        EventManager.process(event);
                    }
                }
	            nextGroup = selectNextChangesGroup(applicableChanges, unAppliedChanges);
	            sortOrderChangesBeforeApplication(nextGroup);

	            /* preserve successfully applied changes */
	            successChanges.addAll(applyInfos);
            }
        }

	    /* return a resulting map from the applied changes */
	    return collectAppliedChanges(successChanges);
    }

	/**
	 * Creates a map of applied changed from a list of already applied change information.
	 */
	private static Map<OrderLineDTO, OrderChangeDTO> collectAppliedChanges(List<ChangeApplyInfo> appliedChangeInfos) {
		Map<OrderLineDTO, OrderChangeDTO> appliedChanges = new HashMap<OrderLineDTO, OrderChangeDTO>();
		for (ChangeApplyInfo changeApply : appliedChangeInfos) {
			appliedChanges.put(changeApply.orderChange.getOrderLine(), changeApply.orderChange);
			for (OrderChangePlanItemDTO orderChangePlanItem : changeApply.orderChange.getOrderChangePlanItems()) {
				OrderChangeDTO change = orderChangePlanItem.getOrderChange();
				OrderLineDTO line = null != change ? change.getOrderLine() : null;
				if (null != change && null != line && !appliedChanges.containsKey(line)) {
					appliedChanges.put(line, change);
				}
			}
		}
		return appliedChanges;
	}

    /**
     * This method select dependent changes from current order changes and other orders in hierarchy changes. !!!
     * Important. Order changes in result collection will not longer presented in input collections
     *
     * @param applicableChanges
     *            changes for current order
     * @param unAppliedChanges
     *            unapplied changes for all orders in hierarchy
     * @return Next group of linked changes
     */
    private static List<OrderChangeDTO> selectNextChangesGroup (Collection<OrderChangeDTO> applicableChanges,
            Set<OrderChangeDTO> unAppliedChanges) {
	    List<OrderChangeDTO> result = new LinkedList<OrderChangeDTO>();
	    Iterator<OrderChangeDTO> nextChangeIterator = applicableChanges.iterator();
	    if (!nextChangeIterator.hasNext())
		    return result;
	    // select first from collection to result group
	    OrderChangeDTO nextChange = nextChangeIterator.next();
	    nextChangeIterator.remove();
	    result.add(nextChange);
	    // find dependent changes
	    while (nextChangeIterator.hasNext()) {
		    OrderChangeDTO orderChange = nextChangeIterator.next();
		    if (isChangesDependent(nextChange, orderChange)) {
			    result.add(orderChange);
			    nextChangeIterator.remove();
		    }
	    }
	    Iterator<OrderChangeDTO> otherOrdersChangeIterator = unAppliedChanges.iterator();
	    for (; otherOrdersChangeIterator.hasNext();) {
		    OrderChangeDTO orderChange = otherOrdersChangeIterator.next();
		    if (isChangesDependent(nextChange, orderChange)) {
			    result.add(orderChange);
			    otherOrdersChangeIterator.remove();
		    }
	    }

        applicableChanges.addAll(result);
        checkIfProductHasMoreDependency(applicableChanges, result);
        applicableChanges.removeAll(result);
        unAppliedChanges.addAll(result);
        checkIfProductHasMoreDependency(unAppliedChanges, result);
        unAppliedChanges.removeAll(result);
	    return result;
    }

    private static void checkIfProductHasMoreDependency(Collection<OrderChangeDTO> orderChangeDTOList, List<OrderChangeDTO> result) {
        for (OrderChangeDTO first : orderChangeDTOList) {
            for (OrderChangeDTO second : orderChangeDTOList) {
                if (first.getItem() != null && second.getItem() != null && !first.equals(second) &&
                        checkCategoryDependency(first.getItem(),first.getUser().getCompany().getId(), second.getItem())
                        //first.getItem().getDependItems().contains(second.getItem())
                                ) {
                    if (!result.contains(second)) {
                        result.add(second);
                    }
                }
            }
        }
    }

    /**
     * This method determines product dependency taking into account categories.
     * @param currentItem
     * @param companyId
     * @param item
     * @return true if the given ItemDTO is on the list of item dependencies.
     */
    private static boolean checkCategoryDependency(ItemDTO currentItem, int companyId, ItemDTO item) {
        Set<ItemDependencyDTO> itemsDependencyList = currentItem.getDependencies();
        ItemTypeBL itemType = new ItemTypeBL();
        ItemDTOEx[] categoryItemsList = null;
        for(ItemDependencyDTO itemDependency : itemsDependencyList) {
            itemType.set(itemDependency.getId());
            if(null!=itemType.getEntity()) {
                categoryItemsList = new ItemBL().getAllItemsByType(itemDependency.getDependentObjectId(), companyId);
                if(null!=categoryItemsList && categoryItemsList.length>0) {
                    for(ItemDTOEx itemDtoEx : categoryItemsList) {
                        if(itemDtoEx.getId()==item.getId()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return currentItem.getDependItems().contains(item);
    }

    /**
     * Define is given changes dependent, that is should be applied together
     * 
     * @param first
     *            First order change to check
     * @param second
     *            Second order change to check
     * @return True if order changes is dependent, false otherwise
     */
    private static boolean isChangesDependent (OrderChangeDTO first, OrderChangeDTO second) {
        if (first.getParentOrderChange() != null && first.getParentOrderChange().equals(second)
                || second.getParentOrderChange() != null && second.getParentOrderChange().equals(first)) {
            return true;
        }
        // process add line mandatory dependency before delete mandatory dependency for same line
        if (first.getParentOrderLine() != null && first.getParentOrderLine().equals(second.getParentOrderLine())
                && first.getItem().equals(second.getItem()) && second.getOrderLine() == null
                && first.getQuantity().compareTo(first.getOrderLine().getQuantity().negate()) == 0) {
            return true;
        }
        // one line can delete asset and another line will add same asset
        if (!CollectionUtils.isEmpty(first.getAssets()) && second.getOrderLine() != null) {
            OrderLineDTO persistedLine = new OrderLineDAS().find(second.getOrderLine().getId());
            if (!CollectionUtils.isEmpty(persistedLine.getAssets())) {
                for (AssetDTO asset : first.getAssets()) {
                    if (persistedLine.containsAsset(asset.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Filter input order changes by order, sort them to suit application order
     * 
     * @param orderChanges
     *            All changes for all orders in hierarchy
     * @param order
     *            target order to filter order changes
     * @return found order changes for target order
     */
    private static Collection<OrderChangeDTO> findChangesForOrder (Collection<OrderChangeDTO> orderChanges,
            OrderDTO order) {
        List<OrderChangeDTO> result = new LinkedList<OrderChangeDTO>();
        for (OrderChangeDTO changeDTO : orderChanges) {
            if (changeDTO.getOrder() != null && changeDTO.getOrder().equals(order)) {
                result.add(changeDTO);
            }
        }
        sortOrderChangesBeforeApplication(result);
        return result;
    }

    private static List<OrderChangeDTO> sortOrderChangesBeforeApplication (List<OrderChangeDTO> orderChanges) {
        if (orderChanges == null)
            return null;
        // sort order changes for safety applying
        Collections.sort(orderChanges, new Comparator<OrderChangeDTO>() {
            public int compare (OrderChangeDTO left, OrderChangeDTO right) {
                // process parent changes first
                if (left.getParentOrderChange() == null && right.getParentOrderChange() != null)
                    return -1;
                if (left.getParentOrderChange() != null && right.getParentOrderChange() == null)
                    return 1;
                if (left.getParentOrderChange() != null && right.getParentOrderChange() != null) {
                    if (left.getParentOrderChange().equals(right))
                        return 1;
                    if (right.getParentOrderChange().equals(left))
                        return -1;
                }
                // process changes for parent lines first
                if (left.getParentOrderLine() == null && right.getParentOrderLine() != null)
                    return -1;
                if (left.getParentOrderLine() != null && right.getParentOrderLine() == null)
                    return 1;
                // create lines before update lines
                if (left.getOrderLine() == null && right.getOrderLine() != null)
                    return -1;
                if (left.getOrderLine() != null && right.getOrderLine() == null)
                    return 1;
                // changes for same line applying in order of creation
                if (left.getOrderLine() != null && left.getOrderLine().equals(right.getOrderLine())) {
                    if (left.getCreateDatetime() == null && right.getCreateDatetime() == null)
                        return 0;
                    if (left.getCreateDatetime() == null && right.getCreateDatetime() != null)
                        return 1;
                    if (left.getCreateDatetime() != null && right.getCreateDatetime() == null)
                        return -1;
                    return left.getCreateDatetime().compareTo(right.getCreateDatetime());
                }
                return 0;
            }
        });
        return orderChanges;
    }

    /**
     * Apply given order change for target order for given date
     *
     * @param orderChange
     *            order change to apply
     * @param onDate
     *            date for which change should be applyed
     * @return false if unsuccessful, true if not applicable or change is applied successfully
     */
    public static ChangeApplyInfo apply (OrderChangeDTO orderChange, Date onDate) {
        if (!isApplicable(orderChange, onDate)) {
            return null;
        }
        ChangeApplyInfo result = new ChangeApplyInfo(orderChange);
        Integer errorCode = null;
        // apply order change to existed order line
        if (orderChange.getOrderLine() != null) {
            errorCode = applyToExistingOrderLine(orderChange, result, errorCode);
        } else {
            applyToNewOrderLine(orderChange);
        }
        // apply child order change for plan (order change for bundle item)
        if (!CollectionUtils.isEmpty(orderChange.getOrderChangePlanItems())
                && isNonEditablePlanOrderChange(orderChange)) {
            // only assets and meta fields change is allowed for non editable plans
            // non editable plan products changes should be persisted in parent plan line
            errorCode = applyPlanItems(orderChange, result, errorCode);
        }
        result.errorCode = errorCode;
        result.success = (errorCode == null);

        return result;
    }

    /**
     * @param orderChange
     * @param result
     * @param errorCode
     * @return
     */
    private static Integer applyToExistingOrderLine (OrderChangeDTO orderChange, ChangeApplyInfo result,
            Integer errorCode) {
        OrderLineDTO lineToChange = orderChange.getOrderLine();
        if (lineToChange.getDeleted() > 0) {
            errorCode = ERROR_CODE_LINE_NOT_FOUND;
        } else {
            result.storeForRollback(lineToChange);
            BigDecimal newQuantity = lineToChange.getQuantity().add(orderChange.getQuantity());
            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                errorCode = ERROR_CODE_NEGATIVE_QUANTITY;
            } else if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                lineToChange.setDeleted(1);
                // clear assets during line delete
                for (AssetDTO asset : lineToChange.getAssets()) {
                    unlinkAsset(asset, lineToChange);
                }
                lineToChange.setAssets(new HashSet<AssetDTO>());
            } else {
                lineToChange.setItem(orderChange.getItem()); // todo: is item can be changed?
                lineToChange.setQuantity(newQuantity);
                lineToChange.setPrice(orderChange.getPrice());
                lineToChange.setDescription(orderChange.getDescription());
                for (AssetDTO asset : lineToChange.getAssets()) {
                    if (!orderChange.getAssets().contains(asset)) {
                        unlinkAsset(asset, lineToChange);
                    }
                }
                synchronizeAssets(lineToChange.getAssets(), orderChange.getAssets());
                for (AssetDTO asset : lineToChange.getAssets()) {
                    linkAsset(asset, lineToChange);
                }
                if (orderChange.getParentOrderLine() != null) {
                    lineToChange.setParentLine(orderChange.getParentOrderLine());
                    orderChange.getParentOrderLine().getChildLines().add(lineToChange);
                } else {
                    if (lineToChange.getParentLine() != null) {
                        lineToChange.getParentLine().getChildLines().remove(lineToChange);
                        lineToChange.setParentLine(null);
                    }
                }
                // set the meta fields
                MetaFieldHelper.updateMetaFieldsWithValidation(lineToChange.getItem().getEntity().getLanguageId(),
                        lineToChange.getItem().getOrderLineMetaFields(), lineToChange, orderChange);
            }
        }
        return errorCode;
    }

    /**
     * @param orderChange
     */
    public static void applyToNewOrderLine (OrderChangeDTO orderChange) {
        OrderLineDTO newLine = new OrderLineDTO();

        newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        newLine.setPurchaseOrder(orderChange.getOrder());
        newLine.setDeleted(0);
        newLine.setItem(orderChange.getItem());
        newLine.setDefaults();
        newLine.setAmount(BigDecimal.ZERO);
        newLine.setOrderLinePlanItems(new OrderLinePlanItemDTOEx[] {});
	    newLine.setProvisioningStatusId(Constants.PROVISIONING_STATUS_ACTIVE);

        newLine.setPrice(orderChange.getPrice());
        newLine.setQuantity(orderChange.getQuantity());
        newLine.setAssets(new HashSet<AssetDTO>());
        synchronizeAssets(newLine.getAssets(), orderChange.getAssets());
        for (AssetDTO asset : newLine.getAssets()) {
            linkAsset(asset, newLine);
        }
        newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        newLine.setPrice(orderChange.getPrice() != null ? orderChange.getPrice() : BigDecimal.ZERO);
        newLine.setDescription(orderChange.getDescription());
        newLine.setUseItem(orderChange.getUseItem() != null && orderChange.getUseItem() > 0);
        newLine.setPercentage(orderChange.isPercentage());
        if (newLine.getUseItem()) {
            if (newLine.getItem().getOrderLineTypeId() != null) {
                newLine.setTypeId(newLine.getItem().getOrderLineTypeId());
            }
        }
        if (orderChange.getParentOrderLine() != null) {
            newLine.setParentLine(orderChange.getParentOrderLine());
        } else if (orderChange.getParentOrderChange() != null) {
            OrderLineDTO parentLine = null;
            if (orderChange.getParentOrderChange().getLineCreated() != null) {
                parentLine = orderChange.getParentOrderChange().getLineCreated();
            } else {
                parentLine = orderChange.getParentOrderChange().getOrderLine();
            }
            newLine.setParentLine(parentLine);
            if (parentLine != null) {
                parentLine.getChildLines().add(newLine);
            }
        }

        // set the meta fields
        MetaFieldHelper
                .updateMetaFieldsWithValidation(newLine.getItem().getEntity().getLanguageId(), newLine.getItem().getOrderLineMetaFields(),
                        newLine, orderChange);

        orderChange.getOrder().getLines().add(newLine);
        // set lineCreated to change for hierarchy build
        orderChange.setLineCreated(newLine);
        orderChange.setOrderLine(newLine);
    }

    /**
     * @param orderChange
     * @param result
     * @param errorCode
     * @return
     */
    private static Integer applyPlanItems (OrderChangeDTO orderChange, ChangeApplyInfo result, Integer errorCode) {
        OrderLineDTO planMainItemLine = orderChange.getOrderLine();
        boolean isNewLine = false;
        if (planMainItemLine == null) {
            planMainItemLine = orderChange.getLineCreated();
            isNewLine = true;
        }
        if (!isNewLine) { // store data for rollback only for existed line
            result.storeForRollback(planMainItemLine.getOrderLinePlanItems());
        }
        if (planMainItemLine == null) {
            errorCode = ERROR_CODE_LINE_NOT_FOUND;
        } else {
            List<OrderLinePlanItemDTOEx> orderLinePlanItems = new LinkedList<OrderLinePlanItemDTOEx>();
            for (OrderChangePlanItemDTO changePlanItemDTO : orderChange.getOrderChangePlanItems()) {
                OrderLinePlanItemDTOEx linePlanItemDTO = new OrderLinePlanItemDTOEx();
                linePlanItemDTO.setItemId(changePlanItemDTO.getItem().getId());
                linePlanItemDTO.setDescription(changePlanItemDTO.getDescription() != null ? changePlanItemDTO
                        .getDescription() : changePlanItemDTO.getItem().getDescription());
                Set<Integer> assets = new HashSet<Integer>();
                for (AssetDTO asset : changePlanItemDTO.getAssets()) {
                    assets.add(asset.getId());
                }
                linePlanItemDTO.setAssetIds(assets.toArray(new Integer[assets.size()]));

                List<MetaFieldValue> newMetaFields = new LinkedList<MetaFieldValue>();
                for (MetaFieldValue value : changePlanItemDTO.getMetaFields()) {
                    MetaFieldValue newValue = value.getField().createValue();
                    newValue.setField(value.getField());
                    newValue.setValue(value.getValue());
                    newMetaFields.add(newValue);
                }
                linePlanItemDTO.setMetaFields(newMetaFields);

                orderLinePlanItems.add(linePlanItemDTO);
            }
            planMainItemLine.setOrderLinePlanItems(orderLinePlanItems
                    .toArray(new OrderLinePlanItemDTOEx[orderLinePlanItems.size()]));
        }
        return errorCode;
    }

    /**
     * Revert OrderChange application using ChangeApplyInfo
     *
     * @param changeApplyInfo
     *            information to revert changes
     */
    private static void revert (ChangeApplyInfo changeApplyInfo) {
        OrderChangeDTO orderChange = changeApplyInfo.orderChange;
        // order change for existed line
        if (orderChange.getOrderLine() != null) {
            OrderLineDTO lineToRevert = orderChange.getOrderLine();
            lineToRevert.setDeleted(0);
            changeApplyInfo.rollback(lineToRevert);
            // revert plan items changes if needed
            if (!CollectionUtils.isEmpty(orderChange.getOrderChangePlanItems())) {
                changeApplyInfo.rollbackLinePlanItems(lineToRevert);
            }
            // order change for new line
        } else if (orderChange.getLineCreated() != null) {
            orderChange.getOrder().getLines().remove(orderChange.getLineCreated());
        }
        orderChange.setLineCreated(null);
        for (AssetDTO changeAsset : changeApplyInfo.orderChange.getAssets()) {
            if (!changeApplyInfo.oldAssets.contains(changeAsset)) {
                changeAsset.setOrderLine(changeApplyInfo.oldAssetsLines.get(changeAsset.getId()));
                changeAsset.setPrevOrderLine(changeApplyInfo.oldAssetsPrevLines.get(changeAsset.getId()));
                changeAsset.setUnlinkedFromLine(false);
            }
        }
    }

    /**
     * Check is this order change should be applied on date passed
     * 
     * @param change
     *            Order change for check
     * @param onDate
     *            Date for check
     * @return True if order change should be applied, false otherwise
     */
    public static boolean isApplicable (OrderChangeDTO change, Date onDate) {
        /*
         * Modified code to fix #7344 and allow products with future effective date to be added to the order provided
         * that order's active since date is equal to or greater than product's effective date.
         */
	    if (change.getStartDate() == null) {
		    return false;
	    }

	    Date effectiveDate = Util.truncateDate(change.getStartDate());
		Date activeSince = null != change.getOrder() &&
				null != change.getOrder().getActiveSince() ?
				Util.truncateDate(change.getOrder().getActiveSince()) : null;

        if (!effectiveDate.after(onDate)) {
            return change.getUserAssignedStatus() != null
                    && change.getUserAssignedStatus().getApplyToOrder().equals(ApplyToOrder.YES);

        } else {
            return  INTEGER_TRUE.equals(change.getAppliedManually())
		            && change.getUserAssignedStatus() != null
                    && change.getUserAssignedStatus().getApplyToOrder().equals(ApplyToOrder.YES)
                    && activeSince != null
                    && activeSince.compareTo(effectiveDate) <= 0;
        }
    }

    /**
     * Validate order change own fields. Throw exception if invalid
     * 
     * @param change
     *            Order Change for check
     * @param onDate
     *            Date for which order change should be valid
     */
    private static void validateOrderChange (OrderChangeDTO change, Date onDate) {

        if (change.getItem() == null) {
            String error = "OrderChangeWS,itemId,validation.error.is.required";
            throw new SessionInternalError("Item is required for order change", new String[] { error });
        }
        if (change.getUserAssignedStatus() == null) {
            String error = "OrderChangeWS,userAssignedStatus,validation.error.is.required";
            throw new SessionInternalError("User assigned status is required for order change", new String[] { error });
        }
        if (change.getOrderChangeType() == null) {
            String error = "OrderChangeWS,orderChangeType,validation.error.is.required";
            throw new SessionInternalError("Order change type is required for order change", new String[]{error});
        }
        if (!change.getOrderChangeType().isAllowOrderStatusChange()) {
            if (change.getOrderStatusToApply() != null) {
                String error = "OrderChangeWS,orderStatusToApply,validation.error.not.allowed";
                throw new SessionInternalError("Order status to apply is not allowed for selected order change type", new String[]{error});
            }
        }
        Collection<MetaField> metaFieldNames = new HashSet<MetaField>();
        metaFieldNames.addAll(change.getItem().getOrderLineMetaFields());
        metaFieldNames.addAll(change.getOrderChangeType().getOrderChangeTypeMetaFields());
        MetaFieldBL.validateMetaFields(change.getItem().getEntity().getLanguageId(), metaFieldNames, change);
    }

    /**
     * Update targetSet of assets from updated set
     * 
     * @param targetSet
     *            Target Assets collection
     * @param updatedSet
     *            Input Assets collection
     */
    private static void synchronizeAssets (Set<AssetDTO> targetSet, Set<AssetDTO> updatedSet) {
        Set<AssetDTO> updatedSetCopy = new HashSet<AssetDTO>(updatedSet);
        for (Iterator<AssetDTO> persistedIter = targetSet.iterator(); persistedIter.hasNext();) {
            AssetDTO persisted = persistedIter.next();
            boolean found = false;
            for (Iterator<AssetDTO> updatedIter = updatedSetCopy.iterator(); updatedIter.hasNext();) {
                AssetDTO updated = updatedIter.next();
                if (persisted.getId() == updated.getId()) {
                    found = true;
                    updatedIter.remove();
                }
            }
            if (!found) {
                persistedIter.remove();
            }
        }
        targetSet.addAll(updatedSetCopy);
    }

    /**
     * Unlink asset from order line for correct validation
     * 
     * @param asset
     *            Asset for unlink from line
     * @param fromLine
     *            Line from which asset should be unlinked
     */
    private static void unlinkAsset (AssetDTO asset, OrderLineDTO fromLine) {
        if (asset.getOrderLine() != null && asset.getOrderLine().getId() == fromLine.getId()) {
            asset.setOrderLine(null);
            asset.setPrevOrderLine(null);
            asset.setUnlinkedFromLine(true);
        } else if (asset.getPrevOrderLine() != null && asset.getPrevOrderLine().getId() == fromLine.getId()) {
            // unlink only prev line if asset already linked to new line
            asset.setPrevOrderLine(null);
            asset.setUnlinkedFromLine(true);
        }
    }

    private static void linkAsset (AssetDTO asset, OrderLineDTO toLine) {
        asset.setOrderLine(toLine);
    }

    public static OrderChangeDTO getDTO (OrderChangeWS ws) {
        Map<OrderWS, OrderDTO> wsToDtoOrdersMap = new HashMap<OrderWS, OrderDTO>();
        Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap = new HashMap<OrderLineWS, OrderLineDTO>();
        Map<OrderChangeWS, OrderChangeDTO> wsToDtoChangesMap = new HashMap<OrderChangeWS, OrderChangeDTO>();
        return getDTO(ws, wsToDtoChangesMap, wsToDtoOrdersMap, wsToDtoLinesMap);
    }

    /**
     * Convert ws orderChange object to appropriate dto object. Order changes hierarchy will be processed correctly.
     * Input 'wsToDto' maps are needed to preserve objects equality in hierarchy. This maps may be empty first and will
     * be filled during conversion
     * 
     * @param ws
     *            input ws object to convert
     * @param wsToDtoChangesMap
     *            map for already converted objects from ws to dto for OrderChanges
     * @param wsToDtoOrdersMap
     *            map for already converted objects from ws to dto for Orders
     * @param wsToDtoLinesMap
     *            map for already converted objects from ws to dto for OrderLines
     * @return DTO object for input ws object.
     */
    public static OrderChangeDTO getDTO (OrderChangeWS ws, Map<OrderChangeWS, OrderChangeDTO> wsToDtoChangesMap,
            Map<OrderWS, OrderDTO> wsToDtoOrdersMap, Map<OrderLineWS, OrderLineDTO> wsToDtoLinesMap) {
        OrderChangeDTO dto = new OrderChangeDTO();
        wsToDtoChangesMap.put(ws, dto);
        dto.setId(ws.getId());
        dto.setOptLock(ws.getOptLock() != null ? ws.getOptLock() : 0);
        if (ws.getOrderId() != null) {
            OrderDTO order = null;
            for (OrderDTO tmp : wsToDtoOrdersMap.values()) {
                if (ws.getOrderId().equals(tmp.getId())) {
                    order = tmp;
                    break;
                }
            }
            if (order == null) {
                order = new OrderDAS().find(ws.getOrderId());
            }
            dto.setOrder(order);
        } else if (ws.getOrderWS() != null) {
            OrderDTO order = wsToDtoOrdersMap.get(ws.getOrderWS());
            if (order == null) {
                order = new OrderBL().getDTO(ws.getOrderWS());
            }
            dto.setOrder(order);
        }
        dto.setItem(ws.getItemId() != null ? new ItemDAS().find(ws.getItemId()) : null);
        if (ws.getParentOrderChangeId() != null) {
            OrderChangeDTO parentChange = null;
            for (OrderChangeDTO tmp : wsToDtoChangesMap.values()) {
                if (ws.getParentOrderChangeId().equals(tmp.getId())) {
                    parentChange = tmp;
                    break;
                }
            }
            dto.setParentOrderChange(parentChange);
        } else if (ws.getParentOrderChange() != null) {
            OrderChangeDTO parentChange = wsToDtoChangesMap.get(ws.getParentOrderChange());
            if (parentChange == null) {
                parentChange = getDTO(ws.getParentOrderChange(), wsToDtoChangesMap, wsToDtoOrdersMap, wsToDtoLinesMap);
            }
            dto.setParentOrderChange(parentChange);
        }
        if (ws.getOrderLineId() != null && ws.getOrderLineId() > 0) {
            OrderLineDTO line = null;
            for (OrderLineDTO tmp : wsToDtoLinesMap.values()) {
                if (ws.getOrderLineId().equals(tmp.getId())) {
                    line = tmp;
                    break;
                }
            }
            if (line == null) {
                for (OrderDTO tmp : wsToDtoOrdersMap.values()) {
                    line = OrderHelper.findOrderLineWithId(tmp.getLines(), ws.getOrderLineId());
                    if (line != null) {
                        break;
                    }
                }
            }
            if (line == null) {
                line = new OrderLineDAS().find(ws.getOrderLineId());
            }
            
            dto.setOrderLine(line);
        }
        if (ws.getParentOrderLineId() != null && ws.getParentOrderLineId() > 0) {
            OrderLineDTO line = null;
            for (OrderLineDTO tmp : wsToDtoLinesMap.values()) {
                if (ws.getParentOrderLineId().equals(tmp.getId())) {
                    line = tmp;
                    break;
                }
            }
            if (line == null) {
                line = new OrderLineDAS().find(ws.getParentOrderLineId());
            }
            
            dto.setParentOrderLine(line);
        }

        dto.setQuantity(ws.getQuantityAsDecimal());
        dto.setPrice(ws.getPriceAsDecimal());
        dto.setDescription(ws.getDescription());
        dto.setUseItem(ws.getUseItem() != null ? ws.getUseItem() : 0);
        dto.setStartDate(Util.truncateDate(ws.getStartDate()));
        dto.setApplicationDate(ws.getApplicationDate());
        if (ws.getAssetIds() != null) {
            AssetDAS assetDas = new AssetDAS();
            Set<AssetDTO> assets = new HashSet<AssetDTO>();
            for (Integer id : ws.getAssetIds()) {
                assets.add(new AssetDTO(assetDas.find(id)));
            }
            dto.getAssets().addAll(assets);
        }
        if (ws.getOrderChangePlanItems() != null && ws.getOrderChangePlanItems().length > 0) {

            dto.getOrderChangePlanItems().clear();
            if (ws.getOrderChangePlanItems() != null) {
                for (OrderChangePlanItemWS changePlanItemWS : ws.getOrderChangePlanItems()) {
                	if(changePlanItemWS.getItemId()==0){
                		continue;
                	}
                    OrderChangePlanItemDTO changePlanItem = getChangePlanItemDTO(changePlanItemWS);
                    changePlanItem.setOrderChange(dto);
                    dto.getOrderChangePlanItems().add(changePlanItem);
                }
            }
        } else {
            dto.setOrderChangePlanItems(new HashSet<OrderChangePlanItemDTO>());
        }
        dto.setUserAssignedStatus(ws.getUserAssignedStatusId() != null ? new OrderChangeStatusDAS().find(ws
                .getUserAssignedStatusId()) : null);
        dto.setOrderChangeType(ws.getOrderChangeTypeId() != null ? new OrderChangeTypeDAS().find(ws.getOrderChangeTypeId()) : null);
        dto.setOrderStatusToApply(ws.getOrderStatusIdToApply() != null ? new OrderStatusDAS().find(ws.getOrderStatusIdToApply()) : null);
        Set<MetaField> allowedMetaFields = new HashSet<MetaField>();
        allowedMetaFields.addAll(dto.getItem().getOrderLineMetaFields());
        if (dto.getOrderChangeType() != null && dto.getOrderChangeType().getOrderChangeTypeMetaFields() != null && !dto.getOrderChangeType().getOrderChangeTypeMetaFields().isEmpty()) {
            allowedMetaFields.addAll(dto.getOrderChangeType().getOrderChangeTypeMetaFields());
        }
        MetaFieldHelper.fillMetaFieldsFromWS(allowedMetaFields, dto, ws.getMetaFields());

        dto.setAppliedManually(ws.getAppliedManually());
        dto.setRemoval(ws.getRemoval());
        dto.setNextBillableDate(ws.getNextBillableDate());
        dto.setEndDate(ws.getEndDate());
        dto.setPercentage(ws.isPercentage());
        return dto;
    }

    private static OrderChangePlanItemDTO getChangePlanItemDTO (OrderChangePlanItemWS ws) {
        OrderChangePlanItemDTO dto = new OrderChangePlanItemDTO();
        dto.setId(ws.getId() > 0 ? ws.getId() : null);
        if (ws.getAssetIds() != null) {
            AssetDAS assetDas = new AssetDAS();
            Set<AssetDTO> assets = new HashSet<AssetDTO>();
            for (Integer id : ws.getAssetIds()) {
                assets.add(new AssetDTO(assetDas.find(id)));
            }
            dto.getAssets().addAll(assets);
        }
        dto.setDescription(ws.getDescription());
        dto.setOptLock(ws.getOptlock() != null ? ws.getOptlock() : 0);
        dto.setItem(new ItemDAS().find(ws.getItemId()));
        MetaFieldHelper.fillMetaFieldsFromWS(new HashSet<MetaField>(dto.getItem().getOrderLineMetaFields()), dto,
                ws.getMetaFields());
        return dto;
    }

    /**
     * Convert input orderChange dto object to ws. OrderChanges hierarchy will be processed correctly
     * 
     * @param dto
     *            OrderChange dto object to convert
     * @param languageId
     *            Language Id of target consumer for this object
     * @param dtoToWsMap
     *            map with already converted orderChanges
     * @return converted WS object
     */
    public static OrderChangeWS getWS (OrderChangeDTO dto, Integer languageId,
            Map<OrderChangeDTO, OrderChangeWS> dtoToWsMap) {
        OrderChangeWS ws = new OrderChangeWS();
        dtoToWsMap.put(dto, ws);
        ws.setId(dto.getId());
        ws.setOptLock(dto.getOptLock());
        ws.setOrderId(dto.getOrder() != null ? dto.getOrder().getId() : null);
        ws.setItemId(dto.getItem() != null ? dto.getItem().getId() : null);
        ws.setQuantity(dto.getQuantity());
        ws.setPrice(dto.getPrice());
        ws.setDescription(dto.getDescription());
        ws.setUseItem(dto.getUseItem());
        ws.setStartDate(dto.getStartDate());
        ws.setApplicationDate(dto.getApplicationDate());
        if (dto.getAssets() != null) {
            Integer[] assetIds = new Integer[dto.getAssets().size()];
            int index = 0;
            for (AssetDTO assetDTO : dto.getAssets()) {
                assetIds[index] = assetDTO.getId();
                index++;
            }
            ws.setAssetIds(assetIds);
        }
        if (dto.getUserAssignedStatus() != null) {
            ws.setUserAssignedStatusId(dto.getUserAssignedStatus().getId());
            ws.setUserAssignedStatus(dto.getUserAssignedStatus().getDescription(languageId));
        }
        if (dto.getOrderChangeType() != null) {
            ws.setOrderChangeTypeId(dto.getOrderChangeType().getId());
            ws.setType(dto.getOrderChangeType().getName());
        }
        if (dto.getOrderStatusToApply() != null) {
            ws.setOrderStatusIdToApply(dto.getOrderStatusToApply().getId());
        }
        if (dto.getStatus() != null) {
            ws.setStatusId(dto.getStatus().getId());
            ws.setStatus(dto.getStatus().getDescription(languageId));
        }
        ws.setOrderLineId(dto.getOrderLine() != null ? dto.getOrderLine().getId() : null);
        ws.setErrorCodes(dto.getErrorCodes());
        ws.setErrorMessage(dto.getErrorMessage());
        ws.setDelete(0);
        // hierarchy
        dtoToWsMap.put(dto, ws);
        if (dto.getParentOrderLine() != null) {
            ws.setParentOrderLineId(dto.getParentOrderLine().getId());
        }
        if (dto.getParentOrderChange() != null) {
            OrderChangeWS parent = dtoToWsMap.get(dto.getParentOrderChange());
            if (parent == null) {
                parent = OrderChangeBL.getWS(dto.getParentOrderChange(), languageId, dtoToWsMap);
                dtoToWsMap.put(dto.getParentOrderChange(), parent);
            }
            ws.setParentOrderChange(parent);
        }
        List<OrderChangePlanItemWS> planItems = new LinkedList<OrderChangePlanItemWS>();
        if (!CollectionUtils.isEmpty(dto.getOrderChangePlanItems())) {
            for (OrderChangePlanItemDTO planItem : dto.getOrderChangePlanItems()) {
                planItems.add(getChangePlanItemWS(planItem));
            }
        }
        ws.setOrderChangePlanItems(planItems.toArray(new OrderChangePlanItemWS[planItems.size()]));
        ws.setMetaFields(MetaFieldHelper.toWSArray(dto.getMetaFields()));

        ws.setAppliedManually(dto.getAppliedManually());
        ws.setRemoval(dto.getRemoval());
        ws.setNextBillableDate(dto.getNextBillableDate());
        ws.setEndDate(dto.getEndDate());
        ws.setAllowedToUpdateOrderChange(PriceModelBL.isAllowedToUpdateOrderChange(dto.getItem(),
                                                                                   dto.getUser().getEntity().getId(),
                                                                                   dto.getOrderLine() != null ? dto.getOrderLine().getStartDate() :
                                                                                                                dto.getStartDate()));

        return ws;
    }
    
    /**
     * Helper method for create OrderChange for given order line
     * @param line Order Line for which change should be created. If orderId field is not empty in input Line, order for order change will be given from line
     * @param order Order for which order change should be created. This param is required if orderId is not provided in Line
     * @param statusId Status id for order changes created
     * @return OrderChange created
     */
    public static final OrderChangeWS buildFromLine(OrderLineWS line, OrderWS order, Integer statusId) {
        OrderChangeWS ws = new OrderChangeWS();
        ws.setOptLock(1);
        ws.setOrderChangeTypeId(Constants.ORDER_CHANGE_TYPE_DEFAULT);
        ws.setUserAssignedStatusId(statusId);
        ws.setStartDate(Util.truncateDate(TimezoneHelper.companyCurrentDateByUserId(order != null ? order.getUserId() : null)));
        if (line.getOrderId() != null && line.getOrderId() > 0) {
            ws.setOrderId(line.getOrderId());
        } else {
            ws.setOrderWS(order);
        }
        if (line.getId() > 0) {
            ws.setOrderLineId(line.getId());
        } else {
            // new line
            ws.setUseItem(line.getUseItem() ? 1 : 0);
        }
        if (line.getParentLine() != null && line.getParentLine().getId() > 0) {
            ws.setParentOrderLineId(line.getParentLine().getId());
        }
        ws.setDescription(line.getDescription());
        ws.setItemId(line.getItemId());
        ws.setAssetIds(line.getAssetIds());
        ws.setPrice(line.getPriceAsDecimal());
        if (line.getDeleted() == 0) {
            if (line.getId() > 0) {
                ws.setQuantity(BigDecimal.ZERO);
            } else {
                ws.setQuantity(line.getQuantityAsDecimal());
            }
        } else {
            ws.setQuantity(line.getQuantityAsDecimal().negate());
        }

        ws.setRemoval(line.getDeleted());
        if (order != null) {
            ws.setNextBillableDate(order.getNextBillableDay());
        }
        ws.setPercentage(line.isPercentage());
        ws.setMetaFields(MetaFieldHelper.copy(line.getMetaFields(), true));
        return ws;
    }
    public static final OrderChangeWS buildFromLine(OrderLineWS line, OrderWS order, Integer statusId, Integer isApplyNow) {
        OrderChangeWS ws = buildFromLine(line,order,statusId);
        if (null != isApplyNow){
            ws.setAppliedManually(isApplyNow);
        }
        return ws;
    }
    /**
     * Helper method for create OrderChanges for order lines in given order
     * @param order Input order with order lines for create OrderChanges
     * @param statusId Status id for order changes created
     * @return OrderChanges created
     */
    public static OrderChangeWS[] buildFromOrder(OrderWS order, Integer statusId) {
        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        Map<OrderLineWS, OrderChangeWS> lineToChangeMap = new HashMap<OrderLineWS, OrderChangeWS>();
        OrderWS rootOrder = OrderHelper.findRootOrderIfPossible(order);
        for (OrderLineWS line : rootOrder.getOrderLines()) {
            OrderChangeWS change = buildFromLine(line, rootOrder, statusId);
            orderChanges.add(change);
            lineToChangeMap.put(line, change);
        }
        for (OrderWS childOrder : OrderHelper.findAllChildren(rootOrder)) {
            for (OrderLineWS line : childOrder.getOrderLines()) {
                OrderChangeWS change = buildFromLine(line, childOrder, statusId);
                orderChanges.add(change);
                lineToChangeMap.put(line, change);
            }
        }
        for (OrderLineWS line : lineToChangeMap.keySet()) {
            if (line.getParentLine() != null) {
                OrderChangeWS change = lineToChangeMap.get(line);
                if (line.getParentLine().getId() > 0) {
                    change.setParentOrderLineId(line.getParentLine().getId());
                } else {
                    OrderChangeWS parentChange = lineToChangeMap.get(line.getParentLine());
                    change.setParentOrderChange(parentChange);
                }
            }
        }
        return orderChanges.toArray(new OrderChangeWS[orderChanges.size()]);
    }
    
    
    private static OrderChangePlanItemWS getChangePlanItemWS (OrderChangePlanItemDTO dto) {
        OrderChangePlanItemWS ws = new OrderChangePlanItemWS();
        ws.setId(dto.getId());

        if (!CollectionUtils.isEmpty(dto.getAssets())) {
            ws.setAssetIds(new int[dto.getAssets().size()]);
            int i = 0;
            for (AssetDTO asset : dto.getAssets()) {
                ws.getAssetIds()[i] = asset.getId();
                i++;
            }
        } else {
            ws.setAssetIds(new int[0]);
        }
        ws.setDescription(dto.getDescription());
        ws.setOptlock(dto.getOptLock());
        ws.setItemId(dto.getItem() != null ? dto.getItem().getId() : 0);
        ws.setMetaFields(MetaFieldHelper.toWSArray(dto.getMetaFields()));
        return ws;
    }

    private static String findErrorMessageForCode (int errorCode) {
        switch (errorCode) {
        case ERROR_CODE_LINE_NOT_FOUND:
            return "OrderChangeWS,somefield,error.orderchange.apply.line.not.found";
        case ERROR_CODE_NEGATIVE_QUANTITY:
            return "OrderChangeWS,quantity,error.orderchange.apply.negative.quantity";
        case ERROR_CODE_UNEXPECTED_APPLY_ERROR:
            return "OrderChangeWS,somefield,error.orderchange.apply.unexpected.error";
        }
        return null;
    }

    private static boolean isNonEditablePlanOrderChange (OrderChangeDTO orderChange) {
        return !(orderChange == null || orderChange.getItem() == null)
                && !CollectionUtils.isEmpty(orderChange.getItem().getPlans())
                && orderChange.getItem().getPlans().iterator().next().getEditable() == 0;
    }

    /**
     * Update order changes status to Error, set error code and error messages
     * @param entityId Entity Id
     * @param orderChanges Order Changes for update
     * @param onDate date of changes application
     * @param errorCode error code to set to order changes
     * @param errorMessage error message to set to changes
     */
    public void updateOrderChangesAsApplyError(Integer entityId, Collection<OrderChangeDTO> orderChanges, Date onDate,
                                               String errorCode, String errorMessage) {
        OrderChangeStatusDTO applyErrorStatus = new OrderChangeStatusDAS().find(Constants.ORDER_CHANGE_STATUS_APPLY_ERROR);
        if (errorCode == null) {
            errorCode = String.valueOf(ERROR_CODE_UNEXPECTED_APPLY_ERROR);
        }
        if (errorMessage == null) {
            errorMessage = findErrorMessageForCode(ERROR_CODE_UNEXPECTED_APPLY_ERROR);
        }
        for (OrderChangeDTO orderChange : orderChanges) {
            orderChange.setErrorCodes(errorCode);
            orderChange.setErrorMessage(errorMessage);
            orderChange.setStatus(applyErrorStatus);
            orderChange.setApplicationDate(TimezoneHelper.serverCurrentDate());
        }
        updateOrderChanges(entityId, orderChanges, new HashSet<Integer>(), onDate);
    }

    /**
     * Helper class to store results of OrderChange apply. Used for check application status and revert changes if
     * needed Main fields description: success - application result errorCode - error code if change apply was
     * unsuccessful orderChange - order change that was applied createdLine / createdLinePlanItem / updatedOrderLine -
     * target object of changes apply 'old' field - store previous state of target object of changes apply
     */
    private static class ChangeApplyInfo {
        public boolean                    success            = true;
        public Integer                    errorCode;

        public OrderChangeDTO             orderChange;
        // previous orderLine fields
        public BigDecimal                 oldQuantity;
        public Set<AssetDTO>              oldAssets          = new HashSet<AssetDTO>();
        public Map<Integer, OrderLineDTO> oldAssetsLines     = new HashMap<Integer, OrderLineDTO>();
        public Map<Integer, OrderLineDTO> oldAssetsPrevLines = new HashMap<Integer, OrderLineDTO>();
        public BigDecimal                 oldPrice;
        public String                     oldDescription;
        public OrderLineDTO               oldParentLine;
        public ItemDTO                    oldItem;
        // orderLinePlanItemDTOEx fields
        public OrderLinePlanItemDTOEx[]   oldOrderLinePlanItems;

        public ChangeApplyInfo (OrderChangeDTO orderChange) {
            this.orderChange = orderChange;
        }

        public void storeForRollback (OrderLineDTO line) {
            this.oldQuantity = line.getQuantity();
            this.oldAssets = new HashSet<AssetDTO>(line.getAssets());
            for (AssetDTO asset : oldAssets) {
                oldAssetsLines.put(asset.getId(), asset.getOrderLine());
                oldAssetsPrevLines.put(asset.getId(), asset.getPrevOrderLine());
            }
            for (AssetDTO changeAsset : orderChange.getAssets()) {
                if (!oldAssets.contains(changeAsset)) {
                    oldAssetsLines.put(changeAsset.getId(), changeAsset.getOrderLine());
                    oldAssetsPrevLines.put(changeAsset.getId(), changeAsset.getPrevOrderLine());
                }
            }
            this.oldPrice = line.getPrice();
            this.oldDescription = line.getDescription();
            this.oldParentLine = line.getParentLine();
            this.oldItem = line.getItem();
        }

        public void storeForRollback (OrderLinePlanItemDTOEx[] orderLinePlanItems) {
            this.oldOrderLinePlanItems = orderLinePlanItems;
        }

        public void rollbackLinePlanItems (OrderLineDTO line) {
            line.setOrderLinePlanItems(this.oldOrderLinePlanItems);
        }

        public void rollback (OrderLineDTO lineToRevert) {
            lineToRevert.setItem(this.oldItem);
            lineToRevert.setQuantity(this.oldQuantity);
            lineToRevert.setPrice(this.oldPrice);
            lineToRevert.setDescription(this.oldDescription);
            synchronizeAssets(lineToRevert.getAssets(), this.oldAssets);
            for (AssetDTO asset : lineToRevert.getAssets()) {
                asset.setOrderLine(this.oldAssetsLines.get(asset.getId()));
                asset.setPrevOrderLine(this.oldAssetsPrevLines.get(asset.getId()));
                asset.setUnlinkedFromLine(false);
            }
            lineToRevert.setParentLine(this.oldParentLine);
        }
    }

    public List<Integer> findOrderChangeIdsByAssetId(Integer assetId) {
        return orderChangeDAS.findOrderChangeIdsByAssetId(assetId);
    }

    public boolean isAssetAttachedToOtherOrderChanges(Integer currentOrderChangeId, List<Integer> relatedOrderChangeIds) {
        return orderChangeDAS.isAssetAttachedToOtherOrderChanges(currentOrderChangeId, relatedOrderChangeIds);
    }

    public List<OrderChangeDTO> getOrderChangesWithEffectiveDate(Integer userId, Date effectiveDate){
        return orderChangeDAS.getOrderChangesWithEffectiveDate(userId, effectiveDate);
    }   
    
    public List<OrderChangeDTO> getOrderChangeDtos(Integer currentOrderChangeId, List<Integer> relatedOrderChangeIds) {
        return orderChangeDAS.getOrderChangeDtos(currentOrderChangeId, relatedOrderChangeIds);
    }

    public void updateOrderChangeEndDate(Integer orderChangeId, Date endDate){
        OrderChangeDTO persistedOrderChange = orderChangeDAS.find(orderChangeId);
        if(null != persistedOrderChange){
            persistedOrderChange.setEndDate(endDate);
            orderChangeDAS.save(persistedOrderChange);
            orderChangeDAS.flush();
        }
    }

	public OrderChangeWS[] createOrderChange(OrderWS orderWS,	Integer itemId, UserWS userWS, BigDecimal newPrice,
			BigDecimal newQuantity, Date changeEffectiveDate, OrderChangeWS[] oldOrderChanges) {

		Optional<OrderLineWS> tempOrderLine = getLineByItemId(orderWS.getOrderLines(), itemId);
		OrderChangeWS[] orderChangeWSArray = new OrderChangeWS[1];
		if(tempOrderLine.isPresent()) {
			updateEndDateOnOrderChange(oldOrderChanges, itemId, changeEffectiveDate);
			orderChangeWSArray[0] = OrderChangeBL.buildFromLine(tempOrderLine.get(), orderWS,
					getOrderChangeApplyStatus(userWS.getEntityId()).get());
		    orderChangeWSArray[0].setQuantityAsDecimal(newQuantity);
		    orderChangeWSArray[0].setPriceAsDecimal(newPrice);
		    orderChangeWSArray[0].setStartDate(changeEffectiveDate);
		}

		updateOrderChangeCountPosition(orderWS.getId(), itemId, userWS.getEntityId(), newQuantity);
		return orderChangeWSArray;
    }

	public OrderChangeWS[] updateCurrentOrderChange(OrderWS orderWS,	Integer itemId, UserWS userWS, BigDecimal newPrice,
			BigDecimal newQuantity, Date changeEffectiveDate, OrderChangeWS oldOrderChanges) {

		oldOrderChanges.setQuantityAsDecimal(newQuantity);
		oldOrderChanges.setPriceAsDecimal(newPrice);

		updateOrderChangeCountPosition(orderWS.getId(), itemId, userWS.getEntityId(), newQuantity);
		return new OrderChangeWS[]{oldOrderChanges};
    }

	public void updateOrderChangeCountPosition(Integer orderId, Integer itemId,
			Integer entityId, BigDecimal newQuantity) {
		OrderDTO orderDto = new OrderDAS().find(orderId);
		OrderChangeUpdatePositionEvent newEvent = new OrderChangeUpdatePositionEvent(
				newQuantity, orderDto, orderDto.getLine(itemId), entityId);
		EventManager.process(newEvent);
	}

    /**
     * The changeEffectiveDate can be today or in the past but it
     * cannot be before the order’s next billable date. The changeEffectiveDate cannot be in future.
     */
	public boolean validateChangeEffectiveDate(Date changeEffectiveDate, OrderWS order, UserWS userWS) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		if (changeEffectiveDate.after(new Date()) || changeEffectiveDate.after(userWS.getNextInvoiceDate())) {
			throw new SessionInternalError("The changeEffectiveDate cannot be in future.");
		}
		if (changeEffectiveDate.after(userWS.getNextInvoiceDate())) {
			throw new SessionInternalError("The changeEffectiveDate cannot be greater than Next Invoice Date of Customer.");
		}
		Date toCompare = null != order.getNextBillableDay() ? order.getNextBillableDay() : order.getActiveSince();
		if (!(changeEffectiveDate.before(new Date()) && changeEffectiveDate.after(toCompare))) {
			throw new SessionInternalError("The changeEffectiveDate can be today or in the past but it cannot"
							+ " be before the order’s next billable date.");
		}
		if (formatter.format(new Date()).equals(formatter.format(changeEffectiveDate))
				|| formatter.format(changeEffectiveDate).equals(formatter.format(toCompare))) {
			return true;
		}
        return true;
	}

	public boolean isPriceOrQuantityUpdated(OrderChangeWS[] oldOrderChanges, Integer itemId,
			BigDecimal receivedPrice, BigDecimal receivedQuantity) {
		boolean isUpdated = false;
		BigDecimal originalQuantity = BigDecimal.ZERO;
		BigDecimal originalPrice = BigDecimal.ZERO;

		for (OrderChangeWS change : oldOrderChanges) {
			if(change.getItemId().equals(itemId) && null == change.getEndDate()) {
				originalQuantity = originalQuantity.add(change.getQuantityAsDecimal());
				originalPrice = originalPrice.add(change.getPriceAsDecimal());
			}
		}

		if((originalPrice.compareTo(receivedPrice) != 0)
				|| (originalQuantity.compareTo(receivedQuantity) != 0))
			isUpdated = true;

		return isUpdated;
	}

	public BigDecimal getCurrentPrice(OrderChangeWS[] oldOrderChanges, Integer itemId) {
		BigDecimal originalPrice = BigDecimal.ZERO;
		for (OrderChangeWS change : oldOrderChanges) {
			if(change.getItemId().equals(itemId) && null == change.getEndDate()) {
				originalPrice = originalPrice.add(change.getPriceAsDecimal());
				break;
			}
		}
		return originalPrice;
	}

	public BigDecimal getCurrentQuantity(OrderChangeWS[] oldOrderChanges, Integer itemId) {
		BigDecimal originalQuantity = BigDecimal.ZERO;
		for (OrderChangeWS change : oldOrderChanges) {
			if(change.getItemId().equals(itemId) && null == change.getEndDate()) {
				originalQuantity = originalQuantity.add(change.getQuantityAsDecimal());
				break;
			}
		}
		return originalQuantity;
	}

	private void updateEndDateOnOrderChange(OrderChangeWS[] orderChangeWSArray, Integer itemId, Date changeEffectiveDate){
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
		for (OrderChangeWS orderChangeWS : orderChangeWSArray) {
			if (null != orderChangeWS.getId() && orderChangeWS.getItemId().equals(itemId)
					&& null == orderChangeWS.getEndDate()) {
				OrderChangeDTO persistedOrderChange = new OrderChangeDAS().find(orderChangeWS.getId());
				if (null != persistedOrderChange) {
					if (changeEffectiveDate.before(persistedOrderChange.getStartDate())) {
						throw new SessionInternalError("Provided changeEffectiveDate already covered in previous order change periods.");
					}
					persistedOrderChange.setEndDate(changeEffectiveDate);
					orderChangeDAS.save(persistedOrderChange);
				}
			}
		}
    }

	public Optional<OrderLineWS> getLineByItemId(OrderLineWS[] lines, Integer itemId){
        for(OrderLineWS orderLineWS : lines){
            if(orderLineWS.getItemId().equals(itemId)) {
                return Optional.of(orderLineWS);
            }
        }
        return Optional.empty();
    }

	private Optional<Integer> getOrderChangeApplyStatus(Integer entityId) {
		List<OrderChangeStatusDTO>statuses = new OrderChangeStatusDAS().findOrderChangeStatuses((entityId));
		Integer statusId = null;
		for(OrderChangeStatusDTO orderChangeStatus : statuses){
			if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
				statusId = orderChangeStatus.getId();
				break;
			}
		}
		return Optional.of(statusId);
	}

	public OrderChangeWS getCurrentOrderChangeByEffectiveDate(Date changeEffectiveDate,
			Integer itemId, OrderChangeWS[] oldOrderChanges) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		OrderChangeWS currentOrderChange = null;
		for (OrderChangeWS orderChangeWS : oldOrderChanges) {
			Date toCompare = null != orderChangeWS.getNextBillableDate() ? orderChangeWS.getNextBillableDate()
					: orderChangeWS.getStartDate();
			if(formatter.format(changeEffectiveDate).equals(formatter.format(toCompare))
					&&  orderChangeWS.getItemId().equals(itemId)) {
				currentOrderChange = orderChangeWS;
			}
		}
		return currentOrderChange;
	}

}
