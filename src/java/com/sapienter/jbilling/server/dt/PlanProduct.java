package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.pricing.PriceModelWS;

public class PlanProduct{
    private Integer itemBundledQuantity;
    private Integer itemBundlePeriod;
    private Integer itemId;
    private PriceModelWS priceModelWS;

    public PlanProduct() {
    }

    public PlanProduct(Integer itemBundledQuantity, Integer itemBundlePeriod, Integer itemId) {
        this.itemBundledQuantity = itemBundledQuantity;
        this.itemBundlePeriod = itemBundlePeriod;
        this.itemId = itemId;
    }

    public Integer getItemBundledQuantity() {
        return itemBundledQuantity;
    }

    public void setItemBundledQuantity(Integer itemBundledQuantity) {
        this.itemBundledQuantity = itemBundledQuantity;
    }

    public Integer getItemBundlePeriod() {
        return itemBundlePeriod;
    }

    public void setItemBundlePeriod(Integer itemBundlePeriod) {
        this.itemBundlePeriod = itemBundlePeriod;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public PriceModelWS getPriceModelWS() {
        return priceModelWS;
    }

    public void setPriceModelWS(PriceModelWS priceModelWS) {
        this.priceModelWS = priceModelWS;
    }
}
