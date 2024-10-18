package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class TransmissionStatus {

    private String userCode;
    private String transmissionNumber;
    private String transmissionStatus;

    public TransmissionStatus(String userCode, String transmissionNumber, String transmissionStatus) {
        this.userCode = userCode;
        this.transmissionNumber = transmissionNumber;
        this.transmissionStatus = transmissionStatus;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getTransmissionNumber() {
        return transmissionNumber;
    }

    public void setTransmissionNumber(String transmissionNumber) {
        this.transmissionNumber = transmissionNumber;
    }

    public String getTransmissionStatus() {
        return transmissionStatus;
    }

    public void setTransmissionStatus(String transmissionStatus) {
        this.transmissionStatus = transmissionStatus;
    }

    @Override
    public String toString() {
        return "TransmissionStatus{" +
                "userCode='" + userCode + '\'' +
                ", transmissionNumber='" + transmissionNumber + '\'' +
                ", transmissionStatus='" + transmissionStatus + '\'' +
                '}';
    }
}
