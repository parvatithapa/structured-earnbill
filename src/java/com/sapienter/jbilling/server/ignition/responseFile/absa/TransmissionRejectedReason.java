package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class TransmissionRejectedReason {

    private String errorCode;
    private String errorMessage;

    public TransmissionRejectedReason(String errorCode, String errorMessage) {

        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
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
        return "TransmissionRejectedReason{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
