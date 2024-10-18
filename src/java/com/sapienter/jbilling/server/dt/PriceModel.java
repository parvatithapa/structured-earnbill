package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.pricing.PriceModelWS;

/**
 * Created by wajeeha on 12/18/17.
 */
public class PriceModel{
    private PriceModelWS priceModelWS;
    private boolean isChained;

    public PriceModelWS getPriceModelWS() {
        return priceModelWS;
    }

    public boolean isChained() {
        return isChained;
    }

    public void setPriceModelWS(PriceModelWS priceModelWS) {
        this.priceModelWS = priceModelWS;
    }

    public void setChained(boolean chained) {
        isChained = chained;
    }
}
