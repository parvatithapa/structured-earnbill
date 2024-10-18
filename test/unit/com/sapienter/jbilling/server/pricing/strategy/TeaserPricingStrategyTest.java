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
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;

import com.sapienter.jbilling.server.pricing.strategy.TeaserPricingStrategy.UseOrderPeriod;
import junit.framework.TestCase;

import static com.sapienter.jbilling.server.pricing.db.PriceModelStrategy.TEASER_PRICING;

/**
 * TeaserPricingStrategyTest
 *
 * @author Leandro Zoi
 * @since 09/07/2017
 */
public class TeaserPricingStrategyTest extends TestCase {

    private static final String EMPTY_ERROR = "bean.TeaserPricingStrategy.strategy.validation.error.at.least.one.period";
    private static final String NO_DECIMAL_ERROR = "bean.TeaserPricingStrategy.strategy.validation.error.all.attr.should.be.integer.numbers";
    private static final String NO_NEGATIVE_ERROR = "bean.TeaserPricingStrategy.strategy.validation.error.all.attr.should.be.positive.numbers";
    private static final String NO_NUMBER_ERROR = "bean.TeaserPricingStrategy.strategy.validation.error.all.attr.should.be.numbers";
    private static final String NO_USE_ORDER_PERIOD = "bean.TeaserPricingStrategy.strategy.validation.error.select.at.one.period";
    private static final String PERIOD = "period";
    private static final String USE_ORDER_PERIOD = "use_order_period";
    private static final String FIRST_PERIOD = "1";

    public void testNonEmptyAttributes() {
        PriceModelDTO priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.YES.name());

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(EMPTY_ERROR, sie.getErrorMessages()[0]);
        }
    }

    public void testNonEmptyCycles() {
        PriceModelDTO priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(FIRST_PERIOD, "A");
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.YES.name());

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(EMPTY_ERROR, sie.getErrorMessages()[0]);
        }

        priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(FIRST_PERIOD, "50");
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.YES.name());
        priceModel.addAttribute("3", "50a");

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(NO_NUMBER_ERROR, sie.getErrorMessages()[0]);
        }
    }

    public void testNonNegativeNumbers() {
        PriceModelDTO priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(FIRST_PERIOD, "-50");
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.YES.name());

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(NO_NEGATIVE_ERROR, sie.getErrorMessages()[0]);
        }

        priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(FIRST_PERIOD, "50");
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.YES.name());
        priceModel.addAttribute("-3", "50");

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(NO_NEGATIVE_ERROR, sie.getErrorMessages()[0]);
        }
    }

    public void testNonDecimalPeriods() {
        PriceModelDTO priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.YES.name());
        priceModel.addAttribute("2.5", "50");

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(NO_DECIMAL_ERROR, sie.getErrorMessages()[0]);
        }
    }

    public void testNonUseOrderPeriod() {
        PriceModelDTO priceModel = new PriceModelDTO();
        priceModel.setType(TEASER_PRICING);
        priceModel.addAttribute(PERIOD, null);
        priceModel.addAttribute(USE_ORDER_PERIOD, UseOrderPeriod.NO.name());
        priceModel.addAttribute("1", "50");
        priceModel.addAttribute("3", "75");
        priceModel.addAttribute("6", "100");

        try {
            priceModel.getStrategy().validate(priceModel);
        } catch (SessionInternalError sie) {
            assertEquals(NO_USE_ORDER_PERIOD, sie.getErrorMessages()[0]);
        }
    }
}
