package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;

/**
 * @author Klim
 */
public class AssetEnvelope extends AssetWS {

    private ItemTypeDTO itemType;

    public AssetEnvelope() {
    }

    public AssetEnvelope(AssetDTO dto) {
        AssetBL.getWS(dto);
        this.itemType = dto.getItem().findItemTypeWithAssetManagement();
    }

    public AssetEnvelope(AssetWS asset, ItemTypeDTO itemType) {

        setId(asset.getId());
        setIdentifier(asset.getIdentifier());
        setCreateDatetime(asset.getCreateDatetime());
        setStatus(asset.getStatus());
        setAssetStatusId(asset.getAssetStatusId());
        setItemId(asset.getItemId());
        setOrderLineId(asset.getOrderLineId());
        setDeleted(asset.getDeleted());
        setNotes(asset.getNotes());
        setEntityId(asset.getEntityId());
        setContainedAssetIds(asset.getContainedAssetIds());
        setGroupId(asset.getGroupId());
        setMetaFields(asset.getMetaFields());

        this.itemType = itemType;
    }

    public ItemTypeDTO getItemType() {
        return itemType;
    }

    public void setItemType(ItemTypeDTO itemType) {
        this.itemType = itemType;
    }
}
