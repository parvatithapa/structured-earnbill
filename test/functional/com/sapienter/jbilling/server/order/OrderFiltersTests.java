package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.FilterConstraint;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Created by marcolin on 17/08/16.
 */
@Test(groups = {"web-services", "order"}, testName = "OrderFiltersTests")
public class OrderFiltersTests {

    @org.junit.Test
    public void testRetrieveOrderById() {
        TestBuilder.newTest(false).given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS testCustomer = envBuilder.customerBuilder(api).withUsername("testCustomer").build();
            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            Integer testProductId = envBuilder.itemBuilder(api).item().withCode("testProduct").withType(testCategory).build();
            envBuilder.orderBuilder(envBuilder.getPrancingPonyApi())
                    .withCodeForTests("testOrder")
                    .forUser(testCustomer.getUserId()).withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withProducts(testProductId).build();
        }).test(((env, envBuilder) -> {
            List<OrderWS> ordersByFilters = envBuilder.getPrancingPonyApi().findOrdersByFilters(0, 1, null, null,
                    Arrays.asList(Filter.integer("id", FilterConstraint.EQ, env.idForCode("testOrder"))));
            assertEquals(1, ordersByFilters.size());
        }));
    }
}
