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
package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * This entities (WSs) are meant to be created by the system and only
 * updated by the system and not from outside. This WS object is only
 * to provide the client with information about the asset assignment.
 *
 * @author Vladimir Carevski
 * @since 30-OCT-2014
 */
@ApiModel(value = "Asset Assignment Data", description = "AssetAssignmentWS model")
public class AssetAssignmentWS implements Serializable {

	private Integer id;
	@NotNull(message = "validation.error.notnull")
	private Integer assetId;
	@NotNull(message = "validation.error.notnull")
	private Integer orderId;
	@NotNull(message = "validation.error.notnull")
	private Integer orderLineId;
	private Date startDatetime;
	private Date endDatetime;

	@ApiModelProperty(value = "Unique identifier of the asset assignment")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "Id of the asset that was assigned", required = true)
	public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer assetId) {
		this.assetId = assetId;
	}

	@ApiModelProperty(value = "Id of the order in which the asset was assigned", required = true)
	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	@ApiModelProperty(value = "Id of the order line in which the asset was assigned", required = true)
	public Integer getOrderLineId() {
		return orderLineId;
	}

	public void setOrderLineId(Integer orderLineId) {
		this.orderLineId = orderLineId;
	}

	@ApiModelProperty(value = "Start time of the asset assignment")
	public Date getStartDatetime() {
		return startDatetime;
	}

	public void setStartDatetime(Date startDatetime) {
		this.startDatetime = startDatetime;
	}

	@ApiModelProperty(value = "End time of the asset assignment")
	public Date getEndDatetime() {
		return endDatetime;
	}

	public void setEndDatetime(Date endDatetime) {
		this.endDatetime = endDatetime;
	}

	@Override
	public String toString() {
		return "Asset Assign ID: " + id +
				", Asset ID: " + assetId +
				", Order Line ID: " + orderLineId +
				", Start Date: " + startDatetime +
				", End Date: " + endDatetime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AssetAssignmentWS)) return false;

		AssetAssignmentWS that = (AssetAssignmentWS) o;
		return nullSafeEquals(id, that.id) &&
				nullSafeEquals(assetId, that.assetId) &&
				nullSafeEquals(orderId, that.orderId) &&
				nullSafeEquals(orderLineId, that.orderLineId) &&
				nullSafeEquals(startDatetime, that.startDatetime) &&
				nullSafeEquals(endDatetime, that.endDatetime);
	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(id);
		result = 31 * result + nullSafeHashCode(assetId);
		result = 31 * result + nullSafeHashCode(orderId);
		result = 31 * result + nullSafeHashCode(orderLineId);
		result = 31 * result + nullSafeHashCode(startDatetime);
		result = 31 * result + nullSafeHashCode(endDatetime);
		return result;
	}
}