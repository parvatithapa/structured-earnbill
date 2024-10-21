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

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.EntitySignupValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;
import com.sapienter.jbilling.server.util.cxf.CxfMapIntegerDateAdapter;
import com.sapienter.jbilling.server.util.cxf.CxfMapListDateAdapter;
import com.sapienter.jbilling.server.util.cxf.CxfMapMapListMetafieldAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/** @author Emil */
@ApiModel(value = "User Data", description = "UserWS Model")
public class UserWS implements WSSecured, HierarchicalEntity, Serializable, AutoCloseable {
	@Min(value = 1, message = "validation.error.min,1", groups = UpdateValidationGroup.class)
    @Max(value = 0, message = "validation.error.max,0", groups = CreateValidationGroup.class)
    private int id;
    private Integer currencyId;
    @Pattern(regexp=Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES, message="validation.error.password.size,8,128", groups = {CreateValidationGroup.class, EntitySignupValidationGroup.class })
    private String password;
    private boolean createCredentials = false;
    private int deleted;
    @ConvertToTimezone
    private Date createDatetime;
    @ConvertToTimezone
    private Date lastStatusChange;
    @ConvertToTimezone
    private Date lastLogin;
    private boolean accountExpired;
    @ConvertToTimezone
    private Date accountDisabledDate;
    @NotNull(message="validation.error.notnull")
    //@Size(min = 9, max = 10, message = "validation.error.size,9,10")
    //@Pattern(regexp=Constants.ADENNET_USERNAME_PATTERN, message="validation.error.invalid.username", groups = {CreateValidationGroup.class, EntitySignupValidationGroup.class })
    private String userName;
    private int failedAttempts;
    private Integer languageId;

    @Valid
    private ContactWS contact = null;
    private String role = null;
    private String language = null;
    private String status = null;
    private Integer mainRoleId = null;
    private Integer statusId = null;
    private Integer subscriberStatusId = null;
    private Integer customerId = null;
    @Digits(integer = 12, fraction = 0, message= "validation.error.invalid.agentid")
    private Integer partnerRoleId = null;
    private Integer[] partnerIds = null;
    private Integer parentId = null;
    private Boolean isParent = null;
    private Boolean invoiceChild = null;
    private Boolean useParentPricing = null;
    private Boolean excludeAgeing = null;
    private String[] blacklistMatches = null;
    private Boolean userIdBlacklisted = null;
    private Integer[] childIds = null;
    private String owingBalance = null;
    private Integer balanceType = null;
    private String dynamicBalance = null;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String autoRecharge = null;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditLimit = null;
	private String creditLimitNotification1 = null;
	private String creditLimitNotification2 = null;
    private String notes;
    private Integer automaticPaymentType;
    private String companyName;
    private Boolean isAccountLocked;

    private Integer invoiceDeliveryMethodId;
    private Integer dueDateUnitId;
    private Integer dueDateValue;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    private Date nextInvoiceDate;

    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String rechargeThreshold = "-1";
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String lowBalanceThreshold = "-1";
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String monthlyLimit;
    private Integer entityId;

    @Valid
    private MetaFieldValueWS[] metaFields;

    @Valid
    private MainSubscriptionWS mainSubscription;
    private CustomerNoteWS[] customerNotes;
    @Valid
    private CustomerCommissionDefinitionWS[] commissionDefinitions;

    private Integer accountTypeId;
    private String invoiceDesign;
    private Integer invoiceTemplateId;

    private Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = new HashMap<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>>();
    private Map<Integer, ArrayList<Date>> timelineDatesMap = new HashMap<Integer, ArrayList<Date>>(0);
    private Map<Integer, Date> effectiveDateMap = new HashMap<Integer, Date>(0);
    private Map<Integer, ArrayList<Date>> removedDatesMap = new HashMap<Integer, ArrayList<Date>>(0);
    
    //user codes of other users linked to this customer
    private String userCodeLink;
    // payment instruments
    private List<PaymentInformationWS> paymentInstruments = new ArrayList<PaymentInformationWS>();

    private List<Integer> accessEntities;
    private Integer cimProfileError;
    private String identificationType;
    private String identificationText;
    private String identificationImage;
    private Integer reissueCount;
    @ConvertToTimezone
    private Date reissueDate;
    public UserWS() {
    }

    @ApiModelProperty(value = "Unique identifier for this record")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The ids of the partners related to this record")
	public Integer[] getPartnerIds() {
        return partnerIds;
    }

    public void setPartnerIds(Integer[] partnerIds) {
        this.partnerIds = partnerIds;
    }

    @ApiModelProperty(value = "The role id of the partner")
    public Integer getPartnerRoleId() {
        return partnerRoleId;
    }

    public void setPartnerRoleId(Integer partnerRoleId) {
        this.partnerRoleId = partnerRoleId;
    }

    @ApiModelProperty("Customer commission definition")
    public CustomerCommissionDefinitionWS[] getCommissionDefinitions() {
        return commissionDefinitions;
    }

    public void setCommissionDefinitions(CustomerCommissionDefinitionWS[] commissionDefinitions) {
        this.commissionDefinitions = commissionDefinitions;
    }

    @ApiModelProperty(value = "user codes of other users linked to current customer")
    public String getUserCodeLink() {
        return userCodeLink;
    }

    public void setUserCodeLink(String userCodeLink) {
        this.userCodeLink = userCodeLink;
    }

    @ApiModelProperty(value = "The primary contact information for this user")
    public ContactWS getContact() {
        return contact;
    }

    public void setContact(ContactWS contact) {
        this.contact = contact;
    }

    @ApiModelProperty(value = "Name of the language (i.e. \"English\")")
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @ApiModelProperty(value = "Name of the User's current status (i.e. \"Suspended\" or \"Active\")")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiModelProperty(value = "The name of the User's role (i.e. \"Clerk\" or \"Customer\")")
    public String getRole() {
        return role;
    }

    public void setRole(String type) {
        this.role = type;
    }

    @ApiModelProperty(value = "The level of privilege granted to the user when logged into the system",
            allowableValues = "1(Internal), 2(Super User), 3(Clerk), 4(Partner), 5(Customer)")
    public Integer getMainRoleId() {
        return mainRoleId;
    }

    public void setMainRoleId(Integer mainRoleId) {
        this.mainRoleId = mainRoleId;
    }

    @ApiModelProperty(value = "Current status identifier of the user.",
            allowableValues = "1(Active), 2(Overdue), 3(Overdue 2), 4(Overdue 3), 5(Suspended), 6(Suspended 2), 7(Suspended 3), 8(Deleted)")
    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    @ApiModelProperty(value = "Subscriber status for this user",
            allowableValues = "1(Active), 2(Pending Subscription), 3(Unsubscribed), 4(Pending Expiration), 5(Expired), 6(Nonsubscriber), 7(Discontinued)")
    public Integer getSubscriberStatusId() {
        return subscriberStatusId;
    }

    public void setSubscriberStatusId(Integer subscriberStatusId) {
        this.subscriberStatusId = subscriberStatusId;
    }

    @ApiModelProperty(value = "Reference to the Customer information for this user")
    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    @ApiModelProperty(value = "If the user belongs to a parent record, this field contains the identifier of the parent record")
    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    @ApiModelProperty(value = "true if this record is a \"parent\" user A parent user can have sub-accounts (children)")
    public Boolean getIsParent() {
        return isParent;
    }

    public void setIsParent(Boolean isParent) {
        this.isParent = isParent;
    }

    @ApiModelProperty(value = "true if this is a sub-account (child of a parent account), but this user will still receive invoices")
    public Boolean getInvoiceChild() {
        return invoiceChild;
    }

    public void setInvoiceChild(Boolean invoiceChild) {
        this.invoiceChild = invoiceChild;
    }

    @ApiModelProperty(value = "This field will indicate to the system that you want the child account to take on the same rates that the parent carries for particular products")
    public Boolean getUseParentPricing() {
        return useParentPricing;
    }

    public Boolean useParentPricing() {
        return useParentPricing;
    }

    public void setUseParentPricing(Boolean useParentPricing) {
        this.useParentPricing = useParentPricing;
    }

    @ApiModelProperty(value = "Boolean value to indicate excluding this User/Customer from the Ageing process")
    public Boolean getExcludeAgeing() {
        return excludeAgeing;
    }

    public void setExcludeAgeing(Boolean excludeAgeing) {
        this.excludeAgeing = excludeAgeing;
    }

    @ApiModelProperty(value = "Creation date of this data record")
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ApiModelProperty(value = "Contains the currency code for this user")
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "If the record has been deleted, this field contains '1', otherwise it contains '0'. Note that deletion cannot be carried out by simply setting a '1' in this field.")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "Number of login attempts that have been failed by this user (i.e., the user has entered the wrong password)")
    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    @JsonIgnore
    public int getUserId() {
        return id;
    }

    public void setUserId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Date of the last login performed by this user")
    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @ApiModelProperty(value = "This field indicates to the system that this account has expired")
    @JsonProperty(value = "accountExpired")
    public boolean isAccountExpired() { 
        return accountExpired; 
    }

    public void setAccountExpired(boolean accountExpired) { 
        this.accountExpired = accountExpired; 
    }

    @ApiModelProperty(value = "Account disable date")
    public Date getAccountDisabledDate() { 
        return accountDisabledDate; 
    }

    public void setAccountDisabledDate(Date accountDisabledDate) { 
        this.accountDisabledDate = accountDisabledDate; 
    }

    @ApiModelProperty(value = "Date of the last status change incurred by this user")
    public Date getLastStatusChange() {
        return lastStatusChange;
    }

    public void setLastStatusChange(Date lastStatusChange) {
        this.lastStatusChange = lastStatusChange;
    }

    @ApiModelProperty(value = "Authenticates the user's identity during login. This could be meaningless if the password is encrypted")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(value = "Identifies the user during login", required = true)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @ApiModelProperty(value = "Contains the preferred language id for this user. Can be configured in jBilling")
    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    @ApiModelProperty(value = "Lists any blacklist matches for this user. See the jBilling User Guide for more information on blacklists")
    public String[] getBlacklistMatches() {
        return blacklistMatches;
    }

    public void setBlacklistMatches(String[] blacklistMatches) {
        this.blacklistMatches = blacklistMatches;
    }

    @ApiModelProperty(value = "true if the user id is blacklisted See the jBilling User Guide for more information on blacklists")
    public Boolean getUserIdBlacklisted() {
        return userIdBlacklisted;
    }

    public void setUserIdBlacklisted(Boolean userIdBlacklisted) {
        this.userIdBlacklisted = userIdBlacklisted;
    }

    @ApiModelProperty(value = "The identifiers of any sub-accounts for this user")
    public Integer[] getChildIds() {
        return childIds;
    }

    public void setChildIds(Integer[] childIds) {
        this.childIds = childIds;
    }

    @JsonIgnore
    public String getOwingBalance() {
        return owingBalance;
    }

    @ApiModelProperty(value = "A real-time calculated owing balance (All Invoices - All Payments)")
    @JsonProperty(value = "owingBalance")
    public BigDecimal getOwingBalanceAsDecimal() {
        return Util.string2decimal(owingBalance);
    }

    @JsonIgnore
    public void setOwingBalance(String owingBalance) {
        this.owingBalance = owingBalance;
    }

    @JsonProperty("owingBalance")
    public void setOwingBalance(BigDecimal owingBalance) {
        this.owingBalance = (owingBalance != null ? owingBalance.toString() : null);
    }

    @ApiModelProperty(value = "Balance type")
    public Integer getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(Integer balanceType) {
        this.balanceType = balanceType;
    }


    @JsonIgnore
    public String getCreditLimit() {
        return creditLimit;
    }

    @ApiModelProperty(value = "The credit limit. Only valid if balanceType is of credit limit type")
    @JsonProperty("creditLimit")
    public BigDecimal getCreditLimitAsDecimal() {
        return Util.string2decimal(creditLimit);
    }

    @JsonIgnore
    public void setCreditLimitAsDecimal(BigDecimal creditLimit) {
        setCreditLimit(creditLimit);
    }

    @JsonIgnore
    public void setCreditLimit(String creditLimit) {
        this.creditLimit = creditLimit;
    }

    @JsonProperty("creditLimit")
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = (creditLimit != null ? creditLimit.toString() : null);
    }


    @JsonIgnore
	public String getCreditLimitNotification1() {
		return creditLimitNotification1;
	}

    @JsonIgnore
	public void setCreditLimitNotification1(String creditLimitNotification1) {
		this.creditLimitNotification1 = creditLimitNotification1;
	}

    @ApiModelProperty(value = "Credit limit for which the first notification will be sent")
    @JsonProperty("creditLimitNotification1")
    public BigDecimal getCreditLimitNotification1AsDecimal() {
		return Util.string2decimal(creditLimitNotification1);
	}

    @JsonProperty("creditLimitNotification1")
	public void setCreditLimitNotification1(BigDecimal creditLimitNotification1) {
		this.creditLimitNotification1 = (null != creditLimitNotification1 ? creditLimitNotification1.toString() : null);
	}

    @JsonIgnore
	public String getCreditLimitNotification2() {
		return creditLimitNotification2;
	}

    @JsonIgnore
	public void setCreditLimitNotification2(String creditLimitNotification2) {
		this.creditLimitNotification2 = creditLimitNotification2;
	}

    @ApiModelProperty(value = "Credit limit for which the second notification will be sent")
    @JsonProperty("creditLimitNotification2")
	public BigDecimal getCreditLimitNotification2AsDecimal() {
		return Util.string2decimal(creditLimitNotification2);
	}

    @JsonProperty("creditLimitNotification2")
	public void setCreditLimitNotification2(BigDecimal creditLimitNotification2) {
		this.creditLimitNotification2 = (null != creditLimitNotification2 ? creditLimitNotification2.toString() : null);
	}

    @JsonIgnore
	public String getDynamicBalance() {
        return dynamicBalance;
    }

    @ApiModelProperty(value = "String representation of this Customer's dynamic balance. If balanceType is credit limit, this represents the amount of credit used on the account. " +
            "If balanceType is pre paid, this represents the pre paid balance remaining")
    @JsonProperty("dynamicBalance")
    public BigDecimal getDynamicBalanceAsDecimal() {
        return Util.string2decimal(dynamicBalance);
    }

    @JsonIgnore
    public void setDynamicBalance(String dynamicBalance) {
        this.dynamicBalance = dynamicBalance;
    }

    @JsonProperty("dynamicBalance")
    public void setDynamicBalance(BigDecimal dynamicBalance) {
        this.dynamicBalance = (dynamicBalance != null ? dynamicBalance.toPlainString() : null);
    }

    @JsonIgnore
    public String getAutoRecharge() {
        return autoRecharge;
    }

    @ApiModelProperty(value = "Amount by which the customer's account will be auto-recharged when depleted")
    @JsonProperty("autoRecharge")
    public BigDecimal getAutoRechargeAsDecimal() {
        return Util.string2decimal(autoRecharge);
    }

    @JsonIgnore
    public void setAutoRechargeAsDecimal(BigDecimal autoRecharge) {
        setAutoRecharge(autoRecharge);
    }

    @JsonIgnore
    public void setAutoRecharge(String autoRecharge) {
        this.autoRecharge = autoRecharge;
    }

    @JsonProperty("autoRecharge")
    public void setAutoRecharge(BigDecimal autoRecharge) {
        this.autoRecharge = (autoRecharge != null ? autoRecharge.toString() : null);
    }

    @ApiModelProperty(value = "CRM notes for this user")
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @ApiModelProperty(value = "Integer value to determine which of the three payment methods does the customer want to apply for automatic payment processing.",
            allowableValues = "1(Credit Card), 2(ACH), 3(Cheque)")
    public Integer getAutomaticPaymentType() {
        return automaticPaymentType;
    }

    public void setAutomaticPaymentType(Integer automaticPaymentType) {
        this.automaticPaymentType = automaticPaymentType;
    }

    @ApiModelProperty(value = "User's company name")
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @ApiModelProperty(value = "Reference ID for one of the Invoice Delivery Methods",
            allowableValues = "1(Email), 2(Paper), 3(Email & Paper), 4(None)")
    public Integer getInvoiceDeliveryMethodId() {
        return invoiceDeliveryMethodId;
    }

    public void setInvoiceDeliveryMethodId(Integer invoiceDeliveryMethodId) {
        this.invoiceDeliveryMethodId = invoiceDeliveryMethodId;
    }

    @ApiModelProperty(value = "Period Unit of this Customer's Invoice due date")
    public Integer getDueDateUnitId() {
        return dueDateUnitId;
    }

    public void setDueDateUnitId(Integer dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    @ApiModelProperty(value = "Customer specific Invoice Due date value")
    public Integer getDueDateValue() {
        return dueDateValue;
    }

    public void setDueDateValue(Integer dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    @ApiModelProperty(value = "The earliest next billable date for this user's Orders")
    public Date getNextInvoiceDate() {
        return nextInvoiceDate;
    }

    public void setNextInvoiceDate(Date nextInvoiceDate) {
        this.nextInvoiceDate = nextInvoiceDate;
    }

    @ApiModelProperty(value = "User define fields")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "The Main Subscription field allows a billing administrator to select any billing period required for a specific customer")
    public MainSubscriptionWS getMainSubscription() {
		return mainSubscription;
	}

	public void setMainSubscription(MainSubscriptionWS mainSubscription) {
		this.mainSubscription = mainSubscription;
	}

    @ApiModelProperty(value = "Customer notes")
    public CustomerNoteWS[] getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(CustomerNoteWS[] customerNotes) {
        this.customerNotes = customerNotes;
    }

    @ApiModelProperty(value = "This field stores the account type id.")
    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountTypeId = accountTypeId;
    }

    @ApiModelProperty(value = "The name of the invoice design used for this user")
    public String getInvoiceDesign() {
        return invoiceDesign;
    }

    public void setInvoiceDesign(String invoiceDesign) {
        this.invoiceDesign = invoiceDesign;
    }

    @ApiModelProperty("The id of the invoice template used")
    public Integer getInvoiceTemplateId() {
        return invoiceTemplateId;
    }

    public void setInvoiceTemplateId(Integer invoiceTemplateId) {
        this.invoiceTemplateId = invoiceTemplateId;
    }

    @XmlJavaTypeAdapter(CxfMapListDateAdapter.class)
    @ApiModelProperty(value = "This is a map of the key and date of timeline wise meta field values. " +
            "It is possible to store meta field values for a specific date range. " +
            "For example, for account information type such as business contact, it is possible to keep one contact address and phone number for year 2014 and setup a different address for year 2015")
	public Map<Integer, ArrayList<Date>> getTimelineDatesMap() {
		return timelineDatesMap;
	}

	public void setTimelineDatesMap(Map<Integer, ArrayList<Date>> timelineDatesMap) {
		this.timelineDatesMap = timelineDatesMap;
	}

    @XmlJavaTypeAdapter(CxfMapIntegerDateAdapter.class)
    @ApiModelProperty(value = "This is a map of the key and date of effective wise meta field values. " +
            "It is possible to store meta field values for a specific date range. Selecting an Effective Date lets the system know the day the User becomes active on.")
	public Map<Integer, Date> getEffectiveDateMap() {
		return effectiveDateMap;
	}

	public void setEffectiveDateMap(Map<Integer, Date> effectiveDateMap) {
		this.effectiveDateMap = effectiveDateMap;
	}

    @XmlJavaTypeAdapter(CxfMapListDateAdapter.class)
    @ApiModelProperty(value = "Identifies the removed effective dates")
	public Map<Integer, ArrayList<Date>> getRemovedDatesMap() {
		return removedDatesMap;
	}

	public void setRemovedDatesMap(Map<Integer, ArrayList<Date>> removedDatesMap) {
		this.removedDatesMap = removedDatesMap;
	}

    @XmlJavaTypeAdapter(CxfMapMapListMetafieldAdapter.class)
    @ApiModelProperty(value = "This is a map of account information type id as the key and map of time-line wise meta field values")
	public Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> getAccountInfoTypeFieldsMap() {
		return accountInfoTypeFieldsMap;
	}
	
	public void setAccountInfoTypeFieldsMap(Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap) {
		this.accountInfoTypeFieldsMap = accountInfoTypeFieldsMap;
	}

    @ApiModelProperty("Is the user account locked or not")
    @JsonProperty(value = "isAccountLocked")
    public Boolean isAccountLocked() {
        return isAccountLocked;
    }

    public void setIsAccountLocked(Boolean isAccountLocked) {
        this.isAccountLocked = isAccountLocked;
    }

    @JsonIgnore
    public String getRechargeThreshold() {
        return rechargeThreshold;
    }

    @ApiModelProperty(value = "A recharge threshold")
    @JsonProperty("rechargeThreshold")
    public BigDecimal getRechargeThresholdAsDecimal() {
        return Util.string2decimal(rechargeThreshold);
    }

    @JsonIgnore
    public void setRechargeThreshold(String rechargeThreshold) {
        this.rechargeThreshold = rechargeThreshold;
    }

    @JsonProperty("rechargeThreshold")
    public void setRechargeThreshold(BigDecimal rechargeThreshold) {
        this.rechargeThreshold = (rechargeThreshold != null ? rechargeThreshold.toString() : null);
    }

    @JsonIgnore
    public String getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    @ApiModelProperty(value = "Low balance threshold")
    @JsonProperty("lowBalanceThreshold")
    public BigDecimal getLowBalanceThresholdAsDecimal() {
        return Util.string2decimal(lowBalanceThreshold);
    }

    @JsonIgnore
    public void setLowBalanceThreshold(String lowBalanceThreshold) {
        this.lowBalanceThreshold = lowBalanceThreshold;
    }

    @JsonProperty("lowBalanceThreshold")
    public void setLowBalanceThreshold(BigDecimal lowBalanceThreshold) {
        this.lowBalanceThreshold = (lowBalanceThreshold != null ? lowBalanceThreshold.toString() : null);
    }

    @JsonIgnore
    public String getMonthlyLimit () {
        return monthlyLimit;
    }

    @ApiModelProperty(value = "This field sets the limit on amount up to which recharge should be used for the month")
    @JsonProperty("monthlyLimit")
    public BigDecimal getMonthlyLimitAsDecimal () {
        return Util.string2decimal(monthlyLimit);
    }

    @JsonIgnore
    public void setMonthlyLimit (String monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    @JsonProperty("monthlyLimit")
    public void setMonthlyLimit (BigDecimal monthlyLimit) {
        this.monthlyLimit = (monthlyLimit != null ? monthlyLimit.toString() : null);
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     *
     * @return null
     */
    @JsonIgnore
    public Integer getOwningEntityId() {
        return null;
    }

    @JsonIgnore
    public Integer getOwningUserId() {
        return getUserId();
    }

    /**
     * Returns the list of jBilling Entity IDs within a hierarchy that have access to this object.
     *
     * @return list of entities that have access.
     */
    @Override
    @ApiModelProperty(value = "The list of jBilling Entity IDs within a hierarchy that have access to this object")
    public List<Integer> getAccessEntities() {
        return this.accessEntities;
    }

    public void setAccessEntities(List<Integer> accessEntities) {
        this.accessEntities = accessEntities;
    }

    @ApiModelProperty(value = "Current Company ID")
    public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

    @ApiModelProperty(value = "Identifies the payment instruments that is used for payment")
	public List<PaymentInformationWS> getPaymentInstruments() {
		return paymentInstruments;
	}

	public void setPaymentInstruments(List<PaymentInformationWS> paymentInstruments) {
		this.paymentInstruments = paymentInstruments;
	}

    @ApiModelProperty(value = "This field will indicate to the system that you want to create credentials for the user")
    @JsonProperty(value = "createCredentials")
    public boolean isCreateCredentials() {
        return createCredentials;
    }

    public void setCreateCredentials(boolean createCredentials) {
        this.createCredentials = createCredentials;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(", autoRecharge=");
        builder.append(autoRecharge);
        builder.append(", automaticPaymentType=");
        builder.append(automaticPaymentType);
        builder.append(", balanceType=");
        builder.append(balanceType);
        builder.append(", blacklistMatches=");
        builder.append(Arrays.toString(blacklistMatches));
        builder.append(", childIds=");
        builder.append(Arrays.toString(childIds));
        builder.append(", companyName=");
        builder.append(companyName);
        builder.append(", contact=");
        builder.append(contact);
        builder.append(", createDatetime=");
        builder.append(createDatetime);
        builder.append(", creditLimit=");
        builder.append(creditLimit);
        builder.append(", currencyId=");
        builder.append(currencyId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", deleted=");
        builder.append(deleted);
        builder.append(", dueDateUnitId=");
        builder.append(dueDateUnitId);
        builder.append(", dueDateValue=");
        builder.append(dueDateValue);
        builder.append(", dynamicBalance=");
        builder.append(dynamicBalance);
        builder.append(", excludeAgeing=");
        builder.append(excludeAgeing);
        builder.append(", failedAttempts=");
        builder.append(failedAttempts);
        builder.append(", id=");
        builder.append(id);
        builder.append(", invoiceChild=");
        builder.append(invoiceChild);
        builder.append(", invoiceDeliveryMethodId=");
        builder.append(invoiceDeliveryMethodId);
        builder.append(", isParent=");
        builder.append(isParent);
        builder.append(", language=");
        builder.append(language);
        builder.append(", languageId=");
        builder.append(languageId);
        builder.append(", lastLogin=");
        builder.append(lastLogin);
        builder.append(", lastStatusChange=");
        builder.append(lastStatusChange);
        builder.append(", mainRoleId=");
        builder.append(mainRoleId);
        builder.append(", nextInvoiceDate=");
        builder.append(nextInvoiceDate);
        builder.append(", notes=");
        builder.append(notes);
        builder.append(", owingBalance=");
        builder.append(owingBalance);
        builder.append(", parentId=");
        builder.append(parentId);
        builder.append(", partnerIds=");
        builder.append(Arrays.toString(partnerIds));
        builder.append(", role=");
        builder.append(role);
        builder.append(", status=");
        builder.append(status);
        builder.append(", statusId=");
        builder.append(statusId);
        builder.append(", subscriberStatusId=");
        builder.append(subscriberStatusId);
        builder.append(", userIdBlacklisted=");
        builder.append(userIdBlacklisted);
        builder.append(", userName=");
        builder.append(userName);
        builder.append(", accountTypeId=");
        builder.append(accountTypeId);
        builder.append(", invoiceDesign=");
        builder.append(invoiceDesign);
        builder.append(", invoiceTemplateId=");
        builder.append(invoiceTemplateId);
        builder.append(", userCodeLink=");
        builder.append(userCodeLink);
        builder.append(", isAccountLocked=");
        builder.append(isAccountLocked);
        builder.append(", commissionDefinitions=");
        builder.append(Arrays.toString(commissionDefinitions));
        builder.append(", accountExpired=");
        builder.append(accountExpired);
        builder.append(", accountDisabledDate=");
        builder.append(accountDisabledDate);
        builder.append(']');
        return builder.toString();
    }

    /**
     * Named differently to avoid name conflict with implementing entities.
     *
     * @return
     */
    @Override
    @JsonIgnore
    public Boolean ifGlobal() {
        return false;
    }

    @ApiModelProperty(value = "CIM Profile Error")
	public Integer getCimProfileError() {
		return cimProfileError;
	}

	public void setCimProfileError(Integer cimProfileError) {
		this.cimProfileError = cimProfileError;
	}

    public String getIdentificationType() { return identificationType; }

    public void setIdentificationType(String identificationType) { this.identificationType = identificationType; }

    public String getIdentificationText() { return identificationText; }

    public void setIdentificationText(String identificationText) { this.identificationText = identificationText; }

    public String getIdentificationImage() { return identificationImage; }

    public void setIdentificationImage(String identificationImage) { this.identificationImage = identificationImage; }

    public Integer getReissueCount() {
        return reissueCount;
    }

    public void setReissueCount(Integer reissueCount) {
        this.reissueCount = reissueCount;
    }

    public Date getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(Date reissueDate) {
        this.reissueDate = reissueDate;
    }

    @Override
    public void close() throws Exception {
        for(int i = 0; i < paymentInstruments.size(); i++ ){
            paymentInstruments.get(i).close();
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserWS)) return false;

        UserWS userWS = (UserWS) o;
        return id == userWS.id &&
                createCredentials == userWS.createCredentials &&
                deleted == userWS.deleted &&
                accountExpired == userWS.accountExpired &&
                failedAttempts == userWS.failedAttempts &&
                nullSafeEquals(currencyId, userWS.currencyId) &&
                nullSafeEquals(password, userWS.password) &&
                nullSafeEquals(createDatetime, userWS.createDatetime) &&
                nullSafeEquals(lastStatusChange, userWS.lastStatusChange) &&
                nullSafeEquals(lastLogin, userWS.lastLogin) &&
                nullSafeEquals(accountDisabledDate, userWS.accountDisabledDate) &&
                nullSafeEquals(userName, userWS.userName) &&
                nullSafeEquals(languageId, userWS.languageId) &&
                nullSafeEquals(contact, userWS.contact) &&
                nullSafeEquals(role, userWS.role) &&
                nullSafeEquals(mainRoleId, userWS.mainRoleId) &&
                nullSafeEquals(statusId, userWS.statusId) &&
                nullSafeEquals(subscriberStatusId, userWS.subscriberStatusId) &&
                nullSafeEquals(customerId, userWS.customerId) &&
                nullSafeEquals(partnerRoleId, userWS.partnerRoleId) &&
                nullSafeEquals(partnerIds, userWS.partnerIds) &&
                nullSafeEquals(parentId, userWS.parentId) &&
                nullSafeEquals(isParent, userWS.isParent) &&
                nullSafeEquals(invoiceChild, userWS.invoiceChild) &&
                nullSafeEquals(useParentPricing, userWS.useParentPricing) &&
                nullSafeEquals(excludeAgeing, userWS.excludeAgeing) &&
                nullSafeEquals(blacklistMatches, userWS.blacklistMatches) &&
                nullSafeEquals(userIdBlacklisted, userWS.userIdBlacklisted) &&
                nullSafeEquals(childIds, userWS.childIds) &&
                Util.decimalEquals(getOwingBalanceAsDecimal(), userWS.getOwingBalanceAsDecimal()) &&
                Util.decimalEquals(getDynamicBalanceAsDecimal(), userWS.getDynamicBalanceAsDecimal()) &&
                Util.decimalEquals(getAutoRechargeAsDecimal(), userWS.getAutoRechargeAsDecimal()) &&
                Util.decimalEquals(getCreditLimitAsDecimal(), userWS.getCreditLimitAsDecimal()) &&
                Util.decimalEquals(getCreditLimitNotification1AsDecimal(), userWS.getCreditLimitNotification1AsDecimal()) &&
                Util.decimalEquals(getCreditLimitNotification2AsDecimal(), userWS.getCreditLimitNotification2AsDecimal()) &&
                nullSafeEquals(notes, userWS.notes) &&
                nullSafeEquals(automaticPaymentType, userWS.automaticPaymentType) &&
                nullSafeEquals(companyName, userWS.companyName) &&
                nullSafeEquals(isAccountLocked, userWS.isAccountLocked) &&
                nullSafeEquals(invoiceDeliveryMethodId, userWS.invoiceDeliveryMethodId) &&
                nullSafeEquals(dueDateUnitId, userWS.dueDateUnitId) &&
                nullSafeEquals(dueDateValue, userWS.dueDateValue) &&
                nullSafeEquals(nextInvoiceDate, userWS.nextInvoiceDate) &&
                Util.decimalEquals(getRechargeThresholdAsDecimal(), userWS.getRechargeThresholdAsDecimal()) &&
                Util.decimalEquals(getMonthlyLimitAsDecimal(), userWS.getMonthlyLimitAsDecimal()) &&
                nullSafeEquals(entityId, userWS.entityId) &&
                nullSafeEquals(metaFields, userWS.metaFields) &&
                nullSafeEquals(mainSubscription, userWS.mainSubscription) &&
                nullSafeEquals(customerNotes, userWS.customerNotes) &&
                nullSafeEquals(commissionDefinitions, userWS.commissionDefinitions) &&
                nullSafeEquals(accountTypeId, userWS.accountTypeId) &&
                nullSafeEquals(invoiceDesign, userWS.invoiceDesign) &&
                nullSafeEquals(invoiceTemplateId, userWS.invoiceTemplateId) &&
                nullSafeEquals(accountInfoTypeFieldsMap, userWS.accountInfoTypeFieldsMap) &&
                nullSafeEquals(timelineDatesMap, userWS.timelineDatesMap) &&
                nullSafeEquals(effectiveDateMap, userWS.effectiveDateMap) &&
                nullSafeEquals(removedDatesMap, userWS.removedDatesMap) &&
                nullSafeEquals(userCodeLink, userWS.userCodeLink) &&
                nullSafeEquals(paymentInstruments, userWS.paymentInstruments) &&
                nullSafeEquals(accessEntities, userWS.accessEntities);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + nullSafeHashCode(currencyId);
        result = 31 * result + nullSafeHashCode(password);
        result = 31 * result + (createCredentials ? 1 : 0);
        result = 31 * result + deleted;
        result = 31 * result + nullSafeHashCode(createDatetime);
        result = 31 * result + nullSafeHashCode(lastStatusChange);
        result = 31 * result + nullSafeHashCode(lastLogin);
        result = 31 * result + (accountExpired ? 1 : 0);
        result = 31 * result + nullSafeHashCode(accountDisabledDate);
        result = 31 * result + nullSafeHashCode(userName);
        result = 31 * result + failedAttempts;
        result = 31 * result + nullSafeHashCode(languageId);
        result = 31 * result + nullSafeHashCode(contact);
        result = 31 * result + nullSafeHashCode(role);
        result = 31 * result + nullSafeHashCode(mainRoleId);
        result = 31 * result + nullSafeHashCode(statusId);
        result = 31 * result + nullSafeHashCode(subscriberStatusId);
        result = 31 * result + nullSafeHashCode(customerId);
        result = 31 * result + nullSafeHashCode(partnerRoleId);
        result = 31 * result + nullSafeHashCode(partnerIds);
        result = 31 * result + nullSafeHashCode(parentId);
        result = 31 * result + nullSafeHashCode(isParent);
        result = 31 * result + nullSafeHashCode(invoiceChild);
        result = 31 * result + nullSafeHashCode(useParentPricing);
        result = 31 * result + nullSafeHashCode(excludeAgeing);
        result = 31 * result + nullSafeHashCode(blacklistMatches);
        result = 31 * result + nullSafeHashCode(userIdBlacklisted);
        result = 31 * result + nullSafeHashCode(childIds);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getOwingBalanceAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getDynamicBalanceAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getAutoRechargeAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getCreditLimitAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getCreditLimitNotification1AsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getCreditLimitNotification2AsDecimal()));
        result = 31 * result + nullSafeHashCode(notes);
        result = 31 * result + nullSafeHashCode(automaticPaymentType);
        result = 31 * result + nullSafeHashCode(companyName);
        result = 31 * result + nullSafeHashCode(isAccountLocked);
        result = 31 * result + nullSafeHashCode(invoiceDeliveryMethodId);
        result = 31 * result + nullSafeHashCode(dueDateUnitId);
        result = 31 * result + nullSafeHashCode(dueDateValue);
        result = 31 * result + nullSafeHashCode(nextInvoiceDate);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getRechargeThresholdAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getMonthlyLimitAsDecimal()));
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + nullSafeHashCode(metaFields);
        result = 31 * result + nullSafeHashCode(mainSubscription);
        result = 31 * result + nullSafeHashCode(customerNotes);
        result = 31 * result + nullSafeHashCode(commissionDefinitions);
        result = 31 * result + nullSafeHashCode(accountTypeId);
        result = 31 * result + nullSafeHashCode(invoiceDesign);
        result = 31 * result + nullSafeHashCode(invoiceTemplateId);
        result = 31 * result + nullSafeHashCode(accountInfoTypeFieldsMap);
        result = 31 * result + nullSafeHashCode(timelineDatesMap);
        result = 31 * result + nullSafeHashCode(effectiveDateMap);
        result = 31 * result + nullSafeHashCode(removedDatesMap);
        result = 31 * result + nullSafeHashCode(userCodeLink);
        result = 31 * result + nullSafeHashCode(paymentInstruments);
        result = 31 * result + nullSafeHashCode(accessEntities);
        return result;
    }
}
