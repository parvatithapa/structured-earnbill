package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.recycle.cache;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import com.sapienter.jbilling.server.mediation.quantityRating.usage.RecycleMediationCacheProvider;


public class DtRecycleErrorRecordCache implements RecycleMediationCacheProvider {

    private ConcurrentMap<String, BigDecimal> recordQuantityCache;

    private DtRecycleErrorRecordDataProvider dataProvider;

    @PostConstruct
    public void create() {
        recordQuantityCache = new ConcurrentHashMap<>(256, 0.8f, 1);
    }

    @Override
    public void init() {
        loadCache();
    }

    @Override
    public void reset() {
        recordQuantityCache.clear();
    }

    @Override
    public BigDecimal getResolvedQuantity(String key) {
        return recordQuantityCache.get(key);
    }

    private void loadCache() {
        Map<String,BigDecimal> records = dataProvider.getErrorRecords();
        recordQuantityCache.putAll(records);
    }

    @Override
    public boolean mightContain(String key) {
        return false;
    }

    public void setDataProvider(DtRecycleErrorRecordDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
}
