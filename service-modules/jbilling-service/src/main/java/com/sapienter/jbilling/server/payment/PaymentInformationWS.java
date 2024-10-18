package com.sapienter.jbilling.server.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.lang.AutoCloseable;
import java.lang.Override;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Payment Information Data", description = "PaymentInformationWS Model")
public class PaymentInformationWS implements WSSecured, Serializable, AutoCloseable {
	private Integer id;
	private Integer userId;
	private Integer processingOrder;
	private Integer paymentMethodTypeId;
	private Integer paymentMethodId;
    @ConvertToTimezone
	private Date createDateTime;
    @ConvertToTimezone
	private Date updateDateTime;

	@Valid
    private MetaFieldValueWS[] metaFields;
	// for worldpay payment

	private char[] cvv;
	private Map<String, String> metaFieldMap = new HashMap<>();

	public PaymentInformationWS() {

	}
	
	public PaymentInformationWS(Integer id, Integer processingOrder, Integer paymentMethodTypeId) {
		this.id = id;
		this.processingOrder = processingOrder;
		this.paymentMethodTypeId = paymentMethodTypeId;
	}

	@ApiModelProperty(value = "The unique identifier of this record")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "The id of the user for which this payment info is defined for")
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	@ApiModelProperty(value = "The processing order")
	public Integer getProcessingOrder() {
		return processingOrder;
	}

	public void setProcessingOrder(Integer processingOrder) {
		this.processingOrder = processingOrder;
	}

	@ApiModelProperty(value = "The identifier of the payment method type")
	public Integer getPaymentMethodTypeId() {
		return paymentMethodTypeId;
	}

	public void setPaymentMethodTypeId(Integer paymentMethodTypeId) {
		this.paymentMethodTypeId = paymentMethodTypeId;
	}

	@ApiModelProperty(value = "Meta-field values related to this payment info")
	public MetaFieldValueWS[] getMetaFields() {
		return metaFields;
	}
	
	public void setMetaFields(MetaFieldValueWS[] metaFields) {
		this.metaFields = metaFields;
	}

	@ApiModelProperty(value = "Meta-field values related to this payment info")
	public Map<String, String> getMetaFieldMap() {
        return metaFieldMap;
    }

    public void setMetaFieldMap(Map<String, String> metaFieldMap) {
        this.metaFieldMap = metaFieldMap;
    }

    @ApiModelProperty(value = "Identifier of the payment method")
	public Integer getPaymentMethodId() {
		return paymentMethodId;
	}

	public void setPaymentMethodId(Integer paymentMethodId) {
		this.paymentMethodId = paymentMethodId;
	}

    @ApiModelProperty(value = "payment cvv")
    public String getCvv() {
        return null != this.cvv ? new String(cvv) : StringUtils.EMPTY;
    }

	@ApiModelProperty(value = "Timestamp of when Payment Information was created")
    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(Date createDateTime) {
        this.createDateTime = createDateTime;
    }

	@ApiModelProperty(value = "Timestamp of when Payment Information was updated")
    public Date getUpdateDateTime() {
        return updateDateTime;
    }

    public void setUpdateDateTime(Date updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    public void setCvv(char[] cvv) {
        this.cvv = cvv;
    }

    public void setCvv(String cvv) {
        if (null != cvv) {
            this.cvv = cvv.toCharArray();
        }
    }

    @JsonIgnore
	public static Comparator<PaymentInformationWS> ProcessingOrderComparator =
	    	new Comparator<PaymentInformationWS>() {

				@Override
				public int compare(PaymentInformationWS o1,
						PaymentInformationWS o2) {
					Integer o1ProcessingOrder = o1.getProcessingOrder();
					Integer o2ProcessingOrder = o2.getProcessingOrder();
					
					return o1ProcessingOrder.compareTo(o2ProcessingOrder);
				}
			};

	@Override
	public String toString() {
		return "PaymentInformationWS{" +
				"id=" + id + "userId=" + userId +
				", processingOrder=" + processingOrder +
				", paymentMethodTypeId=" + paymentMethodTypeId +
				", paymentMethodId=" + paymentMethodId +
				", createDateTime=" + createDateTime +
				", updateDateTime=" + updateDateTime +
				", metaFields=" + Arrays.toString(metaFields) +
				'}';
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentInformationWS that = (PaymentInformationWS) o;

        if (! Arrays.equals(metaFields, that.metaFields)) return false;
        if (! nullSafeEquals(paymentMethodId, that.paymentMethodId)) return false;
        if (! nullSafeEquals(paymentMethodTypeId, that.paymentMethodTypeId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(paymentMethodTypeId);
        result = 31 * result + nullSafeHashCode(paymentMethodId);
        result = 31 * result + nullSafeHashCode(metaFields);
        return result;
    }

	@JsonIgnore
	public Integer getOwningEntityId() {
		return null;
	}

	@JsonIgnore
	public Integer getOwningUserId() {
		return userId;
	}

	@Override
	public void close() throws Exception {
        if(metaFields!=null) {
            for (MetaFieldValueWS metaFieldValue : metaFields) {
                if (metaFieldValue.getMetaField().getDataType().equals(DataType.CHAR)) {
                    metaFieldValue.clearCharValue();
                }
            }
        }
        if(this.cvv!=null)
            Arrays.fill(this.cvv, ' ');
	}
}
