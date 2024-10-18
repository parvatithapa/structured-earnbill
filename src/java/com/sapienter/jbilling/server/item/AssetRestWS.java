package com.sapienter.jbilling.server.item;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Swapnil
 * @since 11/03/2019
 * @see com.sapienter.jbilling.server.item.db.AssetDTO
 */
@ApiModel(value = "Asset Data", description = "AssetRestWS model")
public class AssetRestWS {

    private Integer id;
    @NotNull(message = "validation.error.null.asset.identifier")
    @Size(min = 1, max = 200, message = "validation.error.size,1,200")
    private String identifier;
    @ConvertToTimezone
    private Date createDatetime;
    private String status;
    @NotNull(message = "validation.error.null.asset.status")
    private Integer assetStatusId;
    @NotNull(message = "validation.error.null.item")
    private Integer itemId;
    private String itemDescription;
    private String productCode;
    private Integer orderLineId;
    private int deleted;
    @Size(min = 0, max = 1000, message = "validation.error.length.max,1000")
    private String notes;
    private Integer entityId;
    private boolean global = false;
    @Valid
    private Map<String, Object> metaFieldsMap;
    private AssetAssignmentWS[] assignments;
    private Integer orderId;
    private Integer categoryId;
    @ConvertToTimezone
    private Date startTime;

    @ApiModelProperty(value = "System unique identifier of this asset")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    @ApiModelProperty(value = "Flag set if the asset is global")
    @JsonProperty(value = "global")
    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global ? true : false;
    }

    @ApiModelProperty(value = "Defined meta field values for the asset")
    public Map<String, Object> getMetaFieldsMap() {
        return metaFieldsMap;
    }

    public void setMetaFieldsMap(Map<String, Object> metaFieldsMap) {
        this.metaFieldsMap = metaFieldsMap;
    }

    @ApiModelProperty(value = "List of assignments for the asset")
    public AssetAssignmentWS[] getAssignments() {
        return assignments;
    }

    public void setAssignments(AssetAssignmentWS[] assignments) {
        this.assignments = assignments;
    }

    @ApiModelProperty(value = "Current company id")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    @ApiModelProperty(value = "Order id")
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @ApiModelProperty(value = "Item Category id")
    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AssetRestWS [id=");
        builder.append(id);
        builder.append(", global=");
        builder.append(global);
        builder.append(", metaFieldsMap=");
        builder.append(metaFieldsMap);
        builder.append(", assignments=");
        builder.append(Arrays.toString(assignments));
        builder.append(", identifier=");
        builder.append(identifier);
        builder.append(", createDatetime=");
        builder.append(createDatetime);
        builder.append(", status=");
        builder.append(status);
        builder.append(", assetStatusId=");
        builder.append(assetStatusId);
        builder.append(", itemId=");
        builder.append(itemId);
        builder.append(", orderLineId=");
        builder.append(orderLineId);
        builder.append(", deleted=");
        builder.append(deleted);
        builder.append(", notes=");
        builder.append(notes);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append("]");
        return builder.toString();
    }

}
