package com.sapienter.jbilling.server.item;

import java.io.Serializable;

/**
 * Created by Fernando Sivila on 17/01/17.
 */
public class SpaPlanWS implements Serializable {
    private PlanWS plan;
    private String[] supportedModems;
    private PlanWS[] optionalPlans;

    public SpaPlanWS(PlanWS plan, String[] supportedModems, PlanWS[] optionalPlans) {
        this.plan = plan;
        this.supportedModems = supportedModems;
        this.optionalPlans = optionalPlans;
    }

    public PlanWS getPlan() {
        return plan;
    }

    public void setPlan(PlanWS plan) {
        this.plan = plan;
    }

    public String[] getSupportedModems() {
        return supportedModems;
    }

    public void setSupportedModems(String[] supportedModems) {
        this.supportedModems = supportedModems;
    }

    public PlanWS[] getOptionalPlans() {
        return optionalPlans;
    }

    public void setOptionalPlans(PlanWS[] optionalPlans) {
        this.optionalPlans = optionalPlans;
    }
}
