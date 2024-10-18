package com.sapienter.jbilling.paymentUrl.domain.response;

import java.util.Map;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@ToString
public class PaymentResponse {
    private String responseCode;
    private Boolean responseStatus;
    private String responseMessage;
    private String merchantTransactionId;
    private String transactionId;
    private Map<String, String> metaData;
}
