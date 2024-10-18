package com.sapienter.jbilling.subscribe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;



/**
 * Just a wrapper class for the ReservedInstanceResource - REST endpoints
 * It contains upgrade info needed to upgrade order
 * Instances of this class are passed in request body as JSON objects
 */

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DtSubscribeRequestPayload implements Serializable {

    String subscriptionId;
    private String planId;
    private Date activeSince;

    public DtSubscribeRequestPayload(){}

    public DtSubscribeRequestPayload(String subscriptionId, String planId, Date activeSince) {
        this.subscriptionId = subscriptionId;
        this.planId = planId;
        this.activeSince = activeSince;
    }

    public Date getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
