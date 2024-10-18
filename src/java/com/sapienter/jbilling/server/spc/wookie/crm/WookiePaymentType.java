package com.sapienter.jbilling.server.spc.wookie.crm;

import com.fasterxml.jackson.annotation.JsonValue;

enum WookiePaymentType {
    PAYMENT("payment"), REFUND("refund"), CREDIT("credit");

    private String type;

    WookiePaymentType(String type) {
        this.type = type;
    }

    @JsonValue
    String getType() {
        return this.type;
    }
}
