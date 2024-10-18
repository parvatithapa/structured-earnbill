package com.sapienter.jbilling.server.quantity.usage.service;

import static com.sapienter.jbilling.server.util.QuantityRatingConstants.USAGE_USECACHE_PROPERTY;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.util.QuantityRatingProperties;


public class UsageRecordService implements IUsageRecordService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final boolean useCache;
    private Cache<String, Optional<IUsageRecord>> usageCache;

    static {
        String str = QuantityRatingProperties.get(USAGE_USECACHE_PROPERTY, "false");
        useCache = Boolean.valueOf(str);
    }

    @PostConstruct
    @Override
    public void init() {
        usageCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .build();
    }

    @Override
    public Optional<IUsageRecord> getItemUsage(
            Integer itemId,
            Integer userId,
            Integer entityId,
            Date startDate,
            Date endDate,
            Supplier<String> key,
            String mediationProcessId) {

        UsageRecordBL bl = new UsageRecordBL();
        return getUsage(key,
                () -> Optional.ofNullable(bl.getItemUsage(itemId, userId,
                        entityId, startDate, endDate, mediationProcessId)));
    }

    @Override
    public Optional<IUsageRecord> getItemResourceUsage(
            Integer itemId,
            Integer userId,
            Integer entityId,
            String itemResourceId,
            Date startDate, Date endDate,
            Supplier<String> key,
            String mediationProcessId) {

        UsageRecordBL bl = new UsageRecordBL();
        return getUsage(key,
                () -> Optional.ofNullable(bl.getItemResourceUsage(itemId, userId,
                        entityId, itemResourceId, startDate, endDate, mediationProcessId)));
    }

    private Optional<IUsageRecord> getUsage(
            Supplier<String> key,
            Callable<Optional<IUsageRecord>> callable) {

        try {
            if (useCache) {
                return usageCache.get(key.get(), callable);
            } else {
                return callable.call();
            }
        } catch (Exception e) {
            logger.error("Fatal: error retrieving usage data", e);
            return Optional.empty();
        }
    }

    @Override
    public void reset() {
        if (useCache) {
            usageCache.invalidateAll();
        }
    }
}
