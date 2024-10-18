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
package com.sapienter.jbilling.server.user.db;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 *
 * @author Leandro Zoi
 * @since 01/51/18
 *
 */

@Entity
@TableGenerator(
        name            = "customer_status_change_history_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "customer_status_change_history",
        allocationSize  = 100
)

@Table(name="customer_status_change_history")
public class CustomerStatusChangeHistoryDTO implements Serializable {

    private int id;
    private UserDTO baseUser;
    private String collectionsStepStatus;
    private CustomerStatusType currentStatus;
    private Date modifiedAt;
    private String modifiedBy;

    /**
     * Constructor by default
     */
    public CustomerStatusChangeHistoryDTO() { }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_status_change_history_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserDTO getBaseUser() {
        return baseUser;
    }

    public void setBaseUser(UserDTO baseUser) {
        this.baseUser = baseUser;
    }

    @Column(name = "collections_step_status", nullable = false)
    public String getCollectionsStepStatus() {
        return collectionsStepStatus;
    }

    public void setCollectionsStepStatus(String collectionsStepStatus) {
        this.collectionsStepStatus = collectionsStepStatus;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    public CustomerStatusType getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(CustomerStatusType currentStatus) {
        this.currentStatus = currentStatus;
    }

    @Column(name = "modified_at", nullable = false)
    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    @Column(name = "modified_by", nullable = false)
    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public String toString() {
        return "CustomerStatusChangeHistoryDTO{" +
                "id=" + id +
                ", baseUser=" + baseUser +
                ", collectionsStepStatus='" + collectionsStepStatus + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", modifiedAt=" + modifiedAt +
                ", modifiedBy='" + modifiedBy + '\'' +
                '}';
    }
}


