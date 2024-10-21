package com.sapienter.jbilling.server.spc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.ToString;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.system.event.Event;

@ToString
public class OptusMurNotificationEvent implements Event {

    private Integer entityId;
    private BigDecimal oldUsageQuanity;
    private BigDecimal newUsageQuanity;
    private List<BigDecimal> utilizedPercentages;
    private JbillingMediationRecord jmr;
    private Map<String, String> notificationParams;
    private String currentUsagePoolName;
    private Integer subscriptionOrderId;
    private Integer userId;

    public OptusMurNotificationEvent(Integer entityId, BigDecimal oldUsageQuanity, BigDecimal newUsageQuanity,
            List<BigDecimal> utilizedPercentages, JbillingMediationRecord jmr,
            Map<String, String> notificationParams, String currentUsagePoolName,
            Integer subscriptionOrderId, Integer userId) {
        this.entityId = entityId;
        this.oldUsageQuanity = oldUsageQuanity;
        this.newUsageQuanity = newUsageQuanity;
        this.utilizedPercentages = utilizedPercentages;
        this.jmr = jmr;
        this.notificationParams = notificationParams;
        this.currentUsagePoolName = currentUsagePoolName;
        this.subscriptionOrderId = subscriptionOrderId;
        this.userId = userId;
    }

    public BigDecimal getOldUsageQuanity() {
        return oldUsageQuanity;
    }

    public BigDecimal getNewUsageQuanity() {
        return newUsageQuanity;
    }

    public List<BigDecimal> getUtilizedPercentages() {
        return utilizedPercentages;
    }


    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public JbillingMediationRecord getJmr() {
        return jmr;
    }

    public Map<String, String> getNotificationParams() {
        return notificationParams;
    }

    public String getCurrentUsagePoolName() {
        return currentUsagePoolName;
    }

    @Override
    public String getName() {
        return "OptusMurNotificationEvent-"+ getEntityId();
    }

    public Integer getSubscriptionOrderId() {
        return subscriptionOrderId;
    }
    
    public Integer getUserId() {
        return userId;
    }

}
