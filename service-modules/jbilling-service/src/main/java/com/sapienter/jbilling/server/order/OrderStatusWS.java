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

package com.sapienter.jbilling.server.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Maruthi
 */
@ApiModel(value = "Order status data", description = "OrderStatusWS model")
public class OrderStatusWS implements WSSecured, Serializable {

    private static final long serialVersionUID = 20140605L;

	private Integer id;
    private OrderStatusFlag orderStatusFlag;
    private Integer userId = null;
    private CompanyWS entity;
    @Size(min = 1, max = 50,message = "validation.error.size,1,50")
    private String description;
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);
   
    
    public OrderStatusWS() {
    }

    public OrderStatusWS(Integer id, CompanyWS entity, OrderStatusFlag orderStatusFlag, String description) {
        setId(id);
        setEntity(entity);
        setOrderStatusFlag(orderStatusFlag);
        setDescription(description);
    }

    @ApiModelProperty(value = "Description for the order status")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

    @ApiModelProperty(value = "Enumeration flag for the order status")
	public OrderStatusFlag getOrderStatusFlag() {
		return orderStatusFlag;
	}

	public void setOrderStatusFlag(OrderStatusFlag orderStatusFlag) {
		this.orderStatusFlag = orderStatusFlag;
	}

    @JsonIgnore
	public CompanyWS getEntity() {
		return entity;
	}

    @JsonIgnore
	public void setEntity(CompanyWS entity) {
		this.entity = entity;
	}

    @ApiModelProperty(value = "Unique identifier of the order status", required = true)
	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @JsonIgnore
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "List of descriptions in different languages for the order status")
    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("OrderStatusWS");
        sb.append("{id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", orderStatusFlag=").append(orderStatusFlag);
        sb.append(", entity=").append(entity);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     * @return null
     */
    @JsonIgnore
    public Integer getOwningEntityId() {
        return entity!=null ? entity.getId() : null;
    }

    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof OrderStatusWS)) {
            return false;
        }
        OrderStatusWS orderStatusWS = (OrderStatusWS) object;
        return nullSafeEquals(this.id, orderStatusWS.id) &&
                nullSafeEquals(this.userId, orderStatusWS.userId) &&
                nullSafeEquals(this.entity, orderStatusWS.entity) &&
                nullSafeEquals(this.orderStatusFlag, orderStatusWS.orderStatusFlag) &&
                nullSafeEquals(this.description, orderStatusWS.description) &&
                nullSafeEquals(this.descriptions, orderStatusWS.descriptions);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(userId);
        result = 31 * result + nullSafeHashCode(entity);
        result = 31 * result + nullSafeHashCode(orderStatusFlag);
        result = 31 * result + nullSafeHashCode(description);
        result = 31 * result + nullSafeHashCode(descriptions);
        return result;
    }
}
