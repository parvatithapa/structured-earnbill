package com.sapienter.jbilling.server.payment.tasks.westpac;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PaymentResponse {

    private String transactionId;
    private String receiptNumber;
    private String responseCode;
    private String status;
    private String responseText;
    private String transactionType;
    private String transactionDateTime;
    private String settlementDate;
    private String declinedDate;
    private String orderNumber;
    private PayWayError error;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getDeclinedDate() {
        return declinedDate;
    }

    public void setDeclinedDate(String declinedDate) {
        this.declinedDate = declinedDate;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public PayWayError getError() {
        return error;
    }

    public void setError(PayWayError error) {
        this.error = error;
    }

    @JsonIgnore
    public boolean isPassed() {
        return responseCode.equals("00") || responseCode.equals("08") || responseCode.equals("G") ||
                (StringUtils.isNotEmpty(status) ? status.toLowerCase().contains("approved"): Boolean.FALSE);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PaymentResponse [transactionId=");
        builder.append(transactionId);
        builder.append(", receiptNumber=");
        builder.append(receiptNumber);
        builder.append(", responseCode=");
        builder.append(responseCode);
        builder.append(", status=");
        builder.append(status);
        builder.append(", responseText=");
        builder.append(responseText);
        builder.append(", transactionType=");
        builder.append(transactionType);
        builder.append(", transactionDateTime=");
        builder.append(transactionDateTime);
        builder.append(", settlementDate=");
        builder.append(settlementDate);
        builder.append(", declinedDate=");
        builder.append(declinedDate);
        builder.append(", orderNumber=");
        builder.append(orderNumber);
        builder.append("]");
        return builder.toString();
    }
}
