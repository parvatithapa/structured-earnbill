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
package com.sapienter.jbilling.server.process.db;

import java.io.Serializable;

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
import javax.persistence.Version;


import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@TableGenerator(name = "ageing_entity_step_GEN",
               table = "jbilling_seqs",
               pkColumnName = "name",
               valueColumnName = "next_id",
               pkColumnValue = "ageing_entity_step",
               allocationSize = 100)
@Table(name = "ageing_entity_step")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class AgeingEntityStepDTO extends AbstractDescription implements Serializable {

    private int id;
    private CompanyDTO company;
    private UserStatusDTO userStatus;
    private int days;
    private int retryPayment;
    private int suspend;
    private int sendNotification;
    private CollectionType collectionType;

	private int versionNum;
	private int stopActivationOnPayment;

	public AgeingEntityStepDTO() {
    }

    public AgeingEntityStepDTO(int id, int days) {
        this.id = id;
        this.days = days;
    }

    public AgeingEntityStepDTO(int id, CompanyDTO entity,
            UserStatusDTO userStatus, int days) {
        this.id = id;
        this.company = entity;
        this.userStatus = userStatus;
        this.days = days;
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_AGEING_ENTITY_STEP;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ageing_entity_step_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return this.company;
    }

    public void setCompany(CompanyDTO entity) {
        this.company = entity;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    public UserStatusDTO getUserStatus() {
        return this.userStatus;
    }

    public void setUserStatus(UserStatusDTO userStatus) {
        this.userStatus = userStatus;
    }

    @Column(name = "days", nullable = false)
    public int getDays() {
        return this.days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    @Column(name = "suspend", nullable = false)
    public int getSuspend() {
        return this.suspend;
    }

    public void setSuspend(int suspend) {
        this.suspend = suspend;
    }

    @Column(name = "retry_payment", nullable = false)
    public int getRetryPayment() {
        return retryPayment;
    }

    public void setRetryPayment(int retryPayment) {
        this.retryPayment = retryPayment;
    }

    @Column(name = "send_notification", nullable = false)
    public int getSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(int sendNotification) {
        this.sendNotification = sendNotification;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Column(name = "stop_activation_on_payment", nullable = false)
    public int getStopActivationOnPayment() {
		return stopActivationOnPayment;
	}

	public void setStopActivationOnPayment(int stopActivationOnPayment) {
		this.stopActivationOnPayment = stopActivationOnPayment;
	}

	@Column(name = "collection_type", nullable = false)
	@Enumerated(EnumType.STRING)
	public CollectionType getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(CollectionType collectionType) {
		this.collectionType = collectionType;
	}
    
    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getCompany().getId())
                .append("-")
                .append(id);

        return key.toString();
    }
}
