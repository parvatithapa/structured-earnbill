package com.sapienter.jbilling.fc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

/**
 * Created by nazish on 12/18/14.
 */

@Test(groups = { "fullcreative" }, testName = "FullCreativeOrderAPITest")
public class FullCreativeOrderAPITest {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeOrderAPITest.class);
    private JbillingAPI api;
    private int product8XXTollFreeId;
    private Integer basicItemManagerPlugInId;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI("apiClient");
        product8XXTollFreeId = FullCreativeTestConstants.TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID;

        basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);
    }

    @Test
    public void test01OrderSoapAPI() throws Exception {
	
        logger.debug("Testing getLatestOrder with soap API");
        
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
	SearchCriteria criteria = new SearchCriteria();
	criteria.setMax(1);
	criteria.setOffset(0);
	criteria.setSort("id");
	criteria.setTotal(-1);
	criteria.setFilters(new BasicFilter[]{basicFilter});
	
	// get an available asset's id for plan subscription item (id = 603)
	AssetSearchResult assetsResult603 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
	assertNotNull("No available asset found for product "+product8XXTollFreeId, assetsResult603);
	AssetWS[] available603Assets = assetsResult603.getObjects();
	assertTrue("No assets found for product 603.", null != available603Assets && available603Assets.length != 0);
	Integer assetIdProduct603 = available603Assets[0].getId();
	logger.debug("Asset Available for product {} = {}", product8XXTollFreeId, assetIdProduct603);
	
        Calendar calendar = Calendar.getInstance();
	calendar.set(Calendar.YEAR, 2014);
	calendar.set(Calendar.MONTH, 1);
	calendar.set(Calendar.DAY_OF_MONTH, 1);
	UserWS user = FullCreativeUtil.createUser(calendar.getTime());
	logger.debug("User created : {}", user.getId());
	
	logger.debug("Creating order...");
	logger.debug("Creating order...");
	logger.debug("Creating order...");
	OrderWS order = new OrderWS();
	order.setUserId(user.getId());
        order.setActiveSince(new Date());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(new Integer(2)); // monthly
	order.setCurrencyId(new Integer(1));
	
	PlanWS planWS = api.getPlanWS(FullCreativeTestConstants.AF_BEST_VALUE_PLAN_ID); // AF Best Value Plan
	
	OrderLineWS line1 = new OrderLineWS();
	line1.setItemId(planWS.getItemId());
	line1.setAmount("225.00");
	line1.setPrice("225.00");
	line1.setTypeId(Integer.valueOf(1));
	line1.setDescription("AF Best Value Plan");
	line1.setQuantity("1");
	line1.setUseItem(true);

	order.setOrderLines(new OrderLineWS[]{line1});
	
	//Setting up the asset with the plan
	OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, 3);
	for (OrderChangeWS ws : orderChanges) {
		if (ws.getItemId().intValue() == FullCreativeTestConstants.AF_BEST_VALUE_PLAN_SUBSCRIPTION_ID) {
			
			OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
        orderChangePlanItem.setItemId(product8XXTollFreeId);
        orderChangePlanItem.setId(0);
        orderChangePlanItem.setOptlock(0);
        orderChangePlanItem.setBundledQuantity(1);
        orderChangePlanItem.setDescription("DID-8XX");
        orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
        
        orderChangePlanItem.setAssetIds(new int[]{assetIdProduct603});
			
        ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
		}
	}
	
	Integer orderId	= api.createOrder(order, orderChanges);
	logger.debug("Order Created with asset : {}", orderId);
	
        OrderWS childOrder = api.getLatestOrder(user.getId());
        logger.debug("childOrder id : {}", childOrder.getId());
        assertNotNull("Order should be retrieved : ", childOrder);
        assertNotNull("Parent Order Id should be retrieved : ", childOrder.getParentOrderId());
        assertEquals("Expected Parent Order Id : ",orderId.toString(),childOrder.getParentOrderId());
        logger.debug("Order retrieved from getLatestOrder : {}", childOrder);

    }

	@AfterClass
	public void cleanUp(){
		FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
	}

}
