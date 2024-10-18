package com.sapienter.jbilling.server.usagePool.db;/*
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

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by marcomanzi on 3/5/14.
 */
@Entity
@TableGenerator(
        name="usage_consumption_action_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="usage_pool_consumption_action",
        allocationSize = 100
)
@Table(name = "usage_pool_consumption_actions")
public class UsagePoolConsumptionActionDTO implements Serializable {

    private Integer id;
    private Integer percentage;
    private Integer notificationId;
    private String mediumType;
    private String type;
    private Integer productId;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "usage_consumption_action_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "percentage", unique = false, nullable = false)
    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    @Column(name = "notification_id", nullable = false)
    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    @Column(name = "medium_type", nullable = false)
    public String getMediumType() {
        return mediumType;
    }

    public void setMediumType(String mediumType) {
        this.mediumType = mediumType;
    }

    @Column(name = "product_id",nullable = true)
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer product_id) {
        this.productId = product_id;
    }

    @Column(name = "type", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
