package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.Util;

public class BasicMediationEvaluationStrategyTask extends AbstractMediationEvaluationStrategyTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected Date calculateActualEventDate() {
        logger.debug("BasicMediationEvaluationStrategyTask.calculateActualEventDate()");
        return data.getEventDate();
    }

    protected String buildCacheKey(boolean newOrderPerMediationRun) {
        logger.debug("building cache key");
        StringBuilder cacheKeyBuilder = new StringBuilder(data.getUserId().toString()).append(Util.truncateDate(data.getEventDate()));
        if(newOrderPerMediationRun) {
            cacheKeyBuilder.append(data.getMediationProcessId());
        }
        return cacheKeyBuilder.toString();
    }

    protected Date getActiveSinceDate(Map<Integer, Map<String,Date>> activeSinceDateMapByUser) {
        Date activeSinceDate = null;
        if (null != data.getItemId()){
            Map<String, Date> activeSinceDateMapByItem = activeSinceDateMapByUser.getOrDefault(data.getUserId(), new HashMap<>());
            activeSinceDate = activeSinceDateMapByItem.computeIfAbsent(String.valueOf(data.getItemId()), action ->
            orderBL.getSubscriptionOrderActiveSinceDateByUsageItem(data.getUserId(), data.getItemId()));
            activeSinceDateMapByItem.put(String.valueOf(data.getItemId()), activeSinceDate);
            activeSinceDateMapByUser.put(data.getUserId(), activeSinceDateMapByItem);
        }
        return activeSinceDate;
    }
}
