package com.sapienter.jbilling.server.prepaidswapplan;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

/**
 * 
 * @author Dipak Kardel
 * Date:19-March-2018
 *
 */

@Test(groups = { "prepaid-swapPlan" }, testName = "PrepaidSubscriptionOrderBillingProcessScenariosTest")
public class PrepaidSubscriptionOrderBillingProcessScenariosTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String SUBSCRIPTION_PROD_01 = "testPlanSubscriptionItem_01";
    private static final String USAGE_POOL_01 = "UP with 100 Quantity"+System.currentTimeMillis();
    private static final String PLAN_01 = "100 free minute Plan";
    private static final String USER_01 = "Test-PP-Subscpt-Order-Billing-User-1";
    private static final String USER_02 = "Test-PP-Subscpt-Order-Billing-User-2";
    private static final String USAGEPOOL_LOGGER_MSG = "Cycle start date: {} Cycle end date: {}";
    private static final Integer CC_PM_ID = 5;
    private static final Integer NEXT_INVOICE_DAY = 1;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    // Mediated Usage Products
    private static final int INBOUND_USAGE_PRODUCT_ID = 320101;
    private static final int CHAT_USAGE_PRODUCT_ID = 320102;
    private static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
    private static Integer customerUsagePoolEvalutionPluginId;
    private String testAccount = "Account Type";
    private String testCat1 = "MediatedUsageCategory";
    private Integer accTypeId;
    private BillingProcessConfigurationWS oldBillingProcessConfig;
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            PluggableTaskTypeWS pluginType = api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.usagePool.task.SwapPlanFUPTransferTask");
            PluggableTaskWS swapPlanFUPTransferPlugin = api.getPluginWSByTypeId(pluginType.getId());
            Hashtable<String, String> swapPlanFUPTransferPluginparameters = new Hashtable<>();
            swapPlanFUPTransferPluginparameters.put("adjustment_product_id", "320111");
            swapPlanFUPTransferPlugin.setParameters(swapPlanFUPTransferPluginparameters);
            api.updatePlugin(swapPlanFUPTransferPlugin);

            oldBillingProcessConfig = api.getBillingProcessConfiguration();
            BillingProcessConfigurationWS newBillingProcessConfig = api.getBillingProcessConfiguration();
            newBillingProcessConfig.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());
            api.createUpdateBillingProcessConfiguration(newBillingProcessConfig);

            PluggableTaskWS customerUsagePoolEvaluationTask= new PluggableTaskWS();
            customerUsagePoolEvaluationTask.setProcessingOrder(523);
            customerUsagePoolEvaluationTask.setTypeId(118);
            customerUsagePoolEvaluationTask.setNotes("customerUsagePoolEvaluationTask");
            customerUsagePoolEvalutionPluginId = api.createPlugin(customerUsagePoolEvaluationTask);

            // Creating account type
            accTypeId = buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            // Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_01, false, envBuilder.idForCode(testCat1), "99", true);
            // Usage product item ids 
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2018);
            pricingDate.set(Calendar.MONTH, 0);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);
            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());

            // creating usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder,api,USAGE_POOL_01, "100", envBuilder.idForCode(testCat1), items);

            // creating 100 min plan
            buildAndPersistPlan(envBuilder,api, PLAN_01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
                    envBuilder.idForCode(SUBSCRIPTION_PROD_01), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),planItemProd01WS, planItemProd02WS, planItemProd03WS);
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_01));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_01));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_01));
        });
    }

    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        api.createUpdateBillingProcessConfiguration(oldBillingProcessConfig);
        if(null != customerUsagePoolEvalutionPluginId){
            api.deletePlugin(customerUsagePoolEvalutionPluginId);
        }
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    @Test
    public void test001PrepaidSubscriptionOrderBillingProcessScenarioA1(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                //Create User with Next Invoice Date 1st Jan 2018
                buildAndPersistCustomer(envBuilder, api, USER_01, accTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);

                //Create Order with Active since 1st Jan 2018
                buildAndPersistOrder(envBuilder, api, "testSubScriptionOrderO1", envBuilder.idForCode(USER_01), nextInvoiceDate.getTime(), null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_PRE_PAID,
                        true, productQuantityMap);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //fetching user and checking next invoice date
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));
                assertEquals(parseDate(user.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //generating invoice for invoice date 1 Jan 2018
                api.createInvoiceWithDate(user.getId(),nextInvoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 1);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //updating users Next Invoice Date to 1 Feb 2018
                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);
                user = api.getUserWS(envBuilder.idForCode(USER_01));

                assertEquals(parseDate(user.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                //Creating one time order active since 1 Jan 2018
                createOneTimeOrder(api,user.getUserId(),activeSince.getTime(),"120");

                Calendar cycleStartdate = Calendar.getInstance();
                cycleStartdate.set(Calendar.YEAR, 2018);
                cycleStartdate.set(Calendar.MONTH, 0);
                cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

                //asserting start date and end date of customer field usage pool
                logger.debug("Cycle start date: {} Cycle end date {}", parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 1);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));

                //generating invoice for invoice date 1 Feb 2018
                api.createInvoiceWithDate(envBuilder.idForCode(USER_01),nextInvoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 2);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //updating users Next Invoice Date to 1 March 2018
                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);
                api.triggerScheduledTask(customerUsagePoolEvalutionPluginId , new Date());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                user = api.getUserWS(envBuilder.idForCode(USER_01));
                assertEquals(parseDate(nextInvoiceDate.getTime()),parseDate(user.getNextInvoiceDate()));

                Calendar cycleStartdate = Calendar.getInstance();
                cycleStartdate.set(Calendar.YEAR, 2018);
                cycleStartdate.set(Calendar.MONTH, 1);
                cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 1);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 28);

                //Creating one time order active since 1 Jan 2018
                createOneTimeOrder(api,user.getUserId(),activeSince.getTime(),"150");

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

                //asserting start date and end date of customer field usage pool
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 2);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));
                //generating invoice for invoice date 1 March 2018
                api.createInvoiceWithDate(envBuilder.idForCode(USER_01),nextInvoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 3);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //updating users Next Invoice Date to 1 April 2018
                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);
                api.triggerScheduledTask(customerUsagePoolEvalutionPluginId , new Date());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                user = api.getUserWS(envBuilder.idForCode(USER_01));
                assertEquals(parseDate(nextInvoiceDate.getTime()),parseDate(user.getNextInvoiceDate()));

                Calendar cycleStartdate = Calendar.getInstance();
                cycleStartdate.set(Calendar.YEAR, 2018);
                cycleStartdate.set(Calendar.MONTH, 2);
                cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 2);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

                //asserting start date and end date of customer field usage pool
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                logger.debug("Inital quantity : {}",fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_01), 10, 0))
                 .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_01));
        }
    }

    @Test
    public void test002PostpaidSubscriptionOrderBillingProcessScenarioA2(){

        TestEnvironment environment = testBuilder.getTestEnvironment();
        try {
            testBuilder.given(envBuilder -> {
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 11);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar acticeSince = Calendar.getInstance();
                acticeSince.set(Calendar.YEAR, 2017);
                acticeSince.set(Calendar.MONTH, 11);
                acticeSince.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                //Create User with NID 1st Dec 2017
                buildAndPersistCustomer(envBuilder, api, USER_02, accTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);

                //Create Order with Active since 1st Dec 2017
                buildAndPersistOrder(envBuilder, api, "testSubScriptionOrderO1", envBuilder.idForCode(USER_02), acticeSince.getTime(), null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_POST_PAID,
                        true, productQuantityMap);
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 11);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //fetching user and checking next invoice date
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));
                assertEquals(parseDate(user.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 11);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //generating invoice for invoice date 1 Dec 2017
                api.createInvoiceWithDate(user.getId(),nextInvoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //updating users Next Invoice Date to 1 Jan 2018
                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);
                user = api.getUserWS(envBuilder.idForCode(USER_02));
                assertEquals(parseDate(user.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2017);
                activeSince.set(Calendar.MONTH, 11);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                //Create One Time Order with Active since 1st Dec 2017
                createOneTimeOrder(api,user.getUserId(),activeSince.getTime(),"120");

                Calendar cycleStartdate = Calendar.getInstance();
                cycleStartdate.set(Calendar.YEAR, 2017);
                cycleStartdate.set(Calendar.MONTH, 11);
                cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2017);
                cycleEnddate.set(Calendar.MONTH, 11);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

                //asserting start date and end date of customer field usage pool
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));

                //generating invoice for invoice date 1 Jan 2018
                api.createInvoiceWithDate(envBuilder.idForCode(USER_02),nextInvoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 1);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //updating users Next Invoice Date to 1 Feb 2018
                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);
                api.triggerScheduledTask(customerUsagePoolEvalutionPluginId , new Date());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                user = api.getUserWS(envBuilder.idForCode(USER_02));
                assertEquals(parseDate(nextInvoiceDate.getTime()),parseDate(user.getNextInvoiceDate()));

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                //Create One Time Order with Active since 1st Jan 2018
                createOneTimeOrder(api,user.getUserId(),activeSince.getTime(),"150");

                Calendar cycleStartdate = Calendar.getInstance();
                cycleStartdate.set(Calendar.YEAR, 2018);
                cycleStartdate.set(Calendar.MONTH, 0);
                cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 0);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

                //asserting start date and end date of customer field usage pool
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 1);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));

                //generating invoice for invoice date 1 Feb 2018
                api.createInvoiceWithDate(envBuilder.idForCode(USER_02),nextInvoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 2);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                //updating users Next Invoice Date to 1 March 2018
                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);
                api.triggerScheduledTask(customerUsagePoolEvalutionPluginId , new Date());
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                user = api.getUserWS(envBuilder.idForCode(USER_02));
                assertEquals(parseDate(nextInvoiceDate.getTime()),parseDate(user.getNextInvoiceDate()));

                Calendar cycleStartdate = Calendar.getInstance();
                cycleStartdate.set(Calendar.YEAR, 2018);
                cycleStartdate.set(Calendar.MONTH, 1);
                cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar cycleEnddate = Calendar.getInstance();
                cycleEnddate.set(Calendar.YEAR, 2018);
                cycleEnddate.set(Calendar.MONTH, 1);
                cycleEnddate.set(Calendar.DAY_OF_MONTH, 28);

                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

                //asserting start date and end date of customer field usage pool
                logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                logger.debug("Inital quantity : {}",fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
                assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_02), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_02));
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
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
                .withCyclePeriodValue(1).withName(code)
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

    public Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, JbillingAPI api, String username,
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

    public Integer buildAndPersistOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, Integer userId,
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

    public static Integer createOneTimeOrder(JbillingAPI api,Integer userId, Date activeSinceDate, String inboundProductQuantity){

        logger.debug("Creating One time usage order...");
        OrderWS oTOrder = new OrderWS();
        oTOrder.setUserId(userId);
        oTOrder.setActiveSince(activeSinceDate);
        oTOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        oTOrder.setPeriod(1); // Onetime
        oTOrder.setCurrencyId(1);

        OrderLineWS oTline1 = new OrderLineWS();
        oTline1.setItemId(TestConstants.INBOUND_USAGE_PRODUCT_ID);
        oTline1.setDescription("Inbound");
        oTline1.setQuantity(inboundProductQuantity);
        oTline1.setTypeId(1);
        oTline1.setPrice("0.00");
        oTline1.setAmount("0.00");
        oTline1.setUseItem(true);

        oTOrder.setOrderLines(new OrderLineWS[]{oTline1});
        Integer oneTimeOrderId = api.createOrder(oTOrder, OrderChangeBL.buildFromOrder(oTOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        logger.debug("Created one time usage order with Id: {}", oneTimeOrderId);
        assertNotNull("one time usage order creation failed", oneTimeOrderId);

        return oneTimeOrderId;
    }

    private String parseDate(Date date) {
        if(date == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

}
