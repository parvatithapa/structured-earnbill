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
import com.sapienter.jbilling.server.BigDecimalTestCase;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests the CDRFieldBasedPercentagePricingStrategy
 *
 * @author Vladimir Carevski
 * @since 18-DEC-2013
 */
public class CDRFieldBasedPercentagePricingStrategyTest extends BigDecimalTestCase {

    public CDRFieldBasedPercentagePricingStrategyTest() {
    }

    public CDRFieldBasedPercentagePricingStrategyTest(String name) {
        super(name);
    }

    public void testPositivePercentage() {
        PriceModelDTO planPrice = new PriceModelDTO();
        planPrice.setType(PriceModelStrategy.FIELD_BASED);

        planPrice.addAttribute("rate_pricing_field_name", "PRICE");
        planPrice.addAttribute("apply_percentage", "85");

        PricingResult result = new PricingResult(1, 2, 3);

        List<PricingField> fields = new ArrayList<PricingField>();
        PricingField.add(fields, createPricingField("PRICE", "100"));

        planPrice.applyTo(null, null, null, result, fields, null, false, new Date());

        assertEquals(new BigDecimal("185.00"), result.getPrice());
    }

    public void testNegativePercentage() {
        PriceModelDTO planPrice = new PriceModelDTO();
        planPrice.setType(PriceModelStrategy.FIELD_BASED);

        planPrice.addAttribute("rate_pricing_field_name", "PRICE");
        planPrice.addAttribute("apply_percentage", "-85");

        PricingResult result = new PricingResult(1, 2, 3);

        List<PricingField> fields = new ArrayList<PricingField>();
        PricingField.add(fields, createPricingField("PRICE", "100"));

        planPrice.applyTo(null, null, null, result, fields, null, false, new Date());

        assertEquals(new BigDecimal("15.00"), result.getPrice());
    }

    public void testNoPriceField() {
        PriceModelDTO planPrice = new PriceModelDTO();
        planPrice.setType(PriceModelStrategy.FIELD_BASED);

        planPrice.addAttribute("rate_pricing_field_name", "PRICE");
        planPrice.addAttribute("apply_percentage", "-85");

        PricingResult result = new PricingResult(1, 2, 3);

        List<PricingField> fields = new ArrayList<PricingField>();
        PricingField.add(fields, createPricingField("DUMMY", "100"));

        planPrice.applyTo(null, null, null, result, fields, null, false, new Date());

        assertNull("The price should be null (undetermined)", result.getPrice());
    }

    public void testNoPriceAttribute() {
        PriceModelDTO planPrice = new PriceModelDTO();
        planPrice.setType(PriceModelStrategy.FIELD_BASED);

        planPrice.addAttribute("apply_percentage", "-85");

        PricingResult result = new PricingResult(1, 2, 3);

        List<PricingField> fields = new ArrayList<PricingField>();
        PricingField.add(fields, createPricingField("PRICE", "100"));

        planPrice.applyTo(null, null, null, result, fields, null, false, new Date());

        assertNull("The price should be null (undetermined)", result.getPrice());
    }

    public void testNotValidPercentageNumber() {
        PriceModelDTO planPrice = new PriceModelDTO();
        planPrice.setType(PriceModelStrategy.FIELD_BASED);

        planPrice.addAttribute("rate_pricing_field_name", "PRICE");
        planPrice.addAttribute("apply_percentage", "-a5");//not valid percentage number

        PricingResult result = new PricingResult(1, 2, 3);

        List<PricingField> fields = new ArrayList<PricingField>();
        PricingField.add(fields, createPricingField("PRICE", "100"));
        try {
            planPrice.applyTo(null, null, null, result, fields, null, false, new Date());
            fail("The pricing strategy should throw exception");
        } catch (SessionInternalError error){
        }
        assertNull("The price should be null (undetermined)", result.getPrice());
    }

    public void testNotValidPriceFieldNumber() {
        PriceModelDTO planPrice = new PriceModelDTO();
        planPrice.setType(PriceModelStrategy.FIELD_BASED);

        planPrice.addAttribute("rate_pricing_field_name", "PRICE");
        planPrice.addAttribute("apply_percentage", "55");//not valid percentage number

        PricingResult result = new PricingResult(1, 2, 3);

        List<PricingField> fields = new ArrayList<PricingField>();
        PricingField.add(fields, createPricingField("PRICE", "10a0"));
        try {
            planPrice.applyTo(null, null, null, result, fields, null, false, new Date());
            fail("The pricing strategy should throw exception");
        } catch (SessionInternalError error){
        }
        assertNull("The price should be null (undetermined)", result.getPrice());
    }

    private PricingField createPricingField(String name, String value){
        PricingField priceField = new PricingField();
        priceField.setName(name);
        priceField.setStrValue(value);
        priceField.setType(PricingField.Type.STRING);
        return priceField;
    }
}
