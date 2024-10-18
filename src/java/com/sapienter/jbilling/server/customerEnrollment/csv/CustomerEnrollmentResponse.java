package com.sapienter.jbilling.server.customerEnrollment.csv;

import java.util.Date;
import java.util.List;

public class CustomerEnrollmentResponse {

    private List<String> brokerIds;
    private String ldc;
    private String accountNumber;
    private Code code;
    private String reason;
    private Date timestamp;

    public List<String> getBrokerIds() {
        return brokerIds;
    }

    public void setBrokerIds(List<String> brokerIds) {
        this.brokerIds = brokerIds;
    }

    public String getLdc() {
        return ldc;
    }

    public void setLdc(String ldc) {
        this.ldc = ldc;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public enum Code {
        L1, //Rejected by LDC - Inconsistent Data
        L2, //Rejected by LDC - Cannot Enroll
        PG, //Pending LDC Response
        SE, //Successful Enrollment
        V0, //Failed Level 0 Validation (Account number is missing)
        V1, //Failed Level 1 Validation (Incomplete or Invalid data)
        V2  //Failed Level 2 Validation (Business rule violation)
    }
}