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
package com.sapienter.jbilling.server.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.cxf.CxfSMapIntMetafieldsAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Gerhard
 * @since 15/04/13
 * @see com.sapienter.jbilling.server.item.db.AssetDTO
 */
@ApiModel(value = "Asset Data", description = "AssetWS model")
public class AssetWS implements WSSecured, HierarchicalEntity, Serializable {

    private Integer id;
    @NotNull(message = "validation.error.null.asset.identifier")
    @Size(min=1,max=200, message="validation.error.size,1,200")
    private String identifier;

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

    @ConvertToTimezone
    private Date createDatetime;
    private String status;
    @NotNull(message = "validation.error.null.asset.status")
    private Integer assetStatusId;
    @NotNull(message = "validation.error.null.item")
    private Integer itemId;
    private Integer orderLineId;
    private int deleted;
    @Size(min=0,max=1000, message="validation.error.length.max,1000")
    private String notes;
    private Integer entityId;
    private Integer[] containedAssetIds;
    private Integer groupId;
    private ProvisioningCommandWS[] provisioningCommands;
    private boolean global = false;
    @Valid
    private MetaFieldValueWS[] metaFields;
    private List<Integer> entities = new ArrayList<Integer>(0);
	private AssetAssignmentWS[] assignments;

    private SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap = new TreeMap<Integer, MetaFieldValueWS[]>();

    // ICCID changes

    private String subscriberNumber;
    private String imsi;
    public boolean isSuspended;
    private String pin1;
    private String pin2;
    private String puk1;
    private String puk2;
    private String discardedIdentifier;
    private String suspendedBy;
    public AssetWS() {
    }

    public AssetWS(AssetDTO dto) {
        this.id = dto.getId();
        this.identifier = dto.getIdentifier();
        this.createDatetime = dto.getCreateDatetime();
        this.notes = dto.getNotes();
        this.assetStatusId = dto.getAssetStatus().getId();
        this.entityId = null != dto.getEntity() ? dto.getEntity().getId() : null;
        this.itemId = dto.getItem().getId();
        this.orderLineId = (dto.getOrderLine() != null) ? dto.getOrderLine().getId() : null;
        this.global = dto.isGlobal();
        this.containedAssetIds = new Integer[dto.getContainedAssets().size()];
        this.subscriberNumber = dto.getSubscriberNumber();
        this.imsi= dto.getImsi();
        this.isSuspended = dto.isSuspended();
        this.pin1 = dto.getPin1();
        this.pin2 = dto.getPin2();
        this.puk1 = dto.getPuk1();
        this.puk2 = dto.getPuk2();
        this.discardedIdentifier= dto.getDiscardedIdentifier();
        this.suspendedBy=dto.getSuspendedBy();
        int idx = 0;
        for(AssetDTO containtedAsset : dto.getContainedAssets()) {
            this.containedAssetIds[idx++] = containtedAsset.getId();
        }

        if(dto.getGroup() != null) {
            this.groupId = dto.getGroup().getId();
        }
    }

    @ApiModelProperty(value = "System unique identifier of this asset")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "In case this asset is part of a group, this field represents the id of the the asset group it belongs to")
    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    @ApiModelProperty(value = "In case the asset represents an asset group, this array contains the asset ids belonging to that group")
    public Integer[] getContainedAssetIds() {
        return containedAssetIds;
    }

    public void setContainedAssetIds(Integer[] containedAssetIds) {
        this.containedAssetIds = containedAssetIds;
    }

    @ApiModelProperty(value = "Unique identifier of the order line to which an asset is added")
    public Integer getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }

    @ApiModelProperty(value = "String representing the asset identifier", required = true)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty(value = "Date and time of the creation")
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ApiModelProperty(value = "Asset status string")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiModelProperty(value = "Id of the current status of the asset", required = true)
    public Integer getAssetStatusId() {
        return assetStatusId;
    }

    public void setAssetStatusId(Integer assetStatusId) {
        this.assetStatusId = assetStatusId;
    }

    @ApiModelProperty(value = "Id of the item for which this asset is defined", required = true)
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @ApiModelProperty(value = "Flag set if the asset is deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "Notes for the asset")
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @ApiModelProperty(value = "Defined meta field values for the asset")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "Current company id")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "An array of associated provisioning commands to the asset")
    public ProvisioningCommandWS[] getProvisioningCommands() {
        return provisioningCommands;
    }

    public void setProvisioningCommands(ProvisioningCommandWS[] provisioningCommands) {
        this.provisioningCommands = provisioningCommands;
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    /**
     * Returns the list of jBilling Entity IDs within a hierarchy that have access to this object.
     *
     * @return list of entities that have access.
     */
    @Override
    @JsonIgnore
    public List<Integer> getAccessEntities() {
        return this.entities;
    }

    @ApiModelProperty(value = "Flag set if the asset is global")
    @JsonProperty(value = "global")
    public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global ? true : false ;
	}

	@ApiModelProperty(value = "List of company ids for which the asset is available")
	public List<Integer> getEntities() {
		return entities;
	}

	public void setEntities(List<Integer> entities) {
		this.entities = entities;
	}

	@ApiModelProperty(value = "List of assignments for the asset")
	public AssetAssignmentWS[] getAssignments() {
		return assignments;
	}

	public void setAssignments(AssetAssignmentWS[] assignments) {
		this.assignments = assignments;
	}

    @XmlJavaTypeAdapter(CxfSMapIntMetafieldsAdapter.class)
    public SortedMap <Integer, MetaFieldValueWS[]> getMetaFieldsMap() {
		return metaFieldsMap;
	}

	public void setMetaFieldsMap(SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap) {
		this.metaFieldsMap = metaFieldsMap;
	}

    @ApiModelProperty(value = "subscriber number")

    public String getSubscriberNumber() {
        return subscriberNumber;
    }

    public void setSubscriberNumber(String subscriberNumber) {
        this.subscriberNumber = subscriberNumber;
    }

    @ApiModelProperty(value = "Asset imsi string")
    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    @ApiModelProperty(value = "Asset temporary suspended")
    public boolean isSuspended() {
        return isSuspended;
    }

    public void setSuspended(boolean suspended) {
        isSuspended = suspended;
    }

    @ApiModelProperty(value = "pin 1")
    public String getPin1() {
        return pin1;
    }

    public void setPin1(String pin1) {
        this.pin1 = pin1;
    }

    @ApiModelProperty(value = "pin 2")
    public String getPin2() {
        return pin2;
    }

    public void setPin2(String pin2) {
        this.pin2 = pin2;
    }

    @ApiModelProperty(value = "puk 1")
    public String getPuk1() {
        return puk1;
    }

    public void setPuk1(String puk1) {
        this.puk1 = puk1;
    }

    @ApiModelProperty(value = "puk 2")
    public String getPuk2() {
        return puk2;
    }

    public void setPuk2(String puk2) {
        this.puk2 = puk2;
    }

    @ApiModelProperty(value = "discarded_identifier")
    public String getDiscardedIdentifier() {
        return this.discardedIdentifier;
    }

    public void setDiscardedIdentifier(String discardedIdentifier) {
        this.discardedIdentifier = discardedIdentifier;
    }

    @ApiModelProperty(value = "suspended_by")
    public String getSuspendedBy() {
        return this.suspendedBy;
    }

    public void setSuspendedBy(String suspendedBy) {
        this.suspendedBy = suspendedBy;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetWS)) return false;

        AssetWS assetWS = (AssetWS) o;
        return deleted == assetWS.deleted &&
                global == assetWS.global &&
                nullSafeEquals(id, assetWS.id) &&
                nullSafeEquals(identifier, assetWS.identifier) &&
                nullSafeEquals(createDatetime, assetWS.createDatetime) &&
                nullSafeEquals(status, assetWS.status) &&
                nullSafeEquals(assetStatusId, assetWS.assetStatusId) &&
                nullSafeEquals(itemId, assetWS.itemId) &&
                nullSafeEquals(orderLineId, assetWS.orderLineId) &&
                nullSafeEquals(notes, assetWS.notes) &&
                nullSafeEquals(entityId, assetWS.entityId) &&
                nullSafeEquals(containedAssetIds, assetWS.containedAssetIds) &&
                nullSafeEquals(groupId, assetWS.groupId) &&
                nullSafeEquals(provisioningCommands, assetWS.provisioningCommands) &&
                nullSafeEquals(metaFields, assetWS.metaFields) &&
                nullSafeEquals(entities, assetWS.entities) &&
                nullSafeEquals(assignments, assetWS.assignments);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(identifier);
        result = 31 * result + nullSafeHashCode(createDatetime);
        result = 31 * result + nullSafeHashCode(status);
        result = 31 * result + nullSafeHashCode(assetStatusId);
        result = 31 * result + nullSafeHashCode(itemId);
        result = 31 * result + nullSafeHashCode(orderLineId);
        result = 31 * result + deleted;
        result = 31 * result + nullSafeHashCode(notes);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + nullSafeHashCode(containedAssetIds);
        result = 31 * result + nullSafeHashCode(groupId);
        result = 31 * result + nullSafeHashCode(provisioningCommands);
        result = 31 * result + (global ? 1 : 0);
        result = 31 * result + nullSafeHashCode(metaFields);
        result = 31 * result + nullSafeHashCode(entities);
        result = 31 * result + nullSafeHashCode(assignments);
        return result;
    }

    @Override
    public String toString() {
        return "AssetWS{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", createDatetime=" + createDatetime +
                ", status='" + status + '\'' +
                ", assetStatusId=" + assetStatusId +
                ", itemId=" + itemId +
                ", orderLineId=" + orderLineId +
                ", deleted=" + deleted +
                ", notes='" + notes + '\'' +
                ", entityId=" + entityId +
                ", containedAssetIds=" + Arrays.toString(containedAssetIds) +
                ", groupId=" + groupId +
                ", metaFields=" + Arrays.toString(metaFields) +
                "  subscriberNumber=" + subscriberNumber +
                ", imsi=" + imsi +
                ", isSuspended=" + isSuspended +
                ", pin1=" + pin1 +
                ", pin2=" + pin2 +
                ", puk1=" + puk1 +
                ", puk2=" + puk2 +
                ", discardedIdentifier=" + discardedIdentifier +
                ", suspendedBy=" + suspendedBy +
                '}';
    }
}
