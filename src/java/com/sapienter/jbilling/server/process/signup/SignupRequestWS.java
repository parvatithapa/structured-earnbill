package com.sapienter.jbilling.server.process.signup;

import java.io.Serializable;
import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.sapienter.jbilling.server.user.UserWS;

@SuppressWarnings("serial")
public class SignupRequestWS implements Serializable {

    @NotNull(message="validation.error.notnull")
    @Valid
    private UserWS user;
    private String planCode;
    private String[] addonProductCodes;
    private String[] oneTimeCharges;
    @Digits(integer = 12, fraction = 4, message = "validation.error.invalid.number.or.fraction.4.decimals")
    private String creditAmount;
    @Valid
    private PaymentRequestWS paymentRequest;
    private Integer referringCustomerId;

    public UserWS getUser() {
        return user;
    }

    public void setUser(UserWS user) {
        this.user = user;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String[] getAddonProductCodes() {
        return addonProductCodes;
    }

    public void setAddonProductCodes(String[] addonProductCodes) {
        this.addonProductCodes = addonProductCodes;
    }

    public String[] getOneTimeCharges() {
        return oneTimeCharges;
    }

    public void setOneTimeCharges(String[] oneTimeCharges) {
        this.oneTimeCharges = oneTimeCharges;
    }

    public String getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(String creditAmount) {
        this.creditAmount = creditAmount;
    }

    public PaymentRequestWS getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(PaymentRequestWS paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public Integer getReferringCustomerId() {
        return referringCustomerId;
    }

    public void setReferringCustomerId(Integer referringCustomerId) {
        this.referringCustomerId = referringCustomerId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SignupRequestWS [user=");
        builder.append(user);
        builder.append(", planCode=");
        builder.append(planCode);
        builder.append(", addonProductCodes=");
        builder.append(Arrays.toString(addonProductCodes));
        builder.append(", oneTimeCharges=");
        builder.append(Arrays.toString(oneTimeCharges));
        builder.append(", creditAmount=");
        builder.append(creditAmount);
        builder.append(", paymentRequest=");
        builder.append(paymentRequest);
        builder.append(", referringCustomerId=");
        builder.append(referringCustomerId);
        builder.append("]");
        return builder.toString();
    }

}
