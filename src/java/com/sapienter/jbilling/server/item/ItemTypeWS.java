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
package com.sapienter.jbilling.server.item;

import com.fasterxml.jackson.annotation.*;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.cxf.CxfSMapIntMetafieldsAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Brian Cowdery
 * @since 07-10-2009
 */
@ApiModel(value = "Item Type Data", description = "ItemTypeWS model")
public class ItemTypeWS implements Serializable, HierarchicalEntity, WSSecured {

    private Integer id;
    
    @Size (min=1,max=100, message="validation.error.size,1,100")    
    private String description;
    
    @Min(value = 1, message="validation.error.min,1")
    private Integer orderLineTypeId;

    private Integer parentItemTypeId;
    
    private boolean global;
    private boolean internal;

    private Integer entityId;
    private List<Integer> entities = new ArrayList<Integer>(0);

    private Integer allowAssetManagement = new Integer(0);
    private String assetIdentifierLabel;
    @Valid
    private Set<AssetStatusDTOEx> assetStatuses = new HashSet<>(0);

    @Valid
    private Set<MetaFieldWS> assetMetaFields = new HashSet<>(0);

    @Valid
    private MetaFieldValueWS[] metaFields;
    private SortedMap<Integer, MetaFieldValueWS[]> metaFieldsMap = new TreeMap<>();

    private boolean onePerCustomer = false;
    private boolean onePerOrder = false;
    
    public ItemTypeWS() {
    }

    public ItemTypeWS(Integer id, String description, Integer orderLineTypeId, Integer allowAssetManagement) {
        this.id = id;
        this.description = description;
        this.orderLineTypeId = orderLineTypeId;
        this.allowAssetManagement = allowAssetManagement;
    }

    @ApiModelProperty(value = "This defines the Label of the asset identifier. Every asset has an identifier which is unique and by default it's just a word Identifier but you can override it")
    public String getAssetIdentifierLabel() {
        return assetIdentifierLabel;
    }

    public void setAssetIdentifierLabel(String assetIdentifierLabel) {
        this.assetIdentifierLabel = assetIdentifierLabel;
    }

    @ApiModelProperty(value = "Specifies if this item category allows asset management")
    public Integer getAllowAssetManagement() {
        return allowAssetManagement;
    }

    public void setAllowAssetManagement(Integer allowAssetManagement) {
        this.allowAssetManagement = allowAssetManagement;
    }

    @ApiModelProperty(value = "This is a list with all the statuses an asset can have. An status can be marked as Active, Default or Order Saved")
    public Set<AssetStatusDTOEx> getAssetStatuses() {
        return assetStatuses;
    }

    public void setAssetStatuses(Set<AssetStatusDTOEx> assetStatuses) {
        this.assetStatuses = assetStatuses;
    }

    @ApiModelProperty(value = "Set of meta-fields regarding assets")
    public Set<MetaFieldWS> getAssetMetaFields() {
        return assetMetaFields;
    }

    public void setAssetMetaFields(Set<MetaFieldWS> assetMetaFields) {
        this.assetMetaFields = assetMetaFields;
    }

    @ApiModelProperty(value = "If a product from a given category can be added to order and if it belongs to one per customer category then its quantity can not exceed one")
    @JsonProperty(value = "onePerCustomer")
    public boolean isOnePerCustomer() {
		return onePerCustomer;
	}

	public void setOnePerCustomer(boolean onePerCustomer) {
		this.onePerCustomer = onePerCustomer;
	}

    @ApiModelProperty(value = "If a product from a given category can be added to order and if it belongs to one per order category then its quantity can not exceed one")
    @JsonProperty(value = "onePerOrder")
	public boolean isOnePerOrder() {
		return onePerOrder;
	}

	public void setOnePerOrder(boolean onePerOrder) {
		this.onePerOrder = onePerOrder;
	}

    @ApiModelProperty(value = "The id of the item category")
	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The description of the item category")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Type of order line for this item category")
    public Integer getOrderLineTypeId() {
        return orderLineTypeId;
    }

    public void setOrderLineTypeId(Integer orderLineTypeId) {
        this.orderLineTypeId = orderLineTypeId;
    }

    @ApiModelProperty(value = "The id of the parent for this item category")
    public Integer getParentItemTypeId() {
        return parentItemTypeId;
    }

    public void setParentItemTypeId(Integer parentItemTypeId) {
        this.parentItemTypeId = parentItemTypeId;
    }

    @ApiModelProperty(value = "Specifies if this item category is global or not")
    @JsonProperty("global")
    public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

    @JsonIgnore
    public boolean isInternal() {
        return internal;
    }

    @JsonIgnore
    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    @ApiModelProperty(value = "The id of the owning company")
    public Integer getEntityId() {
        return this.entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "A list of companies ids for which this item category is available")
	public List<Integer> getEntities() {
		return entities;
	}

	public void setEntities(List<Integer> entities) {
		this.entities = entities;
	}

    @ApiModelProperty(value = "A list of meta field values")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "This is a map of meta fields for the product. When creating a product you can have meta fields to fill in. Just like any other meta field in the system")
    @XmlJavaTypeAdapter(CxfSMapIntMetafieldsAdapter.class)
    public SortedMap<Integer, MetaFieldValueWS[]> getMetaFieldsMap() {
        return metaFieldsMap;
    }

    public void setMetaFieldsMap(SortedMap<Integer, MetaFieldValueWS[]> metaFieldsMap) {
        this.metaFieldsMap = metaFieldsMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemTypeWS)) return false;

        ItemTypeWS that = (ItemTypeWS) o;

        return global == that.global &&
                internal == that.internal &&
                onePerCustomer == that.onePerCustomer &&
                onePerOrder == that.onePerOrder &&
                nullSafeEquals(id, that.id) &&
                nullSafeEquals(description, that.description) &&
                nullSafeEquals(orderLineTypeId, that.orderLineTypeId) &&
                nullSafeEquals(parentItemTypeId, that.parentItemTypeId) &&
                nullSafeEquals(entityId, that.entityId) &&
                nullSafeEquals(entities, that.entities) &&
                nullSafeEquals(allowAssetManagement, that.allowAssetManagement) &&
                nullSafeEquals(assetIdentifierLabel, that.assetIdentifierLabel) &&
                nullSafeEquals(assetStatuses, that.assetStatuses) &&
                nullSafeEquals(assetMetaFields, that.assetMetaFields) &&
                nullSafeEquals(metaFields, that.metaFields) &&
                nullSafeEquals(metaFieldsMap, that.metaFieldsMap);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(description);
        result = 31 * result + nullSafeHashCode(orderLineTypeId);
        result = 31 * result + nullSafeHashCode(parentItemTypeId);
        result = 31 * result + (global ? 1 : 0);
        result = 31 * result + (internal ? 1 : 0);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + nullSafeHashCode(entities);
        result = 31 * result + nullSafeHashCode(allowAssetManagement);
        result = 31 * result + nullSafeHashCode(assetIdentifierLabel);
        result = 31 * result + nullSafeHashCode(assetStatuses);
        result = 31 * result + nullSafeHashCode(assetMetaFields);
        result = 31 * result + nullSafeHashCode(metaFields);
        result = 31 * result + nullSafeHashCode(metaFieldsMap);
        result = 31 * result + (onePerCustomer ? 1 : 0);
        result = 31 * result + (onePerOrder ? 1 : 0);
        return result;
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return this.entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    @JsonIgnore
    public List<Integer> getAccessEntities() {
        return this.entities;
    }

    /**
     * Named differently to avoid name conflict with implementing entities.
     *
     * @return
     */
    @Override
    @JsonIgnore
    public Boolean ifGlobal() {
        return Boolean.valueOf(this.global);
    }

}
