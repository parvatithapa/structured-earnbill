package com.sapienter.jbilling.fc;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;


/**
 * 
 * @author manish
 *
 */
@Test(groups = { "fullcreative" }, testName = "FullCreativePrimaryAccountNumberTest")
public class FullCreativePrimaryAccountNumberTest {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativePrimaryAccountNumberTest.class);
    private JbillingAPI api;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private final static String CUSTOMER_META_FIELD = "primaryAccountNumber";
	private Integer afbestvaluePlanId ;
	private Integer afbasicPlanId;
	private Integer answerforceCategoryId;
	private int product8XXTollFreeId;
	private int productchatAccountId;
	private int productActiveResponseAccountId;
	private int productLocalEcfNumberId;
	private int product800TollFreeId;
	private int pluginIdForAccountNumberUpdateTask;
    private Integer basicItemManagerPlugInId;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        afbestvaluePlanId = FullCreativeTestConstants.AF_BEST_VALUE_PLAN_ID;
        product8XXTollFreeId = FullCreativeTestConstants.TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID;
        productchatAccountId = FullCreativeTestConstants.CHAT_ACCOUNT_ASSET_PRODUCT_ID;
        productActiveResponseAccountId = FullCreativeTestConstants.ACTIVE_RESPONSE_ACCOUNT_ASSET_PRODUCT_ID;
        productLocalEcfNumberId = FullCreativeTestConstants.LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID;
        product800TollFreeId = FullCreativeTestConstants.TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID;
        afbasicPlanId = FullCreativeTestConstants.AF_BASIC_PLAN_ID;
        answerforceCategoryId = FullCreativeTestConstants.ANSWER_FORCE_PLANS_CATEGORY_ID;
        java.util.Hashtable<String, String> parameterMap = new java.util.Hashtable<String, String>();
        parameterMap.put("DID_8XX", "DID-8XX");
        parameterMap.put("DID_800", "DID-800");
        parameterMap.put("DID_LOCAL_ECF", "DID-LOCAL-ECF");
        parameterMap.put("PhoneNumberCategoryId", String.valueOf(FullCreativeTestConstants.NUMBER_ASSET_CATEGORY_ID));
        PluggableTaskWS plugIn = new PluggableTaskWS();
        plugIn.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.UPDATE_CUSTOMER_ACCOUNT_NUMBER_TASK_CLASS_NAME).getId());
        plugIn.setOwningEntityId(api.getCallerCompanyId());
        plugIn.setParameters(parameterMap);
        plugIn.setProcessingOrder(1231);
        plugIn.setNotes("Test-Plugin");
        pluginIdForAccountNumberUpdateTask = api.createPlugin(plugIn);

        basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);
    }

    @Test
    public void test01PrimaryAccountNumberUpdate() throws Exception {
		// setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(1);
		criteria.setOffset(1);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});
		
		// get an available asset's id for plan subscription item (id = 603)
		AssetSearchResult assetsResult603 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
		assertNotNull("No available asset found for product 603", assetsResult603);
		AssetWS[] available603Assets = assetsResult603.getObjects();
		assertTrue("No assets found for product 603.", null != available603Assets && available603Assets.length != 0);
		Integer assetIdProduct603 = available603Assets[0].getId();
		logger.debug("Asset Available for product id 603 = {}", assetIdProduct603);
		
		// get an available asset's id for product id = 606
		AssetSearchResult assetsResult606 = api.findProductAssetsByStatus(productchatAccountId, criteria);
		assertNotNull("No available asset found for product 606", assetsResult606);
		AssetWS[] available606Assets = assetsResult606.getObjects();
		assertTrue("No assets found for product 606.", null != available606Assets && available606Assets.length != 0);
		Integer assetIdProduct606 = available606Assets[0].getId();
		logger.debug("Asset Available for product id 606 = {}", assetIdProduct606);
		
		// get an available asset's id for product id = 607
		AssetSearchResult assetsResult607 = api.findProductAssetsByStatus(productActiveResponseAccountId, criteria);
		assertNotNull("No available asset found for product 607", assetsResult607);
		AssetWS[] available607Assets = assetsResult607.getObjects();
		assertTrue("No assets found for product 607.", null != available607Assets && available607Assets.length != 0);
		Integer assetIdProduct607 = available607Assets[0].getId();
		logger.debug("Asset Available for product id 607 = {}", assetIdProduct607);
		
		logger.debug("Creating user...");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		UserWS user = FullCreativeUtil.createUser(calendar.getTime());
		logger.debug("User created with id: {}", user.getId());
		
		logger.debug("Creating order...");
		OrderWS order = new OrderWS();
		order.setUserId(user.getId());
                order.setActiveSince(new Date());
                order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
                order.setPeriod(new Integer(2)); // monthly
		order.setCurrencyId(new Integer(1));
		
		PlanWS planWS = api.getPlanWS(afbestvaluePlanId); // AF Best Value Plan
		
		OrderLineWS line1 = new OrderLineWS();
		line1.setItemId(planWS.getItemId());
		line1.setAmount("225.00");
		line1.setPrice("225.00");
		line1.setTypeId(Integer.valueOf(1));
		line1.setDescription("AF Best Value Plan");
		line1.setQuantity("1");
		line1.setUseItem(true);
		
		OrderLineWS line2 = new OrderLineWS();
		line2.setItemId(Integer.valueOf(productActiveResponseAccountId));
		line2.setDescription("Active Response Account");
		line2.setQuantity("1.0000000000");
		line2.setAmount("0.00");
		line2.setTypeId(Integer.valueOf(1));
		line2.setPrice("0.00");
		line2.setAssetIds(new Integer[]{
				Integer.valueOf(assetIdProduct607)
		});
	
		OrderLineWS line3 = new OrderLineWS();
		line3.setItemId(Integer.valueOf(productchatAccountId));
		line3.setDescription("Chat Account");
		line3.setQuantity("1.0000000000");
		line3.setAmount("0.00");
		line3.setTypeId(Integer.valueOf(1));
		line3.setPrice("0.00");
		line3.setAssetIds(new Integer[]{
				Integer.valueOf(assetIdProduct606)
		});
		
		order.setOrderLines(new OrderLineWS[]{line1, line2, line3});
		
		//Setting up the asset with the plan
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
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
		
		UserWS userWs = api.getUserWS(user.getId());
		String primaryNumber  = getMetaField(userWs.getMetaFields(), CUSTOMER_META_FIELD).getStringValue();
		
        //customer's primary account number assert
        assertEquals("Customer's primary account number is" + available603Assets[0].getIdentifier(), available603Assets[0].getIdentifier(), primaryNumber);
		
    }

public void test02PrimaryAccountNumberUpdate() throws Exception {
    	// setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(3);
		criteria.setOffset(3);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});
    	
    	// get an available asset's id for plan subscription item (id = 603)
		AssetSearchResult assetsResult603 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
		assertNotNull("No available asset found for product 603", assetsResult603);
		AssetWS[] available603Assets = assetsResult603.getObjects();
		assertTrue("No assets found for product 603.", null != available603Assets && available603Assets.length != 0);
		Integer assetIdProduct603_1 = available603Assets[0].getId();
		logger.debug("Asset Available for product id 603 = {}", assetIdProduct603_1);
		Integer assetIdProduct603_2 = available603Assets[1].getId();
		logger.debug("Asset Available for product id 603 = {}", assetIdProduct603_2);
		Integer assetIdProduct603_3 = available603Assets[2].getId();
		logger.debug("Asset Available for product id 603 = {}", assetIdProduct603_3);
		
		// get an available asset's id for product id = 604
		AssetSearchResult assetsResult604 = api.findProductAssetsByStatus(product800TollFreeId, criteria);
		assertNotNull("No available asset found for product 604", assetsResult604);
		AssetWS[] available604Assets = assetsResult604.getObjects();
		assertTrue("No assets found for product 604.", null != available604Assets && available604Assets.length != 0);
		Integer assetIdProduct604 = available604Assets[0].getId();
		logger.debug("Asset Available for product id 604 = {}", assetIdProduct604);
		
		// get an available asset's id for product id = 605
		AssetSearchResult assetsResult605 = api.findProductAssetsByStatus(productLocalEcfNumberId, criteria);
		assertNotNull("No available asset found for product 605", assetsResult605);
		AssetWS[] available605Assets = assetsResult605.getObjects();
		assertTrue("No assets found for product 605.", null != available605Assets && available605Assets.length != 0);
		Integer assetIdProduct605 = available605Assets[0].getId();
		logger.debug("Asset Available for product id 605 = {}", assetIdProduct605);
		
		// get an available asset's id for product id = 606
		AssetSearchResult assetsResult606 = api.findProductAssetsByStatus(productchatAccountId, criteria);
		assertNotNull("No available asset found for product 606", assetsResult606);
		AssetWS[] available606Assets = assetsResult606.getObjects();
		assertTrue("No assets found for product 606.", null != available606Assets && available606Assets.length != 0);
		Integer assetIdProduct606 = available606Assets[0].getId();
		logger.debug("Asset Available for product id 603 = {}", assetIdProduct606);
		
		// get an available asset's id for product id = 607
		AssetSearchResult assetsResult607 = api.findProductAssetsByStatus(productActiveResponseAccountId, criteria);
		assertNotNull("No available asset found for product 607", assetsResult607);
		AssetWS[] available607Assets = assetsResult607.getObjects();
		assertTrue("No assets found for product 607.", null != available607Assets && available607Assets.length != 0);
		Integer assetIdProduct607 = available607Assets[0].getId();
		logger.debug("Asset Available for product id 603 = {}", assetIdProduct607);
    	
		logger.debug("testACPrimaryAccountNumberSetToPlanAssetTest");
		
		logger.debug("Creating user...");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		UserWS user = FullCreativeUtil.createUser(calendar.getTime());
		logger.debug("User created : {}", user.getId());
		
		logger.debug("##Creating order...");
		OrderWS order = new OrderWS();
		order.setUserId(user.getId());
                order.setActiveSince(new Date());
                order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
                order.setPeriod(new Integer(2)); // monthly
		order.setCurrencyId(new Integer(1));
		
		PlanWS planWS = api.getPlanWS(afbasicPlanId); // AF Basic Plan
		
		OrderLineWS subscriptionLine = new OrderLineWS();
		subscriptionLine.setItemId(planWS.getItemId());
		subscriptionLine.setAmount("225.00");
		subscriptionLine.setPrice("225.00");
		subscriptionLine.setTypeId(Integer.valueOf(1));
		subscriptionLine.setDescription("AF Basic Plan");
		subscriptionLine.setQuantity("1");
		subscriptionLine.setUseItem(true);
		
		OrderLineWS line1 = new OrderLineWS();
		line1.setItemId(Integer.valueOf(productActiveResponseAccountId));
		line1.setDescription("Active Response Account");
		line1.setQuantity("1.0000000000");
		line1.setAmount("0.00");
		line1.setTypeId(Integer.valueOf(1));
		line1.setPrice("0.00");
		line1.setAssetIds(new Integer[]{assetIdProduct607});
		
		OrderLineWS line2 = new OrderLineWS();
		line2.setItemId(Integer.valueOf(productchatAccountId));
		line2.setDescription("Chat Account");
		line2.setQuantity("1.0000000000");
		line2.setAmount("0.00");
		line2.setTypeId(Integer.valueOf(1));
		line2.setPrice("0.00");
		line2.setAssetIds(new Integer[]{assetIdProduct606});
		
		OrderLineWS line3 = new OrderLineWS();
		line3.setItemId(Integer.valueOf(product8XXTollFreeId));
		line3.setDescription("DID-8XX-Product");
		line3.setQuantity("2");
		line3.setAmount("0.00");
		line3.setTypeId(Integer.valueOf(1));
		line3.setPrice("0.00");
		line3.setAssetIds(new Integer[]{assetIdProduct603_2, assetIdProduct603_3});
		
		OrderLineWS line4 = new OrderLineWS();
		line4.setItemId(Integer.valueOf(product800TollFreeId));
		line4.setDescription("DID-800");
		line4.setQuantity("1");
		line4.setAmount("0.00");
		line4.setTypeId(Integer.valueOf(1));
		line4.setPrice("0.00");
		line4.setAssetIds(new Integer[]{assetIdProduct604});
		
		OrderLineWS line5 = new OrderLineWS();
		line5.setItemId(Integer.valueOf(productLocalEcfNumberId));
		line5.setDescription("DID-LOCAL-ECF");
		line5.setQuantity("1");
		line5.setAmount("0.00");
		line5.setTypeId(Integer.valueOf(1));
		line5.setPrice("0.00");
		line5.setAssetIds(new Integer[]{assetIdProduct605});

		order.setOrderLines(new OrderLineWS[]{subscriptionLine, line1, line2, line3, line4, line5});
		
		//Setting up the asset with the plan
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		for (OrderChangeWS ws : orderChanges) {
			if (ws.getItemId().intValue() == planWS.getItemId().intValue()) {
				
				OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(product8XXTollFreeId);
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setBundledQuantity(1);
                orderChangePlanItem.setDescription("DID-8XX");
                orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
                
                orderChangePlanItem.setAssetIds(new int[]{assetIdProduct603_1});
				logger.debug("Assets ids ###### {}", orderChangePlanItem.getAssetIds());
				
                ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
                break;
			}
		}
		
		Integer orderId	= api.createOrder(order, orderChanges);
		logger.debug("Order Created with asset : {}", orderId);
		
		UserWS userWs = api.getUserWS(user.getId());
		String primaryNumber  = getMetaField(userWs.getMetaFields(), CUSTOMER_META_FIELD).getStringValue();
		
        //customer's primary account number assert
        assertEquals("Customer's primary account number is " + available603Assets[0].getIdentifier(), available603Assets[0].getIdentifier(), primaryNumber);

    }

    public void test03PrimaryAccountNumberUpdate() throws Exception {
    	// setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(2);
		criteria.setOffset(2);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});
		
		// get an available asset's id for product id = 605
		AssetSearchResult assetsResult605 = api.findProductAssetsByStatus(productLocalEcfNumberId, criteria);
		assertNotNull("No available asset found for product 605", assetsResult605);
		AssetWS[] available605Assets = assetsResult605.getObjects();
		List<AssetWS> assets= sortAssetWs(Arrays.asList(available605Assets));
		
		assertTrue("No assets found for product 605.", null != available605Assets && available605Assets.length == 2);
		Integer assetIdProduct605_1 = assets.get(0).getId();
		logger.debug("Asset Available for product id 605 = {}", assetIdProduct605_1);
		Integer assetIdProduct605_2 = assets.get(1).getId();
		logger.debug("Asset Available for product id 605 = {}", assetIdProduct605_2);

		// get an available asset's id for product id = 606
		AssetSearchResult assetsResult606 = api.findProductAssetsByStatus(productchatAccountId, criteria);
		assertNotNull("No available asset found for product 606", assetsResult606);
		AssetWS[] available606Assets = assetsResult606.getObjects();
		assertTrue("No assets found for product 606.", null != available606Assets && available606Assets.length != 0);
		Integer assetIdProduct606 = available606Assets[0].getId();
		logger.debug("Asset Available for product id 606 = {}", assetIdProduct606);

		// get an available asset's id for product id = 607
		AssetSearchResult assetsResult607 = api.findProductAssetsByStatus(productActiveResponseAccountId, criteria);
		assertNotNull("No available asset found for product 607", assetsResult607);
		AssetWS[] available607Assets = assetsResult607.getObjects();
		assertTrue("No assets found for product 607.", null != available607Assets && available605Assets.length != 0);
		Integer assetIdProduct607 = available607Assets[0].getId();
		logger.debug("Asset Available for product id 607 = {}", assetIdProduct607);

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);

		UserWS user = FullCreativeUtil.createUser(calendar.getTime());
        
		logger.debug("Creating order for 2nd Scenario...");
		OrderWS order2 = new OrderWS();
		order2.setUserId(user.getId());
                order2.setActiveSince(new Date());
                order2.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
                order2.setPeriod(new Integer(2)); // monthly
		order2.setCurrencyId(new Integer(1));

		OrderLineWS line1 = new OrderLineWS();
		line1.setItemId(Integer.valueOf(productActiveResponseAccountId));
		line1.setDescription("Active Response Account");
		line1.setAmount("0.00");
		line1.setTypeId(Integer.valueOf(1));
		line1.setPrice("0.00");
		line1.setQuantity(1);
		line1.setMetaFields(new MetaFieldValueWS[0]);
		line1.setAssetIds(new Integer[]{assetIdProduct607});
		
		OrderLineWS line2 = new OrderLineWS();
		line2.setItemId(Integer.valueOf(productchatAccountId));
		line2.setDescription("Chat Account");
		line2.setAmount("0.00");
		line2.setTypeId(Integer.valueOf(1));
		line2.setPrice("0.00");
		line2.setQuantity(1);
		line2.setMetaFields(new MetaFieldValueWS[0]);
		line2.setAssetIds(new Integer[]{assetIdProduct606});
		
		OrderLineWS line3 = new OrderLineWS();
		line3.setItemId(Integer.valueOf(productLocalEcfNumberId));
		line3.setDescription("DID-LOCAL-ECF");
		line3.setAmount("0.00");
		line3.setTypeId(Integer.valueOf(1));
		line3.setPrice("0.00");
		line3.setQuantity(2);
		line3.setMetaFields(new MetaFieldValueWS[0]);
		line3.setAssetIds(new Integer[]{assetIdProduct605_1, assetIdProduct605_2});
		
		order2.setOrderLines(new OrderLineWS[]{line1, line2, line3});
		
		Integer orderId2 = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));
		
		logger.debug("orderId2: {}", orderId2);
		
		UserWS userWs = api.getUserWS(user.getId());
		String primaryNumber  = getMetaField(userWs.getMetaFields(), CUSTOMER_META_FIELD).getStringValue();
		
		assertEquals("Customer's primary account number is " + assets.get(0).getIdentifier(), assets.get(0).getIdentifier(), primaryNumber);
    }
 
    public void test04PrimaryAccountNumberUpdate() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		UserWS user = FullCreativeUtil.createUser(calendar.getTime());

        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setEntityId(api.getCallerCompanyId());	// AnswerForce company id
        newItem.setDescription("DID-800 Plan");
        newItem.setTypes(new Integer[]{answerforceCategoryId});	// AnswerForce Plans Category
        newItem.setPrice("100.00");
        newItem.setNumber(String.valueOf(new Date().getTime()));
        newItem.setCurrencyId(new Integer(1));
        newItem.setPriceModelCompanyId(api.getCallerCompanyId());	// AnswerForce company id

        Integer newItemId = api.createItem(newItem);
        logger.debug("newItemId : {}", newItemId);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(2); //monthly
        bundle1.setQuantity(BigDecimal.ONE);
		
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setItemId(Integer.valueOf(product800TollFreeId)); //DID-800 product Id
        pi1.setBundle(bundle1);
		
        PlanWS planWS2 = new PlanWS();
        planWS2.setItemId(newItemId); 
        planWS2.setDescription("DID-800 Plan");
        planWS2.setPeriodId(2);//monthly
        planWS2.addPlanItem(pi1);
        
        planWS2.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("4.99"), 1));
        
        Integer planId = api.createPlan(planWS2);
        logger.debug("Plan created Successfully : {}", planId);
        
        // setup a BasicFilter which will be used to filter assets on Available status
 		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
 		SearchCriteria criteria = new SearchCriteria();
 		criteria.setMax(2);
 		criteria.setOffset(2);
 		criteria.setSort("id");
 		criteria.setTotal(-1);
 		criteria.setFilters(new BasicFilter[]{basicFilter});
     	
     	// get an available asset's id for plan subscription item (id = 603)
 		AssetSearchResult assetsResult603 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
 		assertNotNull("No available asset found for product 603", assetsResult603);
 		AssetWS[] available603Assets = assetsResult603.getObjects();
 		assertTrue("No assets found for product 603.", null != available603Assets && available603Assets.length != 0);
 		Integer assetIdProduct603_1 = available603Assets[0].getId();
 		logger.debug("Asset Available for product id 603 = {}", assetIdProduct603_1);
 		Integer assetIdProduct603_2 = available603Assets[1].getId();
 		logger.debug("Asset Available for product id 603 = {}", assetIdProduct603_2);
 		
 		// get an available asset's id for product id = 604
 		AssetSearchResult assetsResult604 = api.findProductAssetsByStatus(product800TollFreeId, criteria);
 		assertNotNull("No available asset found for product 604", assetsResult604);
 		AssetWS[] available604Assets = assetsResult604.getObjects();
 		assertTrue("No assets found for product 604.", null != available604Assets && available604Assets.length != 0);
 		Integer assetIdProduct604 = available604Assets[0].getId();
 		logger.debug("Asset Available for product id 604 = {}", assetIdProduct604);
        
		logger.debug("Creating order for 3rd Scenario...");
		OrderWS order3 = new OrderWS();
		order3.setUserId(user.getId());
        order3.setActiveSince(new Date());
        order3.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order3.setPeriod(new Integer(2)); // monthly
		order3.setCurrencyId(new Integer(1));

        
		PlanWS planWS = api.getPlanWS(planId); //Loading the previously created Plan
		
		OrderLineWS subscriptionLine = new OrderLineWS();
		subscriptionLine.setItemId(planWS.getItemId());
		subscriptionLine.setAmount("230.00");
		subscriptionLine.setPrice("230.00");
		subscriptionLine.setTypeId(Integer.valueOf(1));
		subscriptionLine.setDescription("AF Basic Plan");
		subscriptionLine.setQuantity("1");
		subscriptionLine.setUseItem(true);
		
		OrderLineWS line = new OrderLineWS();
		line.setItemId(Integer.valueOf(product8XXTollFreeId));
		line.setDescription("DID-8XX");
		line.setQuantity("2");
		line.setAmount("0.00");
		line.setTypeId(Integer.valueOf(1));
		line.setPrice("0.00");
		line.setAssetIds(new Integer[]{assetIdProduct603_1, assetIdProduct603_2});

		order3.setOrderLines(new OrderLineWS[]{subscriptionLine, line});
		
		//Setting up the asset with the plan
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID);
		for (OrderChangeWS ws : orderChanges) {
			if (ws.getItemId() == planWS.getItemId()) {
				
				OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(product800TollFreeId);
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setBundledQuantity(1);
                orderChangePlanItem.setDescription("DID-800");
                orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
                
                orderChangePlanItem.setAssetIds(new int[]{assetIdProduct604});
                
                ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
                break;
			}
		}
		
		Integer orderId3 = api.createOrder(order3, orderChanges);
		
		logger.debug("orderId2: {}", orderId3);
		
		UserWS userWs = api.getUserWS(user.getId());
		String primaryNumber  = getMetaField(userWs.getMetaFields(), CUSTOMER_META_FIELD).getStringValue();
		
        assertEquals("Customer's primary account number is " + available604Assets[0].getIdentifier(), available604Assets[0].getIdentifier(), primaryNumber);

    }
    
    @AfterClass
    protected void cleanUp() {
	api.deletePlugin(pluginIdForAccountNumberUpdateTask);
	FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
    }
    
    public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
			String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
	}
    
    /**
     * Sort Asset Identifier
     * @param list
     * @return
     */
    private List<AssetWS> sortAssetWs(List<AssetWS> list){
    	Collections.sort(list, new Comparator<AssetWS>() {
		public int compare(AssetWS s1, AssetWS s2) {
			if (!NumberUtils.isNumber(s1.getIdentifier()) && !NumberUtils.isNumber(s2.getIdentifier())) {
				return -1;
			}
			Long s1Identifier = Long.parseLong(s1.getIdentifier());
			Long s2Identifier = Long.parseLong(s2.getIdentifier());
			return s1Identifier.compareTo(s2Identifier);
		}
	});
    	return list;
    }
}
