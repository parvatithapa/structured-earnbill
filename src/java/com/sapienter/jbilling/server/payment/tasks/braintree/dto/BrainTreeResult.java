package com.sapienter.jbilling.server.payment.tasks.braintree.dto;

import org.apache.commons.lang.StringUtils;

public class BrainTreeResult {

    private boolean succeseded;
    private String errorCode;
    private String transactionId;
    private String errorMessage;
    private String avs;
    private String cardNumber;
    private String cardType;
    private String expiryDate;
    private String paymentType;
    private String statusDesc;
    private String amount;

    public boolean isSucceseded() {
        return succeseded;
    }

    public void setSucceseded(boolean succeseded) {
        this.succeseded = succeseded;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAvs() {
        return avs;
    }

    public void setAvs(String avs) {
        this.avs = avs;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getErrorResponse() {
        StringBuilder response = new StringBuilder();
        response.append(StringUtils.isNotBlank(errorCode) ? " errorCode:" + errorCode + "," : "");
        response.append(StringUtils.isNotBlank(statusDesc) ? " statusDesc:" + statusDesc + "," : "");
        response.append(StringUtils.isNotBlank(errorMessage) ? " errorMessage:" + errorMessage : "");
        if (StringUtils.isNotBlank(response.toString())) {
            response.insert(0, "{error: {").append(" }}");
        }
        return response.toString();
    }

    @Override
    public String toString() {
        return "BrainTreeResult [succeseded=" + succeseded + ", errorCode=" + errorCode + ", transactionId="
                + transactionId + ", errorMessage=" + errorMessage + ", avs=" + avs + ", cardNumber=" + cardNumber
                + ", cardType=" + cardType + ", expiryDate=" + expiryDate + ", paymentType=" + paymentType
                + ", statusDesc=" + statusDesc + ", amount=" + amount + "]";
    }
}
