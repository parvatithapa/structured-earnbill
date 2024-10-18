package com.sapienter.jbilling.server.ignition.responseFile.absa;

import java.util.List;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class PaymentOutput {

    private TransmissionHeader transmissionHeader;
    private OutputUserHeaderRecord userHeaderRecord;
    private OutputSetHeaderRecord setHeaderRecord;
    private List<OutputTransactionRecord> transactionRecord;
    private OutputSetTrailerRecord setTrailerRecord;
    private OutputUserTrailerRecord userTrailerRecord;

    public PaymentOutput(TransmissionHeader transmissionHeader, OutputUserHeaderRecord userHeaderRecord,
                         List<OutputTransactionRecord> transactionRecord) {
        this.transmissionHeader = transmissionHeader;
        this.userHeaderRecord = userHeaderRecord;
        this.transactionRecord = transactionRecord;
    }

    public TransmissionHeader getTransmissionHeader() {
        return transmissionHeader;
    }

    public void setTransmissionHeader(TransmissionHeader transmissionHeader) {
        this.transmissionHeader = transmissionHeader;
    }

    public OutputUserHeaderRecord getUserHeaderRecord() {
        return userHeaderRecord;
    }

    public void setUserHeaderRecord(OutputUserHeaderRecord userHeaderRecord) {
        this.userHeaderRecord = userHeaderRecord;
    }

    public OutputSetHeaderRecord getSetHeaderRecord() {
        return setHeaderRecord;
    }

    public void setSetHeaderRecord(OutputSetHeaderRecord setHeaderRecord) {
        this.setHeaderRecord = setHeaderRecord;
    }

    public OutputSetTrailerRecord getSetTrailerRecord() {
        return setTrailerRecord;
    }

    public void setSetTrailerRecord(OutputSetTrailerRecord setTrailerRecord) {
        this.setTrailerRecord = setTrailerRecord;
    }

    public OutputUserTrailerRecord getUserTrailerRecord() {
        return userTrailerRecord;
    }

    public void setUserTrailerRecord(OutputUserTrailerRecord userTrailerRecord) {
        this.userTrailerRecord = userTrailerRecord;
    }

    public List<OutputTransactionRecord> getTransactionRecord() {
        return transactionRecord;
    }

    public void setTransactionRecord(List<OutputTransactionRecord> transactionRecord) {
        this.transactionRecord = transactionRecord;
    }
}
