package com.sapienter.jbilling.server.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * Created by aristokrates on 2/17/14.
 */
@ApiModel(value = "Order change type data", description = "OrderChangeTypeWS model")
public class OrderChangeTypeWS implements WSSecured, Serializable {

    private Integer id;

    private Integer entityId;

    @NotNull(message = "validation.error.notnull")
    @Size(min=1,max=255, message="validation.error.size,1,255")
    private String name;

    private boolean defaultType;

    private boolean allowOrderStatusChange;

    private List<Integer> itemTypes = new ArrayList<Integer>(0);

    @Valid
    private Set<MetaFieldWS> orderChangeTypeMetaFields = new HashSet<MetaFieldWS>(0);

    public OrderChangeTypeWS() {
    }

    @ApiModelProperty(value = "Unique identifier of the order change type", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the company for which the order change type is defined")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "Name of the order change type")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Flag set if the order change type is the default one for the company")
    @JsonProperty("defaultType")
    public boolean isDefaultType() {
        return defaultType;
    }

    public void setDefaultType(boolean defaultType) {
        this.defaultType = defaultType;
    }

    @ApiModelProperty(value = "Flag set if an order status change is required for the order change type")
    @JsonProperty("allowOrderStatusChange")
    public boolean isAllowOrderStatusChange() {
        return allowOrderStatusChange;
    }

    public void setAllowOrderStatusChange(boolean allowOrderStatusChange) {
        this.allowOrderStatusChange = allowOrderStatusChange;
    }

    @ApiModelProperty(value = "If the order change type applies to specific products," +
            " this list will contain the ids of the item types of those products")
    public List<Integer> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(List<Integer> itemTypes) {
        this.itemTypes = itemTypes;
    }

    @ApiModelProperty(value = "List of defined meta fields for the order change type")
    public Set<MetaFieldWS> getOrderChangeTypeMetaFields() {
        return orderChangeTypeMetaFields;
    }

    public void setOrderChangeTypeMetaFields(Set<MetaFieldWS> orderChangeTypeMetaFields) {
        this.orderChangeTypeMetaFields = orderChangeTypeMetaFields;
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

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof OrderChangeTypeWS)) {
            return false;
        }
        OrderChangeTypeWS orderChangeTypeWS = (OrderChangeTypeWS) object;
        return nullSafeEquals(this.id, orderChangeTypeWS.id) &&
                nullSafeEquals(this.name, orderChangeTypeWS.name) &&
                nullSafeEquals(this.entityId, orderChangeTypeWS.entityId) &&
                (this.defaultType == orderChangeTypeWS.defaultType) &&
                (this.allowOrderStatusChange == orderChangeTypeWS.allowOrderStatusChange) &&
                nullSafeEquals(this.itemTypes, orderChangeTypeWS.itemTypes) &&
                nullSafeEquals(this.orderChangeTypeMetaFields, orderChangeTypeWS.orderChangeTypeMetaFields);
    }


    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(name);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + (defaultType ? 1 : 0);
        result = 31 * result + (allowOrderStatusChange ? 1 : 0);
        result = 31 * result + nullSafeHashCode(itemTypes);
        result = 31 * result + nullSafeHashCode(orderChangeTypeMetaFields);
        return result;
    }
}
