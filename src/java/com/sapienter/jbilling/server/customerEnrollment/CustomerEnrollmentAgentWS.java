package com.sapienter.jbilling.server.customerEnrollment;

/**
 *
 */
public class CustomerEnrollmentAgentWS {
    private String brokerId;
    private String rate;
    private String partnerName;
    private Integer partnerId;

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }
}
