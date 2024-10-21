package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;

public class AssetMediationEvaluationStrategyTask extends BasicMediationEvaluationStrategyTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected Date getActiveSinceDate(Map<Integer, Map<String, Date>> activeSinceDateMapByUser) {
        logger.debug("AssetMediationEvaluationStrategyTask.getActiveSinceDate()");
        Date activeSinceDate = null;
        if (StringUtils.isNotBlank(data.getAssetIdentifier())) {
            Map<String, Date> activeSinceDateMapByAsset = activeSinceDateMapByUser.getOrDefault(data.getUserId(), new HashMap<>());
            activeSinceDate = activeSinceDateMapByAsset.computeIfAbsent(data.getAssetIdentifier(), action ->
            new OrderBL().getSubscriptionOrderActiveSinceDate(data.getUserId(), data.getEventDate(), data.getAssetIdentifier()));
            activeSinceDateMapByAsset.put(data.getAssetIdentifier(), activeSinceDate);
            activeSinceDateMapByUser.put(data.getUserId(), activeSinceDateMapByAsset);
        }
        return activeSinceDate;
    }

    @Override
    protected String buildCacheKey(boolean newOrderPerMediationRun) {
        logger.debug("building cache key");
        OrderDTO subscriptionOrder = new OrderBL().findOrderByUserAssetIdentifierAndEventDate(data.getUserId(), data.getAssetIdentifier(),data.getEventDate());
        StringBuilder cacheKeyBuilder = new StringBuilder(data.getUserId().toString()).append(
                null != subscriptionOrder ? subscriptionOrder.getId().toString() : StringUtils.EMPTY).append(
                Util.truncateDate(data.getEventDate()));
        if (newOrderPerMediationRun) {
            cacheKeyBuilder.append(data.getMediationProcessId());
        }
        return cacheKeyBuilder.toString();
    }

}
