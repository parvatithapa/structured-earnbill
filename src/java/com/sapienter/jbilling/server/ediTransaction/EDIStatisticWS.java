package com.sapienter.jbilling.server.ediTransaction;

/**
 * Created by vivek on 3/11/15.
 */
public class EDIStatisticWS {

    String transactionType;
    Long inbound;
    Long outbound;

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getInbound() {
        return inbound;
    }

    public void setInbound(Long inbound) {
        this.inbound = inbound;
    }

    public Long getOutbound() {
        return outbound;
    }

    public void setOutbound(Long outbound) {
        this.outbound = outbound;
    }

    Long getTotal(){
        return inbound + outbound;
    }
}
