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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@TableGenerator(name = "process_run_GEN", table = "jbilling_seqs", pkColumnName = "name", valueColumnName = "next_id", pkColumnValue = "billing_process_info", allocationSize = 1)
@Table(name = "billing_process_info")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BillingProcessInfoDTO implements Serializable {

    private int id;
    private BillingProcessDTO billingProcessDTO;
    private Integer jobExecutionId;
    private Integer totalFailedUsers;
    private Integer totalSuccessfulUsers;
    private Set<BillingProcessFailedUserDTO> processes = new HashSet<>(0);
    private int versionNum;

    BillingProcessInfoDTO() {
    }

    public BillingProcessInfoDTO(BillingProcessDTO billingProcessDTO, Integer jobExecutionId, Integer totalFailedUsers,
            Integer totalSuccessfulUsers) {
        this.billingProcessDTO = billingProcessDTO;
        this.jobExecutionId = jobExecutionId;
        this.totalFailedUsers = totalFailedUsers;
        this.totalSuccessfulUsers = totalSuccessfulUsers;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "process_run_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId () {
        return this.id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id")
    public BillingProcessDTO getBillingProcess () {
        return this.billingProcessDTO;
    }

    public void setBillingProcess (BillingProcessDTO billingProcessDTO) {
        this.billingProcessDTO = billingProcessDTO;
    }

    @Column(name = "job_execution_id")
    public Integer getJobExecutionId () {
        return jobExecutionId;
    }

    public void setJobExecutionId (Integer jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    @Column(name = "total_failed_users", nullable = false)
    public Integer getTotalFailedUsers () {
        return totalFailedUsers;
    }

    public void setTotalFailedUsers (Integer totalFailedUsers) {
        this.totalFailedUsers = totalFailedUsers;
    }

    @Column(name = "total_successful_users", nullable = false)
    public Integer getTotalSuccessfulUsers () {
        return totalSuccessfulUsers;
    }

    public void setTotalSuccessfulUsers (Integer totalSuccessfulUsers) {
        this.totalSuccessfulUsers = totalSuccessfulUsers;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "batchProcess")
    public Set<BillingProcessFailedUserDTO> getProcesses () {
        return this.processes;
    }

    public void setProcesses (Set<BillingProcessFailedUserDTO> processes) {
        this.processes = processes;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum () {
        return versionNum;
    }

    public void setVersionNum (int versionNum) {
        this.versionNum = versionNum;
    }

    public String toString () {
        return "BillingProcessInfoDTO: id: " + id + " billingProcess: " + billingProcessDTO.getId()
                + " totalFailedUsers: " + totalFailedUsers;
    }
}
