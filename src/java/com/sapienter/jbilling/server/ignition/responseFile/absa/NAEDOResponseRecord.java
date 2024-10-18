package com.sapienter.jbilling.server.ignition.responseFile.absa;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by taimoor on 7/25/17.
 */
public class NAEDOResponseRecord {

    public enum Identifier{
        REQUEST_RESPONSE_RECORD,
        RECALL_RESPONSE_RECORD,
        DISPUTE_RECORD,
        HOMEBACK_TRANSACTION_RECORD
    }

    private Identifier recordIdentifier;
    private String bankServUserCode;
    private String responseCode;
    private String userReference;
    private String contractReference;
    private String homingAccountName;
    private String homingBranchNumber;
    private String homingAccountNumber;

    private BigDecimal installmentAmount;

    private Date originalActionDate;
    private Date originalEffectiveDate;

    public NAEDOResponseRecord(Identifier recordIdentifier, String bankServUserCode, String responseCode, String userReference, String contractReference, String homingAccountName,
                                      String homingBranchNumber, String homingAccountNumber, BigDecimal installmentAmount,
                                      Date originalActionDate, Date originalEffectiveDate) {
        this.recordIdentifier = recordIdentifier;
        this.bankServUserCode = bankServUserCode;
        this.responseCode = responseCode;
        this.userReference = userReference;
        this.contractReference = contractReference;
        this.homingAccountName = homingAccountName;
        this.homingBranchNumber = homingBranchNumber;
        this.homingAccountNumber = homingAccountNumber;
        this.installmentAmount = installmentAmount;
        this.originalActionDate = originalActionDate;
        this.originalEffectiveDate = originalEffectiveDate;
    }

    public Identifier getRecordIdentifier() {
        return recordIdentifier;
    }

    public void setRecordIdentifier(Identifier recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }

    public String getBankServUserCode() {
        return bankServUserCode;
    }

    public void setBankServUserCode(String bankServUserCode) {
        this.bankServUserCode = bankServUserCode;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getHomingAccountName() {
        return homingAccountName;
    }

    public void setHomingAccountName(String homingAccountName) {
        this.homingAccountName = homingAccountName;
    }

    public String getHomingBranchNumber() {
        return homingBranchNumber;
    }

    public void setHomingBranchNumber(String homingBranchNumber) {
        this.homingBranchNumber = homingBranchNumber;
    }

    public String getHomingAccountNumber() {
        return homingAccountNumber;
    }

    public void setHomingAccountNumber(String homingAccountNumber) {
        this.homingAccountNumber = homingAccountNumber;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public void setInstallmentAmount(BigDecimal installmentAmount) {
        this.installmentAmount = installmentAmount;
    }

    public Date getOriginalActionDate() {
        return originalActionDate;
    }

    public void setOriginalActionDate(Date originalActionDate) {
        this.originalActionDate = originalActionDate;
    }

    public Date getOriginalEffectiveDate() {
        return originalEffectiveDate;
    }

    public void setOriginalEffectiveDate(Date originalEffectiveDate) {
        this.originalEffectiveDate = originalEffectiveDate;
    }

    public String getUserReference() {
        return userReference;
    }

    public void setUserReference(String userReference) {
        this.userReference = userReference;
    }

    public String getContractReference() {
        return contractReference;
    }

    public void setContractReference(String contractReference) {
        this.contractReference = contractReference;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NAEDOResponseRecord{");
        sb.append("recordIdentifier=").append(recordIdentifier);
        sb.append(", bankServUserCode='").append(bankServUserCode).append('\'');
        sb.append(", responseCode='").append(responseCode).append('\'');
        sb.append(", userReference='").append(userReference).append('\'');
        sb.append(", contractReference='").append(contractReference).append('\'');
        sb.append(", homingAccountName='").append(homingAccountName).append('\'');
        sb.append(", homingBranchNumber='").append(homingBranchNumber).append('\'');
        sb.append(", homingAccountNumber='").append(homingAccountNumber).append('\'');
        sb.append(", installmentAmount=").append(installmentAmount);
        sb.append(", originalActionDate=").append(originalActionDate);
        sb.append(", originalEffectiveDate=").append(originalEffectiveDate);
        sb.append('}');
        return sb.toString();
    }
}
