package com.sapienter.jbilling.server.process.signup;

import java.io.Serializable;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

@SuppressWarnings("serial")
public class PaymentRequestWS implements Serializable {

    @Digits(integer = 12, fraction = 4, message = "validation.error.invalid.number.or.fraction.4.decimals")
    @NotNull(message="validation.error.notnull")
    private String amount;
    @NotNull(message="validation.error.notnull")
    private String transactionId;
    @NotNull(message="validation.error.notnull")
    private PaymentResult paymentResult;

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public PaymentResult getPaymentResult() {
        return paymentResult;
    }

    public void setPaymentResult(PaymentResult paymentResult) {
        this.paymentResult = paymentResult;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PaymentRequestWS [amount=");
        builder.append(amount);
        builder.append(", transactionId=");
        builder.append(transactionId);
        builder.append(", paymentResult=");
        builder.append(paymentResult);
        builder.append("]");
        return builder.toString();
    }
}
