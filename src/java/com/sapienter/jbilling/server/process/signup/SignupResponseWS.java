package com.sapienter.jbilling.server.process.signup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("serial")
public class SignupResponseWS implements Serializable {

    private Integer userId;
    private Integer paymentId;
    private String  paymentResponse;
    private String  paymentResult;
    private Map<String, String> additonalResponse;
    private List<String> errorResponses;

    public SignupResponseWS() {
        additonalResponse = new HashMap<>();
        errorResponses = new ArrayList<>();
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public void setPaymentResult(String paymentResult) {
        this.paymentResult = paymentResult;
    }

    public void setAdditonalResponse(Map<String, String> additonalResponse) {
        this.additonalResponse = additonalResponse;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public String getPaymentResult() {
        return paymentResult;
    }

    public Map<String, String> getAdditonalResponse() {
        return additonalResponse;
    }

    @JsonIgnore
    public void addAdditionalResponse(String key, String value) {
        additonalResponse.put(key, value);
    }

    public List<String> getErrorResponse() {
        return errorResponses;
    }

    public void setErrorResponse(List<String> errorResponses) {
        this.errorResponses = errorResponses;
    }

    @JsonIgnore
    public void addErrorResponse(String errorMessage) {
        errorResponses.add(errorMessage);
    }

    @JsonIgnore
    public boolean hasError() {
        return !errorResponses.isEmpty();
    }

    public String getPaymentResponse() {
        return paymentResponse;
    }

    public void setPaymentResponse(String paymentResponse) {
        this.paymentResponse = paymentResponse;
    }

    /**
     * Resets all fields of {@link SignupResponseWS} in case of failure except error Response field.
     * @param response
     */
    @JsonIgnore
    public void resetResponse() {
        setAdditonalResponse(Collections.emptyMap());
        setPaymentId(null);
        setUserId(null);
        setPaymentResult(null);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SignupResponseWS [userId=");
        builder.append(userId);
        builder.append(", paymentId=");
        builder.append(paymentId);
        builder.append(", paymentResopnse=");
        builder.append(paymentResponse);
        builder.append(", paymentResult=");
        builder.append(paymentResult);
        builder.append(", additonalResponse=");
        builder.append(additonalResponse);
        builder.append(", errorResponses=");
        builder.append(errorResponses);
        builder.append("]");
        return builder.toString();
    }
}
