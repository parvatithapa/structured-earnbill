package com.sapienter.jbilling.server.spc.wookie.crm;

import com.fasterxml.jackson.annotation.JsonValue;

enum WookiePaymentStatus {

    PAID("Paid"),
    COMPLETED("Completed"),
    PENDING("Pending"),
    SCHEDULED("Scheduled"),
    REQUESTED("Requested"),
    CANCELLED("Cancelled"),
    REFUND("Refund"),
    UNPAID("Unpaid"),
    FAILED("Failed"),
    CREDIT("Credit"),
    CREDIT_USED("Credit-Used"),
    CREDIT_APPLIED("Credit Applied");

    WookiePaymentStatus(String status) {
        this.status = status;
    }

    private String status;

    @JsonValue
    public String getStatus() {
        return status;
    }

}
