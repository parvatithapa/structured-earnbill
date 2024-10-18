package com.sapienter.jbilling.server.spc.wookie.crm;

import com.fasterxml.jackson.annotation.JsonValue;

enum WookieInvoiceStatus {

    AUTO_CREATED("AutoCreated"),
    CANCEL("Cancel"),
    CREATED("Created"),
    PARTIALLY_PAID("Partially Paid"),
    APPROVED("Approved"),
    SENT("Sent"),
    CREDIT_INVOICE("Credit Invoice"),
    PAID("Paid");

    WookieInvoiceStatus(String status) {
        this.status = status;
    }

    private String status;

    @JsonValue
    String getStatus() {
        return status;
    }

}
