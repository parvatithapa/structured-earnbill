package com.sapienter.jbilling.server.invoiceSummary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
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

import org.joda.time.DateMidnight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * Invoice Template Scenarios spreadsheet link: 
 * https://docs.google.com/spreadsheets/d/13ZZ0wHfpnjEJ_qSrTd4Guc310viSe10RIBN1oGA1V8A
 * @author jbilling
 *
 */
@Test(groups = { "invoicesummary" }, testName = "InvoiceSummaryTest")
public class InvoiceSummaryTest {

	private static final Logger logger = LoggerFactory.getLogger(InvoiceSummaryTest.class);
	private EnvironmentHelper envHelper;
	private TestBuilder testBuilder;

	private final static Integer CC_PM_ID = 5;

	private static final Integer nextInvoiceDay = 1;
	private static final Integer postPaidOrderTypeId = Constants.ORDER_BILLING_POST_PAID;
	private static final Integer prePaidOrderTypeId = Constants.ORDER_BILLING_PRE_PAID;
	public final static int MONTHLY_ORDER_PERIOD = 2;
	public final static int ONE_TIME_ORDER_PERIOD = 1;
	public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private String testCat1 = "MediatedUsageCategory";
	private String adjustMentCategory = "AdjustMentCategory";
	private String feesCategory = "FeesCategory";
	private String taxCategory = "TaxCategory";
	private String adjustMentProduct = "adjustMentProduct";
	private String setUpFeeProduct = "SetUp Fee Product";
	private String lateFeesProduct = "Late Fees Product";
	private String lateFeeChagresProduct = "Late Fee Chagres Product";
	private String taxProduct = "taxProduct";
	private String vatProduct = "VATProduct";
	private String subScriptionProd01 = "testPlanSubscriptionItem";
	private String subScriptionProd05 = "testPlanSubscriptionItem05";
	private String subScriptionProd06 = "testPlanSubscriptionItem06";
	private String subScriptionProd10 = "testPlanSubscriptionItem10";
	private String subScriptionProd17 = "testPlanSubscriptionItem17";
	private String plan01 = "100 free minute Plan";
	private String plan05 = "1000 free minute Plan";
	private String plan06 = "1150 free minute Plan";
	private String plan10 = "250 free minute Plan";
	private String usagePoolO1 = "UP with 100 Quantity"+System.currentTimeMillis();
	private String usagePoolO5 = "UP with 1000 Quantity"+System.currentTimeMillis();
	private String usagePoolO6 = "Usage Pool with 1150 Quantity ";
	private String usagePool10 = "UP with 250 Quantity"+System.currentTimeMillis();
	private String testAccount = "Account Type";
	private String user01 = "testInvoiceSummaryUserOne"+System.currentTimeMillis();
	private String user05 = "testInvoiceSummaryUserFive"+System.currentTimeMillis();
	private String user06 = "testInvoiceSummaryUserSix"+System.currentTimeMillis();
	
	public static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID= 320104;
	public static final int TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID= 320105;
	public static final int LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID= 320106;
	
	public static final String INBOUND_MEDIATION_LAUNCHER = "inboundCallsMediationJobLauncher";
	public static final String ACTIVE_MEDIATION_LAUNCHER = "activeResponseMediationJobLauncher";
	public static final String CHAT_MEDIATION_LAUNCHER = "chatMediationJobLauncher";
	private static final String INVOICE_PENALTY_TASK_CLASS_NAME = "com.sapienter.jbilling.server.pluggableTask.OverdueInvoicePenaltyTask";
	private static final String SIMPLE_TAX_COMPOSITION_TASK_CLASS_NAME = "com.sapienter.jbilling.server.process.task.SimpleTaxCompositionTask";
	
	// Mediated Usage Products
	public static final int INBOUND_USAGE_PRODUCT_ID = 320101;
	public static final int CHAT_USAGE_PRODUCT_ID = 320102;
	public static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
	
	// Test data for scenario 2
	private static final String SCENARIO_02_USER = "testScenario02User"+System.currentTimeMillis();
	private static final String SCENARIO_02_ORDER = "testScenario02Order";
	private static final String SCENARIO_02_PRODUCT = "testScenario02Item";

	// Test data for scenario 3
	private static final String SCENARIO_03_USER = "testScenario03User"+System.currentTimeMillis();
	private static final String SCENARIO_03_ORDER = "testScenario03Order";
	
	// Test data for scenario 4
	private static final String SCENARIO_04_USER = "testScenario04User"+System.currentTimeMillis();
	private static final String SCENARIO_04_MONTHLY_ORDER = "testScenario04MonthlyOrder";
	private static final String SCENARIO_04_ONE_TIME_ORDER = "testScenario04OneTimeOrder";
	private static final String SCENARIO_04_PRODUCT = "testScenario04Item";

	// Test data for scenario 7
	private static final String SCENARIO_07_USER = "testScenario07User"+System.currentTimeMillis();

	// Test data for scenario 8
	private static final String SCENARIO_08_USER = "testScenario08User"+System.currentTimeMillis();
	private static final String SCENARIO_08_ONE_TIME_ORDER = "testScenario08OneTimeOrder";
	private static final String SCENARIO_08_MONTHLY_ORDER = "testScenario08MonthlyOrder";
	private static final String SCENARIO_08_PLAN_PRODUCT = "testScenario08PlanItem";
	private static final String SCENARIO_08_ZERO_USAGE_POOL = "testScenario08ZUP"+System.currentTimeMillis();
	private static final String SCENARIO_08_PLAN = "testScenario08PlanDormancy";
	
	// Test data for scenario 9
	private static final String SCENARIO_09_USER = "testScenario09User"+System.currentTimeMillis();
	private static final String SCENARIO_09_ORDER = "testScenario09Order";
	private static final String SCENARIO_09_ONE_TIME_ORDER = "testScenario09OneTimeOrder";
	private static final String SCENARIO_09_ONE_TIME_ADJUSTMENT_ORDER = "testScenario09OneTimeAdjustmentOrder";
	private static final String SCENARIO_09_MONTHLY_ORDER = "testScenario09MonthlyOrder";
	private static final String SCENARIO_09_PLAN_PRODUCT = "testScenario09PlanItem";
	private static final String SCENARIO_09_90_MIN_USAGE_POOL = "testScenario09ZUP"+System.currentTimeMillis();
	private static final String SCENARIO_09_PLAN = "90 MIN $99.99 Month";
	private static final String SCENARIO_09_ADJUSTMENT_ORDER = "adjustment-09";
	private static final String SCENARIO_ADJUSTMENT_PRODUCT_CATEGORY = "AdjustMentCategory_13";

	private static final String SCENARIO_10_USER = "testScenario10User"+System.currentTimeMillis();

	// Test data for scenario 11
	private static final String SCENARIO_11_USER = "testScenario11User"+System.currentTimeMillis();
	private static final String SCENARIO_11_ORDER = "testScenario11Order";
	private static final String SCENARIO_11_PLAN_PRODUCT = "testScenario11PlanItem";
	private static final String SCENARIO_11_ZERO_USAGE_POOL = "testScenario11ZUP"+System.currentTimeMillis();
	private static final String SCENARIO_11_PLAN = "PLAN $414.97 / Month";

	// Test data for scenario 12
	private static final String SCENARIO_12_USER = "testScenario12User"+System.currentTimeMillis();
	private static final String SCENARIO_12_ORDER = "testScenario12Order";
	private static final String SCENARIO_12_PLAN_PRODUCT = "testScenario12PlanItem";
	private static final String SCENARIO_12_USAGE_POOL = "testScenario12FUP135Min"+System.currentTimeMillis();
	private static final String SCENARIO_12_PLAN = "testScenario12Plan135";

	// Test data for scenario 13
	private static final String SCENARIO_13_USER = "testScenario13User"+System.currentTimeMillis();
	private static final String SCENARIO_13_ORDER = "testScenario13Order";
	private static final String SCENARIO_13_ORDER_ADJUSTMENT  = "testScenario13OrderAdjustment";
	private static final String SCENARIO_13_PRODUCT = "testScenario13Item";
	private static final String SCENARIO_13_PLAN_PRODUCT = "testScenario13PlanItem";
	private static final String SCENARIO_13_ZERO_USAGE_POOL = "testScenario13ZUP"+System.currentTimeMillis();
	private static final String SCENARIO_13_PLAN = "PLAN $149.99 / Month";
	private static final String SCENARIO_13_ADJUSTMENT_PRODUCT = "adjustMentProduct_13";
	
	// Test data for scenario 14
	private static final String SCENARIO_14_USER = "testScenario14User"+System.currentTimeMillis();
	private static final String SCENARIO_14_ORDER = "testScenario14Order";
	private static final String SCENARIO_14_PLAN_PRODUCT = "testScenario14PlanItem";
	private static final String SCENARIO_14_USAGE_POOL = "testScenario14FUP200Min"+System.currentTimeMillis();
	private static final String SCENARIO_14_PLAN = "200 Mins Plan";
	
	// Test data for scenario 15
	private static final String SCENARIO_15_USER = "testScenario15User"+System.currentTimeMillis();
	private static final String SCENARIO_15_MONTHLY_ORDER = "testScenario15MonthlyOrder";
	private static final String SCENARIO_15_PLAN_PRODUCT = "testScenario15PlanItem";
	private static final String SCENARIO_15_ZERO_USAGE_POOL = "testScenario15ZUP"+System.currentTimeMillis();
	private static final String SCENARIO_15_PLAN = "testScenario15PlanDormancy";

	// Test data for scenario 16
	private static final String SCENARIO_16_USER = "testScenario16User"+System.currentTimeMillis();
	private static final String SCENARIO_16_MONTHLY_ORDER = "testScenario16MonthlyOrder";
	private static final String SCENARIO_16_ONE_TIME_ORDER = "testScenario16OneTimeOrder";
	private static final String SCENARIO_16_SALES_CREDIT_ORDER = "testScenario16SalesCreditOrder";
	private static final String SCENARIO_16_PLAN_PRODUCT = "testScenario16PlanItem";
	private static final String SCENARIO_16_USAGE_POOL = "testScenario16FUP450Min"+System.currentTimeMillis();
	private static final String SCENARIO_16_PLAN = "testScenario16Plan450Min";
	
	// Test data for scenario 17
	private static final String SCENARIO_17_USER = "testScenario17User"+System.currentTimeMillis();
	private static final String SCENARIO_17_ONE_TIME_ORDER = "testScenario17OneTimeOrder";
	private static final String SCENARIO_17_PLAN_1_ORDER = "testScenario17Order1";
	private static final String SCENARIO_17_PLAN_2_ORDER = "testScenario17Order2";
	private static final String SCENARIO_17_PLAN_3_ORDER = "testScenario17Order3";
	private static final String SCENARIO_17_PLAN_PRODUCT1 = "testScenario17PlanItem1";
	private static final String SCENARIO_17_PLAN_PRODUCT2 = "testScenario17PlanItem2";
	private static final String SCENARIO_17_USAGE_POOL1 = "testScenario17FUP500Min"+System.currentTimeMillis();
	private static final String SCENARIO_17_USAGE_POOL2 = "testScenario17FUP225Min"+System.currentTimeMillis();
	private static final String SCENARIO_17_PLAN1 = "500 Min $29.99 / Month";
	private static final String SCENARIO_17_PLAN2 = "450 Min Plan - $399.99 / Month";
	private static final String SCENARIO_17_ADJUSTMENT_ORDER = "adjustment-17";
	
	// Test data for scenario 18
	private static final String SCENARIO_18_USER = "testScenario18User"+System.currentTimeMillis();
	private static final String SCENARIO_18_MONTHLY_ORDER = "testScenario18MonthlyOrder";
	private static final String SCENARIO_18_ONE_TIME_ORDER = "testScenario18OneTimeOrder";
	private static final String SCENARIO_18_PLAN_PRODUCT = "testScenario18PlanItem";
	private static final String SCENARIO_18_USAGE_POOL = "testScenario18FUP100Min"+System.currentTimeMillis();
	private static final String SCENARIO_18_PLAN = "testScenario18Plan100";

    private int autoPaymentApplication;

	@BeforeClass
	public void initializeTests() {
		testBuilder = getTestEnvironment();
		
		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

            // activate autoPayment in billing configuration
            BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
            if(null != config) {
                autoPaymentApplication = config.getAutoPaymentApplication();
                if(0 == autoPaymentApplication){
                    config.setAutoPaymentApplication(1);
                    api.createUpdateBillingProcessConfiguration(config);
                }
            }

			// Creating account type
			buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
			
			// Creating mediated usage category
			buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
			
			// Creating usage products
			buildAndPersistFlatProduct(envBuilder, api, subScriptionProd01, false, envBuilder.idForCode(testCat1), "100.00", true);
			
			buildAndPersistFlatProduct(envBuilder, api, subScriptionProd17, false, envBuilder.idForCode(testCat1), "29.99", true);

			// Scenario 2: Creating a product with price 95.00
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_02_PRODUCT, false, envBuilder.idForCode(testCat1), "95.00", true);
			
			// Usage product item ids 
			List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
			
			Calendar pricingDate = Calendar.getInstance();
			pricingDate.set(Calendar.YEAR, 2014);
			pricingDate.set(Calendar.MONTH, 6);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);
			
			// Creating Adjustment category
			buildAndPersistCategory(envBuilder, api, adjustMentCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT);
						
			// Creating Adjustment product
			buildAndPersistFlatProduct(envBuilder, api, adjustMentProduct, false, envBuilder.idForCode(adjustMentCategory), "30.00", false);
			
			// Creating Fees category
			buildAndPersistCategory(envBuilder, api, feesCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_PENALTY);
									
			// Creating SetUp Fees product
			buildAndPersistFlatProduct(envBuilder, api, setUpFeeProduct, false, envBuilder.idForCode(feesCategory), "49.00", false);
						
			// Creating Late Fees product
			buildAndPersistLinePercentageProduct(envBuilder, api, lateFeesProduct, false, envBuilder.idForCode(feesCategory), "1.50", false);
						
			// Creating Late Fee Changes Product
			buildAndPersistFlatProduct(envBuilder, api, lateFeeChagresProduct, false, envBuilder.idForCode(feesCategory), "2", false);
						
			// Creating Tax category
			buildAndPersistCategory(envBuilder, api, taxCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_TAX);
												
			// Creating Tax product
			buildAndPersistFlatProduct(envBuilder, api, taxProduct, false, envBuilder.idForCode(taxCategory), "49.00", false);
			
			PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			
			// creating Plans, Usage Pools for scenario 01
			
			// creating usage pool with 100 free minutes
			buildAndPersistUsagePool(envBuilder,api,usagePoolO1, "100", envBuilder.idForCode(testCat1), items);
			
			// creating 100 min plan
			buildAndPersistPlan(envBuilder,api, plan01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
					envBuilder.idForCode(subScriptionProd01), Arrays.asList(envBuilder.idForCode(usagePoolO1)),planItemProd01WS, planItemProd02WS, planItemProd03WS);
			
			// creating Plans, Usage Pools for scenario 05 
						
			// creating usage pool with 1000 free minutes
			buildAndPersistUsagePool(envBuilder,api,usagePoolO5, "1000", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, subScriptionProd05, false, envBuilder.idForCode(testCat1), "1000.00", true);
			
			// creating 1000 min plan 
			buildAndPersistPlan(envBuilder,api, plan05, "1000 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
					envBuilder.idForCode(subScriptionProd05), Arrays.asList(envBuilder.idForCode(usagePoolO5)),planItemProd01WS, planItemProd02WS, planItemProd03WS);

			/*
			 *  creating Plans, Usage Pools for scenario 06
			 *  usage pool 1150 mins
			 *  product : Rs 1000.00
			 *  plan :  1150 min plan
			 */
			buildAndPersistUsagePool(envBuilder,api,usagePoolO6, "1150", envBuilder.idForCode(testCat1), items);
						buildAndPersistFlatProduct(envBuilder, api, subScriptionProd06, false, envBuilder.idForCode(testCat1), "1000.00", true);

			// creating 1150 min plan 
			buildAndPersistPlan(envBuilder,api, plan06, "1150 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
						envBuilder.idForCode(subScriptionProd06), Arrays.asList(envBuilder.idForCode(usagePoolO6)),planItemProd01WS, planItemProd02WS, planItemProd03WS);
			
			// Creating 90 Min Plan for Scenario #09
			pricingDate.set(Calendar.YEAR, 2015);
			pricingDate.set(Calendar.MONTH, 4);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);

			PlanItemWS planItemProd1 = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.30", pricingDate.getTime());
			PlanItemWS planItemProd2 = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.30", pricingDate.getTime());
			PlanItemWS planItemProd3 = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.30", pricingDate.getTime());
			
			//Create Adjustment Category
			buildAndPersistCategory(envBuilder, api, SCENARIO_ADJUSTMENT_PRODUCT_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT);
			
			// Created 90 FUP,FLAT PRODUCT AND PLAN with 90 Min
			buildAndPersistUsagePool(envBuilder, api, SCENARIO_09_90_MIN_USAGE_POOL, "90", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_09_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "99.99", true);
			buildAndPersistPlan(envBuilder,api, SCENARIO_09_PLAN, "90 Mins Plan - $99 / Month", MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(SCENARIO_09_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_09_90_MIN_USAGE_POOL)),planItemProd1, planItemProd2, planItemProd3);
			
			/*
			 *  creating Plans, Usage Pools for scenario 10
			 *  usage pool 250 mins
			 *  product : Rs 199.99
			 *  plan :  250 min plan
			 */
			buildAndPersistUsagePool(envBuilder,api,usagePool10, "250", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, subScriptionProd10, false, envBuilder.idForCode(testCat1), "199.99", true);

			// creating plan for scenario 10
			buildAndPersistPlan(envBuilder,api, plan10, "250 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
					envBuilder.idForCode(subScriptionProd10), Arrays.asList(envBuilder.idForCode(usagePool10)),planItemProd01WS, planItemProd02WS, planItemProd03WS);
			
			//Creating Plan with 725 FUP for scenario 11
			pricingDate.set(Calendar.YEAR, 2016);
			pricingDate.set(Calendar.MONTH, 4);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);

			PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

			// Created FUP,FLAT PRODUCT AND PLAN with 725 Free Min Plan
			buildAndPersistUsagePool(envBuilder, api, SCENARIO_11_ZERO_USAGE_POOL, "725", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_11_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "399.99", true);
			buildAndPersistPlan(envBuilder,api, SCENARIO_11_PLAN, "725 Free Min Plan", MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(SCENARIO_11_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_11_ZERO_USAGE_POOL)),planItemProd1WS, planItemProd2WS, planItemProd3WS);

			//Scenario 13
			pricingDate.set(Calendar.YEAR, 2016);
			pricingDate.set(Calendar.MONTH, 10);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);

			PlanItemWS planItemProd13_1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd13_2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
			PlanItemWS planItemProd13_3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

			// Created FUP,FLAT PRODUCT AND PLAN with 135 Free Min
			buildAndPersistUsagePool(envBuilder, api, SCENARIO_13_ZERO_USAGE_POOL, "135", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_13_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "149.99", true);
			buildAndPersistPlan(envBuilder,api, SCENARIO_13_PLAN, "135 Free Min Plan", MONTHLY_ORDER_PERIOD,
								envBuilder.idForCode(SCENARIO_13_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_13_ZERO_USAGE_POOL)),planItemProd13_1WS, planItemProd13_3WS, planItemProd13_3WS);
			// Create Adjustment product
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_13_ADJUSTMENT_PRODUCT, false, envBuilder.idForCode(SCENARIO_ADJUSTMENT_PRODUCT_CATEGORY), "379.37", false);		
			
			//Creating Plan 200 Mins for scenario 14
			pricingDate.set(Calendar.YEAR, 2016);
			pricingDate.set(Calendar.MONTH, 9);
			pricingDate.set(Calendar.DAY_OF_MONTH, 1);

			planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.89", pricingDate.getTime());
			planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.89", pricingDate.getTime());
			planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.89", pricingDate.getTime());

			// Created FUP,FLAT PRODUCT AND PLAN with 200 Min Plan
			buildAndPersistUsagePool(envBuilder, api, SCENARIO_14_USAGE_POOL, "200", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_14_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "99.00", true);
			buildAndPersistPlan(envBuilder,api, SCENARIO_14_PLAN, "200 Mins Plan", MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(SCENARIO_14_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_11_ZERO_USAGE_POOL)),planItemProd1WS, planItemProd2WS, planItemProd3WS);
			
			planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.09", pricingDate.getTime());
			planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.09", pricingDate.getTime());
			planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.09", pricingDate.getTime());
			
			// Created FUP,FLAT PRODUCT AND PLAN with 500 Min Plan 
			buildAndPersistUsagePool(envBuilder, api, SCENARIO_17_USAGE_POOL1, "500", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_17_PLAN_PRODUCT1, false, envBuilder.idForCode(testCat1), "19.99", true);
			buildAndPersistPlan(envBuilder,api, SCENARIO_17_PLAN1, "500 Min $29.99 / Month", MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(SCENARIO_17_PLAN_PRODUCT1), Arrays.asList(envBuilder.idForCode(SCENARIO_17_USAGE_POOL1)),planItemProd1WS, planItemProd2WS, planItemProd3WS);
			
			planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.89", pricingDate.getTime());
			planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.89", pricingDate.getTime());
			planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.89", pricingDate.getTime());
			
			// Created FUP,FLAT PRODUCT AND PLAN with 225 Min Plan 
			buildAndPersistUsagePool(envBuilder, api, SCENARIO_17_USAGE_POOL2, "450", envBuilder.idForCode(testCat1), items);
			buildAndPersistFlatProduct(envBuilder, api, SCENARIO_17_PLAN_PRODUCT2, false, envBuilder.idForCode(testCat1), "399.99", true);
			buildAndPersistPlan(envBuilder,api, SCENARIO_17_PLAN2, "450 Min Plan - $399.99 / Month", MONTHLY_ORDER_PERIOD,
					envBuilder.idForCode(SCENARIO_17_PLAN_PRODUCT2), Arrays.asList(envBuilder.idForCode(SCENARIO_17_USAGE_POOL2)),planItemProd1WS, planItemProd2WS, planItemProd3WS);
			
			// Configuring Telco Usage Item Manager Task
			Hashtable<String, String> telcoPluginParameter = new Hashtable<>();
			telcoPluginParameter.put("DNIS_Field_Name", "DNIS");
			updatePlugin(FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, testBuilder, telcoPluginParameter);
						
			// Configuring Order Line Based Composition Task
			updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_LINE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);
			
			// Configuring Invoice Penalty Task
			Hashtable<String, String> parameters = new Hashtable<>();
			
			parameters.put("penalty_item_id", envBuilder.idForCode(lateFeesProduct).toString());
			parameters.put("penalty_charge_item_id", envBuilder.idForCode(lateFeeChagresProduct).toString());
			PluggableTaskWS plugins [] = api.getPluginsWS(api.getCallerCompanyId(), INVOICE_PENALTY_TASK_CLASS_NAME);
			if(null == plugins || plugins.length == 0) {
				buildAndPersistPlugIn(envBuilder, api, INVOICE_PENALTY_TASK_CLASS_NAME, parameters);
			} else {
				updatePlugin(plugins[0].getId(), INVOICE_PENALTY_TASK_CLASS_NAME, testBuilder, parameters);
			}
			
		}).test((testEnv, testEnvBuilder) -> {
			
			assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
			assertNotNull("MediatedUsage Category Creation Failed", testEnvBuilder.idForCode(testCat1));
			assertNotNull("Adjustment Category Creation Failed", testEnvBuilder.idForCode(adjustMentCategory));
			assertNotNull("Fees Category Creation Failed", testEnvBuilder.idForCode(feesCategory));
			assertNotNull("Tax Category Creation Failed", testEnvBuilder.idForCode(taxCategory));
			assertNotNull(subScriptionProd01+" Product Creation Failed", testEnvBuilder.idForCode(subScriptionProd01));
			assertNotNull(adjustMentProduct+" Product Creation Failed", testEnvBuilder.idForCode(adjustMentProduct));
			assertNotNull(taxProduct+" Product Creation Failed", testEnvBuilder.idForCode(taxProduct));
			assertNotNull(setUpFeeProduct+" Product Creation Failed", testEnvBuilder.idForCode(setUpFeeProduct));
			assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(usagePoolO1));
			assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(plan01));
			assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(usagePoolO5));
			assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(plan05));
			
			//scenario 2
			assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SCENARIO_02_PRODUCT));

			//Scenario 11
			assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(SCENARIO_11_ZERO_USAGE_POOL));
			assertNotNull(SCENARIO_11_PLAN_PRODUCT+" Product Creation Failed", testEnvBuilder.idForCode(SCENARIO_11_PLAN_PRODUCT));
			assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(SCENARIO_11_PLAN));
			
			//Scenario 13
			assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(SCENARIO_13_ZERO_USAGE_POOL));
			assertNotNull(SCENARIO_13_PLAN_PRODUCT+" Product Creation Failed", testEnvBuilder.idForCode(SCENARIO_13_PLAN_PRODUCT));
			assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(SCENARIO_13_PLAN));

		});
		
		
	}

	@AfterClass
	public void tearDown() {
		// Configuring Telco Usage Item Manager Task
		updatePlugin(FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID, 
				FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, testBuilder, null);
					
		//Configuring Order Line Based Composition Task
		updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, 
				FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);
		
		testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		testBuilder.removeEntitiesCreatedOnJBilling();

        // configure the same AutopaymentApplication value as before starting the test
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        if(null != config) {
            autoPaymentApplication = config.getAutoPaymentApplication();
            config.setAutoPaymentApplication(autoPaymentApplication);
            api.createUpdateBillingProcessConfiguration(config);
        }

        if (null != envHelper) {
			envHelper = null;
		}
		if (null != testBuilder) {
			testBuilder = null;
		}
	}

	private TestBuilder getTestEnvironment() {

		return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {

			this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
		});
	}

	/**
	 * Invoice Template Scenarios Sheet #1
	 * User that has last invoice fully paid.
	 */
	@Test
	public void testInvoiceSummaryScenario01() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #1 - User that has last invoice fully paid");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 8);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
				

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 7);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 6);
				paymentDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
			AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
			
			Map<Integer, Integer> productAssetMap = new HashMap<>();
			productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());
			
			Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
			productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
			productQuantityMap.put(environment.idForCode(subScriptionProd01), BigDecimal.ONE);
			
			 List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "150", "08/10/2016");
		        
			 InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Sep 2016
			 scenario01.createUser(user01,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
			 		   .makePayment("50", paymentDate.getTime(), false)
			 		   //  creating subscription order on 1st of Aug 2016 
			 		   .createOrder("testSubScriptionOrderO1", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap, productAssetMap, false)
			 		   //  trigger Inbound Mediation 
			 		   .triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs)
	                   //  creating fees order on 1st of Aug 2016		 				   
			 		   .createOrder("OneTimeFeeOrderO1", activeSince.getTime(),null, ONE_TIME_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
			 				   Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false);
			 		   
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 8);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(user01)
								// generating invoice for 1'st Sep 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user01));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #1 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-50.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("49.00"))
															.addExpectedLastInvoiceDate(null)
															.addExpectedMonthlyCharges(new BigDecimal("104.99"))
															.addExpectedNewCharges(new BigDecimal("201.49"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("151.49"))
															.addExpectedUsageCharges(new BigDecimal("47.50"))
															.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
															.validate();
				
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 9);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 8);
				paymentDate.set(Calendar.DAY_OF_MONTH, 20);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				List<String> identifiers = getAssetIdentifiers(user01);
				scenarioBuilder.selectUserByName(user01)
							   .makePayment("129", paymentDate.getTime(), false)
							   .makeCreditPayment("5", paymentDate.getTime())
							   .triggerMediation(INBOUND_MEDIATION_LAUNCHER, buildInboundCDR(identifiers, "15", "08/10/2016"))
								// generating invoice for 1'st Oct 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user01));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #1 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-129.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("-5.00"))
															.addExpectedFeesCharges(new BigDecimal("0.26"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("104.99"))
															.addExpectedNewCharges(new BigDecimal("114.50"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("136.99"))
															.addExpectedUsageCharges(new BigDecimal("14.25"))
															.addExpectedAmountOfLastStatement(new BigDecimal("151.49"))
															.validate();
				
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 10);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 9);
				paymentDate.set(Calendar.DAY_OF_MONTH, 20);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				List<String> identifiers = getAssetIdentifiers(user01);
				scenarioBuilder.selectUserByName(user01)
							   .makePayment("120", paymentDate.getTime(), false)
							   .makeCreditPayment("5", paymentDate.getTime())
							   .triggerMediation(INBOUND_MEDIATION_LAUNCHER, buildInboundCDR(identifiers, "5", "08/10/2016"))
								// generating invoice for 1'st Nov 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user01));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #1 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-120.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("-5.00"))
															.addExpectedFeesCharges(new BigDecimal("0.18"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("104.99"))
															.addExpectedNewCharges(new BigDecimal("104.92"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("121.91"))
															.addExpectedUsageCharges(new BigDecimal("4.75"))
															.addExpectedAmountOfLastStatement(new BigDecimal("136.99"))
															.validate();
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 11);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 10);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(user01)
								// generating invoice for 1'st Dec 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user01));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #1 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("1.83"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("104.99"))
															.addExpectedNewCharges(new BigDecimal("106.82"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("228.73"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("121.91"))
															.validate();
				
			});
			logger.debug("Invoice template test scenario #1 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(user01), 10, 0))
			   	  .forEach(invoice -> {
			   		  api.deleteInvoice(invoice.getId());
			   	  });
		}
		
				 
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #2
	 * User that has last invoice unpaid and hence there will one carried invoice
	 */
	@Test
	public void testInvoiceSummaryScenario02() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #2 - User that has last invoice unpaid and hence there will one carried invoice");
					//Data set-up to generate user's last unpaid invoice
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 4);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 3);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(environment.idForCode(SCENARIO_02_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario02 = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Set NID as 1st of May 2016
					scenario02.createUser(SCENARIO_02_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							//Creating subscription order on 1st of Apr 2016
							.createOrder(SCENARIO_02_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
							productQuantityMap, null, false);

					Calendar runDate = Calendar.getInstance();
					runDate.set(Calendar.YEAR, 2016);
					runDate.set(Calendar.MONTH, 4);
					runDate.set(Calendar.DAY_OF_MONTH, 1);

					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Generating invoice for 1st May 2016
					scenarioBuilder
						.selectUserByName(SCENARIO_02_USER)
						.generateInvoice(runDate.getTime(), true);

					// Validation of unpaid invoice
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #2 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(null)
																.addExpectedMonthlyCharges(new BigDecimal("95.00"))
																.addExpectedNewCharges(new BigDecimal("95.00"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("95.00"))
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																.validate();

					Calendar activeUntil = Calendar.getInstance();
					activeUntil.set(Calendar.YEAR, 2016);
					activeUntil.set(Calendar.MONTH, 4);
					activeUntil.set(Calendar.DAY_OF_MONTH, 1);

					OrderWS orderWS = api.getOrder(environment.idForCode(scenario02.getOrderCodes().get(0)));
					orderWS.setActiveUntil(activeUntil.getTime());
					api.updateOrder(orderWS, null);

					//Validation of first scenario (invoice date 6.1.2016) post unpaid invoice
					}).validate((testEnv, envBuilder) -> {

						Calendar runDate = Calendar.getInstance();
						runDate.set(Calendar.YEAR, 2016);
						runDate.set(Calendar.MONTH, 5);
						runDate.set(Calendar.DAY_OF_MONTH, 1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Jun 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_02_USER)
							.generateInvoice(runDate.getTime(), true);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(runDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #2 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("1.43"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("3.06"))
																	.addExpectedNewCharges(new BigDecimal("4.49"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("99.49"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("95.00"))
																	.validate();

					//Validation of second scenario (invoice date 7.1.2016) post unpaid invoice
					}).validate((testEnv, envBuilder) -> {

						Calendar runDate = Calendar.getInstance();
						runDate.set(Calendar.YEAR, 2016);
						runDate.set(Calendar.MONTH, 6);
						runDate.set(Calendar.DAY_OF_MONTH, 1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Jul 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_02_USER)
							.generateInvoice(runDate.getTime(), false);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(runDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("1.49"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("0.00"))
																	.addExpectedNewCharges(new BigDecimal("1.49"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("100.98"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("99.49"))
																	.validate();

					//Validation of third scenario (invoice date 8.1.2016) post unpaid invoice
					}).validate((testEnv, envBuilder) -> {

						Calendar runDate = Calendar.getInstance();
						runDate.set(Calendar.YEAR, 2016);
						runDate.set(Calendar.MONTH, 7);
						runDate.set(Calendar.DAY_OF_MONTH, 1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Aug 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_02_USER)
							.generateInvoice(runDate.getTime(), false);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(runDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #2 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("1.51"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("0.00"))
																	.addExpectedNewCharges(new BigDecimal("1.51"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("102.49"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("100.98"))
																	.validate();

					//Validation of fourth scenario (invoice date 9.1.2016) post unpaid invoice
					}).validate((testEnv, envBuilder) -> {

						Calendar runDate = Calendar.getInstance();
						runDate.set(Calendar.YEAR, 2016);
						runDate.set(Calendar.MONTH, 8);
						runDate.set(Calendar.DAY_OF_MONTH, 1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Sep 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_02_USER)
							.generateInvoice(runDate.getTime(), false);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(runDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #2 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("1.54"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("0.00"))
																	.addExpectedNewCharges(new BigDecimal("1.54"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("104.03"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("102.49"))
																	.validate();
					});
				logger.debug("Invoice template test scenario #2 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_02_USER), 10, 0))
				.forEach(invoice -> {
					api.deleteInvoice(invoice.getId());
				});
		}
	}

	 /**
	 * Invoice Template Scenarios Sheet #3.
	 * A new user with one signup payment.
	 */
	@Test
	public void testInvoiceSummaryScenario03() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #3 - A new user with one signup payment.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 11);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 9);
					paymentDate.set(Calendar.DAY_OF_MONTH, 31);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 10);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					AssetWS scenario03Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

					Map<Integer, Integer> productAssetMap = new HashMap<>();
					productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario03Asset.getId());

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap.put(environment.idForCode(subScriptionProd01), BigDecimal.ONE);

					List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario03Asset.getIdentifier()), "120", "11/10/2016");

					InvoiceSummaryScenarioBuilder scenario03 = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Set NID as 1st of Nov 2016
					scenario03.createUser(SCENARIO_03_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							  //Make sign-up payment on 31st of Oct 2016
							  .makePayment("219.00", paymentDate.getTime(), false)
							  //Create monthly subscription order on 1st of Nov 2016
							  .createOrder(SCENARIO_03_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
											productQuantityMap, productAssetMap, false)
							  //Trigger inbound mediation
							  .triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

					//Validating invoice data generated post sign-up payment
					}).validate((testEnv, envBuilder) -> {

						Calendar runDate = Calendar.getInstance();
						runDate.set(Calendar.YEAR, 2016);
						runDate.set(Calendar.MONTH, 11);
						runDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Dec 2007
						scenarioBuilder
							.selectUserByName(SCENARIO_03_USER)
							.generateInvoice(runDate.getTime(), true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_03_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #3 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-219.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("104.99"))
																	.addExpectedNewCharges(new BigDecimal("123.99"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-95.01"))
																	.addExpectedUsageCharges(new BigDecimal("19.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();
					});
				logger.debug("Invoice template test scenario #3 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(SCENARIO_03_USER);
			Arrays.stream(api.getPaymentsByUserId(userid))
					.forEach(paymentId -> {
						api.deletePayment(paymentId);
					});
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
					.forEach(invoice -> {
						api.deleteInvoice(invoice.getId());
					});
		}
	}

	 /**
	 * Invoice Template Scenarios Sheet #4
	 * Generate invoice and then delete it.
	 */
	@Test
	public void testInvoiceSummaryScenario04() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #4 - Invoice Delete.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 1);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 0);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					buildAndPersistFlatProduct(envBuilder, api, SCENARIO_04_PRODUCT, false, envBuilder.idForCode(testCat1), "95.00", true);

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(environment.idForCode(SCENARIO_04_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario04 = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Set NID as 1st of Feb 2016
					scenario04.createUser(SCENARIO_04_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							//Creating subscription order on 1st of Jan 2016
							.createOrder(SCENARIO_04_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
										productQuantityMap, null, false)
							.createOrder(SCENARIO_04_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
										Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false)
							.makePayment("49.00", activeSince.getTime(), false);

					//Generate invoice, delete invoice and then ensure itemizedAccountWS should be null.
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 1);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Feb 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_04_USER)
							.generateInvoice(billingRunDate.getTime(), true);

						// Validating generated invoice summary data
						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_04_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #4 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-49.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("49.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("95.00"))
																	.addExpectedNewCharges(new BigDecimal("144.00"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("95.00"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();

						api.deleteInvoice(invoice.getId());
						itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						assertNull("ItemizedAccountWS should be null.", itemizedAccountWS);
					});
				logger.debug("Invoice template test scenario #4 has been pa	ssed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_04_USER), 10, 0))
				.forEach(invoice -> {
					api.deleteInvoice(invoice.getId());
				});
		}
	}

	 /**
	 * Invoice Template Scenarios Sheet #5
	 * User that has one carried invoice and 
	 * partial payment against the last invoice. 
	 */
	@Test
	public void testInvoiceSummaryScenario05() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #5 - User that has one carried invoice and partial payment against the last invoice.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 9);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
				

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 8);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);
				
		   final JbillingAPI api = envBuilder.getPrancingPonyApi();
			AssetWS scenario05Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
			
			Map<Integer, Integer> productAssetMap = new HashMap<>();
			productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario05Asset.getId());
			
			Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
			productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
			productQuantityMap.put(environment.idForCode(subScriptionProd05), BigDecimal.ONE);
			
			 List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario05Asset.getIdentifier()), "600","09/10/2016");
			 InvoiceSummaryScenarioBuilder scenario05 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Oct 2016
			 scenario05.createUser(user05,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
			 		   //  creating subscription order on 1st of Sep 2016 
			 		   .createOrder("sub-05", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap, productAssetMap, false)
	                   //  creating fees order on 1st of Sep 2016		 				   
			 		   .createOrderWithPrice("adjustment-05", activeSince.getTime(),null, ONE_TIME_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), BigDecimal.ONE), 
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), new BigDecimal("973.18")))
			 		   .triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
			 		   
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 9);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(user05)
								// generating invoice for 1'st Oct 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user05));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #5 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("973.18"))
															.addExpectedFeesCharges(new BigDecimal("0.00"))
															.addExpectedLastInvoiceDate(null)
															.addExpectedMonthlyCharges(new BigDecimal("1004.99"))
															.addExpectedNewCharges(new BigDecimal("1978.17"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("1978.17"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
															.validate();
				
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 10);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 9);
				paymentDate.set(Calendar.DAY_OF_MONTH, 20);
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(user05)
							   .makePayment("1000", paymentDate.getTime(), false)
								// generating invoice for 1'st Nov 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user05));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #5 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-1000.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("14.67"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("1004.99"))
															.addExpectedNewCharges(new BigDecimal("1019.66"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("1997.83"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("1978.17"))
															.validate();
				
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 11);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 10);
				paymentDate.set(Calendar.DAY_OF_MONTH, 20);
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(user05)
							   .makePayment("750", paymentDate.getTime(), false)
								// generating invoice for 1'st Dec 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user05));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #5 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-750.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("18.72"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("1004.99"))
															.addExpectedNewCharges(new BigDecimal("1023.71"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("2271.54"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("1997.83"))
															.validate();
				
				
			});
			logger.debug("Invoice template test scenario #5 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(user05), 10, 0))
			   	  .forEach(invoice -> {
			   		  api.deleteInvoice(invoice.getId());
			   	  });
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #6
	 * User with refund payment. 
	 */
	@Test
	public void testInvoiceSummaryScenario06() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given( envBuilder -> {
				logger.debug("Scenario #6 - User with refund payment.");
				//October 1st 2016
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 9);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				//September 1st 2016
				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 8);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				AssetWS scenario06Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario06Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(subScriptionProd06), BigDecimal.ONE);

				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario06Asset.getIdentifier()), "150", "09/10/2016");

				InvoiceSummaryScenarioBuilder scenario06 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of Oct 2016
				scenario06.createUser(user06,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)

				//  creating subscription order on 1st of Sept 2016 
				.createOrder("testSubScriptionOrderO6", activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
						productQuantityMap, productAssetMap, false)
						//  trigger Inbound Mediation 
						.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs)
						.createOrderWithPrice("adjustment-05", activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId,
								ORDER_CHANGE_STATUS_APPLY_ID, false, Collections.singletonMap(environment.idForCode(adjustMentProduct), BigDecimal.ONE),
								Collections.singletonMap(environment.idForCode(adjustMentProduct), new BigDecimal("1973.18")));

		}).validate((testEnv, envBuilder) -> {
			Calendar runDate = Calendar.getInstance();
			runDate.set(Calendar.YEAR, 2016);
			runDate.set(Calendar.MONTH, 9);
			runDate.set(Calendar.DAY_OF_MONTH, 1);
			
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
			scenarioBuilder.selectUserByName(user06)
							// generating invoice for 1'st Oct 2016
						   .generateInvoice(runDate.getTime(),true);

			InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user06));
			ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
			logger.debug("Scenario #6 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

			new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("1973.18"))
															.addExpectedFeesCharges(new BigDecimal("0.00"))
															.addExpectedLastInvoiceDate(null)
															.addExpectedMonthlyCharges(new BigDecimal("1004.99"))
															.addExpectedNewCharges(new BigDecimal("2978.17"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("2978.17"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
															.validate();
			
			}).validate((testEnv, envBuilder) -> {
			
			Calendar runDate = Calendar.getInstance();
			runDate.set(Calendar.YEAR, 2016);
			runDate.set(Calendar.MONTH, 10);
			runDate.set(Calendar.DAY_OF_MONTH, 1);

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 9);
			paymentDate.set(Calendar.DAY_OF_MONTH, 20);
			
			Calendar lastInvoiceDate = Calendar.getInstance();
			lastInvoiceDate.setTime(runDate.getTime());
			lastInvoiceDate.add(Calendar.MONTH, -1);

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			List<String> inboundCdrs = buildInboundCDR(getAssetIdentifiers(user06), "10", "10/10/2016");
			InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
			scenarioBuilder.selectUserByName(user06)
						   .makePayment("1000", paymentDate.getTime(), false)
						   .triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs)
						   // generating invoice for 1'st Nov 2016
						   .generateInvoice(runDate.getTime(),true);
			InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user06));
			ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
			logger.debug("Scenario #6 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
			
			new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-1000.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("29.67"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("1004.99"))
															.addExpectedNewCharges(new BigDecimal("1044.16"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("3022.33"))
															.addExpectedUsageCharges(new BigDecimal("9.50"))
															.addExpectedAmountOfLastStatement(new BigDecimal("2978.17"))
															.validate();
			
			}).validate((testEnv, envBuilder) -> {
			
			Calendar runDate = Calendar.getInstance();
			runDate.set(Calendar.YEAR, 2016);
			runDate.set(Calendar.MONTH, 11);
			runDate.set(Calendar.DAY_OF_MONTH, 1);
			
			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 10);
			paymentDate.set(Calendar.DAY_OF_MONTH, 2);
			
			Calendar lastInvoiceDate = Calendar.getInstance();
			lastInvoiceDate.setTime(runDate.getTime());
			lastInvoiceDate.add(Calendar.MONTH, -1);
			
			
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
			scenarioBuilder.selectUserByName(user06)
						   .makePayment("750", paymentDate.getTime(), false)
							// generating invoice for 1'st Dec 2016
						   .generateInvoice(runDate.getTime(),true);
			InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(user06));
			ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
			logger.debug("Scenario #6 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
			
			new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-750.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("34.08"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("1004.99"))
															.addExpectedNewCharges(new BigDecimal("1039.07"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("3311.40"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("3022.33"))
															.validate();
			});
			logger.debug("Invoice template test scenario #6 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(user06);
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
			.forEach(invoice -> {
				api.deleteInvoice(invoice.getId());
			});
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #7
	 * Users with 2 or more than 2 carried invoices
	 */
	@Test
	public void testInvoiceSummaryScenario07() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #7 - Users with 2 or more than 2 carried invoices.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 3);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
				

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 2);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar activeUntil = Calendar.getInstance();
				activeUntil.set(Calendar.YEAR, 2016);
				activeUntil.set(Calendar.MONTH, 2);
				activeUntil.set(Calendar.DAY_OF_MONTH, 31);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
			
			Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
			productQuantityMap.put(environment.idForCode(subScriptionProd01), BigDecimal.ONE);
			
			 InvoiceSummaryScenarioBuilder scenario07 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Next Invoice Date 1st of April 2016
			 scenario07.createUser(SCENARIO_07_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
			 		   .createOrder(subScriptionProd01, activeSince.getTime(),activeUntil.getTime(), MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap, null, false);

			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 3);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_07_USER)
								// generating invoice for 1'st April 2016
							   .generateInvoice(runDate.getTime(), true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_07_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #7 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("0.00"))
															.addExpectedLastInvoiceDate(null)
															.addExpectedMonthlyCharges(new BigDecimal("100.00"))
															.addExpectedNewCharges(new BigDecimal("100.00"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("100.00"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
															.validate();
				
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 4);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				List<String> identifiers = getAssetIdentifiers(SCENARIO_07_USER);
				scenarioBuilder.selectUserByName(SCENARIO_07_USER)
								// generating invoice for 1'st May 2016
							   .generateInvoice(runDate.getTime(), false);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_07_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #7 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("1.50"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("0.00"))
															.addExpectedNewCharges(new BigDecimal("1.50"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("101.50"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("100.00"))
															.validate();
				
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 5);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				List<String> identifiers = getAssetIdentifiers(SCENARIO_07_USER);
				scenarioBuilder.selectUserByName(SCENARIO_07_USER)
								// generating invoice for 1'st June 2016
							   .generateInvoice(runDate.getTime(), false);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_07_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #7 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("1.52"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("0.00"))
															.addExpectedNewCharges(new BigDecimal("1.52"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("103.02"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("101.50"))
															.validate();
				
			}).validate((testEnv, envBuilder) -> {
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 6);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_07_USER)
								// generating invoice for 1'st July 2016
							   .generateInvoice(runDate.getTime(), false);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_07_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #7 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("1.55"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("0.00"))
															.addExpectedNewCharges(new BigDecimal("1.55"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("104.57"))
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("103.02"))
															.validate();
				
			});
			logger.debug("Invoice template test scenario #7 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_07_USER), 10, 0))
			   	  .forEach(invoice -> {
			   		  api.deleteInvoice(invoice.getId());
			   	  });
		}
	}

	 /**
	 * Invoice Template Scenarios Sheet #8.
	 * Users with one payment linked to many invoices and has refund
	 */
	@Test
	public void testInvoiceSummaryScenario08() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #8 - Users with one payment linked to many invoices and has refund.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 7);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 6);
					paymentDate.set(Calendar.DAY_OF_MONTH, 20);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 6);
					activeSince.set(Calendar.DAY_OF_MONTH, 13);

					// Data set-up to create item/plan and orders
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					Calendar pricingDate = Calendar.getInstance();
					pricingDate.set(Calendar.YEAR, 2016);
					pricingDate.set(Calendar.MONTH, 6);
					pricingDate.set(Calendar.DAY_OF_MONTH, 1);

					List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
					PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
					PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
					PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

					buildAndPersistUsagePool(envBuilder, api, SCENARIO_08_ZERO_USAGE_POOL, "1", envBuilder.idForCode(testCat1), items);
					buildAndPersistFlatProduct(envBuilder, api, SCENARIO_08_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "9.99", true);
					buildAndPersistPlan(envBuilder,api, SCENARIO_08_PLAN, "DORMANCY $9.99 / Month", MONTHLY_ORDER_PERIOD, 
										envBuilder.idForCode(SCENARIO_08_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_08_ZERO_USAGE_POOL)), 
										planItemProd1WS, planItemProd2WS, planItemProd3WS);

					AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
					Map<Integer, Integer> productAssetMap = new HashMap<>();
					productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap.put(environment.idForCode(SCENARIO_08_PLAN_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario08 = new InvoiceSummaryScenarioBuilder(testBuilder);
					//Create customer with NID as 1st of Aug 2016
					scenario08.createUser(SCENARIO_08_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							  //Make sign-up payment on 20th Jul 2016
							  .makePayment("149.00", paymentDate.getTime(), false)
							  //Create monthly subscription order on 13th Jul 2016
							  .createOrder(SCENARIO_08_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
											productQuantityMap, productAssetMap, false)
			 				  //Create one time order on 13th Jul 2016
							  .createOrder(SCENARIO_08_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
											Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false);

					//Validating first invoice generated on 01-Aug-2016
					}).validate((testEnv, envBuilder) -> {
						Date runDate = new DateMidnight(2016, 8, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Aug 2016
						scenarioBuilder.selectUserByName(SCENARIO_08_USER)
									   .generateInvoice(runDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_08_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #8 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-149.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("49.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("9.18"))
																	.addExpectedNewCharges(new BigDecimal("58.18"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-90.82"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();

					//Validating second invoice generated on 01-Sep-2016
					}).validate((testEnv, envBuilder) -> {
						Date runDate = new DateMidnight(2016, 9, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Sep 2016
						scenarioBuilder.selectUserByName(SCENARIO_08_USER)
									   .generateInvoice(runDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						Date lastInvoiceDate = new DateMidnight(2016, 8, 1).toDate();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_08_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate)
																	.addExpectedMonthlyCharges(new BigDecimal("14.98"))
																	.addExpectedNewCharges(new BigDecimal("14.98"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-75.84"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("-90.82"))
																	.validate();

					//Validating third invoice generated on 01-Oct-2016
					}).validate((testEnv, envBuilder) -> {
						Date runDate = new DateMidnight(2016, 10, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Oct 2016
						scenarioBuilder.selectUserByName(SCENARIO_08_USER)
									   .generateInvoice(runDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						Date lastInvoiceDate = new DateMidnight(2016, 9, 1).toDate();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_08_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #8 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate)
																	.addExpectedMonthlyCharges(new BigDecimal("14.98"))
																	.addExpectedNewCharges(new BigDecimal("14.98"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-60.86"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("-75.84"))
																	.validate();

					//Making refund payment and validating last generated invoice again
					}).validate((testEnv, envBuilder) -> {
						Calendar refundDate = Calendar.getInstance();
						refundDate.set(Calendar.YEAR, 2016);
						refundDate.set(Calendar.MONTH, 11);
						refundDate.set(Calendar.DAY_OF_MONTH, 8);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						scenarioBuilder.selectUserByName(SCENARIO_08_USER)
									   //Make refund payment on 8th Dec 2016
									   .makePayment("60.86", refundDate.getTime(), true);

						Date lastInvoiceDate = new DateMidnight(2016, 9, 1).toDate();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_08_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #8 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
						
						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate)
																	.addExpectedMonthlyCharges(new BigDecimal("14.98"))
																	.addExpectedNewCharges(new BigDecimal("14.98"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-60.86"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("-75.84"))
																	.validate();
					});
					logger.debug("Invoice template scenario #8 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(SCENARIO_08_USER);
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
					.forEach(invoice -> {
						api.deleteInvoice(invoice.getId());
					});
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #9
	 * Prorate invoices.
	 */
	@Test
	public void testInvoiceSummaryScenario09() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #9 - Prorate invoice.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 0);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2015);
				activeSince.set(Calendar.MONTH, 11);
				activeSince.set(Calendar.DAY_OF_MONTH, 31);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();

				AssetWS scenario09Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario09Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(SCENARIO_09_PLAN_PRODUCT), BigDecimal.ONE);
				
				InvoiceSummaryScenarioBuilder scenario09 = new InvoiceSummaryScenarioBuilder(testBuilder);
				// Set NID as 1st of Feb 2016
				scenario09.createUser(SCENARIO_09_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
						//Creating subscription order on 1st of Jan 2016
						.createOrder(SCENARIO_09_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
									productQuantityMap, productAssetMap, false)
						.createOrder(SCENARIO_09_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
									Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false)
						.createOrderWithPrice(SCENARIO_09_ADJUSTMENT_ORDER, activeSince.getTime(),null, ONE_TIME_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), BigDecimal.ONE), 
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), new BigDecimal("52.39").negate()))
						.makePayment("148.00", activeSince.getTime(), false);

				}).validate((testEnv, envBuilder) -> {
					
					Date billingRunDate = new DateMidnight(2016, 1, 1).toDate();

					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Generating invoice for 1st March 2016
					scenarioBuilder
						.selectUserByName(SCENARIO_09_USER)
						.generateInvoice(billingRunDate, true);

					// Validating generated invoice summary data
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_09_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #9 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());
					
					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("148.00").negate())
																.addExpectedAdjustmentCharges(new BigDecimal("52.39").negate())
																.addExpectedFeesCharges(new BigDecimal("49.00"))
																.addExpectedLastInvoiceDate(null)
																.addExpectedMonthlyCharges(new BigDecimal("3.39"))
																.addExpectedNewCharges(new BigDecimal("0.00"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("148.00").negate())
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																.validate();
					
				}).validate((testEnv, envBuilder) -> {
					Calendar billingRunDate = Calendar.getInstance();
					billingRunDate.set(Calendar.YEAR, 2016);
					billingRunDate.set(Calendar.MONTH, 1);
					billingRunDate.set(Calendar.DAY_OF_MONTH, 1);
					
					Calendar lastInvoiceDate = Calendar.getInstance();
					lastInvoiceDate.setTime(billingRunDate.getTime());
					lastInvoiceDate.add(Calendar.MONTH, -1);

					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Generating invoice for 1st March 2016
					scenarioBuilder.selectUserByName(SCENARIO_09_USER)
						.createOrderWithPrice(SCENARIO_09_ADJUSTMENT_ORDER, lastInvoiceDate.getTime(),null, ONE_TIME_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), BigDecimal.ONE), 
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), new BigDecimal("104.98").negate()))
						.generateInvoice(billingRunDate.getTime(), true);

					// Validating generated invoice summary data
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_09_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #9 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
					
					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																.addExpectedAdjustmentCharges(new BigDecimal("104.98").negate())
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																.addExpectedMonthlyCharges(new BigDecimal("104.98"))
																.addExpectedNewCharges(new BigDecimal("0.00"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("148.00").negate())
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("148.00").negate())
																.validate();
					
				}).validate((testEnv, envBuilder) -> {
					Calendar billingRunDate = Calendar.getInstance();
					billingRunDate.set(Calendar.YEAR, 2016);
					billingRunDate.set(Calendar.MONTH, 2);
					billingRunDate.set(Calendar.DAY_OF_MONTH, 1);
					
					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 1);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Generating invoice for 1st March 2016
					scenarioBuilder
						.selectUserByName(SCENARIO_09_USER)
						.createOrderWithPrice(SCENARIO_09_ADJUSTMENT_ORDER, activeSince.getTime(),null, ONE_TIME_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), BigDecimal.ONE), 
			 				   Collections.singletonMap(environment.idForCode(adjustMentProduct), new BigDecimal("99.99").negate()))
						.generateInvoice(billingRunDate.getTime(), true);

					// Validating generated invoice summary data
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_09_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #9 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
					
					Calendar lastInvoiceDate = Calendar.getInstance();
					lastInvoiceDate.setTime(billingRunDate.getTime());
					lastInvoiceDate.add(Calendar.MONTH, -1);
					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																.addExpectedAdjustmentCharges(new BigDecimal("99.99").negate())
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																.addExpectedMonthlyCharges(new BigDecimal("104.98"))
																.addExpectedNewCharges(new BigDecimal("4.99"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("143.01").negate())
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("148.00").negate())
																.validate();
					
				}).validate((testEnv, envBuilder) -> {
					
					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 2);
					paymentDate.set(Calendar.DAY_OF_MONTH, 2);
					
					Calendar refundDate = Calendar.getInstance();
					refundDate.set(Calendar.YEAR, 2016);
					refundDate.set(Calendar.MONTH, 2);
					refundDate.set(Calendar.DAY_OF_MONTH, 8);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					Date lastInvoiceDate = new DateMidnight(2016, 2, 1).toDate();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_09_USER));
					PaymentWS payment = api.getLatestPayment(invoice.getUserId());
					api.removePaymentLink(invoice.getId(), payment.getId());
					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					scenarioBuilder.selectUserByName(SCENARIO_09_USER)
								   //Make refund payment on 8th March 2016
								   .makePayment("148.00", refundDate.getTime(), true)
								   //Make payment on 2th March 2016
								   .makePayment("4.99", paymentDate.getTime(), false);
					

					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #9 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());

					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																.addExpectedAdjustmentCharges(new BigDecimal("99.99").negate())
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(lastInvoiceDate)
																.addExpectedMonthlyCharges(new BigDecimal("104.98"))
																.addExpectedNewCharges(new BigDecimal("4.99"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("143.01").negate())
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("148.00").negate())
																.validate();
					
				});

			logger.debug("Invoice template test scenario #09 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_09_USER), 10, 0))
			   	  .forEach(invoice -> {
			              api.deleteInvoice(invoice.getId());
			   	  });
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #10.
	 * Users with only one plan and one asset
	 */
	@Test
	public void testInvoiceSummaryScenario10() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #10 - Users with only one plan and one asset.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2015);
				nextInvoiceDate.set(Calendar.MONTH, 11);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);


				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2015);
				activeSince.set(Calendar.MONTH, 10);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				AssetWS scenario10Asset = getAssetIdByProductId(api,TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID);

				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID, scenario10Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(subScriptionProd10), BigDecimal.ONE);

				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario10Asset.getIdentifier()), "257", "11/10/2015");
				InvoiceSummaryScenarioBuilder scenario10 = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenario10.createUser(SCENARIO_10_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
				.createOrder("testSubScriptionOrder10", activeSince.getTime(), null, MONTHLY_ORDER_PERIOD,
						postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,productQuantityMap, productAssetMap, false)
						.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2015);
				runDate.set(Calendar.MONTH, 11);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_10_USER)
				// generating invoice for 1'st Dec 2016
				.generateInvoice(runDate.getTime(),true);

				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_10_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #10 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(null)
																.addExpectedMonthlyCharges(new BigDecimal("209.98"))
																.addExpectedNewCharges(new BigDecimal("216.63"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("216.63"))
																.addExpectedUsageCharges(new BigDecimal("6.65"))
																.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																.validate();

				}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 00);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2015);
				paymentDate.set(Calendar.MONTH,11);
				paymentDate.set(Calendar.DAY_OF_MONTH, 20);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_10_USER)
				// generating invoice for 1'st jan 2016
				.makePayment("206.17", paymentDate.getTime(), false)
				.generateInvoice(runDate.getTime(),true);
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_10_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #10 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("206.17").negate())
																.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																.addExpectedFeesCharges(new BigDecimal("0.16"))
																.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																.addExpectedMonthlyCharges(new BigDecimal("209.98"))
																.addExpectedNewCharges(new BigDecimal("210.14"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("220.60"))
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("216.63"))
																.validate();
			  });
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(SCENARIO_08_USER);
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
			.forEach(invoice -> {
				api.deleteInvoice(invoice.getId());
			});
		}

	}

	 /**
	 * Invoice Template Scenarios Sheet #11.
	 * User with only one plan and multiple assets
	 */
	@Test
	public void testInvoiceSummaryScenario11() {

		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #11 - User with only one plan and multiple assets.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 5);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 4);
					paymentDate.set(Calendar.DAY_OF_MONTH, 20);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 4);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();

					Map<Integer, Integer> productAssetMap1 = new HashMap<>();
					AssetWS[] scenarioAssets = getAssetIdsByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
					productAssetMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenarioAssets[0].getId());
					Map<Integer, Integer> productAssetMap2 = new HashMap<>();
					productAssetMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenarioAssets[1].getId());
					Map<Integer, Integer> productAssetMap3 = new HashMap<>();
					productAssetMap3.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenarioAssets[2].getId());

					Map<Integer, BigDecimal> productQuantityMap1 = new HashMap<>();
					productQuantityMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap1.put(environment.idForCode(SCENARIO_11_PLAN_PRODUCT), BigDecimal.ONE);
					Map<Integer, BigDecimal> productQuantityMap2 = new HashMap<>();
					productQuantityMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					Map<Integer, BigDecimal> productQuantityMap3 = new HashMap<>();
					productQuantityMap3.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);

					List<String> inboundCdrs = buildInboundCDR(Arrays.asList(new String[]{scenarioAssets[0].getIdentifier(), scenarioAssets[1].getIdentifier(), scenarioAssets[2].getIdentifier()}), "376", "05/05/2016");

					InvoiceSummaryScenarioBuilder scenario11 = new InvoiceSummaryScenarioBuilder(testBuilder);
					//create User with NID- 07-01-2016 And subscription order.
					scenario11.createUser(SCENARIO_11_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
			 		   .createOrder(SCENARIO_11_ORDER, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap1, productAssetMap1, false)
			 			.createOrder(SCENARIO_11_ORDER, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap2, productAssetMap2, false)
			 			.createOrder(SCENARIO_11_ORDER, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap3, productAssetMap3, false)
			 			.makePayment("414.96", paymentDate.getTime(), false)
			 			//Creating a credit payment of $191.36 on date 05/22/2016
			 			.makeCreditPayment("191.36", new Date(paymentDate.getTime().getTime()+(1000*60*60*24*2)))
			 			//Creating a credit payment of $191.49 on date 05/29/2016
			 			.makeCreditPayment("191.49", new Date(paymentDate.getTime().getTime()+ (1000*60*60*24*7)))
			 			.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs)
			 			.generateInvoice(nextInvoiceDate.getTime(),true);;

				}).validate((testEnv, envBuilder) -> {

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_11_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #11 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-414.96"))
																.addExpectedAdjustmentCharges(new BigDecimal("-382.85"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(null)
																.addExpectedMonthlyCharges(new BigDecimal("414.96"))
																.addExpectedNewCharges(new BigDecimal("414.96"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("0.00"))
																.addExpectedUsageCharges(new BigDecimal("382.85"))
																.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																.validate();
				}).validate((testEnv, envBuilder) -> {

					//Invoce creation date
					Calendar runDate = Calendar.getInstance();
					runDate.set(Calendar.YEAR, 2016);
					runDate.set(Calendar.MONTH, 6);
					runDate.set(Calendar.DAY_OF_MONTH, 1);
					//PaymentDate
					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 5);
					paymentDate.set(Calendar.DAY_OF_MONTH, 10);

					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					scenarioBuilder.selectUserByName(SCENARIO_11_USER)
					.makePayment("414.96", paymentDate.getTime(), false)
					.generateInvoice(runDate.getTime(),true);

					Calendar lastInvoiceDate = Calendar.getInstance();
					lastInvoiceDate.setTime(runDate.getTime());
					lastInvoiceDate.add(Calendar.MONTH, -1);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_11_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #11 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-414.96"))
																.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																.addExpectedMonthlyCharges(new BigDecimal("414.96"))
																.addExpectedNewCharges(new BigDecimal("414.96"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("0.00"))
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																.validate();
				});
				logger.debug("Invoice template test scenario #11 has been passed successfully");
		}finally{
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_11_USER), 10, 0))
				.forEach(invoice -> {
					api.deleteInvoice(invoice.getId());
				});
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #12.
	 * Invoices with Fees
	 */
	@Test
	public void testInvoiceSummaryScenario12() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #12 - Invoices with Fees.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 2);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 1);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					// Data set-up to create item/plan and orders
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					Calendar pricingDate = Calendar.getInstance();
					pricingDate.set(Calendar.YEAR, 2016);
					pricingDate.set(Calendar.MONTH, 1);
					pricingDate.set(Calendar.DAY_OF_MONTH, 1);

					List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
					PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.29", pricingDate.getTime());
					PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.29", pricingDate.getTime());
					PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.29", pricingDate.getTime());

					buildAndPersistUsagePool(envBuilder, api, SCENARIO_12_USAGE_POOL, "135", envBuilder.idForCode(testCat1), items);
					buildAndPersistFlatProduct(envBuilder, api, SCENARIO_12_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "149.99", true);
					buildAndPersistPlan(envBuilder,api, SCENARIO_12_PLAN, "135 Min Plan - $149.99 / Month", MONTHLY_ORDER_PERIOD, 
										envBuilder.idForCode(SCENARIO_12_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_12_USAGE_POOL)), 
										planItemProd1WS, planItemProd2WS, planItemProd3WS);

					AssetWS scenario12Asset = getAssetIdByProductId(api,TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID);
					Map<Integer, Integer> productAssetMap = new HashMap<>();
					productAssetMap.put(TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID, scenario12Asset.getId());

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap.put(environment.idForCode(SCENARIO_12_PLAN_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario12 = new InvoiceSummaryScenarioBuilder(testBuilder);
					//Create customer with NID as 1st of Mar 2016
					scenario12.createUser(SCENARIO_12_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							  //Create monthly subscription order on 1st Feb 2016
							  .createOrder(SCENARIO_12_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
											productQuantityMap, productAssetMap, false);

					//Validating invoice summary data generated on 01-Mar-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 2);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Mar 2016
						scenarioBuilder.selectUserByName(SCENARIO_12_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_12_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #12 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("159.98"))
																	.addExpectedNewCharges(new BigDecimal("159.98"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("159.98"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();

						Calendar activeUntil = Calendar.getInstance();
						activeUntil.set(Calendar.YEAR, 2016);
						activeUntil.set(Calendar.MONTH, 2);
						activeUntil.set(Calendar.DAY_OF_MONTH, 14);

						OrderWS orderWS = api.getOrder(environment.idForCode(SCENARIO_12_ORDER));
						orderWS.setActiveUntil(activeUntil.getTime());
						api.updateOrder(orderWS, null);


					//Validating invoice summary data generated on 01-Apr-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 3);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Apr 2016
						scenarioBuilder.selectUserByName(SCENARIO_12_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_12_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #12 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("2.40"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("72.25"))
																	.addExpectedNewCharges(new BigDecimal("74.65"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("234.63"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("159.98"))
																	.validate();

					//Validating invoice summary data generated on 01-May-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 4);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st May 2016
						scenarioBuilder.selectUserByName(SCENARIO_12_USER)
									   .generateInvoice(billingRunDate.getTime(), false);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_12_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #12 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("3.52"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("0.00"))
																	.addExpectedNewCharges(new BigDecimal("3.52"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("238.15"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("234.63"))
																	.validate();

					//Validating invoice summary data generated on 01-Aug-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 7);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						//Updating User's NID to 1st Aug 2016
						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						UserWS userWS = api.getUserWS(envBuilder.idForCode(SCENARIO_12_USER));
						userWS.setNextInvoiceDate(billingRunDate.getTime());
						api.updateUser(userWS);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Aug 2016
						scenarioBuilder.selectUserByName(SCENARIO_12_USER)
									   .generateInvoice(billingRunDate.getTime(), false);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -3);

						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_12_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #12 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("3.57"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("0.00"))
																	.addExpectedNewCharges(new BigDecimal("3.57"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("241.72"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("238.15"))
																	.validate();
					});
					logger.debug("Invoice template test scenario #12 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(SCENARIO_12_USER);
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
					.forEach(invoice -> {
						api.deleteInvoice(invoice.getId());
					});
		}
	}

	 /**
	 * Invoice Template Scenarios Sheet #13.
	 * Invoices with Adjustments
	 */
	@Test
	public void testInvoiceSummaryScenario013() {

		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #13 - Invoices with Adjustments.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 10);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 10);
					paymentDate.set(Calendar.DAY_OF_MONTH, 12);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 9);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();

					Map<Integer, Integer> productAssetMap1 = new HashMap<>();
					AssetWS scenarioAssets = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
					productAssetMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenarioAssets.getId());

					Map<Integer, BigDecimal> productQuantityMap1 = new HashMap<>();
					productQuantityMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap1.put(environment.idForCode(SCENARIO_13_PLAN_PRODUCT), BigDecimal.ONE);

					List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenarioAssets.getIdentifier()),"100","10/05/2016");

					InvoiceSummaryScenarioBuilder scenario13 = new InvoiceSummaryScenarioBuilder(testBuilder);
					//create User with NID- 11-01-2016 And subscription order.
					scenario13.createUser(SCENARIO_13_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
			 		   .createOrder(SCENARIO_13_ORDER, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
			 				   productQuantityMap1, productAssetMap1, false)
			 		    .generateInvoice(nextInvoiceDate.getTime(),true)
			 			.makePayment("154.98", paymentDate.getTime(), false)
			 			.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

				}).validate((testEnv, envBuilder) -> {

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_13_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #13 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(null)
																.addExpectedMonthlyCharges(new BigDecimal("154.98"))
																.addExpectedNewCharges(new BigDecimal("154.98"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("154.98"))
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																.validate();
					
					}).validate((testEnv, envBuilder) -> {

					//Invoce creation date
					Calendar runDate = Calendar.getInstance();
					runDate.set(Calendar.YEAR, 2016);
					runDate.set(Calendar.MONTH, 11);
					runDate.set(Calendar.DAY_OF_MONTH, 1);
					
					Calendar lastInvoiceDate = Calendar.getInstance();
					lastInvoiceDate.setTime(runDate.getTime());
					lastInvoiceDate.add(Calendar.MONTH, -1);

					Calendar activeUntil = Calendar.getInstance();
					activeUntil.set(Calendar.YEAR, 2016);
					activeUntil.set(Calendar.MONTH, 11);
					activeUntil.set(Calendar.DAY_OF_MONTH, 23);

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);

					scenarioBuilder.selectUserByName(SCENARIO_13_USER)
					.createOrder(SCENARIO_13_ORDER_ADJUSTMENT, lastInvoiceDate.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
							Collections.singletonMap(environment.idForCode(SCENARIO_13_ADJUSTMENT_PRODUCT),  BigDecimal.ONE), null, false)
					.generateInvoice(runDate.getTime(),true);

					OrderWS orderWS = api.getOrder(environment.idForCode(SCENARIO_13_ORDER));
					orderWS.setActiveUntil(activeUntil.getTime());
					api.updateOrder(orderWS, null);
					
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_13_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #13 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-154.98"))
																.addExpectedAdjustmentCharges(new BigDecimal("379.37"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																.addExpectedMonthlyCharges(new BigDecimal("154.98"))
																.addExpectedNewCharges(new BigDecimal("534.35"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("534.35"))
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("154.98"))
																.validate();
					
					
				}).validate((testEnv, envBuilder) -> {

					//Invoce creation date
					Calendar runDate = Calendar.getInstance();
					runDate.set(Calendar.YEAR, 2016);
					runDate.set(Calendar.MONTH, 11);
					runDate.set(Calendar.DAY_OF_MONTH, 1);

					//PaymentDate
					Calendar paymentDate = Calendar.getInstance();
					paymentDate.set(Calendar.YEAR, 2016);
					paymentDate.set(Calendar.MONTH, 11);
					paymentDate.set(Calendar.DAY_OF_MONTH, 28);

					Calendar lastInvoiceDate = Calendar.getInstance();
					lastInvoiceDate.setTime(runDate.getTime());
					lastInvoiceDate.add(Calendar.MONTH, -1);

					InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
					scenarioBuilder.selectUserByName(SCENARIO_13_USER)
					.makeCreditPayment("534.35", paymentDate.getTime());

					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_13_USER));
					ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
					logger.debug("Scenario #13 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

					new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-154.98"))
																.addExpectedAdjustmentCharges(new BigDecimal("379.37"))
																.addExpectedFeesCharges(new BigDecimal("0.00"))
																.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																.addExpectedMonthlyCharges(new BigDecimal("154.98"))
																.addExpectedNewCharges(new BigDecimal("534.35"))
																.addExpectedTaxesCharges(new BigDecimal("0.00"))
																.addExpectedTotalDue(new BigDecimal("534.35"))
																.addExpectedUsageCharges(new BigDecimal("0.00"))
																.addExpectedAmountOfLastStatement(new BigDecimal("154.98"))
																.validate();
			
				});
				logger.debug("Invoice template test scenario #13 has been passed successfully");
		}finally{
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_13_USER), 10, 0))
				.forEach(invoice -> {
					api.deleteInvoice(invoice.getId());
				});
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #14
	 * Invoices with Taxes.
	 */
	@Test
	public void testInvoiceSummaryScenario14() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #14 - Invoices with Taxes.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 9);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 8);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
			
				// Creating VAT product
				buildAndPersistLinePercentageProduct(envBuilder, api, vatProduct, false, envBuilder.idForCode(taxCategory), "20", false);
				
				// Configuring Simple Tax Composition Task
				Hashtable<String, String> parameters = new Hashtable<>();
				
				parameters.put("charge_carrying_item_id", envBuilder.idForCode(vatProduct).toString());
				PluggableTaskWS plugins [] = api.getPluginsWS(api.getCallerCompanyId(), SIMPLE_TAX_COMPOSITION_TASK_CLASS_NAME);
				if(null == plugins || plugins.length == 0) {
					buildAndPersistPlugIn(envBuilder, api, SIMPLE_TAX_COMPOSITION_TASK_CLASS_NAME, parameters);
				} else {
					updatePlugin(plugins[0].getId(), SIMPLE_TAX_COMPOSITION_TASK_CLASS_NAME, testBuilder, parameters);
				}
				
				AssetWS scenario14Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
				Map<Integer, Integer> productAssetMap = new HashMap<>();
				productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario14Asset.getId());

				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap.put(environment.idForCode(SCENARIO_14_PLAN_PRODUCT), BigDecimal.ONE);
				
				List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario14Asset.getIdentifier()), "158", "09/04/2016");
				
				InvoiceSummaryScenarioBuilder scenario14 = new InvoiceSummaryScenarioBuilder(testBuilder);
				//create User with NID- 1'st Oct 2016 And subscription order.
				scenario14.createUser(SCENARIO_14_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
		 		   .createOrder(SCENARIO_14_ORDER, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
		 				   productQuantityMap, productAssetMap, false)
		 		   .triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
			
			//Validating invoice summary data generated on 01-Oct-2016	
			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 9);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_14_USER)
				// generating invoice for 1'st Oct 2016
				.generateInvoice(runDate.getTime(),true);

				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_14_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #14 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("103.99"))
																	.addExpectedNewCharges(new BigDecimal("124.79"))
																	.addExpectedTaxesCharges(new BigDecimal("20.80"))
																	.addExpectedTotalDue(new BigDecimal("124.79"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();
			
			//Validating invoice summary data generated on 01-Nov-2016
			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 10);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH,9);
				paymentDate.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_14_USER)
				.makePayment("124.79", paymentDate.getTime(), false)
				// generating invoice for 1'st Nov 2016
				.generateInvoice(runDate.getTime(),true);
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_14_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #14 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("124.79").negate())
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("103.99"))
																	.addExpectedNewCharges(new BigDecimal("124.79"))
																	.addExpectedTaxesCharges(new BigDecimal("20.80"))
																	.addExpectedTotalDue(new BigDecimal("124.79"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("124.79"))
																	.validate();

			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 11);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH,10);
				paymentDate.set(Calendar.DAY_OF_MONTH, 1);

				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_14_USER)
				.makePayment("124.79", paymentDate.getTime(), false)
				// generating invoice for 1'st Nov 2016
				.generateInvoice(runDate.getTime(),true);
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_14_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #14 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("124.79").negate())
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("103.99"))
																	.addExpectedNewCharges(new BigDecimal("124.79"))
																	.addExpectedTaxesCharges(new BigDecimal("20.80"))
																	.addExpectedTotalDue(new BigDecimal("124.79"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("124.79"))
																	.validate();

			});
			logger.debug("Invoice template test scenario #14 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_14_USER), 10, 0))
			   	  .forEach(invoice -> {
			   		  api.deleteInvoice(invoice.getId());
			   	  });
			api.deletePlugin(api.getPluginsWS(api.getCallerCompanyId(), SIMPLE_TAX_COMPOSITION_TASK_CLASS_NAME)[0].getId());
		}
	}
	
	 /**
	 * Invoice Template Scenarios Sheet #15.
	 * Invoices with more carried lines and no payment
	 */
	@Test
	public void testInvoiceSummaryScenario15() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #15 - Invoices with more carried lines and no payment.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 7);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 6);
					activeSince.set(Calendar.DAY_OF_MONTH, 1);

					// Data set-up to create item/plan and orders
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					Calendar pricingDate = Calendar.getInstance();
					pricingDate.set(Calendar.YEAR, 2016);
					pricingDate.set(Calendar.MONTH, 6);
					pricingDate.set(Calendar.DAY_OF_MONTH, 1);

					List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
					PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "2.50", pricingDate.getTime());
					PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "2.50", pricingDate.getTime());
					PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "2.50", pricingDate.getTime());

					buildAndPersistUsagePool(envBuilder, api, SCENARIO_15_ZERO_USAGE_POOL, "1", envBuilder.idForCode(testCat1), items);
					buildAndPersistFlatProduct(envBuilder, api, SCENARIO_15_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "9.99", true);
					buildAndPersistPlan(envBuilder,api, SCENARIO_15_PLAN, "DORMANCY $9.99 / Month", MONTHLY_ORDER_PERIOD, 
										envBuilder.idForCode(SCENARIO_15_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_15_ZERO_USAGE_POOL)), 
										planItemProd1WS, planItemProd2WS, planItemProd3WS);

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(environment.idForCode(SCENARIO_15_PLAN_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario08 = new InvoiceSummaryScenarioBuilder(testBuilder);
					//Create customer with NID as 1st of Aug 2016
					scenario08.createUser(SCENARIO_15_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							  //Create monthly subscription order on 1st Jul 2016
							  .createOrder(SCENARIO_15_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
											productQuantityMap, null, false);

					//Validating first invoice generated on 01-Aug-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 7);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Aug 2016
						scenarioBuilder.selectUserByName(SCENARIO_15_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_15_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #15 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("9.99"))
																	.addExpectedNewCharges(new BigDecimal("9.99"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("9.99"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();

					//Validating second invoice generated on 01-Sep-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 8);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Sep 2016
						scenarioBuilder.selectUserByName(SCENARIO_15_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_15_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #15 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.15"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("9.99"))
																	.addExpectedNewCharges(new BigDecimal("10.14"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("20.13"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("9.99"))
																	.validate();

					//Validating third invoice generated on 01-Oct-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 9);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Oct 2016
						scenarioBuilder.selectUserByName(SCENARIO_15_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_15_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #15 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.30"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("9.99"))
																	.addExpectedNewCharges(new BigDecimal("10.29"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("30.42"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("20.13"))
																	.validate();

					//Validating fourth invoice generated on 01-Nov-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 10);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Nov 2016
						scenarioBuilder.selectUserByName(SCENARIO_15_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_15_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #15 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.46"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("9.99"))
																	.addExpectedNewCharges(new BigDecimal("10.45"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("40.87"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("30.42"))
																	.validate();

					//Validating fourth invoice generated on 01-Dec-2016
					}).validate((testEnv, envBuilder) -> {
						Calendar billingRunDate = Calendar.getInstance();
						billingRunDate.set(Calendar.YEAR, 2016);
						billingRunDate.set(Calendar.MONTH, 11);
						billingRunDate.set(Calendar.DAY_OF_MONTH, 1);

						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Dec 2016
						scenarioBuilder.selectUserByName(SCENARIO_15_USER)
									   .generateInvoice(billingRunDate.getTime(), true);

						Calendar lastInvoiceDate = Calendar.getInstance();
						lastInvoiceDate.setTime(billingRunDate.getTime());
						lastInvoiceDate.add(Calendar.MONTH, -1);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_15_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #15 Invoice Summary after 5th Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.61"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
																	.addExpectedMonthlyCharges(new BigDecimal("9.99"))
																	.addExpectedNewCharges(new BigDecimal("10.60"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("51.47"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("40.87"))
																	.validate();
					});

					logger.debug("Invoice template scenario #15 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(SCENARIO_15_USER);
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
					.forEach(invoice -> {
						api.deleteInvoice(invoice.getId());
					});
		}
	}

	 /**
	 * Invoice Template Scenarios Sheet #16
	 * Invoices with credit payment.
	 */
	@Test
	public void testInvoiceSummaryScenario16() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #16 - Invoices with credit payment.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 4);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 3);
					activeSince.set(Calendar.DAY_OF_MONTH, 12);

					// Data set-up to create item/plan and orders
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					Calendar pricingDate = Calendar.getInstance();
					pricingDate.set(Calendar.YEAR, 2016);
					pricingDate.set(Calendar.MONTH, 1);
					pricingDate.set(Calendar.DAY_OF_MONTH, 1);

					List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
					PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.99", pricingDate.getTime());
					PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.99", pricingDate.getTime());
					PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.99", pricingDate.getTime());

					buildAndPersistUsagePool(envBuilder, api, SCENARIO_16_USAGE_POOL, "450", envBuilder.idForCode(testCat1), items);
					buildAndPersistFlatProduct(envBuilder, api, SCENARIO_16_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "399.99", true);
					buildAndPersistPlan(envBuilder,api, SCENARIO_16_PLAN, "450 Min Plan - $399.99 / Month", MONTHLY_ORDER_PERIOD, 
										envBuilder.idForCode(SCENARIO_16_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_16_USAGE_POOL)), 
										planItemProd1WS, planItemProd2WS, planItemProd3WS);

					AssetWS scenario16Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
					Map<Integer, Integer> productAssetMap = new HashMap<>();
					productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario16Asset.getId());

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap.put(environment.idForCode(SCENARIO_16_PLAN_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario16 = new InvoiceSummaryScenarioBuilder(testBuilder);
					// Set NID as 1st of May 2016
					scenario16.createUser(SCENARIO_16_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							//Creating monthly subscription order on 12th Apr 2016
							.createOrder(SCENARIO_16_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
										productQuantityMap, productAssetMap, false)
							//Creating one time order for Set Up Fee on 12th Apr 2016
							.createOrder(SCENARIO_16_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
										Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false)
							//Making signup payment on 12th Apr 2016
							.makePayment("449.98", activeSince.getTime(), false);

					//Generate invoice and validate invoice summary data.
					}).validate((testEnv, envBuilder) -> {
						Date runDate = new DateMidnight(2016, 5, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st May 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_16_USER)
							.generateInvoice(runDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_16_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #16 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-449.98"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("49.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("256.49"))
																	.addExpectedNewCharges(new BigDecimal("305.49"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-144.49"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();

					//Creating credit order with invoice line to adjust the invoice.
					}).validate((testEnv, envBuilder) -> {
						Calendar activeSince = Calendar.getInstance();
						activeSince.set(Calendar.YEAR, 2016);
						activeSince.set(Calendar.MONTH, 4);
						activeSince.set(Calendar.DAY_OF_MONTH, 1);

						Date lastInvoiceDate = new DateMidnight(2016, 5, 1).toDate();
						Date billingRunDate = new DateMidnight(2016, 6, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						// Generating invoice for 1st Jun 2016
						scenarioBuilder
							.selectUserByName(SCENARIO_16_USER)
							.createOrderWithPrice(SCENARIO_16_SALES_CREDIT_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId,
								ORDER_CHANGE_STATUS_APPLY_ID, false, Collections.singletonMap(environment.idForCode(adjustMentProduct), BigDecimal.ONE),
								Collections.singletonMap(environment.idForCode(adjustMentProduct), new BigDecimal("-404.98")))
							.generateInvoice(billingRunDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_16_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #16 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("-404.98"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate)
																	.addExpectedMonthlyCharges(new BigDecimal("404.98"))
																	.addExpectedNewCharges(new BigDecimal("0.00"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-144.49"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("-144.49"))
																	.validate();

					//Creating credit payment for adjustment in invoice
					}).validate((testEnv, envBuilder) -> {
						Calendar paymentDate = Calendar.getInstance();
						paymentDate.set(Calendar.YEAR, 2016);
						paymentDate.set(Calendar.MONTH, 5);
						paymentDate.set(Calendar.DAY_OF_MONTH, 15);

						Date lastInvoiceDate = new DateMidnight(2016, 6, 1).toDate();
						Date billingRunDate = new DateMidnight(2016, 7, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Jul 2016 after creating credit payment
						scenarioBuilder
							.selectUserByName(SCENARIO_16_USER)
							.makeCreditPayment("350", paymentDate.getTime())
							.generateInvoice(billingRunDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_16_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #16 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("-350.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(lastInvoiceDate)
																	.addExpectedMonthlyCharges(new BigDecimal("404.98"))
																	.addExpectedNewCharges(new BigDecimal("54.98"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("-89.51"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("-144.49"))
																	.validate();
					});
				logger.debug("Invoice template test scenario #16 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_16_USER), 10, 0))
				.forEach(invoice -> {
					api.deleteInvoice(invoice.getId());
				});
		}
	}
	
	/**
	 * Invoice Template Scenarios Sheet #17
	 * Credit Payment issue.
	 */
	@Test
	public void testInvoiceSummaryScenario17() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #17 - Credit Payment issue.");
				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 8);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 7);
				activeSince.set(Calendar.DAY_OF_MONTH, 19);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				
				InvoiceSummaryScenarioBuilder scenario17 = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenario17.createUser(SCENARIO_17_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
		 		.createOrder(SCENARIO_17_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
						Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false);
				
			//Validating invoice summary data generated on 01-Sept-2016	
			}).validate((testEnv, envBuilder) -> {

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH,7);
				paymentDate.set(Calendar.DAY_OF_MONTH, 23);
				
				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 8);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_17_USER)
				.makePayment("249.98", paymentDate.getTime(), false)
				//Make credit payment after invoice generation
				.makeCreditPayment("199.99", new Date(paymentDate.getTime().getTime()+ (1000*60*60*24*3)))
				.makeCreditPayment("50.00", paymentDate.getTime())
				.makeCreditPayment("50.00", paymentDate.getTime())
				.makeCreditPayment("50.00", paymentDate.getTime())
				// generating invoice for 1'st Oct 2016
				.generateInvoice(runDate.getTime(),false);

				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_17_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #17 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("249.98").negate())
															.addExpectedAdjustmentCharges(new BigDecimal("349.99").negate())
															.addExpectedFeesCharges(new BigDecimal("49.00"))
															.addExpectedLastInvoiceDate(null)
															.addExpectedMonthlyCharges(new BigDecimal("0.00"))
															.addExpectedNewCharges(new BigDecimal("300.99").negate())
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("550.97").negate())
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
															.validate();
			
			//Validating invoice summary data generated on 01-Sept-2016 - Second Invoice on same date
			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 8);
				runDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 7);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);
				
				Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
				productQuantityMap.put(INBOUND_USAGE_PRODUCT_ID, new BigDecimal("0.50"));
				productQuantityMap.put(CHAT_USAGE_PRODUCT_ID, new BigDecimal("1.10"));
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_17_USER)
				.createOrder(SCENARIO_17_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false, 
						productQuantityMap, null, false)
				.generateInvoice(runDate.getTime(),false);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_17_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #17 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00").negate())
															.addExpectedFeesCharges(new BigDecimal("0.00"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("0.00"))
															.addExpectedNewCharges(new BigDecimal("4.00"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("546.97").negate())
															.addExpectedUsageCharges(new BigDecimal("4.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("550.97").negate())
															.validate();
			}).validate((testEnv, envBuilder) -> {

				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 9);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
				
				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 8);
				activeSince.set(Calendar.DAY_OF_MONTH, 2);
				
				Calendar activeSince1 = Calendar.getInstance();
				activeSince1.set(Calendar.YEAR, 2016);
				activeSince1.set(Calendar.MONTH, 8);
				activeSince1.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				
				AssetWS[] scenario17Assets = getAssetIdsByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
				AssetWS[] scenario17LocalAssets = getAssetIdsByProductId(api,LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID);
				Map<Integer, Integer> productAssetMap1 = new HashMap<>();
				productAssetMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario17Assets[0].getId());
				Map<Integer, Integer> productAssetMap2 = new HashMap<>();
				productAssetMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario17Assets[1].getId());
				Map<Integer, Integer> productAssetMap3 = new HashMap<>();
				productAssetMap3.put(LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID, scenario17LocalAssets[0].getId());

				Map<Integer, BigDecimal> productQuantityMap1 = new HashMap<>();
				productQuantityMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap1.put(environment.idForCode(SCENARIO_17_PLAN_PRODUCT1), BigDecimal.ONE);
				
				Map<Integer, BigDecimal> productQuantityMap2 = new HashMap<>();
				productQuantityMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap2.put(environment.idForCode(SCENARIO_17_PLAN_PRODUCT2), BigDecimal.ONE);
				
				Map<Integer, BigDecimal> productQuantityMap3 = new HashMap<>();
				productQuantityMap3.put(LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
				productQuantityMap3.put(environment.idForCode(subScriptionProd17), BigDecimal.ONE);
				
				List<String> inboundCdrs1 = buildInboundCDR(Arrays.asList(scenario17Assets[0].getIdentifier()), "361", "09/04/2016");
				List<String> inboundCdrs2 = buildInboundCDR(Arrays.asList(scenario17Assets[1].getIdentifier()), "14", "09/06/2016");
				
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_17_USER)
				.createOrder(SCENARIO_17_PLAN_1_ORDER, activeSince1.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
		 				   productQuantityMap1, productAssetMap1, false)
		 		.createOrder(SCENARIO_17_PLAN_2_ORDER, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
		 				   productQuantityMap2, productAssetMap2, false)
		 		.createOrder(SCENARIO_17_PLAN_3_ORDER, activeSince1.getTime(),null, MONTHLY_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
		 				   productQuantityMap3, productAssetMap3, false)
		 		.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs1)
		 		.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs2);
				
			//Validating invoice summary data generated on 01-Oct-2016	
			}).validate((testEnv, envBuilder) -> {

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH,8);
				paymentDate.set(Calendar.DAY_OF_MONTH, 23);
				
				Date runDate = new DateMidnight(2016, 10, 1).toDate();
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_17_USER)
				.makeCreditPayment("200.00", new Date(paymentDate.getTime().getTime()+ (1000*60*60*24*7)))
				.makeCreditPayment("199.99", paymentDate.getTime())
				.makeCreditPayment("199.99", paymentDate.getTime())
				.makeCreditPayment("50.11", paymentDate.getTime())
				// generating invoice for 1'st Oct 2016
				.generateInvoice(runDate,true);

				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_17_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #17 Invoice Summary after 3rd Invoice: {}", itemizedAccountWS.getInvoiceSummary());
				
				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate);
				lastInvoiceDate.add(Calendar.MONTH, -1);

				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("650.09").negate())
															.addExpectedFeesCharges(new BigDecimal("0.00"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("456.44"))
															.addExpectedNewCharges(new BigDecimal("193.65").negate())
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("740.62").negate())
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("546.97").negate())
															.validate();
			
			//Validating invoice summary data generated on 01-Nov-2016
			}).validate((testEnv, envBuilder) -> {

				Calendar runDate = Calendar.getInstance();
				runDate.set(Calendar.YEAR, 2016);
				runDate.set(Calendar.MONTH, 10);
				runDate.set(Calendar.DAY_OF_MONTH, 1);
				
				final JbillingAPI api = envBuilder.getPrancingPonyApi();
				
				logger.debug("Swap Plan 450 MIN Plan with 225 MIN Plan");
		        OrderWS order = api.getOrder(envBuilder.idForCode(SCENARIO_17_PLAN_2_ORDER));
				assertNotNull("getOrder should not be null", order);
				
				OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, 
						api.getPlanWS(environment.idForCode(SCENARIO_17_PLAN2)).getItemId(), 
						api.getPlanWS(environment.idForCode(plan10)).getItemId(), 
						SwapMethod.DIFF, 
						Util.truncateDate(order.getActiveSince()));
				
		        assertNotNull("Swap changes should be calculated", orderChanges);
		        api.createUpdateOrder(order, orderChanges);

				InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
				scenarioBuilder.selectUserByName(SCENARIO_17_USER)
				.generateInvoice(runDate.getTime(),true);
				
				InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_17_USER));
				ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
				logger.debug("Scenario #17 Invoice Summary after 4th Invoice: {}", itemizedAccountWS.getInvoiceSummary());

				Calendar lastInvoiceDate = Calendar.getInstance();
				lastInvoiceDate.setTime(runDate.getTime());
				lastInvoiceDate.add(Calendar.MONTH, -1);
				new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
															.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
															.addExpectedFeesCharges(new BigDecimal("0.00"))
															.addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
															.addExpectedMonthlyCharges(new BigDecimal("269.94"))
															.addExpectedNewCharges(new BigDecimal("269.94"))
															.addExpectedTaxesCharges(new BigDecimal("0.00"))
															.addExpectedTotalDue(new BigDecimal("470.68").negate())
															.addExpectedUsageCharges(new BigDecimal("0.00"))
															.addExpectedAmountOfLastStatement(new BigDecimal("740.62").negate())
															.validate();
			});
			logger.debug("Invoice template test scenario #17 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_17_USER), 10, 0))
			   	  .forEach(invoice -> {
			   		  api.deleteInvoice(invoice.getId());
			   	  });
		}
	}
	
	/**
	 * Invoice Template Scenarios Sheet #18.
	 * Apply order to invoice. Generate an invoice, validate invoice summary data.
	 * Then apply order to that invoice and again validate the invoice.
	 * Make sure the additional charges reflect appropriately onto the invoice.
	 */
	@Test
	public void testInvoiceSummaryScenario18() {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
				testBuilder.given(envBuilder -> {
					logger.debug("Scenario #18 - Apply order to Invoice.");
					Calendar nextInvoiceDate = Calendar.getInstance();
					nextInvoiceDate.set(Calendar.YEAR, 2016);
					nextInvoiceDate.set(Calendar.MONTH, 2);
					nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

					Calendar activeSince = Calendar.getInstance();
					activeSince.set(Calendar.YEAR, 2016);
					activeSince.set(Calendar.MONTH, 1);
					activeSince.set(Calendar.DAY_OF_MONTH, 8);

					// Data set-up to create item/plan and orders
					final JbillingAPI api = envBuilder.getPrancingPonyApi();
					Calendar pricingDate = Calendar.getInstance();
					pricingDate.set(Calendar.YEAR, 2016);
					pricingDate.set(Calendar.MONTH, 1);
					pricingDate.set(Calendar.DAY_OF_MONTH, 1);

					List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
					PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
					PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
					PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

					buildAndPersistUsagePool(envBuilder, api, SCENARIO_18_USAGE_POOL, "100", envBuilder.idForCode(testCat1), items);
					buildAndPersistFlatProduct(envBuilder, api, SCENARIO_18_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "100.00", true);
					buildAndPersistPlan(envBuilder,api, SCENARIO_18_PLAN, "100 Min Plan - $100.00 / Month", MONTHLY_ORDER_PERIOD, 
										envBuilder.idForCode(SCENARIO_18_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(SCENARIO_18_USAGE_POOL)), 
										planItemProd1WS, planItemProd2WS, planItemProd3WS);

					AssetWS scenario18Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
					Map<Integer, Integer> productAssetMap = new HashMap<>();
					productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario18Asset.getId());

					Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
					productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
					productQuantityMap.put(environment.idForCode(SCENARIO_18_PLAN_PRODUCT), BigDecimal.ONE);

					InvoiceSummaryScenarioBuilder scenario18 = new InvoiceSummaryScenarioBuilder(testBuilder);
					//Create customer with NID as 1st of Mar 2016
					scenario18.createUser(SCENARIO_18_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
							  //Create monthly subscription order on 8th Feb 2016
							  .createOrder(SCENARIO_18_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
											productQuantityMap, productAssetMap, false);

					//Validating invoice summary data before 'apply order to invoice'
					}).validate((testEnv, envBuilder) -> {
						Date billingRunDate = new DateMidnight(2016, 3, 1).toDate();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						//Generating invoice for 1st Mar 2016
						scenarioBuilder.selectUserByName(SCENARIO_18_USER)
									   .generateInvoice(billingRunDate, true);

						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_18_USER));
						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #18 Invoice Summary after 1st Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("0.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("79.65"))
																	.addExpectedNewCharges(new BigDecimal("79.65"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("79.65"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();

					//Validating last invoice after 'apply order to invoice'
					}).validate((testEnv, envBuilder) -> {
						final JbillingAPI api = envBuilder.getPrancingPonyApi();
						InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
						Calendar activeSince = Calendar.getInstance();
						activeSince.set(Calendar.YEAR, 2016);
						activeSince.set(Calendar.MONTH, 1);
						activeSince.set(Calendar.DAY_OF_MONTH, 8);

						//Creating one time order
						scenarioBuilder.selectUserByName(SCENARIO_18_USER)
									   .createOrder(SCENARIO_18_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
													Collections.singletonMap(environment.idForCode(setUpFeeProduct), BigDecimal.ONE), null, false);

						InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_18_USER));
						api.applyOrderToInvoice(envBuilder.idForCode(SCENARIO_18_ONE_TIME_ORDER), invoice);

						ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
						logger.debug("Scenario #18 Invoice Summary after 2nd Invoice: {}", itemizedAccountWS.getInvoiceSummary());

						new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
																	.addExpectedAdjustmentCharges(new BigDecimal("0.00"))
																	.addExpectedFeesCharges(new BigDecimal("49.00"))
																	.addExpectedLastInvoiceDate(null)
																	.addExpectedMonthlyCharges(new BigDecimal("79.65"))
																	.addExpectedNewCharges(new BigDecimal("128.65"))
																	.addExpectedTaxesCharges(new BigDecimal("0.00"))
																	.addExpectedTotalDue(new BigDecimal("128.65"))
																	.addExpectedUsageCharges(new BigDecimal("0.00"))
																	.addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
																	.validate();
					});

					logger.debug("Invoice template test scenario #18 has been passed successfully");
		} finally {
			final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			Integer userid = testBuilder.getTestEnvironment().idForCode(SCENARIO_18_USER);
			Arrays.stream(api.getUserInvoicesPage(userid, 10, 0))
					.forEach(invoice -> {
						api.deleteInvoice(invoice.getId());
					});
		}
	}

	public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {

		AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
				.withName(name)
				.withPaymentMethodTypeIds(paymentMethodTypeId)
				.build();

		return accountTypeWS.getId();
	}

	public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
		return envBuilder.itemBuilder(api)
				.itemType()
				.withCode(code)
				.withCategoryType(categoryType)
				.global(global)
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
	
	public Integer buildAndPersistLinePercentageProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
			boolean global, Integer categoryId, String percentage, boolean allowDecimal) {
		return envBuilder.itemBuilder(api)
				.item()
				.withCode(code)
				.withType(categoryId)
				.withLinePercentage(percentage)
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

	
	public static Integer buildAndPersistPlugIn(TestEnvironmentBuilder envBuilder, JbillingAPI api, String pluginClassName, Hashtable<String, String> parameters) {
		 	   envBuilder.configurationBuilder(api)
						 .addPluginWithParameters(pluginClassName, parameters)
						 .build();
		 	   return envBuilder.idForCode(pluginClassName);
		
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

	private void updatePlugin (Integer basicItemManagerPlugInId, String className, TestBuilder testBuilder, Hashtable<String, String> parameters) {
		final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
		PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(className);
	    PluggableTaskWS plugin = api.getPluginWS(basicItemManagerPlugInId);
	    plugin.setTypeId(type.getId());
	    if(null!=parameters && !parameters.isEmpty()) {
	    	plugin.setParameters(parameters);
	    }
	    api.updatePlugin(plugin);
	}
	
	private List<String> getAssetIdentifiers(String userName) {
		if( null == userName || userName.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> identifiers = new ArrayList<String>();
		testBuilder.given(envBuilder -> {
			JbillingAPI api = envBuilder.getPrancingPonyApi();
			 
			OrderWS [] orders = api.getUserSubscriptions(envBuilder.idForCode(userName));
			Arrays.stream(orders)
				  .forEach(order -> {
					  Arrays.stream(order.getOrderLines())
					  		.forEach(line -> {
					  			Integer assetIds [] = line.getAssetIds(); 
					  			if(null!=assetIds && assetIds.length!= 0 ) {
					  				Arrays.stream(assetIds)
					  					  .forEach(assetId -> {
					  						  identifiers.add(api.getAsset(assetId).getIdentifier());
					  					  });
					  			}
					  		});
					  
				  });
				  
		});
		return identifiers;
	}
	
	private List<String> buildInboundCDR(List<String> indentifiers, String quantity, String eventDate) {
		List<String> cdrs = new ArrayList<String>();
		indentifiers.forEach(asset -> {
			cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()+",6165042651,tressie.johnson,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null");
		});
		
		return cdrs;
	}
}
