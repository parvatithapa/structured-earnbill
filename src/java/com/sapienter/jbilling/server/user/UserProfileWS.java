package com.sapienter.jbilling.server.user;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.Util;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "User Profile Data", description = "UserProfileWS Model")
public class UserProfileWS {

    private Integer accountId;
    private String status;
    private String customerName;
    private String languageDescription;
    private String currencyCode;
    private String invoiceTemplate;
    private boolean isInvoiceAsChild;
    private boolean useParentPricing;
    private String billingCycleUnit;
    private Integer billingCycleDay;
    private String accountType;
    private Integer parentId;
    private Date createDateTime;
    private String owingBalance;
    private Map<String,String> customerMetaFieldMap;
    private Map<String,String> customerAitMetaFieldMap;
    private Map<String,String> paymentInstrumentMap;
    private CustomerNoteWS[] notes;
    private Integer numberOfInvoices = 0;
    private Integer numberOfPayments = 0;
    private Integer numberOfUnbilledCallRecords = 0;

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    @ConvertToTimezone
    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(Date createDateTime) {
        this.createDateTime = createDateTime;
    }

    public String getOwingBalance() {
        return owingBalance;
    }

    @ApiModelProperty(value = "A real-time calculated owing balance (All Invoices - All Payments)")
    @JsonProperty(value = "owingBalance")
    public BigDecimal getOwingBalanceAsDecimal() {
        return Util.string2decimal(owingBalance);
    }

    public void setOwingBalance(String owingBalance) {
        this.owingBalance = owingBalance;
    }

    @JsonProperty("owingBalance")
    public void setOwingBalance(BigDecimal owingBalance) {
        this.owingBalance = (owingBalance != null ? owingBalance.toString() : null);
    }

    public Map<String, String> getCustomerMetaFieldMap() {
        return customerMetaFieldMap;
    }

    public void setCustomerMetaFieldMap(Map<String, String> customerMetaFieldMap) {
        this.customerMetaFieldMap = customerMetaFieldMap;
    }

    public Map<String, String> getCustomerAitMetaFieldMap() {
        return customerAitMetaFieldMap;
    }

    public void setCustomerAitMetaFieldMap(
            Map<String, String> customerAitMetaFieldMap) {
        this.customerAitMetaFieldMap = customerAitMetaFieldMap;
    }

    public  Map<String,String> getPaymentInstrumentMap() {
        return paymentInstrumentMap;
    }

    public void setPaymentInstrumentMap(
            Map<String,String> paymentInstrumentMap) {
        this.paymentInstrumentMap = paymentInstrumentMap;
    }

    public String getLanguageDescription() {
        return languageDescription;
    }

    public void setLanguageDescription(String languageDescription) {
        this.languageDescription = languageDescription;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isUseParentPricing() {
        return useParentPricing;
    }

    public void setUseParentPricing(boolean useParentPricing) {
        this.useParentPricing = useParentPricing;
    }

    public String getInvoiceTemplate() {
        return invoiceTemplate;
    }

    public void setInvoiceTemplate(String invoiceTemplate) {
        this.invoiceTemplate = invoiceTemplate;
    }

    public boolean isInvoiceAsChild() {
        return isInvoiceAsChild;
    }

    public void setInvoiceAsChild(boolean isInvoiceAsChild) {
        this.isInvoiceAsChild = isInvoiceAsChild;
    }

    public String getBillingCycleUnit() {
        return billingCycleUnit;
    }

    public void setBillingCycleUnit(String billingCycleUnit) {
        this.billingCycleUnit = billingCycleUnit;
    }

    public Integer getBillingCycleDay() {
        return billingCycleDay;
    }

    public void setBillingCycleDay(Integer billingCycleDay) {
        this.billingCycleDay = billingCycleDay;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public CustomerNoteWS[] getNotes() {
        return notes;
    }

    public void setNotes(CustomerNoteWS[] notes) {
        this.notes = notes;
    }

    public Integer getNumberOfInvoices() {
        return numberOfInvoices;
    }

    public void setNumberOfInvoices(Integer numberOfInvoices) {
        this.numberOfInvoices = numberOfInvoices;
    }

    public Integer getNumberOfPayments() {
        return numberOfPayments;
    }

    public void setNumberOfPayments(Integer numberOfPayments) {
        this.numberOfPayments = numberOfPayments;
    }

    public Integer getNumberOfUnbilledCallRecords() {
        return numberOfUnbilledCallRecords;
    }

    public void setNumberOfUnbilledCallRecords(Integer numberOfUnbilledCallRecords) {
        this.numberOfUnbilledCallRecords = numberOfUnbilledCallRecords;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserProfileWS [accountId=");
        builder.append(accountId);
        builder.append(", status=");
        builder.append(status);
        builder.append(", customerName=");
        builder.append(customerName);
        builder.append(", languageDescription=");
        builder.append(languageDescription);
        builder.append(", currencyCode=");
        builder.append(currencyCode);
        builder.append(", invoiceTemplate=");
        builder.append(invoiceTemplate);
        builder.append(", isInvoiceAsChild=");
        builder.append(isInvoiceAsChild);
        builder.append(", useParentPricing=");
        builder.append(useParentPricing);
        builder.append(", billingCycleUnit=");
        builder.append(billingCycleUnit);
        builder.append(", billingCycleDay=");
        builder.append(billingCycleDay);
        builder.append(", accountType=");
        builder.append(accountType);
        builder.append(", parentId=");
        builder.append(parentId);
        builder.append(", createDateTime=");
        builder.append(createDateTime);
        builder.append(", owingBalance=");
        builder.append(owingBalance);
        builder.append(", customerMetaFieldMap=");
        builder.append(customerMetaFieldMap);
        builder.append(", customerAitMetaFieldMap=");
        builder.append(customerAitMetaFieldMap);
        builder.append(", paymentInstrumentMap=");
        builder.append(paymentInstrumentMap);
        builder.append(", notes=");
        builder.append(notes);
        builder.append(", numberOfInvoices=");
        builder.append(numberOfInvoices);
        builder.append(", numberOfPayments=");
        builder.append(numberOfPayments);
        builder.append(", numberOfUnbilledCallRecords=");
        builder.append(numberOfUnbilledCallRecords);
        builder.append("]");
        return builder.toString();
    }

}
