package com.sapienter.jbilling.server.spc;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.*;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetAssignmentWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.spc.SPCCustomerUsagePoolEvaluationTask;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.item.tasks.SPCRemoveAssetFromActiveOrderTask;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usagePool.util.Util;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.order.task.RefundOnCancelTask;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;
import com.sapienter.jbilling.test.TestUtils;

import com.sapienter.jbilling.resources.CancelOrderInfo;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.tools.ant.taskdefs.Sleep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import static org.testng.Assert.fail;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * 
 * @author Mahesh Kalshetty
 * Jira : JBSPC-1070
 * 
 */
@Test(groups = "agl", testName = "agl.SPCOrderFilterTaskTest")
public class SPCOrderFilterTaskTest extends SPCBaseConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final int BILLIING_TYPE_MONTHLY = 1;
    private static final Integer ORDER_BILLING_PRE_PAID = new Integer(1);
    private static final Integer ORDER_BILLING_POST_PAID = new Integer(2);
    public static final boolean PRORATE_TRUE = true;

    private static final String  USER_01 = "TestUser01" + System.currentTimeMillis();
    private static final String  USER_02 = "TestUser02" + System.currentTimeMillis();
    private static final String  USER_2_1 = "TestUser02_1" + System.currentTimeMillis();
    private static final String  USER_2_2 = "TestUser02_2" + System.currentTimeMillis();
    private static final String  USER_03 = "TestUser03" + System.currentTimeMillis();
    private static final String  USER_04 = "TestUser04" + System.currentTimeMillis();
    private static final String  USER_05 = "TestUser05" + System.currentTimeMillis();
    private static final String  USER_06 = "TestUser06" + System.currentTimeMillis();
    private static final String  USER_07 = "TestUser07" + System.currentTimeMillis();
    private static final String  USER_08 = "TestUser08" + System.currentTimeMillis();
    private static final String  USER_09 = "TestUser09" + System.currentTimeMillis();
    private static final String  USER_10 = "TestUser10" + System.currentTimeMillis();
    private static final String  USER_11 = "TestUser11" + System.currentTimeMillis();
    private static final String  USER_12 = "TestUser12" + System.currentTimeMillis();
    private static final String  USER_13 = "TestUser13" + System.currentTimeMillis();
    private static final String  USER_14 = "TestUser14" + System.currentTimeMillis();
    private static final String  USER_15 = "TestUser15" + System.currentTimeMillis();
    private static final String  USER_16 = "TestUser16" + System.currentTimeMillis();
    private static final String  USER_17 = "TestUser17" + System.currentTimeMillis();
    private static final String  USER_18 = "TestUser18" + System.currentTimeMillis();
    private static final String  USER_19 = "TestUser19" + System.currentTimeMillis();
    private static final String  USER_20 = "TestUser20" + System.currentTimeMillis();
    private static final String  USER_21 = "TestUser21" + System.currentTimeMillis();
    private static final String  USER_22 = "TestUser22" + System.currentTimeMillis();
    private static final String  USER_23 = "TestUser23" + System.currentTimeMillis();
    private static final String  USER_24 = "TestUser24" + System.currentTimeMillis();
    private static final String  USER_25 = "TestUser25" + System.currentTimeMillis();
    private static final String  USER_26 = "TestUser26" + System.currentTimeMillis();
    private static final String  USER_27 = "TestUser27" + System.currentTimeMillis();
    private static final String  USER_28 = "TestUser28" + System.currentTimeMillis();
    private static final String  USER_29 = "TestUser29" + System.currentTimeMillis();
    private static final String  USER_30 = "TestUser30" + System.currentTimeMillis();
    private static final String  USER_30_1 = "TestUser30_1" + System.currentTimeMillis();
    private static final String  USER_30_2 = "TestUser30_2" + System.currentTimeMillis();
    private static final String  USER_30_3 = "TestUser30_3" + System.currentTimeMillis();
    private static final String  USER_31 = "TestUser31" + System.currentTimeMillis();
    private static final String  USER_32 = "TestUser32" + System.currentTimeMillis();

    private static final String SUBSCRIPTION_ORDER_CODE1    = "TestOrder01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE2    = "TestOrder02"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE02_1 = "TestOrder02_1"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE02_2 = "TestOrder02_2"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE02_3 = "TestOrder02_3"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE02_4 = "TestOrder02_4"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE3    = "TestOrder03"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE4    = "TestOrder04"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE5    = "TestOrder05"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE6    = "TestOrder06"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE7    = "TestOrder07"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE8    = "TestOrder08"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE9    = "TestOrder09"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE10    = "TestOrder10"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE11    = "TestOrder11"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE12    = "TestOrder12"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE13    = "TestOrder13"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE14    = "TestOrder14"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE15    = "TestOrder15"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE16    = "TestOrder16"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE17    = "TestOrder17"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE18    = "TestOrder18"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE19    = "TestOrder19"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE20    = "TestOrder20"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE21    = "TestOrder21"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE22    = "TestOrder22"+ System.currentTimeMillis();
    
    private static final String SUBSCRIPTION_ORDER_CODE41    = "TestOrder41"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE42    = "TestOrder42"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE43    = "TestOrder43"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE44    = "TestOrder44"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE45    = "TestOrder45"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE46    = "TestOrder46"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE47    = "TestOrder47"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE48    = "TestOrder48"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE49    = "TestOrder49"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE50    = "TestOrder50"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE51    = "TestOrder51"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE52    = "TestOrder52"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE53    = "TestOrder53"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE54    = "TestOrder54"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE55    = "TestOrder55"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_CODE56    = "TestOrder56"+ System.currentTimeMillis();
        
    private static final String ASSET01 = "0422310701";
    private static final String ASSET02 = "0422310702";
    private static final String ASSET02_1 = "0433310701";
    private static final String ASSET02_2 = "0433310702";
    private static final String ASSET02_3 = "0433310703";
    private static final String ASSET02_4 = "0433310704";
    private static final String ASSET03 = "0422310703";
    private static final String ASSET04 = "0422310704";
    private static final String ASSET05 = "0422310705";
    private static final String ASSET06 = "0422310706";
    private static final String ASSET07 = "0422310707";
    private static final String ASSET08 = "0422310708";
    private static final String ASSET09 = "0422310709";
    private static final String ASSET10 = "0422310710";
    private static final String ASSET11 = "0422310711";
    private static final String ASSET12 = "0422310712";
    private static final String ASSET13 = "0422310713";
    private static final String ASSET14 = "0422310714";
    private static final String ASSET15 = "0422310715";
    private static final String ASSET16 = "0422310716";
    private static final String ASSET17 = "0422310717";
    private static final String ASSET18 = "0422310718";
    private static final String ASSET19 = "0422310719";
    private static final String ASSET20 = "0422310720";
    private static final String ASSET21 = "0422310721";
    private static final String ASSET22 = "0422310722";
    
    private static final String ASSET41 = "0422310741";
    private static final String ASSET42 = "0422310742";
    private static final String ASSET43 = "0422310743";
    private static final String ASSET44 = "0422310744";
    private static final String ASSET45 = "0422310745";
    private static final String ASSET46 = "0422310746";
    private static final String ASSET47 = "0422310747";
    private static final String ASSET48 = "0422310748";
    private static final String ASSET49 = "0422310749";
    private static final String ASSET50 = "0422310750";
    private static final String ASSET51 = "0422310751";
    private static final String ASSET52 = "0422310752";
    private static final String ASSET53 = "0422310753";
    private static final String ASSET54 = "0422310754";
    private static final String ASSET55 = "0422310755";
    private static final String ASSET56 = "0422310756";

    private static final String OPTUS_PLAN_01 = "SPCMO-01";
    private static final String OPTUS_PLAN_02 = "SPCMO-021070";
    private static final String DISCOUNT = "Discount";
    private static final String MEDIATION_FILE_PREFIX = "RESELL_";

    private static final String ORDER_ASSERT = "Order Should not null";
    private static final String USER_ASSERT = "User Created {}";
    private static final String USER_CREATION_ASSERT = "User Creation Failed";
    private static final String PLAN_CREATION_ASSERT = "Plan Creation Failed";
    private static final String ORDER_CREATION_ASSERT = "Order Creation Failed";
    public static final String CREATING_MEDIATION_FILE 		= "Creating Mediation file ....";
    public static final String CDR_LINE 					= "cdr line {}";
    public static final String MEDIATION_FILE_CREATED 		= "Mediation file created {}";
	public static final String MEDIATION_TRIGGER_FAILED 	= "Mediation trigger failed";
	public static final String MEDIATION_PROCESS 			= "Mediation Process {}";
	public static final String MEDIATION_PROCESS_ID 		= "Mediation ProcessId {}";
	public static final String MEDIATION_DONE_AND_BILLABLE 	= "Mediation Done And Billable ";
	public static final String MEDIATION_DONE_AND_NOT_BILLABLE 	= "Mediation Done And Not Billable";

    UserWS spcTestUserWS;
    Integer orderId;
    Integer orderId1;
    Integer userId ;

    private static String[] plan1PoolQuantity = {"200.0000000000", "1024.0000000000", "1024.0000000000", "1024.0000000000"};

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {
        logger.debug("SPCOrderFilterTaskTest  : {}" , testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        testBuilder.given(envBuilder -> {

            // Optus Plan 2
            String optusPlanDescription = "NBN Triple Bundle";
            String planTypeOptus = "Optus";
            String optusPlanServiceType = "Mobile";
            BigDecimal optusPlanPrice = new BigDecimal("21.8182");
            BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024) 200MB
            BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
            Integer optusPlanBoostCount = new Integer("3");
            String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

            Integer optusPlanId2 = CreatePlanUtility.createPlan(api, OPTUS_PLAN_02, planTypeOptus, optusPlanServiceType,
                    optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                    optusPlanBoostCount, optusPlanBoostQuantity);
            assertNotNull(PLAN_CREATION_ASSERT, optusPlanId2);
            logger.info("Optus {} PlanId: {}", OPTUS_PLAN_02, optusPlanId2);
            validatePlanUsagePools(optusPlanId2, 4, "200.0000000000", "1024.0000000000");

        });
    }

    @AfterClass
    public void afterTests() {
        System.out.println("SPCOrderFilterTaskTest.afterTests");
    }
 
    /**
     * Customer NID - 26th Feb 2021 - 26th monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since Day - 26th Feb 2021
     * Generate Invoice - 1st of march 2021
     * Update NId - 26th March 2021
     * Order Post-Paid - Active Since - 4th March 2021, active unit date as 21st FEB 2023
     * Generate Invoice - 29th of march 2021
     * Expected : Only pre-paid order should include and skipped post-paid order.
     */
    @Test(enabled = true, priority = 1)
    public void test3DayDelayPrepaidAndPostpaidPlanOrder01() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 1 - test3DayDelayPrepaidAndPostpaidPlanOrder01");
                Date nextInvoiceDate = getDate(-6, 26, true).getTime(); // 2021-02-26
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_01, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET01, "asset-01");
                AssetWS scenario01Asset = api.getAsset(asset1);
                assetWSs.add(scenario01Asset);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE1, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario01Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(
								DateUtils.addMonths(cycleStartDate, 1),
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 1st March
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                Date activeSince2 = getDate(-5, 04, true).getTime(); // 2021-03-04
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset2 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02, "asset-02");
                assetWSs.clear();
                AssetWS scenario02Asset = api.getAsset(asset2);
                assetWSs.add(scenario02Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE2, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario02Asset);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(spcTestUserWS.getNextInvoiceDate(), 3);
                logger.info("Second order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 29th March
                Integer[] invoices1 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices1);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices1)));
                logger.debug("Plan Order invoice created {}", invoices1[0]);
                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoices1[0]);
                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should include Post-paid order whose period is less than NID", ArrayUtils.contains(invoiceWS1.getOrders(), orderId1));

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);

                OrderWS subscriptionOrder = api.getOrder(orderId1);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(DateUtils.addDays(user.getNextInvoiceDate(), -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                		orderId1, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 8);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 15th May 2021 - 15th monthly customer- 
     * Customer type : Pre Paid
     * Order Prepaid - Active Since Day - 15th May 2021
     * Generate Invoice - 15th of May 2021
     * Update NId - 15th June 2021
     * Order Post-Paid - Active Since - 4th June 2021
     * Generate Invoice - 15th of June 2021
     * Expected : Invoice generated without 3 day delay (Customer type: Pre-Paid) and post-paid order included in invoice..
     */
    @Test(enabled = true, priority = 2)
    public void testInvoiceWithCustomerTypePrePaid() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 2 - testInvoiceWithCustomerTypePrePaid");
                Date nextInvoiceDate = getDate(-3, 15, true).getTime(); // 2021-05-15
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_02, nextInvoiceDate, "",
                		CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset3 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET03, "asset-03");
                AssetWS scenario03Asset = api.getAsset(asset3);
                assetWSs.add(scenario03Asset);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE3, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario03Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(DateUtils.addDays(
								DateUtils.addMonths(cycleStartDate, 1), 
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                // create invoice, 1st March
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.info("nextInvoiceDate: {}, invoices: {}", nextInvoiceDate, invoices);
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                Date activeSince2 = getDate(-2, 04, true).getTime(); // 2021-06-04
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset4 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET04, "asset-04");
                assetWSs.clear();
                AssetWS scenario04Asset = api.getAsset(asset4);
                assetWSs.add(scenario04Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE4, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario04Asset);

                Date cycleStartDate1 = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder1.getActiveSince());
				Date cycleEndDate1 = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(nextInvoiceDate, -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                		orderId1, cycleStartDate1, cycleEndDate1, plan1PoolQuantity, plan1PoolQuantity, 4, 8);

                // create invoice, 2021-06-15
                Integer[] invoices1 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices1)));
                logger.info("Second nextInvoiceDate: {}, invoices: {}", nextInvoiceDate, invoices1);
                logger.debug("Plan Order invoice created {}", invoices1[0]);
                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoices1[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should include Post-paid order whose period is less than NID", ArrayUtils.contains(invoiceWS1.getOrders(), orderId1));
                
                // ****************** Non-Prorate - month end scenario *******************************************************
                nextInvoiceDate = getDate(-2, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_2_1, nextInvoiceDate, "",
                		CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                assetWSs.clear();

                Integer asset02_1 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02_1, "asset-02_1");
                AssetWS scenario02_1Asset = api.getAsset(asset02_1);
                assetWSs.add(scenario02_1Asset);

                Date activeSinceDate = getDate(-2, 30, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE02_1, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario02_1Asset);

                // Non-Prorated Post-paid order
                activeSince2 = getDate(-2, 30, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset02_2 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02_2, "asset-02_2");
                assetWSs.clear();
                AssetWS scenario02_2Asset = api.getAsset(asset02_2);
                assetWSs.add(scenario02_2Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE02_2, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario02_2Asset);
                
                // create invoice, 1st Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));
                
                // Update NID as 1st Oct
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                nextInvoiceDate = spcTestUserWS.getNextInvoiceDate();
                
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                // Update NID as 1st Nov
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                nextInvoiceDate = spcTestUserWS.getNextInvoiceDate();
                
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId1));  
                
                // ****************** Non-Prorate - middle month scenario *******************************************************
                nextInvoiceDate = getDate(-3, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_2_2, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                assetWSs.clear();

                Integer asset02_3 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02_3, "asset-02_3");
                AssetWS scenario02_3Asset = api.getAsset(asset02_3);
                assetWSs.add(scenario02_3Asset);

                activeSinceDate = getDate(-3, 15, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE02_3, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario02_3Asset);

                // Non-Prorated Post-paid order
                activeSince2 = getDate(-3, 15, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset02_4 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02_4, "asset-02_4");
                assetWSs.clear();
                AssetWS scenario02_4Asset = api.getAsset(asset02_4);
                assetWSs.add(scenario02_4Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE02_4, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario02_4Asset);
                
                // create invoice, 15th Aug
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                // Update NID as 1st Sept
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                nextInvoiceDate = spcTestUserWS.getNextInvoiceDate();
                
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                // Update NID as 1st Oct
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                nextInvoiceDate = spcTestUserWS.getNextInvoiceDate();
                
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId1));


            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
                logger.debug("## Customer Id {}", userId);
            });
        } finally {
        	clearTestDataForUser(spcTestUserWS.getId());
        	spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 30th Aug 2021 - 30th monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since Day - 30th Aug 2021
     * Generate Invoice - 3rd of Sept 2021
     * Update NId - 30th Sept 2021
     * Order Post-Paid - Active Since - 27th Sept 2021
     * Generate Invoice - 2nd of Nov 2021
     * Expected : Both pre-paid and post-paid order should included in invoice.
     */
    @Test(enabled = true, priority = 3)
    public void testMonthEndCustomer() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 3 - testMonthEndCustomer");
                Date nextInvoiceDate = getDate(-2, 30, true).getTime(); // 2021-08-30
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_03, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset5 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET05, "asset-05");
                AssetWS scenario05Asset = api.getAsset(asset5);
                assetWSs.add(scenario05Asset);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE5, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario05Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(
								DateUtils.addMonths(cycleStartDate, 1),
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 1st March
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                Date activeSince2 = getDate(-1, 27, true).getTime(); // 2021-10-02
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset6 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET06, "asset-06");
                assetWSs.clear();
                AssetWS scenario06Asset = api.getAsset(asset6);
                assetWSs.add(scenario06Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE6, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario06Asset);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(spcTestUserWS.getNextInvoiceDate(), 3);
                logger.info("Second order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 30th monthly
                Integer[] invoices1 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices1);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices1)));
                logger.debug("Plan Order invoice created {}", invoices1[0]);
                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoices1[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should include Post-paid order whose period is less than NID", ArrayUtils.contains(invoiceWS1.getOrders(), orderId1));

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);

                OrderWS subscriptionOrder = api.getOrder(orderId1);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(DateUtils.addDays(user.getNextInvoiceDate(), -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                		orderId1, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 8);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 1st Aug 2021 - 1st monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since Day - 1st Aug 2021 and prorated
     * Generate Invoice - 4th Aug 2021
     * Upload mediation with event date 10th Aug 2021
     * Update NID - 1st Sept 2021
     * Order Post-Paid - Active Since - 2nd Sept 2021 with non-prorated
     * Upload mediation with event date 3rd Sept 2021
     * Generate Invoice - 4th Sept 2021
     * Expected : Only pre-paid order should include and skipped post-paid order.
     */
    @Test(enabled = true, priority = 4)
    public void testNonProratedPostpaidOrder04() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 4 - testNonProratedPostpaidOrder04");
                Date nextInvoiceDate = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_04, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset7 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET07, "asset-07");
                AssetWS scenario07Asset = api.getAsset(asset7);
                String ASSET_IDENTIFIER = scenario07Asset.getIdentifier();
                assetWSs.add(scenario07Asset);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE7, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario07Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(
								DateUtils.addMonths(cycleStartDate, 1),
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 4th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                // mediation
                String eventDate = getDateFormatted(getLocalDate(-3, 10), DATE_FORMAT_YYYYMMDD); // 10th Aug
                logger.info("eventDate: {}", eventDate);
                String OPTUS_MOBILE_FORMAT_050                   = "50%s                       616695600005050200050502%s183502202105140"
                        + "                    21100000000002TWEED HEADS TWEED HEADS 00000000000TD050CON  090400000000002 1                           "
                        + "G1999989    V000000000020210514095002000000000%s00000000000000000000000000000000000000000200000000000     "
                        + "                                                                                     003789303480";
                BigDecimal dataQuantity = new BigDecimal("112.0000000000");  //  Data in MB
                String stringQuantityInKB = prependZero(dataQuantity.multiply(new BigDecimal("1024")).setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString(), 9);
                String cdrLine = String.format(OPTUS_MOBILE_FORMAT_050, ASSET_IDENTIFIER, eventDate, stringQuantityInKB);
                // --------- Optus MUR Chargeable Mediation
                logger.debug(CREATING_MEDIATION_FILE);
                logger.debug(CDR_LINE, cdrLine);
                String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".STREAM", null,
                        Arrays.asList(cdrLine));
                logger.debug(MEDIATION_FILE_CREATED, cdrFilePath);
                UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                assertNotNull(MEDIATION_TRIGGER_FAILED, mediationProcessId);
                logger.debug(MEDIATION_PROCESS_ID, mediationProcessId);
                pauseUntilMediationCompletes(30, api);
                
                
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug(MEDIATION_PROCESS, mediationProcess);
                assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());

                BigDecimal quantityInKB = dataQuantity.multiply(new BigDecimal("1024"));
                LocalDateTime calculatedEventDateTime = convertToLocalDateTimeViaInstant(
                        getDate(-3, 10, PRORATE_TRUE).getTime())
                        .plusHours(18)
                        .plusMinutes(35)
                        .plusSeconds(02);
                logger.debug("calculatedEventDateTime: {}", calculatedEventDateTime);
                validateMediationRecords(mediationProcess, calculatedEventDateTime, 
                        dataQuantity, quantityInKB);
                
                // Update NID 1st Sept
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                Date activeSince2 = getDate(-2, 02, true).getTime(); // 2021-09-04
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset8 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET08, "asset-08");
                assetWSs.clear();
                AssetWS scenario08Asset = api.getAsset(asset8);
                assetWSs.add(scenario08Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE8, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario08Asset);
                
             // mediation
                eventDate = getDateFormatted(getLocalDate(-2, 3), DATE_FORMAT_YYYYMMDD); // 10th Aug
                logger.info("eventDate: {}", eventDate);
                OPTUS_MOBILE_FORMAT_050                   = "50%s                       616695600005050200050502%s183502202105140"
                        + "                    21100000000002TWEED HEADS TWEED HEADS 00000000000TD050CON  090400000000002 1                           "
                        + "G1999989    V000000000020210514095002000000000%s00000000000000000000000000000000000000000200000000000     "
                        + "                                                                                     003789303480";
                dataQuantity = new BigDecimal("112.0000000000");  //  Data in MB
                stringQuantityInKB = prependZero(dataQuantity.multiply(new BigDecimal("1024")).setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString(), 9);
                cdrLine = String.format(OPTUS_MOBILE_FORMAT_050, ASSET_IDENTIFIER, eventDate, stringQuantityInKB);
                // --------- Optus MUR Chargeable Mediation
                logger.debug(CREATING_MEDIATION_FILE);
                logger.debug(CDR_LINE, cdrLine);
                cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".STREAM", null,
                        Arrays.asList(cdrLine));
                logger.debug(MEDIATION_FILE_CREATED, cdrFilePath);
                mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                assertNotNull(MEDIATION_TRIGGER_FAILED, mediationProcessId);
                logger.debug(MEDIATION_PROCESS_ID, mediationProcessId);
                pauseUntilMediationCompletes(30, api);
                
                
                mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug(MEDIATION_PROCESS, mediationProcess);
                assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());

                quantityInKB = dataQuantity.multiply(new BigDecimal("1024"));
                calculatedEventDateTime = convertToLocalDateTimeViaInstant(
                        getDate(-2, 3, PRORATE_TRUE).getTime())
                        .plusHours(18)
                        .plusMinutes(35)
                        .plusSeconds(02);
                logger.debug("calculatedEventDateTime: {}", calculatedEventDateTime);
                validateMediationRecords(mediationProcess, calculatedEventDateTime, 
                        dataQuantity, quantityInKB);


                nextInvoiceDateThreeDayDelay = DateUtils.addDays(spcTestUserWS.getNextInvoiceDate(), 3);
                logger.info("Second order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 4th Sept
                Integer[] invoices1 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices1);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices1)));
                logger.debug("Plan Order invoice created {}", invoices1[0]);
                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoices1[0]);
                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should not include Post-paid order", !ArrayUtils.contains(invoiceWS1.getOrders(), orderId1));

             // Update NID 1st Oct
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                // generate invoice on 4th Oct
                nextInvoiceDateThreeDayDelay = DateUtils.addDays(spcTestUserWS.getNextInvoiceDate(), 3);
                logger.info("Second order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 4th Sept
                Integer[] invoices2 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);

            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 12 Aug 2021 - 1 monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid Prorated- Active Since Day - 12 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 15 August 2021
     * Expected : Prepaid Prorated Order will be included in the invoice
     */
    @Test(enabled = true, priority = 5)
    public void testDisconnectionCustomerTypePostPaidProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 5 - testDisconnectionCustomerTypePostPaidProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_05, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset41 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET41, "asset-41");
                AssetWS scenarioAsset41 = api.getAsset(asset41);
                String ASSET_IDENTIFIER = scenarioAsset41.getIdentifier();
                assetWSs.add(scenarioAsset41);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE41, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset41);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);
                
              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset41);
                               

                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 15 August
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
            
    /**
     * Customer NID - 01 Sept 2021 - 1 monthly customer- 
     * Customer type : Post Paid
     * Order Postpaid Prorated- Active Since Day - 01 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 04 Sept 2021
     * Expected : Postpaid Prorated Order will be included in the invoice
     * 
     */
    @Test(enabled = true, priority = 6)
    public void testDisconnectionCustomerTypePostPaidProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 6 - testDisconnectionCustomerTypePostPaidProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_06, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 
                
             // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset42 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET42, "asset-42");
                assetWSs.clear();
                AssetWS scenarioAsset42 = api.getAsset(asset42);
                String ASSET_IDENTIFIER = scenarioAsset42.getIdentifier();
                assetWSs.add(scenarioAsset42);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE42, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Post-Paid Order created {}", orderId);
                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset42);

                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset42);
                               
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 04 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 12 Aug 2021 - 1 monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid Non-Prorated- Active Since Day - 12 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 15 August 2021
     * Expected : Prepaid Non-Prorated Order will be included in the invoice
     */
    @Test(enabled = true, priority = 7)
    public void testDisconnectionCustomerTypePostPaidNonProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 7 - testDisconnectionCustomerTypePostPaidNonProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_07, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset43 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET43, "asset-43");
                AssetWS scenarioAsset43 = api.getAsset(asset43);
                String ASSET_IDENTIFIER = scenarioAsset43.getIdentifier();
                assetWSs.add(scenarioAsset43);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE43, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset43);

                //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset43);
                               

                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 15 August
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 1st Aug 2021 - 1st monthly customer 
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 15th Aug 2021 (Prorated)
     * Order Post-Paid - Active Since - 15th Aug 2021 (Prorated)
     * Generate Invoice - 18th of Aug 2021
     * Expected : Pre-paid order should include in invoice and skipped Post-paid .
     */
    @Test(enabled = true, priority = 11)
    public void testCustomerTypePostPaid() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 11 - testCustomerTypePost");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-3, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_11, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset9 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET09, "asset-09");
                AssetWS scenario09Asset = api.getAsset(asset9);
                assetWSs.add(scenario09Asset);

                Date activeSinceDate = getDate(-3, 15, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE9, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario09Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(
								DateUtils.addMonths(nextInvoiceDate, 1),
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                // Pro-rtaed Post-paid order
                Date activeSince2 = getDate(-3, 15, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset10 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET10, "asset-10");
                assetWSs.clear();
                AssetWS scenario10Asset = api.getAsset(asset10);
                assetWSs.add(scenario10Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE10, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario10Asset);
                
                cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder1.getActiveSince());
				cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(nextInvoiceDate, 1), -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                		orderId1, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 8);
                
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 1st Sept 2021 - 1st monthly customer-
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 15th Aug 2021 (Non-Prorated)
     * Order Post-Paid - Active Since - 15th Aug 2021 (Non-Prorated)
     * Generate Invoice - 18th of Aug 2021
     * Expected : Pre-paid order should include in invoice and skipped Post-paid .
     */
    @Test(enabled = true, priority = 12)
    public void testCustomerTypePostPaidMiddleMonthNonProrated() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 12 - testCustomerTypePostPaidMiddleMonthNonProrated");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-3, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_12, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset11 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET11, "asset-11");
                AssetWS scenario11Asset = api.getAsset(asset11);
                assetWSs.add(scenario11Asset);

                Date activeSinceDate = getDate(-3, 15, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE11, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario11Asset);

                // Non-Prorated Post-paid order
                Date activeSince2 = getDate(-3, 15, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset12 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET12, "asset-12");
                assetWSs.clear();
                AssetWS scenario12Asset = api.getAsset(asset12);
                assetWSs.add(scenario12Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE12, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario12Asset);
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 1st Sept 2021 - 1st monthly customer-
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 30th Sept 2021 (Prorated)
     * Order Post-Paid - Active Since - 30th Sept 2021 (Prorated)
     * Generate Invoice - 3rd  Oct 2021
     * Expected : Pre-paid order should include in invoice and skipped Post-paid .
     */
    @Test(enabled = true, priority = 13)
    public void testCustomerTypePostPaidMonthEndProrated() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 13 - testCustomerTypePostPaidMonthEndProrated");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-2, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_13, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset13 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET13, "asset-13");
                AssetWS scenario13Asset = api.getAsset(asset13);
                assetWSs.add(scenario13Asset);
                
                Date activeSinceDate = getDate(-2, 30, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE13, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario13Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(
								DateUtils.addMonths(nextInvoiceDate, 1),
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                // Pro-rtaed Post-paid order
                Date activeSince2 = getDate(-2, 30, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset14 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET14, "asset-14");
                assetWSs.clear();
                AssetWS scenario14Asset = api.getAsset(asset14);
                assetWSs.add(scenario14Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE14, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario14Asset);
                
                cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder1.getActiveSince());
				cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(nextInvoiceDate, 1), -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                		orderId1, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 8);
                
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 1st Sept 2021 - 1st monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 30th Sept 2021 (Non-Prorated)
     * Order Post-Paid - Active Since - 30th Sept 2021 (Non-Prorated)
     * Generate Invoice - 3rd  Oct 2021
     * Expected : Pre-paid order should include in invoice and skipped Post-paid .
     */
    @Test(enabled = true, priority = 14)
    public void testCustomerTypePostPaidMonthEndNonProrated() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 14 - testCustomerTypePostPaidMonthEndNonProrated");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-2, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_14, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset15 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET15, "asset-15");
                AssetWS scenario15Asset = api.getAsset(asset15);
                assetWSs.add(scenario15Asset);

                Date activeSinceDate = getDate(-2, 30, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE15, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario15Asset);

                // Non-Prorated Post-paid order
                Date activeSince2 = getDate(-2, 30, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset16 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET16, "asset-16");
                assetWSs.clear();
                AssetWS scenario16Asset = api.getAsset(asset16);
                assetWSs.add(scenario16Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE16, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario16Asset);
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 1st Sept 2021 - 1st monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 4th Sept 2021 (Prorated)
     * Order Post-Paid - Active Since - 4th Sept 2021 (Prorated)
     * Generate Invoice - 4th Sept 2021
     * Expected : Pre-paid order should include in invoice and skipped Post-paid .
     */
    @Test(enabled = true, priority = 15)
    public void testOrder3DayDelayProrated() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 15 - testOrder3DayDelayProrated");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-2, 1, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_15, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset17 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET17, "asset-17");
                AssetWS scenario17Asset = api.getAsset(asset17);
                assetWSs.add(scenario17Asset);
                
                Date activeSinceDate = getDate(-2, 4, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE17, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario17Asset);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
				Date cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(
								DateUtils.addDays(
								DateUtils.addMonths(nextInvoiceDate, 1),
								-1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

                // Pro-rtaed Post-paid order
                Date activeSince2 = getDate(-2, 4, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset18 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET18, "asset-18");
                assetWSs.clear();
                AssetWS scenario18Asset = api.getAsset(asset18);
                assetWSs.add(scenario18Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE18, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario18Asset);
                
                cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder1.getActiveSince());
				cycleEndDate = com.sapienter.jbilling.server.util.Util
						.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(nextInvoiceDate, 1), -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                		orderId1, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 8);
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 10th Sept 2021 - 10st monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 12th Sept 2021 (Non-Prorated)
     * Order Post-Paid - Active Since - 12th Sept 2021 (Non-Prorated)
     * Generate invoice for 13th Sept, moved NID to 10th Oct 2021
     * Generate invoice for 13th Oct 2021, moved NID to 10th Nov 2021
     * Generate invoice for 13th Nov 2021
     * Expected : Pre-paid order should include and Post-paid with period (12/09/2021 to 11/10/2021) in invoice.
     */
    @Test(enabled = true, priority = 16)
    public void testOrder3DayDelayNonProrated() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 16 - testOrder3DayDelayNonProrated");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-2, 10, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_16, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset19 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET19, "asset-19");
                AssetWS scenario19Asset = api.getAsset(asset19);
                assetWSs.add(scenario19Asset);
                
                Date activeSinceDate = getDate(-2, 12, true).getTime();
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE19, spcTestUserWS.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario19Asset);

                // Non-Prorated Post-paid order
                Date activeSince2 = getDate(-2, 12, true).getTime(); // 2021-09-12
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset20 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET20, "asset-20");
                assetWSs.clear();
                AssetWS scenario20Asset = api.getAsset(asset20);
                assetWSs.add(scenario20Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE20, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario20Asset);
                
                // Generate invoice 13th Sept
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
                // Generate invoice for 13th Oct 2021, moved NID to 10th Nov 2021
                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
                // Generate invoice for 13th Nov 2021
                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    
    /**
     * Customer NID - 15th Aug 2021 - 15th monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since - 15th Aug 2021 (Non-Prorated)
     * Order Post-Paid - Active Since - 15th Aug 2021 (Non-Prorated)
     * Generate Invoice - 18th of Aug 2021
     * Expected : Pre-paid order should include in invoice and skipped Post-paid .
     */
    @Test(enabled = true, priority = 17)
    public void testMiddleMonhlyCustomerWIthMiddleMonthNonProrated() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 17 - testMiddleMonhlyCustomerWIthMiddleMonthNonProrated");
                // Prorated Pre-Paid order with active since middle month.
                Date nextInvoiceDate = getDate(-3, 15, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_17, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset21 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET21, "asset-21");
                AssetWS scenario21Asset = api.getAsset(asset21);
                assetWSs.add(scenario21Asset);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE21, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenario21Asset);

                // Pro-rtaed Post-paid order
                Date activeSince2 = getDate(-3, 15, true).getTime(); // 2021-83-15
                logger.info("Active Since2: {}", activeSince2);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset22 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET22, "asset-22");
                assetWSs.clear();
                AssetWS scenario22Asset = api.getAsset(asset22);
                assetWSs.add(scenario22Asset);
                orderId1 = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE22, spcTestUserWS.getId(), activeSince2, null, MONTHLY_ORDER_PERIOD,
                		com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId1);
                logger.debug("Post-Paid Order created {}", orderId1);
                OrderWS subscriptionOrder1 = api.getOrder(orderId1);
                validateAssetAssignmentByOrder(subscriptionOrder1, scenario22Asset);
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 18th Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                assertTrue("Invoice must have Post-paid order", !ArrayUtils.contains(invoiceWS.getOrders(), orderId1));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 01 Sept 2021 - 1 monthly customer- 
     * Customer type : Post Paid
     * Order Postpaid Non-Prorated- Active Since Day - 01 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 04 Sept 2021
     * Expected : Postpaid Prorated Order will be included in the invoice
     * 
     */
    @Test(enabled = true, priority = 18)
    public void testDisconnectionCustomerTypePostPaidNonProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 18 - testDisconnectionCustomerTypePostPaidNonProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_18, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 
                
             // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset44 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET44, "asset-44");
                assetWSs.clear();
                AssetWS scenarioAsset44 = api.getAsset(asset44);
                String ASSET_IDENTIFIER = scenarioAsset44.getIdentifier();
                assetWSs.add(scenarioAsset44);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE44, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Post-Paid Order created {}", orderId);
                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset44);

              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset44);
                               
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 04 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 12 Aug 2021 - 1 monthly customer- 
     * Customer type : Pre-Paid
     * Order Prepaid Prorated- Active Since Day - 12 Aug 2021
     * Generate Invoice - 12 August 2021
     * Order Cancelled on 14 August
     * 
     * Expected : Prepaid Prorated Order will be included in the invoice
     */
    @Test(enabled = true, priority = 19)
    public void testDisconnectionCustomerTypePrePaidProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 19 - testDisconnectionCustomerTypePrePaidProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_19, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset45 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET45, "asset-45");
                AssetWS scenarioAsset45 = api.getAsset(asset45);
                String ASSET_IDENTIFIER = scenarioAsset45.getIdentifier();
                assetWSs.add(scenarioAsset45);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE45, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset45);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);
                
              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset45);

                // create invoice, 12 August
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 01 Sept 2021 - 1 monthly customer- 
     * Customer type : Pre-Paid
     * Order Postpaid Prorated- Active Since Day - 01 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 01 Sept 2021
     * Expected : Postpaid Prorated Order will be included in the invoice
     * 
     */
    @Test(enabled = true, priority = 20)
    public void testDisconnectionCustomerTypePrePaidProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 6 - testDisconnectionCustomerTypePrePaidProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_21, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 
                
             // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset46 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET46, "asset-46");
                assetWSs.clear();
                AssetWS scenarioAsset46 = api.getAsset(asset46);
                String ASSET_IDENTIFIER = scenarioAsset46.getIdentifier();
                assetWSs.add(scenarioAsset46);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE42, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Post-Paid Order created {}", orderId);
                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset46);

                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);

              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset46);
                               
                // create invoice, 01 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 12 Aug 2021  
     * Customer type : Pre-Paid
     * Order Prepaid Non-Prorated- Active Since Day - 12 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 12 August 2021
     * Expected : Prepaid Non-Prorated Order will be included in the invoice
     */
    @Test(enabled = true, priority = 21)
    public void testDisconnectionCustomerTypePrePaidNonProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 21 - testDisconnectionCustomerTypePrePaidNonProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_22, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset47 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET47, "asset-47");
                AssetWS scenarioAsset47 = api.getAsset(asset47);
                String ASSET_IDENTIFIER = scenarioAsset47.getIdentifier();
                assetWSs.add(scenarioAsset47);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE47, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset47);
              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset47);
                               
                // create invoice, 12 August
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 01 Sept 2021 - 1 monthly customer- 
     * Customer type : Pre-Paid
     * Order Postpaid Non-Prorated- Active Since Day - 01 Aug 2021
     * Order Cancelled on 14 August
     * Generate Invoice - 01 Sept 2021
     * Expected : Postpaid Prorated Order will be included in the invoice
     * 
     */
    @Test(enabled = true, priority = 22)
    public void testDisconnectionCustomerTypePrePaidNonProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 22 - testDisconnectionCustomerTypePrePaidNonProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_23, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 
                
             // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);
                // optus
                planWS = api.getPlanByInternalNumber(OPTUS_PLAN_02, api.getCallerCompanyId());
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer asset48 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET48, "asset-48");
                assetWSs.clear();
                AssetWS scenarioAsset48 = api.getAsset(asset48);
                String ASSET_IDENTIFIER = scenarioAsset48.getIdentifier();
                assetWSs.add(scenarioAsset48);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE48, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Post-Paid Order created {}", orderId);
                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset48);

              //cancel Order
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset48);
                               
                // create invoice, 01 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 12 Aug 2021 
     * Customer type : Post Paid
     * Order Prepaid Prorated- Active Since Day - 12 Aug 2021
     * Generate Invoice - 15 August 2021
     * Order Cancelled on 20 August
     * Generate Invoice - 15 Sept 2021
     * Expected : Credit Order(21 August to 11 Sept) included in the invoice.
     */
    @Test(enabled = true, priority = 23)
    public void testDisconnectionAfterInvoiceCustomerTypePostPaidProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 23 - testDisconnectionCustomerTypePostPaidProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_24, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset49 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET49, "asset-49",ASSET49);
                
                AssetWS scenarioAsset49 = api.getAsset(asset49);
                String ASSET_IDENTIFIER = scenarioAsset49.getIdentifier();
                assetWSs.add(scenarioAsset49);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE49, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset49);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 15 Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 20 Aug
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 20).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset49);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 15 Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 01 Sept 2021
     * Customer type : Post Paid
     * Order Postpaid Prorated- Active Since Day - 01 Aug 2021
     * Generate Invoice - 04 Sept 2021
     * Order Cancelled on 14 Sept
     * Generate Invoice - 04 Oct 2021
     * Expected : Order(01 Sept to 14 Sept) included on the invoice.
     */
    @Test(enabled = true, priority = 24)
    public void testDisconnectionAfterInvoiceCustomerTypePostPaidProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 24 - testDisconnectionAfterInvoiceCustomerTypePostPaidProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_25, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                
                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);

                Integer asset50 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET50, "asset-50",ASSET50);
                
                AssetWS scenarioAsset50 = api.getAsset(asset50);
                String ASSET_IDENTIFIER = scenarioAsset50.getIdentifier();
                assetWSs.add(scenarioAsset50);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE50, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset50);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);
                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 04 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 14 Sept
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-2, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset50);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 04 Oct
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 12 Aug 2021 
     * Customer type : Post Paid
     * Order Prepaid Non-Prorated- Active Since Day - 12 Aug 2021
     * Generate Invoice - 15 August 2021
     * Order Cancelled on 20 August
     * Generate Invoice - 15 Sept 2021
     * Expected : No Invoice created.
     */
    @Test(enabled = true, priority = 25)
    public void testDisconnectionAfterInvoiceCustomerTypePostPaidNonProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 25 - testDisconnectionAfterInvoiceCustomerTypePostPaidNonProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_26, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset51 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET51, "asset-51",ASSET51);
                
                AssetWS scenarioAsset51 = api.getAsset(asset51);
                String ASSET_IDENTIFIER = scenarioAsset51.getIdentifier();
                assetWSs.add(scenarioAsset51);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE51, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset51);
                                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 15 Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 20 Aug
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 20).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset51);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 15 Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice should NOT be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isEmpty(invoices)));
                logger.debug("Invoice is NOT created");
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 01 Sept 2021
     * Customer type : Post Paid
     * Order Postpaid Non-Prorated- Active Since Day - 01 Aug 2021
     * Generate Invoice - 04 Sept 2021
     * Order Cancelled on 14 Sept
     * Generate Invoice - 04 Oct 2021
     * Expected : Order(01 Sept to 30 Sept) included on the invoice.
     */
    @Test(enabled = true, priority = 26)
    public void testDisconnectionAfterInvoiceCustomerTypePostPaidNonProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 24 - testDisconnectionAfterInvoiceCustomerTypePostPaidProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_27, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                
                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);

                Integer asset52 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET52, "asset-52",ASSET52);
                
                AssetWS scenarioAsset52 = api.getAsset(asset52);
                String ASSET_IDENTIFIER = scenarioAsset52.getIdentifier();
                assetWSs.add(scenarioAsset52);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE52, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset52);
                                
                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 04 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 14 Sept
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-2, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset52);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create invoice, 04 Oct
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }

    /**
     * Customer NID - 12 Aug 2021 
     * Customer type : Pre-Paid
     * Order Prepaid Prorated- Active Since Day - 12 Aug 2021
     * Generate Invoice - 12 August 2021
     * Order Cancelled on 20 August
     * Generate Invoice - 12 Sept 2021
     * Expected : No Invoice created.
     */
    @Test(enabled = true, priority = 27)
    public void testDisconnectionAfterInvoiceCustomerTypePrePaidProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 27 - testDisconnectionAfterInvoiceCustomerTypePrePaidProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_28, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset53 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET53, "asset-53",ASSET53);
                
                AssetWS scenarioAsset53 = api.getAsset(asset53);
                String ASSET_IDENTIFIER = scenarioAsset53.getIdentifier();
                assetWSs.add(scenarioAsset53);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE53, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset53);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);
                
                // create invoice, 12 Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 20 Aug
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 20).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset53);
                
                // create invoice, 12 Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice should NOT be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isEmpty(invoices)));
                logger.debug("The invoice is NOT created");
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 01 Sept 2021
     * Customer type : Pre-Paid
     * Order Postpaid Prorated- Active Since Day - 01 Aug 2021
     * Generate Invoice - 01 Sept 2021
     * Order Cancelled on 14 Sept
     * Generate Invoice - 01 Oct 2021
     * Expected : Order(01 Sept to 14 Sept) included on the invoice.
     */
    @Test(enabled = true, priority = 28)
    public void testDisconnectionAfterInvoiceCustomerTypePrePaidProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 28 - testDisconnectionAfterInvoiceCustomerTypePrePaidProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_29, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                
                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);

                Integer asset54 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET54, "asset-54",ASSET54);
                
                AssetWS scenarioAsset54 = api.getAsset(asset54);
                String ASSET_IDENTIFIER = scenarioAsset54.getIdentifier();
                assetWSs.add(scenarioAsset54);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE50, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        ORDER_BILLING_POST_PAID, true, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset54);
                Date cycleStartDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(subscriptionOrder.getActiveSince());
                Date cycleEndDate = com.sapienter.jbilling.server.util.Util
                        .getEndOfDay(
                                DateUtils.addDays(
                                DateUtils.addMonths(cycleStartDate, 1),
                                -1));
                validateCustomerUsagePoolsByCustomer(spcTestUserWS.getCustomerId(), 
                        orderId, cycleStartDate, cycleEndDate, plan1PoolQuantity, plan1PoolQuantity, 4, 4);
                
                // create invoice, 01 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 14 Sept
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-2, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset54);

                // create invoice, 01 Oct
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }


    /**
     * Customer NID - 26th Feb 2021 - 26th monthly customer- 
     * Customer type : Post Paid
     * Order Prepaid - Active Since Day - 26th Feb 2021
     * Generate Invoice - 1st of march 2021
     * Update NId - 26th March 2021
     * Order Post-Paid - Active Since - 4th March 2021, active unit date as 21st FEB 2023
     * Generate Invoice - 29th of march 2021
     * Expected : Only pre-paid order should include and skipped post-paid order.
     * 
     */
    @Test(enabled = true, priority = 30)
    public void testOneTimeOrder30() {

        try {
            testBuilder.given(envBuilder -> {

            	// public static final 
            	String PRICE = "10";
                logger.debug("Scenario 30 - testOneTimeOrder30");
                Date nextInvoiceDate = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_30, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                Integer categoryId =  getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_SERVICES_CATEGORY) ;
                Integer productId = buildAndPersistFlatProduct(envBuilder, api, "Flat Discount", DISCOUNT,
                        false, categoryId, "10.00", true, 0, false);

                // Order with active since as start of the billing cycle.
                logger.debug("First scenario: active since as start of the billing cycle");
                Date activeSinceDate = nextInvoiceDate;
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(productId, BigDecimal.ONE);
                Integer discountOrderId =
                        createServiceOrder("discountOrderId01", activeSinceDate, null,
                        		ONE_TIME_PERIOD, false, productQuantityMap, null,
                        		spcTestUserWS.getUserId(), PRICE, "", null);

                assertNotNull(ORDER_CREATION_ASSERT, discountOrderId);
                logger.debug("One time Order created {}", discountOrderId);

                OrderWS subscriptionOrder = api.getOrder(discountOrderId);

                Date nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create 1st Aug invoice
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);
                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have order", ArrayUtils.contains(invoiceWS.getOrders(), discountOrderId));
                
                logger.debug("************** Second scenario: active since as start of the billing cycle");
                productId = buildAndPersistFlatProduct(envBuilder, api, "BOC-MPF", "Mobile Plan Fee Break of Contract",
                        false, categoryId, "0.00", true, 0, false);
                nextInvoiceDate = getDate(-3, 01, true).getTime();
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_30_3, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);

                activeSinceDate = getDate(-3, 16, true).getTime();
                productQuantityMap = new HashMap<>();
                productQuantityMap.put(productId, BigDecimal.ONE);
                Integer feeOrderId1 =
                        createServiceOrder("Contract Break Fee", activeSinceDate, null,
                        		ONE_TIME_PERIOD, false, productQuantityMap, null,
                        		spcTestUserWS.getUserId(), "0.20", "", null);

                assertNotNull(ORDER_CREATION_ASSERT, feeOrderId1);
                logger.debug("One time Order created {}", feeOrderId1);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create 4st Aug invoice
            	invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice should not create", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isEmpty(invoices)));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(spcTestUserWS.getNextInvoiceDate(), 3);
                logger.info("Order NID: {}", nextInvoiceDateThreeDayDelay);
                // create 1st Sept invoice on 4th Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoices[0]);
                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should include Post-paid order", ArrayUtils.contains(invoiceWS1.getOrders(), feeOrderId1));
                
                logger.debug("************** Third scenario: active since as end of the month");
                nextInvoiceDate = getDate(-3, 01, true).getTime();
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_30_1, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId());

                activeSinceDate = getDate(-3, 30, true).getTime();
                productQuantityMap = new HashMap<>();
                productQuantityMap.put(productId, BigDecimal.ONE);
                Integer feeOrderId2 =
                        createServiceOrder("Contract Break Fee", activeSinceDate, null,
                        		ONE_TIME_PERIOD, false, productQuantityMap, null,
                        		spcTestUserWS.getUserId(), "0.10", "", null);

                assertNotNull(ORDER_CREATION_ASSERT, feeOrderId2);
                logger.debug("One time Order created {}", feeOrderId2);

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);
                // create 4st Aug invoice
            	logger.debug("nextInvoiceDateThreeDayDelay: {}", nextInvoiceDateThreeDayDelay);
            	invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
            	logger.info("Month end scenario: nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices);
                assertEquals("Invoice should not create", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isEmpty(invoices)));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                nextInvoiceDateThreeDayDelay = DateUtils.addDays(spcTestUserWS.getNextInvoiceDate(), 3);
                logger.info("Order NID: {}", nextInvoiceDateThreeDayDelay);
                // create 1st Sept invoice on 4th Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                invoiceWS1 = api.getInvoiceWS(invoices[0]);
                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should include Post-paid order", ArrayUtils.contains(invoiceWS1.getOrders(), feeOrderId2));

                // ------------------------------------------------------------
                logger.debug("************** Fourth scenario: active since as within 3 days delay period");
                nextInvoiceDate = getDate(-3, 01, true).getTime();
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_30_2, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("User Id: {}", spcTestUserWS.getId());
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId());

                activeSinceDate = getDate(-3, 3, true).getTime();
                productQuantityMap = new HashMap<>();
                productQuantityMap.put(productId, BigDecimal.ONE);
                Integer feeOrderId3 =
                        createServiceOrder("Contract Break Fee", activeSinceDate, null,
                        		ONE_TIME_PERIOD, false, productQuantityMap, null,
                        		spcTestUserWS.getUserId(), "0.30", "", null);

                assertNotNull(ORDER_CREATION_ASSERT, feeOrderId3);
                logger.debug("One time Order created {}", feeOrderId3);
                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);

                // create 4st Aug invoice
            	logger.debug("nextInvoiceDateThreeDayDelay: {}", nextInvoiceDateThreeDayDelay);
            	invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
            	logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices);
                assertEquals("Invoice should not create", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isEmpty(invoices)));

                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
                nextInvoiceDateThreeDayDelay = DateUtils.addDays(nextInvoiceDate, 3);
                logger.info("Three delay delay order NID: {}", nextInvoiceDateThreeDayDelay);

            	invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateThreeDayDelay, null, null, false);
                assertEquals("Invoice creation failed", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.info("nextInvoiceDateThreeDayDelay: {}, invoices: {}", nextInvoiceDateThreeDayDelay, invoices);
                invoiceWS1 = api.getInvoiceWS(invoices[0]);
                logger.debug("Orders included in invoice:{} are: {}", invoiceWS1.getId(), invoiceWS1.getOrders());
                assertTrue("Invoice should include Post-paid order", ArrayUtils.contains(invoiceWS1.getOrders(), feeOrderId3));

            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);

            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }
    
    /**
     * Customer NID - 12 Aug 2021 
     * Customer type : Pre-Paid
     * Order Prepaid Non-Prorated- Active Since Day - 12 Aug 2021
     * Generate Invoice - 12 August 2021
     * Order Cancelled on 20 August
     * Generate Invoice - 12 Sept 2021
     * Expected : No Invoice created.
     */
    @Test(enabled = true, priority = 29)
    public void testDisconnectionAfterInvoiceCustomerTypePrePaidNonProratedPrepaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 29 - testDisconnectionAfterInvoiceCustomerTypePostPaidNonProratedPrepaidOrder");
                Date nextInvoiceDate = getDate(-3, 12, true).getTime(); // 2021-08-12
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_31, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset55 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET55, "asset-55",ASSET55);
                
                AssetWS scenarioAsset55 = api.getAsset(asset55);
                String ASSET_IDENTIFIER = scenarioAsset55.getIdentifier();
                assetWSs.add(scenarioAsset55);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE55, spcTestUserWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset55);
                                
                // create invoice, 12 Aug
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 20 Aug
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-3, 20).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset55);

                // create invoice, 12 Sept
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice should NOT be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isEmpty(invoices)));
                logger.debug("Invoice is NOT created");
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;
        }
    }


    private void validateAssetAssignmentByOrder(OrderWS subscriptionOrder, AssetWS asset) {

    	assertNotNull("Subscription order should not null", subscriptionOrder);
    	assertNotNull("Asset should not null", asset);

    	AssetAssignmentWS[] assetAssignments = api.getAssetAssignmentsForOrder(subscriptionOrder.getId());
        assertEquals("Asset Id not matched", asset.getId(), assetAssignments[0].getAssetId());
        assertEquals("Asset assignment start date not matched", subscriptionOrder.getActiveSince(), assetAssignments[0].getStartDatetime());
        if (null != subscriptionOrder.getActiveUntil()) {
        	Date assetAssignmentEndDate = com.sapienter.jbilling.server.util.Util.getEndOfDay(subscriptionOrder.getActiveUntil());
            assertEquals("Asset assignment end Date not matched", assetAssignmentEndDate, assetAssignments[0].getEndDatetime());
        }
    }

    private void validateCustomerUsagePoolsByCustomer(Integer customerId, Integer orderId, Date cycleStartDate, Date cycleEndDate,
            String[] initialQuantity, String[] poolQuantity, int orderPoolCount, int totalPoolCount) {
        CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(customerId);
        assertNotNull("Customer UsagePool creation failed", customerUsagePools);
        assertTrue("Customer Usage Pool Map count should greater than zero", customerUsagePools.length > 0);
        assertEquals("Customer Usage Pool Map count not matched", customerUsagePools.length, totalPoolCount);

        List<CustomerUsagePoolWS> lessPrecedencePools = sortCustomerUsagePoolsByPreference(orderId, customerUsagePools);
        assertEquals("Customer Usage Pool Map creation failed", lessPrecedencePools.size(), orderPoolCount);

        int i = 0;
        for (CustomerUsagePoolWS customerUsagePool : lessPrecedencePools) {
            if (i > orderPoolCount) {
                assertEquals("Customer usage pool count mis-matched: ", i, orderPoolCount);
            }
            if (orderId.equals(customerUsagePool.getOrderId())) {
                validateCustomerUsagePool(customerUsagePool, initialQuantity[i], poolQuantity[i], cycleStartDate, cycleEndDate);
            }
            i = ++i;
        }
    }
    
    /**
     * Customer NID - 01 Sept 2021
     * Customer type : Pre-Paid
     * Order Postpaid Non-Prorated- Active Since Day - 01 Aug 2021
     * Generate Invoice - 01 Sept 2021
     * Order Cancelled on 14 Sept
     * Generate Invoice - 01 Oct 2021
     * Expected : Order(01 Sept to 30 Sept) included on the invoice.
     */
    @Test(enabled = true, priority = 30)
    public void testDisconnectionAfterInvoiceCustomerTypePrePaidNonProratedPostpaidOrder() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Scenario 30 - testDisconnectionAfterInvoiceCustomerTypePrePaidNonProratedPostpaidOrder");
                Date nextInvoiceDate = getDate(-2, 01, true).getTime(); // 2021-09-01
                logger.info("Next Invoice Date: {}", nextInvoiceDate);
                spcTestUserWS = getSPCTestUserWS(envBuilder, USER_32, nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                logger.debug("Customer Id: {}", spcTestUserWS.getCustomerId()); 

                // optus
                PlanWS planWS = api.getPlanByInternalNumber(OPTUS_PLAN_01, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                
                Date activeSince = getDate(-3, 01, true).getTime(); // 2021-08-01
                logger.info("Active Since: {}", activeSince);

                Integer asset56 = buildAndPersistAssetWithServiceId(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET56, "asset-56",ASSET56);
                
                AssetWS scenarioAsset56 = api.getAsset(asset56);
                String ASSET_IDENTIFIER = scenarioAsset56.getIdentifier();
                assetWSs.add(scenarioAsset56);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_CODE56, spcTestUserWS.getId(), activeSince, null, MONTHLY_ORDER_PERIOD,
                        ORDER_BILLING_POST_PAID, false, productQuantityMap, assetWSs, planWS.getId());
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                logger.debug("Plan Order created {}", orderId);

                OrderWS subscriptionOrder = api.getOrder(orderId);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset56);
                                
                // create invoice, 01 Sept
                Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                assertTrue("Invoice must have Pre-paid order", ArrayUtils.contains(invoiceWS.getOrders(), orderId));
                
                nextInvoiceDate = DateUtils.addMonths(nextInvoiceDate, 1);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                
              //cancel Order on 14 Sept
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(orderId);
                cancelOrderInfo.setActiveUntil(getDate(-2, 14).getTime());
                api.cancelServiceOrder(cancelOrderInfo);
                api.removeAssetFromActiveOrder(ASSET_IDENTIFIER);
                validateAssetAssignmentByOrder(subscriptionOrder, scenarioAsset56);

                // create invoice, 01 Oct
                invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, false);
                assertEquals("Invoice must be created ", Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                invoiceWS = api.getInvoiceWS(invoices[0]);

                logger.debug("Orders included in invoice:{} are: {}", invoiceWS.getId(), invoiceWS.getOrders());
                
            }).validate((testEnv, testEnvBuilder) -> {
                userId = spcTestUserWS.getId();
                assertNotNull(USER_CREATION_ASSERT, userId);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);
                UserWS user = api.getUserWS(userId);
            });
        } finally {
            /*clearTestDataForUser(spcTestUserWS.getId());
            spcTestUserWS = null;*/
        }
    }


    private void validateCustomerUsagePool(CustomerUsagePoolWS usagePool, String initialQuantity, String quantity, Date start, Date end) {
        assertNotNull("Customer UsagePool creation failed", usagePool);
        assertEquals("Customer Usage pool initial quantity not matched", initialQuantity, usagePool.getInitialQuantity());
        assertEquals("Customer Usage pool quantity not matched", Util.string2decimal(quantity), usagePool.getQuantityAsDecimal());
        assertEquals("Expected Cycle start date of customer usage pool: ", start, usagePool.getCycleStartDate());
        assertEquals("Expected Cycle end date of customer usage pool: ", end, usagePool.getCycleEndDate());
    }

    private List<CustomerUsagePoolWS> sortCustomerUsagePoolsByPreference(Integer orderId, CustomerUsagePoolWS[] customerUsagePools) {

        assertNotNull("Customer UsagePool creation failed", customerUsagePools);
        assertTrue("Customer Usage Pool Map count should greater than zero", customerUsagePools.length > 0);
        List<CustomerUsagePoolWS> lessPrecedencePools = Arrays
                .stream(customerUsagePools)
                .filter(customerUsagePool -> orderId.equals(customerUsagePool
                        .getOrderId())).collect(Collectors.toList());
        Collections.sort(lessPrecedencePools, Comparator.comparing(p1 -> p1.getUsagePool().getPrecedence()));
        return lessPrecedencePools;
    }

    protected void validateMediationRecords(MediationProcess mediationProcess, LocalDateTime calculatedEventDateTime,
            BigDecimal quantity, BigDecimal originalQuantity) {

        Integer[] orderIds = mediationProcess.getOrderIds();
        assertNotNull("Mediation orders should not null", orderIds);
        assertTrue("Mediation orders count greater than zero", orderIds.length > 0);
        JbillingMediationRecord[] mediationRecords = api.getMediationEventsForOrder(orderIds[0]);
        assertNotNull("Mediation records not null", mediationRecords);
        assertTrue("Mediation records count greater than zero", mediationRecords.length > 0);

        JbillingMediationRecord record = mediationRecords[0];
        assertNotNull("Mediation record should not null", record);

        assertEquals("Mediation user ID not matched", Integer.valueOf(spcTestUserWS.getId()), record.getUserId());
        assertEquals("Mediation order Id not matched", orderIds[0], record.getOrderId());
        assertEquals("Mediation qunatity not matched", quantity, record.getQuantity());
        assertEquals("Mediation byte quantity not matched", originalQuantity, record.getOriginalQuantity());
        // Integer hours = new BigDecimal("39600").
        LocalDateTime mediationEventDate = convertToLocalDateTimeViaInstant(record.getEventDate());
        logger.info("Actual mediation eventDate: {}, calculatedEventDateTime: {}", mediationEventDate, calculatedEventDateTime);
        assertEquals("Mediation event date time not matched", calculatedEventDateTime, mediationEventDate);
    }

}
