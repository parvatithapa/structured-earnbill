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

package com.sapienter.jbilling.server.process;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;


/**
 * AgeingWS
 * @author Vikas Bodani
 */
@ApiModel(value = "AgeingWS", description = "Ageing model")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgeingWS implements WSSecured, Serializable {
	
	@NotNull(message="validation.error.notnull", groups = {UpdateValidationGroup.class})
	private Integer statusId = null;
    @NotEmpty(message="validation.error.notnull")
    private String statusStr = null;
    private Boolean inUse = null;
    private Boolean suspended;
    private Boolean paymentRetry;
    private Boolean sendNotification;
    @NotNull(message="validation.error.notnull")
    private Integer days;
    private Integer entityId;
    private Boolean stopActivationOnPayment;
    private CollectionType collectionType;
    
    //default constructor
    public AgeingWS(){}

    @ApiModelProperty(value = "Unique identifier of the ageing process status")
	public Integer getStatusId() {
		return statusId;
	}
	public void setStatusId(Integer statusId) {
		this.statusId = statusId;
	}

    @ApiModelProperty(value = "String description for this step")
	public String getStatusStr() {
		return statusStr;
	}
	public void setStatusStr(String statusStr) {
		this.statusStr = statusStr;
	}

    @ApiModelProperty(value = "Enabled/Disabled flag. Indicates if this ageing step is in use")
    public Boolean getInUse() {
        return inUse;
    }

    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }

    @ApiModelProperty(value = "Flag that is set when this step will suspend the customer")
    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    @ApiModelProperty(value = "Indicates if a payment should be attempted when a customer enters this step. True for attempt payment, false for do not attempt payment")
    public Boolean getPaymentRetry() {
        return paymentRetry;
    }

    public void setPaymentRetry(Boolean paymentRetry) {
        this.paymentRetry = paymentRetry;
    }

    @ApiModelProperty(value = "Indicates if a notification should be sent when a customer enters this step. True for send notification, false for do not send notification")
    public Boolean getSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    @ApiModelProperty(value = "Field that indicates how many days after the due date this step becomes active. This field is mandatory")
    public Integer getDays() {
		return days;
	}
	public void setDays(Integer days) {
		this.days = days;
	}

    @ApiModelProperty(value = "Current company ID")
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

    @Override
	public Integer getOwningEntityId() {
        return getEntityId();
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    @Override
    public Integer getOwningUserId() {
        return null;
    }

    @ApiModelProperty(value = "Stop activation on payment checked")
	public Boolean getStopActivationOnPayment() {
		return stopActivationOnPayment;
	}

	public void setStopActivationOnPayment(Boolean stopActivationOnPayment) {
		this.stopActivationOnPayment = stopActivationOnPayment;
	}
	
	public CollectionType getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	public String toString() {
		return "AgeingWS [statusId=" + statusId
                + ", statusStr=" + statusStr
				+ ", suspended=" + suspended
                + ", days=" + days
                + ", collectionType=" + collectionType +"]";
	}

}
