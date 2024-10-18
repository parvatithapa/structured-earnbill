package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.resources.CancelOrderInfo;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.creditnote.db.CreditType;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import static org.testng.Assert.fail;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

@Test(groups = { "agl" }, testName = "AdHocCreditNoteTest")
public class AdHocCreditNoteTest extends SPCBaseConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String TEST_CUSTOMER_1 = "TestUser-1" + System.currentTimeMillis();
	private static final String TEST_ORDER_1 = "TestOrder-1" + System.currentTimeMillis();
	private static final String TEST_CUSTOMER_2 = "TestUser-2" + System.currentTimeMillis();
	private static final String TEST_ORDER_2 = "TestOrder-2" + System.currentTimeMillis();
	private static final String TEST_CUSTOMER_3 = "TestUser-3" + System.currentTimeMillis();
	private static final String TEST_ORDER_3 = "TestOrder-3" + System.currentTimeMillis();
	private static final String TEST_CUSTOMER_4 = "TestUser-4" + System.currentTimeMillis();
	private static final String TEST_ORDER_4_1 = "TestOrder-4_1" + System.currentTimeMillis();
	private static final String TEST_ORDER_4_2 = "TestOrder-4_2" + System.currentTimeMillis();
	private static final String TEST_CUSTOMER_5 = "TestUser-5" + System.currentTimeMillis();
    private static final String TEST_ORDER_5_1 = "TestOrder-5_1" + System.currentTimeMillis();
    private static final String TEST_ORDER_5_2 = "TestOrder-5_2" + System.currentTimeMillis();
    private static final String TEST_CUSTOMER_6 = "TestUser-6" + System.currentTimeMillis();
    private static final String TEST_ORDER_6 = "TestOrder-6" + System.currentTimeMillis();
    private static final String TEST_CUSTOMER_7 = "TestUser-7" + System.currentTimeMillis();
    private static final String TEST_ORDER_7 = "TestOrder-7" + System.currentTimeMillis();
    private static final String TEST_CUSTOMER_8 = "TestUser-8" + System.currentTimeMillis();
    private static final String TEST_ORDER_8 = "TestOrder-8" + System.currentTimeMillis();
    private static final String OPTUS_PLAN_01 = "SPCMO-01"+ System.currentTimeMillis();
    private static final String OPTUS_PLAN_02 = "SPCMO-02"+ System.currentTimeMillis();
    private static final int BILLIING_TYPE_MONTHLY = 1;
    UserWS spcTestUserWS;
    private static Integer optusPlanId;
    private static Integer optusPlanId2;
    private static final String PLAN_CREATION_ASSERT = "Plan Creation Failed";
    
    private static final String ASSET01 = "1234117891";
    private static final String ASSET02 = "1034567891";
    private static final String ASSET03 = "1030567891";
    private static final String ASSET04 = "1034067891";
    private static final String ASSET05 = "1034507891";
    private static final String ASSET06 = "1034560891";
    private static final String ASSET07 = "1034567091";
    private static final String ASSET08 = "1034567901";
    
    private static final String SERVICEID01 = "ABC1234";
    private static final String SERVICEID02 = "ABC0234";
    private static final String SERVICEID03 = "ABC0204";
    private static final String SERVICEID04 = "ABC2040";
    private static final String SERVICEID05 = "ABC2004";
    private static final String SERVICEID06 = "ABC2400";
    private static final String SERVICEID07 = "ABC2000";
    private static final String SERVICEID08 = "ABC3000";
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {
    	logger.info("AdHocCreditNoteTest: {}", testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        logger.info("AdHocCreditNoteTest: {}", testBuilder);
        // Optus Plan 1
        String optusPlanDescription = "Optus Budget - $10";
        String planTypeOptus = "Optus";
        String optusPlanServiceType = "Mobile";
        BigDecimal optusPlanPrice = new BigDecimal("9.0909");
        BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024) 200MB
        BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
        Integer optusPlanBoostCount = new Integer("3");
        String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

        logger.debug("************************ Start creating plan");
        optusPlanId = CreatePlanUtility.createPlan(api, OPTUS_PLAN_01, planTypeOptus, optusPlanServiceType,
                optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                optusPlanBoostCount, optusPlanBoostQuantity);
        assertNotNull(PLAN_CREATION_ASSERT, optusPlanId);
        validatePlanUsagePools(optusPlanId,4,"200.0000000000","1024.0000000000");
        logger.info("Optus PlanId: {}", optusPlanId);
        
        // Optus Plan 2
        optusPlanDescription = "NBN Triple Bundle";
        optusPlanPrice = new BigDecimal("21.8182");
        optusPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024) 200MB
        optusPlanBoostQuantity = new BigDecimal("1024");
        optusPlanBoostCount = new Integer("3");
        rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

        optusPlanId2 = CreatePlanUtility.createPlan(api, OPTUS_PLAN_02, planTypeOptus, optusPlanServiceType,
                optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                optusPlanBoostCount, optusPlanBoostQuantity);
        assertNotNull(PLAN_CREATION_ASSERT, optusPlanId);
        logger.info("Optus {} PlanId: {}", OPTUS_PLAN_02, optusPlanId2);
        validatePlanUsagePools(optusPlanId,4,"200.0000000000","1024.0000000000");
        logger.info("Optus {} PlanId2: {}", OPTUS_PLAN_02, optusPlanId2);
    }

    @AfterClass
    public void afterTests() {
    	logger.info("AdHocCreditNoteTest.afterTests");
    }
    
    /**
     * Verify that user is able to create Credit note successfully from API
     * 1. Create customer with NID 1st April
     * 2. Create monthly plan order with active since 1st April 
     * 3. Generate invoice on 1st April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 1st May
     * 7. Create credit note dated 10th April 
     * 8. Generate invoice on 1st May
     * 9. Check credit note should be stored in service summary
     */
    @Test(priority = 1)
    public void testAdHocCreditNote() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 1).getTime();// 1st day of three month back
                Date creditNoteDate = getDate(-4, 10).getTime();// 10th day of three month back
                Date activeSinceDate = nextInvoiceDate;
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate {}", nextInvoiceDate, activeSinceDate, creditNoteDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_1, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);

                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET01,
                        "asset-01",
                        SERVICEID01);

                assetWSs.add(api.getAsset(asset1));

                Integer orderId = createOrderWithAsset(TEST_ORDER_1, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID01);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);
                
                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "6.00", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 1", creditNoteDate, 
                        SERVICEID01, orderId, null);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 1).getTime();// 1st day of two month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                // Generate invoice
                Integer[] invoiceId2 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId2[0], CommonConstants.INVOICE_STATUS_UNPAID);
                
                serviceSummaryList.clear();
                serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId2[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId2);

                creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should be created", creditNotes.length == 1);
                isAdHocCreditNote(creditNotes);
                assertTrue("Ad Hoc credit note id should be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });
        } finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_1));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Verify that user is able to Populate Subscription Order Id using service Id 
     * while creating credit note (Cancellation scenario)
     * 1. Create customer with NID 17th April
     * 2. Create monthly plan order with active since 17th April 
     * 3. Generate invoice on 17th April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 17th May
     * 6. Cancel order on 5th May
     * 7. Create credit note dated 26th April 
     * 8. Check credit note not stored in service summary
     */
    @Test(priority = 2)
    public void testAdHocCreditNoteCancellationOrder() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 17th day of three month back from current
                Date creditNoteDate = getDate(-4, 26).getTime();// 26th day of three month back from current
                Date activeSinceDate = nextInvoiceDate;
                Date activeUntilDate = getDate(-3, 05).getTime();// 5th day of two month back from current
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}, activeUntilDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate, activeUntilDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_2, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);

                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET02,
                        "asset-02",
                        SERVICEID02);

                assetWSs.add(api.getAsset(asset2));

                Integer orderId = createOrderWithAsset(TEST_ORDER_2, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID02);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);
                
                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 17).getTime();// 17th day of three month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                cancelServiceOrder(orderId, activeUntilDate);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "5.00", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 2", creditNoteDate, 
                        SERVICEID02, orderId, null);

                creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should be created", creditNotes.length == 1);
                isAdHocCreditNote(creditNotes);
                assertFalse("Ad Hoc credit note id should not be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });            
        } finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_2));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Verify that user is able to Populate Subscription Order Id 
     * using service Id while creating credit note (Cancellation scenario) 
     * Credit Note date after the cancellation period. (Negative scenario)
     * 1. Create customer with NID 17th April
     * 2. Create monthly plan order with active since 17th April 
     * 3. Generate invoice on 17th April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 17th May
     * 6. Cancel order on 5th May
     * 9. Create credit note dated 6th May 
     * 10. Check error thrown
     */
    @Test(priority = 3, expectedExceptions = SessionInternalError.class,
    	    description = "Subscription Order Id with service Id -")
    public void testAdHocCreditNoteAfterCancellationPeriod() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 17th day of three month back from current
                Date creditNoteDate = getDate(-3, 06).getTime();// 6th day of two month back from current
                Date activeSinceDate = nextInvoiceDate;
                Date activeUntilDate = getDate(-3, 05).getTime();// 5th day of two month back from current
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}, activeUntilDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate, activeUntilDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_3, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);


                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset3 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET03,
                        "asset-03",
                        SERVICEID03);

                assetWSs.add(api.getAsset(asset3));

                Integer orderId = createOrderWithAsset(TEST_ORDER_3, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID03);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);
                
                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 17).getTime();// 17th day of two month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                cancelServiceOrder(orderId, activeUntilDate);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "7.00", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 3", creditNoteDate, 
                        SERVICEID03, null, null);
                assertFalse("Ad Hoc credit note id should not be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });            
        } finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_3));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Verify that user is able to Populate Subscription Order Id using service Id 
     * while creating credit note (Plan Change scenario)
     * 1. Create customer with NID 17th April
     * 2. Create monthly plan order with active since 17th April 
     * 3. Generate invoice on 17th April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 17th May
     * 6. Cancel order on 5th May
     * 7. Remove asset from order 
     * 8. Create another monthly plan order with active since 6th May and same asset
     * 9. Create credit note dated 5th May 
     * 10. Check credit note not stored in service summary
     */
    @Test(priority = 4)
    public void testAdHocCreditNotePlanChange() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 17th day of three month back from current
                Date creditNoteDate = getDate(-3, 05).getTime();// 5th day of two month back from current
                Date activeSinceDate = nextInvoiceDate;
                Date activeUntilDate = getDate(-3, 05).getTime();// 5th day of two month back from current
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}, activeUntilDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate, activeUntilDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_4, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);

                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset4 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET04,
                        "asset-04",
                        SERVICEID04);
                logger.debug("Asset: {}", asset4);
                assetWSs.add(api.getAsset(asset4));

                Integer orderId = createOrderWithAsset(TEST_ORDER_4_1, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID04);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);

                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 17).getTime();// 17th day of two month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                cancelServiceOrder(orderId, activeUntilDate);
                AssetWS assetWS4 = api.getAsset(asset4);
                api.removeAssetFromActiveOrder(assetWS4.getIdentifier());

                Date activeSinceDate2 = getDate(-3, 06).getTime();// 6th day of two month back from current

                planWS = api.getPlanWS(optusPlanId2);
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer orderId2 = createOrderWithAsset(TEST_ORDER_4_2, spcTestUserWS.getId(), activeSinceDate2, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId2, SERVICEID04);
                logger.debug("OrderId2: {}", orderId2);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "8.00", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 4", creditNoteDate, 
                        SERVICEID04, null, null);

                creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should be created", creditNotes.length == 1);
                CreditNoteWS creditNote = api.getCreditNote(creditNotes[0]);
                logger.debug("CreditNote: {} subscription order id: {} service id {} credit note date {}", 
                        creditNote.getId(), creditNote.getSubscriptionOrderId(), creditNote.getServiceId(), creditNote.getCreditNoteDate());
                assertFalse("Ad Hoc credit note subscription order id is not correct", creditNote.getSubscriptionOrderId() == orderId);
                isAdHocCreditNote(creditNotes);
                assertFalse("Ad Hoc credit note id should not be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });            
        } finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_4));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Verify that user is able to Populate Subscription Order Id using service Id 
     * while creating credit note (Plan Change scenario)
     * 1. Create customer with NID 17th April
     * 2. Create monthly plan order with active since 17th April 
     * 3. Generate invoice on 17th April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 17th May
     * 6. Cancel order on 5th May
     * 7. Remove asset from order 
     * 8. Create another monthly plan order with active since 6th May and same asset
     * 9. Create credit note dated 7th May 
     * 10. Check credit note not stored in service summary
     */
    @Test(priority = 5)
    public void testAdHocCreditNotePlanChange2() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 6th day of three month back from current
                Date creditNoteDate = getDate(-3, 07).getTime();// 6th day of two month back from current
                Date activeSinceDate = nextInvoiceDate;
                Date activeUntilDate = getDate(-3, 05).getTime();// 6th day of two month back from current
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}, activeUntilDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate, activeUntilDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_5, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);

                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset5 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET05,
                        "asset-05",
                        SERVICEID05);
                logger.debug("Asset: {}", asset5);
                assetWSs.add(api.getAsset(asset5));

                Integer orderId = createOrderWithAsset(TEST_ORDER_5_1, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID05);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);

                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 17).getTime();// 17th day of two month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                cancelServiceOrder(orderId, activeUntilDate);
                AssetWS assetWS5 = api.getAsset(asset5);
                api.removeAssetFromActiveOrder(assetWS5.getIdentifier());

                Date activeSinceDate2 = getDate(-3, 06).getTime();// 6th day of two month back from current

                planWS = api.getPlanWS(optusPlanId2);
                productQuantityMap.clear();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);

                Integer orderId2 = createOrderWithAsset(TEST_ORDER_5_2, spcTestUserWS.getId(), activeSinceDate2, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId2, SERVICEID05);
                logger.debug("OrderId2: {}", orderId2);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "8.50", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 4", creditNoteDate, 
                        SERVICEID05, null, null);

                creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should be created", creditNotes.length == 1);
                CreditNoteWS creditNote = api.getCreditNote(creditNotes[0]);
                logger.debug("CreditNote: {} subscription order id: {} service id {} credit note date {}", 
                        creditNote.getId(), creditNote.getSubscriptionOrderId(), creditNote.getServiceId(), creditNote.getCreditNoteDate());
                assertFalse("Ad Hoc credit note subscription order id is not correct", creditNote.getSubscriptionOrderId() == orderId);
                isAdHocCreditNote(creditNotes);
                assertFalse("Ad Hoc credit note id should not be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });
        } finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_5));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Verify that user created credit note with 
     * the amount equal to the amount of all total charges
     * 1. Create customer with NID 17th April
     * 2. Create order with active since 17th April 
     * 3. Generate invoice on 17th April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 17th May
     * 6. Cancel order on 5th May 
     * 7. Create credit note dated 28th April with eqaul amount of charges 
     * 8. Check credit note not stored in service summary
     * 9. Check user balance should be zero
     */
    @Test(priority = 6)
    public void testAdHocCreditNoteEqualToInvoiceBalance() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 17th day of three month back from current
                Date creditNoteDate = getDate(-4, 28).getTime();// 28th day of three month back from current
                Date activeSinceDate = nextInvoiceDate;
                Date activeUntilDate = getDate(-3, 05).getTime();// 5th day of two month back from current
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}, activeUntilDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate, activeUntilDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_6, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);


                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset6 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET06,
                        "asset-06",
                        SERVICEID06);

                assetWSs.add(api.getAsset(asset6));

                Integer orderId = createOrderWithAsset(TEST_ORDER_6, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID06);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);

                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 17).getTime();// 17th day of two month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                cancelServiceOrder(orderId, activeUntilDate);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "9.09", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 6", creditNoteDate, 
                        SERVICEID06, orderId, null);

                creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should be created", creditNotes.length == 1);
                isAdHocCreditNote(creditNotes);
                assertFalse("Ad Hoc credit note id should not be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });
        } finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_6));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Verify that user created credit note with 
     * the amount more than amount of all charges
     * 1. Create customer with NID 17th April
     * 2. Create order with active since 17th April 
     * 3. Generate invoice on 17th April
     * 4. Check credit note id not stored in service summary
     * 5. Update customer NID to 17th May 
     * 6. Create credit note dated 28th April with amount more than amount of all charges 
     * 7. Check user balance should be negative
     */
    @Test(priority = 7)
    public void testAdHocCreditNoteGreaterThanInvoiceBalance() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 17th day of three month back from current
                Date creditNoteDate = getDate(-4, 28).getTime();// 28th day of three month back from current
                Date activeSinceDate = nextInvoiceDate;
                Date activeUntilDate = getDate(-3, 05).getTime();// 5th day of two month back from current
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}, activeUntilDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate, activeUntilDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_7, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);


                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset7 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET07,
                        "asset-07",
                        SERVICEID07);

                assetWSs.add(api.getAsset(asset7));

                Integer orderId = createOrderWithAsset(TEST_ORDER_7, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID07);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);

                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 0);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);

                // Update customer NID
                nextInvoiceDate = getDate(-3, 17).getTime();// 17th day of two month back from current
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDate);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                logger.debug("Update customer NID: {}", nextInvoiceDate);

                cancelServiceOrder(orderId, activeUntilDate);

                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "100", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 7", creditNoteDate, 
                        SERVICEID07, orderId, null);

                creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should be created", creditNotes.length == 1);
                isAdHocCreditNote(creditNotes);
                assertFalse("Ad Hoc credit note id should not be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                BigDecimal userBalance = spcTestUserWS.getOwingBalanceAsDecimal();
                logger.debug("UserBalance: {}", userBalance);
                assertNotNull(userBalance);
                assertTrue("User Balance should have been negetive", BigDecimal.ZERO.compareTo(userBalance) > 0);
            });
        }finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_7));
            spcTestUserWS = null;
        }
    }
    
    /**
     * Credit note before order and invoice creation
     * 1. Create customer with NID 17th April
     * 2. Create credit note dated 28th March 
     * without service id and subscription order id
     * 3. Create order with active since 17th April 
     * 4. Check credit note id stored in service summary
     * 
     */
    @Test(priority = 8)
    public void testAdHocCreditNoteBeforeOrderAndInvoiceCreation() {
        try{
            testBuilder
            .given(envBuilder -> {
                Date nextInvoiceDate = getDate(-4, 17).getTime();// 17th day of three month back from current
                Date creditNoteDate = getDate(-5, 28).getTime();// 28th day of three month back from current
                Date activeSinceDate = nextInvoiceDate;
                
                logger.debug("next Invoice Date: {}, activeSinceDate: {}, creditNoteDate: {}", 
                		nextInvoiceDate, activeSinceDate, creditNoteDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER_8, 
                        nextInvoiceDate,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);
                // Ad Hoc Credit Note before order and invoice creation, without service id
                buildAndPersistAdHocCreditNote(spcTestUserWS.getId(),CreditType.USER_GENERATED.name(), "6.5", 
                        api.getCallerCompanyId(), Integer.valueOf("0"), creditNoteDate, 
                        api.getItemID(CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT), "Test AdHoc Credit Note 8", creditNoteDate, 
                        null, null, null);
                
                Integer[] creditNotes = api.getLastCreditNotes(spcTestUserWS.getId(),Integer.valueOf(1));
                logger.debug("CreditNote: {}", creditNotes.length);
                assertTrue("Ad Hoc credit note should not be created", creditNotes.length == 1);
                
                isAdHocCreditNote(creditNotes);
                
                //optus
                PlanWS planWS = api.getPlanWS(optusPlanId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();

                Integer asset8 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        ASSET08,
                        "asset-08",
                        SERVICEID08);

                assetWSs.add(api.getAsset(asset8));

                Integer orderId = createOrderWithAsset(TEST_ORDER_8, spcTestUserWS.getId(), activeSinceDate, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId, SERVICEID08);
                logger.debug("OrderId: {}", orderId);
                // Generate invoice
                Integer[] invoiceId = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDate, null, null, true);
                checkInvoiceAndStatus(invoiceId[0], CommonConstants.INVOICE_STATUS_UNPAID);

                List<ServiceSummaryWS> serviceSummaryList = getServiceSummaryByInvoiceId(invoiceId[0]);
                logger.debug("service summary {} for invoice : {}", serviceSummaryList, invoiceId);
                
                assertTrue("Ad Hoc credit note id should be present in service summary table", isAdHocCreditNotePresentInServiceSummary(serviceSummaryList, creditNotes));
            });
        }finally {
            clearTestDataForUser(api.getUserId(TEST_CUSTOMER_8));
            spcTestUserWS = null;
        }
    }
    
    protected Integer buildAndPersistAdHocCreditNote(Integer userId, String type, String amount, 
    	    Integer entityId, int deleted, Date createDateTime, 
    	    Integer itemId, String description, Date creditNoteDate, 
    	    String serviceId, Integer subscriptionOrderId, String notes) {

        CreditNoteWS creditNoteWS = new CreditNoteWS();
        creditNoteWS.setUserId(userId);
        creditNoteWS.setType(type);
        creditNoteWS.setAmount(amount);
        creditNoteWS.setEntityId(entityId);
        creditNoteWS.setDeleted(deleted);
        creditNoteWS.setCreateDateTime(createDateTime);
        creditNoteWS.setItemId(itemId);
        creditNoteWS.setDescription(description);
        creditNoteWS.setCreditNoteDate(creditNoteDate);
        creditNoteWS.setServiceId(serviceId);
        creditNoteWS.setNotes(notes);
        // AdHocCreditNote
        creditNoteWS.setCreationInvoiceId(null);

        return api.createAdhocCreditNote(creditNoteWS);
    }
    
    protected void isAdHocCreditNote(Integer creditNotes[]) {
        for (Integer creditNoteId : creditNotes) {
            CreditNoteWS creditNote = api.getCreditNote(creditNoteId);
            assertNull("Creation invoice id should be null.", creditNote.getCreationInvoiceId());
        }
    }
    
    protected boolean isAdHocCreditNotePresentInServiceSummary(List<ServiceSummaryWS> serviceSummaryList, Integer creditNotes[]) {
        boolean isAdHocCreditNotePresent = false;
        for (ServiceSummaryWS serviceSummary : serviceSummaryList) {
            for (Integer creditNoteId : creditNotes) {
                CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteId);
                if (null != serviceSummary &&
                        null != serviceSummary.getCreditNoteId() && 
                        serviceSummary.getCreditNoteId().equals(creditNoteWS.getId())) {
                    isAdHocCreditNotePresent = true;    			
                }
            }
        }
        logger.debug("isAdHocCreditNotePresent: {}", isAdHocCreditNotePresent);
        return isAdHocCreditNotePresent;
    }
}