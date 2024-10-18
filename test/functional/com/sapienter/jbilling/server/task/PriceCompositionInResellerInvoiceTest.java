package com.sapienter.jbilling.server.task;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hamcrest.generator.qdox.junit.APITestCase;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;



@Test(groups = { "integration", "task", "tax", "countrytax" }, testName = "PriceCompositionInResellerInvoiceTest", priority = 19)
public class PriceCompositionInResellerInvoiceTest extends APITestCase {
    private static final Logger logger = LoggerFactory.getLogger(PriceCompositionInResellerInvoiceTest.class);
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;

    private final static Integer RE_CC_PM_ID = 4;
    private static final Integer nextInvoiceDay = 1;
    private static final Integer postPaidOrderTypeId = Constants.ORDER_BILLING_POST_PAID;
    public final static int MONTHLY_ORDER_PERIOD = 2;
    public final static int ONE_TIME_ORDER_PERIOD = 1;
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    public static final int RESELLER_ORDER_CHANGE_STATUS_APPLY_ID = 5;

    private String taxCategory = "TaxCategory";
    private String usageCategory = "MediatedUsageCategory";

    private String usageProduct = "UsageProduct";
    private String resellerTestAccount = "Reseller Account Type";
    private String taxPercentageProduct = "Tax Percentage product";

    private static String SCENARIO_01_USER = "testScenario01User";
    private String SCENARIO_01_ONE_TIME_ORDER = "testScenario01OneTimeOrder";

    BillingProcessConfigurationWS configBackup;
    BillingProcessConfigurationWS config;

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();

        testBuilder.given(envBuilder -> {
        	// parent api for creating global products in parent
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            // reseller api for creating account type in reseller
            final JbillingAPI resellerApi = envBuilder.getResellerApi();
            // Creating account type
            buildAndPersistAccountType(envBuilder, resellerApi, resellerTestAccount, RE_CC_PM_ID);
            // Creating usage category
            buildAndPersistCategory(envBuilder, api, usageCategory, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            // Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, usageProduct, true, envBuilder.idForCode(usageCategory), "50.50", true);
            // Creating Tax category
            buildAndPersistCategory(envBuilder, api, taxCategory, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_TAX);
            // Creating taxPercentage Product
            buildAndPersistLinePercentageProduct(envBuilder, api, taxPercentageProduct, true, envBuilder.idForCode(taxCategory), "5.5", true);
            //configure plugin
            updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);

            configBackup = api.getBillingProcessConfiguration();

            config = api.getBillingProcessConfiguration();
            // just change it to a day
            config.setMaximumPeriods(1);
            api.createUpdateBillingProcessConfiguration(config);


        }).test((testEnv, testEnvBuilder) -> {

            assertNotNull("Reseller Account Creation Failed", testEnvBuilder.idForCode(resellerTestAccount));
            assertNotNull("Category with item type Creation Failed", testEnvBuilder.idForCode(usageCategory));
            assertNotNull("Flat product for category with item type Creation Failed", testEnvBuilder.idForCode(usageProduct));
            assertNotNull("Category with tax type Creation Failed", testEnvBuilder.idForCode(taxCategory));
            assertNotNull("Line percentage product for category with tax type Creation Failed", testEnvBuilder.idForCode(taxPercentageProduct));
        });
    }

    /**
     * testAbstractChargeTask with different tax value in global and child company.
     *
     */
    @Test
    public void test01AbstractChargeTask() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                logger.debug("Scenario # test01AbstractChargeTask with different tax value in global and child company.");
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2016);
                nextInvoiceDate.set(Calendar.MONTH, 10);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2016);
                activeSince.set(Calendar.MONTH, 10);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                //final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final JbillingAPI resellerApi = envBuilder.getResellerApi();

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(resellerApi, SCENARIO_01_USER, envBuilder.idForCode(resellerTestAccount), nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, nextInvoiceDay);

                Integer userId = resellerApi.getUserId(SCENARIO_01_USER);

                Integer itemId = envBuilder.idForCode(taxPercentageProduct);
                ItemDTOEx itemDTOEx = resellerApi.getItem(itemId, userId, null);
                SortedMap<Date, PriceModelWS> prices1 =new TreeMap<Date, PriceModelWS>();

                PriceModelWS newLinePercentagePrice = new PriceModelWS();
                newLinePercentagePrice.setType(PriceModelStrategy.LINE_PERCENTAGE.name());
                newLinePercentagePrice.setRate(new BigDecimal(8.5));

                prices1.put(new Date(), newLinePercentagePrice);
                itemDTOEx.setDefaultPrices(prices1);

                itemDTOEx.setEntityId(3);
                itemDTOEx.setPriceModelCompanyId(3);
                resellerApi.updateItem(itemDTOEx);

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(environment.idForCode(usageProduct), BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(taxPercentageProduct), BigDecimal.ONE);

                scenario01.createOrder(resellerApi,userId,SCENARIO_01_ONE_TIME_ORDER, activeSince.getTime(), null, ONE_TIME_ORDER_PERIOD, postPaidOrderTypeId, RESELLER_ORDER_CHANGE_STATUS_APPLY_ID, true,
                			productQuantityMap);


                //Validating invoice summary data generated on 01-Sept-2016
            }).validate((testEnv, envBuilder) -> {
                Calendar runDate = Calendar.getInstance();
                runDate.set(Calendar.YEAR, 2016);
                runDate.set(Calendar.MONTH, 10);
                runDate.set(Calendar.DAY_OF_MONTH, 1);
                // Reseller API for creating invoice;
                final JbillingAPI resellerApi = envBuilder.getResellerApi();
                InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
                // generating invoice for 1'st Sep 2016
                Integer userId = resellerApi.getUserId(SCENARIO_01_USER);
                OrderWS order = resellerApi.getLatestOrder(userId);
                scenarioBuilder.generateInvoice(resellerApi,userId,order.getId(), null);
                InvoiceWS invoice = resellerApi.getLatestInvoice(envBuilder.idForCode(SCENARIO_01_USER));
                assertNotNull("Invoice Creation Failed",invoice);
                InvoiceLineDTO[] lines = invoice.getInvoiceLines();
                assertNotNull("Invoice line Creation Failed",lines);
                //Check Order lines
                assertEquals("Invoice line price ",new BigDecimal("50.50").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line quantity ",new BigDecimal("1.00").setScale(2, BigDecimal.ROUND_HALF_UP),lines[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invoice line price ",new BigDecimal("8.5").setScale(2, BigDecimal.ROUND_HALF_UP),lines[1].getPriceAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            });
        }finally {
            final JbillingAPI resellerApi = testBuilder.getTestEnvironment().getResellerApi();
            Arrays.stream(resellerApi.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(SCENARIO_01_USER), null, 0))
                .forEach(invoice -> {
                	resellerApi.deleteInvoice(invoice.getId());
                });
        }
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

            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi(), testEnvCreator.getResellerApi());
        });
    }

    private Integer buildAndPersistLinePercentageProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String percentage, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withLinePercentage(percentage,new DateTime(1970, 01, 01, 0, 0, 0, 0).withTime(0, 0, 0, 0).toDate())
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {

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
        envHelper = null;
        testBuilder = null;
    }

}
