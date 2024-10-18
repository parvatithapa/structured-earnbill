/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.crm.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CRMPaymentMethod {

    CHECK("Check"), CREDIT_CARD("Credit Card"),
        BANK_ACCOUNT("Bank Account"), CASH("Cash"),
        PAYPAL("PayPal"), OTHER("Other"),
        VISA("Visa"), CREDIT("Credit"),
        MASTERCARD("MasterCard"), AMEX("AMEX");

    String methodName;

    CRMPaymentMethod(String methodName) {
        this.methodName = methodName;
    }

    @JsonValue
    public String getMethodName() {
        return this.methodName;
    }
}
