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

import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.OrderBy;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.*;

@Entity
@TableGenerator(
        name="provisioning_command_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="provisioning_command",
        allocationSize = 100
)
@Table(name="provisioning_command")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
public abstract class ProvisioningCommandDTO implements Serializable {

    private int id;

    private String name;
    private CompanyDTO entity;
    private Integer executionOrder;
    private Date createDate;
    private Date lastUpdateDate;

    private ProvisioningCommandStatus commandStatus;

    private Map<String, String> parameterMap = new HashMap<String, String>();
    private List<ProvisioningRequestDTO> provisioningRequests = new LinkedList<ProvisioningRequestDTO>();

    private int versionNum;

    public ProvisioningCommandDTO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "provisioning_command_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "name", updatable = true, nullable = false)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name = "execution_order")
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
    @Column(name = "last_update_date", nullable = false, length = 29)
    public Date getLastUpdateDate() {
        return this.lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "command_status", nullable = false)
    public ProvisioningCommandStatus getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(ProvisioningCommandStatus commandStatus) {
        this.commandStatus = commandStatus;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "provisioningCommand")
//    @JoinTable(name = "provisioning_request", joinColumns = @JoinColumn(name = "command_id"), inverseJoinColumns = @JoinColumn(name = "id"))
    @Sort(type = SortType.COMPARATOR, comparator = ProvisioningRequestDTO.ProvisioningRequestComparator.class)
    @Fetch(FetchMode.SELECT)
    @OrderBy(clause="executionOrder")
    public List<ProvisioningRequestDTO> getProvisioningRequests() {
        return this.provisioningRequests;
    }

    public void setProvisioningRequests(List<ProvisioningRequestDTO> provisioningRequests) {
        this.provisioningRequests = provisioningRequests;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "provisioning_command_parameter_map", joinColumns = @JoinColumn(name = "provisioning_command_id"))
    @MapKeyColumn(name="parameter_name", nullable = true)
    @Column(name = "parameter_value", nullable = true, length = 255)
    @Cascade(value = {org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.SAVE_UPDATE})
    @Fetch(FetchMode.SELECT)
    public Map<String, String> getCommandParameters() {
        return this.parameterMap;
    }

    public void setCommandParameters(Map<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Transient
    public abstract ProvisioningCommandType getCommandType();

    @Transient
    public abstract void postCommand(MapMessage message, String eventType) throws JMSException;

    @Override
    public String toString() {
        return "ProvisioningCommandDTO = [ id: " + this.id +
                ", name: " + this.name +
                "] ";
    }

    public final static class ProvisioningCommandComparator implements Comparator<ProvisioningCommandDTO> {
        public int compare(ProvisioningCommandDTO o1, ProvisioningCommandDTO o2) {
            return new Integer(o1.getId()).compareTo(new Integer(o2.getId()));
        }
    }
}

