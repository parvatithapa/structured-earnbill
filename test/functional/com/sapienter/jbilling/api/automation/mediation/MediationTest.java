package com.sapienter.jbilling.api.automation.mediation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.api.automation.utils.FileHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord.STATUS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord.TYPE;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;
/**
 * @author Vojislav Stanojevikj
 * @since 05-Jul-2016.
 */
@Test(groups = {"api-automation"}, testName = "MediationTest")
public class MediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String MEDIATION_CONFIG_NAME = "Test Mediation 4.0";
    private static final String MEDIATION_CONFIG_NAME_GLOBAL = "Test Global Mediation 4.0";
    private static final String MEDIATION_JOB_LAUNCHER_NAME = "sampleMediationJob";
    private static final String TEST_CATEGORY_CODE = "testCategory";
    private static final String TEST_CATEGORY_CODE_GLOBAL = "testGlobalCategory";
    private static final String TEST_ITEM_CODE = "testItem";
    private static final String TEST_ITEM_CODE_GLOBAL = "testGlobalItem";
    private static final String CUSTOMER_CODE = "MediationTestCustomer";
    private static final String CUSTOMER_PARENT_CODE = "testParentCustomer";
    private static final String CUSTOMER_CHILD_CODE = "testChildCustomer";
    private static final String ACCOUNT_TYPE_CHILD_CODE = "testChildAccountType";
    private static final String CUSTOMER_ERROR_RECORD_CODE = "JB-USER-NOT-RESOLVED";
    private static final String ITEM_ERROR_RECORD_CODE = "ERR-ITEM_NOT-FOUND";
    public static final Integer INBOUND_USAGE_PRODUCT_ID = 320101;
    public static final int CHAT_USAGE_PRODUCT_ID = 320102;
    public static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final String USAGE_POOL_01 = "UP with 100 Quantity"+System.currentTimeMillis();
    private static final String PLAN_01 = "100 free minute Plan";
    private static final String SUBSCRIPTION_PROD_01 = "testPlanSubscriptionItem_01";
    private static final Integer NEXT_INVOICE_DAY = 1;
    private static final String USER_01 = "Test-1-"+System.currentTimeMillis();
    private static final String ORDER_01 = "testSubScriptionOrderO1";
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID= 320104;
    private static final Integer CC_PM_ID = 5;
    private static final String PLAN_02 = "200 free minute Plan";
    private static final String SUBSCRIPTION_PROD_02 = "testPlanSubscriptionItem_02";
    private static final String USAGE_POOL_02 = "UP with 200 Quantity"+System.currentTimeMillis();
    private static final String USAGEPOOL_LOGGER_MSG = "Cycle start date: {} Cycle end date: {}";
    private static final String INBOUND_MEDIATION_LAUNCHER = "inboundCallsMediationJobLauncher";
    private TestBuilder testBuilder;
    private String testCat1 = "MediatedUsageCategory";
    private String testAccount = "Account Type";
    private EnvironmentHelper envHelper;
    private AssetWS scenario01Asset;

    private EnvironmentHelper environmentHelper;
    private TestBuilder mediationConfigBuilder;
    private TestBuilder mediationConfigGlobalBuilder;
    private final Random ID_GEN = new Random(Integer.MAX_VALUE - 1);

    @BeforeClass
    public void init(){
        mediationConfigBuilder = getMediationSetup();
        JbillingAPI parentApi = mediationConfigBuilder.getTestEnvironment().getPrancingPonyApi();
        JbillingAPI childApi = mediationConfigBuilder.getTestEnvironment().getResellerApi();
        environmentHelper = EnvironmentHelper.getInstance(parentApi, childApi);
        mediationConfigGlobalBuilder = getGlobalMediationSetup();
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            logger.debug("In test builder");
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //Create account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
            //Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            //Create usage products
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_01, false, envBuilder.idForCode(testCat1), "99", true);
            ConfigurationBuilder configurationBuilder = envBuilder.configurationBuilder(api);
            addPluginIfAbsent(configurationBuilder, "com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolConsumptionActionTask",
                    api.getCallerCompanyId());
            addPluginIfAbsent(configurationBuilder, "com.sapienter.jbilling.server.user.tasks.EventBasedCustomNotificationTask",
                    api.getCallerCompanyId());
            configurationBuilder.build();
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
                    envBuilder.idForCode(SUBSCRIPTION_PROD_01), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),planItemProd01WS,planItemProd02WS,planItemProd03WS);

            PlanItemWS planItemProd201WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.25", pricingDate.getTime());
            PlanItemWS planItemProd202WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.25", pricingDate.getTime());
            PlanItemWS planItemProd203WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.25", pricingDate.getTime());

            // Create FUP,FLAT PRODUCT AND PLAN with 200 Free Min
            buildAndPersistUsagePool(envBuilder, api, USAGE_POOL_02, "200", envBuilder.idForCode(testCat1), items);
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_02, false, envBuilder.idForCode(testCat1), "199", true);
            buildAndPersistPlan(envBuilder,api, PLAN_02, "200 Free Min Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SUBSCRIPTION_PROD_02), Arrays.asList(envBuilder.idForCode(USAGE_POOL_02)),planItemProd201WS, planItemProd202WS, planItemProd203WS);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_01));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_02));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_01));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_02));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_01));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_02));
        });
    }

    @AfterClass
    public void tearDown(){
        mediationConfigBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        mediationConfigGlobalBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != mediationConfigBuilder){
            mediationConfigBuilder = null;
        }
        if (null != mediationConfigGlobalBuilder){
            mediationConfigGlobalBuilder = null;
        }
        if (null != environmentHelper){
            environmentHelper = null;
        }
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    private TestBuilder getMediationSetup(){
        return TestBuilder.newTest().givenForMultiple(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.mediationConfigBuilder(api)
            .withName(MEDIATION_CONFIG_NAME).withLauncher(MEDIATION_JOB_LAUNCHER_NAME).build();

            envBuilder.itemBuilder(api).item()
            .withType(
                    envBuilder.itemBuilder(api).itemType()
                    .withCode(TEST_CATEGORY_CODE).build()
                    )
                    .withCode(TEST_ITEM_CODE).withFlatPrice("10").build();

            envBuilder.customerBuilder(api).addTimeToUsername(false).withUsername(CUSTOMER_CODE).build();
        });
    }

    private TestBuilder getGlobalMediationSetup(){
        return TestBuilder.newTest().givenForMultiple(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final JbillingAPI childApi = envBuilder.getResellerApi();
            envBuilder.mediationConfigBuilder(api)
            .withName(MEDIATION_CONFIG_NAME_GLOBAL).withLauncher(MEDIATION_JOB_LAUNCHER_NAME).global(true).build();

            envBuilder.itemBuilder(api).item()
            .withType(
                    envBuilder.itemBuilder(api).itemType()
                    .withCode(TEST_CATEGORY_CODE_GLOBAL).global(true).build()
                    )
                    .withCode(TEST_ITEM_CODE_GLOBAL).global(true).withFlatPrice("10").build();

            envBuilder.customerBuilder(childApi).addTimeToUsername(false).withUsername(CUSTOMER_CHILD_CODE)
            .withAccountTypeId(
                    envBuilder.accountTypeBuilder(childApi)
                    .withName(ACCOUNT_TYPE_CHILD_CODE)
                    .withCreditLimit("10000")
                    .withMainSubscription(environmentHelper.getOrderPeriodMonth(childApi), 1)
                    .withEntityId(childApi.getCallerCompanyId())
                    .build().getId()
                    ).build();
            envBuilder.customerBuilder(api).addTimeToUsername(false).withUsername(CUSTOMER_PARENT_CODE).build();
        });
    }

    private TestBuilder addItemToInitialSetup(String itemCode, String flatPrice){
        return mediationConfigBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.itemBuilder(api).item()
            .withType(envBuilder.env().idForCode(TEST_CATEGORY_CODE))
            .withCode(itemCode).withFlatPrice(flatPrice).build();
        });
    }

    @Test
    public void testSingleCustomerMediationRun(){
        final String[] cdr = new String[1];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        mediationConfigBuilder.given(envBuilder -> cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100),
                envBuilder.env().idForCode(TEST_ITEM_CODE), CUSTOMER_CODE, eventDate))
                .test(testEnvironment -> {

                    final JbillingAPI api = testEnvironment.getPrancingPonyApi();
                    final Integer customerId = testEnvironment.idForCode(CUSTOMER_CODE);
                    final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);
                    final Integer itemId = testEnvironment.idForCode(TEST_ITEM_CODE);
                    UUID processId = api.processCDR(configId, Arrays.asList(cdr));
                    validateMediationProcess(api.getMediationProcess(processId), 1, 1, 0, 0, configId, eventDate);

                    OrderWS currentOrder = api.getLatestOrder(customerId);
                    validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId, eventDate,
                            BigDecimal.valueOf(1000), Integer.valueOf(1));

                    OrderLineWS orderLine = currentOrder.getOrderLines()[0];
                    validateOrderLine(orderLine, itemId, BigDecimal.valueOf(100),
                            BigDecimal.valueOf(1000));

                    JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId, Integer.valueOf(0),
                            Integer.valueOf(100), eventDate, new DateTime(eventDate).plusMonths(1).toDate());
                    assertNotNull(records, "records expected!");
                    assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");
                    validateMediationRecord(records[0], STATUS.PROCESSED, eventDate, itemId,
                            customerId, currentOrder.getId(), orderLine.getId());

                    api.undoMediation(processId);
                });
    }

    @Test
    public void testSingleCustomerMediationRunFromFile(){
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final File file = new File(Util.getSysProp("base_dir") + "mediation/api-test.csv");
        mediationConfigBuilder.given(envBuilder ->
        FileHelper.write(file.getPath(), buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100),
                envBuilder.env().idForCode(TEST_ITEM_CODE), CUSTOMER_CODE, eventDate)))
                .test(testEnvironment -> {

                    final JbillingAPI api = testEnvironment.getPrancingPonyApi();
                    final Integer customerId = testEnvironment.idForCode(CUSTOMER_CODE);
                    final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);
                    final Integer itemId = testEnvironment.idForCode(TEST_ITEM_CODE);

                    UUID processId = api.launchMediation(configId, MEDIATION_JOB_LAUNCHER_NAME, file);
                    validateMediationProcess(api.getMediationProcess(processId), 1, 1, 0, 0, configId, eventDate);

                    OrderWS currentOrder = api.getLatestOrder(customerId);
                    validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId, eventDate,
                            BigDecimal.valueOf(1000), Integer.valueOf(1));

                    OrderLineWS orderLine = currentOrder.getOrderLines()[0];
                    validateOrderLine(orderLine, itemId, BigDecimal.valueOf(100),
                            BigDecimal.valueOf(1000));

                    JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId, Integer.valueOf(0),
                            Integer.valueOf(100), eventDate, new DateTime(eventDate).plusMonths(1).toDate());
                    assertNotNull(records, "records expected!");
                    assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");
                    validateMediationRecord(records[0], STATUS.PROCESSED, eventDate, itemId,
                            customerId, currentOrder.getId(), orderLine.getId());

                    api.undoMediation(processId);
                    FileHelper.deleteFile(file.getPath());
                });
    }

    @Test
    public void testMultipleCustomersMediationRun(){
        final String userName2 = "secondCustomer";
        final String[] cdr = new String[2];
        final Date eventDate = new DateTime(2016, 1, 31, 0, 0).toDate();
        final Date eventDate2 = new DateTime(2016, 2, 14, 0, 0).toDate();
        mediationConfigBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.customerBuilder(api).addTimeToUsername(false).withUsername(userName2).build();
            final Integer itemId = envBuilder.env().idForCode(TEST_ITEM_CODE);
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate);
            cdr[1] = buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(150), itemId, userName2, eventDate2);
        }).test(testEnvironment -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer customerId = testEnvironment.idForCode(CUSTOMER_CODE);
            final Integer customerId2 = testEnvironment.idForCode(userName2);
            final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);
            final Integer itemId = testEnvironment.idForCode(TEST_ITEM_CODE);
            UUID processId = api.processCDR(configId, Arrays.asList(cdr));
            validateMediationProcess(api.getMediationProcess(processId), 2, 2, 0, 0, configId, Util.truncateDate(new Date()));

            OrderWS currentOrder = api.getLatestOrder(customerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId, eventDate,
                    BigDecimal.valueOf(1000), Integer.valueOf(1));

            OrderLineWS orderLine = currentOrder.getOrderLines()[0];
            validateOrderLine(orderLine, itemId, BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            OrderWS currentOrder2 = api.getLatestOrder(customerId2);
            validateCurrentOrder(currentOrder2, environmentHelper.getOrderPeriodOneTime(api), customerId2, eventDate2,
                    BigDecimal.valueOf(1500), Integer.valueOf(1));

            OrderLineWS orderLine2 = currentOrder2.getOrderLines()[0];
            validateOrderLine(orderLine2, itemId, BigDecimal.valueOf(150),
                    BigDecimal.valueOf(1500));

            JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId, Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, new DateTime(eventDate).plusMonths(1).toDate());
            assertNotNull(records, "records expected!");
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(2), "Invalid number of records!");
            Arrays.sort(records, (o1, o2) -> o1.getEventDate().compareTo(o2.getEventDate()));
            validateMediationRecord(records[0], STATUS.PROCESSED, eventDate, itemId, customerId,
                    currentOrder.getId(), orderLine.getId());
            validateMediationRecord(records[1], STATUS.PROCESSED, eventDate2, itemId, customerId2,
                    currentOrder2.getId(), orderLine2.getId());

            api.undoMediation(processId);
        });
    }

    @Test
    public void testSingleCustomerDuplicatesAndErrorsMediationRun(){

        final String[] cdr = new String[4];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        mediationConfigBuilder.given(envBuilder -> {
            final Integer itemId = envBuilder.env().idForCode(TEST_ITEM_CODE);
            Integer id = ID_GEN.nextInt();
            cdr[0] = buildCDR(id, "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate);
            cdr[1] = buildCDR(id, "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate);
            cdr[2] = buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(100), itemId, "No Man's land", eventDate);
            cdr[3] = buildCDR(ID_GEN.nextInt(), "55555", "66666", Integer.valueOf(100), Integer.MAX_VALUE, CUSTOMER_CODE, eventDate);
        }).test(testEnvironment -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);
            UUID processId = api.processCDR(configId, Arrays.asList(cdr));
            validateMediationProcess(api.getMediationProcess(processId), 4, 1, 2, 1, configId, eventDate);
            JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(processId, null);
            assertNotNull(errorRecords, "Error records expected!");
            assertEquals(Integer.valueOf(errorRecords.length), Integer.valueOf(2));
            validateErrorRecord(errorRecords, configId, CUSTOMER_ERROR_RECORD_CODE, ITEM_ERROR_RECORD_CODE);
            api.undoMediation(processId);
        });
    }

    @Test
    public void testUndoSingleMediationRun(){

        final String[] cdr = new String[1];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final UUID[] processId = new UUID[1];
        mediationConfigBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final Integer itemId = envBuilder.env().idForCode(TEST_ITEM_CODE);
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate);
            processId[0] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr));
        }).test(testEnvironment -> {
            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            api.undoMediation(processId[0]);

            MediationProcess mediationProcess = api.getMediationProcess(processId[0]);
            assertNull(mediationProcess, "Mediation process not expected!");

            OrderWS currentOrder = api.getLatestOrder(testEnvironment.idForCode(CUSTOMER_CODE));
            assertNull(currentOrder, "No orders expected!");

            JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0), Integer.valueOf(100),
                    eventDate, null);

            assertNotNull(records, "Empty array expected!");
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(0), "Invalid number of records!");
        });
    }

    @Test
    public void testUndoSpecificMediationRun(){

        final String testItem2 = "testItem2";
        final String[] cdr = new String[2];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final UUID[] processId = new UUID[2];
        addItemToInitialSetup(testItem2, "5").given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100),
                    envBuilder.env().idForCode(TEST_ITEM_CODE), CUSTOMER_CODE, eventDate);
            cdr[1] = buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(20), envBuilder.env().idForCode(testItem2),
                    CUSTOMER_CODE, eventDate);
            processId[0] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr[0]));
            processId[1] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr[1]));
        }).test(testEnvironment -> {
            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer customerId = testEnvironment.idForCode(CUSTOMER_CODE);
            OrderWS currentOrder = api.getLatestOrder(customerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId,
                    eventDate, BigDecimal.valueOf(1100), Integer.valueOf(2));
            OrderLineWS[] orderLines = currentOrder.getOrderLines();
            Arrays.sort(orderLines, (o1, o2) -> o1.getAmountAsDecimal().compareTo(o2.getAmountAsDecimal()));
            validateOrderLine(orderLines[0], testEnvironment.idForCode(testItem2), BigDecimal.valueOf(20),
                    BigDecimal.valueOf(100));
            validateOrderLine(orderLines[1], testEnvironment.idForCode(TEST_ITEM_CODE), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            records = api.getMediationRecordsByMediationProcess(processId[1], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            api.undoMediation(processId[1]);

            currentOrder = api.getLatestOrder(customerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId,
                    eventDate, BigDecimal.valueOf(1000), Integer.valueOf(1));
            orderLines = currentOrder.getOrderLines();
            validateOrderLine(orderLines[0], testEnvironment.idForCode(TEST_ITEM_CODE), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            records = api.getMediationRecordsByMediationProcess(processId[1], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(0), "Invalid number of records!");

            api.undoMediation(processId[0]);
        });
    }

    @Test
    public void testUndoRedoMediationRun(){

        final String testItem2 = "testItem2";
        final String[] cdr = new String[2];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final UUID[] processId = new UUID[2];
        addItemToInitialSetup(testItem2, "5").given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100),
                    envBuilder.env().idForCode(TEST_ITEM_CODE), CUSTOMER_CODE, eventDate);
            cdr[1] = buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(20), envBuilder.env().idForCode(testItem2),
                    CUSTOMER_CODE, eventDate);
            processId[0] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr[0]));
            processId[1] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr[1]));
        }).test(testEnvironment -> {
            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer customerId = testEnvironment.idForCode(CUSTOMER_CODE);
            OrderWS currentOrder = api.getLatestOrder(customerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId,
                    eventDate, BigDecimal.valueOf(1100), Integer.valueOf(2));
            OrderLineWS[] orderLines = currentOrder.getOrderLines();
            Arrays.sort(orderLines, (o1, o2) -> o1.getAmountAsDecimal().compareTo(o2.getAmountAsDecimal()));
            validateOrderLine(orderLines[0], testEnvironment.idForCode(testItem2), BigDecimal.valueOf(20),
                    BigDecimal.valueOf(100));
            validateOrderLine(orderLines[1], testEnvironment.idForCode(TEST_ITEM_CODE), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            records = api.getMediationRecordsByMediationProcess(processId[1], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            api.undoMediation(processId[1]);

            currentOrder = api.getLatestOrder(customerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId,
                    eventDate, BigDecimal.valueOf(1000), Integer.valueOf(1));
            orderLines = currentOrder.getOrderLines();
            validateOrderLine(orderLines[0], testEnvironment.idForCode(TEST_ITEM_CODE), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            records = api.getMediationRecordsByMediationProcess(processId[1], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(0), "Invalid number of records!");

            processId[1] = api.processCDR(testEnvironment.idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr[1]));

            currentOrder = api.getLatestOrder(customerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(api), customerId,
                    eventDate, BigDecimal.valueOf(1100), Integer.valueOf(2));
            orderLines = currentOrder.getOrderLines();
            Arrays.sort(orderLines, (o1, o2) -> o1.getAmountAsDecimal().compareTo(o2.getAmountAsDecimal()));
            validateOrderLine(orderLines[0], testEnvironment.idForCode(testItem2), BigDecimal.valueOf(20),
                    BigDecimal.valueOf(100));
            validateOrderLine(orderLines[1], testEnvironment.idForCode(TEST_ITEM_CODE), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            records = api.getMediationRecordsByMediationProcess(processId[1], Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, null);
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(1), "Invalid number of records!");

            api.undoMediation(processId[0]);
            api.undoMediation(processId[1]);
        });
    }

    @Test
    public void testRecycleCustomerError() {

        final String recycleCustomer = "recycleCustomer1";
        final String[] cdr = new String[1];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final UUID[] processId = new UUID[2];
        mediationConfigBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100),
                    envBuilder.env().idForCode(TEST_ITEM_CODE), recycleCustomer, eventDate);
            processId[0] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr));
        }).test((testEnvironment, envBuilder) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);

            validateMediationProcess(api.getMediationProcess(processId[0]), 1, 0, 1, 0, configId, eventDate);

            JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(processId[0], null);
            assertEquals(Integer.valueOf(errorRecords.length), Integer.valueOf(1), "Invalid error records!");
            validateErrorRecord(errorRecords, configId, CUSTOMER_ERROR_RECORD_CODE);

            envBuilder.customerBuilder(api).addTimeToUsername(false).withUsername(recycleCustomer).build();

            processId[1] = api.runRecycleForProcess(processId[0]);

            waitForMediationComplete(api, 70 * 70 * 100);

            validateMediationProcess(api.getMediationProcess(processId[1]), 1, 1, 0, 0, configId, eventDate);

            api.undoMediation(processId[1]);
        });
    }

    @Test
    public void testRecycleItemError() {

        final String[] cdr = new String[1];
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final UUID[] processId = new UUID[2];
        mediationConfigBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer dummyItemId = envBuilder.itemBuilder(api).item().withType(envBuilder.env().idForCode(TEST_CATEGORY_CODE))
                    .withCode("dummy").withFlatPrice("0").build();
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100), dummyItemId + 1, CUSTOMER_CODE, eventDate);
            processId[0] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME), Arrays.asList(cdr));
        }).test((testEnvironment, envBuilder) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);

            validateMediationProcess(api.getMediationProcess(processId[0]), 1, 0, 1, 0, configId, eventDate);

            JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(processId[0], null);
            assertEquals(Integer.valueOf(errorRecords.length), Integer.valueOf(1), "Invalid error records!");
            validateErrorRecord(errorRecords, configId, ITEM_ERROR_RECORD_CODE);

            envBuilder.itemBuilder(api).item().withType(testEnvironment.idForCode(TEST_CATEGORY_CODE)).withCode("testItem2")
            .withFlatPrice("10").build();

            processId[1] = api.runRecycleForProcess(processId[0]);

            waitForMediationComplete(api, 70 * 70 * 100);

            validateMediationProcess(api.getMediationProcess(processId[1]), 1, 1, 0, 0, configId, eventDate);

            api.undoMediation(processId[1]);
        });
    }

    @Test
    public void testGlobalMediationForGlobalProductTwoCustomers(){
        final String[] cdr = new String[2];
        final Date eventDate = new DateTime(2016, 1, 31, 0, 0).toDate();
        final Date eventDate2 = new DateTime(2016, 2, 14, 0, 0).toDate();
        mediationConfigGlobalBuilder.given(envBuilder -> {
            final Integer itemId = envBuilder.env().idForCode(TEST_ITEM_CODE_GLOBAL);
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CHILD_CODE, eventDate);
            cdr[1] = buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(150), itemId, CUSTOMER_PARENT_CODE, eventDate2);
        }).test(testEnvironment -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final JbillingAPI childApi = testEnvironment.getResellerApi();
            final Integer childCustomerId = testEnvironment.idForCode(CUSTOMER_CHILD_CODE);
            final Integer parentCustomerId = testEnvironment.idForCode(CUSTOMER_PARENT_CODE);
            final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME_GLOBAL);
            final Integer itemId = testEnvironment.idForCode(TEST_ITEM_CODE_GLOBAL);
            UUID processId = api.processCDR(configId, Arrays.asList(cdr));
            validateMediationProcess(api.getMediationProcess(processId), 2, 2, 0, 0, configId, Util.truncateDate(new Date()));

            OrderWS currentOrder = childApi.getLatestOrder(childCustomerId);
            validateCurrentOrder(currentOrder, environmentHelper.getOrderPeriodOneTime(childApi), childCustomerId, eventDate,
                    BigDecimal.valueOf(1000), Integer.valueOf(1));

            OrderLineWS orderLine = currentOrder.getOrderLines()[0];
            validateOrderLine(orderLine, itemId, BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000));

            OrderWS currentOrder2 = api.getLatestOrder(parentCustomerId);
            validateCurrentOrder(currentOrder2, environmentHelper.getOrderPeriodOneTime(api), parentCustomerId, eventDate2,
                    BigDecimal.valueOf(1500), Integer.valueOf(1));

            OrderLineWS orderLine2 = currentOrder2.getOrderLines()[0];
            validateOrderLine(orderLine2, itemId, BigDecimal.valueOf(150),
                    BigDecimal.valueOf(1500));

            JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId, Integer.valueOf(0),
                    Integer.valueOf(100), eventDate, new DateTime(eventDate).plusMonths(1).toDate());
            assertNotNull(records, "records expected!");
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(2), "Invalid number of records!");
            Arrays.sort(records, (o1, o2) -> o1.getEventDate().compareTo(o2.getEventDate()));
            validateMediationRecord(records[0], STATUS.PROCESSED, eventDate, itemId, childCustomerId,
                    currentOrder.getId(), orderLine.getId());
            validateMediationRecord(records[1], STATUS.PROCESSED, eventDate2, itemId, parentCustomerId,
                    currentOrder2.getId(), orderLine2.getId());

            api.undoMediation(processId);
        });
    }

    @Test
    public void testGlobalMediationForGlobalProductUndo(){
        final String[] cdr = new String[2];
        final UUID[] processId = new UUID[1];
        final Date eventDate = new DateTime(2016, 1, 31, 0, 0).toDate();
        final Date eventDate2 = new DateTime(2016, 2, 14, 0, 0).toDate();
        mediationConfigGlobalBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final Integer itemId = envBuilder.env().idForCode(TEST_ITEM_CODE_GLOBAL);
            cdr[0] = buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CHILD_CODE, eventDate);
            cdr[1] = buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(150), itemId, CUSTOMER_PARENT_CODE, eventDate2);
            processId[0] = api.processCDR(envBuilder.env().idForCode(MEDIATION_CONFIG_NAME_GLOBAL), Arrays.asList(cdr));
        }).test(testEnvironment -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final JbillingAPI childApi = testEnvironment.getResellerApi();
            api.undoMediation(processId[0]);

            MediationProcess mediationProcess = api.getMediationProcess(processId[0]);
            assertNull(mediationProcess, "Mediation process not expected!");
            mediationProcess = childApi.getMediationProcess(processId[0]);
            assertNull(mediationProcess, "Mediation process not expected!");

            OrderWS currentOrder = api.getLatestOrder(testEnvironment.idForCode(CUSTOMER_PARENT_CODE));
            assertNull(currentOrder, "No orders expected!");
            currentOrder = childApi.getLatestOrder(testEnvironment.idForCode(CUSTOMER_CHILD_CODE));
            assertNull(currentOrder, "No orders expected!");

            JbillingMediationRecord[] records = api.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0), Integer.valueOf(100),
                    eventDate, null);

            assertNotNull(records, "Empty array expected!");
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(0), "Invalid number of records!");

            records = childApi.getMediationRecordsByMediationProcess(processId[0], Integer.valueOf(0), Integer.valueOf(100),
                    eventDate2, null);

            assertNotNull(records, "Empty array expected!");
            assertEquals(Integer.valueOf(records.length), Integer.valueOf(0), "Invalid number of records!");

        });
    }

    @Test
    public void testMediationInActionFromFile(){
        final Date eventDate = new DateTime().withTimeAtStartOfDay().toDate();
        final File file = new File(Util.getSysProp("base_dir") + "mediation/api-test.csv");
        final String recycleCustomer = "recycleCustomer";
        final UUID[] processId = new UUID[2];
        final Integer[] nextItemId = {null};
        mediationConfigBuilder.given(envBuilder ->{
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final Integer itemId = envBuilder.env().idForCode(TEST_ITEM_CODE);
            final Integer configId = envBuilder.env().idForCode(MEDIATION_CONFIG_NAME);
            nextItemId[0] = envBuilder.itemBuilder(api).item().withType(envBuilder.env().idForCode(TEST_CATEGORY_CODE))
                    .withCode("dummy").withFlatPrice("0").build() + 1;
            Integer id = ID_GEN.nextInt();
            // Duplicates
            FileHelper.write(file.getPath(),
                    buildCDR(id, "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate),
                    buildCDR(id, "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate),
                    buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(100), itemId, recycleCustomer, eventDate),
                    buildCDR(ID_GEN.nextInt(), "55555", "66666", Integer.valueOf(100), nextItemId[0], CUSTOMER_CODE, eventDate));
            processId[0] = api.launchMediation(configId, MEDIATION_JOB_LAUNCHER_NAME, file);
        }).test((testEnvironment, envBuilder) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            final Integer configId = testEnvironment.idForCode(MEDIATION_CONFIG_NAME);
            validateMediationProcess(api.getMediationProcess(processId[0]), 4, 1, 2, 1, configId, eventDate);
            JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(processId[0], null);
            assertNotNull(errorRecords, "Error records expected!");
            assertEquals(Integer.valueOf(errorRecords.length), Integer.valueOf(2));
            validateErrorRecord(errorRecords, configId, CUSTOMER_ERROR_RECORD_CODE, ITEM_ERROR_RECORD_CODE);

            api.undoMediation(processId[0]);

            final Integer itemId = testEnvironment.idForCode(TEST_ITEM_CODE);
            // Fix the duplicates
            FileHelper.deleteFile(file.getPath());

            FileHelper.write(file.getPath(),
                    buildCDR(ID_GEN.nextInt(), "11111", "22222", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate),
                    buildCDR(ID_GEN.nextInt(), "22222", "33333", Integer.valueOf(100), itemId, CUSTOMER_CODE, eventDate),
                    buildCDR(ID_GEN.nextInt(), "33333", "44444", Integer.valueOf(100), itemId, recycleCustomer, eventDate),
                    buildCDR(ID_GEN.nextInt(), "55555", "66666", Integer.valueOf(100), nextItemId[0], CUSTOMER_CODE, eventDate));

            processId[0] = api.launchMediation(configId, MEDIATION_JOB_LAUNCHER_NAME, file);
            validateMediationProcess(api.getMediationProcess(processId[0]), 4, 2, 2, 0, configId, eventDate);

            // Fix errors
            envBuilder.customerBuilder(api).addTimeToUsername(false).withUsername(recycleCustomer).build();
            envBuilder.itemBuilder(api).item().withType(testEnvironment.idForCode(TEST_CATEGORY_CODE)).withFlatPrice("1").withCode("recycleItem").build();

            processId[1] = api.runRecycleForProcess(processId[0]);

            waitForMediationComplete(api, 70 * 70 * 100);

            validateMediationProcess(api.getMediationProcess(processId[1]), 2, 2, 0, 0, configId, eventDate);

            api.undoMediation(processId[0]);
            api.undoMediation(processId[1]);
            FileHelper.deleteFile(file.getPath());
        });
    }

    @Test
    public void testMediationForOverageRate() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final String emailTestNotificationFile = "./resources/emails_sent.txt";
        try {
            testBuilder.given(envBuilder ->{
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Integer asset01 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
                scenario01Asset = api.getAsset(asset01);
                logger.debug("asset Identifier {} fetched for product {}", scenario01Asset.getIdentifier(), TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(USER_01,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(ORDER_01, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_PRE_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);
                Integer orderId = environment.idForCode(ORDER_01);
                if(!checkAssetOnOrder(api, orderId, new Integer[] { asset01 })) {
                    logger.debug("asset {} not found on order {}", scenario01Asset, orderId);
                }

                FileHelper.deleteFile(emailTestNotificationFile);

            }).test((testEnvironment, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));
                CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(2),new BigDecimal("100.00"));
                OrderWS order = api.getOrder(environment.idForCode(ORDER_01));

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
                List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "300", "01/01/2018");
                UUID mediationProcessId = triggerMediationAndLogErrorRecords(api, INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
                logger.debug("testMediationForOverageRate mediation processid {}", mediationProcessId);
                File file = new File(emailTestNotificationFile);
                assertTrue(file.getAbsoluteFile().exists(),"Notification failed");
            });
        } finally {
            File file = new File(emailTestNotificationFile);
            if (!file.getAbsoluteFile().exists()) {
                try {
                    boolean t = file.createNewFile();
                    logger.debug("file created {}", t);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_01));
        }
    }

    private UUID triggerMediationAndLogErrorRecords(JbillingAPI api, String jobConfigName, List<String> cdr) {
        UUID mediationProcessId = api.processCDR(getMediationConfiguration(api, jobConfigName), cdr);
        JbillingMediationErrorRecord[] errors = api.getErrorsByMediationProcess(mediationProcessId.toString(), 0, 10000);
        MediationProcess mediationProcess = api.getMediationProcess(mediationProcessId);
        if(null == mediationProcess.getEndDate()) {
            logger.debug("mediation process {} is still running", mediationProcessId);
        }
        if(ArrayUtils.isNotEmpty(errors)) {
            for(JbillingMediationErrorRecord  error : errors) {
                logger.debug("error cause {}", error.getErrorCodes());
            }
        } else {
            logger.debug("no error record found for process id {}", mediationProcessId);
        }
        return mediationProcessId;
    }

    private Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {

        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                return mediationConfigurationWS.getId();
            }
        }
        return null;
    }

    private String buildCDR(Integer id, String aNumber, String bNumber, Integer duration,
            Integer itemId, String username, Date eventDate) {
        return StringUtils.join(Arrays.asList(id, aNumber, bNumber,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eventDate), duration, itemId, username), ",");
    }

    private void validateMediationProcess(MediationProcess mediationProcess, int recordsProcessed,
            int doneAndBillable, int errors, int duplicates,
            Integer configId, Date eventDate){

        assertNotNull(mediationProcess, "Mediation process expected!");
        assertEquals(mediationProcess.getRecordsProcessed(), Integer.valueOf(recordsProcessed), "Invalid number of processed records!");
        assertEquals(mediationProcess.getDoneAndBillable(), Integer.valueOf(doneAndBillable), "Invalid number of done and billable records!");
        assertEquals(mediationProcess.getErrors(), Integer.valueOf(errors), "Invalid number of error records!");
        assertEquals(mediationProcess.getDuplicates(), Integer.valueOf(duplicates), "Invalid number of error records!");
        assertEquals(Util.truncateDate(mediationProcess.getStartDate()), eventDate, "Invalid event date!");
        assertEquals(mediationProcess.getConfigurationId(), configId, "Invalid config id!");
    }

    private void validateMediationRecord(JbillingMediationRecord record, STATUS status,
            Date eventDate, Integer itemId, Integer userId,
            Integer orderId, Integer orderLineId){

        assertNotNull(record, "Record can not be null!");
        assertEquals(record.getStatus(), status, "Invalid status!!");
        assertEquals(record.getType(), TYPE.MEDIATION, "Invalid type!!");
        assertEquals(Util.truncateDate(record.getEventDate()), eventDate, "Invalid event date!!");
        assertEquals(record.getItemId(), itemId, "Invalid item id!!");
        assertEquals(record.getUserId(), userId, "Invalid user id!!");
        assertEquals(record.getOrderId(), orderId, "Invalid order id!!");
        assertEquals(record.getOrderLineId(), orderLineId, "Invalid order line id!!");
    }

    private void validateErrorRecord(JbillingMediationErrorRecord[] errorRecords, Integer mediationConfigId,
            String... expectedErrorCodes){
        StringBuilder errorCodes = new StringBuilder();
        for (JbillingMediationErrorRecord errorRecord : errorRecords) {
            assertNotNull(errorRecord, "Error record expected!");
            assertEquals(errorRecord.getMediationCfgId(), mediationConfigId, "Invalid config id!");
            errorCodes.append(errorRecord.getErrorCodes());
        }
        for (String expectederrorCode : expectedErrorCodes) {
            assertTrue(errorCodes.toString().contains(expectederrorCode), "Invalid error code!");
        }
    }

    private void validateCurrentOrder(OrderWS order, Integer periodId, Integer userId,
            Date eventDate, BigDecimal total, Integer orderLinesCount){

        assertNotNull(order, "Order can not be null!");
        assertTrue(order.getNotes().contains("Current order created by mediation process. Do not edit manually."));
        assertEquals(order.getPeriod(), periodId, "Invalid Period!");
        assertEquals(order.getUserId(), userId, "Invalid customer id!!");
        assertEquals(order.getActiveSince(), new DateTime(eventDate).withDayOfMonth(1).toDate(), "Invalid active since!!");
        assertEquals(order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                total.setScale(2, BigDecimal.ROUND_CEILING), "Invalid total!!");

        if (null != orderLinesCount){
            OrderLineWS[] orderLines = order.getOrderLines();
            assertNotNull(orderLines, "Order lines expected!");
            assertEquals(Integer.valueOf(orderLines.length), orderLinesCount, "Invalid number of order lines!");
        }
    }

    private void validateOrderLine(OrderLineWS orderLine, Integer itemId, BigDecimal quantity,
            BigDecimal total){

        assertNotNull(orderLine, "Order line expected!");
        assertEquals(orderLine.getItemId(), itemId, "Invalid item id!");
        assertEquals(orderLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                quantity.setScale(2, BigDecimal.ROUND_CEILING), "Invalid quantity!");
        assertEquals(orderLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                total.setScale(2, BigDecimal.ROUND_CEILING), "Invalid total amount!");

    }

    private void waitForMediationComplete(JbillingAPI api, Integer maxTime) {
        Long start = new Date().getTime();
        while (api.isMediationProcessRunning() && new Date().getTime() < maxTime + start) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        if (new Date().getTime() > maxTime + start) {
            fail("Max time for mediation completion is exceeded");
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
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
                .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                        String.valueOf(envHelper.getUsagePoolNotificationId(api)), "100", null))
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

    public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer itemId, String phoneNumber) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        ItemDTOEx item = api.getItem(itemId, null, null);
        ItemTypeWS itemTypeWS = api.getItemCategoryById(item.getTypes()[0]);
        Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                .equals("Available")).collect(Collectors.toList()).get(0).getId();
        return envBuilder.assetBuilder(api)
                .withItemId(itemId)
                .withAssetStatusId(assetStatusId)
                .global(true)
                .withIdentifier(phoneNumber)
                .withCode(phoneNumber)
                .build();
    }

    private boolean checkAssetOnOrder(JbillingAPI api, Integer orderId, Integer[] assets) {
        OrderWS order = api.getOrder(orderId);
        for(OrderLineWS orderLineWS : order.getOrderLines()) {
            Integer[] lineAssets = orderLineWS.getAssetIds();
            for(Integer assetId : assets) {
                if(ArrayUtils.contains(lineAssets, assetId)) {
                    AssetWS asset = api.getAsset(assetId);
                    logger.debug("asset id {} with identifier {} found on order {}", assetId, asset.getIdentifier(), orderId);
                    return true;
                }
            }
        }
        return false;
    }

    public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    private String parseDate(Date date) {
        if(date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    private List<String> buildInboundCDR(List<String> indentifiers, String quantity, String eventDate) {
        List<String> cdrs = new ArrayList<>();
        indentifiers.forEach(asset ->
        cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()+",6165042651,tressie.johnson,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null")
                );
        return cdrs;
    }

    private UsagePoolConsumptionActionWS buildConsumptionAction(String type, NotificationMediumType mediumType,
            String notificationId, String percentage,
            String productId){

        return UsagePoolBuilder.consumptionActionBuilder()
                .withType(type)
                .withMediumType(mediumType)
                .withNotificationId(notificationId)
                .withPercentage(percentage)
                .withProductId(productId)
                .build();
    }

    private void addPluginIfAbsent(ConfigurationBuilder configurationBuilder, String pluginClassName, Integer entityId){

        if (!configurationBuilder.pluginExists(pluginClassName, entityId)){
            configurationBuilder.addPlugin(pluginClassName);
        }
    }

}
