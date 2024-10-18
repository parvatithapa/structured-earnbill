package com.sapienter.jbilling.paymentUrl.domain;

import java.util.Map;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@ToString
public class PaymentConfiguration {
    private Map<String, String> paymentConfig;
    private Map<String, Object> paymentData;
}
