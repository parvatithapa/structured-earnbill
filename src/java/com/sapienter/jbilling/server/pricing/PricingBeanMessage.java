package com.sapienter.jbilling.server.pricing;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PricingBeanMessage implements Serializable {

    enum Action {
        CREATE, UPDATE, REMOVE
    }

    private Action action;
    private PricingBeanRegisterType pricingBeanRegisterType;
    private Integer sourceId;


    private PricingBeanMessage(Action action, PricingBeanRegisterType pricingBeanRegisterType, Integer sourceId) {
        this.action = action;
        this.pricingBeanRegisterType = pricingBeanRegisterType;
        this.sourceId = sourceId;
    }

    public static PricingBeanMessage of(Action action, PricingBeanRegisterType pricingBeanType, Integer sourceId) {
        return new PricingBeanMessage(action, pricingBeanType, sourceId);
    }


    public Action getAction() {
        return action;
    }

    public PricingBeanRegisterType getPricingBeanRegisterType() {
        return pricingBeanRegisterType;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    @Override
    public String toString() {
        return "PricingBeanMessage [action=" + action + ", pricingBeanType="
                + pricingBeanRegisterType + ", sourceId=" + sourceId + "]";
    }

    public void registerBean() {
        pricingBeanRegisterType.registerBean(sourceId, action);
    }

}
