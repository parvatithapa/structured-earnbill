package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class UserSetStatus {

    private String serviceIndicator;
    private String bankServUserCode;
    private String userGenerationNumber;
    private String lastSequenceNumber;
    private String userSetStatus;

    public UserSetStatus(String serviceIndicator, String bankServUserCode, String userGenerationNumber, String lastSequenceNumber, String userSetStatus) {
        this.serviceIndicator = serviceIndicator;
        this.bankServUserCode = bankServUserCode;
        this.userGenerationNumber = userGenerationNumber;
        this.lastSequenceNumber = lastSequenceNumber;
        this.userSetStatus = userSetStatus;
    }

    public String getServiceIndicator() {
        return serviceIndicator;
    }

    public void setServiceIndicator(String serviceIndicator) {
        this.serviceIndicator = serviceIndicator;
    }

    public String getBankServUserCode() {
        return bankServUserCode;
    }

    public void setBankServUserCode(String bankServUserCode) {
        this.bankServUserCode = bankServUserCode;
    }

    public String getUserGenerationNumber() {
        return userGenerationNumber;
    }

    public void setUserGenerationNumber(String userGenerationNumber) {
        this.userGenerationNumber = userGenerationNumber;
    }

    public String getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public void setLastSequenceNumber(String lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }

    public String getUserSetStatus() {
        return userSetStatus;
    }

    public void setUserSetStatus(String userSetStatus) {
        this.userSetStatus = userSetStatus;
    }

    @Override
    public String toString() {
        return "UserSetStatus{" +
                "serviceIndicator='" + serviceIndicator + '\'' +
                ", bankServUserCode='" + bankServUserCode + '\'' +
                ", userGenerationNumber='" + userGenerationNumber + '\'' +
                ", lastSequenceNumber='" + lastSequenceNumber + '\'' +
                ", userSetStatus='" + userSetStatus + '\'' +
                '}';
    }
}
