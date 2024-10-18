package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by taimoor on 7/24/17.
 */
public class RejectedMessage {

    private String serviceIndicator;
    private String bankServUserCode;
    private String userCodeGenerationNumber;
    private String userSequenceNumber;
    private String errorCode;
    private String errorMessage;

    public RejectedMessage(String serviceIndicator, String bankServUserCode, String userCodeGenerationNumber,
                               String userSequenceNumber, String errorCode, String errorMessage) {
        this.serviceIndicator = serviceIndicator;
        this.bankServUserCode = bankServUserCode;
        this.userCodeGenerationNumber = userCodeGenerationNumber;
        this.userSequenceNumber = userSequenceNumber;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
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

    public String getUserCodeGenerationNumber() {
        return userCodeGenerationNumber;
    }

    public void setUserCodeGenerationNumber(String userCodeGenerationNumber) {
        this.userCodeGenerationNumber = userCodeGenerationNumber;
    }

    public String getUserSequenceNumber() {
        return userSequenceNumber;
    }

    public void setUserSequenceNumber(String userSequenceNumber) {
        this.userSequenceNumber = userSequenceNumber;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "RejectedMessage{" +
                "serviceIndicator='" + serviceIndicator + '\'' +
                ", bankServUserCode='" + bankServUserCode + '\'' +
                ", userCodeGenerationNumber='" + userCodeGenerationNumber + '\'' +
                ", userSequenceNumber='" + userSequenceNumber + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
