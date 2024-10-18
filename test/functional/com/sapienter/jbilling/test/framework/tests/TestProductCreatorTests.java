package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by marcolin on 06/11/15.
 */
@Test(groups = {"integration", "test-framework"})
public class TestProductCreatorTests {

    @Test
    public void testProductCreationWithDependencyAndCleanAfterTest() {
        TestBuilder.newTest(false).given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            envBuilder.itemBuilder(api).item().withCode("testProduct").withType(testCategory).build();
            ItemBuilder itemBuilder = envBuilder.itemBuilder(api);
            Integer testCategory2 = itemBuilder.itemType().withCode("testCategory2").build();
            envBuilder.itemBuilder(api).item().withCode("testProduct2").withType(testCategory2)
                    .withDependencies(
                            itemBuilder.itemDependency()
                                    .withItemDependencyType(ItemDependencyType.ITEM_TYPE)
                                    .withDependentId(testCategory2)
                                    .withMinimum(1)
                                    .withMaximum(2)
                                    .build()
                    ).build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer itemId = api.getItemID(env.jBillingCode("testProduct2"));
            ItemDTOEx item = api.getItem(itemId, null, null);
            assertEquals(1, item.getDependencies().length);
        });
    }

//    @Test
//    public void testOrderCreationForProductWithDependencies() {
//        TestBuilder.newTest(false).given(environmentCreator -> {
//            JbillingAPI api = envBuilder.getPrancingPonyApi();
//            envBuilder.customerBuilder(api)
//                    .withUsername("testCustomer").build();
//            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
//            envBuilder.itemBuilder(api).item().withCode("testProduct").withType(testCategory).build();
//            Integer testCategory2 = envBuilder.itemBuilder(api).itemType().withCode("testCategory2").build();
//            envBuilder.itemBuilder(api).item().withCode("testProduct2").withType(testCategory2)
//                    .withDependencyOn("testCategory2").build();
//        }).test((environment, envBuilder) -> {
//            envBuilder.orderBuilder(environment.getPrancingPonyApi())
//                    .forUser(environment.idForCode("testCustomer")).withPeriod(Constants.ORDER_PERIOD_ONCE)
//                    .withProducts(environment.idForCode("testProduct2"))
//            ;
//
//        });
//
//
//    }
}
