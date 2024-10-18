package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetStatusBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.event.AssetStatusUpdateEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Arrays;

/**
 * Created by faizan on 5/16/17.
 */
public class AssetStatusUpdateTask  extends PluggableTask implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AssetStatusUpdateTask.class));

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            AssetStatusUpdateEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * @param event event to process
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException
     * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
     */
    @Override
    public void process(Event event) throws PluggableTaskException {

        LOG.debug("AssetStatusUpdateTask starts at: "+ Calendar.getInstance().getTime());
        AssetStatusUpdateEvent ev = (AssetStatusUpdateEvent) event;
        Integer executorId = ev.getExecutorId();
        OrderChangeWS[] newOrderChanges = ev.getNewOrderChanges();
        OrderChangeWS[] oldOrderChanges = ev.getOldOrderChanges();

        updateAssetStatus(executorId, newOrderChanges, oldOrderChanges);
        LOG.debug("AssetStatusUpdateTask ends at: "+ Calendar.getInstance().getTime());
    }

    private void updateAssetStatus(Integer executorId, OrderChangeWS[] orderChanges, OrderChangeWS[] oldOrderChanges){
        List<Integer> newAssetIds = new ArrayList<Integer>();

        //Setting the assigned status for assets
        if(null!=orderChanges) {
            for (OrderChangeWS newOrderChange : orderChanges) {
                OrderChangePlanItemWS[] orderChangePlanItems = newOrderChange.getOrderChangePlanItems();
                if (!ArrayUtils.isEmpty(orderChangePlanItems)) {
                    for(OrderChangePlanItemWS orderChangePlanItemWS : orderChangePlanItems){
                        if (orderChangePlanItemWS.getAssetIds().length > 0) {
                            Integer[] assetIds = ArrayUtils.toObject(orderChangePlanItemWS.getAssetIds());
                            newAssetIds.addAll(Arrays.asList(assetIds));
                            if (newOrderChange.getStartDate().after(new Date())) {
                                AssetStatusBL assetStatusBL = new AssetStatusBL();
                                AssetStatusDTO assetStatusDTO = assetStatusBL.findPendingStatusForItem(orderChangePlanItemWS.getItemId());
                                for (Integer assetId : assetIds) {
                                    updateStatus(assetId, assetStatusDTO, executorId);
                                }
                            }
                        }
                    }
                } else if(newOrderChange.getAssetIds().length > 0){
                    Integer[] assetIds = newOrderChange.getAssetIds();
                    newAssetIds.addAll(Arrays.asList(assetIds));
                    if (newOrderChange.getStartDate().after(new Date())) {
                        AssetStatusBL assetStatusBL = new AssetStatusBL();
                        AssetStatusDTO assetStatusDTO = assetStatusBL.findPendingStatusForItem(newOrderChange.getItemId());
                        for (Integer assetId : assetIds) {
                            updateStatus(assetId, assetStatusDTO, executorId);
                        }
                    }
                }
            }
        }

        //Setting the available status for assets
        for(OrderChangeWS oldOrderChange : oldOrderChanges){
            OrderChangePlanItemWS[] orderChangePlanItems = oldOrderChange.getOrderChangePlanItems();
            if (!ArrayUtils.isEmpty(orderChangePlanItems)) {
                for(OrderChangePlanItemWS orderChangePlanItemWS : orderChangePlanItems){
                    if (orderChangePlanItemWS.getAssetIds().length > 0) {
                        Integer[] assetIds = ArrayUtils.toObject(orderChangePlanItemWS.getAssetIds());
                        AssetStatusBL assetStatusBL = new AssetStatusBL();
                        AssetStatusDTO assetStatusDTO = assetStatusBL.findAvailableStatusForItem(orderChangePlanItemWS.getItemId());
                        for (Integer assetId : assetIds) {
                            boolean isAssetAttachedToOtherOrders = isAssetRelatedToOtherOrders(assetId, oldOrderChange.getId());
                            if(!newAssetIds.contains(assetId) && !isAssetAttachedToOtherOrders) {
                                updateStatus(assetId, assetStatusDTO, executorId);
                            }else if(isAssetAttachedToOtherOrders){
                                AssetStatusDTO assignedAssetStatusDTO = getRelatedStatusForAsset(assetId, orderChangePlanItemWS.getItemId(), oldOrderChange.getId());
                                updateStatus(assetId, assignedAssetStatusDTO, executorId);
                            }
                        }
                    }
                }
            } else if(oldOrderChange.getAssetIds().length>0){
                Integer[] oldAssets = oldOrderChange.getAssetIds();
                AssetStatusBL assetStatusBL = new AssetStatusBL();
                AssetStatusDTO availableAssetStatusDTO = assetStatusBL.findAvailableStatusForItem(oldOrderChange.getItemId());
                for(Integer assetId : oldAssets){
                    boolean isAssetAttachedToOtherOrders = isAssetRelatedToOtherOrders(assetId, oldOrderChange.getId());
                    if(!newAssetIds.contains(assetId) && !isAssetAttachedToOtherOrders){
                        updateStatus(assetId, availableAssetStatusDTO, executorId);
                    }else if(isAssetAttachedToOtherOrders){
                        AssetStatusDTO assignedAssetStatusDTO = getRelatedStatusForAsset(assetId, oldOrderChange.getItemId(), oldOrderChange.getId());
                        updateStatus(assetId, assignedAssetStatusDTO, executorId);
                    }
                }
            }
        }
    }

    private void updateStatus(Integer assetId, AssetStatusDTO assetStatusDTO, Integer executorId){
        AssetDTO assetDTO = new AssetBL(assetId).getEntity();
        if(assetDTO.getAssetStatus().getId()!=assetStatusDTO.getId()) {
            assetDTO.setAssetStatus(assetStatusDTO);
            AssetBL assetBL = new AssetBL();
            LOG.info("Updating asset = " + assetId + "status = " + assetStatusDTO.getId());
            assetBL.update(assetDTO, executorId);
        }
    }

    private boolean isAssetRelatedToOtherOrders(Integer assetId, Integer currentOrderChangeId){
        OrderChangeBL orderChangeBL = new OrderChangeBL();
        List<Integer> orderChangeIds = orderChangeBL.findOrderChangeIdsByAssetId(assetId);
        return !orderChangeIds.isEmpty() ? orderChangeBL.isAssetAttachedToOtherOrderChanges(currentOrderChangeId, orderChangeIds) : false;
    }

    private AssetStatusDTO getRelatedStatusForAsset(Integer assetId, Integer itemId, Integer currentOrderChangeId){
        OrderChangeBL orderChangeBL = new OrderChangeBL();
        AssetStatusDTO relatedStatus = new AssetBL().find(assetId).getAssetStatus();
        if(1 == relatedStatus.getIsAvailable() || 1 == relatedStatus.getIsDefault()){
            List<Integer> orderChangeIds = orderChangeBL.findOrderChangeIdsByAssetId(assetId);
            List<OrderChangeDTO> orderChangeDTOList = orderChangeBL.getOrderChangeDtos(currentOrderChangeId, orderChangeIds);
            for(OrderChangeDTO orderChangeDTO : orderChangeDTOList){
                if(ApplyToOrder.YES.equals(orderChangeDTO.getStatus().getApplyToOrder())){
                    return new AssetStatusBL().findActiveStatusForItem(itemId);
                }
            }
            relatedStatus = new AssetStatusBL().findPendingStatusForItem(itemId);
        }
        return relatedStatus;
    }
}
