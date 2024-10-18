package com.sapienter.jbilling.server.quantity.usage.service;

import com.sapienter.jbilling.server.mediation.cache.CacheProvider;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;


public interface IUsageRecordService extends CacheProvider {

    String DEFAULT_SERVICE_BEAN_NAME = "defaultUsageRecordService";

    Optional<IUsageRecord> getItemUsage(
            Integer itemId, Integer userId, Integer entityId, Date startDate,
            Date endDate, Supplier<String> key, String mediationProcessId);

    Optional<IUsageRecord> getItemResourceUsage(
            Integer itemId, Integer userId, Integer entityId, String itemResourceId,
            Date startDate, Date endDate, Supplier<String> key, String mediationProcessId);
}
