package com.sapienter.jbilling.server.spc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.util.Arrays;

import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.spc.util.Plan;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.order.OrderLineWS;

import org.apache.commons.io.FileUtils;

import java.io.*;


@Test(groups = "agl", testName = "agl.DuplicateAssetIdentifierAglTest")
public class DuplicateAssetIdentifierAglTest extends SPCBaseConfiguration {

    //String assetIdentifier = "04"+randomLong(10000000L, 99999999L);
    String assetIdentifier = "JBAGL20000";
    String assetIdentifier1 = "JBAGL20000_";
    Integer userId;
    Integer assetId;
    Integer assetId1;
    Integer customerId;
    Integer planId;
    Integer firstPlanOrderId;
    Integer secondPlanOrderId;
    BigDecimal dataQuantity = new BigDecimal("11.00");  //  Data in MB
    String quantityInKB;
    Integer firstMediatedOrderId;
    Integer secondMediatedOrderId;  
    Integer thirdMediatedOrderId;       
    String planCode = "SPCMO-200";
    String planType ="Optus";
    String planCategory = "Mobile";
    String planDescription="Optus Budget - $10";
    String planOrigin="SPC";
    String planRating="SPC-OM-Plan-Rating-1";
    String price="9.0909";
    String dataBoostQuantity="1024";            // Quantity In MegaBytes
    String mainPoolQuantity="2147483648";       // Quantity In bytes
    List<Integer> invoicesGenerated = new ArrayList<Integer>();
    String userName = "Test-"+randomLong(100L, 999L);
    String assetName = "Asset-"+randomLong(100L, 999L);
    String assetName1 = "Asset-"+randomLong(100L, 999L);
    LocalDate baseDate =  LocalDate.now().withDayOfMonth(1).minusMonths(2);

    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

    private static final String MEDIATION_FILE_PREFIX                    = "RESELL_";

    private static final String OPTUS_MOBILE_FORMAT_050                   = "50%s                       616695600005050200050502%s183502202105140"
            + "                    21100000000002TWEED HEADS TWEED HEADS 00000000000TD050CON  090400000000002 1                           "
            + "G1999989    V000000000020210514095002000000000%s00000000000000000000000000000000000000000200000000000     "
            + "                                                                                     003789303480";

    Date nextInvoiceDate = null;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {        
        logger.debug("DuplicateAssetIdentifierAglTest.initialize started....",testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        logger.debug("DuplicateAssetIdentifierAglTest.initialize completed....",testBuilder);
    }

    @AfterClass
    public void afterTests() {

        logger.debug("DuplicateAssetIdentifierAglTest.afterTests started.....");
        logger.debug("cleaning the data of user created in test runs....");
        clearTestDataForUser(userId);
        logger.debug("completed the cleaning process....");
    }

    /**
     * 
     * This method executes below steps and the asserts
     * Create a User with NID 1st of Month with AGL invoice template
     * Create an asset
     * Create a plan
     * Create a plan order with active since 1st of Month
     * Create an Invoice for 1st of Month
     * Change NID to the next Month.
     * Process the CDR with event date 10th on Month
     * Create a duplicate asset(via Database update) with the same identifier as the first asset.
     * Create an Invoice for 1st of the next Month.
     * The invoice is created without any errors even if there are 2 assets with the same identifier.
     * 
     * */
    @Test(priority = 1)
    void test01DuplicateAssetIdentifier() {

        testBuilder.given(envBuilder -> {
            api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            quantityInKB = prependZero(dataQuantity.multiply(new BigDecimal("1024")).setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString(), 9);

            nextInvoiceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());            
            UserWS spcTestUserWS = getSPCTestUserWS(
                    envBuilder,
                    userName, 
                    nextInvoiceDate,
                    "agl_invoice", 
                    CUSTOMER_TYPE_VALUE_POST_PAID,
                    AUSTRALIA_POST,CC);

            userId = spcTestUserWS.getId(); 
            assertNotNull("User Creation Failed", userId);
            logger.debug("User created, user id is : {}",  userId);         
            customerId = spcTestUserWS.getCustomerId();         
            logger.debug("User id is {} and Customer id is : {}", userId , customerId);         
            UserWS userws = api.getUserWS(userId);          
            userws.setNextInvoiceDate(nextInvoiceDate);         
            api.updateUser(userws);         
            logger.debug("User {} updated with NID as : {}", userId, nextInvoiceDate);      
            
            //api.updateCustomerInvoiceDesign( userId, "agl_invoice");

            planId = CreatePlanUtility.createPlan(api, planCode, planType, planCategory, planDescription, planOrigin, 
                    planRating, "x", new BigDecimal(price), true, new BigDecimal(mainPoolQuantity), 3, new BigDecimal(dataBoostQuantity), new HashMap<String, String>());

            logger.debug("Plan created with Id : {}", planId);
            PlanWS planWS = api.getPlanWS(planId);
            assertNotNull("Plan creation failed", planWS);
            List<AssetWS> assetWSs = new ArrayList<>();
            List<AssetWS> assetWSs1 = new ArrayList<>();
            productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);            
            assetId = buildAndPersistAssetWithServiceId(envBuilder,
                    getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                    getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                    assetIdentifier,
                    assetName, assetIdentifier);
            
            assertNotNull("Asset Creation Failed", assetId);
            logger.debug("Asset created with Id : {}", assetId);            
            assetWSs.add(api.getAsset(assetId));            
            
            assetId1 = buildAndPersistAssetWithServiceId(envBuilder,
                    getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), 
                    getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                    assetIdentifier1,
                    assetName1, assetIdentifier1);

            assertNotNull("Asset Creation Failed", assetId1);
            logger.debug("Asset created with Id : {}", assetId1);            
            assetWSs1.add(api.getAsset(assetId1));
           
            Date activeSinceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());            
            logger.debug("Active since for the order is {}", activeSinceDate);
            firstPlanOrderId =  createOrderWithAsset("TestOrder", userId, activeSinceDate, null, 
                    2, 1, true, productQuantityMap , assetWSs, planId, assetIdentifier);
            assertNotNull("First subscription order Creation Failed", firstPlanOrderId);
            logger.debug("First Subscription order id {} for user {}", firstPlanOrderId, userId);           
            Integer[] invoices = api.createInvoiceWithDate(userId, nextInvoiceDate, null, null, false);         
            for(Integer i : invoices) {             
                logger.debug("Invoice Id created is {} for user {}", i, userId);
                invoicesGenerated.add(i);
            }                       
            nextInvoiceDate = Date.from(baseDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());           
            logger.debug("Updating the Next Invoice date as {} for user {}", nextInvoiceDate, userId);              
            userws.setNextInvoiceDate(nextInvoiceDate);         
            api.updateUser(userws);         
            logger.debug("Next invoice date for user {} updated now", userId);

        }).validate((testEnv, testEnvBuilder) -> {

            logger.debug("Creating Mediation file ....");           
            String eventDate = getDateFormatted(baseDate.plusDays(10), "yyyyMMdd");         
            logger.debug("Event date set is : {} ", eventDate);         
            String cdrLine = String.format(OPTUS_MOBILE_FORMAT_050, assetIdentifier, eventDate, quantityInKB);          
            logger.debug("cdr line : {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".STREAM", null,
                    Arrays.asList(cdrLine));            
            logger.debug("Mediation file created {}", cdrFilePath);
            UUID mediationProcessId = 
                    api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));           
            assertNotNull("Mediation trigger is failed", mediationProcessId);           
            logger.debug("Mediation ProcessId {}", mediationProcessId);         
            pauseUntilMediationCompletes(20, api);          
            logger.debug("Mediation Process completed");

        }).validate((testEnv, testEnvBuilder) -> {

            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());

            logger.debug("Mediation Process {}", mediationProcess);

            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable ", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error ", Integer.valueOf(0), mediationProcess.getErrors());

            OrderWS order = api.getLatestOrder(userId);
            assertNotNull("Mediation Order created", order);
            firstMediatedOrderId = order.getId();
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediated Order line ", 1, order.getOrderLines().length);
            assertEquals("Mediated Order Amount ", new BigDecimal("0.00"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            logger.debug("The optus mobile data item id is : {}",OM_MOBILE_DATA_ITEM_ID);
            OrderLineWS callLine = getLineByItemId(order, OM_MOBILE_DATA_ITEM_ID);
            assertNotNull("Optus Mobile Usage Item not found", callLine);
            assertEquals("Optus Mobile Item Line Quantity ", dataQuantity,
                    callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Optus Mobile Item Line Amount ", new BigDecimal("0.00"),
                    callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            CustomerUsagePoolWS[] customerUsagePoolWS =   api.getCustomerUsagePoolsByCustomerId(customerId);

            BigDecimal totalQuantity = new BigDecimal("0.0");
            BigDecimal remainingQuantity = new BigDecimal("0.0");
            assertNotNull("Customer usage pool data",customerUsagePoolWS);          
            for (CustomerUsagePoolWS cpws : customerUsagePoolWS) {
                if(firstPlanOrderId.equals( cpws.getOrderId()) 
                        && com.sapienter.jbilling.server.util.Util.getEpochDate().before(cpws.getCycleStartDate())) {                   
                    totalQuantity = totalQuantity.add(cpws.getInitialQuantityAsDecimal());
                    remainingQuantity = remainingQuantity.add(cpws.getQuantityAsDecimal());
                }
            }
            BigDecimal quantityUtilized = totalQuantity.subtract(remainingQuantity);
            logger.debug("For plan order {} , Quantity utilized is : {}",firstPlanOrderId,quantityUtilized);

            assertEquals("Quantity utilized from usage pool quantity should be ", dataQuantity.setScale(2, BigDecimal.ROUND_HALF_UP),
                    quantityUtilized.setScale(2, BigDecimal.ROUND_HALF_UP));
            
            updateAssetIdentifier(assetIdentifier, assetIdentifier1);
            
            assertEquals("Number of Assets for the Identifier count ", 2, getNoOfAssetsForIdentifier(assetIdentifier));
            
            Integer[] invoices = api.createInvoiceWithDate(userId, nextInvoiceDate, null, null, false);         
            for(Integer invoiceId : invoices) {             
                logger.debug("Next Invoice Id created is {} for user {}", invoiceId , userId);
                
                byte[] pdfBytes = api.getPaperInvoicePDF(invoiceId);
                try{
                    FileUtils.writeByteArrayToFile(new File("/tmp/pdffile"), pdfBytes);
                }catch(IOException io){
                    
                }
                invoicesGenerated.add(invoiceId);
            }
        });
    }

}
