package com.sapienter.jbilling.server.payment.tasks.stripe.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class Payment {
    private long amount;
    private String currencyCode;
}