package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.RouteRecordWS;
import com.sapienter.jbilling.server.spc.billing.SPCUserFilterTask;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.NameValueString;

@Test(groups = "agl", testName = "agl.JMRPostProcessorTest")
public class JMRPostProcessorTest extends SPCBaseConfiguration {

    private static final String ASSET01                 = "1231231231";
    private static final String ASSET02                 = "1231231444";
    private static final String ASSET03                 = "1231231555";
    private static final String TEST_CUSTOMER_OPTUS     = "Test-SPC";
    private static final String TEST_CUSTOMER_TELSTRA   = "Test-SPC-Telstra";
    private static final String OPTUS_PLAN              = "SPCMO-0111";
    private static final String TELSTRA_PLAN            = "SPCMT-02X";
    private static final int    BILLIING_TYPE_MONTHLY   = 1;
    private static final String MEDIATION_FILE_PREFIX   = "RESELL_";
    List<AssetWS> assetWSs = new ArrayList<>();
    UserWS spcOptusUserWS;
    UserWS spcTelstraUserWS;
    Map<Integer, BigDecimal> productQuantityMapOptus = new HashMap<>();
    Map<Integer, BigDecimal> productQuantityMapTelstra = new HashMap<>();

    @BeforeClass
    public void beforeClass () {

        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        testBuilder.given(envBuilder -> {
            logger.debug("JMRPostProstProcessorTest.beforeClass : {} "+testBuilder);

            String optusPlanDescription             = "Optus Budget - $10";
            String planTypeOptus                    = "Optus";
            String optusPlanServiceType             = "Mobile";
            BigDecimal optusPlanPrice               = new BigDecimal("9.0909");
            // 1024×1024×1024×(200÷1024) 200 MB
            BigDecimal optusPlanUsagePoolQuantity   = new BigDecimal("209715200"); 
            BigDecimal optusPlanBoostQuantity       = new BigDecimal("1024");
            Integer optusPlanBoostCount             = new Integer("3");

            String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

            logger.debug("************************ Start creating plan");
            Integer optusPlanId = CreatePlanUtility.createPlan(api, OPTUS_PLAN, planTypeOptus, optusPlanServiceType,
                    optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                    optusPlanBoostCount, optusPlanBoostQuantity);
            logger.info("Optus PlanId: {}", optusPlanId);

            buildAndPersistCreditPoolDataTableRecord(optusPlanId.toString(), "OM:NEZ", "50", api.getItemID("MC").toString(), "10",
                    "credit_pool");

            validatePlanUsagePools(optusPlanId,4,"200.0000000000","1024.0000000000");
            // Creating PLAN: SPCMT-02
            String telstraPlanDescription           = "Southern 4G $20";
            String planTypeTelstra                  = "Telstra";
            String telstraPlanServiceType           = "Mobile";
            BigDecimal telstraPlanPrice             = new BigDecimal("18.1818");
            BigDecimal telstraPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024)  200 MB
            BigDecimal telstraPlanBoostQuantity     = new BigDecimal("1024");
            Integer telstraPlanBoostCount           = new Integer("3");

            String rate_card_name_2_with_hypen = ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_1.replace('_', '-');

            logger.debug("************************ Start creating plan : " + TELSTRA_PLAN + ", " + telstraPlanDescription);
            Integer telstraPlanId = CreatePlanUtility.createPlan(api, TELSTRA_PLAN, planTypeTelstra, telstraPlanServiceType,
                    telstraPlanDescription, "SPC", rate_card_name_2_with_hypen, "x", telstraPlanPrice, true,
                    telstraPlanUsagePoolQuantity, telstraPlanBoostCount, telstraPlanBoostQuantity);
            logger.info("Telstra PlanId: {}", telstraPlanId);

            buildAndPersistCreditPoolDataTableRecord(telstraPlanId.toString(), "TM:MVR", "50", api.getItemID("MC").toString(), "20",
                    "credit_pool");
            validatePlanUsagePools(telstraPlanId,4,"200.0000000000","1024.0000000000");
        });
    }

    /**
     * Customer NID - 1st Last month
     * Order Prepaid - Active Since Day - 1st of last Month
     * Generate Invoice - 1st of Last Month
     * Update NID - 1st of current month
     * Upload Mediation - event date - from last month
     * Mediation Order - 1st of last month Onetime
     * Generate Invoice - 1st of current month.
     * System will generate Credit order with the latest invoice
     */
    @Test(priority = 1)
    public void jmrPostProcessorInvoiceAndCreditOrderOptusMobileTest() {

        try {
            testBuilder.given(envBuilder -> {
                spcOptusUserWS = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_OPTUS + System.currentTimeMillis(), getDate(-2, 1).getTime(), "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", spcOptusUserWS);
                logger.debug("spcTestUserWS created {}", spcOptusUserWS.getUserName()); 

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET01, "asset-01");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));
             // optus
                PlanWS optusPlanWS = api.getPlanByInternalNumber(OPTUS_PLAN, api.getCallerCompanyId());
                assertNotNull("optusPlanWS :  ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset("TestOrder", spcOptusUserWS.getId(), getDate(-2, 1).getTime(), null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                // create invoice
                // 1st MAY
                Integer[] invoices = api.createInvoiceWithDate(spcOptusUserWS.getId(), getDate(-2, 1).getTime(), null, null, false);
                assertEquals("Plan Order invoice created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);
                InvoiceLineDTO[] invoiceLines = invoiceWS.getInvoiceLines();
                for (InvoiceLineDTO line : invoiceLines) {
                    logger.debug("The invoice line item id is {} ", line.getItemId());
                    if (optusPlanWS.getItemId().equals(line.getItemId())) {
                        validateTaxOnInvoice (line);
                        break;
                    }
                }
                assertEquals("Orders count in invoice", 1,
                        collectOrdersFromInvoice(invoiceWS).length);
                spcOptusUserWS = api.getUserWS(spcOptusUserWS.getId());
                spcOptusUserWS.setNextInvoiceDate(getDate(-1, 1).getTime());
                api.updateUser(spcOptusUserWS);

                String eventDate = getDateFormatted(getLocalDate(-2, 2),DATE_FORMAT_YYYYMMDD);
                //@formatter:off
                //@formatter:off
                String  OPTUS_MOBILE_CHARGEABLE_CDR_FORMAT = "016166956000050502006826"+eventDate+"12422700019287I05                       " + "\n"        
                        + "10"+ASSET01+"                       000000000000000616695600005050200050502"+eventDate+"073151002200000200000004"+eventDate+"00116493795051       1110321143S1523127E10000000000000000000000               NEW ZEALAND    Forster        11"+eventDate+"072950000149000000012201000000538IDDSP090400000000000000000012206011" + "\n"
                        + "996166958000050502006826024336100923250000000007071900000000080317"+eventDate+"095739"+eventDate+"034118000044526740000210252800000000000000004466650000000000000001903481000000000000000025358100001649900035827823035827823000000000000000000000000558421468769270000000000000000000000";
                //@formatter:on                //@formatter:on
                logger.debug("OPTUS_MOBILE_CHARGEABLE_CDR_FORMAT {}", OPTUS_MOBILE_CHARGEABLE_CDR_FORMAT);
                String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".dat", null,
                        Arrays.asList(OPTUS_MOBILE_CHARGEABLE_CDR_FORMAT));
                logger.debug("Mediation file created {}", cdrFilePath);
                UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME),
                        new File(cdrFilePath));
                assertNotNull("Mediation trigger failed", mediationProcessId);
                logger.debug("Mediation ProcessId {}", mediationProcessId);
                pauseUntilMediationCompletes(30, api);

            }).validate((testEnv, testEnvBuilder) -> {

                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());

                OrderWS order = api.getLatestOrder(spcOptusUserWS.getId());
                assertEquals("Order must me mediated", Boolean.TRUE, order.getIsMediated()); 

                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                validatePricingFields(viewEvents);

                logger.debug("original quantity  {}", viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                logger.debug("resolved quantity  {}", order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                logger.debug("order amount  {}", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                assertEquals("Invalid original quantity", new BigDecimal("1320.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", new BigDecimal("22.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", new BigDecimal("20.16"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                //Next Cycle 
                Integer[] invoices2 = api.createInvoiceWithDate(spcOptusUserWS.getId(), getDate(-1, 1).getTime(), null, null, false);
                assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices2)));
                InvoiceWS invoiceWS2 = api.getLatestInvoice(spcOptusUserWS.getId());
                assertEquals("Orders count in invoice", 3,
                        collectOrdersFromInvoice(invoiceWS2).length);
                logger.debug("Invoice balance  {}", invoiceWS2.getBalance());
                logger.debug("Invoice amount  {}", invoiceWS2.getTotal());
                logger.debug("Invoice carried balance {}", invoiceWS2.getCarriedBalance());

                order = api.getLatestOrder(spcOptusUserWS.getId());
                logger.debug("Credit Total {}", order.getTotalAsDecimal());
                assertEquals("Credit Total {}", new BigDecimal("-10.00"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                InvoiceLineDTO[] invoiceLines = invoiceWS2.getInvoiceLines();
                for (InvoiceLineDTO line : invoiceLines) {
                    logger.debug("The invoice line item id is {} ", line.getItemId());
                    if (api.getItemID("MC").equals(line.getItemId())) {
                        validateTaxOnInvoice (line);
                        break;
                    }
                }
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(spcOptusUserWS.getId());
        }
    }

    private void validateTaxOnInvoice (InvoiceLineDTO line) {

        logger.debug("The invoice line item id is {} ", line.getItemId());

        BigDecimal invoiceLineAmount       = new BigDecimal(line.getAmount());
        BigDecimal invoiceLineTaxAmount    = new BigDecimal(line.getTaxAmount());
        BigDecimal invoiceLineTaxRate      = new BigDecimal(line.getTaxRate());
        BigDecimal invoiceLineGrossAmount  = new BigDecimal(line.getGrossAmount());

        assertEquals("Invoice Line amount From Invoice line table does not"
                + " match with Invoice line gross amount"
                + " + invoice line tax rate"
                + " * invoice line gross amount",
                invoiceLineAmount.setScale(4, BigDecimal.ROUND_HALF_UP),
                invoiceLineGrossAmount.add(invoiceLineTaxRate
                                      .multiply(invoiceLineGrossAmount)
                                      .divide(new BigDecimal("100")))
                                      .setScale(4, BigDecimal.ROUND_HALF_UP));
    }

    /**
     * Customer NID - 1st Last month
     * Order Prepaid - Active Since Day - 1st of last Month
     * Generate Invoice - 1st of Last Month
     * Update NID - 1st of current month
     * Upload Mediation - event date - from last month
     * Mediation Order - 1st of last month Onetime
     * Generate Invoice - 1st of current month.
     * System will generate Credit order with the latest invoice
     */
    @Test(priority = 2)
    public void jmrPostProcessorInvoiceAndCreditOrderTelstraMobileTest() {

        try {
            testBuilder.given(envBuilder -> {
                spcTelstraUserWS = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_TELSTRA + System.currentTimeMillis(), getDate(-2, 1).getTime(), "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", spcTelstraUserWS);
                logger.debug("spcTestUserWS created {}", spcTelstraUserWS.getUserName()); 

                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02, "asset-02");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

             // Telstra
                PlanWS telstraPlanWS  = api.getPlanByInternalNumber(TELSTRA_PLAN, api.getCallerCompanyId());
                assertNotNull("telstraPlanWS must not be null ", telstraPlanWS);
                productQuantityMapTelstra.put(telstraPlanWS.getItemId(), BigDecimal.ONE);
                
                Integer orderId = createOrderWithAsset("TestOrder-TM", spcTelstraUserWS.getId(), getDate(-2, 1).getTime(), null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapTelstra, assetWSs, telstraPlanWS.getId());
                assertNotNull("orderId must not be null ", orderId);
                logger.debug("Plan Order created {}", orderId);

                Integer[] invoices      = api.createInvoiceWithDate(spcTelstraUserWS.getId(), getDate(-2, 1).getTime(), null, null, false);
                assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS     = api.getInvoiceWS(invoices[0]);
                ///TODO ########## check for order_id in invoice line  and assert for tax amount######
                assertEquals("Orders count in invoice", 1,
                        collectOrdersFromInvoice(invoiceWS).length);
                spcTelstraUserWS          = api.getUserWS(spcTelstraUserWS.getId());
                spcTelstraUserWS.setNextInvoiceDate(getDate(-1, 1).getTime());
                api.updateUser(spcTelstraUserWS);

                InvoiceLineDTO[] invoiceLines = invoiceWS.getInvoiceLines();
                for (InvoiceLineDTO line : invoiceLines) {
                    logger.debug("The invoice line item id is {} ", line.getItemId());
                    if (telstraPlanWS.getItemId().equals(line.getItemId())) {
                        validateTaxOnInvoice (line);
                        break;
                    }
                }
                String eventDate = getDateFormatted(getLocalDate(-2, 2),DATE_FORMAT_YYYYMMDD);
                String TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT = "P1,1231231444,HHHHH,E.164,1,0,505015602749590,IMSI,cjprvccn01,HHHHH,E.164,61141,E.164,1,0,CS,IP,61,1001,HHHHH,61141,E.164,1,N,B,500,36000,"+eventDate+"085537,4222,7852860,20090618,,,,,,,,,#,"
                        + "P2,1,5,H,,,,,,,33,,,664,Sec,,,,1411,,Diverted Call,ALL,I,,36000,"+eventDate+"085537,GTEL,OUT,GVZN000226,CAALL,S,N,GSM,1,0,,,,,,,#,"
                        + "P3,Sec,4,Sec,4,0.000000,AUD,0.000000,1,5,N,DUR,BASE,I,TWTT000001,36000,"+eventDate+"085537,0.000000,0.000000,1,0,0,,,,,,,,#,P4,U,S,Sec,4,Sec,4,0.000000,AUD,0,4222100,D,,,,,,,,#,";

                logger.debug("TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT {}", TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT);
                String cdrFilePath      = createFileWithData("Reseller" + System.currentTimeMillis(), ".txt", null,
                        Arrays.asList(TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT));
                logger.debug("Mediation file created {}", cdrFilePath);
                UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME),
                        new File(cdrFilePath));
                assertNotNull("Mediation trigger failed", mediationProcessId);
                logger.debug("Mediation ProcessId {}", mediationProcessId);
                pauseUntilMediationCompletes(30, api);

            }).validate((testEnv, testEnvBuilder) -> {

                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(spcTelstraUserWS.getId());
                assertEquals("Order must me mediated", Boolean.TRUE, order.getIsMediated());

                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                validatePricingFields(viewEvents);

                logger.debug("original quantity  {}", viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                logger.debug("resolved quantity  {}", order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                logger.debug("order amount  {}", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                assertEquals("Invalid original quantity", new BigDecimal("664.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", new BigDecimal("11.07"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", new BigDecimal("4.93"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                Integer[] invoices2 = api.createInvoiceWithDate(spcTelstraUserWS.getId(), getDate(-1, 1).getTime(), null, null, false);
                assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices2)));
                InvoiceWS invoiceWS2 = api.getLatestInvoice(spcTelstraUserWS.getId());
                assertEquals("Orders count in invoice", 3,
                        collectOrdersFromInvoice(invoiceWS2).length);
                logger.debug("Invoice balance  {}", invoiceWS2.getBalance());
                logger.debug("Invoice amount  {}", invoiceWS2.getTotal());
                logger.debug("Invoice carried balance {}", invoiceWS2.getCarriedBalance());

                order = api.getLatestOrder(spcTelstraUserWS.getId());
                logger.debug("Credit Total {}", order.getTotalAsDecimal());
                assertEquals("Invalid order amount", new BigDecimal("-4.93"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                InvoiceLineDTO[] invoiceLines = invoiceWS2.getInvoiceLines();
                for (InvoiceLineDTO line : invoiceLines) {
                    logger.debug("The invoice line item id is {} ", line.getItemId());
                    if (api.getItemID("MC").equals(line.getItemId())) {
                        validateTaxOnInvoice (line);
                        break;
                    }
                }
            });

        } finally {
            assetWSs.clear();
            productQuantityMapTelstra.clear();
            clearTestDataForUser(spcTelstraUserWS.getId());
        }

    }
    
    
    /**
     * Customer NID - 1st Last month
     * Order Prepaid - Active Since Day - 1st of last Month
     * Generate Invoice - 1st of Last Month
     * Update NID - 1st of current month
     * Upload Mediation - event date - from last month
     * Mediation Order - 1st of last month Onetime
     * Generate Invoice - 1st of current month.
     * System will generate Credit order with the latest invoice
     * In this test case the Plugin parameter value - PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE is used as true and false.
     */
    @Test(priority = 3)
    public void jmrPostProcessorInvoiceAndCreditOrderTelstraMobilePreferenceTest() {

        try {
            testBuilder.given(envBuilder -> {
                spcTelstraUserWS = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_TELSTRA + System.currentTimeMillis(), getDate(-2, 1).getTime(), "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", spcTelstraUserWS);
                logger.debug("spcTestUserWS created {}", spcTelstraUserWS.getUserName()); 

                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET03, "asset-03");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                // Telstra
                PlanWS telstraPlanWS  = api.getPlanByInternalNumber(TELSTRA_PLAN, api.getCallerCompanyId());
                assertNotNull("telstraPlanWS must not be null ", telstraPlanWS);
                productQuantityMapTelstra.put(telstraPlanWS.getItemId(), BigDecimal.ONE);

                Integer orderId = createOrderWithAsset("TestOrder-TM", spcTelstraUserWS.getId(), getDate(-2, 1).getTime(), null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapTelstra, assetWSs, telstraPlanWS.getId());
                assertNotNull("orderId must not be null ", orderId);
                logger.debug("Plan Order created {}", orderId);

                Integer[] invoices      = api.createInvoiceWithDate(spcTelstraUserWS.getId(), getDate(-2, 1).getTime(), null, null, false);
                assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices)));
                logger.debug("Plan Order invoice created {}", invoices[0]);
                InvoiceWS invoiceWS     = api.getInvoiceWS(invoices[0]);
                assertEquals("Orders count in invoice", 1,
                        collectOrdersFromInvoice(invoiceWS).length);
                spcTelstraUserWS          = api.getUserWS(spcTelstraUserWS.getId());
                spcTelstraUserWS.setNextInvoiceDate(getDate(-1, 1).getTime());
                api.updateUser(spcTelstraUserWS);

                InvoiceLineDTO[] invoiceLines = invoiceWS.getInvoiceLines();
                for (InvoiceLineDTO line : invoiceLines) {
                    logger.debug("The invoice line item id is {} ", line.getItemId());
                    if (telstraPlanWS.getItemId().equals(line.getItemId())) {
                        validateTaxOnInvoice (line);
                        break;
                    }
                }
                PluggableTaskWS pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(SpcJMRPostProcessorTask.class.getName())
                        .getId());
                
                Map<String, String> spcJMRPostProcessorParams = pluggableTask.getParameters();           
                spcJMRPostProcessorParams.put(SpcJMRPostProcessorTask.PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE.getName(), "true");
                updateExistingPlugin(api, pluggableTask.getId(), SpcJMRPostProcessorTask.class.getName(), spcJMRPostProcessorParams);                
                logger.debug("Updated the plugin SpcJMRPostProcessorTask PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE as true");
                
                String eventDate = getDateFormatted(getLocalDate(-2, 12),DATE_FORMAT_YYYYMMDD);
                String TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT = "P1,1231231555,HHHHH,E.164,1,0,505015602749590,IMSI,cjprvccn01,HHHHH,E.164,61141,E.164,1,0,CS,IP,61,1001,HHHHH,61141,E.164,1,N,B,500,36000,"+eventDate+"085537,4222,7852860,20090618,,,,,,,,,#,"
                        + "P2,1,5,H,,,,,,,33,,,664,Sec,,,,1411,,Diverted Call,ALL,I,,36000,"+eventDate+"085537,GTEL,OUT,GVZN000226,CAALL,S,N,GSM,1,0,,,,,,,#,"
                        + "P3,Sec,4,Sec,4,0.000000,AUD,0.000000,1,5,N,DUR,BASE,I,TWTT000001,36000,"+eventDate+"085537,0.000000,0.000000,1,0,0,,,,,,,,#,P4,U,S,Sec,4,Sec,4,0.000000,AUD,0,4222100,D,,,,,,,,#,";

                logger.debug("TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT {}", TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT);
                String cdrFilePath      = createFileWithData("Reseller" + System.currentTimeMillis(), ".txt", null,
                        Arrays.asList(TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT));
                logger.debug("Mediation file created {}", cdrFilePath);
                UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME),
                        new File(cdrFilePath));
                assertNotNull("Mediation trigger failed", mediationProcessId);
                logger.debug("Mediation ProcessId {}", mediationProcessId);
                pauseUntilMediationCompletes(30, api);
                
                spcJMRPostProcessorParams.put(SpcJMRPostProcessorTask.PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE.getName(), "false");
                updateExistingPlugin(api, pluggableTask.getId(), SpcJMRPostProcessorTask.class.getName(), spcJMRPostProcessorParams);
                logger.debug("Updated the plugin SpcJMRPostProcessorTask PARAM_REDUCE_PRICING_FIELD_IN_JMR_TABLE as false");
                
                eventDate = getDateFormatted(getLocalDate(-2, 13),DATE_FORMAT_YYYYMMDD);
                TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT = "P1,1231231555,HHHHH,E.164,1,0,505015602749590,IMSI,cjprvccn01,HHHHH,E.164,61141,E.164,1,0,CS,IP,61,1001,HHHHH,61141,E.164,1,N,B,500,36000,"+eventDate+"085537,4222,7852860,20090618,,,,,,,,,#,"
                        + "P2,1,5,H,,,,,,,33,,,664,Sec,,,,1411,,Diverted Call,ALL,I,,36000,"+eventDate+"085537,GTEL,OUT,GVZN000226,CAALL,S,N,GSM,1,0,,,,,,,#,"
                        + "P3,Sec,4,Sec,4,0.000000,AUD,0.000000,1,5,N,DUR,BASE,I,TWTT000001,36000,"+eventDate+"085537,0.000000,0.000000,1,0,0,,,,,,,,#,P4,U,S,Sec,4,Sec,4,0.000000,AUD,0,4222100,D,,,,,,,,#,";

                cdrFilePath = createFileWithData("Reseller" + System.currentTimeMillis(), ".txt", null,
                        Arrays.asList(TELSTRA_MOBILE_CHARGEABLE_CDR_FORMAT));
                mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME),
                        new File(cdrFilePath));
                assertNotNull("Mediation trigger failed", mediationProcessId);
                logger.debug("Mediation ProcessId {}", mediationProcessId);
                pauseUntilMediationCompletes(30, api);

            }).validate((testEnv, testEnvBuilder) -> {

                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(spcTelstraUserWS.getId());
                assertEquals("Order must me mediated", Boolean.TRUE, order.getIsMediated());

                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                validatePricingFields(viewEvents);

                logger.debug("original quantity  {}", viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                logger.debug("resolved quantity  {}", order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                logger.debug("order amount  {}", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                assertEquals("Invalid original quantity", new BigDecimal("664.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", (new BigDecimal("11.07").multiply(new BigDecimal("2.0"))).setScale(2, BigDecimal.ROUND_HALF_UP),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", (new BigDecimal("4.93").multiply(new BigDecimal("2.0"))).setScale(2, BigDecimal.ROUND_HALF_UP),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                Integer[] invoices2 = api.createInvoiceWithDate(spcTelstraUserWS.getId(), getDate(-1, 1).getTime(), null, null, false);
                assertEquals("Invoice must be created ",Boolean.TRUE, Boolean.valueOf(ArrayUtils.isNotEmpty(invoices2)));
                InvoiceWS invoiceWS2 = api.getLatestInvoice(spcTelstraUserWS.getId());
                assertEquals("Orders count in invoice", 3,
                        collectOrdersFromInvoice(invoiceWS2).length);
                logger.debug("Invoice balance  {}", invoiceWS2.getBalance());
                logger.debug("Invoice amount  {}", invoiceWS2.getTotal());
                logger.debug("Invoice carried balance {}", invoiceWS2.getCarriedBalance());

                order = api.getLatestOrder(spcTelstraUserWS.getId());
                logger.debug("Credit Total {}", order.getTotalAsDecimal());
                //TODO : need to add the assert on exact credit order amount after the JBSPC-881 fix is added.
                /*assertEquals("Invalid order amount", (new BigDecimal("-4.93").multiply(new BigDecimal("1.1")).setScale(2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("2")))
                        .divide(new BigDecimal("1.1"),2, BigDecimal.ROUND_HALF_UP),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));*/
                assertTrue("Invalid order amount", order.getTotalAsDecimal().compareTo(new BigDecimal("0.0")) < 0);
                InvoiceLineDTO[] invoiceLines = invoiceWS2.getInvoiceLines();
                for (InvoiceLineDTO line : invoiceLines) {
                    logger.debug("The invoice line item id is {} ", line.getItemId());
                    if (api.getItemID("MC").equals(line.getItemId())) {
                        validateTaxOnInvoice (line);
                        break;
                    }
                }
            });

        } finally {
            assetWSs.clear();
            productQuantityMapTelstra.clear();
            clearTestDataForUser(spcTelstraUserWS.getId());
        }

    }

    @AfterClass
    private void teardown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
    }
}
