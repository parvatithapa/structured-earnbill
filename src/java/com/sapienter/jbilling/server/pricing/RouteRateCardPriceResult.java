package com.sapienter.jbilling.server.pricing;

import java.math.BigDecimal;

public class RouteRateCardPriceResult {

    private BigDecimal price;
    private RouteRateCardRecord usedRouteRateCardRecord;

    public RouteRateCardPriceResult(BigDecimal price, RouteRateCardRecord usedRouteRateCardRecord) {
        this.price = price;
        this.usedRouteRateCardRecord = usedRouteRateCardRecord;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public RouteRateCardRecord getUsedRouteRateCardRecord() {
        return usedRouteRateCardRecord;
    }

}
