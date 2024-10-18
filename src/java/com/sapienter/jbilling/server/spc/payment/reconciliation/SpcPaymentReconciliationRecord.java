package com.sapienter.jbilling.server.spc.payment.reconciliation;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

public class SpcPaymentReconciliationRecord {
    private static final String BPAY_FIELD_NAME = "BPAY Ref";
    private static final String CARD_PAN = "CardPAN";
    private static final String CUSTOMER_BANK_ACCOUNT = "CustomerBankAccount";
    private static final String STATUS = "Status";
    private static final String RECEIPT_NUMBER = "ReceiptNumber";
    private static final String TRANSACTION_DATE_TIME = "TransactionDateTime";
    private static final String SETTLEMENT_DATE = "SettlementDate";
    static final String TRANSACTION_DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm";
    static final String SETTLEMENT_DATE_FORMAT = "yyyyMMdd";

    private Integer userId;
    private Integer entityId;
    private Map<String, String> paymentFields = new HashMap<>();

    public SpcPaymentReconciliationRecord(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Map<String, String> getPaymentFields() {
        return paymentFields;
    }

    public void setPaymentFields(Map<String, String> paymentFields) {
        this.paymentFields = paymentFields;
    }

    public void addPaymentField(String name, String value) {
        this.paymentFields.put(name, value);
    }

    public String getPaymentFieldByName(String name) {
        Assert.notNull(name, "please provide payment field name");
        return getPaymentFields().get(name);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpcPaymentReconciliationRecord [userId=");
        builder.append(userId);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append(", paymentFields=");
        builder.append(paymentFields);
        builder.append("]");
        return builder.toString();
    }

    public boolean isBPay() {
        String bpayRef = getPaymentFieldByName(BPAY_FIELD_NAME);
        return StringUtils.isNotEmpty(bpayRef);
    }

    public boolean isCreditPay() {
        String carPAN = getPaymentFieldByName(CARD_PAN);
        return StringUtils.isNotEmpty(carPAN);
    }

    public boolean isBankAccountPay() {
        String bankAccount = getPaymentFieldByName(CUSTOMER_BANK_ACCOUNT);
        return StringUtils.isNotEmpty(bankAccount);
    }

    public boolean isDeclined() {
        String status = getPaymentFieldByName(STATUS);
        Assert.hasLength(status, "status not found in fields " + paymentFields);
        return status.equalsIgnoreCase("Declined");
    }

    public String getTransactionId() {
        String receiptNumber = getPaymentFieldByName(RECEIPT_NUMBER);
        Assert.hasLength(receiptNumber, "ReceiptNumber not found in fields " + paymentFields);
        return receiptNumber;
    }

    public String getTransactionDateTime() {
        String txDateTime = getPaymentFieldByName(TRANSACTION_DATE_TIME);
        Assert.hasLength(txDateTime, "TransactionDateTime not found in fields " + paymentFields);
        return txDateTime;
    }

    public String getSettlementDate() {
        String settlementDate = getPaymentFieldByName(SETTLEMENT_DATE);
        Assert.hasLength(settlementDate, "settlementDate not found in fields " + paymentFields);
        return settlementDate;
    }

}
