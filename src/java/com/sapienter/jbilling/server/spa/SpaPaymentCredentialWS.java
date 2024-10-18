package com.sapienter.jbilling.server.spa;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Created by pablo_galera on 10/01/17.
 */
public class SpaPaymentCredentialWS implements Serializable {
    private String ccname;
    private String ccyear;
    private String ccmonth;
    private String customerToken;
    private String ccnumber;
    private String paymentProfileId;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ccname: ").append(ccname);
        stringBuilder.append("\nccyear: ").append(ccyear);
        stringBuilder.append("\nccmonth: ").append(ccmonth);
        stringBuilder.append("\ncustomerToken: ").append(customerToken);
        stringBuilder.append("\nccnumber: ").append(ccnumber);
        stringBuilder.append("\npaymentProfileId: ").append(paymentProfileId);
        return stringBuilder.toString();
    }

    public String getCcname() {
        return ccname;
    }

    public void setCcname(String ccname) {
        this.ccname = ccname;
    }

    public String getCcyear() {
        return ccyear;
    }

    public void setCcyear(String ccyear) {
        this.ccyear = ccyear;
    }

    public String getCcmonth() {
        return ccmonth;
    }

    public void setCcmonth(String ccmonth) {
        this.ccmonth = ccmonth;
    }

    public String getCustomerToken() {
        return customerToken;
    }

    public void setCustomerToken(String customerToken) {
        this.customerToken = customerToken;
    }

    public String getCcnumber() {
        return ccnumber;
    }

    public void setCcnumber(String ccnumber) {
        this.ccnumber = ccnumber;
    }

    public String getPaymentProfileId() {
        return paymentProfileId;
    }

    public void setPaymentProfileId(String paymentProfileId) {
        this.paymentProfileId = paymentProfileId;
    }

    public boolean hasInvalidFields() {
        return StringUtils.isEmpty(ccname) || StringUtils.isEmpty(ccyear)
                || StringUtils.isEmpty(ccmonth) || StringUtils.isEmpty(customerToken)
                || StringUtils.isEmpty(paymentProfileId);
    }

}
