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

package com.sapienter.jbilling.server.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Alexander Aksenov
 * @since 05.07.13
 */
@ApiModel(value = "Order change status data", description = "OrderChangeStatusWS model")
public class OrderChangeStatusWS implements WSSecured, Serializable {

    private Integer id;
    private Integer order;
    private Integer entityId;
    private int deleted;
    @NotNull(message = "validation.error.notnull")
    private ApplyToOrder applyToOrder;
    @Size(min = 1, message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);

    public OrderChangeStatusWS() {

    }

    @ApiModelProperty(value = "Unique identifier of the order change status", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Identifies the order in which the order change status will appear in the UI")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @ApiModelProperty(value = "Field indicating to the system that when the associated order change status" +
            " is selected on the order, the order line is active," +
            " and the change or update is applied immediately", required = true)
    public ApplyToOrder getApplyToOrder() {
        return applyToOrder;
    }

    public void setApplyToOrder(ApplyToOrder applyToOrder) {
        this.applyToOrder = applyToOrder;
    }

    @ApiModelProperty(value = "List of descriptions in different languages")
    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public void setName(String name,Integer languageId) {
        InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, name);
        addDescription(description);
    }

    @ApiModelProperty(value = "Unique identifier of the company for which the order change status is defined")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "Flag that indicates if this record is logically deleted in the database." +
            " 0 - not deleted; 1 - deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public void addDescription(InternationalDescriptionWS description) {
        this.descriptions.add(description);
    }

    @JsonIgnore
    public InternationalDescriptionWS getDescription(Integer languageId) {
        for (InternationalDescriptionWS descriptionWS : this.descriptions) {
            if (descriptionWS.getLanguageId().equals(languageId)) {
                return descriptionWS;
            }
        }
        return null;
    }


    /**
     * Returns the entity ID of the company owning the secure object, or null
     * if the entity ID is not available.
     *
     * @return owning entity ID
     */
    @JsonIgnore
    @Override
    public Integer getOwningEntityId() {
        return entityId;
    }

    /**
     * Returns the user ID of the user owning the secure object, or null if the
     * user ID is not available.
     *
     * @return owning user ID
     */
    @JsonIgnore
    @Override
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public String toString() {
        return "OrderChangeStatusWS{" +
                "id=" + String.valueOf(id) +
                ", order=" + String.valueOf(order) +
                ", entityId=" + String.valueOf(entityId) +
                ", deleted=" + String.valueOf(deleted) +
                ", applyToOrder=" + String.valueOf(applyToOrder) +
                ", descriptions=" + String.valueOf(descriptions) +
                '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof OrderChangeStatusWS)) {
            return false;
        }
        OrderChangeStatusWS orderChangeStatusWS = (OrderChangeStatusWS) object;
        return nullSafeEquals(this.id, orderChangeStatusWS.id) &&
                nullSafeEquals(this.order, orderChangeStatusWS.order) &&
                nullSafeEquals(this.entityId, orderChangeStatusWS.entityId) &&
                (this.deleted == orderChangeStatusWS.deleted) &&
                nullSafeEquals(this.applyToOrder, orderChangeStatusWS.applyToOrder) &&
                nullSafeEquals(this.descriptions, orderChangeStatusWS.descriptions);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(order);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + deleted;
        result = 31 * result + nullSafeHashCode(applyToOrder);
        result = 31 * result + nullSafeHashCode(descriptions);
        return result;
    }
}
