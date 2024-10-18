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
package com.sapienter.jbilling.server.customerEnrollment.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.csv.Exportable;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@TableGenerator(
        name="customer_enrollment_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="customer_enrollment",
        allocationSize = 100
)
// No cache, mutable and critical
@Table(name="customer_enrollment")
public class CustomerEnrollmentDTO extends CustomizedEntity implements Serializable, Exportable {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CustomerEnrollmentDTO.class));

    private int id;
    private int versionNum;
    private AccountTypeDTO  accountType;
    private CompanyDTO company;
    private UserDTO user; //User Id of customer created by customer enrollment

    private int deleted = 0;
    private UserDTO parentCustomer;
    private CustomerEnrollmentDTO parentEnrollment;
    // This is a enum class. Details are mentioned in next point.
    private CustomerEnrollmentStatus status = CustomerEnrollmentStatus.PENDING;
    private Set<CustomerEnrollmentCommentDTO> comments = new HashSet<CustomerEnrollmentCommentDTO>();
    private Set<CustomerEnrollmentAgentDTO> agents = new HashSet<>();

    private Date createDatetime;
    private String message;
    private Boolean bulkEnrollment;

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_enrollment_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Version
    @Column(name="OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    public AccountTypeDTO getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountTypeDTO accountType) {
        this.accountType = accountType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false)
    public CustomerEnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerEnrollmentStatus status) {
        this.status = status;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "customerEnrollment", orphanRemoval = true)
    public Set<CustomerEnrollmentCommentDTO> getComments() {
        return comments;
    }

    public void setComments(Set<CustomerEnrollmentCommentDTO> comments) {
        this.comments = comments;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "enrollment", orphanRemoval = true)
    public Set<CustomerEnrollmentAgentDTO> getAgents() {
        return agents;
    }

    public void setAgents(Set<CustomerEnrollmentAgentDTO> agents) {
        this.agents = agents;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_customer_id")
    public UserDTO getParentCustomer() {
        return parentCustomer;
    }

    public void setParentCustomer(UserDTO parentCustomer) {
        this.parentCustomer = parentCustomer;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_enrollment_id")
    public CustomerEnrollmentDTO getParentEnrollment() {
        return parentEnrollment;
    }

    public void setParentEnrollment(CustomerEnrollmentDTO parentEnrollment) {
        this.parentEnrollment = parentEnrollment;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "customer_enrollment_meta_field_map",
            joinColumns = @JoinColumn(name = "customer_enrollment_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ACCOUNT_TYPE, EntityType.ENROLLMENT };
    }

    @Column(name = "deleted", nullable = false)
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Transient
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name = "bulk_enrollment", nullable = false)
    public Boolean isBulkEnrollment() {
        return bulkEnrollment;
    }

    public void setBulkEnrollment(Boolean bulkEnrollment) {
        this.bulkEnrollment = bulkEnrollment;
    }

    @Override
    public String toString() {
        return "CustomerEnrollmentDTO{" +
                "id=" + id +
                ", versionNum=" + versionNum +
                ", accountType=" + accountType +
                ", company=" + company +
                ", status=" + status +
                ", createDatetime=" + createDatetime +
                '}';
    }

    @Transient
    public String[] getFieldNames() {
        return new String[] {
                "id",
                "accountType",
                "company",
                "status",
                "comments"
                      };
    }

    @Transient
    public Object[][] getFieldValues() {

        List<Object[]> values = new ArrayList<Object[]>();

        // main invoice row
        values.add(
                new Object[] {
                        id,
                        getComments(),
                        getAccountType(),
                        getStatus(),
                        getCompany()
                }
        );
        return values.toArray(new Object[values.size()][]);
    }
}


