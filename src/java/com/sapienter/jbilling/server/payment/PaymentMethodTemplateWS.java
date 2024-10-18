package com.sapienter.jbilling.server.payment;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Payment Method Template Data", description = "PaymentMethodTemplateWS Model")
public class PaymentMethodTemplateWS implements Serializable{
	
	private Integer id;
	private String templateName;
	
	private Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>(0);

	@ApiModelProperty(value = "The id of the payment method template entity")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "The name of the payment method template entity")
	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	@ApiModelProperty(value = "Meta-field names for the payment method template")
	public Set<MetaFieldWS> getMetaFields() {
		return metaFields;
	}

	public void setMetaFields(Set<MetaFieldWS> metaFields) {
		this.metaFields = metaFields;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PaymentMethodTemplateWS)) return false;

		PaymentMethodTemplateWS that = (PaymentMethodTemplateWS) o;

		return nullSafeEquals(id, that.id) &&
				nullSafeEquals(templateName, that.templateName) &&
				nullSafeEquals(metaFields, that.metaFields);
	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(id);
		result = 31 * result + nullSafeHashCode(templateName);
		result = 31 * result + nullSafeHashCode(metaFields);
		return result;
	}

	@Override
	public String toString() {
		return "PaymentMethodTemplateWS{" +
				"id=" + id +
				", templateName='" + templateName + '\'' +
				", metaFields=" + metaFields +
				'}';
	}
}
