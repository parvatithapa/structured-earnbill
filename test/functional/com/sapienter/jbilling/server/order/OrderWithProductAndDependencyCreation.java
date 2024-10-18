package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import org.testng.annotations.Test;

import static org.junit.Assert.*;

/**
 * Created by marco manzi on 21/01/16.
 */
@Test(groups = { "web-services", "order" }, testName = "OrderWithProductAndDependencyCreation")
public class OrderWithProductAndDependencyCreation {
    @org.junit.Test
    public void testOrderWithAProductWithOneDependencyShouldFailIfDependencyProductIsNotInTheOrder() {
        TestBuilder.newTest(false).given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.customerBuilder(api).withUsername("testCustomer").build();
            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            envBuilder.itemBuilder(api).item().withCode("testProduct").withType(testCategory).build();
            ItemBuilder itemBuilder = envBuilder.itemBuilder(api);
            Integer testCategoryWithDependency = itemBuilder.itemType().withCode("testCategoryWithDependency").build();
            envBuilder.itemBuilder(api).item().withCode("testProductWithDependency")
                    .withType(testCategoryWithDependency)
                    .withDependencies(
                            itemBuilder.itemDependency()
                                    .withItemDependencyType(ItemDependencyType.ITEM_TYPE)
                                    .withDependentId(testCategory)
                                    .withMinimum(1)
                                    .withMaximum(2)
                                    .build())
                    .build();
        }).test(((env, envBuilder) -> {
            try {
                envBuilder.orderBuilder(envBuilder.getPrancingPonyApi())
                        .withCodeForTests("testOrder")
                        .forUser(env.idForCode("testCustomer")).withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withProducts(env.idForCode("testProductWithDependency")).build();
                fail("This should be throw a Session Internal Error because of dependency missing.");
            } catch (SessionInternalError e) {
                assertTrue(e.getMessage().contains("Error during order change apply: OrderWS,hierarchy,error.order.hierarchy.product.mandatory.dependency.not.meet. Errors: OrderWS,hierarchy,error.order.hierarchy.product.mandatory.dependency.not.meet"));
            }
        }));
    }

//    @org.junit.Test
//    public void testOrderWithAProductWithOneDependencyWorkIfBothProductsAreOnTheOrder() {
//        TestBuilder.newTest(false).given(environmentCreator -> {
//            JbillingAPI api = environmentCreator.getPrancingPonyApi();
//            environmentCreator.customerBuilder(api).withUsername("testCustomer").build();
//            Integer testCategory = environmentCreator.itemBuilder(api).itemType().withCode("testCategory").build();
//            environmentCreator.itemBuilder(api).item().withCode("testProduct").withType(testCategory).build();
//            Integer testCategoryWithDependency = environmentCreator.itemBuilder(api).itemType().withCode("testCategoryWithDependency").build();
//            environmentCreator.itemBuilder(api).item().withCode("testProductWithDependency")
//                    .withType(testCategoryWithDependency).withDependencyOn("testCategory").build();
//        }).test(((environment, environmentCreator) -> {
//            Integer builtOrder = environmentCreator.orderBuilder(environmentCreator.getPrancingPonyApi())
//                    .withCodeForTests("testOrder")
//                    .forUser(environment.idForCode("testCustomer")).withPeriod(Constants.ORDER_PERIOD_ONCE)
//                    .withProducts(environment.idForCode("testProductWithDependency"))
//                    .withChildLines(new HashMap<Integer, List<Integer>>() {{
//                        put(environment.idForCode("testProductWithDependency"),
//                                Arrays.asList());
//                    }}).build();
//            assertNotNull(builtOrder);
//        }));
//    }

}
