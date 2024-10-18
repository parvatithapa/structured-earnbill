package com.sapienter.jbilling.server.discounts;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.builders.DiscountBuilder;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by Leandro Zoi on 12/09/17.
 */
@Test(groups = { "web-services", "discounts" }, testName = "discounts.WSTest")
public class WSTest {
    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);

    private static final String DISCOUNT_DESCRIPTION = "Discount %d";
    private static final String DISCOUNT_RATE = "10.25";
    private static final String DISCOUNT_WRONG_RATE = "10.25612";

    private static JbillingAPI api;

    @BeforeTest
    public void initializeTests() throws Exception {
        // Prancing Pony entities
        api = JbillingAPIFactory.getAPI();
    }

    @AfterTest
    public void cleanUp(){
        api = null;
    }

    @Test
    public void test001TestDiscountConstraint() {
        boolean fail = false;

        try {
            new DiscountBuilder(api, null).withCodeForTests(String.valueOf(System.currentTimeMillis()))
                                          .withDescription(String.format(DISCOUNT_DESCRIPTION, System.currentTimeMillis()))
                                          .withRate(DISCOUNT_WRONG_RATE)
                                          .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                                          .build();
        } catch (SessionInternalError sie) {
            logger.debug("The creation of discount fail because the rate is wrong");
            assertTrue(sie.getErrorMessages()[0].equals("DiscountWS,rate,validation.error.invalid.number.or.fraction.4.decimals"));
            fail = true;
        }

        assertTrue(fail, "The discount creation should be fail");
        fail = false;

        try {
            new DiscountBuilder(api, null).withCodeForTests(String.valueOf(System.currentTimeMillis()))
                                          .withDescription(String.format(DISCOUNT_DESCRIPTION, System.currentTimeMillis()))
                                          .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                                          .build();

        } catch (SessionInternalError sie) {
            logger.debug("The creation of discount fail because the rate is wrong");
            assertTrue(sie.getErrorMessages()[0].equals("DiscountWS,rate,validation.error.notnull"));
            fail = true;
        }

        assertTrue(fail, "The discount creation should be fail");
        fail = false;

        try {
            new DiscountBuilder(api, null).withCodeForTests(String.valueOf(System.currentTimeMillis()))
                                          .withRate(DISCOUNT_RATE)
                                          .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                                          .build();

        } catch (SessionInternalError sie) {
            logger.debug("The creation of discount fail because don't have descriptions");
            assertTrue(sie.getErrorMessages()[0].equals("DiscountWS,descriptions,validation.error.notempty"));
            fail = true;
        }

        assertTrue(fail, "The discount creation should be fail");
        fail = false;

        try {
            new DiscountBuilder(api, null).withDescription(String.format(DISCOUNT_DESCRIPTION, System.currentTimeMillis()))
                                          .withRate(DISCOUNT_RATE)
                                          .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                                          .build();

        } catch (SessionInternalError sie) {
            logger.debug("The creation of discount fail because don't have descriptions");
            assertTrue(sie.getErrorMessages()[0].equals("DiscountWS,code,validation.error.notnull"));
            fail = true;
        }

        assertTrue(fail, "The discount creation should be fail");
        fail = false;

        try {
            new DiscountBuilder(api, null).withCodeForTests(RandomStringUtils.randomAlphanumeric(21))
                                          .withDescription(String.format(DISCOUNT_DESCRIPTION, System.currentTimeMillis()))
                                          .withRate(DISCOUNT_RATE)
                                          .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                                          .build();

        } catch (SessionInternalError sie) {
            logger.debug("The creation of discount fail because don't have descriptions");
            assertTrue(sie.getErrorMessages()[0].equals("DiscountWS,code,validation.error.size,1,20"));
            fail = true;
        }

        assertTrue(fail, "The discount creation should be fail");

        Integer discountId = new DiscountBuilder(api, null).withCodeForTests(String.valueOf(System.currentTimeMillis()))
                                                           .withDescription(String.format(DISCOUNT_DESCRIPTION, System.currentTimeMillis()))
                                                           .withRate(DISCOUNT_RATE)
                                                           .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                                                           .build();

        assertNotNull(discountId, "The discount id shouldn't be null");
        api.deleteDiscount(discountId);
    }
}
