package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class PaymentReply {

    private TransmissionHeader transmissionHeader;
    private TransmissionStatus transmissionStatus;
    private TransmissionRejectedReason transmissionRejectedReason;
    private UserSetStatus userSetStatus;
    private RejectedMessage rejectedMessage;

    public PaymentReply(TransmissionHeader transmissionHeader, TransmissionStatus transmissionStatus,
                            TransmissionRejectedReason transmissionRejectedReason, UserSetStatus userSetStatus, RejectedMessage rejectedMessage) {

        this.transmissionHeader = transmissionHeader;
        this.transmissionStatus = transmissionStatus;
        this.transmissionRejectedReason = transmissionRejectedReason;
        this.userSetStatus = userSetStatus;
        this.rejectedMessage = rejectedMessage;
    }

    public TransmissionHeader getTransmissionHeader() {
        return transmissionHeader;
    }

    public void setTransmissionHeader(TransmissionHeader transmissionHeader) {
        this.transmissionHeader = transmissionHeader;
    }

    public TransmissionStatus getTransmissionStatus() {
        return transmissionStatus;
    }

    public void setTransmissionStatus(TransmissionStatus transmissionStatus) {
        this.transmissionStatus = transmissionStatus;
    }

    public TransmissionRejectedReason getTransmissionRejectedReason() {
        return transmissionRejectedReason;
    }

    public void setTransmissionRejectedReason(TransmissionRejectedReason transmissionRejectedReason) {
        this.transmissionRejectedReason = transmissionRejectedReason;
    }

    public UserSetStatus getUserSetStatus() {
        return userSetStatus;
    }

    public void setUserSetStatus(UserSetStatus userSetStatus) {
        this.userSetStatus = userSetStatus;
    }

    public RejectedMessage getRejectedMessage() {
        return rejectedMessage;
    }

    public void setRejectedMessage(RejectedMessage rejectedMessage) {
        this.rejectedMessage = rejectedMessage;
    }
}
