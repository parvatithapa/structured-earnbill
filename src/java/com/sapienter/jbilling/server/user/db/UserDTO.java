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

import java.io.Closeable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SortComparator;

import com.sapienter.jbilling.server.audit.Auditable;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.db.NotificationMessageArchDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessFailedUserDTO;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionUserDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
		name            = "base_user_GEN",
		table           = "jbilling_seqs",
		pkColumnName    = "name",
		valueColumnName = "next_id",
		pkColumnValue   = "base_user",
		allocationSize  = 10
		)
// No cache, mutable and critical
@Table(name = "base_user")
@DynamicUpdate(true) //Only update modified column values to database
public class UserDTO extends CustomizedEntity implements Serializable, Exportable, Auditable, AutoCloseable {

	private int id;
	private String userName;
	private String password;
	private int deleted;
	boolean enabled = true;
	boolean accountExpired = false;
	boolean accountLocked = false;
	boolean passwordExpired = false;

	private Date createDatetime;
	private Date lastStatusChange;
	private Date lastLogin;
	private Date accountDisabledDate;
	private int failedAttempts;
	private Date changePasswordDate;

	private Set<RoleDTO> roles = new HashSet<>();
	private Set<PermissionUserDTO> permissions = new HashSet<>();

	private CurrencyDTO currencyDTO;
	private CompanyDTO company;
	private SubscriberStatusDTO subscriberStatus;
	private UserStatusDTO userStatus;
	private LanguageDTO language;
	private CustomerDTO customer;
	private ContactDTO contact;
	private PartnerDTO partnersForUserId;
	private Integer encryptionScheme;
	private int versionNum;

	private Set<PaymentDTO> payments = new HashSet<>();
	private Set<OrderDTO> purchaseOrdersForCreatedBy = new HashSet<>();
	private Set<OrderDTO> orders = new HashSet<>();
	private Set<NotificationMessageArchDTO> notificationMessageArchs = new HashSet<>();
	private Set<EventLogDTO> eventLogs = new HashSet<>();
	private Set<InvoiceDTO> invoices = new HashSet<>();
	private Set<CustomerPriceDTO> prices = new HashSet<>();
	private Set<CustomerCommissionDefinitionDTO> commissionDefinitions = new HashSet<>();

	private Set<BillingProcessFailedUserDTO> processes = new HashSet<>();

	// payment instruments
	private List<PaymentInformationDTO> paymentInstruments = new ArrayList<>();

	private Date accountLockedTime;

	private UserExportableWrapper userExportableWrapper;

    public UserDTO() {
    }

    public UserDTO(int id) {
        this.id = id;
    }

    public UserDTO(int id, short deleted, Date createDatetime, int failedAttempts) {
        this.id = id;
        this.deleted = deleted;
        this.createDatetime = createDatetime;
        this.failedAttempts = failedAttempts;
    }

    public UserDTO(int id, CurrencyDTO currencyDTO, CompanyDTO entity, SubscriberStatusDTO subscriberStatus,
            UserStatusDTO userStatus, LanguageDTO language, String password, short deleted, Date createDatetime,
            Date lastStatusChange, Date lastLogin, String userName, int failedAttempts, Set<PaymentDTO> payments,
            Set<PermissionUserDTO> permissionUsers,
            CustomerDTO customer, PartnerDTO partnersForUserId,
            Set<OrderDTO> purchaseOrdersForCreatedBy, Set<OrderDTO> purchaseOrdersForUserId,
            Set<NotificationMessageArchDTO> notificationMessageArchs, Set<RoleDTO> roles,
            Set<EventLogDTO> eventLogs, Set<InvoiceDTO> invoices) {
        this.id = id;
        this.currencyDTO = currencyDTO;
        this.company = entity;
        this.subscriberStatus = subscriberStatus;
        this.userStatus = userStatus;
        this.language = language;
        this.password = password;
        this.deleted = deleted;
        this.createDatetime = createDatetime;
        this.lastStatusChange = lastStatusChange;
        this.lastLogin = lastLogin;
        this.userName = userName;
        this.failedAttempts = failedAttempts;
        this.payments = payments;
        this.permissions = permissionUsers;
        this.customer = customer;
        this.partnersForUserId = partnersForUserId;
        this.purchaseOrdersForCreatedBy = purchaseOrdersForCreatedBy;
        this.orders = purchaseOrdersForUserId;
        this.notificationMessageArchs = notificationMessageArchs;
        this.roles = roles;
        this.eventLogs = eventLogs;
        this.invoices = invoices;
    }

    public UserDTO(UserDTO another) {
        setId(another.getId());
        setCurrency(another.getCurrency());
        setCompany(another.getCompany());
        setSubscriberStatus(another.getSubscriberStatus());
        setUserStatus(another.getUserStatus());
        setLanguage(another.getLanguage());
        setPassword(another.getPassword());
        setDeleted(another.getDeleted());
        setCreateDatetime(another.getCreateDatetime());
        setLastStatusChange(another.getLastStatusChange());
        setLastLogin(another.getLastLogin());
        setUserName(another.getUserName());
        setFailedAttempts(another.getFailedAttempts());
        setCustomer(another.getCustomer());
        setPartner(another.getPartner());
        setPayments(another.getPayments());
        setPermissions(another.getPermissions());
        setPurchaseOrdersForCreatedBy(another.getPurchaseOrdersForCreatedBy());
        setOrders(another.getOrders());
        setNotificationMessageArchs(another.getNotificationMessageArchs());
        setRoles(another.getRoles());
        setEventLogs(another.getEventLogs());
        setInvoices(another.getInvoices());
        setPaymentInstruments(another.getPaymentInstruments());
        setMetaFields(new ArrayList<>(another.getMetaFields()));
    }


    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "base_user_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Transient
    public Integer getUserId() {
        return id;
    }

    @Transient
    public boolean isSuspended() {
        return getUserStatus().getAgeingEntityStep() != null &&
               getUserStatus().getAgeingEntityStep().getSuspend() == 1;
    }

    @Column(name = "user_name", length = 50)
    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "password", length = 1024)
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns 1 if this user is deleted, 0 if they are active.
     * @return is user deleted
     */
    @Column(name="change_password_date")
    public Date getChangePasswordDate() {
    	return this.changePasswordDate;
    }

    public void setChangePasswordDate(Date changePasswordDate) {
    	this.changePasswordDate=changePasswordDate;
    }
    @Column(name = "deleted", nullable = false)
    public int getDeleted() {
        return this.deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    /**
     * Returns true if this user is enabled and not deleted.
     *
     * todo: enabled flag is transient, field currently only exists for Spring Security integration
     *
     * @return true if user enabled
     */
    @Transient
    public boolean isEnabled() {
        return enabled && deleted == 0;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns true if this user's account is expired.
     *
     * todo: expired flag is transient, field currently only exists for Spring Security integration
     *
     * @return true if user expired
     */
    @Transient
    public boolean isAccountExpired() {
        return accountExpired;
    }

    public void setAccountExpired(boolean accountExpired) {
        this.accountExpired = accountExpired;
    }

    /**
     * Returns true if this user's account has been locked, either by a system administrator
     * or by too many failed log-in attempts.
     *
     * todo: locked flag is transient, field currently only exists for Spring Security integration
     *
     * @return true if user is locked
     */
    @Transient
    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    /**
     * Returns true if the users password has expired.
     *
     * todo: expired flag is transient, field currently only exists for Spring Security integration
     *
     * @return true if password has expired
     */
    @Transient
    public boolean isPasswordExpired() {
        return passwordExpired;
    }

    public void setPasswordExpired(boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Column(name = "last_status_change", length = 29)
    public Date getLastStatusChange() {
        return this.lastStatusChange;
    }

    public void setLastStatusChange(Date lastStatusChange) {
        this.lastStatusChange = lastStatusChange;
    }

    @Column(name = "last_login", length = 29)
    public Date getLastLogin() {
        return this.lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Column(name = "account_disabled_date", length = 29, nullable = true)
    public Date getAccountDisabledDate() {
        return this.accountDisabledDate;
    }

    public void setAccountDisabledDate(Date accountDisabledDate) {
        this.accountDisabledDate = accountDisabledDate;
    }

    @Column(name = "failed_attempts", nullable = false)
    public int getFailedAttempts() {
        return this.failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    @Column(name = "encryption_scheme", nullable=false)
    public Integer getEncryptionScheme(){
    	return this.encryptionScheme;
    }

    public void setEncryptionScheme(Integer scheme){
    	this.encryptionScheme = scheme;
    }

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "user_role_map",
               joinColumns = {@JoinColumn(name = "user_id", updatable = false)},
               inverseJoinColumns = {@JoinColumn(name = "role_id", updatable = false)})
    public Set<RoleDTO> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<RoleDTO> roles) {
        this.roles = roles;
    }

    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUser")
    public Set<PermissionUserDTO> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<PermissionUserDTO> permissionUsers) {
        this.permissions = permissionUsers;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    public CurrencyDTO getCurrency() {
        return this.currencyDTO;
    }

    public void setCurrency(CurrencyDTO currencyDTO) {
        this.currencyDTO = currencyDTO;
    }

    @Transient
    public Integer getCurrencyId() {
        if (getCurrency() == null) {
            return null;
        }
        return getCurrency().getId();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return this.company;
    }

    public void setCompany(CompanyDTO entity) {
        this.company = entity;
    }

    @Transient
    public CompanyDTO getEntity() {
        return getCompany();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_status")
    public SubscriberStatusDTO getSubscriberStatus() {
        return this.subscriberStatus;
    }

    public void setSubscriberStatus(SubscriberStatusDTO subscriberStatus) {
        this.subscriberStatus = subscriberStatus;
    }

    @ManyToOne
    @JoinColumn(name = "status_id")
    public UserStatusDTO getUserStatus() {
        return this.userStatus;
    }

    public void setUserStatus(UserStatusDTO userStatus) {
        this.userStatus = userStatus;
    }

    @Transient
    public UserStatusDTO getStatus() {
        return getUserStatus();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    public LanguageDTO getLanguage() {
        return this.language;
    }

    public void setLanguage(LanguageDTO language) {
        this.language = language;
    }

    @Transient
    public Integer getLanguageIdField() {
        if (getLanguage() == null) {
            return getEntity().getLanguageId();
        }

        return getLanguage().getId();
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUser")
    public Set<PaymentDTO> getPayments() {
        return this.payments;
    }

    public void setPayments(Set<PaymentDTO> payments) {
        this.payments = payments;
    }

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUser")
    public CustomerDTO getCustomer() {
        return this.customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    /**
     * Convenience mapping for the users primary contact. This association is read-only and
     * will not persist or update the users stored contact. use {@link ContactBL} instead.
     *
     * @return users primary contact
     */
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "baseUser")
    public ContactDTO getContact() {
        return contact;
    }

    public void setContact(ContactDTO contact) {
        this.contact = contact;
    }

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUser")
    public PartnerDTO getPartner() {
        return this.partnersForUserId;
    }

    public void setPartner(PartnerDTO partnersForUserId) {
        this.partnersForUserId = partnersForUserId;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUserByCreatedBy")
    public Set<OrderDTO> getPurchaseOrdersForCreatedBy() {
        return this.purchaseOrdersForCreatedBy;
    }

    public void setPurchaseOrdersForCreatedBy(Set<OrderDTO> purchaseOrdersForCreatedBy) {
        this.purchaseOrdersForCreatedBy = purchaseOrdersForCreatedBy;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUserByUserId")
    public Set<OrderDTO> getOrders() {
        return this.orders;
    }

    public void setOrders(Set<OrderDTO> purchaseOrdersForUserId) {
        this.orders = purchaseOrdersForUserId;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "baseUser")
    public Set<NotificationMessageArchDTO> getNotificationMessageArchs() {
        return this.notificationMessageArchs;
    }

    public void setNotificationMessageArchs(Set<NotificationMessageArchDTO> notificationMessageArchs) {
        this.notificationMessageArchs = notificationMessageArchs;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "baseUser")
    public Set<EventLogDTO> getEventLogs() {
        return this.eventLogs;
    }

    public void setEventLogs(Set<EventLogDTO> eventLogs) {
        this.eventLogs = eventLogs;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "baseUser")
    public Set<InvoiceDTO> getInvoices() {
        return this.invoices;
    }

    public void setInvoices(Set<InvoiceDTO> invoices) {
        this.invoices = invoices;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "id.baseUser")
    public Set<CustomerPriceDTO> getPrices() {
        return prices;
    }

    public void setPrices(Set<CustomerPriceDTO> prices) {
        this.prices = prices;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "id.user", orphanRemoval = true)
    public Set<CustomerCommissionDefinitionDTO> getCommissionDefinitions() {
        return commissionDefinitions;
    }

    public void setCommissionDefinitions(Set<CustomerCommissionDefinitionDTO> commissionDefinitions) {
        this.commissionDefinitions = commissionDefinitions;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
    public Set<BillingProcessFailedUserDTO> getProcesses() {
        return this.processes;
    }

    public void setProcesses(Set<BillingProcessFailedUserDTO> processes) {
        this.processes = processes;
    }

    @Version
    @Column(name = "OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @OrderBy("processingOrder")
    public List<PaymentInformationDTO> getPaymentInstruments() {
		return paymentInstruments;
	}

	public void setPaymentInstruments(List<PaymentInformationDTO> paymentInstruments) {
		this.paymentInstruments = paymentInstruments;
	}

	@Column(name="account_locked_time", length = 29)
	public Date getAccountLockedTime() {
		return accountLockedTime;
	}

	public void setAccountLockedTime(Date accountLockedTime) {
		this.accountLockedTime = accountLockedTime;
	}

	@Override
    @Transient
	public String[] getFieldNames() {
		return getUserExportableWrapper().getFieldNames();
	}

	@Override
    @Transient
	public Object[][] getFieldValues() {
		return getUserExportableWrapper().getFieldValues();
	}

	@Override
    public String getAuditKey(Serializable id) {
		StringBuilder key = new StringBuilder();
		key.append(getCompany().getId())
		.append("-")
		.append(id);

		return key.toString();
	}

	@Transient
	public UserExportableWrapper getUserExportableWrapper() {
		if(null == userExportableWrapper) {
			userExportableWrapper = new UserExportableWrapper(getId());
		}
		return userExportableWrapper;
	}

	/**
     * Find the customer specific commission that must be paid to this partner.
     *
     * @param partnerId
     * @return the rate or null
     */
    public BigDecimal findCommissionRate(int partnerId) {
        for(CustomerCommissionDefinitionDTO commission : commissionDefinitions) {
            if(commission.getId().getPartner().getId() == partnerId) {
                return commission.getRate();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        /*  Avoid lazy loaded fields to prevent a LazyInitializationException
            when printing users outside of the initial transaction. */
        return "UserDTO{"
               + "id=" + id
               + ", userName='" + userName + '\''
               + ", accountExpired=" + accountExpired
               + ", accountDisabledDate=" + accountDisabledDate
               + '}';
    }

    public void touch() {
        // touch
        if (getCustomer() != null) {
            getCustomer().getTotalSubAccounts();
            if (getCustomer().getParent() != null) {
                getCustomer().getParent().getBaseUser().getId();
            }
        }

        if (getPartner() != null) {
            getPartner().touch();
        }
    }

    @Transient
    public boolean isInvoiceAsChild() {
        return ( this.getCustomer() != null && this.getCustomer().invoiceAsChild());
    }

    @Override
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "user_meta_field_map",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @SortComparator(value = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.USER };
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {

        // Close Payment Information DTO objects from the list
        for (PaymentInformationDTO paymentInformationDTO: paymentInstruments){
            paymentInformationDTO.close();
        }
    }
}
