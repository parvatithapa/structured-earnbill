package com.sapienter.jbilling.server.ignition.responseFile.absa;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class OutputTransactionRecord {

    private BigDecimal amount;

    private Date transmissionDate;

    private String transactionType;
    private String sequenceNumber;
    private String homingBranchCode;
    private String homingAccountNumber;
    private String homingAccountName;
    private String userReference;
    private String rejectionReason;
    private String rejectionQualifier;
    private String distributionSequenceNumber;

    public OutputTransactionRecord(BigDecimal amount, Date transmissionDate, String transactionType, String sequenceNumber, String homingBranchCode,
                                       String homingAccountNumber, String homingAccountName, String userReference, String rejectionReason, String rejectionQualifier, String distributionSequenceNumber) {
        this.amount = amount;
        this.transmissionDate = transmissionDate;
        this.transactionType = transactionType;
        this.sequenceNumber = sequenceNumber;
        this.homingBranchCode = homingBranchCode;
        this.homingAccountNumber = homingAccountNumber;
        this.homingAccountName = homingAccountName;
        this.userReference = userReference;
        this.rejectionReason = rejectionReason;
        this.rejectionQualifier = rejectionQualifier;
        this.distributionSequenceNumber = distributionSequenceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getTransmissionDate() {
        return transmissionDate;
    }

    public void setTransmissionDate(Date transmissionDate) {
        this.transmissionDate = transmissionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getHomingBranchCode() {
        return homingBranchCode;
    }

    public void setHomingBranchCode(String homingBranchCode) {
        this.homingBranchCode = homingBranchCode;
    }

    public String getHomingAccountNumber() {
        return homingAccountNumber;
    }

    public void setHomingAccountNumber(String homingAccountNumber) {
        this.homingAccountNumber = homingAccountNumber;
    }

    public String getHomingAccountName() {
        return homingAccountName;
    }

    public void setHomingAccountName(String homingAccountName) {
        this.homingAccountName = homingAccountName;
    }

    public String getUserReference() {
        return userReference;
    }

    public void setUserReference(String userReference) {
        this.userReference = userReference;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionQualifier() {
        return rejectionQualifier;
    }

    public void setRejectionQualifier(String rejectionQualifier) {
        this.rejectionQualifier = rejectionQualifier;
    }

    public String getDistributionSequenceNumber() {
        return distributionSequenceNumber;
    }

    public void setDistributionSequenceNumber(String distributionSequenceNumber) {
        this.distributionSequenceNumber = distributionSequenceNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OutputTransactionRecord{");
        sb.append("amount=").append(amount);
        sb.append(", transmissionDate=").append(transmissionDate);
        sb.append(", transactionType='").append(transactionType).append('\'');
        sb.append(", sequenceNumber='").append(sequenceNumber).append('\'');
        sb.append(", homingBranchCode='").append(homingBranchCode).append('\'');
        sb.append(", homingAccountNumber='").append(homingAccountNumber).append('\'');
        sb.append(", homingAccountName='").append(homingAccountName).append('\'');
        sb.append(", userReference='").append(userReference).append('\'');
        sb.append(", rejectionReason='").append(rejectionReason).append('\'');
        sb.append(", rejectionQualifier='").append(rejectionQualifier).append('\'');
        sb.append(", distributionSequenceNumber='").append(distributionSequenceNumber).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
