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
package com.sapienter.jbilling.server.order.task;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetAssignmentDAS;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.event.AssetAddedToOrderEvent;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.event.NewActiveSinceEvent;
import com.sapienter.jbilling.server.order.event.NewActiveUntilEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Util;

/**
 * This Class will evaluate the Asset Assignment End Date and Start Date in case of
 * New Active Until Date, New Active Since Date and Asset Added to Order.
 *
 * @author ashwinkumar.patra
 * @since 01-March-2021
 */
public class AssetAssignmentEvaluationStrategy extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        NewActiveUntilEvent.class, AssetAddedToOrderEvent.class, NewActiveSinceEvent.class, AssetUpdatedEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        OrderDAS orderDAS = new OrderDAS();
        AssetAssignmentDAS assignmentDAS = new AssetAssignmentDAS();
        // validate the type of the event
        if (event instanceof NewActiveUntilEvent) {
            NewActiveUntilEvent activeUntilEvent = (NewActiveUntilEvent) event;
            OrderDTO order = orderDAS.find(activeUntilEvent.getOrderId());
            List<AssetAssignmentDTO> assetAssignments = assignmentDAS.getAssignmentsForOrder(order.getId());
            if(CollectionUtils.isNotEmpty(assetAssignments)) {
                AssetAssignmentDTO assetAssignment = assetAssignments.get(0);
                if (null != assetAssignment) {
                    Date newActiveUntil = activeUntilEvent.getNewActiveUntil();
                    Date activeUntilEndOfDay = null != newActiveUntil ? Util.getEndOfDay(newActiveUntil) : null;
                    assetAssignment.setEndDatetime(activeUntilEndOfDay);
                    logger.debug("setting asset assignment id {} for order id {} with end date to : {}", assetAssignment.getId(), order.getId(), activeUntilEndOfDay);
                }

            }
        } else if (event instanceof AssetAddedToOrderEvent) { // AssetAddedToOrderEvent
            AssetAddedToOrderEvent assetAddedToOrderEvent = (AssetAddedToOrderEvent) event;
            AssetDTO asset = assetAddedToOrderEvent.getAsset();
            OrderDTO currentOrder = asset.getOrderLine().getPurchaseOrder();
            validateAndUpdateAssetAssignments(orderDAS, assignmentDAS, asset, currentOrder, false);
        } else if (event instanceof NewActiveSinceEvent) {
            NewActiveSinceEvent updateActiveSinceEvent = (NewActiveSinceEvent) event;
            OrderDTO currentOrder = updateActiveSinceEvent.getNewOrder();
            List<String> assetIdentifiers = currentOrder.getAssetIdentifiers();
            if (CollectionUtils.isNotEmpty(assetIdentifiers)) {
                for (String assetIdentifier : assetIdentifiers) {
                    AssetDAS assetDAS = new AssetDAS();
                    AssetDTO asset = assetDAS.getAssetByIdentifier(assetIdentifier);
                    validateAndUpdateAssetAssignments(orderDAS, assignmentDAS, asset, currentOrder, true);
                }
            }
        } else if(event instanceof AssetUpdatedEvent) {
            AssetUpdatedEvent assetUpdatedEvent = (AssetUpdatedEvent) event;
            AssetDTO assetDTO = new AssetDAS().findForUpdate(assetUpdatedEvent.getAsset().getId());
            new AssetBL().deleteAssignmentsWithEndDateBeforeStartDate(assetDTO);
        } else {
            throw new SessionInternalError("Can't process anything except New Active Until Event or New Active Since Event or Asset Added To Order Event");
        }
    }

    /**
     * This method will validate if the previous asset assign
     * @param orderDAS
     * @param assignmentDAS
     * @param asset
     * @param currentOrder
     * @param isAssignmentStartDateUpdateRequired
     */
    private void validateAndUpdateAssetAssignments(OrderDAS orderDAS, AssetAssignmentDAS assignmentDAS, AssetDTO asset,
                                                   OrderDTO currentOrder, boolean isAssignmentStartDateUpdateRequired) {
        List<AssetAssignmentDTO> assignments = assignmentDAS.getAssignmentsForAsset(asset.getId());
        Date currentOrderActiveSince = currentOrder.getActiveSince();
        if (CollectionUtils.isNotEmpty(assignments)) {
            if (assignments.size() >= 2) { // check if this asset was assigned to any other order previous to the current order.
                AssetAssignmentDTO previousAssetAssignment = assignments.get(1);
                if (previousAssetAssignment.getOrderLine().getPurchaseOrder().getUserId().equals(currentOrder.getUserId())) {
                    updateAssetAssignmentDatesForPlanChange(orderDAS, assignmentDAS, asset, currentOrder, currentOrderActiveSince, isAssignmentStartDateUpdateRequired);
                }
            } else {
                logger.debug("Previous asset assignment not found for asset {}", asset.getIdentifier());
            }
        }
    }

    /**
     * This method will validate if Plan change is been happened
     * if Plan change is true, and plan change is happened today, then it will update the current asset assignment start date to current server time and previous asset assignment end date to current server time - 1 seconds.
     * if plan change is false, and if this method is triggered from new active since date then it will update the asset assignment start date only
     *
     * @param orderDAS
     * @param assignmentDAS
     * @param asset
     * @param currentOrder
     * @param currentOrderActiveSince
     * @param isAssignmentStartDateUpdateRequired
     */
    private void updateAssetAssignmentDatesForPlanChange(OrderDAS orderDAS, AssetAssignmentDAS assignmentDAS, AssetDTO asset,
                                                         OrderDTO currentOrder, Date currentOrderActiveSince, boolean isAssignmentStartDateUpdateRequired) {
        Date effectiveDate = DateUtils.addDays(currentOrderActiveSince, -1);
        Integer userId = currentOrder.getUserId();
        Integer currentOrderId = currentOrder.getId();
        List<String> assetIdentifiers = currentOrder.getAssetIdentifiers();
        if (CollectionUtils.isNotEmpty(assetIdentifiers)) {
            for (String assetIdentifier : assetIdentifiers) {
                if(asset.getIdentifier().equalsIgnoreCase(assetIdentifier)) {
                    OrderDTO previousOrder = orderDAS.findOrderByUserAssetIdentifierEffectiveDate(userId, assetIdentifier, effectiveDate);
                    if (null == previousOrder) {
                        logger.debug("Previous Subscription Order not found for user {} for asset identifier {} for date {}",
                            userId, assetIdentifier, effectiveDate);
                        continue;
                    }
                    Date previousOrderActiveUntil = previousOrder.getActiveUntil();
                    Integer previousOrderId = previousOrder.getId();
                    boolean isPlanChange = DateUtils.isSameDay(previousOrderActiveUntil, effectiveDate);
                    logger.debug("Previous Order {} active until {} and new order {} active since {} and is plan change happened : {}",
                        previousOrderId, previousOrderActiveUntil, currentOrderId, currentOrderActiveSince, isPlanChange);
                    if (isPlanChange) {
                        if(DateUtils.isSameDay(currentOrderActiveSince, new Date())) {
                            Date serverCurrentDate = TimezoneHelper.serverCurrentDate();
                            List<AssetAssignmentDTO> previousAssignments = assignmentDAS.getAssignmentsForOrder(previousOrderId);
                            AssetAssignmentDTO currentAssignment = setAssignmentStartDate(assignmentDAS, currentOrderId, serverCurrentDate);
                            if (CollectionUtils.isNotEmpty(previousAssignments) && null != currentAssignment) {
                                AssetAssignmentDTO previousAssignment = previousAssignments.get(0);
                                Date endDatetime = DateUtils.addSeconds(serverCurrentDate, -1);
                                previousAssignment.setEndDatetime(endDatetime);
                                logger.debug("setting asset assignment id {} for order id {} with end date to : {}",
                                    previousAssignment.getId(), previousOrderId, endDatetime);
                            }
                        }
                    } else if (isAssignmentStartDateUpdateRequired) {
                        Date startDate = Util.getStartOfDay(currentOrderActiveSince);
                        setAssignmentStartDate(assignmentDAS, currentOrderId, startDate);
                    }

                }
            }
        }
    }

    /**
     * this method is to set the Asset Assignment Start Date
     * @param assignmentDAS
     * @param currentOrderId
     * @param startDate
     * @return
     */
    private AssetAssignmentDTO setAssignmentStartDate(AssetAssignmentDAS assignmentDAS, Integer currentOrderId, Date startDate) {
        List<AssetAssignmentDTO> currentAssignments = assignmentDAS.getAssignmentsForOrder(currentOrderId);
        AssetAssignmentDTO currentAssignment = null;
        if (CollectionUtils.isNotEmpty(currentAssignments)) {
            currentAssignment = currentAssignments.get(0);
            currentAssignment.setStartDatetime(startDate);
            logger.debug("setting asset assignment id {} for order id {} with start date to : {}",
                currentAssignment.getId(), currentOrderId, startDate);
        }
        return currentAssignment;
    }
}
