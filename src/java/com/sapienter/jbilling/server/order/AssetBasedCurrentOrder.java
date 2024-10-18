/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.order;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.db.OrderDTO;

public class AssetBasedCurrentOrder extends CurrentOrder {

    private final String assetIdentifier;

    public AssetBasedCurrentOrder(Integer userId, Date eventDate, Integer itemId, String mediationProcessId,
            Integer entityId, String assetIdentifier) {
        super(userId, eventDate, itemId, mediationProcessId, entityId);
        this.assetIdentifier = assetIdentifier;
    }

    @Override
    protected Date getActiveSinceDate(Map<Integer, Map<String, Date>> activeSinceDateMapByUser) {
        Date activeSinceDate = null;
        if (StringUtils.isNotBlank(assetIdentifier)) {
            Map<String, Date> activeSinceDateMapByAsset = activeSinceDateMapByUser.getOrDefault(userId, new HashMap<>());
            activeSinceDate = activeSinceDateMapByAsset.computeIfAbsent(assetIdentifier, action -> orderBl.getSubscriptionOrderActiveSinceDate(userId, eventDate, assetIdentifier));
            activeSinceDateMapByAsset.put(assetIdentifier, activeSinceDate);
            activeSinceDateMapByUser.put(userId, activeSinceDateMapByAsset);
        }
        return activeSinceDate;
    }

    @Override
    protected String buildCacheKey(boolean newOrderPerMediationRun) {
        OrderDTO subscriptionOrder = new OrderBL().findOrderByUserAndAssetIdentifier(userId, assetIdentifier);
        StringBuilder cacheKeyBuilder = new StringBuilder(userId.toString()).append(
                null != subscriptionOrder ? subscriptionOrder.getId().toString() : StringUtils.EMPTY).append(
                Util.truncateDate(eventDate));
        if (newOrderPerMediationRun) {
            cacheKeyBuilder.append(mediationProcessId);
        }
        return cacheKeyBuilder.toString();
    }
}
