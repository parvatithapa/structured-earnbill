package com.sapienter.jbilling.server.order;

import java.util.ArrayList;
import java.util.List;

public final class MediationEventResultList {

    private boolean isRolledBack = false;
    private final List<MediationEventResult> results;

    public MediationEventResultList(int size) {
        this.results = new ArrayList<>(size);
    }

    public boolean isRolledBack() {
        return isRolledBack;
    }

    public void setRolledBack(boolean rolledBack) {
        isRolledBack = rolledBack;
    }

    public List<MediationEventResult> getResults() {
        return results;
    }

    public void addResult(MediationEventResult result) {
        this.results.add(result);
    }

    public void clear() {
        this.results.clear();
    }
}
