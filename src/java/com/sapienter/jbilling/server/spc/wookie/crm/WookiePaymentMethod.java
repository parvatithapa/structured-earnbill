package com.sapienter.jbilling.server.spc.wookie.crm;

import com.fasterxml.jackson.annotation.JsonValue;

enum WookiePaymentMethod {

    CHECK("Check"), CREDIT_CARD("Credit Card"),
    BANK_ACCOUNT("Bank Account"), CASH("Cash"),
    PAYPAL("PayPal"), OTHER("Other"),
    VISA("Visa"), CREDIT("Credit");

    String methodName;

    WookiePaymentMethod(String methodName) {
        this.methodName = methodName;
    }

    @JsonValue
    String getMethodName() {
        return this.methodName;
    }
}
