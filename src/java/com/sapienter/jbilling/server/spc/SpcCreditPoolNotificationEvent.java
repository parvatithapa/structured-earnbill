package com.sapienter.jbilling.server.spc;

import java.math.BigDecimal;
import java.util.List;

import lombok.ToString;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.system.event.Event;

@ToString
public class SpcCreditPoolNotificationEvent implements Event {

    private Integer entityId;
    private BigDecimal oldAmount;
    private BigDecimal newAmount;
    private SpcCreditPoolInfo creditPoolToUse;
    private List<BigDecimal> utilizedPercentages;
    private JbillingMediationRecord jmr;
    private Integer planOrderId;


    public SpcCreditPoolNotificationEvent(Integer entityId, BigDecimal oldAmount, BigDecimal newAmount,
            SpcCreditPoolInfo creditPoolToUse, List<BigDecimal> utilizedPercentages, JbillingMediationRecord jmr, Integer planOrderId) {
        this.entityId = entityId;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.creditPoolToUse = creditPoolToUse;
        this.utilizedPercentages = utilizedPercentages;
        this.jmr = jmr;
        this.planOrderId = planOrderId;
    }

    public BigDecimal getOldAmount() {
        return oldAmount;
    }

    public BigDecimal getNewAmount() {
        return newAmount;
    }

    public SpcCreditPoolInfo getCreditPoolToUse() {
        return creditPoolToUse;
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

    public Integer getPlanOrderId() {
        return planOrderId;
    }

    @Override
    public String getName() {
        return "SpcCreditPoolNotificationEvent-"+ getEntityId();
    }

}
