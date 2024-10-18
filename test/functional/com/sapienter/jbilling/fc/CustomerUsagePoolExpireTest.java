package com.sapienter.jbilling.fc;


import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryTest;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

/**
 * @author Harshad Pathan
 */
@Test(groups = { "fullcreative" }, testName = "CustomerUsagePoolExpireTest")
public class CustomerUsagePoolExpireTest {


	private static final Logger logger = LoggerFactory.getLogger(CustomerUsagePoolExpireTest.class);
	private EnvironmentHelper envHelper;
	private TestBuilder testBuilder;

	private String testAccount = "Account Type";
	private String testCat1 = "MediatedUsageCategory";

	private static final String PLAN = "90 MIN $99.99 Month";
	private static final String PLAN2 = "100 MIN $99.99 Month";
	private static final String PLAN_PRODUCT = "testPlanItem";
	private static final String PLAN_PRODUCT2 = "testPlanItem2";

	private static final Integer postPaidOrderTypeId = Constants.ORDER_BILLING_POST_PAID;
	public static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID= 320104;

	// Mediated Usage Products
	public static final int INBOUND_USAGE_PRODUCT_ID = 320101;
	public static final int CHAT_USAGE_PRODUCT_ID = 320102;
	public static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;

	private static final String USAGE_POOL_90_MINS = "test90MinsFUP"+System.currentTimeMillis();
	private static final String USAGE_POOL_100_MINS = "test100MinsFUP"+System.currentTimeMillis();
	private static final Integer nextInvoiceDay = 1;
	
	public final static int MONTHLY_ORDER_PERIOD = 2;
	public final static int ONE_TIME_ORDER_PERIOD = 1;
	public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;

	// Test data for scenario 
	private static final String SCENARIO_05_USER = "testScenario05User"+System.currentTimeMillis();
	private static final String SCENARIO_05_ONE_TIME_ORDER = "testScenario04OneTimeOrder";
	private static final String SCENARIO_05_PRODUCT = "testScenario04Item";
	private String setUpFeeProduct = "SetUp Fee Product";
	private String feesCategory = "FeesCategory";

	private String user01 = "testScenario01User"+System.currentTimeMillis();
	private String user02 = "testScenario02User-"+System.currentTimeMillis();
	private String userPlanSwap = "userPlanSwap"+System.currentTimeMillis();
	private String user04 = "testScenario04User-"+System.currentTimeMillis();

	private final static Integer CC_PM_ID = 5;
	
	public static final String INBOUND_MEDIATION_LAUNCHER = "inboundCallsMediationJobLauncher";

	@BeforeClass
	public void initializeTests() {
		testBuilder = getTestEnvironment();

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			// Creating account type
			buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
			// Creating mediated usage category
			buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

			List<Integer> items = Arrays.asList(InvoiceSummaryTest.INBOUND_USAGE_PRODUCT_ID, InvoiceSummaryTest.CHAT_USAGE_PRODUCT_ID, InvoiceSummaryTest.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);

			// Creating Fees category
			buildAndPersistCategory(envBuilder, api, feesCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_PENALTY);

			// Creating SetUp Fees product
			buildAndPersistFlatProduct(envBuilder, api, setUpFeeProduct, false, envBuilder.idForCode(feesCategory), "49.00", false);

			Calendar pricingDate = Calendar.getInstance();
			pricingDate.set(Calendar.YEAR, 2014);
			pricingDate.set(Calendar.MONTH, 6);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);

			// Creating 90 Min Plan for Scenario #09
			pricingDate.set(Calendar.YEAR, 2015);
			pricingDate.set(Calendar.MONTH, 4);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);

			PlanItemWS planItemProd1 = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.30", pricingDate.getTime());
			PlanItemWS planItemProd2 = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.30", pricingDate.getTime());
			PlanItemWS planItemProd3 = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.30", pricingDate.getTime());

			PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

			// Created 90 FUP,FLAT PRODUCT AND PLAN with 90 Min
			buildAndPersistUsagePool(envBuilder, api, USAGE_POOL_90_MINS, "90", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "99.99", true);
			buildAndPersistPlan(envBuilder,api, PLAN, "90 Mins Plan - $99 / Month", InvoiceSummaryTest.MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(USAGE_POOL_90_MINS)),planItemProd1,planItemProd2,planItemProd3);

			// Created FUP,FLAT PRODUCT AND PLAN with 725 Free Min Plan
			buildAndPersistUsagePool(envBuilder, api, USAGE_POOL_100_MINS, "100", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, PLAN_PRODUCT2, false, envBuilder.idForCode(testCat1), "399.99", true);
			buildAndPersistPlan(envBuilder,api, PLAN2, "100 Free Min Plan", MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(PLAN_PRODUCT2), Arrays.asList(envBuilder.idForCode(USAGE_POOL_100_MINS)),planItemProd01WS, planItemProd02WS, planItemProd03WS);
		});
	}

	@AfterClass
	public void tearDown() {

		testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		testBuilder.removeEntitiesCreatedOnJBilling();
		if (null != envHelper) {
			envHelper = null;
		}
		if (null != testBuilder) {
			testBuilder = null;
		}
	}

	@Test
	public void testScenario01SingleSubscriptionOrder() {

		TestEnvironment environment = testBuilder.getTestEnvironment();

		try {
			testBuilder.given(envBuilder -> {

				logger.debug("Scenario #1 - Edit the plan order & change the status to FINISHED, "
						+ "save the order. FUP should be finished.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2017);
				nextInvoiceDate.set(Calendar.MONTH, 1);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2017);
				activeSince.set(Calendar.MONTH, 0);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(PLAN_PRODUCT), BigDecimal.ONE);

				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "150", "01/02/2017");

				InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Feb 2017
				scenario01.createUser(user01,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
				.createOrder("testSubScriptionOrderO1", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap, productAssetMap, false)
				.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

			}).validate((testEnv, envBuilder) -> {
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				OrderWS orderWS = api.getOrder(environment.idForCode("testSubScriptionOrderO1"));
				assertNotNull("getOrder should not be null", orderWS);
				UserWS userWS = api.getUserWS(environment.idForCode(user01));
				assertNotNull("getUserWS should not be null", userWS);

				Calendar cal = Calendar.getInstance();
				cal.setTime(userWS.getNextInvoiceDate());
				cal.add(Calendar.DATE,-1);

				validateCustomerUsagePool(api,userWS.getCustomerId(),TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(0, 31, 2017)));

				//update satatus as finished
				orderWS.getOrderStatusWS().setId(2);
				api.updateOrder(orderWS, null);
				validateCustomerUsagePool(api,environment.idForCode(user01),getEpochDate().toString());
				assertEquals("Expected Rerated order amount :: "
						,new BigDecimal("78.00"),api.getLatestOrder(environment.idForCode(user01)).getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * testScenario02MultipleSubscriptionOrder
	 */
	@Test
	public void testScenario02MultipleSubscriptionOrder() {

		TestEnvironment environment = testBuilder.getTestEnvironment();

		try {

			testBuilder.given(envBuilder -> {

				logger.debug("Scenario #2 - User that has multi Subscription Order");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2017);
				nextInvoiceDate.set(Calendar.MONTH, 1);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2017);
				activeSince.set(Calendar.MONTH, 0);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();

				Map<Integer, Integer> productAssetMap1 = new HashMap<>();
				AssetWS[] scenarioAssets = getAssetIdsByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
				productAssetMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenarioAssets[0].getId());
				Map<Integer, Integer> productAssetMap2 = new HashMap<>();
				productAssetMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenarioAssets[1].getId());

				Map<Integer, BigDecimal> productQuantityMap1 = new HashMap<>();
				productQuantityMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap1.put(environment.idForCode(PLAN_PRODUCT), BigDecimal.ONE);
				Map<Integer, BigDecimal> productQuantityMap2 = new HashMap<>();
				productQuantityMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap2.put(environment.idForCode(PLAN_PRODUCT2), BigDecimal.ONE);
				
				AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "250", "01/02/2017");
				InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);

				// Next Invoice Date 1st of Feb 2017
				scenario01.createUser(user02,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
				.createOrder("testSubScriptionOrderO01", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap1, productAssetMap1, false)
				.createOrder("testSubScriptionOrderO02", activeSince.getTime(), null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap2, productAssetMap2, false)
				.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

			}).validate((testEnv, envBuilder) -> {

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				OrderWS orderWS = api.getOrder(environment.idForCode("testSubScriptionOrderO01"));
				UserWS userWS = api.getUserWS(environment.idForCode(user02));

				Calendar expectedEndDate = Calendar.getInstance();
				expectedEndDate.setTime(userWS.getNextInvoiceDate());
				expectedEndDate.add(Calendar.DATE,-1);

				CustomerUsagePoolWS[] customerUsagePoolWSs = api.getCustomerUsagePoolsByCustomerId(userWS.getCustomerId());
				assertNotNull("customerUsagePoolWSs should not be null", customerUsagePoolWSs);

				Optional<CustomerUsagePoolWS> customerUsagePoolWS= Arrays.stream(customerUsagePoolWSs).filter(cUsagePoolWS 
						-> cUsagePoolWS.getOrderId().equals(orderWS.getId()))
						.findAny();

				assertEquals("Expected Cycle end date of customer usage pool :: "
						,TestConstants.DATE_FORMAT.format(expectedEndDate.getTime()),TestConstants.DATE_FORMAT.format(customerUsagePoolWS.get().getCycleEndDate()));

				//update status as finished
				orderWS.getOrderStatusWS().setId(2);
				orderWS.setProrateFlag(false);
				api.updateOrder(orderWS, null);

				CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolWS.get().getId());
				assertEquals("Expected Cycle end date of customer usage pool :: "
						,TestConstants.DATE_FORMAT.format(getEpochDate()),TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleEndDate()));
				
				assertEquals("Expected Rerated order amount :: "
						,new BigDecimal("57.00"),api.getLatestOrder(environment.idForCode(user02)).getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testScenario03PlanSwap() {

		TestEnvironment environment = testBuilder.getTestEnvironment();

		try {
			testBuilder.given(envBuilder -> {

				logger.debug("Scenario 3 #PlanSwap - Edit the plan order & swap the plan to different plan. "
						+ "FUP of earilear plan should be FINISHED.");

				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2017);
				nextInvoiceDate.set(Calendar.MONTH, 1);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2017);
				activeSince.set(Calendar.MONTH, 0);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(PLAN_PRODUCT), BigDecimal.ONE);
				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "250", "01/02/2017");

				InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Feb 2017
				scenario01.createUser(userPlanSwap,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
				.createOrder("testOrderUserPlanSwap", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap, productAssetMap, false)
				.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
				
			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2017);
				runDate.set(Calendar.MONTH, 1);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				OrderWS orderWS = api.getOrder(environment.idForCode("testOrderUserPlanSwap"));
				assertNotNull("orderWS should not be null", orderWS);

				UserWS userWS = api.getUserWS(environment.idForCode(userPlanSwap));
				assertNotNull("userWS should not be null", userWS);

				Calendar expectedEndDate = Calendar.getInstance();
				expectedEndDate.setTime(userWS.getNextInvoiceDate());
				expectedEndDate.add(Calendar.DATE,-1);

				PlanWS planWS = api.getPlanWS(environment.idForCode(PLAN));
				assertNotNull("planWS should not be null", planWS);

				CustomerUsagePoolWS[] customerUsagePoolWSs = api.getCustomerUsagePoolsByCustomerId(userWS.getCustomerId());
				Optional<CustomerUsagePoolWS> customerUsagePoolWS= Arrays.stream(customerUsagePoolWSs).filter(cUsagePoolWS 
						-> cUsagePoolWS.getPlanId().equals(planWS.getId()))
						.findAny();

				Integer cUsagePoolId = customerUsagePoolWS.get().getId();
				assertEquals("Expected Cycle end date of customer usage pool :: "
						,TestConstants.DATE_FORMAT.format(expectedEndDate.getTime()),TestConstants.DATE_FORMAT.format(customerUsagePoolWS.get().getCycleEndDate()));

				OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(orderWS, 
						planWS.getItemId(), 
						api.getPlanWS(environment.idForCode(PLAN2)).getItemId(), 
						SwapMethod.DIFF, 
						Util.truncateDate(orderWS.getActiveSince()));

				assertNotNull("Swap changes should be calculated", orderChanges);
				api.createUpdateOrder(orderWS, orderChanges);
				CustomerUsagePoolWS customerUsagePoolWS2 = api.getCustomerUsagePoolById(cUsagePoolId);
				
				assertEquals("Expected Cycle end date of customer usage pool :: "
						,TestConstants.DATE_FORMAT.format(getEpochDate()),TestConstants.DATE_FORMAT.format(customerUsagePoolWS2.getCycleEndDate()));

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testScenario04SingleSubscriptionOrder() {

		TestEnvironment environment = testBuilder.getTestEnvironment();

		try {
			testBuilder.given(envBuilder -> {

				logger.debug("Scenario #4 - Edit the plan order set the active until date, run "
						+ "the billing process. Order should be finished, so the FUP as well..");

				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2017);
				nextInvoiceDate.set(Calendar.MONTH, 1);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2017);
				activeSince.set(Calendar.MONTH, 0);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(PLAN_PRODUCT), BigDecimal.ONE);

				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "250", "01/02/2017");
				InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Feb 2017
				scenario01.createUser(user04,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
				.createOrder("testOrderO4", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap, productAssetMap, false)
				.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2017);
				runDate.set(Calendar.MONTH, 1);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeUntil = Calendar.getInstance();
				activeUntil.set(Calendar.YEAR, 2017);
				activeUntil.set(Calendar.MONTH, 0);
				activeUntil.set(Calendar.DAY_OF_MONTH, 20);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				validateCustomerUsagePool(api, environment.idForCode(user04), activeUntil.getTime().toString());

				OrderWS orderWS = api.getOrder(environment.idForCode("testOrderO4"));
				assertNotNull("orderWS should not be null", orderWS);

				orderWS.setActiveUntil(activeUntil.getTime());
				api.updateOrder(orderWS, null);

				Integer[] ids = api.createInvoiceWithDate(envBuilder.idForCode(user04),runDate.getTime(), PeriodUnitDTO.MONTH, 21, true);
				validateCustomerUsagePool(api, environment.idForCode(user04), getEpochDate().toString());
				assertEquals("Expected Rerated order amount :: "
						,new BigDecimal("249.52"),api.getLatestOrder(environment.idForCode(user04)).getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

				Arrays.asList(ids).stream().forEach(id ->{
					api.deleteInvoice(id);
				});
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testScenario05() {

		TestEnvironment environment = testBuilder.getTestEnvironment();

		try {
			testBuilder.given(envBuilder -> {

				logger.debug("Scenario #5 - Edit the plan order & remove plan from the order, "
						+ "assert for rerated amount of order");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2017);
				nextInvoiceDate.set(Calendar.MONTH, 1);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2017);
				activeSince.set(Calendar.MONTH, 0);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(PLAN_PRODUCT), BigDecimal.ONE);

				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "150", "01/02/2017");

				InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Feb 2017
				scenario01.createUser(SCENARIO_05_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
				.createOrder("testSubScriptionOrderO1", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap, productAssetMap, false)
				.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

			}).validate((testEnv, envBuilder) -> {


				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				OrderWS orderWS = api.getOrder(environment.idForCode("testSubScriptionOrderO1"));
				assertNotNull("getOrder should not be null", orderWS);
				UserWS userWS = api.getUserWS(environment.idForCode(SCENARIO_05_USER));
				assertNotNull("getUserWS should not be null", userWS);

				Calendar cal = Calendar.getInstance();
				cal.setTime(userWS.getNextInvoiceDate());
				cal.add(Calendar.DATE,-1);


				validateCustomerUsagePool(api,userWS.getCustomerId(),TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(0, 31, 2017)));

				OrderLineWS lineWS = Arrays.asList(orderWS.getOrderLines()).stream()
						.filter(line -> line.getItemId().equals(environment.idForCode(PLAN_PRODUCT)))
						.findAny().get();

				OrderChangeWS changeWS = OrderChangeBL.buildFromLine(lineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
				changeWS.setQuantity("-1");
				Integer updatedOrderId = api.createUpdateOrder(orderWS, new OrderChangeWS[]{changeWS});
				OrderWS lastUpdatedOrder = api.getLatestOrder(environment.idForCode(SCENARIO_05_USER));

				assertEquals("Expected Rerated order amount :: "
						,new BigDecimal("195.00"),lastUpdatedOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
		return envBuilder.itemBuilder(api)
				.itemType()
				.withCode(code)
				.withCategoryType(categoryType)
				.global(global)
				.build();
	}

	public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {

		AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
				.withName(name)
				.withPaymentMethodTypeIds(paymentMethodTypeId)
				.build();

		return accountTypeWS.getId();
	}

	private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String quantity,Integer categoryId, List<Integer>  items) {
		return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
				.withQuantity(quantity)
				.withResetValue("Reset To Initial Value")
				.withItemIds(items)
				.addItemTypeId(categoryId)
				.withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
				.withCyclePeriodValue(Integer.valueOf(1)).withName(code)
				.build();
	}

	public Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
			boolean global, Integer categoryId, String flatPrice, boolean allowDecimal) {
		return envBuilder.itemBuilder(api)
				.item()
				.withCode(code)
				.withType(categoryId)
				.withFlatPrice(flatPrice)
				.global(global)
				.allowDecimal(allowDecimal)
				.build();
	}

	private PlanItemWS buildPlanItem(JbillingAPI api, Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {

		return PlanBuilder.PlanItemBuilder.getBuilder()
				.withItemId(itemId)
				.withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
				.addModel(pricingDate, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
				.withBundledPeriodId(periodId)
				.withBundledQuantity(quantity)
				.build();
	}

	private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
			Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
		return envBuilder.planBuilder(api, code)
				.withDescription(desc)
				.withPeriodId(periodId)
				.withItemId(itemId)
				.withUsagePoolsIds(usagePools)
				.withPlanItems(Arrays.asList(planItems))
				.build().getId();
	}

	private static AssetWS getAssetIdByProductId(JbillingAPI api, Integer productId) {
		// setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(1);
		criteria.setOffset(0);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});

		AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
		assertNotNull("No available asset found for product "+productId, assetsResult);
		AssetWS[] availableAssets = assetsResult.getObjects();
		assertTrue("No assets found for product .", null != availableAssets && availableAssets.length != 0);
		Integer assetIdProduct = availableAssets[0].getId();
		logger.debug("Asset Available for product {} = {}", productId, assetIdProduct);
		return availableAssets[0];
	}


	/**
	 *
	 * @param api
	 * @param productId
	 * @return the Array of all avilable AssetWS for a given product.
	 */
	private static AssetWS[] getAssetIdsByProductId(JbillingAPI api, Integer productId) {
		// setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setOffset(0);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});

		AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
		assertNotNull("No available asset found for product "+productId, assetsResult);
		AssetWS[] availableAssets = assetsResult.getObjects();
		assertTrue("No assets found for product .", null != availableAssets && availableAssets.length != 3);
		Integer assetIdProduct = availableAssets[2].getId();
		logger.debug("Asset Available for product {} = {}", productId, assetIdProduct);
		return availableAssets;
	}

	private Date getEpochDate() {

		Date epochDate = new Date(0);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		try {
			epochDate = sdf.parse("1970-01-01-00:00:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return epochDate;
	}

	private void validateCustomerUsagePool(JbillingAPI api,Integer customerId,String expectedEndDate){

		CustomerUsagePoolWS[] customerUsagePoolWSs = api.getCustomerUsagePoolsByCustomerId(customerId);

		Arrays.stream(customerUsagePoolWSs).forEachOrdered(customerUsagePoolWS-> {
			System.out.println(" #### Cycle end date at finished order :: "+customerUsagePoolWS.getCycleEndDate());

			assertEquals("Expected Cycle end date of customer usage pool :: "
					,expectedEndDate,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

		});

	}

	private TestBuilder getTestEnvironment() {

		return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {

			this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
		});
	}

	private List<String> buildInboundCDR(List<String> indentifiers, String quantity, String eventDate) {
		List<String> cdrs = new ArrayList<String>();
		indentifiers.forEach(asset -> {
			cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()+",6105042651,tressie.johnson,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null");
		});
		return cdrs;
	}
}
