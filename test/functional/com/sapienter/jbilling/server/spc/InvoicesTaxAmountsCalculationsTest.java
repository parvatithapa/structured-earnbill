package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.math.BigDecimal;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.spc.util.Plan;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;

/**
 * 
 * This class contains the test methods to test the fix done for ticket
 * JBSPC-881 The asserts are added around the order line, invoice lines and JMR
 * amounts and the tax amounts
 * 
 * */
@Test(groups = "agl", testName = "agl.InvoicesTaxAmountsCalculationsTest")
public class InvoicesTaxAmountsCalculationsTest extends SPCBaseConfiguration {

    String assetIdentifier1 = "04" + randomLong(10000000L, 99999999L);
    String assetIdentifier2 = "61" + randomLong(10000000L, 99999999L);
    Integer userId1;
    Integer userId2;
    Integer assetId1;
    Integer assetId2;
    Integer customerId;
    Integer planId;
    Integer firstPlanOrderId;
    Integer secondPlanOrderId;
    String callString = "S4NMR";
    Integer firstMediatedOrderId;
    Integer secondMediatedOrderId;
    String planCode = "SPCMO-881";
    String planType = "Optus";
    String planCategory = "Mobile";
    String planDescription = "Optus Budget - $10";
    String planOrigin = "SPC";
    String planRating = "SPC-OM-Plan-Rating-1";
    String price = "9.0909";
    String dataBoostQuantity = "1024"; // Quantity In MegaBytes
    String mainPoolQuantity = "2147483648"; // Quantity In bytes
    List<Integer> invoicesGenerated = new ArrayList<Integer>();
    String userName1 = "Test-" + randomLong(100L, 999L);
    String userName2 = "TestT-" + randomLong(100L, 999L);
    String assetName1 = "Asset-" + randomLong(100L, 999L);
    String assetName2 = "AssetT-" + randomLong(100L, 999L);
    UUID mediationProcessId = null;
    UUID mediationProcessId1 = null;
    LocalDate baseDate = LocalDate.now().withDayOfMonth(1).minusMonths(2);
    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

    Integer OM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID = null;
    String OM_MOBILE_TO_FIXED_CALLS_PRODUCT_CODE = "om_mobile_to_fixed_calls";
    Integer TM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID = null;
    String TM_MOBILE_TO_FIXED_CALLS_PRODUCT_CODE = "tm_voicemail";

    private static final String TELSTRA_PLAN = "SPCMT-881";
    private static final String TELSTRA_MEDIATION_FILE_PREFIX = "Reseller42_";
    private static final String MEDIATION_FILE_PREFIX = "RESELL_";

    private static final String OPTUS_MOBILE_FORMAT_010_CDR = "10%s                       000000000000000616695600005050200050502%s%s000100000001201811260411000321           "
            + "1110374900S1445741E1000000000000000000000062A            DEPOSIT        "
            + "Bourke St      1120181126091351000003000000000001000000524%s0904000000000" + "00000000000001011                  ";

    private static final String TELSTRA_MOBILE_CHARGEABLE_CDR = "P1,%s,HHHHH,E.164,1,0,505015602749590,IMSI,cjprvccn01,HHHHH,E.164,61141,E.164,1,0,CS,IP,61,1001,HHHHH,61141,E.164,1,N,B,500,36000,%s%s,4222,7852860,20090618,,,,,,,,,#,"
            + "P2,1,5,H,,,,,,,33,,,664,Sec,,,,1411,,Diverted Call,ALL,I,,36000,%s%s,GTEL,OUT,GSZN000226,CAALL,S,N,GSM,1,0,,,,,,,#,"
            + "P3,Sec,4,Sec,4,0.000000,AUD,0.000000,1,5,N,DUR,BASE,I,TWTT000001,36000,%s%s,0.000000,0.000000,1,0,0,,,,,,,,#,P4,U,S,Sec,4,Sec,4,0.000000,AUD,0,4222100,D,,,,,,,,#,";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {
        logger.debug("OptusMobileDataMediationTest.initialize started....", testBuilder);
        if (null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        logger.debug("OptusMobileDataMediationTest.initialize completed....", testBuilder);
    }

    @AfterClass
    public void afterTests() {

        logger.debug("OptusMobileDataMediationTest.afterTests started.....");
        logger.debug("cleaning the data of user created in test runs....");
        clearTestDataForUser(userId1);
        clearTestDataForUser(userId2);
        logger.debug("completed the cleaning process....");
    }

    /**
     * 
     * This method executes below steps and the asserts Create a User with NID
     * 1st of Month Create an asset Create a plan Create a plan order with
     * active since 1st of Month Create an Invoice for 1st of Month Process the
     * mediation file with 70 Optus Mobile type 10 CDR with event date 10th on
     * Month Generate the invoice for next month validate the tax invoice amount
     * jmr.rated_price + jmr.tax_amount = jmr.rated_price_with_tax
     * sum(jmr.rated_price) = ol.amount sum(jmr.rated_price_with_tax) =
     * il.amount sum(jmr.rated_price) = il.gross_amount il.amount -
     * il.gross_amount = il.tax_amount il.amount = il.gross_amount + (tax_rate *
     * il.gross_amount) ol.amount = il.gross_amount
     *
     * */
    @Test(priority = 1)
    void test01OptusMobileMediationUpload() {

        testBuilder
                .given(envBuilder -> {
                    api = testBuilder.getTestEnvironment().getPrancingPonyApi();

                    Date nextInvoiceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    UserWS spcTestUserWS = getSPCTestUserWS(envBuilder, userName1, nextInvoiceDate, "", CUSTOMER_TYPE_VALUE_POST_PAID,
                            AUSTRALIA_POST, CC);

                    userId1 = spcTestUserWS.getId();
                    assertNotNull("User Creation Failed", userId1);
                    logger.debug("User created, user id is : {}", userId1);

                    planId = CreatePlanUtility.createPlan(api, planCode, planType, planCategory, planDescription, planOrigin, planRating,
                            "x", new BigDecimal(price), true, new BigDecimal(mainPoolQuantity), 3, new BigDecimal(dataBoostQuantity));

                    logger.debug("Plan created with Id : {}", planId);
                    PlanWS planWS = api.getPlanWS(planId);
                    assertNotNull("Plan creation failed", planWS);
                    List<AssetWS> assetWSs = new ArrayList<>();
                    productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                    assetId1 = buildAndPersistAssetWithServiceId(envBuilder,
                            getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                            getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), assetIdentifier1,
                            assetName1, assetIdentifier1);

                    assertNotNull("Asset Creation Failed", assetId1);
                    logger.debug("Asset created with Id : {}", assetId1);
                    assetWSs.add(api.getAsset(assetId1));
                    Date activeSinceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Active since for the order is {}", activeSinceDate);
                    firstPlanOrderId = createOrderWithAsset("TestOrder", userId1, activeSinceDate, null, 2, 1, true, productQuantityMap,
                            assetWSs, planId, assetIdentifier1);
                    assertNotNull("First subscription order Creation Failed", firstPlanOrderId);
                    logger.debug("First Subscription order id {} for user {}", firstPlanOrderId, userId1);
                    Integer[] invoices = api.createInvoiceWithDate(userId1, nextInvoiceDate, null, null, false);
                    for (Integer invoiceId : invoices) {
                        logger.debug("Invoice Id created is {} for user {}", invoiceId, userId1);
                        invoicesGenerated.add(invoiceId);
                    }
                    nextInvoiceDate = Date.from(baseDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Updating the Next Invoice date as {} for user {}", nextInvoiceDate, userId1);
                    UserWS userws = api.getUserWS(userId1);
                    userws.setNextInvoiceDate(nextInvoiceDate);
                    api.updateUser(userws);
                    logger.debug("Next invoice date for user {} updated now", userId1);
                    OM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID = getItemIdByCode(testBuilder.getTestEnvironment(),
                            OM_MOBILE_TO_FIXED_CALLS_PRODUCT_CODE);

                })
                .validate(
                        (testEnv, testEnvBuilder) -> {

                            logger.debug("Creating Mediation file ....");
                            List<String> cdrList = new ArrayList();
                            for (int i = 0; i < 70; i++) {
                                String eventDate = getDateFormatted(baseDate.plusDays(10).atStartOfDay().plusSeconds(100 * i),
                                        "yyyyMMddHHmmss");
                                String duration = prependZero(randomLong(10L, 59L) + "", 6);
                                logger.debug("Event date set is : {} ", eventDate);
                                cdrList.add(String.format(OPTUS_MOBILE_FORMAT_010_CDR, assetIdentifier1, eventDate, duration, callString));
                            }
                            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".STREAM", null,
                                    cdrList);
                            logger.debug("Mediation file created {}", cdrFilePath);
                            mediationProcessId = api.triggerMediationByConfigurationByFile(
                                    getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                            assertNotNull("Mediation trigger is failed", mediationProcessId);
                            logger.debug("Mediation ProcessId {}", mediationProcessId);
                            pauseUntilMediationCompletes(20, api);
                            logger.debug("Mediation Process completed");

                        })
                .validate((testEnv, testEnvBuilder) -> {

                    MediationProcess mediationProcess = api.getMediationProcess(mediationProcessId);

                    logger.debug("Mediation Process {}", mediationProcess);

                    assertEquals("Mediation Done And Billable ", Integer.valueOf(70), mediationProcess.getDoneAndBillable());
                    assertEquals("Mediation Done And Not Billable ", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                    assertEquals("Mediation Error ", Integer.valueOf(0), mediationProcess.getErrors());

                    Date nextInvoiceDate = Date.from(baseDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Integer[] invoices = api.createInvoiceWithDate(userId1, nextInvoiceDate, null, null, false);
                    Integer invoiceId = null;
                    for (Integer invoice_id : invoices) {
                        logger.debug("Invoice Id created is {} for user {}", invoice_id, userId1);
                        invoiceId = invoice_id;
                        invoicesGenerated.add(invoiceId);
                    }

                    nextInvoiceDate = Date.from(baseDate.plusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Updating the Next Invoice date as {} for user {}", nextInvoiceDate, userId1);
                    UserWS userws = api.getUserWS(userId1);
                    userws.setNextInvoiceDate(nextInvoiceDate);
                    api.updateUser(userws);

                    // Getting JMR level amounts
                        JbillingMediationRecord[] JmrEvents = api.getMediationEventsForInvoice(invoiceId);
                        logger.debug("The Jbilling mediation events arrat size is {} ", JmrEvents.length);
                        BigDecimal ratedPrice = new BigDecimal("0.0");
                        BigDecimal ratedPriceWithTax = new BigDecimal("0.0");
                        BigDecimal taxAmount = new BigDecimal("0.0");
                        for (JbillingMediationRecord jmr : JmrEvents) {
                            ratedPrice = ratedPrice.add(jmr.getRatedPrice());
                            taxAmount = taxAmount.add(jmr.getTaxAmount());
                            ratedPriceWithTax = ratedPriceWithTax.add(jmr.getRatedPriceWithTax());
                            assertTrue("Tax amount is invalid ", jmr.getTaxAmount().compareTo(BigDecimal.ZERO) > 0);
                        }

                        // Getting the Mediated order level amounts
                        OrderWS latestOrder = api.getLatestOrder(userId1);
                        firstMediatedOrderId = latestOrder.getId();
                        assertTrue("Mediated Order is not created", latestOrder.getIsMediated());

                        BigDecimal orderLineAmount = null;

                        for (OrderLineWS orderLine : latestOrder.getOrderLines()) {
                            logger.debug("The item id is {} ", orderLine.getItemId());
                            logger.debug("The item id to check is  {} ", OM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID);
                            if (OM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID.equals(orderLine.getItemId())) {
                                orderLineAmount = new BigDecimal(orderLine.getAmount());
                            }
                        }
                        assertTrue("Order line amount is not charged ", orderLineAmount.compareTo(BigDecimal.ZERO) > 0);

                        // Getting the Invoice levet amounts
                        InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
                        assertTrue("Plan order is not included in the invoice ",
                                Arrays.asList(invoiceWS.getOrders()).contains(firstPlanOrderId));
                        assertTrue("Mediated order is not included in the invoice ",
                                Arrays.asList(invoiceWS.getOrders()).contains(firstMediatedOrderId));
                        InvoiceLineDTO[] invoiceLines = invoiceWS.getInvoiceLines();
                        BigDecimal invoiceLineAmount = null;
                        BigDecimal invoiceLineTaxAmount = null;
                        BigDecimal invoiceLineTaxRate = null;
                        BigDecimal invoiceLineGrossAmount = null;
                        for (InvoiceLineDTO line : invoiceLines) {
                            logger.debug("The invoice line item id is {} ", line.getItemId());
                            if (OM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID.equals(line.getItemId())) {
                                invoiceLineAmount = new BigDecimal(line.getAmount());
                                invoiceLineTaxAmount = new BigDecimal(line.getTaxAmount());
                                invoiceLineTaxRate = new BigDecimal(line.getTaxRate());
                                invoiceLineGrossAmount = new BigDecimal(line.getGrossAmount());
                            }
                        }

                        logger.debug("JMR rated price {} ", ratedPrice.toPlainString());
                        logger.debug("JMR tax amount {} ", taxAmount.toPlainString());
                        logger.debug("JMR rated price with tax {} ", ratedPriceWithTax.toPlainString());
                        logger.debug("Invoice line amount {} ", invoiceLineAmount.toPlainString());
                        logger.debug("Invoice line tax amount {} ", invoiceLineTaxAmount.toPlainString());
                        logger.debug("Invoice line tax rate  {} ", invoiceLineTaxRate.toPlainString());
                        logger.debug("Invoice line gross amount  {} ", invoiceLineGrossAmount.toPlainString());

                        // Checking assets on all the amounts
                        assertEquals(
                                "SUM of rated price and Tax Amount in Jbilling Mediation Record is not matching with rated price with tax",
                                ratedPrice.add(taxAmount).setScale(4, BigDecimal.ROUND_HALF_UP),
                                ratedPriceWithTax.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Rated price from Jbilling Mediation Record table does not matches Order Line Amount",
                                ratedPrice.setScale(4, BigDecimal.ROUND_HALF_UP), orderLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Rated price with tax from Jbilling Mediation Record table does not matches Invoice Line Amount",
                                ratedPriceWithTax.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Rated price from Jbilling Mediation Record table does not matches Invoice Line Amount",
                                ratedPrice.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineGrossAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals(
                                "Invoice line amount minus invoice line gross amount from Invoice line table does not matches Invoice Line Tax Amount",
                                invoiceLineAmount.subtract(invoiceLineGrossAmount).setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineTaxAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals(
                                "Invoice Line amount From Invoice line table does not match with Invoice line gross amount + invoice line tax rate * invoice line gross amount",
                                invoiceLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineGrossAmount.add(
                                        invoiceLineTaxRate.multiply(invoiceLineGrossAmount).divide(new BigDecimal("100"))).setScale(4,
                                        BigDecimal.ROUND_HALF_UP));
                        assertEquals("Order line amount from order line table is not matching with invoice line gross amount",
                                orderLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineGrossAmount.setScale(4, BigDecimal.ROUND_HALF_UP));

                    });
    }

    /**
     * 
     * This method executes below steps and the asserts Create a User with NID
     * 1st of Month Create an asset Create a plan Create a plan order with
     * active since 1st of Month Create an Invoice for 1st of Month Process the
     * mediation file with 70 Telstra Mobile type 10 CDR with event date 10th on
     * Month Generate the invoice for next month validate the tax invoice amount
     * jmr.rated_price + jmr.tax_amount = jmr.rated_price_with_tax
     * sum(jmr.rated_price) = ol.amount sum(jmr.rated_price_with_tax) =
     * il.amount sum(jmr.rated_price) = il.gross_amount il.amount -
     * il.gross_amount = il.tax_amount il.amount = il.gross_amount + (tax_rate *
     * il.gross_amount) ol.amount = il.gross_amount
     *
     * */
    @Test(priority = 2)
    void test01TelstraMobileMediationUpload() {

        testBuilder
                .given(envBuilder -> {
                    api = testBuilder.getTestEnvironment().getPrancingPonyApi();

                    Date nextInvoiceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    UserWS spcTestUserWS = getSPCTestUserWS(envBuilder, userName2, nextInvoiceDate, "", CUSTOMER_TYPE_VALUE_POST_PAID,
                            AUSTRALIA_POST, CC);

                    userId2 = spcTestUserWS.getId();
                    assertNotNull("User Creation Failed", userId2);
                    logger.debug("User created, user id is : {}", userId2);

                    // Creating PLAN: SPCMT-02
                    String telstraPlanDescription = "Southern 4G $20";
                    String planTypeTelstra = "Telstra";
                    String telstraPlanServiceType = "Mobile";
                    BigDecimal telstraPlanPrice = new BigDecimal("18.1818");
                    BigDecimal telstraPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024)
                    // 200
                    // MB
                    BigDecimal telstraPlanBoostQuantity = new BigDecimal("1024");
                    Integer telstraPlanBoostCount = new Integer("3");
                    String rate_card_name_2_with_hypen = ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_2.replace('_', '-');
                    logger.debug("************************ Start creating plan : " + TELSTRA_PLAN + ", " + telstraPlanDescription);
                    Integer telstraPlanId = CreatePlanUtility.createPlan(api, TELSTRA_PLAN, planTypeTelstra, telstraPlanServiceType,
                            telstraPlanDescription, "SPC", rate_card_name_2_with_hypen, "x", telstraPlanPrice, true,
                            telstraPlanUsagePoolQuantity, telstraPlanBoostCount, telstraPlanBoostQuantity);
                    logger.info("Telstra PlanId: {}", telstraPlanId);
                    validatePlanUsagePools(telstraPlanId, 4, "200.0000000000", "1024.0000000000");

                    // Telstra
                    PlanWS planWS = api.getPlanByInternalNumber(TELSTRA_PLAN, api.getCallerCompanyId());
                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                    logger.debug("Plan created with Id : {}", telstraPlanId);
                    planWS = api.getPlanWS(telstraPlanId);
                    assertNotNull("Plan creation failed", planWS);
                    List<AssetWS> assetWSs = new ArrayList<>();
                    productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                    assetId2 = buildAndPersistAssetWithServiceId(envBuilder,
                            getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                            getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), assetIdentifier2,
                            assetName2, assetIdentifier2);

                    assertNotNull("Asset Creation Failed", assetId2);
                    logger.debug("Asset created with Id : {}", assetId2);
                    assetWSs.add(api.getAsset(assetId2));
                    Date activeSinceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Active since for the order is {}", activeSinceDate);
                    secondPlanOrderId = createOrderWithAsset("TestOrder", userId2, activeSinceDate, null, 2, 1, true, productQuantityMap,
                            assetWSs, telstraPlanId, assetIdentifier2);
                    assertNotNull("First subscription order Creation Failed", secondPlanOrderId);
                    logger.debug("First Subscription order id {} for user {}", secondPlanOrderId, userId2);
                    Integer[] invoices = api.createInvoiceWithDate(userId2, nextInvoiceDate, null, null, false);
                    for (Integer i : invoices) {
                        logger.debug("Invoice Id created is {} for user {}", i, userId2);
                        invoicesGenerated.add(i);
                    }
                    nextInvoiceDate = Date.from(baseDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Updating the Next Invoice date as {} for user {}", nextInvoiceDate, userId2);
                    UserWS userws = api.getUserWS(userId2);
                    userws.setNextInvoiceDate(nextInvoiceDate);
                    api.updateUser(userws);
                    logger.debug("Next invoice date for user {} updated now", userId2);
                    TM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID = getItemIdByCode(testBuilder.getTestEnvironment(),
                            TM_MOBILE_TO_FIXED_CALLS_PRODUCT_CODE);

                })
                .validate(
                        (testEnv, testEnvBuilder) -> {

                            logger.debug("Creating Mediation file ....");
                            List<String> cdrList = new ArrayList();
                            for (int i = 0; i < 70; i++) {
                                String eventDate = getDateFormatted(baseDate.plusDays(10).atStartOfDay(), "yyyyMMdd");
                                String duration = prependZero((18 * i) + "", 6);
                                String duration1 = prependZero((52 * i) + "", 6);
                                String duration2 = prependZero((76 * i) + "", 6);
                                logger.debug("Event date set is : {} ", eventDate);
                                cdrList.add(String.format(TELSTRA_MOBILE_CHARGEABLE_CDR, assetIdentifier2, eventDate, duration, eventDate,
                                        duration1, eventDate, duration2));
                            }
                            String cdrFilePath = createFileWithData(TELSTRA_MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".txt",
                                    null, cdrList);
                            logger.debug("Mediation file created {}", cdrFilePath);
                            mediationProcessId1 = api.triggerMediationByConfigurationByFile(
                                    getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                            assertNotNull("Mediation trigger is failed", mediationProcessId1);
                            logger.debug("Mediation ProcessId {}", mediationProcessId1);
                            pauseUntilMediationCompletes(30, api);
                            logger.debug("Mediation Process completed");

                        })
                .validate((testEnv, testEnvBuilder) -> {

                    MediationProcess mediationProcess = api.getMediationProcess(mediationProcessId1);

                    logger.debug("Mediation Process {}", mediationProcess);

                    assertEquals("Mediation Done And Billable ", Integer.valueOf(70), mediationProcess.getDoneAndBillable());
                    assertEquals("Mediation Done And Not Billable ", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                    assertEquals("Mediation Error ", Integer.valueOf(0), mediationProcess.getErrors());

                    Date nextInvoiceDate = Date.from(baseDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Integer[] invoices = api.createInvoiceWithDate(userId2, nextInvoiceDate, null, null, false);
                    Integer invoiceId = null;
                    for (Integer invoice_id : invoices) {
                        logger.debug("Invoice Id created is {} for user {}", invoice_id, userId2);
                        invoiceId = invoice_id;
                        invoicesGenerated.add(invoiceId);
                    }

                    nextInvoiceDate = Date.from(baseDate.plusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Updating the Next Invoice date as {} for user {}", nextInvoiceDate, userId2);
                    UserWS userws = api.getUserWS(userId2);
                    userws.setNextInvoiceDate(nextInvoiceDate);
                    api.updateUser(userws);

                    // Getting JMR level amounts
                        JbillingMediationRecord[] JmrEvents = api.getMediationEventsForInvoice(invoiceId);
                        logger.debug("The Jbilling mediation events arrat size is {} ", JmrEvents.length);
                        BigDecimal ratedPrice = new BigDecimal("0.0");
                        BigDecimal ratedPriceWithTax = new BigDecimal("0.0");
                        BigDecimal taxAmount = new BigDecimal("0.0");
                        for (JbillingMediationRecord jmr : JmrEvents) {
                            ratedPrice = ratedPrice.add(jmr.getRatedPrice());
                            taxAmount = taxAmount.add(jmr.getTaxAmount());
                            ratedPriceWithTax = ratedPriceWithTax.add(jmr.getRatedPriceWithTax());
                            assertTrue("Tax amount is invalid ", jmr.getTaxAmount().compareTo(BigDecimal.ZERO) > 0);
                        }

                        // Getting the Mediated order level amounts
                        OrderWS latestOrder = api.getLatestOrder(userId2);
                        secondMediatedOrderId = latestOrder.getId();
                        assertTrue("Mediated Order is not created", latestOrder.getIsMediated());

                        BigDecimal orderLineAmount = null;

                        for (OrderLineWS orderLine : latestOrder.getOrderLines()) {
                            logger.debug("The item id is {} ", orderLine.getItemId());
                            logger.debug("The item id to check is {} ", TM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID);
                            if (TM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID.equals(orderLine.getItemId())) {
                                orderLineAmount = new BigDecimal(orderLine.getAmount());
                            }
                        }
                        assertTrue("Order line amount is not charged ", orderLineAmount.compareTo(BigDecimal.ZERO) > 0);

                        // Getting the Invoice levet amounts
                        InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
                        assertTrue("Plan order is not included in the invoice ",
                                Arrays.asList(invoiceWS.getOrders()).contains(secondPlanOrderId));
                        assertTrue("Mediated order is not included in the invoice ",
                                Arrays.asList(invoiceWS.getOrders()).contains(secondMediatedOrderId));
                        InvoiceLineDTO[] invoiceLines = invoiceWS.getInvoiceLines();
                        BigDecimal invoiceLineAmount = null;
                        BigDecimal invoiceLineTaxAmount = null;
                        BigDecimal invoiceLineTaxRate = null;
                        BigDecimal invoiceLineGrossAmount = null;
                        for (InvoiceLineDTO line : invoiceLines) {
                            logger.debug("The invoice line item id is {} ", line.getItemId());
                            if (TM_MOBILE_TO_FIXED_CALLS_PRODUCT_ID.equals(line.getItemId())) {
                                invoiceLineAmount = new BigDecimal(line.getAmount());
                                invoiceLineTaxAmount = new BigDecimal(line.getTaxAmount());
                                invoiceLineTaxRate = new BigDecimal(line.getTaxRate());
                                invoiceLineGrossAmount = new BigDecimal(line.getGrossAmount());
                            }
                        }
                        logger.debug("JMR rated price {} ", ratedPrice.toPlainString());
                        logger.debug("JMR tax amount {} ", taxAmount.toPlainString());
                        logger.debug("JMR rated price with tax {} ", ratedPriceWithTax.toPlainString());
                        logger.debug("Invoice line amount {} ", invoiceLineAmount.toPlainString());
                        logger.debug("Invoice line tax amount {} ", invoiceLineTaxAmount.toPlainString());
                        logger.debug("Invoice line tax rate  {} ", invoiceLineTaxRate.toPlainString());
                        logger.debug("Invoice line gross amount  {} ", invoiceLineGrossAmount.toPlainString());

                        // Checking assets on all the amounts
                        assertEquals(
                                "SUM of rated price and Tax Amount in Jbilling Mediation Record is not matching with rated price with tax",
                                ratedPrice.add(taxAmount).setScale(4, BigDecimal.ROUND_HALF_UP),
                                ratedPriceWithTax.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Rated price from Jbilling Mediation Record table does not matches Order Line Amount",
                                ratedPrice.setScale(4, BigDecimal.ROUND_HALF_UP), orderLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Rated price with tax from Jbilling Mediation Record table does not matches Invoice Line Amount",
                                ratedPriceWithTax.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Rated price from Jbilling Mediation Record table does not matches Invoice Line Amount",
                                ratedPrice.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineGrossAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals(
                                "Invoice line amount minus invoice line gross amount from Invoice line table does not matches Invoice Line Tax Amount",
                                invoiceLineAmount.subtract(invoiceLineGrossAmount).setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineTaxAmount.setScale(4, BigDecimal.ROUND_HALF_UP));
                        assertEquals(
                                "Invoice Line amount From Invoice line table does not match with Invoice line gross amount + invoice line tax rate * invoice line gross amount",
                                invoiceLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineGrossAmount.add(
                                        invoiceLineTaxRate.multiply(invoiceLineGrossAmount).divide(new BigDecimal("100"))).setScale(4,
                                        BigDecimal.ROUND_HALF_UP));
                        assertEquals("Order line amount from order line table is not matching with invoice line gross amount",
                                orderLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP),
                                invoiceLineGrossAmount.setScale(4, BigDecimal.ROUND_HALF_UP));

                    });
    }
}