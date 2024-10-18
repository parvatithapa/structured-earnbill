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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.cache.MatchCallback;
import com.sapienter.jbilling.server.pricing.cache.MatchType;
import com.sapienter.jbilling.server.pricing.cache.RateCardFinder;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pricing.RateCardBL;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.*;

/**
 * RateCardPricingStrategy
 *
 * @author Brian Cowdery
 * @since 19-02-2012
 */
public class RateCardPricingStrategy extends AbstractPricingStrategy {

    private static final String DROP_CHARGE_CARD_FIELD = "drop_charge_card_field";
    private static final String CURRENCY_FIELD = "REQ_CURRENCY_CODE";

    private static final String SETUP_CHARGE_APPLIED = "setup_charge_applied";
    private static final String SETTLE_SETUP_CHARGE_APPLIED = "settle_setup_charge_applied";
    public static final String FROM_SETTLE_RESERVATION = "from_settle_reservation";

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public RateCardPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition("rate_card_id", INTEGER, true),
                new AttributeDefinition("lookup_field", STRING, true),
                new AttributeDefinition("match_type", STRING, true),
                new AttributeDefinition(DROP_CHARGE_CARD_FIELD, STRING, false)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
        setVariableUsagePricing(false);
    }


    /**
     * @param pricingOrder   target order for this pricing request (not used by this strategy)
     * @param result         pricing result to apply pricing to
     * @param fields         pricing fields (not used by this strategy)
     * @param planPrice      the plan price to apply (not used by this strategy)
     * @param quantity       quantity of item being priced (not used by this strategy)
     * @param usage          total item usage for this billing period
     * @param singlePurchase true if pricing a single purchase/addition to an order, false if
     * @param orderLineDTO
     */
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice,
                        BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        // rate cards can exist in a chain, but we don't want to bother with another lookup
        // if a price was found earlier
        if (result.getPrice() != null && result.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("Price already found, skipping rate card lookup.");
            result.setPerCurrencyRateCard(true);
            return;
        }

        String applyToCurrency = planPrice.getCurrency().getCode();
        PricingField curr = find(fields, CURRENCY_FIELD);
        if (curr != null) {
            String currency = curr.getStrValue();
            if (currency != null && applyToCurrency != null && !currency.equals(applyToCurrency)) {
                logger.debug("Request currency {} does not match card currency {}, skipping rate card lookup",
                        currency, planPrice.getCurrency().getCode());
                result.setPerCurrencyRateCard(true);
                return;
            } else {
                result.setPerCurrencyRateCard(false);
            }
        }

        // get and validate attributes
        Integer rateCardId = AttributeUtils.getInteger(planPrice.getAttributes(), "rate_card_id");
        MatchType matchType = MatchType.valueOf(planPrice.getAttributes().get("match_type"));

        String lookupFieldName = planPrice.getAttributes().get("lookup_field");
        final String oneTimeChargeColumn = StringUtils.defaultIfBlank(
                planPrice.getAttributes().get(DROP_CHARGE_CARD_FIELD), "");

        PricingField lookupField = find(fields, lookupFieldName);

        // fetch the finder bean from spring
        // and do the pricing lookup
        BigDecimal price = BigDecimal.ZERO;

        if (lookupField != null) {
            try {
                RateCardBL rateCard = new RateCardBL(rateCardId);
                RateCardFinder pricingFinder = rateCard.getBeanFactory().getFinderInstance();

                if (pricingFinder != null) {
                    final BigDecimal[] oneTimeCharge = {BigDecimal.ZERO};
                    // Create a match callback that assigns the price according to
                    // the pricing term field, if it exists.
                    MatchCallback callback = new MatchCallback() {
                        @Override
                        public BigDecimal onMatch(SqlRowSet set) {

                            if (StringUtils.isNotBlank(oneTimeChargeColumn)
                                    && columnExists(oneTimeChargeColumn, set)) {
                                oneTimeCharge[0] = set.getBigDecimal(oneTimeChargeColumn);
                            }
                            return set.getBigDecimal("rate");
                        }

                        public Object onMatchObject(SqlRowSet set) {
                            return null;
                        }
                    };
                    price = pricingFinder.findPrice(matchType, lookupField.getStrValue(), callback);
                    if (oneTimeCharge[0].compareTo(BigDecimal.ZERO) != 0) {
                        if (!getBooleanValue(fields, SETUP_CHARGE_APPLIED)) {
                            price = applySetupCharge(price, quantity, oneTimeCharge[0], fields);
                        }
                        if (applySettleSetupCharge(fields)) {
                            price = doApplySettleSetupCharge(price, quantity, oneTimeCharge[0], fields);
                        }
                    }
                }

            } catch (ObjectNotFoundException e) {
                throw new SessionInternalError("Rate card does not exist!", e,
                        new String[]{"RateCardPricingStrategy,rate_card_id,rate.card.not.found"});
            }

        } else {
            logger.debug("Lookup field not found - not running in mediation or fields don't match configuration.");
        }

        result.setPrice(price);
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

    private boolean getBooleanValue(List<PricingField> fields, String key) {
        return find(fields, key) != null && find(fields, key).getBooleanValue();
    }

    private boolean applySettleSetupCharge(List<PricingField> fields) {
        return getBooleanValue(fields, FROM_SETTLE_RESERVATION) && !getBooleanValue(fields, SETTLE_SETUP_CHARGE_APPLIED);
    }

    private BigDecimal applySetupCharge(BigDecimal price, BigDecimal quantity, BigDecimal oneTimeCharge, List<PricingField> fields) {
        PricingField.add(fields, new PricingField(SETUP_CHARGE_APPLIED, Boolean.TRUE));
        return quantity.multiply(price).add(oneTimeCharge).divide(quantity, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal doApplySettleSetupCharge(BigDecimal price, BigDecimal quantity, BigDecimal oneTimeCharge, List<PricingField> fields) {
        PricingField.add(fields, new PricingField(SETTLE_SETUP_CHARGE_APPLIED, Boolean.TRUE));
        return quantity.multiply(price).add(oneTimeCharge).divide(quantity, 10, RoundingMode.HALF_UP);
    }

}
