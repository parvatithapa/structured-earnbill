package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.util.Date;

import org.springmodules.cache.CachingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.util.audit.EventLogger;

public class MediationEvaluationStrategyData {
    private EventLogger eLogger;
    private Integer userId;
    private Integer itemId;
    private Date eventDate;
    private String mediationProcessId;
    private String assetIdentifier;
    private OrderDTO subscriptionOrder;
    private CacheProviderFacade cache;
    private CachingModel cacheModel;

    public EventLogger geteLogger() {
        return eLogger;
    }

    public void seteLogger(EventLogger eLogger) {
        this.eLogger = eLogger;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getMediationProcessId() {
        return mediationProcessId;
    }

    public void setMediationProcessId(String mediationProcessId) {
        this.mediationProcessId = mediationProcessId;
    }

    public String getAssetIdentifier() {
        return assetIdentifier;
    }

    public void setAssetIdentifier(String assetIdentifier) {
        this.assetIdentifier = assetIdentifier;
    }

    public CacheProviderFacade getCache() {
        return cache;
    }

    public void setCache(CacheProviderFacade cache) {
        this.cache = cache;
    }

    public CachingModel getCacheModel() {
        return cacheModel;
    }

    public void setCacheModel(CachingModel cacheModel) {
        this.cacheModel = cacheModel;
    }

    public OrderDTO getSubscriptionOrder() {
        return subscriptionOrder;
    }

    public void setSubscriptionOrder(OrderDTO subscriptionOrder) {
        this.subscriptionOrder = subscriptionOrder;
    }

}
