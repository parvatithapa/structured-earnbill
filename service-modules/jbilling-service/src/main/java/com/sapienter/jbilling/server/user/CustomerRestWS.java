package com.sapienter.jbilling.server.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.Diffable;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.lang.Integer;
import java.lang.String;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 
 * @author Neelabh
 * @since 07-May-2019
 */
@ApiModel(value = "Customer Data", description = "CustomerRestWS Model")
public class CustomerRestWS implements Serializable, Diffable<CustomerRestWS> {

    private Integer userId;
    private Integer statusId = null;
    private Integer subscriberStatusId = null;
    private Integer languageId;
    private Integer currencyId;
    private Integer automaticPaymentType;
    private Integer[] partnerIds = null;
    private Integer parentId = null;

    private Boolean isParent = null;
    private Boolean invoiceChild = null;
    private Boolean useParentPricing = null;
    private Boolean excludeAgeing = null;
    private Boolean isAccountLocked = null;
    private Boolean accountExpired = null;

    private String userCodeLink;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditLimit = null;

    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String autoRecharge = null;

    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String rechargeThreshold = null;

    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String lowBalanceThreshold = null;

    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String monthlyLimit = null;

    private Integer invoiceDeliveryMethodId;
    private Integer dueDateValue;
    private Integer dueDateUnitId;

    @Valid
    private MainSubscriptionWS mainSubscription;
    private String nextInvoiceDate;
    private Integer invoiceTemplateId;

    public CustomerRestWS() {
    }

    @ApiModelProperty(value = "The id of the customer for this record", required = true)
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    @ApiModelProperty(value = "Contains the preferred language id for this user. Can be configured in jBilling")
    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    @ApiModelProperty(value = "Contains the currency code for this user")
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "Integer value to determine which of the three payment methods does the customer want to apply for automatic payment processing.",
            allowableValues = "1(Credit Card), 2(ACH), 3(Cheque)")
    public Integer getAutomaticPaymentType() {
        return automaticPaymentType;
    }

    public void setAutomaticPaymentType(Integer automaticPaymentType) {
        this.automaticPaymentType = automaticPaymentType;
    }

    @ApiModelProperty(value = "The ids of the partners related to this record")
    public Integer[] getPartnerIds() {
        return partnerIds;
    }

    public void setPartnerIds(Integer[] partnerIds) {
        this.partnerIds = partnerIds;
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

    @ApiModelProperty("Is the user account locked or not")
    @JsonProperty(value = "isAccountLocked")
    public Boolean getIsAccountLocked() {
        return isAccountLocked;
    }

    public void setIsAccountLocked(Boolean isAccountLocked) {
        this.isAccountLocked = isAccountLocked;
    }

    @ApiModelProperty(value = "This field indicates to the system that this account has expired")
    @JsonProperty(value = "accountExpired")
    public boolean getAccountExpired() { 
        return accountExpired; 
    }

    public void setAccountExpired(boolean accountExpired) { 
        this.accountExpired = accountExpired; 
    }

    @ApiModelProperty(value = "user codes of other users linked to current customer")
    public String getUserCodeLink() {
        return userCodeLink;
    }

    public void setUserCodeLink(String userCodeLink) {
        this.userCodeLink = userCodeLink;
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
    public void setCreditLimit(String creditLimit) {
        this.creditLimit = creditLimit;
    }

    @JsonProperty("creditLimit")
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = (creditLimit != null ? Util.getScaledDecimal(creditLimit).toString() : null);
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
    public void setAutoRecharge(String autoRecharge) {
        this.autoRecharge = autoRecharge;
    }

    @JsonProperty("autoRecharge")
    public void setAutoRecharge(BigDecimal autoRecharge) {
        this.autoRecharge = (autoRecharge != null ? Util.getScaledDecimal(autoRecharge).toString() : null);
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
        this.rechargeThreshold = (rechargeThreshold != null ? Util.getScaledDecimal(rechargeThreshold).toString() : null);
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
        this.lowBalanceThreshold = (lowBalanceThreshold != null ? Util.getScaledDecimal(lowBalanceThreshold).toString() : null);
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
        this.monthlyLimit = (monthlyLimit != null ? Util.getScaledDecimal(monthlyLimit).toString() : null);
    }

    @ApiModelProperty(value = "Reference ID for one of the Invoice Delivery Methods",
            allowableValues = "1(Email), 2(Paper), 3(Email & Paper), 4(None)")
    public Integer getInvoiceDeliveryMethodId() {
        return invoiceDeliveryMethodId;
    }

    public void setInvoiceDeliveryMethodId(Integer invoiceDeliveryMethodId) {
        this.invoiceDeliveryMethodId = invoiceDeliveryMethodId;
    }

    @ApiModelProperty(value = "Customer specific Invoice Due date value")
    public Integer getDueDateValue() {
        return dueDateValue;
    }

    public void setDueDateValue(Integer dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    @ApiModelProperty(value = "Period Unit of this Customer's Invoice due date")
    public Integer getDueDateUnitId() {
        return dueDateUnitId;
    }

    public void setDueDateUnitId(Integer dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    @ApiModelProperty(value = "The Main Subscription field allows a billing administrator to select any billing period required for a specific customer")
    public MainSubscriptionWS getMainSubscription() {
        return mainSubscription;
    }

    public void setMainSubscription(MainSubscriptionWS mainSubscription) {
        this.mainSubscription = mainSubscription;
    }

    @ApiModelProperty(value = "The earliest next billable date for this user's Orders. The date format is 'yyyy-MM-dd'")
    public String getNextInvoiceDate() {
        return nextInvoiceDate;
    }

    public void setNextInvoiceDate(String nextInvoiceDate) {
        this.nextInvoiceDate = nextInvoiceDate;
    }

    @JsonIgnore
    public Date getNextInvoiceDateParsed() {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            return  StringUtils.isNotBlank(nextInvoiceDate) ? formatter.parse(nextInvoiceDate) : null;
        } catch (ParseException pe) {
            return null;
        }
    }

    @ApiModelProperty("The id of the invoice template used")
    public Integer getInvoiceTemplateId() {
        return invoiceTemplateId;
    }

    public void setInvoiceTemplateId(Integer invoiceTemplateId) {
        this.invoiceTemplateId = invoiceTemplateId;
    }

    @Override
    public DiffResult diff(CustomerRestWS other) {
         return new DiffBuilder(this, other, ToStringStyle.DEFAULT_STYLE)
           .append("currencyId", this.currencyId, other.currencyId)
           .append("languageId", this.languageId, other.languageId)
           .append("statusId", this.statusId, other.statusId)
           .append("subscriberStatusId", this.subscriberStatusId, other.subscriberStatusId)
           .append("automaticPaymentType", this.automaticPaymentType, other.automaticPaymentType)
           .append("partnerIds", this.partnerIds, other.partnerIds)
           .append("parentId", this.parentId, other.parentId)
           .append("isParent", this.isParent, other.isParent)
           .append("invoiceChild", this.invoiceChild, other.invoiceChild)
           .append("useParentPricing", this.useParentPricing, other.useParentPricing)
           .append("excludeAgeing", this.excludeAgeing, other.excludeAgeing)
           .append("isAccountLocked", this.isAccountLocked, other.isAccountLocked)
           .append("accountExpired", this.accountExpired, other.accountExpired)
           .append("userCodeLink", this.userCodeLink, other.userCodeLink)
           .append("creditLimit", this.creditLimit, other.creditLimit)
           .append("autoRecharge", this.autoRecharge, other.autoRecharge)
           .append("rechargeThreshold", this.rechargeThreshold, other.rechargeThreshold)
           .append("lowBalanceThreshold", this.lowBalanceThreshold, other.lowBalanceThreshold)
           .append("monthlyLimit", this.monthlyLimit, other.monthlyLimit)
           .append("invoiceDeliveryMethodId", this.invoiceDeliveryMethodId, other.invoiceDeliveryMethodId)
           .append("dueDateValue", this.dueDateValue, other.dueDateValue)
           .append("dueDateUnitId", this.dueDateUnitId, other.dueDateUnitId)
           .append("mainSubscription", this.mainSubscription, other.mainSubscription)
           .append("nextInvoiceDate", this.nextInvoiceDate, other.nextInvoiceDate)
           .append("invoiceTemplateId", this.invoiceTemplateId, other.invoiceTemplateId)
           .build();
    }
}
