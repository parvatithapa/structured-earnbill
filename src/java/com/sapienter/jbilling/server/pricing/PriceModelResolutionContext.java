package com.sapienter.jbilling.server.pricing;

import java.util.Date;
import java.util.Map;

public class PriceModelResolutionContext {
    
    private Integer userId;
    private Integer itemId;
    private Date pricingDate;
    private Map<String, String> attributes;
    private Boolean isMediatedOrder = false;

    private PriceModelResolutionContext() {}
    
    private PriceModelResolutionContext(Integer itemId) {
        this.itemId = itemId;
    }
    
    public static Builder builder(Integer itemId) {
        return new Builder(itemId);
    }
    
    public static class Builder {

        private PriceModelResolutionContext managedInstance;
        
        private Builder(Integer itemId) {
            managedInstance = new PriceModelResolutionContext(itemId);
        }
        
        public Builder user(Integer userId) {
            managedInstance.userId = userId;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            managedInstance.attributes = attributes;
            return this;
        }

        public Builder pricingDate(Date pricingDate) {
            managedInstance.pricingDate = pricingDate;
            return this;
        }

        public Builder isMediatedOrder(Boolean isMediatedOrder) {
            managedInstance.isMediatedOrder = isMediatedOrder;
            return this;
        }

        public PriceModelResolutionContext build() {
            return managedInstance;
        }
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Date getPricingDate() {
        return pricingDate;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Boolean getMediatedOrder() {
        return isMediatedOrder;
    }
}
