package com.sapienter.jbilling.server.pricing.strategy;


import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.pricing.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

@Test(groups = { "web-services", "pricing", "tiered" }, testName = "TieredPricingWSTest")
public class TieredPricingWSTest {

    private static JbillingAPI api;
	private static Integer CURRENCY_ID;
	private static Integer MONTHLY_PERIOD;

    @BeforeTest
    public void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();
	    CURRENCY_ID = PricingTestHelper.CURRENCY_USD;
	    MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
    }

    //Test issue #3665
    @Test
    public void testCreateTieredPlan() {
	    String random = String.valueOf(System.currentTimeMillis());
    	Integer categoryId = PricingTestHelper.createItemCategory(api);
    	Integer planItemId = api.createItem(createItem(categoryId));
        Integer planAffectedItemId = api.createItem(createItem(categoryId));
        
        PriceModelWS tieredPrice = new PriceModelWS(PriceModelStrategy.TIERED.name(), null, CURRENCY_ID);
        tieredPrice.addAttribute("0", "*breakme&");

        PlanItemWS callPrice = new PlanItemWS();
        callPrice.setItemId(planAffectedItemId);
        callPrice.getModels().put(CommonConstants.EPOCH_DATE, tieredPrice);

        PlanWS plan = new PlanWS();
        plan.setItemId(planItemId);
        plan.setDescription("Tiered calls." + random);
        plan.setPeriodId(MONTHLY_PERIOD);
        plan.addPlanItem(callPrice);

        try {
            plan.setId(api.createPlan(plan)); // create plan
            fail("Validation error expected");
        } catch (SessionInternalError e) {
            assertContainsError(e, "TieredPricingStrategy,0,validation.error.not.a.number", null);
        }

        tieredPrice.addAttribute("0", "4");

        plan.setId(api.createPlan(plan)); // create plan
        assertNotNull("plan created", plan.getId());

        // cleanup
        api.deletePlan(plan.getId());
        api.deleteItem(planItemId);
        api.deleteItem(planAffectedItemId);
        api.deleteItemCategory(categoryId);
    }
    
    private ItemDTOEx createItem(Integer type){
    	ItemDTOEx testItem = new ItemDTOEx();
        testItem.setDescription("item"+Short.toString((short)System.currentTimeMillis()));
        testItem.setEntityId(api.getCallerCompanyId());
        testItem.setTypes(new Integer[]{type});
        testItem.setPrice("1");
        testItem.setNumber("Number"+Short.toString((short)System.currentTimeMillis()));
        testItem.setActiveSince(Util.getDate(2010, 1, 1));
        return testItem;
    }
}
