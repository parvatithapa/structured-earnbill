package com.sapienter.jbilling.server.task;

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

import org.hamcrest.generator.qdox.junit.APITestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
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
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;



@Test(groups = { "integration", "task", "tax", "countrytax" }, testName = "InvoiceCompositionTaskTest")
public class OrderChangeBasedCompositionTaskTest extends APITestCase {
    private static final Logger logger = LoggerFactory.getLogger(OrderChangeBasedCompositionTaskTest.class);
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;

    private final static Integer CC_PM_ID = 5;
    private static final Integer nextInvoiceDay = 1;
    private static final Integer postPaidOrderTypeId = Constants.ORDER_BILLING_POST_PAID;
    private static final Integer prePaidOrderTypeId = Constants.ORDER_BILLING_PRE_PAID;
    public final static int MONTHLY_ORDER_PERIOD = 2;
    public final static int ONE_TIME_ORDER_PERIOD = 1;
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;

    private String adjustmentCategory = "AdjustmentUsageCategory";
    private String usageCategory = "MediatedUsageCategory";

    private String adjustmenProduct = "First Month Promo Off";
    private String usageProduct = "MediatedUsageProduct";
    private String testAccount = "Account Type";
    private String linePercentageProduct = "Line Percentage product";
    private String SCENARIO_01_USER = "testScenario01User"+System.currentTimeMillis();
    private String SCENARIO_01_ONE_TIME_ORDER = "testScenario01OneTimeOrder";

    private String SCENARIO_02_USER = "testScenario02User"+System.currentTimeMillis();
    private String SCENARIO_02_MONTHLY_ORDER = "testScenario02MonthlyOrder";

    private String SCENARIO_03_USER = "testScenario03User"+System.currentTimeMillis();
    private String SCENARIO_03_MONTHLY_ORDER = "testScenario03MonthlyOrder";

    private String SCENARIO_04_USER = "testScenario04User"+System.currentTimeMillis();
    private String SCENARIO_04_ONE_TIME_ORDER = "testScenario04OneTimeOrder";
    
    BillingProcessConfigurationWS configBackup;
    BillingProcessConfigurationWS config;

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            // Creating account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
            // Creating usage category
            buildAndPersistCategory(envBuilder, api, usageCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            // Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, usageProduct, false, envBuilder.idForCode(usageCategory), "54.54", true);
            // Creating Adjustment category
            buildAndPersistCategory(envBuilder, api, adjustmentCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT);
            // Creating SetUp adjustment product
            buildAndPersistFlatProduct(envBuilder, api, adjustmenProduct, false, envBuilder.idForCode(adjustmentCategory), "54.54", false);
            //Creating Linepercentage product
            buildAndPersistLinePercentageProduct(envBuilder, api, linePercentageProduct, false, envBuilder.idForCode(adjustmentCategory), "100", false);
            //configure plugin
            updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);

            configBackup = api.getBillingProcessConfiguration();

            config = api.getBillingProcessConfiguration();
            // just change it to a day
            config.setMaximumPeriods(1);
            api.createUpdateBillingProcessConfiguration(config);


        }).test((testEnv, testEnvBuilder) -> {
            
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("Category with item type Creation Failed", testEnvBuilder.idForCode(usageCategory));
            assertNotNull("Flat product for category with item type Creation Failed", testEnvBuilder.idForCode(usageProduct));
            assertNotNull("Category with adjustment type Creation Failed", testEnvBuilder.idForCode(adjustmentCategory));
            assertNotNull("Flat Product for category with adjustment type Creation Failed", testEnvBuilder.idForCode(adjustmenProduct));
            assertNotNull("Line percentage product for category with adjustment type Creation Failed", testEnvBuilder.idForCode(adjustmenProduct));
        });
    }


    /**
     * testInvoiceCompositionTask with Order change based composition task and with one time adjustment order.
     *
     */
    @Test
    public void test01InvoiceCompositionTask() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario # testInvoiceCompositionTask with Orderline based composition task.");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2016);
                nextInvoiceDate.set(Calendar.MONTH, 10);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2016);
                activeSince.set(Calendar.MONTH, 10);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(SCENARIO_01_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
                .createOrder(SCENARIO_01_ONE_TIME_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, prePaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        Collections.singletonMap(environment.idForCode(usageProduct), BigDecimal.ONE), null, false)
                .createOrder(SCENARIO_01_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                                Collections.singletonMap(environment.idForCode(adjustmenProduct), BigDecimal.ONE), null, true);


                //Validating invoice summary data generated on 01-Sept-2016
            }).validate((testEnv, envBuilder) -> {
                Calendar runDate = Calendar.getInstance();
                runDate.set(Calendar.YEAR, 2016);
                runDate.set(Calendar.MONTH, 10);
                runDate.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenarioBuilder.selectUserByName(SCENARIO_01_USER)
                // generating invoice for 1'st Sep 2016
                .generateInvoice(runDate.getTime(), true);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_01_USER));
                assertNotNull("Invoice Creation Failed",invoice);
                InvoiceLineDTO[] lines = invoice.getInvoiceLines();
                assertNotNull("Invoice line Creation Failed",lines);
                //Check Order lines
                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertTrue("Invoce line description contains Period from", lines[0].getDescription().contains("Period from"));


                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[1].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertFalse("Invoce line description contains Period from", lines[1].getDescription().contains("Period from"));


            });
        }finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_01_USER), null, 0))
                .forEach(invoice -> {
                    api.deleteInvoice(invoice.getId());
                });
        }
    }

    /**
     * testInvoiceCompositionTask with Order Change based composition task and with monthly adjustment order with product containing flat price.
     *
     */
    @Test
    public void test02InvoiceCompositionTask() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario # testInvoiceCompositionTask with Orderline based composition task.");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2016);
                nextInvoiceDate.set(Calendar.MONTH, 10);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2016);
                activeSince.set(Calendar.MONTH, 9);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                
                InvoiceSummaryScenarioBuilder scenario02 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario02.createUser(SCENARIO_02_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
                .createOrder(SCENARIO_02_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, prePaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        Collections.singletonMap(environment.idForCode(usageProduct), BigDecimal.ONE), null, false)
                .createOrder(SCENARIO_02_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                                Collections.singletonMap(environment.idForCode(adjustmenProduct), BigDecimal.ONE), null, true);


                //Validating invoice summary data generated on 01-Sept-2016
            }).validate((testEnv, envBuilder) -> {
                Calendar runDate = Calendar.getInstance();
                runDate.set(Calendar.YEAR, 2016);
                runDate.set(Calendar.MONTH, 10);
                runDate.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenarioBuilder.selectUserByName(SCENARIO_02_USER)
                // generating invoice for 1'st Sep 2016
                .generateInvoice(runDate.getTime(), true);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
                assertNotNull("Invoice Creation Failed",invoice);
                InvoiceLineDTO[] lines = invoice.getInvoiceLines();
                assertNotNull("Invoice line Creation Failed",lines);
                //Check Order lines
                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertTrue("Invoce line description contains Period from", lines[0].getDescription().contains("Period from"));


                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[1].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertTrue("Invoce line description contains Period from", lines[1].getDescription().contains("Period from"));


            });
        }finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_02_USER), null, 0))
                .forEach(invoice -> {
                    api.deleteInvoice(invoice.getId());
                });
        }
    }


    /**
     * testInvoiceCompositionTask with Order line based composition task and with monthly adjustment order with product containing flat price.
     *
     */
    @Test
    public void test03InvoiceCompositionTask() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {

            //configure plugin
            updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_LINE_BASED_COMPOSITION_TASK, testBuilder, null);

            testBuilder.given(envBuilder -> {
                logger.debug("Scenario # testInvoiceCompositionTask with Orderline based composition task.");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2016);
                nextInvoiceDate.set(Calendar.MONTH, 10);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2016);
                activeSince.set(Calendar.MONTH, 9);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                InvoiceSummaryScenarioBuilder scenario02 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario02.createUser(SCENARIO_03_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
                .createOrder(SCENARIO_03_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, prePaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        Collections.singletonMap(environment.idForCode(usageProduct), BigDecimal.ONE), null, false)
                .createOrder(SCENARIO_03_MONTHLY_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                                Collections.singletonMap(environment.idForCode(adjustmenProduct), BigDecimal.ONE), null, true);


                //Validating invoice summary data generated on 01-Sept-2016
            }).validate((testEnv, envBuilder) -> {
                Calendar runDate = Calendar.getInstance();
                runDate.set(Calendar.YEAR, 2016);
                runDate.set(Calendar.MONTH, 10);
                runDate.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenarioBuilder.selectUserByName(SCENARIO_02_USER)
                // generating invoice for 1'st Sep 2016
                .generateInvoice(runDate.getTime(), true);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_02_USER));
                assertNotNull("Invoice Creation Failed",invoice);
                InvoiceLineDTO[] lines = invoice.getInvoiceLines();
                assertNotNull("Invoice line Creation Failed",lines);
                //Check Order lines
                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertTrue("Invoce line description contains Period from", lines[0].getDescription().contains("Period from"));


                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[1].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertTrue("Invoce line description contains Period from", lines[1].getDescription().contains("Period from"));


            });
        }finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_03_USER), null, 0))
                .forEach(invoice -> {
                    api.deleteInvoice(invoice.getId());
                });
        }
    }


    /**
     * testInvoiceCompositionTask with Order line based composition task and with one time adjustment order.
     *
     */
    @Test
    public void test04InvoiceCompositionTask() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario # testInvoiceCompositionTask with Orderline based composition task.");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2016);
                nextInvoiceDate.set(Calendar.MONTH, 10);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2016);
                activeSince.set(Calendar.MONTH, 9);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(SCENARIO_04_USER,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay)
                .createOrder(SCENARIO_04_ONE_TIME_ORDER, activeSince.getTime(), null, MONTHLY_ORDER_PERIOD, prePaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        Collections.singletonMap(environment.idForCode(usageProduct), BigDecimal.ONE), null, false)
                .createOrder(SCENARIO_04_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                                Collections.singletonMap(environment.idForCode(adjustmenProduct), BigDecimal.ONE), null, true);

                //Validating invoice summary data generated on 01-Sept-2016
            }).validate((testEnv, envBuilder) -> {
                Calendar runDate = Calendar.getInstance();
                runDate.set(Calendar.YEAR, 2016);
                runDate.set(Calendar.MONTH, 10);
                runDate.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenarioBuilder.selectUserByName(SCENARIO_01_USER)
                // generating invoice for 1'st Sep 2016
                .generateInvoice(runDate.getTime(), true);
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(SCENARIO_01_USER));
                assertNotNull("Invoice Creation Failed",invoice);
                InvoiceLineDTO[] lines = invoice.getInvoiceLines();
                assertNotNull("Invoice line Creation Failed",lines);
                //Check Order lines
                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertTrue("Invoce line description contains Period from", lines[0].getDescription().contains("Period from"));


                assertEquals("Invoice line Amount ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("54.54").setScale(2, BigDecimal.ROUND_HALF_UP).negate(),lines[1].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[1].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertFalse("Invoce line description contains Period from", lines[1].getDescription().contains("Period from"));


            });
        }finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_04_USER), null, 0))
                .forEach(invoice -> {
                    api.deleteInvoice(invoice.getId());
                });
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
    
    private TestBuilder getTestEnvironment() {

        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {

            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
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
    
    public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {

        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();

        return accountTypeWS.getId();
    }

    @AfterClass
    public void tearDown() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi(); 
            api.createUpdateBillingProcessConfiguration(configBackup);
        });
       //Configuring Order Change Based Composition Task
        updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID,
                FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);

        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        if (null != testBuilder) {
            testBuilder = null;
        }
    }

}
