/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.io.Serializable;

/**
 * @author: Alexander Aksenov
 * @since: 21.03.14
 */
@ApiModel(value = "Order Change for plan item", description = "OrderChangePlanItemWS model")
public class OrderChangePlanItemWS implements Serializable {
    private int id;
    private int itemId;
    private String description;
    private int[] assetIds;
    @Valid
    private MetaFieldValueWS[] metaFields;
    private Integer optlock;

    /** Here for convenience. Not used by the server */
    private Integer bundledQuantity;

    @ApiModelProperty(value = "Identifier of the object")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Item ID of the plan item", required = true)
    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    @ApiModelProperty(value = "Description of the plan item")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Assets linked to the plan item")
    public int[] getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(int[] assetIds) {
        this.assetIds = assetIds;
    }

    @ApiModelProperty(value = "Meta field values as required by the item")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "Quantity included in the plan")
    public Integer getBundledQuantity() {
        return bundledQuantity;
    }

    public void setBundledQuantity(Integer bundledQuantity) {
        this.bundledQuantity = bundledQuantity;
    }

    public Integer getOptlock() {
        return optlock;
    }

    public void setOptlock(Integer optlock) {
        this.optlock = optlock;
    }
}