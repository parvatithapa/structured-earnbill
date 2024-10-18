package com.sapienter.jbilling.server.customer;

import java.io.Serializable;

import com.sapienter.jbilling.server.security.WSSecured;

/**
 * This class is to hold the response from process signup payment API
 * 
 * @author ashwin 
 * @since 05/06/2018
 *
 */
public class CustomerSignupResponseWS implements WSSecured, Serializable{

    private Integer userId;
    private Integer result;
    private Integer paymentId;
    private String  responseCode;
    private String  responseMessage;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    @Override
    public String toString() {
        return "CustomerSignupResponseWS [userId=" + userId + ", result="
                + result + ", paymentId=" + paymentId + ", responseCode="
                + responseCode + ", responseMessage=" + responseMessage + "]";
    }

    @Override
    public Integer getOwningEntityId() {
        return null;
    }

    @Override
    public Integer getOwningUserId() {
        return getUserId();
    }

}
