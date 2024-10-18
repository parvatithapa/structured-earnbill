package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;
import java.util.Arrays;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


/**
 * Test cases for Usage pool
 * builder creations and clean up.
 *
 * @author Vojislav Stanojevikj
 * @since 15-Jun-2016.
 */
@Test(groups = {"integration", "test-framework"})
public class TestFupCreationForTests {

    @Test
    public void testFUPCreationCleanAfterTest() {
        final TestBuilder testBuilder = TestBuilder.newTest();

        final Integer[] poolIds = new Integer[1];
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        final Integer initialNumberOfPools = api.getAllUsagePools().length;
        testBuilder.given(envBuilder -> {
            Integer testCategoryId = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            Integer testItemId = envBuilder.itemBuilder(api).item().withCode("testProduct").withType(testCategoryId).build();
            envBuilder.usagePoolBuilder(api, "TestFUP")
                    .withName("TestFUP")
                    .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1))
                    .withQuantity("1")
                    .withResetValue("Zero")
                    .withItemIds(Arrays.asList(testItemId))
                    .withItemTypesIds(Arrays.asList(testCategoryId))
                    .build();
        }).test((env) -> {
            poolIds[0] = env.idForCode("TestFUP");
            UsagePoolWS usagePool = api.getUsagePoolWS(poolIds[0]);
            assertNotNull(usagePool, "Usage pool not found!");
            assertEquals(Integer.valueOf(initialNumberOfPools + 1), Integer.valueOf(api.getAllUsagePools().length));
            api.deleteUsagePool(poolIds[0]);
        });
        assertEquals(Integer.valueOf(initialNumberOfPools), Integer.valueOf(api.getAllUsagePools().length));
    }

}
