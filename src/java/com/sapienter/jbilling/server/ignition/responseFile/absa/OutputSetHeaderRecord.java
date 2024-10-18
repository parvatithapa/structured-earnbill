package com.sapienter.jbilling.server.ignition.responseFile.absa;

import java.util.Date;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class OutputSetHeaderRecord {

    private String bankServUserCode;
    private String userBranchCode;
    private String userAccountNumber;
    private String userAccountType;

    private Date actionDate;

    public OutputSetHeaderRecord(String bankServUserCode, String userBranchCode, String userAccountNumber,
                               String userAccountType, Date actionDate) {
        this.bankServUserCode = bankServUserCode;
        this.userBranchCode = userBranchCode;
        this.userAccountNumber = userAccountNumber;
        this.userAccountType = userAccountType;
        this.actionDate = actionDate;
    }

    public String getBankServUserCode() {
        return bankServUserCode;
    }

    public void setBankServUserCode(String bankServUserCode) {
        this.bankServUserCode = bankServUserCode;
    }

    public String getUserBranchCode() {
        return userBranchCode;
    }

    public void setUserBranchCode(String userBranchCode) {
        this.userBranchCode = userBranchCode;
    }

    public String getUserAccountNumber() {
        return userAccountNumber;
    }

    public void setUserAccountNumber(String userAccountNumber) {
        this.userAccountNumber = userAccountNumber;
    }

    public String getUserAccountType() {
        return userAccountType;
    }

    public void setUserAccountType(String userAccountType) {
        this.userAccountType = userAccountType;
    }

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }
}
