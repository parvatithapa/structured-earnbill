package com.sapienter.jbilling.server.usagePool;/*
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


import com.sapienter.jbilling.server.item.validator.NonNegative;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

import javax.validation.Validator;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by marcomanzi on 3/5/14.
 */
public class UsagePoolConsumptionActionWS implements Serializable {

    protected String id;
    @NonNegative(message = "validation.error.nonnegative")
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class} )
    protected String percentage;
    protected String type;
    private String notificationId;
    private NotificationMediumType mediumType;
    private String productId;

    public UsagePoolConsumptionActionWS() {}

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationMediumType getMediumType() {
        return mediumType;
    }

    public void setMediumType(NotificationMediumType mediumType) {
        this.mediumType = mediumType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "UsagePoolConsumptionActionWS={id=" + id +"" +
                ", percentage=" + percentage + "}" +
                ", notificationId=" + notificationId + "}" +
                ", mediumType=" + mediumType +
                ", productId=" + productId + "}";
    }

}
