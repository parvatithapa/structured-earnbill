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
package com.sapienter.jbilling.server.order.validator;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangePlanItemDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Aksenov
 * @since 14.07.13
 */
public class OrderAssetsValidator {

    public final static String ERROR_ASSET_MANAGEMENT_DISABLED = "OrderLineWS,assetIds,validation.assets.but.no.assetmanagement";
    public final static String ERROR_QUANTITY_NOT_MATCH = "OrderLineWS,assetIds,validation.assets.unequal.to.quantity";
    public final static String ERROR_ASSET_LINKED_TO_DIFFERENT_PRODUCT = "OrderLineWS,assetIds,validation.asset.linked.to.different.product";
    public final static String ERROR_ASSET_ALREADY_LINKED = "OrderLineWS,assetIds,validation.asset.already.linked";
    public final static String ERROR_ASSET_STATUS_UNAVAILABLE = "OrderLineWS,assetIds,validation.asset.status.unavailable";
    public final static String ERROR_ASSET_STATUS_RESERVED = "OrderLineWS,assetIds,validation.asset.status.reserved";

    private static final FormatLogger LOG = new FormatLogger(OrderAssetsValidator.class);

    /**
     * Validate assets in given order line
     * @param line Order line for validation
     * @param unlinkedAssets Assets unlinked from another lines
     * @return Error code if validation fails, null otherwise
     */
    public static String validateAssets(OrderLineDTO line, Map<Integer, AssetDTO> unlinkedAssets) {
        return validateAssets(line, unlinkedAssets, true);
    }

    public static String validateAssetsForOrderChangesApply(OrderLineDTO line) {
        return validateAssets(line, new HashMap<Integer, AssetDTO>(), false);
    }

    /**
     * Checks
     * - item allows asset mangement
     * - line quantity match the number of assets
     * - asset is not linked to another order line
     * - if asset is linked for the first time check that the old status is available
     *
     * @param line to validate
     * @param unlinkedAssets assets unlinked from other lines
     * @param validateStatus flag to indicate is status validation always needed, or should be skipped for unlinked lines
     * @return error code or null if validation passed
     */
    private static String validateAssets(OrderLineDTO line, Map<Integer, AssetDTO> unlinkedAssets, boolean validateStatus) {

        if ( null != line.getPurchaseOrder().getResellerOrder() ) {
            LOG.debug("We do not validate Assets for the orders in the parent entity for the Reseller Customer.");
            return null;
        }

        if (line.getDeleted() > 0) {
            return null;
        }
        //if asset management is not done

        if (line.getItem()== null || line.getItem().getAssetManagementEnabled() == 0) {
            if (line.getAssets().size() > 0) {
                return ERROR_ASSET_MANAGEMENT_DISABLED;
            } else {
                return null;
            }
        }

        if (line.getQuantityInt() != line.getAssets().size()) {
            return ERROR_QUANTITY_NOT_MATCH;
        }

        ItemDTO itemDto = line.getItem();

        for (AssetDTO asset : line.getAssets()) {
            //check if this asset was removed from another line
            if (unlinkedAssets.containsKey(asset.getId())) {
                asset = unlinkedAssets.get(asset.getId());
            }
            if (asset.getItem().getId() != itemDto.getId())
                return ERROR_ASSET_LINKED_TO_DIFFERENT_PRODUCT;

            if (asset.getPrevOrderLine() != null) {
                if (asset.getPrevOrderLine().getId() != line.getId()) {
                    return ERROR_ASSET_ALREADY_LINKED;
                }
            } else {
                //this is a new asset to link
                if (validateStatus || !asset.isUnlinkedFromLine()) {
                    /* an asset, that has been added to an order, should not be reserved by any other customer */
                    AssetReservationDTO activeReservation = new AssetReservationDAS().findReservationByAssetNoFlush(asset.getId());
                    if(asset.getAssetStatus().getIsAvailable() == 0 && validateFutureDatedOrder(line, asset)){
                        return ERROR_ASSET_STATUS_UNAVAILABLE;
                    } else if ((activeReservation!=null && activeReservation.getUser().getId()!=line.getPurchaseOrder().getUser().getId())){
                        return ERROR_ASSET_STATUS_RESERVED;
                    }
                }
            }
        }
        return null;
    }

    private static boolean validateFutureDatedOrder(OrderLineDTO line, AssetDTO asset){
        Integer orderId= line.getPurchaseOrder().getId();
        List<OrderChangeDTO> orderChanges = new OrderChangeDAS()
                .findByOrder(orderId);
        for(OrderChangeDTO orderChangeDTO : orderChanges){
            if(null == orderChangeDTO.getOrderLine()
                    && ApplyToOrder.NO.equals(orderChangeDTO.getStatus().getApplyToOrder()) && 1 == asset.getAssetStatus().getIsPending()) {
                if (!orderChangeDTO.getAssets().isEmpty()) {
                    for(AssetDTO assetDTO : orderChangeDTO.getAssets()){
                        if(assetDTO.getId() == asset.getId()){
                            return false;
                        }
                    }
                } else if (!orderChangeDTO.getOrderChangePlanItems().isEmpty()) {
                    for (OrderChangePlanItemDTO orderChangePlanItemDTO : orderChangeDTO.getOrderChangePlanItems()) {
                        for(AssetDTO assetDTO : orderChangePlanItemDTO.getAssets()){
                            if(assetDTO.getId() == asset.getId()){
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
