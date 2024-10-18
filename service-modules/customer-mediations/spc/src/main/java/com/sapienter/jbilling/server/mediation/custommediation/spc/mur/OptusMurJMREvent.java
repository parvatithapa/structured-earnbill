package com.sapienter.jbilling.server.mediation.custommediation.spc.mur;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.system.event.Event;

public class OptusMurJMREvent implements Event {

    private JbillingMediationRecord jmr;

    public OptusMurJMREvent(JbillingMediationRecord jmr) {
        this.jmr = jmr;
    }

    @Override
    public Integer getEntityId() {
        return jmr.getjBillingCompanyId();
    }

    @Override
    public String getName() {
        return "OptusMurJMREvent";
    }

    public JbillingMediationRecord getJmr() {
        return jmr;
    }

}
