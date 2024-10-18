package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.recycle.listener;

import com.sapienter.jbilling.server.mediation.listener.RecycleMediationJobListener;
import com.sapienter.jbilling.server.mediation.quantityRating.usage.RecycleMediationCacheProvider;

import java.util.Optional;

public class DtRecycleMediationJobListener extends RecycleMediationJobListener {

    private RecycleMediationCacheProvider errorRecordCache;

    @Override
    public Optional<RecycleMediationCacheProvider> recycleMediationCacheProvider() {
        return Optional.of(this.errorRecordCache);
    }

    @Override
    public void setRecycleMediationCacheProvider(RecycleMediationCacheProvider errorRecordCache) {
        this.errorRecordCache = errorRecordCache;
    }
}
