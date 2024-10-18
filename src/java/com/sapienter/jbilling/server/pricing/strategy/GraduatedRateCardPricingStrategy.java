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

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL;
import com.sapienter.jbilling.server.pricing.RouteRecord;
import com.sapienter.jbilling.server.pricing.cache.RouteFinder;
import com.sapienter.jbilling.server.pricing.db.*;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;

import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.*;

/**
 * Graduated rate card pricing strategy.
 * <p/>
 * Only usage over the included quantity will be billed.
 * Uses a route based rate card for pricing.
 *
 * @author Shweta Gupta
 * @since 06-08-2013
 */
public class GraduatedRateCardPricingStrategy extends GraduatedPricingStrategy {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(GraduatedRateCardPricingStrategy.class));
    private static final String ROUTE_RATE_CARD_ID = "route_rate_card_id";
    private static final String RATE_CARD_DURATION_FIELD_NAME = "duration_field_name";
    protected static final String RATE_CARD_CALL_COST_FIELD_NAME = "cdr_call_charge_field_name";

    public GraduatedRateCardPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(INCLUDED, DECIMAL, true),
                new AttributeDefinition(ROUTE_RATE_CARD_ID, INTEGER, true),
                new AttributeDefinition(RATE_CARD_DURATION_FIELD_NAME, STRING, true),
                new AttributeDefinition(RATE_CARD_CALL_COST_FIELD_NAME, STRING, false)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(true);
        setVariableUsagePricing(true);
    }

    /**
     * @param pricingOrder target order for this pricing request (not used by this strategy)
     * @param result       pricing result to apply pricing to
     * @param fields       pricing fields (not used by this strategy)
     * @param planPrice    the plan price to apply
     * @param quantity     quantity of item being priced
     * @param usage        total item usage for this billing period
     * @param orderLineDTO
     */
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields,
                        PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        BigDecimal price = getPrice(result, fields, planPrice);
        if (price == null) {
            result.setPrice(BigDecimal.ZERO);
        } else {
            result.setPrice(price.divide(quantity != null ? quantity : BigDecimal.ONE,
                    com.sapienter.jbilling.common.Constants.BIGDECIMAL_SCALE,
                    com.sapienter.jbilling.common.Constants.BIGDECIMAL_ROUND));
        }

        super.applyTo(pricingOrder, result, fields, planPrice, quantity, usage, singlePurchase);
    }

    public BigDecimal getPrice(PricingResult result, List<PricingField> fields, PriceModelDTO planPrice){
        BigDecimal price = BigDecimal.ZERO;

        if(null!=fields && !fields.isEmpty()){

            SortedMap<Integer, String> routeLabels = getRoutes(planPrice.getAttributes());

            UserDTO user= new UserBL(result.getUserId()).getEntity();

            // try to resolve the route records based on the route table mapping attribiutes
            resolveRoutes(fields, routeLabels);

            // get and validate attributes
            Integer routeRateCardId = AttributeUtils.getInteger(planPrice.getAttributes(), ROUTE_RATE_CARD_ID);
            RouteRateCardDAS rateCardDAS= new RouteRateCardDAS();
            RouteRateCardDTO rateCard= rateCardDAS.find(routeRateCardId);

            // and do the pricing lookup
            price = determinePrice(rateCard, fields, planPrice);
        } else {
            LOG.debug("Pricing fields is empty, can't look up into route rate card configurations");
        }

        return price;
    }

    private BigDecimal determinePrice(RouteRateCardDTO rateCard, List<PricingField> fields,  PriceModelDTO planPrice) {
        LOG.debug("Determining price..");
        BigDecimal price = BigDecimal.ZERO;
        try{

            //use RouteBasedRateCardFinder to resolve the price
            //include route information
            RouteBasedRateCardBL rateCardBL= new RouteBasedRateCardBL(rateCard);

            price= rateCardBL.getBeanFactory().getFinderInstance().findRoutePrice(
                    rateCard,
                    fields,
                    planPrice.getAttributes().get(RATE_CARD_DURATION_FIELD_NAME),
                    planPrice.getAttributes().get(RATE_CARD_CALL_COST_FIELD_NAME));

        } catch (Exception e){
            LOG.debug("Exception at determining price : "+e);
            e.printStackTrace();
        }

        LOG.debug("Price resolved : " + price);
        return price;
    }

    private void resolveRoutes(List<PricingField> fields, SortedMap<Integer, String> routeLabels) {

        for (Map.Entry<Integer, String> entry : routeLabels.entrySet()) {
            try {
                Integer routeId = entry.getKey();
                RouteRecord routeRecord = determineRoute(fields, routeId);
                if (routeRecord != null) {
                    LOG.debug("Record found for routeId %s, name: %s", routeId, routeRecord.getName());
                    PricingField.add(fields, new PricingField(entry.getValue(), routeRecord.getRouteId()));
                }

            } catch (Exception e) {
                LOG.debug("Exception occured: Skipping record:" + entry.getKey());
            }
        }
    }

    /**
     * A route needs to be determined only once, at the outset. Subsequently, the rate will be fetched from the rate card.
     * @param fields
     * @param routeId
     * @return
     */
    private RouteRecord determineRoute(List<PricingField> fields, Integer routeId) throws Exception {

        RouteRecord routeRecord = null;
        RouteBL routeBL= new RouteBL(routeId);
        RouteFinder routeFinder= routeBL.getBeanFactory().getFinderInstance();
        RouteDTO route = routeBL.getEntity();

        if ( null != routeFinder ) {
            if ( route.getMatchingFields().size() > 0 ) {
                routeRecord = routeFinder.findRoute(route, fields);
            }
        }

        return routeRecord;
    }

    private SortedMap<Integer, String> getRoutes(SortedMap<String, String> attributes) {
        SortedMap<Integer, String> routes = new TreeMap<Integer, String>();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!entry.getKey().equals(ROUTE_RATE_CARD_ID) && !entry.getKey().equals(RATE_CARD_DURATION_FIELD_NAME)
                    && !entry.getKey().equals(INCLUDED)) {
                routes.put(AttributeUtils.parseInteger(entry.getKey()), entry.getValue());
            }
        }

        return routes;
    }

}
