/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.provisioning.db;

import com.sapienter.jbilling.server.provisioning.ProvisioningRequestStatus;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import org.apache.velocity.util.StringUtils;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@TableGenerator(
        name="provisioning_request_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="provisioning_request",
        allocationSize = 100
)
@Table(name="provisioning_request")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ProvisioningRequestDTO implements Serializable {
    private int id;

    // unique identifier - UUID of the request (different than the DB id)
    private String identifier;
    private ProvisioningCommandDTO commandDTO;
    private String processor;
    private Integer executionOrder;

    private Date createDate;
    private Date submitDate;

    private String submitRequest;
    private String rollbackRequest;
    private String continueOnType;

    private ProvisioningRequestStatus requestStatus;
    private Date resultReceivedDate;
    private Map<String, String> resultMap = new HashMap<String, String>();

    private int versionNum;

    public ProvisioningRequestDTO() {}

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "provisioning_request_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "identifier", nullable = false)
    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    public ProvisioningCommandDTO getProvisioningCommand() {
        return this.commandDTO;
    }

    public void setProvisioningCommand(ProvisioningCommandDTO commandDTO) {
        this.commandDTO = commandDTO;
    }

    @Column(name = "processor", nullable = false)
    public String getProcessor() {
        return this.processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    @Column(name = "execution_order", nullable = false)
    public Integer getExecutionOrder() {
        return this.executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date", nullable = false, length = 29)
    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submit_date", length = 29)
    public Date getSubmitDate() {
        return this.submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "result_received_date", nullable = true, length = 29)
    public Date getResultReceivedDate() {
        return this.resultReceivedDate;
    }

    public void setResultReceivedDate(Date resultReceivedDate) {
        this.resultReceivedDate = resultReceivedDate;
    }

    @Column(name = "submit", nullable = false)
    public String getSubmitRequest() {
        return this.submitRequest;
    }

    public void setSubmitRequest(String submitRequest) {
        this.submitRequest = submitRequest;
    }

    @Column(name = "rollback", nullable = true)
    public String getRollbackRequest() {
        return this.rollbackRequest;
    }

    public void setRollbackRequest(String rollbackRequest) {
        this.rollbackRequest = rollbackRequest;
    }

    @Column(name = "continue_on_type", nullable = true)
    public String getContinueOnType() {
        return continueOnType;
    }

    public void setContinueOnType(String continueOnType) {
        this.continueOnType = continueOnType;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    public ProvisioningRequestStatus getRequestStatus() {
        return this.requestStatus;
    }

    public void setRequestStatus(ProvisioningRequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "provisioning_request_result_map", joinColumns = @JoinColumn(name = "provisioning_request_id"))
    @MapKeyColumn(name="result_parameter_name", nullable = true)
    @Column(name = "result_parameter_value", nullable = true, length = 255)
    @Cascade(value = {org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.SAVE_UPDATE})
    @Fetch(FetchMode.SELECT)
    public Map<String, String> getResultMap() {
        return this.resultMap;
    }

    public void setResultMap(Map<String, String> resultMap) {
        this.resultMap = resultMap;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    public final static class ProvisioningRequestComparator implements Comparator<ProvisioningRequestDTO> {
        public int compare(ProvisioningRequestDTO o1, ProvisioningRequestDTO o2) {
            return new Integer(o1.getExecutionOrder()).compareTo(new Integer(o2.getExecutionOrder()));
        }
    }
}
