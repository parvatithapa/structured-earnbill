package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.*;

/**
 * Test cases for Discount
 * builder creations and clean up
 * Created by hristijan on 6/21/16.
 *
 * @author Hristijan Todorovski
 * @since 21-Jun-2016
 */

@Test(groups = {"integration", "test-framework"})
public class TestDiscountCreationTests {

    @Test
    public void testDiscountCreationCleanAfterTest(){

        final Integer[] discountIds = new Integer[1];
        final String code = "D"+System.currentTimeMillis();

        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withDescription("TestDiscount" + System.currentTimeMillis())
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                    .build();

        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            discountIds[0] = env.idForCode(code);
            DiscountWS discountWS = api.getDiscountWS(discountIds[0]);
            assertNotNull(discountWS, "Item not found");
            assertEquals(discountWS.getRateAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal(10).setScale(2, BigDecimal.ROUND_CEILING), "Invalid Rate");
            assertEquals(discountWS.getType(), DiscountStrategyType.ONE_TIME_AMOUNT.name());

            //ToDo this should be fixed , you can delete the discount , but not the order lines
            api.deleteDiscount(discountIds[0]);
        });
        try {
            testEnv.getPrancingPonyApi().getDiscountWS(discountIds[0]);
            fail("Discount is found, but should not be found");

        }catch (Exception e){
            //exception nom nom
            /*TODO here we should not eat this exception and the assert should be different
            *we are missing couple of methods getAllDiscounts. And there should be difference in
            * find and get methods.
            */
        }
    }
}
