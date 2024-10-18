package com.sapienter.jbilling.server.payment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;

import com.sapienter.jbilling.server.metafields.MetaFieldWS;

import lombok.ToString;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ToString
@ApiModel(value = "Payment Method Type Data", description = "PaymentMethodTypeWS Model")
public class PaymentMethodTypeWS implements Serializable, WSSecured {
	
	private Integer id;
    @Size(max=20,message = "validation.error.length.max,20")
    @NotEmpty(message = "validation.error.is.required")
	private String methodName;
	private Boolean isRecurring;
	private Boolean allAccountType;
	private Integer templateId;
	
	private List<Integer> accountTypes = new ArrayList<Integer>();
	
	@NotNull(message = "validation.error.notnull")
    @NotEmpty(message = "validation.error.notnull")
    @Valid
	private MetaFieldWS[] metaFields;

    private Integer owningEntityId;

	@ApiModelProperty(value = "The id of the payment method type entity")
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "Name of the payment method type. Some examples include, Credit Card, Direct Debit, and Cheque", required = true)
	public String getMethodName() {
		return methodName;
	}

	@ApiModelProperty(value = "If you want the payment method to apply (or be available to) all existing account types this flag should be set")
	@JsonProperty(value = "allAccountType")
	public Boolean isAllAccountType() {
		return allAccountType;
	}

	public void setAllAccountType(Boolean isAllAccountType) {
		this.allAccountType = isAllAccountType;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@ApiModelProperty(value = "Flag to set if the payment method type is recurring. Recurring payment methods are ones that a customer can choose to use repeatedly such as a credit card or ACH")
	public Boolean getIsRecurring() {
		return isRecurring;
	}
	
	public void setIsRecurring(Boolean isRecurring) {
		this.isRecurring = isRecurring;
	}

	@ApiModelProperty(value = "ID of the payment method template used in this payment method type.")
	public Integer getTemplateId() {
		return templateId;
	}
	
	public void setTemplateId(Integer templateId) {
		this.templateId = templateId;
	}

	@ApiModelProperty(value = "List of IDs of account types that will use this payment method. It is possible to choose one, several , or all")
	public List<Integer> getAccountTypes() {
		return accountTypes;
	}
	
	public void setAccountTypes(List<Integer> accountTypes) {
		this.accountTypes = accountTypes;
	}

	@ApiModelProperty(value = "Array of meta fields configured for this payment type", required = true)
	public MetaFieldWS[] getMetaFields() {
		return metaFields;
	}
	
	public void setMetaFields(MetaFieldWS[] metaFields) {
		this.metaFields = metaFields;
	}

    @Override
	@JsonIgnore
    public Integer getOwningEntityId() {
        return this.owningEntityId;
    }

	public void setOwningEntityId(Integer owningEntityId) {
		this.owningEntityId = owningEntityId;
	}

	@JsonIgnore
	@Override
    public Integer getOwningUserId() {
        return null;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PaymentMethodTypeWS)) return false;

		PaymentMethodTypeWS that = (PaymentMethodTypeWS) o;
		return nullSafeEquals(id, that.id) &&
				nullSafeEquals(methodName, that.methodName)&&
				nullSafeEquals(isRecurring, that.isRecurring)&&
				nullSafeEquals(allAccountType, that.allAccountType)&&
				nullSafeEquals(templateId, that.templateId)&&
				nullSafeEquals(accountTypes, that.accountTypes)&&
				nullSafeEquals(metaFields, that.metaFields)&&
				nullSafeEquals(owningEntityId, that.owningEntityId);

	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(id);
		result = 31 * result + nullSafeHashCode(methodName);
		result = 31 * result + nullSafeHashCode(isRecurring);
		result = 31 * result + nullSafeHashCode(allAccountType);
		result = 31 * result + nullSafeHashCode(templateId);
		result = 31 * result + nullSafeHashCode(accountTypes);
		result = 31 * result + nullSafeHashCode(metaFields);
		result = 31 * result + nullSafeHashCode(owningEntityId);
		return result;
	}
}