package com.sapienter.jbilling.server.user;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@ApiModel(value = "AccountType Data", description = "AccountTypeWS Model")
public class AccountTypeWS implements Serializable, WSSecured {

    private Integer id;
    private Integer entityId;
    private String invoiceDesign;
    private Integer invoiceTemplateId;
    @ConvertToTimezone
    private Date dateCreated;

    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number.12.decimal")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditNotificationLimit1;
    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number.12.decimal")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditNotificationLimit2;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number.12.decimal")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditLimit = null;

    @NotNull
    private Integer invoiceDeliveryMethodId;
    @NotNull
    private Integer currencyId;
    @NotNull
    private Integer languageId;

    @NotNull(message = "validation.error.is.required")
    @Size(min = 1, message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);

    @NotNull(message = "validation.error.is.required")
    @Valid
    private MainSubscriptionWS mainSubscription;

    // optional list of account information type ids
    private Integer[] informationTypeIds = null;

    private Integer[] paymentMethodTypeIds = new Integer[]{};
    
    private Integer[] preferedInformationTypeIds = null;


	public AccountTypeWS() {

    }

    public AccountTypeWS(Integer id, String creditLimit) {
        this.id = id;
        this.creditLimit = creditLimit;
    }

    public void setName(String name,Integer languageId) {
        if (!updateDescriptionsNameForLanguageIfAny(name, languageId)){
            InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, name);
            addDescription(description);
        }
    }

    private boolean updateDescriptionsNameForLanguageIfAny(String name, Integer languageId){
        for (InternationalDescriptionWS description : getDescriptions()){
            if(descriptionContainsNameForLanguage(description, languageId)){
                description.setContent(name);
                return true;
            }
        }
        return false;
    }

    private boolean descriptionContainsNameForLanguage(InternationalDescriptionWS description, Integer languageId){
        return languageId.equals(description.getLanguageId()) &&
                "description".equalsIgnoreCase(description.getPsudoColumn()) &&
                null != description.getContent() && !description.getContent().isEmpty();
    }

    @ApiModelProperty(value = "The id of the description language",
            required = true)
    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }
    @ApiModelProperty("The id of the company")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty("The name of the invoice design used")
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

    @ApiModelProperty("Creation date")
    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @JsonIgnore
    public String getCreditNotificationLimit1() {
        return creditNotificationLimit1;
    }

    @ApiModelProperty(value = "The credit limit for which the first notification happens",
            dataType = "BigDecimal")
    @JsonProperty(value = "creditNotificationLimit1")
    public BigDecimal getCreditNotificationLimit1AsDecimal() {
        return Util.string2decimal(creditNotificationLimit1);
    }

    @JsonIgnore
    public void setCreditNotificationLimit1AsDecimal(BigDecimal creditNotificationLimit1) {
        setCreditNotificationLimit1(creditNotificationLimit1);
    }

    @JsonIgnore
    public void setCreditNotificationLimit1(String creditNotificationLimit1) {
        this.creditNotificationLimit1 = creditNotificationLimit1;
    }

    @JsonProperty("creditNotificationLimit1")
    public void setCreditNotificationLimit1(BigDecimal creditNotificationLimit1) {
        this.creditNotificationLimit1 = (creditNotificationLimit1 != null ? creditNotificationLimit1.toString() : null);
    }

    @JsonIgnore
    public String getCreditNotificationLimit2() {
        return creditNotificationLimit2;
    }

    @ApiModelProperty(value = "The credit limit for which the second notification happens",
            dataType = "BigDecimal")
    @JsonProperty(value = "creditNotificationLimit2")
    public BigDecimal getCreditNotificationLimit2AsDecimal() {
        return Util.string2decimal(creditNotificationLimit2);
    }

    @JsonIgnore
    public void setCreditNotificationLimit2AsDecimal(BigDecimal creditNotificationLimit2) {
        setCreditNotificationLimit2(creditNotificationLimit2);
    }

    @JsonIgnore
    public void setCreditNotificationLimit2(String creditNotificationLimit2) {
        this.creditNotificationLimit2 = creditNotificationLimit2;
    }

    @JsonProperty("creditNotificationLimit2")
    public void setCreditNotificationLimit2(BigDecimal creditNotificationLimit2) {
        this.creditNotificationLimit2 = (creditNotificationLimit2 != null ? creditNotificationLimit2.toString() : null);
    }

    @ApiModelProperty(value = "The id of the invoice delivery method used",
            allowableValues = "1(Email), 2(Paper), 3(Email and Paper), 4(None)",
            required = true)
    public Integer getInvoiceDeliveryMethodId() {
        return invoiceDeliveryMethodId;
    }

    public void setInvoiceDeliveryMethodId(Integer invoiceDeliveryMethodId) {
        this.invoiceDeliveryMethodId = invoiceDeliveryMethodId;
    }

    @ApiModelProperty(value = "The id of the currency used", required = true)
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(required = true, value = "Main subscription template for all the customers in this account type")
    public MainSubscriptionWS getMainSubscription() {
        return mainSubscription;
    }

    public void setMainSubscription(MainSubscriptionWS mainSubscription) {
        this.mainSubscription = mainSubscription;
    }

    @ApiModelProperty("The id of the account type entity")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @JsonIgnore
    public String getCreditLimit() {
        return creditLimit;
    }

    @ApiModelProperty(value = "Credit limit for all the customers in this account type",
            dataType = "BigDecimal")
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

    @ApiModelProperty(value = "Array of all descriptions regarding this account type",
            required = true)
    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public void addDescription(InternationalDescriptionWS description) {
        this.descriptions.add(description);
    }

    @JsonIgnore
    public InternationalDescriptionWS getDescription(Integer languageId) {
        for (InternationalDescriptionWS description : descriptions)
            if (description.getLanguageId().equals(languageId))
                return description;
        return null;
    }

    @ApiModelProperty(value = "The id of the AITs used in this account type")
    public Integer[] getInformationTypeIds() {
        return informationTypeIds;
    }

    public void setInformationTypeIds(Integer[] informationTypeIds) {
        this.informationTypeIds = informationTypeIds;
    }

    @ApiModelProperty(value = "The id of the payment method types used in this account type")
    public Integer[] getPaymentMethodTypeIds() {
    	return paymentMethodTypeIds;
    }
    
    public void setPaymentMethodTypeIds(Integer[] paymentMethodTypeIds) {
    	this.paymentMethodTypeIds = paymentMethodTypeIds;
    }

    public Integer[] getPreferedInformationTypeIds() {
	        return preferedInformationTypeIds;
	}

    public void setPreferedInformationTypeIds(Integer[] preferedInformationTypeIds) {
	this.preferedInformationTypeIds = preferedInformationTypeIds;
	}

    @Override
    public String toString() {
        return "AccountTypeWS{" +
                "id=" + id +
                ", entityId=" + entityId +
                ", invoiceDesign='" + invoiceDesign + '\'' +
                ", invoiceTemplateId='" + invoiceTemplateId + '\'' +
                ", dateCreated=" + dateCreated +
                ", creditNotificationLimit1='" + creditNotificationLimit1 + '\'' +
                ", creditNotificationLimit2='" + creditNotificationLimit2 + '\'' +
                ", creditLimit='" + creditLimit + '\'' +
                ", invoiceDeliveryMethodId=" + invoiceDeliveryMethodId +
                ", currencyId=" + currencyId +
                ", languageId=" + languageId +
                ", descriptions=" + descriptions +
                ", mainSubscription=" + mainSubscription +
                '}';
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountTypeWS that = (AccountTypeWS) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
        if (invoiceDesign != null ? !invoiceDesign.equals(that.invoiceDesign) : that.invoiceDesign != null)
            return false;
        if (invoiceTemplateId != null ? !invoiceTemplateId.equals(that.invoiceTemplateId) : that.invoiceTemplateId != null)
            return false;
        if (dateCreated != null ? !dateCreated.equals(that.dateCreated) : that.dateCreated != null) return false;
        if (creditNotificationLimit1 != null ? !creditNotificationLimit1.equals(that.creditNotificationLimit1) : that.creditNotificationLimit1 != null)
            return false;
        if (creditNotificationLimit2 != null ? !creditNotificationLimit2.equals(that.creditNotificationLimit2) : that.creditNotificationLimit2 != null)
            return false;
        if (creditLimit != null ? !creditLimit.equals(that.creditLimit) : that.creditLimit != null) return false;
        if (invoiceDeliveryMethodId != null ? !invoiceDeliveryMethodId.equals(that.invoiceDeliveryMethodId) : that.invoiceDeliveryMethodId != null)
            return false;
        if (currencyId != null ? !currencyId.equals(that.currencyId) : that.currencyId != null) return false;
        if (languageId != null ? !languageId.equals(that.languageId) : that.languageId != null) return false;
        if (descriptions != null ? !descriptions.equals(that.descriptions) : that.descriptions != null) return false;
        if (mainSubscription != null ? !mainSubscription.equals(that.mainSubscription) : that.mainSubscription != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(informationTypeIds, that.informationTypeIds)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(paymentMethodTypeIds, that.paymentMethodTypeIds);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (invoiceDesign != null ? invoiceDesign.hashCode() : 0);
        result = 31 * result + (invoiceTemplateId != null ? invoiceTemplateId.hashCode() : 0);
        result = 31 * result + (dateCreated != null ? dateCreated.hashCode() : 0);
        result = 31 * result + (creditNotificationLimit1 != null ? creditNotificationLimit1.hashCode() : 0);
        result = 31 * result + (creditNotificationLimit2 != null ? creditNotificationLimit2.hashCode() : 0);
        result = 31 * result + (creditLimit != null ? creditLimit.hashCode() : 0);
        result = 31 * result + (invoiceDeliveryMethodId != null ? invoiceDeliveryMethodId.hashCode() : 0);
        result = 31 * result + (currencyId != null ? currencyId.hashCode() : 0);
        result = 31 * result + (languageId != null ? languageId.hashCode() : 0);
        result = 31 * result + (descriptions != null ? descriptions.hashCode() : 0);
        result = 31 * result + (mainSubscription != null ? mainSubscription.hashCode() : 0);
        result = 31 * result + (informationTypeIds != null ? Arrays.hashCode(informationTypeIds) : 0);
        result = 31 * result + (paymentMethodTypeIds != null ? Arrays.hashCode(paymentMethodTypeIds) : 0);
        return result;
    }
}
