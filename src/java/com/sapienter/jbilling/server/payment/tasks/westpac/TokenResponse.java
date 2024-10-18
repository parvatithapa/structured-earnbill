package com.sapienter.jbilling.server.payment.tasks.westpac;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class TokenResponse{
    private String singleUseTokenId;
    private String paymentMethod;
    private Map<String, Object> paymentInstrumentInfo;

    public String getSingleUseTokenId() {
        return singleUseTokenId;
    }

    public void setSingleUseTokenId(String singleUseTokenId) {
        this.singleUseTokenId = singleUseTokenId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Map<String, Object> getPaymentInstrumentInfo() {
        return paymentInstrumentInfo;
    }

    @JsonAnySetter
    public void setPaymentInstrumentProperty(String key, Object value) {
        if(null == paymentInstrumentInfo) {
            paymentInstrumentInfo = new HashMap<>();
        }
        paymentInstrumentInfo.put(key, value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TokenResponse [singleUseTokenId=");
        builder.append(singleUseTokenId);
        builder.append(", paymentMethod=");
        builder.append(paymentMethod);
        builder.append(", paymentInstrumentInfo=");
        builder.append(paymentInstrumentInfo);
        builder.append("]");
        return builder.toString();
    }
}
