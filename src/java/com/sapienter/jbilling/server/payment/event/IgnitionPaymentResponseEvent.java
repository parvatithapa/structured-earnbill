package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;

import java.util.List;
import java.util.Map;

/**
 * Created by wajeeha on 9/21/17.
 */
public class IgnitionPaymentResponseEvent extends AbstractPaymentEvent  {
    private String fileType= null;
    private String acbCode = null;
    private Integer fileSequenceNo = null;
    private IgnitionConstants.PaymentStatus paymentFailureType =null;
    private Map<Integer,String> transactionDetails=null;
    private String transmissionDate = null;

    public List<Integer> getPaymentIds() {
        return paymentIds;
    }

    private List<Integer> paymentIds = null;

    private String serviceProvider = null;

    public String getTransmissionDate() {
        return transmissionDate;
    }

    public void setTransmissionDate(String transmissionDate) {
        this.transmissionDate = transmissionDate;
    }

    public String getFileType() {
        return fileType;
    }

    public String getAcbCode() {
        return acbCode;
    }

    public Integer getFileSequenceNo() {
        return fileSequenceNo;
    }

    public IgnitionConstants.PaymentStatus getPaymentFailureType() {
        return paymentFailureType;
    }

    public Map<Integer, String> getTransactionDetails() {
        return transactionDetails;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public IgnitionPaymentResponseEvent(Integer entityId, String fileType,
                                        Integer fileSequenceNo, String acbCode, IgnitionConstants.PaymentStatus failureType,
                                        Map<Integer,String> transactionDetails, List<Integer> paymentIds, String serviceProvider) {
        super(entityId, null);
        this.fileType = fileType;
        this.fileSequenceNo = fileSequenceNo;
        this.paymentFailureType = failureType;
        this.acbCode = acbCode;
        this.transactionDetails = transactionDetails;
        this.paymentIds = paymentIds;
        this.serviceProvider = serviceProvider;
    }

    @Override
    public String getName() {
        return "Ignition Payment Response Event";
    }
}
