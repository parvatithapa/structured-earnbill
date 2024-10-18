package com.sapienter.jbilling;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.catalogue.DtPlanWS;

public class DtReserveInstanceCache {
    private static Cache<String, Object> reservedInstanceCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(Long.MAX_VALUE)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .<String, Object> build();
    public static final String RESERVE_CACHE_KEY = "Reserved-Instance-List-EntityId-";

    public static List<DtPlanWS> getReservedInstanceCache(String key){
        return (List<DtPlanWS>) reservedInstanceCache.getIfPresent(key);
    }

    public static  void setReservedInstanceCache(String key, List<DtPlanWS> value) {
        reservedInstanceCache.put(key, value);
    }

    public static void cleanUp(String key){
        reservedInstanceCache.asMap().remove(key);
    }
}
