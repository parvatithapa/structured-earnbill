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

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineInfo;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL;
import com.sapienter.jbilling.server.pricing.RouteRateCardPriceResult;
import com.sapienter.jbilling.server.pricing.RouteRecord;
import com.sapienter.jbilling.server.pricing.cache.RouteFinder;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountTypePriceDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * RouteBasedRateCardPricingStrategy
 *
 * @author Vikas Bodani
 * @since 25/7/2013
 */
public class RouteBasedRateCardPricingStrategy extends AbstractPricingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PARAM_ROUTE_RATE_CARD_ID = "route_rate_card_id";
    protected static final String PARAM_DURATION_FIELD_NAME = "cdr_duration_field_name";
    protected static final String PARAM_CALL_COST_FIELD_NAME = "cdr_call_charge_field_name";
    private static final String PARAM_PRICING_FIELD_PREFIX = "pf_";

    protected static enum FupKey {NEW_QTY, FREE_QTY};

    public RouteBasedRateCardPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(PARAM_ROUTE_RATE_CARD_ID, INTEGER, true),
                new AttributeDefinition(PARAM_DURATION_FIELD_NAME, STRING, false),
                new AttributeDefinition(PARAM_CALL_COST_FIELD_NAME, STRING, false)
                );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
                );

        setRequiresUsage(false);
        setUsesDynamicAttributes(true);
        setVariableUsagePricing(false);
    }


    /**
     *  @param pricingOrder target order for this pricing request (not used by this strategy)
     * @param result pricing result to apply pricing to
     * @param fields pricing fields (not used by this strategy)
     * @param planPrice the plan price to apply (not used by this strategy)
     * @param quantity quantity of item being priced (not used by this strategy)
     * @param usage total item usage for this billing period
     * @param singlePurchase true if pricing a single purchase/addition to an order, false if
     * @param orderLineDTO
     */
    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice,
            BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        Map<FupKey, BigDecimal> fupResult = calculateFreeUsageQty(orderLineDTO, result, quantity);

        BigDecimal chargeableQuantity = fupResult.get(FupKey.NEW_QTY);
        BigDecimal price = calculatePrice(pricingOrder, result, fields, planPrice, chargeableQuantity);
        BigDecimal totalQuantity;
        if(null!= orderLineDTO) {
            if(orderLineDTO.isMediated()) {
                totalQuantity = orderLineDTO.getMediatedQuantity();
            } else {
                totalQuantity = orderLineDTO.getQuantity();
            }
        } else {
            totalQuantity = quantity;
        }
        calculateUnitPrice(result, totalQuantity, fupResult.get(FupKey.FREE_QTY), price);
    }


    /**
     * Calculate the unit price based on the total value.
     *
     * @param result - the price (unit price) will be set on the result.
     * @param quantity - quantity that was priced
     * @param fupQty - free usage (not priced).
     * @param price - total value of line
     */
    public void calculateUnitPrice(PricingResult result, BigDecimal quantity, BigDecimal fupQty, BigDecimal price) {
        if (price == null) {
            result.setPrice(BigDecimal.ZERO);
        } else {
            if(quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
                result.setPrice(price.divide(quantity, Constants.BIGDECIMAL_PRECISION_DECIMAL128, RoundingMode.HALF_EVEN));
            } else {
                result.setPrice(BigDecimal.ZERO);
            }
        }
    }

    /**
     * Calculate the qty of the total {@code quantity} which is seen as free usage.
     * @param mediatedLine
     * @param result
     * @return
     */
    public Map<FupKey, BigDecimal> calculateFreeUsageQty(OrderLineDTO mediatedLine, PricingResult result, BigDecimal lineQuantity) {
        Map<FupKey, BigDecimal> fupResult = new EnumMap<>(FupKey.class);
        BigDecimal freeUsageQuantityOfItem = BigDecimal.ZERO;
        if(null!= mediatedLine && mediatedLine.getItem().getId() == result.getItemId()) {
            OrderLineInfo oldLineInfo = mediatedLine.getOldOrderLineInfo();
            if(null!= oldLineInfo) {
                freeUsageQuantityOfItem = oldLineInfo.getFreeUsageQuantity();
            }
            if(0 == BigDecimal.ZERO.compareTo(freeUsageQuantityOfItem)) {
                freeUsageQuantityOfItem = mediatedLine.getFreeUsagePoolQuantity();
            } else {
                freeUsageQuantityOfItem = mediatedLine.getFreeUsagePoolQuantity().subtract(freeUsageQuantityOfItem);
            }
        }
        BigDecimal quantity = null;
        if(null!= mediatedLine) {
            if(mediatedLine.isMediated()) {
                quantity = mediatedLine.getMediatedQuantity();
            } else {
                quantity = mediatedLine.getQuantity();
            }
        } else {
            quantity = lineQuantity;
        }
        fupResult.put(FupKey.FREE_QTY, freeUsageQuantityOfItem);
        fupResult.put(FupKey.NEW_QTY, quantity.subtract(freeUsageQuantityOfItem));
        return fupResult;
    }

    /**
     * Calculate the total cost of the {@code quantity}. Does not subtract any FUP quantity.
     *
     * @param pricingOrder
     * @param result
     * @param fields
     * @param planPrice
     * @param quantity
     * @return
     */
    public BigDecimal calculatePrice(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields,
            PriceModelDTO planPrice, BigDecimal quantity) {

        List<PricingField> additionalFields = new ArrayList<>();

        //any price model attribute which starts with 'pf_' will be converted to a pricing field if one
        // with that name does not exist.
        SortedMap<String, String> attributes = planPrice.getAttributes();
        for(Map.Entry<String, String> entry : attributes.entrySet()) {
            if(entry.getKey().startsWith(PARAM_PRICING_FIELD_PREFIX)) {
                String parmName = entry.getKey().substring(PARAM_PRICING_FIELD_PREFIX.length());
                if(PricingField.getPricingFieldsValue(parmName).length == 0) {
                    PricingField.add(additionalFields, new PricingField(parmName, entry.getValue()));
                }
            }
        }

        return calculatePrice(pricingOrder, result, fields, additionalFields, planPrice, quantity, PARAM_ROUTE_RATE_CARD_ID);
    }

    public BigDecimal calculatePrice(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, List<PricingField> additionalFields, PriceModelDTO planPrice,
            BigDecimal quantity, String routeRateCardAttrName) {
        String durationFieldName= planPrice.getAttributes().get(PARAM_DURATION_FIELD_NAME);
        SortedMap<Integer, String> routeLabels = getRoutes(planPrice.getAttributes());
        LOG.debug("Route table mapping" + routeLabels);

        durationFieldName = (!StringUtils.isEmpty(durationFieldName)) ? durationFieldName : Constants.DEFAULT_DURATION_FIELD_NAME;

        // if a price was found earlier, skip
        if (result.getPrice() != null && result.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            LOG.debug("Price already found, skipping rate card lookup.");
            return BigDecimal.ZERO;
        }

        if (null == fields) {
            fields= new ArrayList<>(2);
        } else {
            fields= new ArrayList<>(fields);
        }

        Date eventDate= null;

        if ( fields.isEmpty() ) {
            //if order is null, return price for today
            if (null != pricingOrder ) {
                eventDate= pricingOrder.getPricingDate();
                logger.debug("Order Pricing Date {} ", eventDate);
            } else {
                eventDate= TimezoneHelper.companyCurrentDateByUserId(result.getUserId());
                logger.debug("In absence of Pricing Date, will resolve price for today.");
            }

            PricingField.add(fields, new PricingField(durationFieldName, quantity));
            logger.debug("Initialized pricingFields for date and duration from applyTo params");

        }

        fields.addAll(additionalFields);

        UserDTO user= null;

        if (null != result.getUserId() ) {
            user= new UserBL(result.getUserId()).getEntity();
        }

        // try to resolve the route records based on the route table mapping attribiutes
        resolveRoutes(fields, routeLabels);

        // get and validate attributes
        Integer routeRateCardId = AttributeUtils.getInteger(planPrice.getAttributes(), routeRateCardAttrName);
        RouteRateCardDAS rateCardDAS= new RouteRateCardDAS();
        RouteRateCardDTO rateCard= rateCardDAS.find(routeRateCardId);

        logger.debug("Route Rate Card ID Used {} for Customer price determination. ", routeRateCardId);
        boolean isMediated = pricingOrder!=null ? pricingOrder.getIsMediated() : Boolean.FALSE;
        // and do the pricing lookup
        BigDecimal price = determineRateCardPrice(rateCard, fields, planPrice, quantity, isMediated, result);
        logger.debug("Customer Rate Card Price {}", price);
        //account type rate card
        if ( null == price && null != user && user.getCustomer() != null ) {
            PlanItemDTO accountTypePlanItem= new AccountTypePriceDAS().findPriceByItem(user.getCustomer().getAccountType().getId(), result.getItemId());
            if (null != accountTypePlanItem) {
                PriceModelDTO priceModel= accountTypePlanItem.getPrice( eventDate ); //pricing date

                if (null != priceModel) {
                    routeRateCardId = AttributeUtils.getInteger(priceModel.getAttributes(), routeRateCardAttrName);
                    rateCard= rateCardDAS.find(routeRateCardId);
                    logger.debug("Route Rate Card ID Used {} for Accnt Type price determination. ", routeRateCardId);
                    price= determineRateCardPrice(rateCard, fields, planPrice, quantity, isMediated, result);
                }
            }
        }

        if ( null == price ) {
            //plan item price
            List<PlanDTO> plans= new PlanDAS().findByAffectedItem(result.getItemId());
            if ( CollectionUtils.isNotEmpty(plans) ) {
                PlanDTO plan= plans.get(0);
                //find plan item
                PriceModelDTO priceModel= plan.getPlanItems().get(0).getPrice( eventDate ); //pricing date
                if (null != priceModel) {
                    routeRateCardId = AttributeUtils.getInteger(priceModel.getAttributes(), routeRateCardAttrName);
                    rateCard= rateCardDAS.find(routeRateCardId);
                    logger.debug("Route Rate Card ID Used {} for Plan Item price determination. ", routeRateCardId);
                    price= determineRateCardPrice(rateCard, fields, planPrice, quantity, isMediated, result);
                }
            }
        }

        if (null == price) {
            // product price
            ItemDTO item= new ItemDAS().find(result.getItemId());
            if ( null != item && null != user) {
                PriceModelDTO priceModel = item.getPrice( eventDate , user.getEntity().getId());// pricing date
                if (null != priceModel) {
                    routeRateCardId = AttributeUtils.getInteger(
                            planPrice.getAttributes(), routeRateCardAttrName);
                    rateCard = rateCardDAS.find(routeRateCardId);
                    logger.debug("Route Rate Card ID Used {} for default product price determination. ", routeRateCardId);
                    price = determineRateCardPrice(rateCard, fields, planPrice, quantity, isMediated, result);
                }
            }
        }

        return price;
    }

    protected void resolveRoutes(List<PricingField> fields, SortedMap<Integer, String> routeLabels) {

        for (Map.Entry<Integer, String> entry : routeLabels.entrySet()) {
            try {
                Integer routeId = entry.getKey();
                RouteRecord routeRecord = determineRoute(fields, routeId);
                if (routeRecord != null) {
                    logger.debug("Record found for routeId {} , name: {}", routeId, routeRecord.getName());
                    PricingField.add(fields, new PricingField(entry.getValue(), routeRecord.getRouteId()));
                }

            } catch (Exception e) {
                LOG.debug("Exception occured: Skipping record:" + entry.getKey());
            }
        }
    }

    protected SortedMap<Integer, String> getRoutes(SortedMap<String, String> attributes) {
        SortedMap<Integer, String> routes = new TreeMap<Integer, String>();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!entry.getKey().equals(PARAM_ROUTE_RATE_CARD_ID) && !entry.getKey().equals(PARAM_DURATION_FIELD_NAME) && !entry.getKey().startsWith(PARAM_PRICING_FIELD_PREFIX)) {
                if (NumberUtils.isNumber(entry.getKey())) {
                    routes.put(AttributeUtils.parseInteger(entry.getKey()), entry.getValue());
                }
            }
        }

        return routes;
    }

    private BigDecimal determineRateCardPrice(RouteRateCardDTO rateCard, List<PricingField> fields,
            PriceModelDTO planPrice, BigDecimal quantity, boolean isMediated, PricingResult result) {

        //use RouteBasedRateCardFinder to resolve the price
        //include route information
        try {
            RouteBasedRateCardBL rateCardBL= new RouteBasedRateCardBL(rateCard);

            String durationFieldName= planPrice.getAttributes().get(PARAM_DURATION_FIELD_NAME);
            String callCostFieldName= planPrice.getAttributes().get(PARAM_CALL_COST_FIELD_NAME);
            callCostFieldName = (!StringUtils.isEmpty(callCostFieldName)) ? callCostFieldName : Constants.CALL_CHARGE;

            RouteRateCardPriceResult priceResult= rateCardBL.getBeanFactory().getFinderInstance().findRoutePrice(
                    rateCard, fields, durationFieldName, callCostFieldName, quantity, isMediated);
            if(null!= priceResult) {
                LOG.debug("Price resolved " + priceResult.getPrice());
                result.setRouteRateCardRecord(priceResult.getUsedRouteRateCardRecord());
                return priceResult.getPrice();
            }
            return null;
        } catch (Exception e) {

            LOG.debug("Exception occured while resolving price for a rate card.",  e);
            return null;
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

    public static boolean columnExists(String columnName, SqlRowSet set) {
        boolean result = false;
        for (String name : set.getMetaData().getColumnNames()) {
            if (name.equalsIgnoreCase(columnName)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
