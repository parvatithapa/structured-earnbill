package com.sapienter.jbilling.server.spc;

import org.testng.annotations.Test;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;


import com.sapienter.jbilling.resources.CancelOrderInfo;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;


import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.util.Constants;


import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;

@Test(groups = "agl", testName = "agl.ProRatePeriodCalculatorTest")
public class ProRatePeriodCalculatorTest extends SPCBaseConfiguration{

    private static final int BILLIING_TYPE_MONTHLY = 1;
    private static final int BILLIING_TYPE_MONTHLY_POST_PAID = 2;
    
    //1 user
    private static final String TEST_CUSTOMER1 = "Test1-1102";
    private static final String optusPlan = "SPCMO-1102";
    //2 user
    private static final String TEST_CUSTOMER2 = "Test2-1102";
    //3 user
    private static final String TEST_CUSTOMER3 = "Test3-1102";
  //3 user
    private static final String TEST_CUSTOMER4 = "Test4-1102";
    //5 user
    private static final String TEST_CUSTOMER5 = "Test5-1102";
    //6 user
    private static final String TEST_CUSTOMER6 = "Test6-1102";
  //7 user
    private static final String TEST_CUSTOMER7 = "Test7-1102";
    
    private static final String ORDER_ASSERT = "Order Should not null";
    private static final String USER_ASSERT = "User Created {}";
    private static final String USER_CREATION_ASSERT = "User Creation Failed";
    private static final String PLAN_CREATION_ASSERT = "Plan Creation Failed";
    private static final String ORDER_CREATION_ASSERT = "Order Creation Failed";
    
    Integer orderId;
    Integer orderId1;
    Integer userId ;
    OrderWS orderWs;
    
    List<AssetWS> assetWSs = new ArrayList<>();
    List<AssetWS> assetWSs1 = new ArrayList<>();
    UserWS spcTestUserWS;
    Integer optusPlanId;
    String optusPlanDescription = "Optus Budget - $10";
    
    String assetIdentifier1 = "04"+randomLong(10000000L, 99999999L);
    String assetIdentifier2 = "02"+randomLong(10000000L, 99999999L);
    String assetIdentifier3 = "03"+randomLong(10000000L, 99999999L);
    String assetIdentifier4 = "05"+randomLong(10000000L, 99999999L);
    String assetIdentifier5 = "06"+randomLong(10000000L, 99999999L);
    String assetIdentifier6 = "07"+randomLong(10000000L, 99999999L);
    String assetIdentifier7 = "08"+randomLong(10000000L, 99999999L);
    String assetIdentifier8 = "09"+randomLong(10000000L, 99999999L);
    String assetIdentifier9 = "08"+randomLong(10000000L, 99999999L);
    String assetIdentifier10 = "09"+randomLong(10000000L, 99999999L);
    String assetIdentifier11 = "08"+randomLong(10000000L, 99999999L);
    String assetIdentifier12 = "09"+randomLong(10000000L, 99999999L);
    String assetIdentifier13 = "08"+randomLong(10000000L, 99999999L);
    String assetIdentifier14 = "09"+randomLong(10000000L, 99999999L);
    String assetIdentifier15 = "08"+randomLong(10000000L, 99999999L);
    String assetIdentifier16 = "09"+randomLong(10000000L, 99999999L);
    String assetIdentifier17 = "08"+randomLong(10000000L, 99999999L);
    String assetIdentifier18 = "09"+randomLong(10000000L, 99999999L);
    
    @BeforeClass
    public void beforeClass () {

        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        testBuilder.given(envBuilder -> {
            logger.debug("JMRPostProstProcessorTest.beforeClass : {} "+testBuilder);

            // Creating PLAN: SPCMO-01 

            String planTypeOptus = "Optus";
            String optusPlanServiceType = "Mobile";
            BigDecimal optusPlanPrice = new BigDecimal("9.0909");
            BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024) 200 MB
            BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
            Integer optusPlanBoostCount = new Integer("3");

            String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_','-');

            Map<String, String> optusPlanMetaFieldCodeMap = new HashMap<>();
            optusPlanMetaFieldCodeMap.put("USAGE_POOL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("COST_GL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("REVENUE_GL_CODE", "410026-150");

           logger.debug("************************ Start creating plan");
           optusPlanId = CreatePlanUtility.createPlan(api, optusPlan, planTypeOptus, optusPlanServiceType,
                   optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                   optusPlanBoostCount, optusPlanBoostQuantity);
           assertNotNull(PLAN_CREATION_ASSERT, optusPlanId);
           logger.info("Optus PlanId: {}", optusPlanId);
        });
    }
    
    /**
     * 
     * Create a customer with NID - 1st Oct 2020
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 1st Oct 2020
     * Invoice generated for 1st Oct 2020
     * Invoice generated for 1st Nov 2020
     * Invoice generated for 1st Dec 2020
     */
    @Test(priority = 2)
    public void testPeriodForPrepaidOrderwithFirstMonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2020, 10, 1);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        testBuilder.given(envBuilder -> {
              
              spcTestUserWS = getSPCTestUserWS(
                      envBuilder,
                      TEST_CUSTOMER1, 
                      nextInvoiceDateNew,
                      "", 
                      CUSTOMER_TYPE_VALUE_PRE_PAID,
                      AUSTRALIA_POST,CC);
                
              // Update customer NID
              logger.debug("Update customer NID: {}", nextInvoiceDate);
              spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
              api.updateUser(spcTestUserWS);
              spcTestUserWS = api.getUserWS(spcTestUserWS.getId());

             //optus
             PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
             Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
             productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
             List<AssetWS> assetWSs = new ArrayList<>();
             List<AssetWS> assetWSs1 = new ArrayList<>();

             Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "assetIdentifier1",
                        "asset-01", assetIdentifier1);
             
             Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "assetIdentifier2",
                        "asset-02", assetIdentifier2);

                 assetWSs.add(api.getAsset(asset1));
                 assetWSs1.add(api.getAsset(asset2));

                 orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                 
                 orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);
                 
                 //Bill run process
                 Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                 
              // create invoice:
                 Integer[] invoicess = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess)));
                 logger.debug("Plan Order invoice created {}", invoicess[0]);
                 InvoiceWS invoiceWS = api.getInvoiceWS(invoicess[0]);
                 
                 LocalDate nextInvoiceDate1 = LocalDate.of(2020, 11, 1);  
                 Date nextInvoiceDateNew1 = Date.from(nextInvoiceDate1.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew1);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 Date nid = Date.from(LocalDate.of(2020, 11, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nid,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList = new ArrayList<String>();
                 BigDecimal invoiceLineAmt = new BigDecimal(0);
                 Integer number = 1;
                 Integer[] invoiceWs = api.getLastInvoices(spcTestUserWS.getId(), number);
                 InvoiceWS invoices = api.getInvoiceWS(invoiceWs[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt = invoiceLineAmt.add(new BigDecimal(ildto.getAmount()));
                         descriptionList.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList.contains(optusPlanDescription +" Period from 10/01/2020 to 10/31/2020"));
                 String totalAmt = "15.0000000000" , balanceAmt = "15.0000000000"; BigDecimal invoiceLine = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoices, invoiceLineAmt, totalAmt, balanceAmt, invoiceLine);
                 
                 Date runDate1 = DateUtils.addMonths(runDate, 1);
                 logger.debug("Update customer NID1: {}", runDate1);
                 
                 Date runDate11 = com.sapienter.jbilling.server.util.Util.getStartOfDay(runDate1);
                 
                 // create invoice:
                 Integer[] invoicess1 = api.createInvoiceWithDate(spcTestUserWS.getId(), runDate11, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess1)));
                 logger.debug("Plan Order invoice created {}", invoicess1[0]);
                 InvoiceWS invoiceWS1 = api.getInvoiceWS(invoicess1[0]);
                 
                 LocalDate nextInvoiceDate2 = LocalDate.of(2020, 12, 1);  
                 Date nextInvoiceDateNew2 = Date.from(nextInvoiceDate2.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew2);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   Date nid1 = Date.from(LocalDate.of(2020, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   assertEquals("Customer NID should be match", nid1,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                 
                   List<String> descriptionList1 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                 Integer number1 = 1;
                 Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS.getId(), number1);
                 InvoiceWS invoices1 = api.getInvoiceWS(invoiceWs1[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices1.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                         descriptionList1.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList1.contains(optusPlanDescription +" Period from 10/01/2020 to 10/31/2020"));
                 assertTrue( descriptionList1.contains(optusPlanDescription +" Period from 11/01/2020 to 11/30/2020"));
                 String totalAmt1 = "45.0000000000" , balanceAmt1 = "30.0000000000"; BigDecimal invoiceLine1 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices1, invoiceLineAmt1, totalAmt1, balanceAmt1, invoiceLine1);
                 
                 Date runDate2 = DateUtils.addMonths(runDate1, 1);               
                 logger.debug("Update customer NID2: {}", runDate2);
                 
                 Date runDate22 = com.sapienter.jbilling.server.util.Util.getStartOfDay(runDate2);
                 
              // create invoice:
                 Integer[] invoicess2 = api.createInvoiceWithDate(spcTestUserWS.getId(), runDate22, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess2)));
                 logger.debug("Plan Order invoice created {}", invoicess2[0]);
                 InvoiceWS invoiceWS2 = api.getInvoiceWS(invoicess2[0]);
                 
                 LocalDate nextInvoiceDate3 = LocalDate.of(2021, 01, 1);  
                 Date nextInvoiceDateNew3 = Date.from(nextInvoiceDate3.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew3);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 assertEquals("Customer NID should be match", nextInvoiceDateNew3,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nextInvoiceDateNew3,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList2 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt2 = new BigDecimal(0);
                 Integer number2 = 1;
                 Integer[] invoiceWs2 = api.getLastInvoices(spcTestUserWS.getId(), number2);
                 InvoiceWS invoices2 = api.getInvoiceWS(invoiceWs2[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices2.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt2 = invoiceLineAmt2.add(new BigDecimal(ildto.getAmount()));
                         descriptionList2.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 11/01/2020 to 11/30/2020"));
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 12/01/2020 to 12/31/2020"));
                 String totalAmt2 = "75.0000000000" , balanceAmt2 = "30.0000000000"; BigDecimal invoiceLine2 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices2, invoiceLineAmt2, totalAmt2, balanceAmt2, invoiceLine2);
                 
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    
    /**
     * 
     * Create a customer with NID - 15th Oct 2020
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 15th Oct 2020
     * Invoice generated for 15th Oct 2020
     * Invoice generated for 15th Nov 2020
     * Invoice generated for 15th Dec 2020
     */
    @Test(priority = 3)
    public void testPeriodForPrepaidOrderwithFifteenMonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2020, 10, 15);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        testBuilder.given(envBuilder -> {
              
              spcTestUserWS = getSPCTestUserWS(
                      envBuilder,
                      TEST_CUSTOMER2, 
                      nextInvoiceDateNew,
                      "", 
                      CUSTOMER_TYPE_VALUE_PRE_PAID,
                      AUSTRALIA_POST,CC);
                
              // Update customer NID
              logger.debug("Update customer NID: {}", nextInvoiceDate);
              spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
              api.updateUser(spcTestUserWS);
              spcTestUserWS = api.getUserWS(spcTestUserWS.getId());

             //optus
             PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
             Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
             productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
             List<AssetWS> assetWSs = new ArrayList<>();
             List<AssetWS> assetWSs1 = new ArrayList<>();

             Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "assetIdentifier3",
                        "asset-03", assetIdentifier3);
             
             Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "assetIdentifier4",
                        "asset-04", assetIdentifier4);

                 assetWSs.add(api.getAsset(asset1));
                 assetWSs1.add(api.getAsset(asset2));

                 orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                 
                 orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);
                 
                 //Bill run process
                 Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                 
              // create invoice:
                 Integer[] invoicess = api.createInvoiceWithDate(spcTestUserWS.getId(), runDate, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess)));
                 logger.debug("Plan Order invoice created {}", invoicess[0]);
                 InvoiceWS invoiceWS = api.getInvoiceWS(invoicess[0]);
                 
                 LocalDate nextInvoiceDate1 = LocalDate.of(2020, 11, 15);  
                 Date nextInvoiceDateNew1 = Date.from(nextInvoiceDate1.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew1);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 Date nid = Date.from(LocalDate.of(2020, 11, 15).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nid,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList = new ArrayList<String>();
                 BigDecimal invoiceLineAmt = new BigDecimal(0);
                 Integer number = 1;
                 Integer[] invoiceWs = api.getLastInvoices(spcTestUserWS.getId(), number);
                 InvoiceWS invoices = api.getInvoiceWS(invoiceWs[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt = invoiceLineAmt.add(new BigDecimal(ildto.getAmount()));
                         descriptionList.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList.contains(optusPlanDescription+" Period from 10/15/2020 to 11/14/2020"));
                 String totalAmt = "15.0000000000" , balanceAmt = "15.0000000000"; BigDecimal invoiceLine = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoices, invoiceLineAmt, totalAmt, balanceAmt, invoiceLine);
                 
                 Date runDate1 = DateUtils.addMonths(runDate, 1);
                 logger.debug("Update customer NID1: {}", runDate1);
                 
                 Date runDate11 = com.sapienter.jbilling.server.util.Util.getStartOfDay(runDate1);
                 
                 // create invoice:
                 Integer[] invoicess1 = api.createInvoiceWithDate(spcTestUserWS.getId(), runDate11, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess1)));
                 logger.debug("Plan Order invoice created {}", invoicess1[0]);
                 InvoiceWS invoiceWS1 = api.getInvoiceWS(invoicess1[0]);
                 
                 LocalDate nextInvoiceDate2 = LocalDate.of(2020, 12, 15);  
                 Date nextInvoiceDateNew2 = Date.from(nextInvoiceDate2.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew2);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   Date nid1 = Date.from(LocalDate.of(2020, 12, 15).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   assertEquals("Customer NID should be match", nid1,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList1 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                 Integer number1 = 1;
                 Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS.getId(), number1);
                 InvoiceWS invoices1 = api.getInvoiceWS(invoiceWs1[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices1.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                         descriptionList1.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 10/15/2020 to 11/14/2020"));
                 assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 11/15/2020 to 12/14/2020"));
                 String totalAmt1 = "45.0000000000" , balanceAmt1 = "30.0000000000"; BigDecimal invoiceLine1 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices1, invoiceLineAmt1, totalAmt1, balanceAmt1, invoiceLine1);
                 
                 Date runDate2 = DateUtils.addMonths(runDate1, 1);               
                 logger.debug("Update customer NID2: {}", runDate2);
                 
                 Date runDate22 = com.sapienter.jbilling.server.util.Util.getStartOfDay(runDate2);
                 
              // create invoice:
                 Integer[] invoicess2 = api.createInvoiceWithDate(spcTestUserWS.getId(), runDate22, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess2)));
                 logger.debug("Plan Order invoice created {}", invoicess2[0]);
                 InvoiceWS invoiceWS2 = api.getInvoiceWS(invoicess2[0]);
                 
                 LocalDate nextInvoiceDate3 = LocalDate.of(2021, 01, 15);  
                 Date nextInvoiceDateNew3 = Date.from(nextInvoiceDate3.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew3);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 assertEquals("Customer NID should be match", nextInvoiceDateNew3,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nextInvoiceDateNew3,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList2 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt2 = new BigDecimal(0);
                 Integer number2 = 1;
                 Integer[] invoiceWs2 = api.getLastInvoices(spcTestUserWS.getId(), number2);
                 InvoiceWS invoices2 = api.getInvoiceWS(invoiceWs2[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices2.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt2 = invoiceLineAmt2.add(new BigDecimal(ildto.getAmount()));
                         descriptionList2.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 11/15/2020 to 12/14/2020"));
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 12/15/2020 to 01/14/2021"));
                 String totalAmt2 = "75.0000000000" , balanceAmt2 = "30.0000000000"; BigDecimal invoiceLine2 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices2, invoiceLineAmt2, totalAmt2, balanceAmt2, invoiceLine2);
                 
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    
    /**
     * 
     * Create a customer with NID - 28th Oct 2020
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 28th Oct 2020
     * Invoice generated for 28th Oct 2020
     * Invoice generated for 28th Nov 2020
     * Invoice generated for 28th Dec 2020
     */
    @Test(priority = 4)
    public void testPeriodForPrepaidOrderwithTwentyEightMonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2020, 10, 28);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        testBuilder.given(envBuilder -> {
              
              spcTestUserWS = getSPCTestUserWS(
                      envBuilder,
                      TEST_CUSTOMER3, 
                      nextInvoiceDateNew,
                      "", 
                      CUSTOMER_TYPE_VALUE_PRE_PAID,
                      AUSTRALIA_POST,CC);
                
              // Update customer NID
              logger.debug("Update customer NID: {}", nextInvoiceDate);
              spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
              api.updateUser(spcTestUserWS);
              spcTestUserWS = api.getUserWS(spcTestUserWS.getId());

             //optus
             PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
             Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
             productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
             List<AssetWS> assetWSs = new ArrayList<>();
             List<AssetWS> assetWSs1 = new ArrayList<>();

             Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "assetIdentifier5",
                        "asset-05", assetIdentifier5);
             
             Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "assetIdentifier6",
                        "asset-06", assetIdentifier6);

                 assetWSs.add(api.getAsset(asset1));
                 assetWSs1.add(api.getAsset(asset2));

                 orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                 
                 orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);
                 
              // create invoice:
                 Integer[] invoicess = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess)));
                 logger.debug("Plan Order invoice created {}", invoicess[0]);
                 InvoiceWS invoiceWS = api.getInvoiceWS(invoicess[0]);
                 
                 LocalDate nextInvoiceDate1 = LocalDate.of(2020, 11, 28);  
                 Date nextInvoiceDateNew1 = Date.from(nextInvoiceDate1.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew1);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 Date nid = Date.from(LocalDate.of(2020, 11, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nid,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList = new ArrayList<String>();
                 BigDecimal invoiceLineAmt = new BigDecimal(0);
                 Integer number = 1;
                 Integer[] invoiceWs = api.getLastInvoices(spcTestUserWS.getId(), number);
                 InvoiceWS invoices = api.getInvoiceWS(invoiceWs[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt = invoiceLineAmt.add(new BigDecimal(ildto.getAmount()));
                         descriptionList.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList.contains(optusPlanDescription+" Period from 10/28/2020 to 11/27/2020"));
                 String totalAmt = "15.0000000000" , balanceAmt = "15.0000000000"; BigDecimal invoiceLine = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoices, invoiceLineAmt, totalAmt, balanceAmt, invoiceLine);

                 // create invoice:
                 Integer[] invoicess1 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew1, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess1)));
                 logger.debug("Plan Order invoice created {}", invoicess1[0]);
                 InvoiceWS invoiceWS1 = api.getInvoiceWS(invoicess1[0]);
                 
                 LocalDate nextInvoiceDate2 = LocalDate.of(2020, 12, 28);  
                 Date nextInvoiceDateNew2 = Date.from(nextInvoiceDate2.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew2);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   Date nid1 = Date.from(LocalDate.of(2020, 12, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   assertEquals("Customer NID should be match", nid1,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList1 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                 Integer number1 = 1;
                 Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS.getId(), number1);
                 InvoiceWS invoices1 = api.getInvoiceWS(invoiceWs1[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices1.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                         descriptionList1.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 10/28/2020 to 11/27/2020"));
                 assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 11/28/2020 to 12/27/2020"));
                 String totalAmt1 = "45.0000000000" , balanceAmt1 = "30.0000000000"; BigDecimal invoiceLine1 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices1, invoiceLineAmt1, totalAmt1, balanceAmt1, invoiceLine1);
                
              // create invoice:
                 Integer[] invoicess2 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew2, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess2)));
                 logger.debug("Plan Order invoice created {}", invoicess2[0]);
                 InvoiceWS invoiceWS2 = api.getInvoiceWS(invoicess2[0]);
                 
                 LocalDate nextInvoiceDate3 = LocalDate.of(2021, 01, 28);  
                 Date nextInvoiceDateNew3 = Date.from(nextInvoiceDate3.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew3);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 //Date nid2 = Date.from(LocalDate.of(2021, 01, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nextInvoiceDateNew3,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nextInvoiceDateNew3,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList2 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt2 = new BigDecimal(0);
                 Integer number2 = 1;
                 Integer[] invoiceWs2 = api.getLastInvoices(spcTestUserWS.getId(), number2);
                 InvoiceWS invoices2 = api.getInvoiceWS(invoiceWs2[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices2.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt2 = invoiceLineAmt2.add(new BigDecimal(ildto.getAmount()));
                         descriptionList2.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 11/28/2020 to 12/27/2020"));
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 12/28/2020 to 01/27/2021"));
                 String totalAmt2 = "75.0000000000" , balanceAmt2 = "30.0000000000"; BigDecimal invoiceLine2 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices2, invoiceLineAmt2, totalAmt2, balanceAmt2, invoiceLine2);
              // create invoice:
                 Integer[] invoicess3 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew3, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess2)));
                 logger.debug("Plan Order invoice created {}", invoicess3[0]);
                 InvoiceWS invoiceWS3 = api.getInvoiceWS(invoicess3[0]);
                 
                 LocalDate nextInvoiceDate4 = LocalDate.of(2021, 02, 28);  
                 Date nextInvoiceDateNew4 = Date.from(nextInvoiceDate4.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew4);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 //Date nid2 = Date.from(LocalDate.of(2021, 01, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nextInvoiceDateNew4,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nextInvoiceDateNew4,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList3 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt3 = new BigDecimal(0);
                 Integer number3 = 1;
                 Integer[] invoiceWs3 = api.getLastInvoices(spcTestUserWS.getId(), number3);
                 InvoiceWS invoices3 = api.getInvoiceWS(invoiceWs3[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices3.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt3 = invoiceLineAmt3.add(new BigDecimal(ildto.getAmount()));
                         descriptionList3.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList3.contains(optusPlanDescription+" Period from 12/28/2020 to 01/27/2021"));
                 assertTrue( descriptionList3.contains(optusPlanDescription+" Period from 01/28/2021 to 02/27/2021"));
                 String totalAmt3 = "105.0000000000" , balanceAmt3 = "30.0000000000"; BigDecimal invoiceLine3 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices3, invoiceLineAmt3, totalAmt3, balanceAmt3, invoiceLine3);
                 
              // create invoice:
                 Integer[] invoicess4 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew4, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess4)));
                 logger.debug("Plan Order invoice created {}", invoicess4[0]);
                 InvoiceWS invoiceWS4 = api.getInvoiceWS(invoicess4[0]);
                 
                 LocalDate nextInvoiceDate5 = LocalDate.of(2021, 03, 28);  
                 Date nextInvoiceDateNew5 = Date.from(nextInvoiceDate5.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID 1st March 2021
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew5);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 assertEquals("Customer NID should be match", nextInvoiceDateNew5,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nextInvoiceDateNew5,orderWs.getNextBillableDay());
                 
                 List<String> descriptionList4 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt4 = new BigDecimal(0);
                 Integer number4 = 1;
                 Integer[] invoiceWs4 = api.getLastInvoices(spcTestUserWS.getId(), number4);
                 InvoiceWS invoices4 = api.getInvoiceWS(invoiceWs4[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices4.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt4 = invoiceLineAmt4.add(new BigDecimal(ildto.getAmount()));
                         descriptionList4.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList4.contains(optusPlanDescription+" Period from 01/28/2021 to 02/27/2021"));
                 assertTrue( descriptionList4.contains(optusPlanDescription+" Period from 02/28/2021 to 03/27/2021"));
                 String totalAmt4 = "135.0000000000" , balanceAmt4 = "30.0000000000"; BigDecimal invoiceLine4 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoices4, invoiceLineAmt4, totalAmt4, balanceAmt4, invoiceLine4);
                 
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    /**
     * 
     * Create a customer with NID - 28th , 29th, 30th, 31st Oct 2020 (Included 28,29,30,31 Monthly cycles)
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 28 Oct 2020
     * Invoice generated for 28,29,30,31 Oct 2020
     * Invoice generated for 28,29,30 Nov 2020
     * Invoice generated for 28,29,30,31 Dec 2020
     * Invoice generated for 28,29,30,31 Jan 2020
     * Invoice generated for 28 Feb 2020
     * Invoice generated for 28,29,30,31 March 2020
     * Invoice generated for 28,29,30 April 2020
     */
    @Test(priority = 6)
    public void testPeriodForPrepaidOrderwithJanToFebMonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2020, 10, 28);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        testBuilder.given(envBuilder -> {
              
              spcTestUserWS = getSPCTestUserWS(
                      envBuilder,
                      "test-1102-1", 
                      nextInvoiceDateNew,
                      "", 
                      CUSTOMER_TYPE_VALUE_PRE_PAID,
                      AUSTRALIA_POST,CC);
                
              // Update customer NID
              logger.debug("Update customer NID: {}", nextInvoiceDate);
              spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
              api.updateUser(spcTestUserWS);
              spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
              
              LocalDate nextInvoiceDate1 = LocalDate.of(2020, 10, 29);  
              Date nextInvoiceDateNew1 = Date.from(nextInvoiceDate1.atStartOfDay(ZoneId.systemDefault()).toInstant());
              UserWS spcTestUserWS1 = getSPCTestUserWS(
                        envBuilder,
                        "test-1102-2", 
                        nextInvoiceDateNew1,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);
                  
                // Update customer NID
                logger.debug("Update customer NID: {}", nextInvoiceDate1);
                spcTestUserWS1.setNextInvoiceDate(nextInvoiceDateNew1);
                api.updateUser(spcTestUserWS1);
                spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                
                LocalDate nextInvoiceDate2 = LocalDate.of(2020, 10, 30);  
                Date nextInvoiceDateNew2 = Date.from(nextInvoiceDate2.atStartOfDay(ZoneId.systemDefault()).toInstant());
                UserWS spcTestUserWS2 = getSPCTestUserWS(
                          envBuilder,
                          "test-1102-3", 
                          nextInvoiceDateNew2,
                          "", 
                          CUSTOMER_TYPE_VALUE_PRE_PAID,
                          AUSTRALIA_POST,CC);
                    
                  // Update customer NID
                  logger.debug("Update customer NID: {}", nextInvoiceDate2);
                  spcTestUserWS2.setNextInvoiceDate(nextInvoiceDateNew2);
                  api.updateUser(spcTestUserWS2);
                  spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());

                  LocalDate nextInvoiceDate3 = LocalDate.of(2020, 10, 31);  
                  Date nextInvoiceDateNew3 = Date.from(nextInvoiceDate3.atStartOfDay(ZoneId.systemDefault()).toInstant());
                  UserWS spcTestUserWS3 = getSPCTestUserWS(
                            envBuilder,
                            "test-1102-4", 
                            nextInvoiceDateNew3,
                            "", 
                            CUSTOMER_TYPE_VALUE_PRE_PAID,
                            AUSTRALIA_POST,CC);
                      
                    // Update customer NID
                    logger.debug("Update customer NID: {}", nextInvoiceDate3);
                    spcTestUserWS3.setNextInvoiceDate(nextInvoiceDateNew3);
                    api.updateUser(spcTestUserWS3);
                    spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
             //optus
             PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
             Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
             productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
             List<AssetWS> assetWSs = new ArrayList<>();
             List<AssetWS> assetWSs1 = new ArrayList<>();
             List<AssetWS> assetWSs3 = new ArrayList<>();
             List<AssetWS> assetWSs4 = new ArrayList<>();
             List<AssetWS> assetWSs5 = new ArrayList<>();
             List<AssetWS> assetWSs6 = new ArrayList<>();
             List<AssetWS> assetWSs7 = new ArrayList<>();
             List<AssetWS> assetWSs2 = new ArrayList<>();

             Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier9,
                                  "asset-01", assetIdentifier9);
             
             Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier10,
                                   "asset-02", assetIdentifier10);
             
             Integer asset3 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier11,
                                   "asset-01", assetIdentifier11);
          
             Integer asset4 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier12,
                                   "asset-02", assetIdentifier12);
          
             Integer asset5 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier13,
                                   "asset-01", assetIdentifier13);
       
             Integer asset6 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier14,
                                   "asset-02", assetIdentifier14);
       
             Integer asset7 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              assetIdentifier15,
                                   "asset-01", assetIdentifier15);
    
             Integer asset8 = buildAndPersistAssetWithServiceId(envBuilder,
                              getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                              getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                              "assetIdentifier16",
                                    "asset-02", assetIdentifier16);

                 assetWSs.add(api.getAsset(asset1));
                 assetWSs1.add(api.getAsset(asset2));
                 assetWSs2.add(api.getAsset(asset3));
                 assetWSs3.add(api.getAsset(asset4));
                 assetWSs4.add(api.getAsset(asset5));
                 assetWSs5.add(api.getAsset(asset6));
                 assetWSs6.add(api.getAsset(asset7));
                 assetWSs7.add(api.getAsset(asset8));

                 orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                 
                 orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);
                 
                 Integer orderId2 = createOrderWithAsset("TestOrder", spcTestUserWS1.getId(), nextInvoiceDateNew1, null, 
                         MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs2, optusPlanId);
                  
                 Integer orderId3 = createOrderWithAsset("TestOrder", spcTestUserWS1.getId(), nextInvoiceDateNew1, null, 
                             MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs3, optusPlanId);
                 
                 Integer orderId4 = createOrderWithAsset("TestOrder", spcTestUserWS2.getId(), nextInvoiceDateNew2, null, 
                         MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs4, optusPlanId);
                  
                 Integer orderId5 = createOrderWithAsset("TestOrder", spcTestUserWS2.getId(), nextInvoiceDateNew2, null, 
                             MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs5, optusPlanId);
                  
                 Integer orderId6 = createOrderWithAsset("TestOrder", spcTestUserWS3.getId(), nextInvoiceDateNew3, null, 
                          MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs6, optusPlanId);
                   
                 Integer orderId7 = createOrderWithAsset("TestOrder", spcTestUserWS3.getId(), nextInvoiceDateNew3, null, 
                              MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs7, optusPlanId);
                   
                 // 28th , 29th, 30th, 31st Oct
                 
              // create invoice:
                 Integer[] invoices = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                 logger.debug("Plan Order invoice created {}", invoices[0]);
                 InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);
                 
                 LocalDate nextInvoiceDate4 = LocalDate.of(2020, 11, 28);  
                 Date nextInvoiceDateNew4 = Date.from(nextInvoiceDate4.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew4);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 Date nid = Date.from(LocalDate.of(2020, 11, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nid,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList = new ArrayList<String>();
                 BigDecimal invoiceLineAmt = new BigDecimal(0);
                 Integer number = 1;
                 Integer[] invoiceWs = api.getLastInvoices(spcTestUserWS.getId(), number);
                 InvoiceWS invoicess = api.getInvoiceWS(invoiceWs[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt = invoiceLineAmt.add(new BigDecimal(ildto.getAmount()));
                         descriptionList.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList.contains(optusPlanDescription+" Period from 10/28/2020 to 11/27/2020"));
                 String totalAmt = "15.0000000000" , balanceAmt = "15.0000000000"; BigDecimal invoiceLine = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoicess, invoiceLineAmt, totalAmt, balanceAmt, invoiceLine);
                 
                 Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew1);
                 
                 updateRunDateBillingProcessConfiguration(runDate);
                 executeBillingProcessByRunDate(runDate);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                 Date nid1 = Date.from(LocalDate.of(2020, 11, 29).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid1,spcTestUserWS1.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId2);
                 assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList1 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                 Integer number1 = 1;
                 Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS1.getId(), number1);
                 InvoiceWS invoicess1 = api.getInvoiceWS(invoiceWs1[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess1.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                         descriptionList1.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 10/29/2020 to 11/28/2020"));
                 String totalAmt1 = "15.0000000000" , balanceAmt1 = "15.0000000000"; BigDecimal invoiceLine1 = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoicess1, invoiceLineAmt1, totalAmt1, balanceAmt1, invoiceLine1);

                 Date runDate1 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew2);
                 
                 updateRunDateBillingProcessConfiguration(runDate1);
                 executeBillingProcessByRunDate(runDate1);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                 Date nid2 = Date.from(LocalDate.of(2020, 11, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid2,spcTestUserWS2.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId4);
                 assertEquals("Order NDB should be match", nid2,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList2 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt2 = new BigDecimal(0);
                 Integer number2 = 1;
                 Integer[] invoiceWs2 = api.getLastInvoices(spcTestUserWS1.getId(), number2);
                 InvoiceWS invoicess2 = api.getInvoiceWS(invoiceWs2[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess2.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt2 = invoiceLineAmt2.add(new BigDecimal(ildto.getAmount()));
                         descriptionList2.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 10/29/2020 to 11/28/2020"));
                 String totalAmt2 = "15.0000000000" , balanceAmt2 = "15.0000000000"; BigDecimal invoiceLine2 = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoicess2, invoiceLineAmt2, totalAmt2, balanceAmt2, invoiceLine2);
                 
                 Date runDate2 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew3);
                 
                 updateRunDateBillingProcessConfiguration(runDate2);
                 executeBillingProcessByRunDate(runDate2);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS3.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
                 Date nid3 = Date.from(LocalDate.of(2020, 11, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid3,spcTestUserWS3.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId6);
                 assertEquals("Order NDB should be match", nid3,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList3 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt3 = new BigDecimal(0);
                 Integer number3 = 1;
                 Integer[] invoiceWs3 = api.getLastInvoices(spcTestUserWS3.getId(), number3);
                 InvoiceWS invoicess3 = api.getInvoiceWS(invoiceWs3[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess3.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt3 = invoiceLineAmt3.add(new BigDecimal(ildto.getAmount()));
                         descriptionList3.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList3.contains(optusPlanDescription+" Period from 10/31/2020 to 11/29/2020"));
                 String totalAmt3 = "15.0000000000" , balanceAmt3 = "15.0000000000"; BigDecimal invoiceLine3 = new BigDecimal("14.9999850000");
                 validateInvoiceLineAmt(invoicess3, invoiceLineAmt3, totalAmt3, balanceAmt3, invoiceLine3);
                 
                 // 28th , 29th, 30th Nov
                 LocalDate nextInvoiceDate0 = LocalDate.of(2020, 11, 28);  
                 Date nextInvoiceDateNew0 = Date.from(nextInvoiceDate0.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // create invoice:
                 Integer[] invoices0 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew0, null, null, false);
                 assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices0)));
                 logger.debug("Plan Order invoice created {}", invoices0[0]);
                 InvoiceWS invoiceWS0 = api.getInvoiceWS(invoices0[0]);
                 
                 LocalDate nextInvoiceDate20 = LocalDate.of(2020, 12, 28);  
                 Date nextInvoiceDateNew20 = Date.from(nextInvoiceDate20.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
              // Update NID
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew20);
                 api.updateUser(spcTestUserWS);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 Date nid19 = Date.from(LocalDate.of(2020, 12, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid19,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nid19,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList4 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt20 = new BigDecimal(0);
                 Integer number20 = 1;
                 Integer[] invoiceWs20 = api.getLastInvoices(spcTestUserWS.getId(), number20);
                 InvoiceWS invoicess20 = api.getInvoiceWS(invoiceWs20[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess20.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt20 = invoiceLineAmt20.add(new BigDecimal(ildto.getAmount()));
                         descriptionList4.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList4.contains(optusPlanDescription+" Period from 11/28/2020 to 12/27/2020"));
                 assertTrue( descriptionList4.contains(optusPlanDescription+" Period from 10/28/2020 to 11/27/2020"));
                 String totalAmt20 = "45.0000000000" , balanceAmt20 = "30.0000000000"; BigDecimal invoiceLine20 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess20, invoiceLineAmt20, totalAmt20, balanceAmt20, invoiceLine20);
                 
                 LocalDate nextInvoiceDate21 = LocalDate.of(2020, 11, 29);  
                 Date nextInvoiceDateNew21 = Date.from(nextInvoiceDate21.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
                 updateRunDateBillingProcessConfiguration(nextInvoiceDateNew21);
                 executeBillingProcessByRunDate(nextInvoiceDateNew21);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                 Date nid20 = Date.from(LocalDate.of(2020, 12, 29).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid20,spcTestUserWS1.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId2);
                 assertEquals("Order NDB should be match", nid20,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList5 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt21 = new BigDecimal(0);
                 Integer number21 = 1;
                 Integer[] invoiceWs21 = api.getLastInvoices(spcTestUserWS1.getId(), number1);
                 InvoiceWS invoicess21 = api.getInvoiceWS(invoiceWs21[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess21.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt21 = invoiceLineAmt21.add(new BigDecimal(ildto.getAmount()));
                         descriptionList5.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList5.contains(optusPlanDescription+" Period from 11/29/2020 to 12/28/2020"));
                 assertTrue( descriptionList5.contains(optusPlanDescription+" Period from 10/29/2020 to 11/28/2020"));
                 String totalAmt21 = "45.0000000000" , balanceAmt21 = "30.0000000000"; BigDecimal invoiceLine21 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess21, invoiceLineAmt21, totalAmt21, balanceAmt21, invoiceLine21);

                 LocalDate nextInvoiceDate22 = LocalDate.of(2020, 11, 30);  
                 Date nextInvoiceDateNew22 = Date.from(nextInvoiceDate22.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
                 updateRunDateBillingProcessConfiguration(nextInvoiceDateNew22);
                 executeBillingProcessByRunDate(nextInvoiceDateNew22);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                 Date nid22 = Date.from(LocalDate.of(2020, 12, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid22,spcTestUserWS2.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId4);
                 assertEquals("Order NDB should be match", nid22,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList6 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt22 = new BigDecimal(0);
                 Integer number22 = 1;
                 Integer[] invoiceWs22 = api.getLastInvoices(spcTestUserWS2.getId(), number22);
                 InvoiceWS invoicess22 = api.getInvoiceWS(invoiceWs22[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess22.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt22 = invoiceLineAmt22.add(new BigDecimal(ildto.getAmount()));
                         descriptionList6.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList6.contains(optusPlanDescription+" Period from 11/30/2020 to 12/29/2020"));
                 assertTrue( descriptionList6.contains(optusPlanDescription+" Period from 10/30/2020 to 11/29/2020"));
                 String totalAmt22 = "45.0000000000" , balanceAmt22 = "30.0000000000"; BigDecimal invoiceLine22 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess22, invoiceLineAmt22, totalAmt22, balanceAmt22, invoiceLine22);
                 
              // 28th , 29th, 30th, 31st Dec
                 LocalDate nextInvoiceDate23 = LocalDate.of(2020, 12, 28);  
                 Date nextInvoiceDateNew23 = Date.from(nextInvoiceDate23.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 // create invoice:
                    Integer[] invoices23 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew23, null, null, false);
                    assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices23)));
                    logger.debug("Plan Order invoice created {}", invoices23[0]);
                    InvoiceWS invoiceWS23 = api.getInvoiceWS(invoices23[0]);
                    
                    LocalDate nextInvoiceDate24 = LocalDate.of(2021, 1, 28);  
                    Date nextInvoiceDateNew24 = Date.from(nextInvoiceDate24.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                 // Update NID
                    spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                    spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew24);
                    api.updateUser(spcTestUserWS);
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                    Date nid24 = Date.from(LocalDate.of(2021, 1, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid24,spcTestUserWS.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId);
                    assertEquals("Order NDB should be match", nid24,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList7 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt24 = new BigDecimal(0);
                    Integer number24 = 1;
                    Integer[] invoiceWs24 = api.getLastInvoices(spcTestUserWS.getId(), number24);
                    InvoiceWS invoicess24 = api.getInvoiceWS(invoiceWs24[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess24.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt24 = invoiceLineAmt24.add(new BigDecimal(ildto.getAmount()));
                            descriptionList7.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList7.contains(optusPlanDescription+" Period from 12/28/2020 to 01/27/2021"));
                    assertTrue( descriptionList7.contains(optusPlanDescription+" Period from 11/28/2020 to 12/27/2020"));
                    String totalAmt24 = "75.0000000000" , balanceAmt24 = "30.0000000000"; BigDecimal invoiceLine24 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess24, invoiceLineAmt24, totalAmt24, balanceAmt24, invoiceLine24);
                    
                    LocalDate nextInvoiceDate25 = LocalDate.of(2020, 12, 29);  
                    Date nextInvoiceDateNew25 = Date.from(nextInvoiceDate25.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    updateRunDateBillingProcessConfiguration(nextInvoiceDateNew25);
                    executeBillingProcessByRunDate(nextInvoiceDateNew25);
                    
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                    Date nid25 = Date.from(LocalDate.of(2021, 1, 29).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid25,spcTestUserWS1.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId2);
                    assertEquals("Order NDB should be match", nid25,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList8 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt25 = new BigDecimal(0);
                    Integer number25 = 1;
                    Integer[] invoiceWs25 = api.getLastInvoices(spcTestUserWS1.getId(), number25);
                    InvoiceWS invoicess25 = api.getInvoiceWS(invoiceWs25[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess25.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt25 = invoiceLineAmt25.add(new BigDecimal(ildto.getAmount()));
                            descriptionList8.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList8.contains(optusPlanDescription+" Period from 12/29/2020 to 01/28/2021"));
                    assertTrue( descriptionList8.contains(optusPlanDescription+" Period from 11/29/2020 to 12/28/2020"));
                    String totalAmt25 = "75.0000000000" , balanceAmt25 = "30.0000000000"; BigDecimal invoiceLine25 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess25, invoiceLineAmt25, totalAmt25, balanceAmt25, invoiceLine25);

                    LocalDate nextInvoiceDate26 = LocalDate.of(2020, 12, 30);  
                    Date nextInvoiceDateNew26 = Date.from(nextInvoiceDate26.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    updateRunDateBillingProcessConfiguration(nextInvoiceDateNew26);
                    executeBillingProcessByRunDate(nextInvoiceDateNew26);
                    
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                    Date nid26 = Date.from(LocalDate.of(2021, 1, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid26,spcTestUserWS2.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId4);
                    assertEquals("Order NDB should be match", nid26,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList9 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt26 = new BigDecimal(0);
                    Integer number26 = 1;
                    Integer[] invoiceWs26 = api.getLastInvoices(spcTestUserWS1.getId(), number26);
                    InvoiceWS invoicess26 = api.getInvoiceWS(invoiceWs26[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess26.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt26 = invoiceLineAmt26.add(new BigDecimal(ildto.getAmount()));
                            descriptionList9.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList9.contains(optusPlanDescription+" Period from 12/29/2020 to 01/28/2021"));
                    assertTrue( descriptionList9.contains(optusPlanDescription+" Period from 11/29/2020 to 12/28/2020"));
                    String totalAmt26 = "75.0000000000" , balanceAmt26 = "30.0000000000"; BigDecimal invoiceLine26 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess26, invoiceLineAmt26, totalAmt26, balanceAmt26, invoiceLine26);
                    
                    LocalDate nextInvoiceDate27 = LocalDate.of(2020, 12, 31);  
                    Date nextInvoiceDateNew27 = Date.from(nextInvoiceDate27.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    updateRunDateBillingProcessConfiguration(nextInvoiceDateNew27);
                    executeBillingProcessByRunDate(nextInvoiceDateNew27);
                    
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS3.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
                    Date nid27 = Date.from(LocalDate.of(2021, 1, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid27,spcTestUserWS3.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId6);
                    assertEquals("Order NDB should be match", nid27,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList10 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt27 = new BigDecimal(0);
                    Integer number27 = 1;
                    Integer[] invoiceWs27 = api.getLastInvoices(spcTestUserWS3.getId(), number27);
                    InvoiceWS invoicess27 = api.getInvoiceWS(invoiceWs27[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess27.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt27 = invoiceLineAmt27.add(new BigDecimal(ildto.getAmount()));
                            descriptionList10.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList10.contains(optusPlanDescription+" Period from 12/31/2020 to 01/30/2021"));
                    assertTrue( descriptionList10.contains(optusPlanDescription+" Period from 11/30/2020 to 12/30/2020"));
                    String totalAmt27 = "75.0000000000" , balanceAmt27 = "30.0000000000"; BigDecimal invoiceLine27 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess27, invoiceLineAmt27, totalAmt27, balanceAmt27, invoiceLine27);
                    
                 // 28th , 29th, 30th, 31st Jan
                    LocalDate nextInvoiceDate28 = LocalDate.of(2021, 1, 28);  
                    Date nextInvoiceDateNew28 = Date.from(nextInvoiceDate28.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    // create invoice:
                       Integer[] invoices28 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew28, null, null, false);
                       assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices28)));
                       logger.debug("Plan Order invoice created {}", invoices28[0]);
                       InvoiceWS invoiceWS28 = api.getInvoiceWS(invoices28[0]);
                       
                       LocalDate nextInvoiceDate29 = LocalDate.of(2021, 2, 28);  
                       Date nextInvoiceDateNew29 = Date.from(nextInvoiceDate29.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                    // Update NID
                       spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                       spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew29);
                       api.updateUser(spcTestUserWS);
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                       Date nid29 = Date.from(LocalDate.of(2021, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid29,spcTestUserWS.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId);
                       assertEquals("Order NDB should be match", nid29,orderWs.getNextBillableDay());
                       
                     //Validate invoice total amount and balance amount
                       List<String> descriptionList11 = new ArrayList<String>();
                       BigDecimal invoiceLineAmt29 = new BigDecimal(0);
                       Integer number29 = 1;
                       Integer[] invoiceWs29 = api.getLastInvoices(spcTestUserWS.getId(), number29);
                       InvoiceWS invoicess29 = api.getInvoiceWS(invoiceWs29[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess29.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt29 = invoiceLineAmt29.add(new BigDecimal(ildto.getAmount()));
                               descriptionList11.add(ildto.getDescription());
                           }
                       }
                       assertTrue( descriptionList11.contains(optusPlanDescription+" Period from 01/28/2021 to 02/27/2021"));
                       assertTrue( descriptionList11.contains(optusPlanDescription+" Period from 12/28/2020 to 01/27/2021"));
                       String totalAmt29 = "105.0000000000" , balanceAmt29 = "30.0000000000"; BigDecimal invoiceLine29 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess29, invoiceLineAmt29, totalAmt29, balanceAmt29, invoiceLine29);
                       
                       LocalDate nextInvoiceDate30 = LocalDate.of(2021, 1, 29);  
                       Date nextInvoiceDateNew30 = Date.from(nextInvoiceDate30.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                       updateRunDateBillingProcessConfiguration(nextInvoiceDateNew30);
                       executeBillingProcessByRunDate(nextInvoiceDateNew30);
                       
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                       Date nid28 = Date.from(LocalDate.of(2021, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid28,spcTestUserWS1.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId2);
                       assertEquals("Order NDB should be match", nid28,orderWs.getNextBillableDay());
                       
                     //Validate invoice total amount and balance amount
                       List<String> descriptionList12 = new ArrayList<String>();
                       BigDecimal invoiceLineAmt28 = new BigDecimal(0);
                       Integer number28 = 1;
                       Integer[] invoiceWs28 = api.getLastInvoices(spcTestUserWS1.getId(), number28);
                       InvoiceWS invoicess28 = api.getInvoiceWS(invoiceWs28[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess28.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt28 = invoiceLineAmt28.add(new BigDecimal(ildto.getAmount()));
                               descriptionList12.add(ildto.getDescription());
                           }
                       }
                       assertTrue( descriptionList12.contains(optusPlanDescription+" Period from 01/29/2021 to 02/27/2021"));
                       assertTrue( descriptionList12.contains(optusPlanDescription+" Period from 12/29/2020 to 01/28/2021"));
                       String totalAmt28 = "105.0000000000" , balanceAmt28 = "30.0000000000"; BigDecimal invoiceLine28 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess28, invoiceLineAmt28, totalAmt28, balanceAmt28, invoiceLine28);

                       LocalDate nextInvoiceDate31 = LocalDate.of(2021, 1, 30);  
                       Date nextInvoiceDateNew31 = Date.from(nextInvoiceDate31.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                       updateRunDateBillingProcessConfiguration(nextInvoiceDateNew31);
                       executeBillingProcessByRunDate(nextInvoiceDateNew31);
                       
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                       Date nid30 = Date.from(LocalDate.of(2021, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid30,spcTestUserWS2.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId4);
                       assertEquals("Order NDB should be match", nid30,orderWs.getNextBillableDay());
                       
                     //Validate invoice total amount and balance amount
                       List<String> descriptionList13 = new ArrayList<String>();
                       BigDecimal invoiceLineAmt30 = new BigDecimal(0);
                       Integer number30 = 1;
                       Integer[] invoiceWs30 = api.getLastInvoices(spcTestUserWS1.getId(), number30);
                       InvoiceWS invoicess30 = api.getInvoiceWS(invoiceWs30[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess30.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt30 = invoiceLineAmt30.add(new BigDecimal(ildto.getAmount()));
                               descriptionList13.add(ildto.getDescription());
                           }
                       }
                       assertTrue( descriptionList13.contains(optusPlanDescription+" Period from 01/29/2021 to 02/27/2021"));
                       assertTrue( descriptionList13.contains(optusPlanDescription+" Period from 12/29/2020 to 01/28/2021"));
                       String totalAmt30 = "105.0000000000" , balanceAmt30 = "30.0000000000"; BigDecimal invoiceLine30 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess30, invoiceLineAmt30, totalAmt30, balanceAmt30, invoiceLine30);
                       
                       LocalDate nextInvoiceDate32 = LocalDate.of(2021, 1, 31);  
                       Date nextInvoiceDateNew32 = Date.from(nextInvoiceDate32.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                       updateRunDateBillingProcessConfiguration(nextInvoiceDateNew32);
                       executeBillingProcessByRunDate(nextInvoiceDateNew32);
                       
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS3.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
                       Date nid32 = Date.from(LocalDate.of(2021, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid32,spcTestUserWS3.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId6);
                       assertEquals("Order NDB should be match", nid32,orderWs.getNextBillableDay());
                       
                     //Validate invoice total amount and balance amount
                       List<String> descriptionList14 = new ArrayList<String>();
                       BigDecimal invoiceLineAmt32 = new BigDecimal(0);
                       Integer number32 = 1;
                       Integer[] invoiceWs32 = api.getLastInvoices(spcTestUserWS3.getId(), number32);
                       InvoiceWS invoicess32 = api.getInvoiceWS(invoiceWs32[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess32.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt32 = invoiceLineAmt32.add(new BigDecimal(ildto.getAmount()));
                               descriptionList14.add(ildto.getDescription());
                           }
                       }
                       assertTrue( descriptionList14.contains(optusPlanDescription+" Period from 01/31/2021 to 02/27/2021"));
                       assertTrue( descriptionList14.contains(optusPlanDescription+" Period from 12/31/2020 to 01/30/2021"));
                       String totalAmt32 = "105.0000000000" , balanceAmt32 = "30.0000000000"; BigDecimal invoiceLine32 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess32, invoiceLineAmt32, totalAmt32, balanceAmt32, invoiceLine32);
                 
                 //****************28th Feb***************//
              // Bill run
                 LocalDate nextInvoiceDate5 = LocalDate.of(2021, 02, 28);  
                 Date nextInvoiceDateNew5 = Date.from(nextInvoiceDate5.atStartOfDay(ZoneId.systemDefault()).toInstant());
                 
                 Date runDate3 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew5);
                 
                 updateRunDateBillingProcessConfiguration(runDate3);
                 executeBillingProcessByRunDate(runDate3);
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                 Date nid4 = Date.from(LocalDate.of(2021, 3, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid4,spcTestUserWS.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId);
                 assertEquals("Order NDB should be match", nid4,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList15 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt4 = new BigDecimal(0);
                 Integer number4 = 1;
                 Integer[] invoiceWs4 = api.getLastInvoices(spcTestUserWS.getId(), number4);
                 InvoiceWS invoicess4 = api.getInvoiceWS(invoiceWs4[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess4.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt4 = invoiceLineAmt4.add(new BigDecimal(ildto.getAmount()));
                         descriptionList15.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList15.contains(optusPlanDescription+" Period from 02/28/2021 to 03/27/2021"));
                 assertTrue( descriptionList15.contains(optusPlanDescription+" Period from 01/28/2021 to 02/27/2021"));
                 String totalAmt4 = "135.0000000000" , balanceAmt4 = "30.0000000000"; BigDecimal invoiceLine4 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess4, invoiceLineAmt4, totalAmt4, balanceAmt4, invoiceLine4);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                 Date nid6 = Date.from(LocalDate.of(2021, 3, 29).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid6,spcTestUserWS1.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId2);
                 assertEquals("Order NDB should be match", nid6,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList16 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt5 = new BigDecimal(0);
                 Integer number5 = 1;
                 Integer[] invoiceWs5 = api.getLastInvoices(spcTestUserWS1.getId(), number5);
                 InvoiceWS invoicess5 = api.getInvoiceWS(invoiceWs5[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess5.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt5 = invoiceLineAmt5.add(new BigDecimal(ildto.getAmount()));
                         descriptionList16.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList16.contains(optusPlanDescription+" Period from 02/28/2021 to 03/28/2021"));
                 assertTrue( descriptionList16.contains(optusPlanDescription+" Period from 01/29/2021 to 02/27/2021"));
                 String totalAmt5 = "135.0000000000" , balanceAmt5 = "30.0000000000"; BigDecimal invoiceLine5 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess5, invoiceLineAmt5, totalAmt5, balanceAmt5, invoiceLine5);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                 Date nid7 = Date.from(LocalDate.of(2021, 3, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid7,spcTestUserWS2.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId4);
                 assertEquals("Order NDB should be match", nid7,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList17 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt6 = new BigDecimal(0);
                 Integer number6 = 1;
                 Integer[] invoiceWs6 = api.getLastInvoices(spcTestUserWS1.getId(), number6);
                 InvoiceWS invoicess6 = api.getInvoiceWS(invoiceWs6[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess6.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt6 = invoiceLineAmt6.add(new BigDecimal(ildto.getAmount()));
                         descriptionList17.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList17.contains(optusPlanDescription+" Period from 02/28/2021 to 03/28/2021"));
                 assertTrue( descriptionList17.contains(optusPlanDescription+" Period from 01/29/2021 to 02/27/2021"));
                 String totalAmt6 = "135.0000000000" , balanceAmt6 = "30.0000000000"; BigDecimal invoiceLine6 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess6, invoiceLineAmt6, totalAmt6, balanceAmt6, invoiceLine6);
                 
                 logger.debug("## customer nextInvoiceDate {}",spcTestUserWS3.getNextInvoiceDate());
                 
               //Validate Customer NID and Order NBD
                 spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
                 Date nid8 = Date.from(LocalDate.of(2021, 3, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
                 assertEquals("Customer NID should be match", nid8,spcTestUserWS3.getNextInvoiceDate());
                 orderWs = api.getOrder(orderId6);
                 assertEquals("Order NDB should be match", nid8,orderWs.getNextBillableDay());
                 
               //Validate invoice total amount and balance amount
                 List<String> descriptionList18 = new ArrayList<String>();
                 BigDecimal invoiceLineAmt7 = new BigDecimal(0);
                 Integer number7 = 1;
                 Integer[] invoiceWs7 = api.getLastInvoices(spcTestUserWS3.getId(), number7);
                 InvoiceWS invoicess7 = api.getInvoiceWS(invoiceWs7[0]);
                 for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess7.getInvoiceLines()){
                     if(ildto.getTypeId()==1){
                         invoiceLineAmt7 = invoiceLineAmt7.add(new BigDecimal(ildto.getAmount()));
                         descriptionList18.add(ildto.getDescription());
                     }
                 }
                 assertTrue( descriptionList18.contains(optusPlanDescription+" Period from 02/28/2021 to 03/30/2021"));
                 assertTrue( descriptionList18.contains(optusPlanDescription+" Period from 01/31/2021 to 02/27/2021"));
                 String totalAmt7 = "135.0000000000" , balanceAmt7 = "30.0000000000"; BigDecimal invoiceLine7 = new BigDecimal("29.9999700000");
                 validateInvoiceLineAmt(invoicess7, invoiceLineAmt7, totalAmt7, balanceAmt7, invoiceLine7);
                 
               //****************28th, 29th, 30th, 31st March***************//
                 // Bill run
                    LocalDate nextInvoiceDate8 = LocalDate.of(2021, 03, 28);  
                    Date nextInvoiceDateNew8 = Date.from(nextInvoiceDate8.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    Date runDate8 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew8);
                    
                    updateRunDateBillingProcessConfiguration(runDate8);
                    executeBillingProcessByRunDate(runDate8);
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                    Date nid9 = Date.from(LocalDate.of(2021, 4, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid9,spcTestUserWS.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId);
                    assertEquals("Order NDB should be match", nid9,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList19 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt9 = new BigDecimal(0);
                    Integer number9 = 1;
                    Integer[] invoiceWs9 = api.getLastInvoices(spcTestUserWS.getId(), number9);
                    InvoiceWS invoicess9 = api.getInvoiceWS(invoiceWs9[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess9.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt9 = invoiceLineAmt9.add(new BigDecimal(ildto.getAmount()));
                            descriptionList19.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList19.contains(optusPlanDescription+" Period from 03/28/2021 to 04/27/2021"));
                    assertTrue( descriptionList19.contains(optusPlanDescription+" Period from 02/28/2021 to 03/27/2021"));
                    String totalAmt9 = "165.0000000000" , balanceAmt9 = "30.0000000000"; BigDecimal invoiceLine9 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess9, invoiceLineAmt9, totalAmt9, balanceAmt9, invoiceLine9);
                    
                    LocalDate nextInvoiceDate9 = LocalDate.of(2021, 03, 29);  
                    Date nextInvoiceDateNew10 = Date.from(nextInvoiceDate9.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    Date runDate9 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew10);
                    
                    updateRunDateBillingProcessConfiguration(runDate9);
                    executeBillingProcessByRunDate(runDate9);
                    
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                    Date nid10 = Date.from(LocalDate.of(2021, 4, 29).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid10,spcTestUserWS1.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId2);
                    assertEquals("Order NDB should be match", nid10,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList20 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt10 = new BigDecimal(0);
                    Integer number10 = 1;
                    Integer[] invoiceWs10 = api.getLastInvoices(spcTestUserWS1.getId(), number10);
                    InvoiceWS invoicess10 = api.getInvoiceWS(invoiceWs10[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess10.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt10 = invoiceLineAmt10.add(new BigDecimal(ildto.getAmount()));
                            descriptionList20.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList20.contains(optusPlanDescription+" Period from 03/29/2021 to 04/28/2021"));
                    assertTrue( descriptionList20.contains(optusPlanDescription+" Period from 02/28/2021 to 03/28/2021"));
                    String totalAmt10 = "165.0000000000" , balanceAmt10 = "30.0000000000"; BigDecimal invoiceLine10 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess10, invoiceLineAmt10, totalAmt10, balanceAmt10, invoiceLine10);
                    
                    LocalDate nextInvoiceDate10 = LocalDate.of(2021, 03, 30);  
                    Date nextInvoiceDateNew11 = Date.from(nextInvoiceDate10.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    Date runDate10 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew11);
                    
                    updateRunDateBillingProcessConfiguration(runDate10);
                    executeBillingProcessByRunDate(runDate10);
                    
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                    Date nid11 = Date.from(LocalDate.of(2021, 4, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid11,spcTestUserWS2.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId4);
                    assertEquals("Order NDB should be match", nid11,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList21 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt11 = new BigDecimal(0);
                    Integer number11 = 1;
                    Integer[] invoiceWs11 = api.getLastInvoices(spcTestUserWS1.getId(), number11);
                    InvoiceWS invoicess11 = api.getInvoiceWS(invoiceWs11[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess11.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt11 = invoiceLineAmt11.add(new BigDecimal(ildto.getAmount()));
                            descriptionList21.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList21.contains(optusPlanDescription+" Period from 03/29/2021 to 04/28/2021"));
                    assertTrue( descriptionList21.contains(optusPlanDescription+" Period from 02/28/2021 to 03/28/2021"));
                    String totalAmt11 = "165.0000000000" , balanceAmt11 = "30.0000000000"; BigDecimal invoiceLine11 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess11, invoiceLineAmt11, totalAmt11, balanceAmt11, invoiceLine11);
                    
                    LocalDate nextInvoiceDate11 = LocalDate.of(2021, 03, 31);  
                    Date nextInvoiceDateNew12 = Date.from(nextInvoiceDate11.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    Date runDate11 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew12);
                    
                    updateRunDateBillingProcessConfiguration(runDate11);
                    executeBillingProcessByRunDate(runDate11);
                    
                    logger.debug("## customer nextInvoiceDate {}",spcTestUserWS3.getNextInvoiceDate());
                    
                  //Validate Customer NID and Order NBD
                    spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
                    Date nid13 = Date.from(LocalDate.of(2021, 4, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    assertEquals("Customer NID should be match", nid13,spcTestUserWS3.getNextInvoiceDate());
                    orderWs = api.getOrder(orderId6);
                    assertEquals("Order NDB should be match", nid13,orderWs.getNextBillableDay());
                    
                  //Validate invoice total amount and balance amount
                    List<String> descriptionList22 = new ArrayList<String>();
                    BigDecimal invoiceLineAmt12 = new BigDecimal(0);
                    Integer number12 = 1;
                    Integer[] invoiceWs12 = api.getLastInvoices(spcTestUserWS3.getId(), number12);
                    InvoiceWS invoicess12 = api.getInvoiceWS(invoiceWs12[0]);
                    for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess12.getInvoiceLines()){
                        if(ildto.getTypeId()==1){
                            invoiceLineAmt12 = invoiceLineAmt12.add(new BigDecimal(ildto.getAmount()));
                            descriptionList22.add(ildto.getDescription());
                        }
                    }
                    assertTrue( descriptionList22.contains(optusPlanDescription+" Period from 03/31/2021 to 04/29/2021"));
                    assertTrue( descriptionList22.contains(optusPlanDescription+" Period from 02/28/2021 to 03/30/2021"));
                    String totalAmt12 = "165.0000000000" , balanceAmt12 = "30.0000000000"; BigDecimal invoiceLine12 = new BigDecimal("29.9999700000");
                    validateInvoiceLineAmt(invoicess12, invoiceLineAmt12, totalAmt12, balanceAmt12, invoiceLine12);
                    
                  //****************28th, 29th, 30th April***************//
                    // Bill run
                       LocalDate nextInvoiceDate13 = LocalDate.of(2021, 04, 28);  
                       Date nextInvoiceDateNew13 = Date.from(nextInvoiceDate13.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                       Date runDate13 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew13);
                       
                       updateRunDateBillingProcessConfiguration(runDate13);
                       executeBillingProcessByRunDate(runDate13);
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                       Date nid14 = Date.from(LocalDate.of(2021, 5, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid14,spcTestUserWS.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId);
                       assertEquals("Order NDB should be match", nid14,orderWs.getNextBillableDay());
                       
                     //Validate invoice total amount and balance amount
                       List<String> descriptionList23 = new ArrayList<String>();
                       BigDecimal invoiceLineAmt14 = new BigDecimal(0);
                       Integer number14 = 1;
                       Integer[] invoiceWs14 = api.getLastInvoices(spcTestUserWS.getId(), number14);
                       InvoiceWS invoicess14 = api.getInvoiceWS(invoiceWs14[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess14.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt14 = invoiceLineAmt14.add(new BigDecimal(ildto.getAmount()));
                               descriptionList23.add(ildto.getDescription());
                           }
                       }
                       assertTrue( descriptionList23.contains(optusPlanDescription+" Period from 04/28/2021 to 05/27/2021"));
                       assertTrue( descriptionList23.contains(optusPlanDescription+" Period from 03/28/2021 to 04/27/2021"));
                       String totalAmt14 = "195.0000000000" , balanceAmt14 = "30.0000000000"; BigDecimal invoiceLine14 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess14, invoiceLineAmt9, totalAmt14, balanceAmt14, invoiceLine14);
                       
                       LocalDate nextInvoiceDate14 = LocalDate.of(2021, 04, 29);  
                       Date nextInvoiceDateNew14 = Date.from(nextInvoiceDate14.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                       Date runDate14 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew14);
                       
                       updateRunDateBillingProcessConfiguration(runDate14);
                       executeBillingProcessByRunDate(runDate14);
                       
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS1.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS1 = api.getUserWS(spcTestUserWS1.getId());
                       Date nid15 = Date.from(LocalDate.of(2021, 5, 29).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid15,spcTestUserWS1.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId2);
                       assertEquals("Order NDB should be match", nid15,orderWs.getNextBillableDay());
                       
                     //Validate invoice total amount and balance amount
                       List<String> descriptionList24 = new ArrayList<String>();
                       BigDecimal invoiceLineAmt15 = new BigDecimal(0);
                       Integer number15 = 1;
                       Integer[] invoiceWs15 = api.getLastInvoices(spcTestUserWS1.getId(), number15);
                       InvoiceWS invoicess15 = api.getInvoiceWS(invoiceWs15[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess15.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt15 = invoiceLineAmt15.add(new BigDecimal(ildto.getAmount()));
                               descriptionList24.add(ildto.getDescription());
                           }
                       }
                       assertTrue( descriptionList24.contains(optusPlanDescription+" Period from 04/29/2021 to 05/28/2021"));
                       assertTrue( descriptionList24.contains(optusPlanDescription+" Period from 03/29/2021 to 04/28/2021"));
                       String totalAmt15 = "195.0000000000" , balanceAmt15 = "30.0000000000"; BigDecimal invoiceLine15 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess15, invoiceLineAmt15, totalAmt15, balanceAmt15, invoiceLine15);
                       
                       LocalDate nextInvoiceDate15 = LocalDate.of(2021, 04, 30);  
                       Date nextInvoiceDateNew15 = Date.from(nextInvoiceDate15.atStartOfDay(ZoneId.systemDefault()).toInstant());
                       
                       Date runDate15 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew15);
                       
                       updateRunDateBillingProcessConfiguration(runDate15);
                       executeBillingProcessByRunDate(runDate15);
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS2 = api.getUserWS(spcTestUserWS2.getId());
                       Date nid16 = Date.from(LocalDate.of(2021, 5, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid16,spcTestUserWS2.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId4);
                       assertEquals("Order NDB should be match", nid16,orderWs.getNextBillableDay());
                       
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS2.getNextInvoiceDate());
                       
                     //Validate Customer NID and Order NBD
                       spcTestUserWS3 = api.getUserWS(spcTestUserWS3.getId());
                       Date nid18 = Date.from(LocalDate.of(2021, 5, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
                       assertEquals("Customer NID should be match", nid18,spcTestUserWS3.getNextInvoiceDate());
                       orderWs = api.getOrder(orderId6);
                       assertEquals("Order NDB should be match", nid18,orderWs.getNextBillableDay());
                       
                       logger.debug("## customer nextInvoiceDate {}",spcTestUserWS3.getNextInvoiceDate());
                       
                     //Validate invoice total amount and balance amount
                       BigDecimal invoiceLineAmt16 = new BigDecimal(0);
                       Integer number16 = 1;
                       Integer[] invoiceWs16 = api.getLastInvoices(spcTestUserWS2.getId(), number16);
                       InvoiceWS invoicess16 = api.getInvoiceWS(invoiceWs16[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess16.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt16 = invoiceLineAmt16.add(new BigDecimal(ildto.getAmount()));
                           }
                       }
                       String totalAmt16 = "195.0000000000" , balanceAmt16 = "30.0000000000"; BigDecimal invoiceLine16 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess16, invoiceLineAmt16, totalAmt16, balanceAmt16, invoiceLine16);
                       
                     //Validate invoice total amount and balance amount
                       BigDecimal invoiceLineAmt18 = new BigDecimal(0);
                       Integer number18 = 1;
                       Integer[] invoiceWs18 = api.getLastInvoices(spcTestUserWS2.getId(), number18);
                       InvoiceWS invoicess18 = api.getInvoiceWS(invoiceWs18[0]);
                       for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoicess18.getInvoiceLines()){
                           if(ildto.getTypeId()==1){
                               invoiceLineAmt18 = invoiceLineAmt18.add(new BigDecimal(ildto.getAmount()));
                           }
                       }
                       String totalAmt18 = "195.0000000000" , balanceAmt18 = "30.0000000000"; BigDecimal invoiceLine18 = new BigDecimal("29.9999700000");
                       validateInvoiceLineAmt(invoicess18, invoiceLineAmt18, totalAmt18, balanceAmt18, invoiceLine18);
                       
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    /**
     * 
     * Create a customer with NID - 30th June 2020
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 30th June 2020
     * Invoice generated for 30th June 2020
     * Invoice generated for 30th July 2020
     * Invoice generated for 30th August 2020
     */
    @Test(priority = 5)
    public void testPeriodForPrepaidOrderwithThirtyMonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2020, 6, 30);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            testBuilder.given(envBuilder -> {
                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER5, 
                        nextInvoiceDateNew,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);
                // Update customer NID
                logger.debug("Update customer NID: {}", nextInvoiceDateNew);
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
            

               //optus
               PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
               Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
               productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
               List<AssetWS> assetWSs = new ArrayList<>();
               List<AssetWS> assetWSs1 = new ArrayList<>();

               Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648562",
                        "asset-15", "5123648562");
               
               Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "2145632589",
                        "asset-16", "2145632589");

                   assetWSs.add(api.getAsset(asset1));
                   assetWSs1.add(api.getAsset(asset2));

                   orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                   
                   orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);
                   
                
                   //Bill run process
                   Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                   
                   //Validate Customer NID 
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   Date nid = Date.from(LocalDate.of(2020, 6, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                   
                   updateRunDateBillingProcessConfiguration(runDate);
                   executeBillingProcessByRunDate(runDate);
                   
                   //Validate invoice total amount and balance amount
                   List<String> descriptionList = new ArrayList<String>();
                   Integer number = 1;
                   Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS.getId(), number);
                   InvoiceWS invoices = api.getInvoiceWS(invoiceWs1[0]);
                   BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices.getInvoiceLines()){
                       if(ildto.getTypeId()==1){
                           System.out.println(ildto.getAmount());
                           invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                           descriptionList.add(ildto.getDescription());
                       }
                   }
                   assertTrue( descriptionList.contains(optusPlanDescription+" Period from 06/30/2020 to 07/29/2020"));
                   String totalAmt1 = "15.0000000000" , balanceAmt1 = "15.0000000000"; BigDecimal invoiceLine1 = new BigDecimal("14.9999850000");
                   validateInvoiceLineAmt(invoices, invoiceLineAmt1, totalAmt1, balanceAmt1, invoiceLine1);
                   
                   Date nid1 = Date.from(LocalDate.of(2020, 7, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   
                   Date runDate11 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nid1);
                   
                   //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid1, spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                   
                   updateRunDateBillingProcessConfiguration(runDate11);
                   executeBillingProcessByRunDate(runDate11);
                   
                   List<String> descriptionList1 = new ArrayList<String>();
                   BigDecimal invoiceLineAmt2 = new BigDecimal(0);
                   Integer number1 = 1;
                   Integer[] invoiceWs2 = api.getLastInvoices(spcTestUserWS.getId(), number1);
                   InvoiceWS invoices1 = api.getInvoiceWS(invoiceWs2[0]);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto1 : invoices1.getInvoiceLines()){
                       if(ildto1.getTypeId()==1){
                           invoiceLineAmt2 = invoiceLineAmt2.add(new BigDecimal(ildto1.getAmount()));
                           descriptionList1.add(ildto1.getDescription());
                       }
                   }
                   assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 06/30/2020 to 07/29/2020"));
                   assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 07/30/2020 to 08/29/2020"));
                   String totalAmt2 = "45.0000000000" , balanceAmt2 = "30.0000000000"; BigDecimal invoiceLine2 = new BigDecimal("29.9999700000");
                   validateInvoiceLineAmt(invoices1, invoiceLineAmt2, totalAmt2, balanceAmt2, invoiceLine2);
                   //31st May bill run
                   Date nid2 = Date.from(LocalDate.of(2020, 8, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   
                   Date runDate22 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nid2);
                   
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid2,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid2,orderWs.getNextBillableDay());
                   
                   updateRunDateBillingProcessConfiguration(runDate22);
                   executeBillingProcessByRunDate(runDate22);
                   
                   List<String> descriptionList2 = new ArrayList<String>();
                   BigDecimal invoiceLineAmt3 = new BigDecimal(0);
                   Integer number2 = 1;
                   Integer[] invoiceWs3 = api.getLastInvoices(spcTestUserWS.getId(), number2);
                   InvoiceWS invoices3 = api.getInvoiceWS(invoiceWs3[0]);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto1 : invoices3.getInvoiceLines()){
                       if(ildto1.getTypeId()==1){
                           invoiceLineAmt3 = invoiceLineAmt3.add(new BigDecimal(ildto1.getAmount()));
                           descriptionList2.add(ildto1.getDescription());
                       }
                   }
                   assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 07/30/2020 to 08/29/2020"));
                   assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 08/30/2020 to 09/29/2020"));
                   String totalAmt3 = "75.0000000000" , balanceAmt3 = "30.0000000000"; BigDecimal invoiceLine3 = new BigDecimal("29.9999700000");
                   validateInvoiceLineAmt(invoices3, invoiceLineAmt3, totalAmt3, balanceAmt3, invoiceLine3);
                 //Validate Customer NID and Order NBD
                   Date nid3 = Date.from(LocalDate.of(2020, 9, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid3,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid3,orderWs.getNextBillableDay());
                 
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    
    /**
     * 
     * Create a customer with NID - 31th May 2021
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 31th May 2021
     * Bill run for 31th May 2021
     * Bill run for 30th June 2021
     * Bill run for 31th July 2021
     * Bill run for 31th August 2021
     */
    @Test(priority = 1)
    public void testPeriodForPrepaidOrderwithThirtyFirstMonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2019, 5, 31);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            testBuilder.given(envBuilder -> {
                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER6, 
                        nextInvoiceDateNew,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);
                // Update customer NID
                logger.debug("Update customer NID: {}", nextInvoiceDateNew);
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
            

               //optus
               PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
               Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
               productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
               List<AssetWS> assetWSs = new ArrayList<>();
               List<AssetWS> assetWSs1 = new ArrayList<>();

               Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "8541236548",
                        "asset-18", "8541236548");
               
               Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "9632541258",
                        "asset-19", "9632541258");

                   assetWSs.add(api.getAsset(asset1));
                   assetWSs1.add(api.getAsset(asset2));

                   orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                   
                   orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);
                   
                
                   //bill run
                   Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                   
                   //Validate Customer NID 
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   Date nid = Date.from(LocalDate.of(2019, 5, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                   
                   updateRunDateBillingProcessConfiguration(runDate);
                   executeBillingProcessByRunDate(runDate);
                   
                   //Validate invoice total amount and balance amount
                   List<String> descriptionList = new ArrayList<String>();
                   Integer number = 1;
                   Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS.getId(), number);
                   InvoiceWS invoices = api.getInvoiceWS(invoiceWs1[0]);
                   BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices.getInvoiceLines()){
                       if(ildto.getTypeId()==1){
                           invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                           descriptionList.add(ildto.getDescription());
                       }
                   }
                   assertTrue( descriptionList.contains(optusPlanDescription+" Period from 05/31/2019 to 06/29/2019"));
                   String totalAmt = "15.0000000000" , balanceAmt = "15.0000000000"; BigDecimal invoiceLine = new BigDecimal("14.9999850000");
                   validateInvoiceLineAmt(invoices, invoiceLineAmt1, totalAmt, balanceAmt, invoiceLine);
                   
                   Date nid1 = Date.from(LocalDate.of(2019, 6, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   
                   Date runDate11 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nid1);
                   
                   //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid1, spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                   
                   updateRunDateBillingProcessConfiguration(runDate11);
                   executeBillingProcessByRunDate(runDate11);
                   
                   List<String> descriptionList1 = new ArrayList<String>();
                   BigDecimal invoiceLineAmt2 = new BigDecimal(0);
                   Integer number1 = 1;
                   Integer[] invoiceWs2 = api.getLastInvoices(spcTestUserWS.getId(), number1);
                   InvoiceWS invoices1 = api.getInvoiceWS(invoiceWs2[0]);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto1 : invoices1.getInvoiceLines()){
                       if(ildto1.getTypeId()==1){
                           invoiceLineAmt2 = invoiceLineAmt2.add(new BigDecimal(ildto1.getAmount()));
                           descriptionList1.add(ildto1.getDescription());
                       }
                   }
                   assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 05/31/2019 to 06/29/2019"));
                   assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 06/30/2019 to 07/30/2019"));
                   String totalAmt2 = "45.0000000000" , balanceAmt2 = "30.0000000000"; BigDecimal invoiceLine2 = new BigDecimal("29.9999700000");
                   validateInvoiceLineAmt(invoices1, invoiceLineAmt2, totalAmt2, balanceAmt2, invoiceLine2);
                   
                   Date nid2 = Date.from(LocalDate.of(2019, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   
                   Date runDate22 = com.sapienter.jbilling.server.util.Util.getStartOfDay(nid2);
                   
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid2,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid2,orderWs.getNextBillableDay());
                   
                   updateRunDateBillingProcessConfiguration(runDate22);
                   executeBillingProcessByRunDate(runDate22);
                   
                   List<String> descriptionList2 = new ArrayList<String>();
                   BigDecimal invoiceLineAmt3 = new BigDecimal(0);
                   Integer number2 = 1;
                   Integer[] invoiceWs3 = api.getLastInvoices(spcTestUserWS.getId(), number2);
                   InvoiceWS invoices3 = api.getInvoiceWS(invoiceWs3[0]);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto1 : invoices3.getInvoiceLines()){
                       if(ildto1.getTypeId()==1){
                           invoiceLineAmt3 = invoiceLineAmt3.add(new BigDecimal(ildto1.getAmount()));
                           descriptionList2.add(ildto1.getDescription());
                       }
                   }
                   assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 06/30/2019 to 07/30/2019"));
                   assertTrue( descriptionList2.contains(optusPlanDescription+" Period from 07/31/2019 to 08/30/2019"));
                   String totalAmt3 = "75.0000000000" , balanceAmt3 = "30.0000000000"; BigDecimal invoiceLine3 = new BigDecimal("29.9999700000");
                   validateInvoiceLineAmt(invoices3, invoiceLineAmt3, totalAmt3, balanceAmt3, invoiceLine3);
                   
                 //Validate Customer NID and Order NBD
                   Date nid3 = Date.from(LocalDate.of(2019, 8, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid3,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid3,orderWs.getNextBillableDay());
                 
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    
    /**
     * 
     * Create a customer with NID - 28th January 2021
     * Create 2 Prepaid Prorated Subscription Order with Active Since Date - 28th January 2021
     * Invoice generated for 28th January 2021
     * Set active until as 26th Feb 2021
     * Invoice generated for 28th Feb 2021
     */
    @Test(priority = 7)
    public void testPeriodForPrepaidOrderwithCancelOrder28MonthlyCycle () {
        try {
            LocalDate nextInvoiceDate = LocalDate.of(2021, 1, 28);  
            Date nextInvoiceDateNew = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            testBuilder.given(envBuilder -> {
                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER7, 
                        nextInvoiceDateNew,
                        "", 
                        CUSTOMER_TYPE_VALUE_PRE_PAID,
                        AUSTRALIA_POST,CC);
                // Update customer NID
                logger.debug("Update customer NID: {}", nextInvoiceDateNew);
                spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew);
                api.updateUser(spcTestUserWS);
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
            

               //optus
               PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
               Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
               productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
               List<AssetWS> assetWSs = new ArrayList<>();
               List<AssetWS> assetWSs1 = new ArrayList<>();

               Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "2514236529",
                        "asset-20", "2514236529");
               
               Integer asset2 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5412789635",
                        "asset-21", "5412789635");

                   assetWSs.add(api.getAsset(asset1));
                   assetWSs1.add(api.getAsset(asset2));

                   orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);
                   
                   orderId1 = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null, 
                            MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs1, optusPlanId);

                   // create invoice:
                   Integer[] invoicess = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew, null, null, false);
                   assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess)));
                   logger.debug("Plan Order invoice created {}", invoicess[0]);
                   InvoiceWS invoiceWS = api.getInvoiceWS(invoicess[0]);
                   
                   LocalDate nextInvoiceDate1 = LocalDate.of(2021, 2, 28);  
                   Date nextInvoiceDateNew1 = Date.from(nextInvoiceDate1.atStartOfDay(ZoneId.systemDefault()).toInstant());
                   
                // Update NID 1st March 2021
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   spcTestUserWS.setNextInvoiceDate(nextInvoiceDateNew1);
                   api.updateUser(spcTestUserWS);
                   logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                   
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   Date nid = Date.from(LocalDate.of(2021, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   assertEquals("Customer NID should be match", nid,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid,orderWs.getNextBillableDay());
                   
                 //Validate invoice total amount and balance amount
                   List<String> descriptionList = new ArrayList<String>();
                   BigDecimal invoiceLineAmt = new BigDecimal(0);
                   Integer number = 1;
                   Integer[] invoiceWs = api.getLastInvoices(spcTestUserWS.getId(), number);
                   InvoiceWS invoices = api.getInvoiceWS(invoiceWs[0]);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices.getInvoiceLines()){
                       if(ildto.getTypeId()==1){
                           invoiceLineAmt = invoiceLineAmt.add(new BigDecimal(ildto.getAmount()));
                           descriptionList.add(ildto.getDescription());
                       }
                   }
                   assertTrue( descriptionList.contains(optusPlanDescription+" Period from 01/28/2021 to 02/27/2021"));
                   String totalAmt = "15.0000000000" , balanceAmt = "15.0000000000" ; BigDecimal invoiceLine = new BigDecimal("14.9999850000");
                   validateInvoiceLineAmt(invoices, invoiceLineAmt, totalAmt, balanceAmt, invoiceLine);
                   
                   //cancel order 26th Feb using API
                   Date activeUntilDate = Date.from(LocalDate.of(2021, 2, 26).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                   cancelOrderInfo.setOrderId(orderId1);
                   cancelOrderInfo.setActiveUntil(activeUntilDate);
                   logger.debug("Canceling order with active until date equals to order active since");
                   api.cancelServiceOrder(cancelOrderInfo);
                   
                // create invoice:
                   Integer[] invoicess1 = api.createInvoiceWithDate(spcTestUserWS.getId(), nextInvoiceDateNew1, null, null, false);
                   assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoicess1)));
                   logger.debug("Plan Order invoice created {}", invoicess1[0]);
                   InvoiceWS invoiceWS1 = api.getInvoiceWS(invoicess[0]);
                   Date nid1 = Date.from(LocalDate.of(2021, 3, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
                   // Update NID 
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   spcTestUserWS.setNextInvoiceDate(nid1);
                   api.updateUser(spcTestUserWS);
                   logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());
                   
                 //Validate Customer NID and Order NBD
                   spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                   assertEquals("Customer NID should be match", nid1,spcTestUserWS.getNextInvoiceDate());
                   orderWs = api.getOrder(orderId);
                   assertEquals("Order NDB should be match", nid1,orderWs.getNextBillableDay());
                   
                 //Validate invoice total amount and balance amount
                   List<String> descriptionList1 = new ArrayList<String>();
                   BigDecimal invoiceLineAmt1 = new BigDecimal(0);
                   Integer number1 = 1;
                   Integer[] invoiceWs1 = api.getLastInvoices(spcTestUserWS.getId(), number1);
                   InvoiceWS invoices1 = api.getInvoiceWS(invoiceWs1[0]);
                   for(com.sapienter.jbilling.server.entity.InvoiceLineDTO ildto : invoices1.getInvoiceLines()){
                       if(ildto.getTypeId()==1){
                           invoiceLineAmt1 = invoiceLineAmt1.add(new BigDecimal(ildto.getAmount()));
                           descriptionList1.add(ildto.getDescription());
                       }
                   }
                   assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 01/28/2021 to 02/26/2021"));
                   assertTrue( descriptionList1.contains(optusPlanDescription+" Period from 02/28/2021 to 03/27/2021"));
                   String totalAmt1 = "44.5200000000", balanceAmt1 = "29.5200000000" ; BigDecimal invoiceLine1 = new BigDecimal("29.5160995173");
                   validateInvoiceLineAmt(invoices1, invoiceLineAmt1, totalAmt1, balanceAmt1, invoiceLine1);
                   
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
            assertNotNull(ORDER_CREATION_ASSERT, orderId);
            assertNotNull(ORDER_CREATION_ASSERT, orderId1);
            UserWS user = api.getUserWS(spcTestUserWS.getId());
            logger.debug("## Customer Id {}", user.getCustomerId());

            OrderWS subscriptionOrder = api.getOrder(orderId);
            OrderWS subscriptionOrder1 = api.getOrder(orderId1);
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            assertNotNull(ORDER_ASSERT, subscriptionOrder1);
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planWS.getId());
            assertTrue("Plan usage pools mismatch", usagePools.length == 4);
        });
      }finally{
            assetWSs.clear();
            assetWSs1.clear();
            clearTestDataForUser(spcTestUserWS.getId());
      }
    }
    
    private void validateInvoiceLineAmt(InvoiceWS invoices1, BigDecimal invoiceLineAmt1, String totalAmt, String balanceAmt, BigDecimal invoiceLine) {
        assertEquals("Invoice Amount should be match", invoiceLine ,invoiceLineAmt1);
        assertEquals("Invoice Amount should be match", totalAmt ,invoices1.getTotal());
        assertEquals("Invoice Amount should be match", balanceAmt ,invoices1.getBalance());
    }
    
    private void updateRunDateBillingProcessConfiguration(Date runDate) {
        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        logger.debug("Billing runDate: {}", runDate);
        config.setNextRunDate(runDate);
        config.setPeriodUnitId(3); // 3-Daily , 1-monthly 
        config.setRetries(null);
        config.setDaysForRetry(new Integer(1));
        config.setGenerateReport(new Integer(0));
        config.setAutoPaymentApplication(new Integer(0));
        config.setDfFm(new Integer(0));
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(new Integer(1));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(1));
        config.setOnlyRecurring(new Integer(1));
        config.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());

        api.createUpdateBillingProcessConfiguration(config);
    }

    private void executeBillingProcessByRunDate(Date runDate) {
        try {
            logger.debug("Billing runDate: {}", runDate);
            api.triggerBilling(runDate);

            BillingProcessWS billingProcess = api.getBillingProcess(api.getLastBillingProcess());
            assertEquals("Billing process configuration failed", runDate, billingProcess.getBillingDate());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void resetBillingProcessConfiguration() {
        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setGenerateReport(new Integer(1));
        config.setPeriodUnitId(1); // 3-Daily , 1-monthly 
        config.setRetries(new Integer(0));
        config.setDaysForRetry(new Integer(1));
        config.setGenerateReport(new Integer(1));
        config.setAutoPaymentApplication(new Integer(1));
        config.setDfFm(new Integer(0));
        config.setDueDateUnitId(Constants.PERIOD_UNIT_MONTH);
        config.setDueDateValue(new Integer(1));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(1));
        config.setOnlyRecurring(new Integer(1));
        config.setProratingType(ProratingType.PRORATING_AUTO_OFF.getProratingType());

        api.createUpdateBillingProcessConfiguration(config);
    }
    
    @AfterClass
    public void tearDown() {
        resetBillingProcessConfiguration();
    }
}
