package com.sapienter.jbilling.server.boa.batch;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Javier Rivero
 * @since 05/01/16.
 */
public class BOAFileRecord {
    private Integer userId;
    private Integer transactionType;
    private String fundingAccountId;
    private BigDecimal amount;
    private String bankReferenceNo;
    private String custReferenceNo;
    private Date transactionDate;
    private int transactionTime;
    private String depositFileName;
    private String depositFileDirectory;
    private String rawData;

    private List<String> details;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal bigDecimal) {
        this.amount = bigDecimal;
    }

    public String getBankReferenceNo() {
        return bankReferenceNo;
    }

    public void setBankReferenceNo(String bankReferenceNo) {
        this.bankReferenceNo = bankReferenceNo;
    }

    public String getCustReferenceNo() {
        return custReferenceNo;
    }

    public void setCustReferenceNo(String custReferenceNo) {
        this.custReferenceNo = custReferenceNo;
    }

    public String getFundingAccountId() {
        return fundingAccountId;
    }

    public void setFundingAccountId(String fundingAccountId) {
        this.fundingAccountId = fundingAccountId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public int getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(int transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getDepositFileName() {
        return depositFileName;
    }

    public void setDepositFileName(String depositFileName) {
        this.depositFileName = depositFileName;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public String getDepositFileDirectory() {
        return depositFileDirectory;
    }

    public void setDepositFileDirectory(String depositFileDirectory) {
        this.depositFileDirectory = depositFileDirectory;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String toString() {
        return "date=" + transactionDate + "; time=" + transactionTime + "; customerAccountNo=" + fundingAccountId + "; amount=" + amount + "; transactionType=" + transactionType + "; bankReferenceNo=" + bankReferenceNo + "; custReferenceNo=" + custReferenceNo + "; details=" + details ;
    }
}
