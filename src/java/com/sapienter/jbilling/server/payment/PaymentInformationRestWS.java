package com.sapienter.jbilling.server.payment;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Neelabh
 * @since 23-Apr-2019
 */
@ApiModel(value = "Payment Instrument Data", description = "PaymentInformationRestWS Model")
public class PaymentInformationRestWS implements Serializable {

    private Integer id;
    private Integer userId;
    private Integer processingOrder;
    private Integer paymentMethodId;
    private Integer paymentMethodTypeId;
    private String cvv;
    private Map<String, Object> metaFields = new HashMap<>();

    
    @ApiModelProperty(value = "The id of the payment instrument")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The id of the user for which this payment info is defined")
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

    @ApiModelProperty(value = "The identifier of the payment card type")
    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    @ApiModelProperty(value = "The identifier of the payment method type")
    public Integer getPaymentMethodTypeId() {
        return paymentMethodTypeId;
    }

    public void setPaymentMethodTypeId(Integer paymentMethodTypeId) {
        this.paymentMethodTypeId = paymentMethodTypeId;
    }

    @ApiModelProperty(value = "payment cvv")
    public String getCvv() {
        return cvv;
    }


    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
    @ApiModelProperty(value = "Used to collect any type of metafield")
    public Map<String, Object> getMetaFields() {
        return metaFields;
    }

    @JsonAnySetter
    public void setMetaField(String name, Object value) {
        metaFields.put(name, value);
    }

    public void setMetaFields(Map<String, Object> metaFields) {
        this.metaFields = metaFields;
    }
}
