package com.sapienter.jbilling.server.ignition.responseFile.absa;

import java.util.Date;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class TransmissionHeader {

    private String recordStatus;
    private String userCode;
    private String transmissionNumber;
    private Date transmissionDate;

    public TransmissionHeader(String recordStatus, String userCode, String transmissionNumber, Date transmissionDate) {
        this.recordStatus = recordStatus;
        this.userCode = userCode;
        this.transmissionNumber = transmissionNumber;
        this.transmissionDate = transmissionDate;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus) {
        this.recordStatus = recordStatus;
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

    public Date getTransmissionDate() {
        return transmissionDate;
    }

    public void setTransmissionDate(Date transmissionDate) {
        this.transmissionDate = transmissionDate;
    }

    @Override
    public String toString() {
        return "TransmissionHeader{" +
                "recordStatus='" + recordStatus + '\'' +
                ", userCode='" + userCode + '\'' +
                ", transmissionNumber='" + transmissionNumber + '\'' +
                ", transmissionDate=" + transmissionDate +
                '}';
    }
}
