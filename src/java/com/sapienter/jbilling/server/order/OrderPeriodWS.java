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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * OrderPeriodWS
 *
 * @author Vikas Bodani
 * @since 29-03-2011
 */
@ApiModel(value = "Order Period Data", description = "OrderPeriodWS model")
public class OrderPeriodWS implements WSSecured, Serializable {

    private Integer id;
    private Integer entityId;
    
    private Integer periodUnitId;

    @NotNull(message = "orderPeriodWS.value.validation.error.notnull")
    @Min(value = 1, message = "validation.error.min,1")
    @Digits(integer=3, fraction=0, message="validation.error.invalid.digits.max.three")
    private Integer value;
    private Integer versionNum;

    @Size(min=1, message="validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>();


    public OrderPeriodWS() {
    }

    public OrderPeriodWS(Integer id, Integer entityId, Integer periodUnitId, Integer value) {
       this.id = id;
       this.entityId = entityId;
       this.periodUnitId= periodUnitId;
       this.value = value;
    }

	@ApiModelProperty(value = "The id of the order period entity")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "The id of the company for which this order period entity exists")
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	@ApiModelProperty(value = "The id of the period unit used for this entity",
			allowableValues = "1(Month), 2(Week), 3(Day), 4(Year), 5(Semi Monthly)")
	public Integer getPeriodUnitId() {
		return periodUnitId;
	}

	public void setPeriodUnitId(Integer periodUnitId) {
		this.periodUnitId = periodUnitId;
	}

	@ApiModelProperty(value = "The value of the period unit id",
			required = true)
	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	@ApiModelProperty("Array of all descriptions regarding this order period")
	public List<InternationalDescriptionWS> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
		this.descriptions = descriptions;
	}

	public InternationalDescriptionWS getDescription(Integer languageId) {
        for (InternationalDescriptionWS description : descriptions)
            if (description.getLanguageId().equals(languageId))
                return description;
        return null;
    }

	public String toString() {
		return "OrderPeriodWS [id=" + id + ", entityId=" + entityId
				+ ", periodUnitId=" + periodUnitId + ", value=" + value
				+ ", descriptions=" + descriptions + "]";
	}

	@JsonIgnore
	public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OrderPeriodWS)) return false;

		OrderPeriodWS that = (OrderPeriodWS) o;

		return nullSafeEquals(id, that.id) &&
				nullSafeEquals(entityId, that.entityId) &&
				nullSafeEquals(periodUnitId, that.periodUnitId) &&
				nullSafeEquals(value, that.value) &&
				nullSafeEquals(versionNum, that.versionNum) &&
				nullSafeEquals(descriptions, that.descriptions);
	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(id);
		result = 31 * result + nullSafeHashCode(entityId);
		result = 31 * result + nullSafeHashCode(periodUnitId);
		result = 31 * result + nullSafeHashCode(value);
		result = 31 * result + nullSafeHashCode(versionNum);
		result = 31 * result + nullSafeHashCode(descriptions);
		return result;
	}
}


