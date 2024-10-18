package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests for plans creations and
 * clean up in the testing framework.
 *
 * @author Vojislav Stanojevikj
 * @since 16-Jun-2016.
 */
@Test(groups = {"integration", "test-framework"})
public class TestPlanCreationForTests {

    @Test
    public void testPlanCreationAndCleanUp(){
        final TestBuilder testBuilder = TestBuilder.newTest();
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        final String planTestCode = "TestPlan";
        final String subscriptionItemTestCode = "TestSubscriptionItem";
        final String planItemTestCode = "TestItem";
        final Integer initialPlansCount = api.getAllPlans().length;

        TestEnvironment environment = testBuilder.given(envBuilder -> {

            Integer categoryId = envBuilder.itemBuilder(api).itemType()
                    .withCode("TestCategory")
                    .withEntities(api.getCallerCompanyId())
                    .build();

            Integer subscriptionItemId = createItem(envBuilder, subscriptionItemTestCode, categoryId, "1");
            Integer productId = createItem(envBuilder, planItemTestCode, categoryId, "10");

            envBuilder.planBuilder(api, planTestCode)
                    .withDescription(planTestCode)
                    .withItemId(subscriptionItemId)
                    .withPeriodId(Constants.ORDER_PERIOD_ONCE)
                    .addPlanItem(
                            PlanBuilder.PlanItemBuilder.getBuilder()
                                    .withItemId(productId)
                                    .withBundledQuantity("1")
                                    .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                    .build()
                    )
                    .build();
        }).test(env -> {

            PlanWS plan = api.getPlanWS(env.idForCode(planTestCode));
            validatePlan(plan, env.idForCode(subscriptionItemTestCode), Constants.ORDER_PERIOD_ONCE, planTestCode);
            List<PlanItemWS> planItems = plan.getPlanItems();
            assertEquals(Integer.valueOf(planItems.size()), Integer.valueOf(1), "Invalid number of plan items!");
            validatePlanItem(planItems.get(0), env.idForCode(planItemTestCode),
                    BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE);
            assertEquals(Integer.valueOf(api.getAllPlans().length), Integer.valueOf(initialPlansCount + 1),
                    "Invalid number of persisted plans!");
        });
        assertEquals(Integer.valueOf(api.getAllPlans().length), initialPlansCount, "Invalid number of persisted plans!");
        assertNull(environment.idForCode(planTestCode));
    }

    private Integer createItem(TestEnvironmentBuilder testEnvironmentBuilder, String code,
                               Integer categoryId, String price){

        JbillingAPI api = testEnvironmentBuilder.getPrancingPonyApi();
        return testEnvironmentBuilder.itemBuilder(api).item()
                .withCode(code)
                .withEntities(api.getCallerCompanyId())
                .withType(categoryId)
                .withFlatPrice(price)
                .build();
    }

    private void validatePlan(PlanWS plan, Integer subscriptionId, Integer periodId, String description){

        assertNotNull(plan, "Plan can not be null!");
        assertEquals(plan.getItemId(), subscriptionId, "Invalid plan subscription id!");
        assertEquals(plan.getPeriodId(), periodId, "Invalid plan period id!");
        assertEquals(plan.getDescription(), description, "Invalid plan description!");
    }

    private void validatePlanItem(PlanItemWS planItem, Integer itemId,
                                  BigDecimal bundledQuantity, Integer bundledPeriodId){

        assertNotNull(planItem, "Plan item can not be null!");
        assertEquals(planItem.getItemId(), itemId, "Invalid item id!");
        assertNotNull(planItem.getBundle(), "Plan item bundle can not be null!");
        assertEquals(planItem.getBundle().getQuantityAsDecimal().setScale(2, RoundingMode.CEILING),
                bundledQuantity.setScale(2, RoundingMode.CEILING), "Invalid bundled quantity!");
        assertEquals(planItem.getBundle().getPeriodId(), bundledPeriodId, "Invalid bundled period id!");
    }
}
