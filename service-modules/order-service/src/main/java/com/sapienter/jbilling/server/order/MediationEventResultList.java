package com.sapienter.jbilling.server.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

public final class MediationEventResultList {

    private boolean isRolledBack = false;
    private final Map<JbillingMediationRecord, MediationEventResult> resultJmrRecordMap;

    public MediationEventResultList() {
        this.resultJmrRecordMap = new LinkedHashMap<>();
    }

    public boolean isRolledBack() {
        return isRolledBack;
    }

    public void setRolledBack(boolean rolledBack) {
        isRolledBack = rolledBack;
    }

    public List<MediationEventResult> results() {
        return new ArrayList<>(resultJmrRecordMap.values());
    }

    public void addResult(JbillingMediationRecord jmr, MediationEventResult result) {
        this.resultJmrRecordMap.put(jmr, result);
    }

    public void clear() {
        this.resultJmrRecordMap.clear();
    }

    public Map<JbillingMediationRecord, MediationEventResult> getResultJmrRecordMap() {
        return resultJmrRecordMap;
    }
}
