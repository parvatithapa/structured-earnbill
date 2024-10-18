package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import org.testng.annotations.Test;

import static org.junit.Assert.*;

/**
 * Created by marco manzi on 21/01/16.
 */
@Test(groups = {"integration", "test-framework"})
public class TestOrderCreationForTests {
    @Test
    public void testSingleOrderCreationWithOneProduct() {
        final Integer[] orderId = new Integer[1];
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS testCustomer = envBuilder.customerBuilder(api).withUsername("testCustomer").build();
            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            Integer testProduct = envBuilder.itemBuilder(api).item().withCode("testProduct").withType(testCategory).build();
            envBuilder.orderBuilder(api)
                    .withCodeForTests("testOrder")
                    .forUser(testCustomer.getId()).withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withProducts(testProduct).build();
        }).test(((env, envBuilder) -> {
            orderId[0] = env.idForCode("testOrder");
            envBuilder.getPrancingPonyApi().getOrder(orderId[0]);
        }));
        OrderWS testOrder = testEnv.getPrancingPonyApi().getOrder(orderId[0]);
        assertEquals(1, testOrder.getDeleted());
    }

}
