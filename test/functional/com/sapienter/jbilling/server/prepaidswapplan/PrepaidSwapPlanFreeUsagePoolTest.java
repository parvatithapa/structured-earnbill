package com.sapienter.jbilling.server.prepaidswapplan;

import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.invoiceSummary.ItemizedAccountTester;
import com.sapienter.jbilling.server.invoiceSummary.ItemizedAccountWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.CollectionType;

@Test(groups = { "prepaid-swapPlan" }, testName = "PrepaidFlatFreeUsagePoolTest")
public class PrepaidSwapPlanFreeUsagePoolTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String SUBSCRIPTION_PROD_01 = "testPlanSubscriptionItem_01";
    private static final String USAGE_POOL_01 = "UP with 100 Quantity"+System.currentTimeMillis();;
    private static final String PLAN_01 = "100 free minute Plan";
    private static final String PLAN_02 = "200 free minute Plan";
    private static final String SUBSCRIPTION_PROD_02 = "testPlanSubscriptionItem_02";
    private static final String USAGE_POOL_02 = "UP with 200 Quantity"+System.currentTimeMillis();;
    private static final Integer NEXT_INVOICE_DAY = 1;
    private static final String USER_01 = "Test-1-"+System.currentTimeMillis();
    private static final String ORDER_01 = "testSubScriptionOrderO1";
    private static final String USER_02 = "Test-2-"+System.currentTimeMillis();
    private static final String ORDER_02 = "testSubScriptionOrderO2";
    private static final String USER_03 = "Test-3-"+System.currentTimeMillis();
    private static final String ORDER_03 = "testSubScriptionOrderO3";
    private static final String USER_04 = "Test-4-"+System.currentTimeMillis();
    private static final String ORDER_04 = "testSubScriptionOrderO4";
    private static final String USER_05 = "Test-5-"+System.currentTimeMillis();
    private static final String ORDER_05 = "testSubScriptionOrderO5";
    private static final String USER_06 = "Test-6-"+System.currentTimeMillis();
    private static final String ORDER_06 = "testSubScriptionOrderO6";
    private static final String USER_07 = "Test-7-"+System.currentTimeMillis();
    private static final String ORDER_07 = "testSubScriptionOrderO7";
    private static final String USER_08 = "Test-8-"+System.currentTimeMillis();
    private static final String ORDER_08 = "testSubScriptionOrderO8";
    private static final String INBOUND_MEDIATION_LAUNCHER = "inboundCallsMediationJobLauncher";
    private static final String USAGEPOOL_LOGGER_MSG = "Cycle start date: {} Cycle end date: {}";
    private static final String INBOUND_CDR_DATE = "03/10/2018";
    private static final Integer CC_PM_ID = 5;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID= 320104;
    // Mediated Usage Products
    public static final int INBOUND_USAGE_PRODUCT_ID = 320101;
    public static final int CHAT_USAGE_PRODUCT_ID = 320102;
    public static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
    private String testAccount = "Account Type";
    private String testCat1 = "MediatedUsageCategory";
    private AssetWS scenario01Asset;
    private BillingProcessConfigurationWS oldBillingProcessConfig;
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private Integer discountId;

    // Collection Steps
    private static final String ACTIVE_PAST_DUE_STEP                = "Active | Past Due";
    private static final String ACTIVE_NOTICE_TO_BLOCK_STEP         = "Active | Notice to Block";
    private static final String ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP = "Active | Blocked for Non-Payment";
    private static final String CANCELLED_CUSTOMER_REQUEST_STEP     = "Cancelled | Customer Request";
    private static final String CANCELLED_NOTICE_TO_COLLECTION_STEP = "Cancelled | Notice to Collection";
    private static final String CANCELLED_TP_COLLECTIONS_STEP       = "Cancelled | TP Collections";
    // Fixed constants for now
    private static final Integer INVOICE_STATUS_UNPAID = Integer.valueOf(2);

    @BeforeClass
    public void initializeTests() {
        cleanAgeingSteps();
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            //Configure Swap Plan FUP Transfer Task
            PluggableTaskTypeWS pluginType = api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.usagePool.task.SwapPlanFUPTransferTask");
            PluggableTaskWS swapPlanFUPTransferPlugin = api.getPluginWSByTypeId(pluginType.getId());
            Hashtable<String, String> swapPlanFUPTransferPluginparameters = new Hashtable<>();
            swapPlanFUPTransferPluginparameters.put("credit_product_id", "320111");
            swapPlanFUPTransferPluginparameters.put("debit_product_id", "320111");
            swapPlanFUPTransferPlugin.setParameters(swapPlanFUPTransferPluginparameters);
            api.updatePlugin(swapPlanFUPTransferPlugin);

            //To configure Customer usagepool evalution task we need to set prorate type as "Manual".
            oldBillingProcessConfig = api.getBillingProcessConfiguration();
            BillingProcessConfigurationWS newBillingProcessConfig = api.getBillingProcessConfiguration();
            newBillingProcessConfig.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());
            api.createUpdateBillingProcessConfiguration(newBillingProcessConfig);

            //Create account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
            //Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            //Create usage products
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_01, false, envBuilder.idForCode(testCat1), "99", true);
            //Usage product item ids
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2018);
            pricingDate.set(Calendar.MONTH, 0);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);
            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());

            //Create usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder,api,USAGE_POOL_01, "100", envBuilder.idForCode(testCat1), items);

            //Create 100 min free plan
            buildAndPersistPlan(envBuilder,api, PLAN_01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
                    envBuilder.idForCode(SUBSCRIPTION_PROD_01), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),planItemProd01WS, planItemProd02WS, planItemProd03WS);

            PlanItemWS planItemProd201WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.25", pricingDate.getTime());
            PlanItemWS planItemProd202WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.25", pricingDate.getTime());
            PlanItemWS planItemProd203WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.25", pricingDate.getTime());

            // Create FUP,FLAT PRODUCT AND PLAN with 135 Free Min
            buildAndPersistUsagePool(envBuilder, api, USAGE_POOL_02, "200", envBuilder.idForCode(testCat1), items);
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_02, false, envBuilder.idForCode(testCat1), "199", true);
            buildAndPersistPlan(envBuilder,api, PLAN_02, "200 Free Min Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SUBSCRIPTION_PROD_02), Arrays.asList(envBuilder.idForCode(USAGE_POOL_02)),planItemProd201WS, planItemProd202WS, planItemProd203WS);
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_01));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_02));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_01));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_02));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_01));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_02));
        });
    }

    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        api.createUpdateBillingProcessConfiguration(oldBillingProcessConfig);
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    /**
     * Plan Upgrade
     * Existing plan -100min free $99
     * Upgraded plan -200min free $199
     */
    @Test
    public void test001SwapPlanFUPTransferScenarioA1(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #1 - User that has last invoice fully paid");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_01,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                        //Creating subscription order on 1st ofJan 2018
                        .createOrder(ORDER_01, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                                ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false)
                        .generateInvoice(nextInvoiceDate.getTime(), false)
                        .makePayment("103.99", activeSince.getTime(), false);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_01));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(null)
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("103.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("103.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("0.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "01/10/2018");
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2018);
                invoiceDate.set(Calendar.MONTH, 1);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                scenario01.selectUserByName(USER_01)
                    .generateInvoice(invoiceDate.getTime(), false)
                    .makePayment("403.99", invoiceDate.getTime(), false);

                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.setTime(invoiceDate.getTime());
                lastInvoiceDate.add(Calendar.MONTH, -1);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_01));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-103.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("403.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("403.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("300.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("103.99"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "02/10/2018");

                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                api.triggerCustomerUsagePoolEvaluation(1 , invoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 1);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 1);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 28);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2018);
                invoiceDate.set(Calendar.MONTH, 2);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_01));
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, 
                        environment.idForCode(SUBSCRIPTION_PROD_01), 
                        environment.idForCode(SUBSCRIPTION_PROD_02), 
                        SwapMethod.DIFF, 
                        Util.truncateDate(order.getActiveSince()));
                api.createUpdateOrder(order,orderChanges);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.selectUserByName(USER_01)
                    .generateInvoice(invoiceDate.getTime(), false)
                    .makePayment("428.99", invoiceDate.getTime(), false);

                api.triggerCustomerUsagePoolEvaluation(1 , invoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.setTime(invoiceDate.getTime());
                lastInvoiceDate.add(Calendar.MONTH, -1);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_01));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-403.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("100.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("203.99"))
                                                            .addExpectedNewCharges(new BigDecimal("428.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("428.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("125.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("403.99"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", INBOUND_CDR_DATE);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 2);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 2);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.setTime(invoiceDate.getTime());
                nextInvoiceDate.add(Calendar.MONTH, 1);

                scenario01.selectUserByName(USER_01)
                    .generateInvoice(nextInvoiceDate.getTime(), false)
                    .makePayment("328.99", nextInvoiceDate.getTime(), false);

                api.triggerCustomerUsagePoolEvaluation(1 , nextInvoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.set(Calendar.YEAR, 2018);
                lastInvoiceDate.set(Calendar.MONTH, 2);
                lastInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_01));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-428.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("203.99"))
                                                            .addExpectedNewCharges(new BigDecimal("328.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("328.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("125.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("428.99"))
                                                            .validate();
            });
        } finally {
            cleanUp(USER_01);
        }
    }
    private void cleanUp(String user) {
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(user), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
        Arrays.stream(api.getUserOrdersPage(testBuilder.getTestEnvironment().idForCode(user), 10, 0))
                .forEach(order -> api.deleteOrder(order.getId()));
        api.deleteUser(testBuilder.getTestEnvironment().idForCode(user));
    }

    /**
     * Plan Downgrade
     * Existing plan -200min free $199
     * Upgraded plan -100min free $99
     * */
    @Test
    public void test002SwapPlanFUPTransferScenarioA2(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_02), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                // Next Invoice Date 1st of Jan 2018
                scenario01.createUser(USER_02,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                        //Creating subscription order on 1st of Jan 2018 
                        .createOrder(ORDER_02, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID, 
                                ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false)
                        .generateInvoice(nextInvoiceDate.getTime(), false)
                        .makePayment("203.99", activeSince.getTime(), false);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_02));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(null)
                                                            .addExpectedMonthlyCharges(new BigDecimal("203.99"))
                                                            .addExpectedNewCharges(new BigDecimal("203.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("203.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("0.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "01/10/2018");
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2018);
                invoiceDate.set(Calendar.MONTH, 1);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                scenario01.selectUserByName(USER_02)
                    .generateInvoice(invoiceDate.getTime(), false)
                    .makePayment("328.99", invoiceDate.getTime(), false);

                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.setTime(invoiceDate.getTime());
                lastInvoiceDate.add(Calendar.MONTH, -1);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_02));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-203.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("203.99"))
                                                            .addExpectedNewCharges(new BigDecimal("328.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("328.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("125.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("203.99"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "02/10/2018");
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                api.triggerCustomerUsagePoolEvaluation(1 , invoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 1);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 1);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 28);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2018);
                invoiceDate.set(Calendar.MONTH, 2);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_02));
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        SwapMethod.DIFF, 
                        Util.truncateDate(order.getActiveSince()));
                api.createUpdateOrder(order,orderChanges);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.selectUserByName(USER_02)
                    .generateInvoice(invoiceDate.getTime(), false)
                    .makePayment("303.99", invoiceDate.getTime(), false);

                api.triggerCustomerUsagePoolEvaluation(1 , invoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.setTime(invoiceDate.getTime());
                lastInvoiceDate.add(Calendar.MONTH, -1);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_02));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-328.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("-100.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("303.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("303.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("300.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("328.99"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", INBOUND_CDR_DATE);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 2);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 2);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.setTime(invoiceDate.getTime());
                nextInvoiceDate.add(Calendar.MONTH, 1);

                scenario01.selectUserByName(USER_02)
                    .generateInvoice(nextInvoiceDate.getTime(), false)
                    .makePayment("403.99", nextInvoiceDate.getTime(), false);

                api.triggerCustomerUsagePoolEvaluation(1 , nextInvoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.set(Calendar.YEAR, 2018);
                lastInvoiceDate.set(Calendar.MONTH, 2);
                lastInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_02));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-303.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("403.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("403.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("300.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("303.99"))
                                                            .validate();
            });
        } finally {
            cleanUp(USER_02);
        }
    }

    /**
     * Plan Upgrade-Downgrade
     * Existing plan -100min free $99
     * Upgraded plan -200min free $199
     * Downgraded plan -100min free $99
     */
    @Test
    public void test003SwapPlanFUPTransferScenarioA3(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #1 - User that has last invoice fully paid");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_03,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                        //Creating subscription order on 1st ofJan 2018
                        .createOrder(ORDER_03, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                                ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false)
                        .generateInvoice(nextInvoiceDate.getTime(), false)
                        .makePayment("103.99", activeSince.getTime(), false);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_03));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("0.00"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(null)
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("103.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("103.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("0.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("0.00"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "01/10/2018");
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_03));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2018);
                invoiceDate.set(Calendar.MONTH, 1);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                scenario01.selectUserByName(USER_03)
                    .generateInvoice(invoiceDate.getTime(), false)
                    .makePayment("403.99", invoiceDate.getTime(), false);

                Calendar lastInvoiceDate = Calendar.getInstance(); try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                lastInvoiceDate.setTime(invoiceDate.getTime());
                lastInvoiceDate.add(Calendar.MONTH, -1);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_03));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-103.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("403.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("403.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("300.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("103.99"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "02/10/2018");

                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                api.triggerCustomerUsagePoolEvaluation(1 , invoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 1);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 1);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 28);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_03));
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2018);
                invoiceDate.set(Calendar.MONTH, 2);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_03));
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                api.createUpdateOrder(order,orderChanges);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_03));
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("200.00"));

                order = api.getOrder(environment.idForCode(ORDER_03));
                orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));

                api.createUpdateOrder(order,orderChanges);

                user = api.getUserWS(envBuilder.idForCode(USER_03));
                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("100.00"));

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.selectUserByName(USER_03)
                    .generateInvoice(invoiceDate.getTime(), false)
                    .makePayment("403.99", invoiceDate.getTime(), false);

                api.triggerCustomerUsagePoolEvaluation(1 , invoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.setTime(invoiceDate.getTime());
                lastInvoiceDate.add(Calendar.MONTH, -1);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_03));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-403.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("403.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("403.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("300.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("403.99"))
                                                            .validate();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", INBOUND_CDR_DATE);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                user = api.getUserWS(envBuilder.idForCode(USER_03));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 2);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 2);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.setTime(invoiceDate.getTime());
                nextInvoiceDate.add(Calendar.MONTH, 1);

                scenario01.selectUserByName(USER_03)
                    .generateInvoice(nextInvoiceDate.getTime(), false)
                    .makePayment("403.99", nextInvoiceDate.getTime(), false);

                api.triggerCustomerUsagePoolEvaluation(1 , nextInvoiceDate.getTime());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar lastInvoiceDate = Calendar.getInstance();
                lastInvoiceDate.set(Calendar.YEAR, 2018);
                lastInvoiceDate.set(Calendar.MONTH, 2);
                lastInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_03));
                ItemizedAccountWS itemizedAccountWS = api.getItemizedAccountByInvoiceId(invoice.getId());
                new ItemizedAccountTester(itemizedAccountWS).addExpectedPaymentReceived(new BigDecimal("-403.99"))
                                                            .addExpectedAdjustmentCharges(new BigDecimal("0.00"))
                                                            .addExpectedFeesCharges(new BigDecimal("0.00"))
                                                            .addExpectedLastInvoiceDate(lastInvoiceDate.getTime())
                                                            .addExpectedMonthlyCharges(new BigDecimal("103.99"))
                                                            .addExpectedNewCharges(new BigDecimal("403.99"))
                                                            .addExpectedTaxesCharges(new BigDecimal("0.00"))
                                                            .addExpectedTotalDue(new BigDecimal("403.99"))
                                                            .addExpectedUsageCharges(new BigDecimal("300.00"))
                                                            .addExpectedAmountOfLastStatement(new BigDecimal("403.99"))
                                                            .validate();
            });
        } finally {
            cleanUp(USER_03);
        }
    }

    @Test
    public void test004SwapPlanFUPTransferScenarioA4(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #4 - On Suspending a customer the end date should be later than start date with remaining quantity being prorated");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                // create new ageing steps
                cleanAgeingSteps();
                api.saveAgeingConfigurationWithCollectionType(buildAgeingSteps(api), api.getCallerLanguageId(), CollectionType.REGULAR);

                AgeingWS[] ageingList = api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR);

                validateStep(ageingList[0], ACTIVE_PAST_DUE_STEP, Integer.valueOf(1), Boolean.TRUE, Boolean.FALSE,
                        Boolean.FALSE, Boolean.FALSE);
                validateStep(ageingList[1], ACTIVE_NOTICE_TO_BLOCK_STEP, Integer.valueOf(3), Boolean.TRUE, Boolean.FALSE,
                        Boolean.TRUE, Boolean.FALSE);
                validateStep(ageingList[2], ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, Integer.valueOf(18), Boolean.TRUE, Boolean.FALSE,
                        Boolean.FALSE, Boolean.FALSE);
                validateStep(ageingList[3], CANCELLED_CUSTOMER_REQUEST_STEP, Integer.valueOf(100), Boolean.TRUE, Boolean.FALSE,
                        Boolean.FALSE, Boolean.TRUE);
                validateStep(ageingList[4], CANCELLED_NOTICE_TO_COLLECTION_STEP, Integer.valueOf(125), Boolean.TRUE, Boolean.FALSE,
                        Boolean.FALSE, Boolean.TRUE);
                validateStep(ageingList[5], CANCELLED_TP_COLLECTIONS_STEP, Integer.valueOf(150), Boolean.TRUE, Boolean.FALSE,
                        Boolean.FALSE, Boolean.TRUE);

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_04,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(ORDER_04, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "01/10/2018");
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_04));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug("Usage pool start date and end date before plan swap and after mediation is run");
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("100.00"));

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_04));
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));

                api.createUpdateOrder(order,orderChanges);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_04));

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug("Usage pool start date and end date after plan swap");
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("200.00"));

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.selectUserByName(USER_04)
                .generateInvoice(nextInvoiceDate.getTime(), false);

                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_04));

                Date invoiceDueDate = invoice.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(user.getId(), ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug("Usage pool start date and end date after billing process and ageing is run and customer is not suspended");
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(user.getId(), ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                logger.debug("Usage pool start date and end date when customer is suspended");
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));

            });
        } finally {
            cleanUp(USER_04);
        }
    }

    private void cleanAgeingSteps() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            AgeingWS[] ageingWSs = api.getAgeingConfiguration(api.getCallerLanguageId());
            for(AgeingWS ageingTemp : ageingWSs){
                if(ageingTemp.getInUse()){
                    updateAgedUsersStatusToActive(api);
                    logger.debug("status id : {}  days : {}  in use : {}",ageingTemp.getStatusId(), ageingTemp.getDays(), ageingTemp.getInUse());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

    private void updateAgedUsersStatusToActive(JbillingAPI api) {
        Integer[] agedUsers = api.getUsersNotInStatus(1);
        for(Integer id : agedUsers){
            logger.debug("aged user id : {} ",id);
            updateCustomerStatusToActive(id, api);
        }
    }

    @Test
    public void test005SwapPlanFUPTransferScenarioA5() {

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #5 - Plan swap when period based discount is applied");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_05,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(ORDER_05, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_05));

                DiscountWS discount = createDiscount(envBuilder,api,null,"6","1"," 10% RECURRING",DiscountStrategyType.RECURRING_PERIODBASED,"PERIODBASED%123");
                order.setDiscountLines(createDiscountLines(order, discount));
                order.setProrateFlag(Boolean.TRUE);    // Enable prorating on the main order, so discount gets prorated as well.
                discountId = discount.getId();
                OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);

                for (OrderChangeWS change : changes) {
                    change.setStartDate(order.getActiveSince());
                }

                api.createUpdateOrder(order, changes);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_05));
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("100.00"));
                OrderWS order = api.getOrder(environment.idForCode(ORDER_05));

                assertNotNull("mainOrder is null.", order);
                OrderWS[] linkedOrders = order.getChildOrders();
                assertNotNull("linkedOrders is null.", linkedOrders);

                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));

                api.createUpdateOrder(order,orderChanges);
                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("200.00"));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteDiscount(discountId);
            cleanUp(USER_05);
        }

    }

    @Test
    public void test006SwapPlanFUPTransferScenarioA6() {

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #6 - Plan swap when one time percentage discount is applied");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_06,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(ORDER_06, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_06));

                DiscountWS discount = createDiscount(envBuilder,api,null,"1","1","10% ONETIME",DiscountStrategyType.ONE_TIME_PERCENTAGE,"ONETIME%123");
                order.setDiscountLines(createDiscountLines(order, discount));
                order.setProrateFlag(Boolean.TRUE);    // Enable prorating on the main order, so discount gets prorated as well.
                discountId = discount.getId();
                OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);

                for (OrderChangeWS change : changes) {
                    change.setStartDate(order.getActiveSince());
                }

                api.createUpdateOrder(order, changes);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_06));
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("100.00"));
                OrderWS order = api.getOrder(environment.idForCode(ORDER_06));

                assertNotNull("mainOrder is null.", order);
                OrderWS[] linkedOrders = order.getChildOrders();
                assertNotNull("linkedOrders is null.", linkedOrders);

                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                api.createUpdateOrder(order,orderChanges);
                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("200.00"));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteDiscount(discountId);
            cleanUp(USER_06);
        }

    }

    @Test
    public void test007SwapPlanFUPTransferScenarioA7() {

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #7 - Plan swap when one time flat discount is applied");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_07,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(ORDER_07, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);

                OrderWS order = api.getOrder(environment.idForCode(ORDER_07));

                DiscountWS discount = createDiscount(envBuilder,api,null,"1","0","$10 ONETIMEFLAT",DiscountStrategyType.ONE_TIME_AMOUNT,"ONETIMEFLAT123");
                order.setDiscountLines(createDiscountLines(order, discount));
                order.setProrateFlag(Boolean.TRUE);    // Enable prorating on the main order, so discount gets prorated as well.
                discountId = discount.getId();
                OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);

                for (OrderChangeWS change : changes) {
                    change.setStartDate(order.getActiveSince());
                }

                api.createUpdateOrder(order, changes);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_07));
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("100.00"));
                OrderWS order = api.getOrder(environment.idForCode(ORDER_07));

                assertNotNull("mainOrder is null.", order);
                OrderWS[] linkedOrders = order.getChildOrders();
                assertNotNull("linkedOrders is null.", linkedOrders);

                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        environment.idForCode(SUBSCRIPTION_PROD_01),
                        environment.idForCode(SUBSCRIPTION_PROD_02),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                api.createUpdateOrder(order,orderChanges);
                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("200.00"));

                Calendar cycleStartDate = Calendar.getInstance();
                cycleStartDate.set(Calendar.YEAR, 2018);
                cycleStartDate.set(Calendar.MONTH, 0);
                cycleStartDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                //Check CUSTOMER USAGE POOLS cycle start date and cycle end date.
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartDate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteDiscount(discountId);
            cleanUp(USER_07);
        }

    }
    
    /**
     * @author Murali
     * JB-3245 : Zendesk #40997 : Debit Adjustment Order being created with incorrect Active Since date
     */
    
    @Test
    public void test008SwapPlanFUPTransferScenarioA8(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario #8 - Debit Adjustment Order being created with incorrect Active Since date");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_08,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                        //Creating subscription order on 1st ofJan 2018
                        .createOrder(ORDER_08, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                                ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false)
                        .generateInvoice(nextInvoiceDate.getTime(), false);
                
                OrderWS order = api.getOrder(environment.idForCode(ORDER_08));
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, 
                        environment.idForCode(SUBSCRIPTION_PROD_01), 
                        environment.idForCode(SUBSCRIPTION_PROD_02), 
                        SwapMethod.DIFF, 
                        Util.truncateDate(order.getActiveSince()));
                api.createUpdateOrder(order,orderChanges);
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_08));
                Integer userId = user.getId();
                Integer[] ordersList = api.getLastOrders(userId, 2);
                for(int i=0; i<ordersList.length; i++){                	
					OrderWS orderWS = api.getOrder(ordersList[i]);
					Double debitAdjOrderAmount = Double.valueOf(orderWS.getTotal());
                	if(debitAdjOrderAmount == 100.0000000000){
                      assertEquals(parseDate(orderWS.getActiveSince()), parseDate(activeSince.getTime()));
                	}
                }
            });
        } finally {
            cleanUp(USER_08);
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
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

    private Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, JbillingAPI api, String username,
            Integer accountTypeId, Date nextInvoiceDate, Integer periodId, Integer nextInvoiceDay) {

        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(username)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate)
                .withMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay))
                .build();
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        return userWS.getId();
    }

    private Integer buildAndPersistOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, Integer userId,
            Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId,
            boolean prorate, Map<Integer, BigDecimal> productQuantityMap) {

        OrderBuilder orderBuilder = envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(activeSince)
                .withPeriod(orderPeriodId)
                .withBillingTypeId(billingTypeId)
                .withProrate(prorate);

        for (Map.Entry<Integer, BigDecimal> entry : productQuantityMap.entrySet()) {
            orderBuilder.withOrderLine(
                    orderBuilder.orderLine()
                    .withItemId(entry.getKey())
                    .withQuantity(entry.getValue())
                    .build());
        }
        return orderBuilder.build();
    }

    private Integer createOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api,String code,Integer userId,Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId, int statusId,
            boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, Integer> productAssetMap, boolean createNegativeOrder) {
        List<OrderLineWS> lines = productQuantityMap.entrySet()
                .stream()
                .map((lineItemQuatityEntry) -> {
                    OrderLineWS line = new OrderLineWS();
                    line.setItemId(lineItemQuatityEntry.getKey());
                    line.setTypeId(Integer.valueOf(1));
                    ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                    line.setDescription(item.getDescription());
                    line.setQuantity(lineItemQuatityEntry.getValue());
                    line.setUseItem(true);
                    if(createNegativeOrder) {
                        line.setUseItem(false);
                        line.setPrice(item.getPriceAsDecimal().negate());
                        line.setAmount(line.getQuantityAsDecimal().multiply(line.getPriceAsDecimal()));
                    }
                    if(null!=productAssetMap && !productAssetMap.isEmpty() 
                            && productAssetMap.containsKey(line.getItemId())) {
                        line.setAssetIds(new Integer[] {productAssetMap.get(line.getItemId())});
                    }
                    return line;
                }).collect(Collectors.toList());

        return envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(activeSince)
                .withPeriod(orderPeriodId)
                .withBillingTypeId(billingTypeId)
                .withProrate(prorate)
                .withOrderLines(lines)
                .withOrderChangeStatus(statusId)
                .build();
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

    private List<String> buildInboundCDR(List<String> indentifiers, String quantity, String eventDate) {
        List<String> cdrs = new ArrayList<String>();
        indentifiers.forEach(asset -> {
            cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()+",6165042651,tressie.johnson,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null");
        });
        return cdrs;
    }

    private String parseDate(Date date) {
        if(date == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }

    private AgeingWS[] buildAgeingSteps(JbillingAPI api) {
        AgeingWS[] ageingSteps = new AgeingWS[6];
        ageingSteps[0] = buildAgeingStep(ACTIVE_PAST_DUE_STEP, 1, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[1] = buildAgeingStep(ACTIVE_NOTICE_TO_BLOCK_STEP, 3, true, false, true, false, api, CollectionType.REGULAR);
        ageingSteps[2] = buildAgeingStep(ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, 18, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[3] = buildAgeingStep(CANCELLED_CUSTOMER_REQUEST_STEP, 100, true, false, false, true, api, CollectionType.REGULAR);
        ageingSteps[4] = buildAgeingStep(CANCELLED_NOTICE_TO_COLLECTION_STEP, 125, true, false, false, true, api, CollectionType.REGULAR);
        ageingSteps[5] = buildAgeingStep(CANCELLED_TP_COLLECTIONS_STEP, 150, true, false, false, true, api, CollectionType.REGULAR);
        return ageingSteps;
    }

    private AgeingWS buildAgeingStep(String statusStep,Integer days,
            boolean sendNotification , boolean payment, boolean suspended, boolean stopActivationOnPayment, JbillingAPI api, CollectionType collectionType){

        AgeingWS ageingWS = new AgeingWS();
        ageingWS.setEntityId(api.getCallerCompanyId());
        ageingWS.setStatusStr(statusStep);
        ageingWS.setDays(days);
        ageingWS.setPaymentRetry(Boolean.valueOf(payment));
        ageingWS.setSendNotification(Boolean.valueOf(sendNotification));
        ageingWS.setSuspended(Boolean.valueOf(suspended));
        ageingWS.setStopActivationOnPayment(stopActivationOnPayment);
        ageingWS.setCollectionType(collectionType);
        return  ageingWS;
    }

    private void validateStep(AgeingWS ageingWS, String statusStr, Integer days,
            Boolean sendNotification , Boolean payment, Boolean suspended, Boolean stopActivationOnPayment ){

        assertEquals(ageingWS.getStatusStr(), statusStr, "Invalid Step name");
        assertEquals(ageingWS.getDays(), days, "Invalid number of days");
        assertEquals(ageingWS.getPaymentRetry(), payment, "Invalid payment check");
        assertEquals(ageingWS.getSendNotification(), sendNotification, "Invalid notification check");
        assertEquals(ageingWS.getSuspended(), suspended, "Invalid suspended check");
        assertEquals(ageingWS.getStopActivationOnPayment(), stopActivationOnPayment, "Invalid stopActivationOnPayment check");
    }

    private void validateUserAndInvoiceStatus(Integer userId, String userStatus, Integer invoiceStatus, JbillingAPI api) {
        UserWS user = api.getUserWS(userId);
        assertEquals(user.getStatus(), userStatus);
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertEquals(invoice.getStatusId(), invoiceStatus);
    }

    private DiscountWS createDiscount(TestEnvironmentBuilder envBuilder, JbillingAPI api,Date discountStartDate, String periodValue,String percentageFlag,String description,DiscountStrategyType type,String code) {
        Calendar startOfThisMonth = Calendar.getInstance();
        startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);

        Calendar afterOneMonth = Calendar.getInstance();
        afterOneMonth.setTime(startOfThisMonth.getTime());
        afterOneMonth.add(Calendar.MONTH, 1);

        DiscountWS discountWs = new DiscountWS();
        discountWs.setCode(code);
        discountWs.setDescription(description);
        discountWs.setRate(BigDecimal.TEN);
        discountWs.setType(type.name());

        SortedMap<String, String> attributes = new TreeMap<String, String>();
        attributes.put("periodUnit", "2");    // period unit month
        attributes.put("periodValue", periodValue);
        attributes.put("isPercentage", percentageFlag);    // Consider rate as amount

        discountWs.setAttributes(attributes);

        if (null != discountStartDate) {
            discountWs.setStartDate(discountStartDate);
        }

        Integer discountId = api.createOrUpdateDiscount(discountWs);
        return api.getDiscountWS(discountId);
    }

    private DiscountLineWS[] createDiscountLines(OrderWS order, DiscountWS discount) {

        // Period based Amount Discount applied at Order level
        DiscountLineWS periodBasedAmountOrderLevel = new DiscountLineWS();
        periodBasedAmountOrderLevel.setDiscountId(discount.getId());
        periodBasedAmountOrderLevel.setOrderId(order.getId());
        periodBasedAmountOrderLevel.setDescription(discount.getDescription() + " Discount On Order Level");

        DiscountLineWS discountLines[] = new DiscountLineWS[1];
        discountLines[0] = periodBasedAmountOrderLevel;        // Period Based Amount Discount applied on Order level

        // return discount lines
        return discountLines;
    }
}
