package com.sapienter.jbilling.server.order;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Transient;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

/**
 * The class will transfer the plan items changeable properties.
 *
 * @author Maeis Gharibjanian
 * @since 10-07-2013
 */
public class OrderLinePlanItemDTOEx implements MetaContent, Serializable {

    private Integer itemId;
    private String description;
    private Boolean useItem;
    private Integer[] assetIds;
    private List<MetaFieldValue> metaFields = new LinkedList<MetaFieldValue>();

    private OrderLineDTO lineCreated;
    
    public OrderLinePlanItemDTOEx(){}
    
    /**
     * ItemDTO id of the plan line item.
     * @return ItemDTO id of the plan line item.
     */
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    /**
     * New description of editable plan item.
     * @return New description of editable plan item.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Indicates to use item price and description.
     * @return true if need to use item price and description.
     */
    public Boolean getUseItem() {
        return useItem;
    }

    public void setUseItem(Boolean useItem) {
        this.useItem = useItem;
    }

    /**
     * AssetDTO ids of the assets are assigned to the plan item.
     * @return array of ids.
     */
    public Integer[] getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(Integer[] assetIds) {
        this.assetIds = assetIds;
    }

    public OrderLineDTO getLineCreated() {
        return lineCreated;
    }

    public void setLineCreated(OrderLineDTO lineCreated) {
        this.lineCreated = lineCreated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderLinePlanItemDTOEx that = (OrderLinePlanItemDTOEx) o;

        if (!Arrays.equals(assetIds, that.assetIds)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
        if (useItem != null ? !useItem.equals(that.useItem) : that.useItem != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = itemId != null ? itemId.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (useItem != null ? useItem.hashCode() : 0);
        result = 31 * result + (assetIds != null ? Arrays.hashCode(assetIds) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OrderLinePlanItemDTOEx{" +
                "itemId=" + itemId +
                ", description='" + description + '\'' +
                ", useItem=" + useItem +
                ", assetIds=" + Arrays.toString(assetIds) +
                '}';
    }

    @Transient
    public void setMetaFields(List<MetaFieldValue> fields) {
        this.metaFields = fields;
    }

    @Override
    public List<MetaFieldValue> getMetaFields() {
        return metaFields;
    }

    @Transient
    public MetaFieldValue getMetaField(String name) {
        return MetaFieldHelper.getMetaField(this, name);
    }

    @Transient
    public MetaFieldValue getMetaField(String name, Integer groupId) {
        return MetaFieldHelper.getMetaField(this, name, groupId);
    }

    @Transient
    public MetaFieldValue getMetaField(Integer metaFieldNameId) {
        return MetaFieldHelper.getMetaField(this, metaFieldNameId);
    }

    @Transient
    public void setMetaField(MetaFieldValue field, Integer groupId) {
        MetaFieldHelper.setMetaField(this, field, groupId);
    }

    @Transient
    public void setMetaField(Integer entitId, Integer groupId, String name, Object value) throws IllegalArgumentException {
        MetaFieldHelper.setMetaField(entitId, groupId, this, name, value);
    }

    @Transient
    public void updateMetaFieldsWithValidation(Integer languageId, Integer entitId, Integer accountTypeId, MetaContent dto) {
        MetaFieldHelper.updateMetaFieldsWithValidation(languageId, entitId, accountTypeId, this, dto);
    }

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ORDER_LINE };
    }
}
