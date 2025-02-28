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

package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.pricing.strategy.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Simple mapping enum for PricingStrategy implementations. This class is used
 * to produce PricingStrategy instances for modeled prices.
 *
 * Enum type strings are also mapped in the {@link com.sapienter.jbilling.server.pricing.PriceModelWS}
 * class for convenience when using the Web Services API.
 *
 * @author Brian Cowdery
 * @since 05-08-2010
 */
public enum PriceModelStrategy {

    /** Zero pricing strategy, always sets price to ZERO */
    ZERO                        (new ZeroPricingStrategy()),

    /** Flat pricing strategy, sets a configurable $/unit rate */
    FLAT                        (new FlatPricingStrategy()),

    /****/
    LINE_PERCENTAGE              (new LinePercentagePricingStrategy()),
    
    /** Graduated pricing strategy, allows a set number of included units before enforcing a $/unit rate */
    GRADUATED                   (new GraduatedPricingStrategy()),

    /** Graduated pricing strategy with a maximum total usage $ cap */
    CAPPED_GRADUATED            (new CappedGraduatedPricingStrategy()),

    /** Pricing strategy that uses the current time (or time of a mediated event) to determine the price */
    TIME_OF_DAY                 (new TimeOfDayPricingStrategy()),

    /** MIDDLE or END of chain pricing strategy that applies a percentage to a previously calculated rate */
    PERCENTAGE                  (new PercentageStrategy()),

    /** MIDDLE or END of chain, time-of-day strategy that applies a percentage to a previously calculated rate */
    TIME_OF_DAY_PERCENTAGE      (new TimeOfDayPercentageStrategy()),
    
    TIERED                      (new TieredPricingStrategy()),

    /** Pricing based on the quantity purchased. */
    VOLUME_PRICING              (new VolumePricingStrategy()),

    /** Graduated pricing strategy that counts a users subscription to an item as the "pooled" included quantity */
    POOLED                      (new PooledPricingStrategy()),

    /** Strategy that adds another item to the order based on the level of usage within a specific item type */
    ITEM_SELECTOR               (new ItemSelectorStrategy()),

    /** Strategy that adds another item to the order based on the percentage used of one item type over another */
    ITEM_PERCENTAGE_SELECTOR    (new ItemPercentageSelectorStrategy()),

    /** START of chain pricing strategy that increases the "included" quantity of a Graduated price using other purchased add-on items */
    QUANTITY_ADDON              (new QuantityAddonPricingStrategy()),

    /** Pricing strategy that queries the price from a cached rating table using the value from a provided pricing field. */
    RATE_CARD                   (new RateCardPricingStrategy()),
    
    /** Pricing strategy that allows hierarchy of rate cards with an added option to chose Rate based on Route. */
    ROUTE_BASED_RATE_CARD       (new RouteBasedRateCardPricingStrategy()),

    /** Initial block of units are priced at a fixed rate. All additional units priced based on rate card. */
    BLOCK_AND_INDEX             (new BlockIndexRouteRateCardStrategy()),

    /**  Graduated pricing strategy that fetches price based on route based rate cards. */
    GRADUATED_RATE_CARD       (new GraduatedRateCardPricingStrategy()),

    /***/
    COMPANY_POOLED              (new CompanyPooledPricingStrategy()),

    TEASER_PRICING            (new TeaserPricingStrategy(), false),

    /** Blocks are defined in data table. Effective usage is calculated on other factors besides actual usage */
    BLOCK_AND_INDEX_PRICE_PLUS            (new BlockIndexPricePlusPricingStrategy()),

    /** This pricing strategy will look up for a pricing in predefined pricing field.
     * And can additionally add a certain percentage on top of the price.
     * This pricing strategy is not meant to be chained with other pricing strategies*/
    FIELD_BASED                 (new CDRFieldBasedPercentagePricingStrategy()),

    /**  It is route rate card based model which calculate rate for next day of Order's active until date */
    DAY_AHEAD             (new DayAheadPricingStrategy()),

    /** Expected quantity is provided as range. Above/below the expected usage, rate will be different */
    USAGE_LIMIT             (new UsageLimitPricingStrategy()),

    VARIABLE_RATE             (new VariableRatePricingStrategy()),

    LBMP_PLUS_BLENDED_RATE (new LbmpPlusBlendedRatePricingStrategy()),

    NYMEX_PLUS_MONTHLY (new NYMEXPlusMonthlyPricingStrategy()),

    RRC_PERCENTAGE (new RRCPercentagePricingStrategy());

    private final PricingStrategy strategy;
    private final boolean allowedToUpdateOrderChange;

    PriceModelStrategy(PricingStrategy strategy) {
        this(strategy, true);
    }

    PriceModelStrategy(PricingStrategy strategy, boolean allowedToUpdateOrderChange) {
        this.strategy = strategy;
        this.allowedToUpdateOrderChange = allowedToUpdateOrderChange;
    }

    public PricingStrategy getStrategy() { return strategy; }
    public boolean isAllowedToUpdateOrderChange() { return allowedToUpdateOrderChange; }

    public static Set<PriceModelStrategy> getStrategyByChainPosition(ChainPosition ...chainPositions) {
        Set<PriceModelStrategy> strategies = new LinkedHashSet<>();
        for (PriceModelStrategy strategy : values()) {
            for (ChainPosition position : chainPositions) {
                if (strategy.getStrategy().getChainPositions().contains(position)) {
                    strategies.add(strategy);
                }
            }
        }
        return strategies;
    }
    
}
