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

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Version of AssetStatusDTO which is safe for external communication.
 *
 * @author Gerhard
 * @since 15/04/13
 * @see com.sapienter.jbilling.server.item.db.AssetStatusDTO
 */
@ApiModel(value = "Asset Status Data", description = "AssetStatusDTOEx model")
public class AssetStatusDTOEx implements Serializable {

    private int id;
    @Size(min=1,max=50, message="validation.error.size,1,50")
    private String description;
    private int isDefault;
    private int isAvailable;
    private int isOrderSaved;
    private int isInternal;
    private int isActive;
    private int isPending;
    private int isOrderFinished;

    public AssetStatusDTOEx() {

    }

    public AssetStatusDTOEx(int id, String description, int aDefault, int available, int orderSaved, int internal) {
        this.id = id;
        this.description = description;
        this.isDefault = aDefault;
        this.isAvailable = available;
        this.isOrderSaved = orderSaved;
        this.isInternal = internal;
        this.isPending = 0;
        this.isActive = 0;
        this.isOrderFinished = 0;
    }

    public AssetStatusDTOEx(int id, String description, int aDefault, int available, int orderSaved, int internal, int active, int pending, int orderFinished) {
        this.id = id;
        this.description = description;
        this.isDefault = aDefault;
        this.isAvailable = available;
        this.isOrderSaved = orderSaved;
        this.isInternal = internal;
        this.isActive = active;
        this.isPending = pending;
        this.isOrderFinished = orderFinished;
    }

    @ApiModelProperty(value = "The id of the asset status entity")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The description of the asset status entity")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Specifies if this status is internal")
    public int getIsInternal() {
        return isInternal;
    }

    public void setIsInternal(int internal) {
        isInternal = internal;
    }

    @ApiModelProperty(value = "Specifies if this status is default")
    public int getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(int aDefault) {
        isDefault = aDefault;
    }

    @ApiModelProperty(value = "Specifies if this status is available")
    public int getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(int available) {
        isAvailable = available;
    }

    @ApiModelProperty(value = "Specifies if this status is order saved")
    public int getIsOrderSaved() {
        return isOrderSaved;
    }

    public void setIsOrderSaved(int orderSaved) {
        isOrderSaved = orderSaved;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public int getIsPending() {
        return isPending;
    }

    public void setIsPending(int isPending) {
        this.isPending = isPending;
    }

    public int getIsOrderFinished() {
        return isOrderFinished;
    }

    public void setIsOrderFinished(int isOrderFinished) {
        this.isOrderFinished = isOrderFinished;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetStatusDTOEx)) return false;

        AssetStatusDTOEx that = (AssetStatusDTOEx) o;

        if (id != that.id) return false;
        if (isDefault != that.isDefault) return false;
        if (isAvailable != that.isAvailable) return false;
        if (isOrderSaved != that.isOrderSaved) return false;
        if (isInternal != that.isInternal) return false;
        return !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + isDefault;
        result = 31 * result + isAvailable;
        result = 31 * result + isOrderSaved;
        result = 31 * result + isInternal;
        return result;
    }
}