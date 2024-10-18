package com.sapienter.jbilling.server.mediation.custommediation.spc.mur;

import java.util.List;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.system.event.Event;

public class OptusMurJMREvent implements Event {

    private final List<JbillingMediationRecord> jmrs;
    private final Integer entityId;

    public OptusMurJMREvent(List<JbillingMediationRecord> jmrs, Integer entityId) {
        this.jmrs = jmrs;
        this.entityId = entityId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "OptusMurJMREvent";
    }

    public List<JbillingMediationRecord> getJmrs() {
        return jmrs;
    }

}
