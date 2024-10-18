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
package com.sapienter.jbilling.server.user.partner.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.util.csv.Exportable;

@SuppressWarnings("serial")
@NamedQueries({
        @NamedQuery(name = "PartnerDTO.findForBroker",
                query = "select partner from PartnerDTO as partner " +
                        "where partner.brokerId = :brokerId " +
                        "and partner.baseUser.company.id = :entityId")
})
@Entity
@TableGenerator(
        name = "partner_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "partner",
        allocationSize = 10
)
@Table(name = "partner")
public class PartnerDTO extends CustomizedEntity implements java.io.Serializable, Exportable {
    private int id;

    private UserDTO baseUserByUserId;
    private BigDecimal totalPayments;
    private BigDecimal totalRefunds;
    private BigDecimal totalPayouts;
    private BigDecimal duePayout;
    private PartnerType type;
    private PartnerDTO parent;
    private Set<PartnerDTO> children = new HashSet<PartnerDTO>(0);
    private List<CommissionDTO> commissions;
    private List<PartnerCommissionExceptionDTO> commissionExceptions;
    private List<PartnerReferralCommissionDTO> referralCommissions;
    private List<PartnerReferralCommissionDTO> referrerCommissions;
    private List<PartnerCommissionValueDTO> commissionValues;
    private PartnerCommissionType commissionType;
    private String brokerId;

    private Set<PartnerPayout> partnerPayouts = new HashSet<PartnerPayout>(0);
    private Set<CustomerDTO> customers = new HashSet<CustomerDTO>(0);
    private Set<CustomerCommissionDefinitionDTO> customerCommissionDefinitions = new HashSet<>(0);
    private int versionNum;
    private int deleted;

    public PartnerDTO() {
        super();
        this.commissions = new ArrayList<CommissionDTO>();
        this.commissionExceptions = new ArrayList<PartnerCommissionExceptionDTO>();
        this.referralCommissions = new ArrayList<PartnerReferralCommissionDTO>();
        this.referrerCommissions = new ArrayList<PartnerReferralCommissionDTO>();
    }

    public PartnerDTO(int id) {
        this();
        this.id = id;
    }

    public PartnerDTO(int id, BigDecimal totalPayments, BigDecimal totalRefunds, BigDecimal totalPayouts) {
        this(id);
        this.totalPayments = totalPayments;
        this.totalRefunds = totalRefunds;
        this.totalPayouts = totalPayouts;
    }

    public PartnerDTO(int id, UserDTO baseUserByUserId, BigDecimal totalPayments, BigDecimal totalRefunds,
                      BigDecimal totalPayouts, BigDecimal duePayout, Set<PartnerPayout> partnerPayouts,
                      Set<CustomerDTO> customers) {
        this(id);
        this.baseUserByUserId = baseUserByUserId;
        this.totalPayments = totalPayments;
        this.totalRefunds = totalRefunds;
        this.totalPayouts = totalPayouts;
        this.duePayout = duePayout;
        this.partnerPayouts = partnerPayouts;
        this.customers = customers;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "partner_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getBaseUser() {
        return this.baseUserByUserId;
    }

    public void setBaseUser(UserDTO baseUserByUserId) {
        this.baseUserByUserId = baseUserByUserId;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "id.partner")
    public Set<CustomerCommissionDefinitionDTO> getCustomerCommissionDefinition() {
        return customerCommissionDefinitions;
    }

    public void setCustomerCommissionDefinition(Set<CustomerCommissionDefinitionDTO> customerCommissionDefinitions) {
        this.customerCommissionDefinitions = customerCommissionDefinitions;
    }

    @Column(name = "total_payments", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalPayments() {
        return this.totalPayments;
    }

    public void setTotalPayments(BigDecimal totalPayments) {
        this.totalPayments = totalPayments;
    }

    @Column(name = "total_refunds", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalRefunds() {
        return this.totalRefunds;
    }

    public void setTotalRefunds(BigDecimal totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    @Column(name = "total_payouts", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalPayouts() {
        return this.totalPayouts;
    }

    public void setTotalPayouts(BigDecimal totalPayouts) {
        this.totalPayouts = totalPayouts;
    }

    @Column(name = "due_payout", precision = 17, scale = 17)
    public BigDecimal getDuePayout() {
        return this.duePayout;
    }

    public void setDuePayout(BigDecimal duePayout) {
        this.duePayout = duePayout;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "partner")
    public Set<PartnerPayout> getPartnerPayouts() {
        return this.partnerPayouts;
    }

    public void setPartnerPayouts(Set<PartnerPayout> partnerPayouts) {
        this.partnerPayouts = partnerPayouts;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "customer_partner_map", joinColumns = {
            @JoinColumn(name = "partner_id", updatable = true) }, inverseJoinColumns = {
            @JoinColumn(name = "customer_id", updatable = true) })
    public Set<CustomerDTO> getCustomers() {
        return this.customers;
    }

    public void setCustomers(Set<CustomerDTO> customers) {
        this.customers = customers;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "partner_meta_field_map",
            joinColumns = @JoinColumn(name = "partner_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    public PartnerType getType () {
        return type;
    }

    public void setType (PartnerType type) {
        this.type = type;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public PartnerDTO getParent() {
        return this.parent;
    }

    public void setParent(PartnerDTO parent) {
        this.parent = parent;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    public Set<PartnerDTO> getChildren() {
        return children;
    }

    public void setChildren(Set<PartnerDTO> children) {
        this.children = children;
    }

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.AGENT };
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="partner", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<CommissionDTO> getCommissions() {
        return commissions;
    }

    public void setCommissions (List<CommissionDTO> commissions) {
        this.commissions = commissions;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="partner", orphanRemoval=true)
    @OrderBy(clause="days")
    public List<PartnerCommissionValueDTO> getCommissionValues() {
        if(commissionValues == null) {
            commissionValues = new ArrayList<>();
        }
        return commissionValues;
    }

    public void setCommissionValues(List<PartnerCommissionValueDTO> commissionValues) {
        this.commissionValues = commissionValues;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="partner", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<PartnerCommissionExceptionDTO> getCommissionExceptions () {
        return commissionExceptions;
    }

    public void setCommissionExceptions (List<PartnerCommissionExceptionDTO> commissionExceptions) {
        this.commissionExceptions = commissionExceptions;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="referral", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<PartnerReferralCommissionDTO> getReferralCommissions () {
        return referralCommissions;
    }

    public void setReferralCommissions (List<PartnerReferralCommissionDTO> referralCommissions) {
        this.referralCommissions = referralCommissions;
    }
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="referrer", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<PartnerReferralCommissionDTO> getReferrerCommissions () {
        return referrerCommissions;
    }

    public void setReferrerCommissions (List<PartnerReferralCommissionDTO> referrerCommissions) {
        this.referrerCommissions = referrerCommissions;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = true)
    public PartnerCommissionType getCommissionType () {
        return commissionType;
    }

    public void setCommissionType (PartnerCommissionType commissionType) {
        this.commissionType = commissionType;
    }

    @Column(name = "broker_id")
    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    @Column(name = "deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Version
    @Column(name = "OPTLOCK")

    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        return "PartnerDTO{" +
                "id=" + id +
                ", baseUserByUserId=" + (baseUserByUserId != null ? baseUserByUserId.getId() : "") +
                ", totalPayments=" + totalPayments +
                ", totalRefunds=" + totalRefunds +
                ", totalPayouts=" + totalPayouts +
                ", duePayout=" + duePayout +
                ", type=" + type +
                ", parent=" + (parent != null ? parent.getId() : "") +
                ", commissions=" + commissions +
                ", commissionExceptions=" + commissionExceptions +
                ", commissionValues=" + commissionValues +
                ", commissionType=" + commissionType +
                ", brokerId='" + brokerId + '\'' +
                ", versionNum=" + versionNum +
                '}';
    }

    /*
         * Inherited from DTOEx
         */
    @Transient
    public UserDTO getUser() {
        return getBaseUser();
    }

    public void touch() {
        for (PartnerPayout payout : getPartnerPayouts()) {
            payout.touch();
        }
    }

    @Transient
    @Override
    public String[] getFieldNames() {
        return new String[] {
                "id",
                "userName",
                "firstName",
                "lastName",
                "totalPayments",
                "totalRefunds",
                "totalPayouts",
                "type",
                "parentUserName",
        };    }

    @Transient
    @Override
    public Object[][] getFieldValues() {
        List<Object[]> values = new ArrayList<Object[]>();
        values.add(
                new Object[] {
                        id,
                        (getBaseUser().getUserName() != null ? getBaseUser().getUserName() : null),
                        (getBaseUser().getContact() != null ? getBaseUser().getContact().getFirstName() : null),
                        (getBaseUser().getContact() != null ? getBaseUser().getContact().getLastName() : null),
                        totalPayments,
                        totalRefunds,
                        totalPayouts,
                        type,
                        (getParent() != null ? getParent().getBaseUser().getUserName() : null)
                }
        );
        return values.toArray(new Object[values.size()][]);
    }
}
