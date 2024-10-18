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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.user.db.UserDTO;

@Entity
@TableGenerator(
        name = "process_run_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "billing_process_failed_user", 
        allocationSize = 100)
@Table(name = "billing_process_failed_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BillingProcessFailedUserDTO implements Serializable {

    private int id;
    private BillingProcessInfoDTO batchProcessDTO;
    private UserDTO userDTO;
    private int versionNum;

    public BillingProcessFailedUserDTO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "process_run_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_process_id")
    public BillingProcessInfoDTO getBatchProcess() {
        return this.batchProcessDTO;
    }

    public void setBatchProcess(BillingProcessInfoDTO batchProcessDTO) {
        this.batchProcessDTO = batchProcessDTO;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
		return userDTO;
	}

	public void setUser(UserDTO user) {
		this.userDTO = user;
	}

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    public String toString() {
        StringBuffer ret = new StringBuffer(" BatchProcessInfoDTO: id: " + id + " batchProcess: " + batchProcessDTO.getId() + " user: " + userDTO.getId());

        return ret.toString();
    }
}
