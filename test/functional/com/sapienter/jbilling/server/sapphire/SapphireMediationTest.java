package com.sapienter.jbilling.server.sapphire;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@Test(groups = { "sapphire" }, testName = "sapphire")
@ContextConfiguration(classes = SapphireTestConfig.class)
public class SapphireMediationTest extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CDR_BASE_DIRECTORY = Util.getSysProp("base_dir") + "/sapphire-mediation-test/cdr";

    private TestBuilder       testBuilder;
    private EnvironmentHelper envHelper;
    private static final Integer CC_PM_ID                                      = 5;
    private static final String  ACCOUNT_NAME                                  = "Sapphire Test Account";
    private static final String  MEDIATED_USAGE_CATEGORY                       = "Sapphire Mediation Usage Category";
    private static final String  INCOMING_CALL_ITEM                            = "Incoming Call";
    private static final String  OUTGOING_CALL_ITEM                            = "OutGoing Call";
    private static final String  ON_NET_CALL_ITEM                              = "On Net Call";
    private static final String  FORWARDED_CALL_ITEM                           = "ForWarded Call";
    private static final String  FORWARDED_CALL_ITEM_NEW                       = "ForWarded Call New";
    private static final String  SAPPHIRE_MEDIATION_CONFIG_NAME                = "Sapphire Mediation";
    private static final String  SAPPHIRE_MEDIATION_JOB_NAME                   = "sapphireMediationJob";
    private static final int     MONTHLY_ORDER_PERIOD                          =  2;
    private static final int     NEXT_INVOICE_DAY                              =  1;
    private static final String  CARRIER_USER_1                                = "Carrier-User-1-"+ UUID.randomUUID().toString();
    private static final String  CARRIER_USER_2                                = "Carrier-User-2-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_1                                   = "Test-User-1-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "Test-User-2-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_3                                   = "Test-User-3-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_4                                   = "Test-User-4-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_041                                 = "Test-User-041-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_042                                 = "Test-User-042-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_043                                 = "Test-User-043-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_044                                 = "Test-User-044-"+ UUID.randomUUID().toString();
    private static final String  ACCOUNT_TYPE_ITEM_MAP_TABLE_NAME              = "account_type_item_map";
    private static final String  CARRIER_MAP_TABLE_NAME                        = "carrier_map";
    private static final String  CDR_FILE_NAME                                 = "sapphire-cdr.xml";
    private static final String  RATE_CARD_CDR_FILE_NAME                       = "sapphire_rout-rate-card-cdr.xml";
    private static final String  SKIP_CDR_FILE_NAME                            = "sapphire-skip-cdr.xml";
    private static final String  RECYCLE_CDR_FILE_NAME                         = "sapphire-recycle-cdr.xml";
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID         =  320104;
    private static final int     NUMBER_ASSET_CATEGORY_ID                      =  230304;
    private static final String  SATELLITE_COUNTRY_CODE_DATA_TABLE             = "satelltite_table";
    private static final Map<String, String> ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS;
    private static final Map<String, String> CARRIER_MAP_TABLE_DETAILS;
    private static final Map<String, String> SATELLITE_COUNTRY_CODE_TABLE_DETAILS;

    private static PriceModelWS callRateCardPrice;
    private JbillingAPI api;

    static {
        ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS = new LinkedHashMap<>();
        ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS.put("id", "SERIAL NOT NUll");
        ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS.put("account_type_id", "VARCHAR(255)");
        ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS.put("cdr_type", "VARCHAR(255)");
        ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS.put("item_id", "VARCHAR(255)");
        ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS.put("PRIMARY KEY", " ( id ) ");

        CARRIER_MAP_TABLE_DETAILS = new LinkedHashMap<>();
        CARRIER_MAP_TABLE_DETAILS.put("id", "SERIAL NOT NUll");
        CARRIER_MAP_TABLE_DETAILS.put("jbilling_user_id", "VARCHAR(255)");
        CARRIER_MAP_TABLE_DETAILS.put("trunk_group_id", "VARCHAR(255)");
        CARRIER_MAP_TABLE_DETAILS.put("PRIMARY KEY", " ( id ) ");

        SATELLITE_COUNTRY_CODE_TABLE_DETAILS = new LinkedHashMap<>();
        SATELLITE_COUNTRY_CODE_TABLE_DETAILS.put("id", "SERIAL NOT NUll");
        SATELLITE_COUNTRY_CODE_TABLE_DETAILS.put("country_code", "VARCHAR(255)");
        SATELLITE_COUNTRY_CODE_TABLE_DETAILS.put("location", "VARCHAR(255)");
        SATELLITE_COUNTRY_CODE_TABLE_DETAILS.put("PRIMARY KEY", " ( id ) ");

    }

    @Resource(name = "sapphireJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
        });
    }

    @BeforeClass
    public void beforeClass() {
        logger.info("Creating Table {} with columns {}", ACCOUNT_TYPE_ITEM_MAP_TABLE_NAME, ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS);
        createTable(ACCOUNT_TYPE_ITEM_MAP_TABLE_NAME, ACCOUNT_TYPE_ITEM_MAP_TABLE_DETAILS);

        logger.info("Creating Table {} with columns {}", CARRIER_MAP_TABLE_NAME, CARRIER_MAP_TABLE_DETAILS);
        createTable(CARRIER_MAP_TABLE_NAME, CARRIER_MAP_TABLE_DETAILS);

        logger.info("Creating Table {} with columns {}", SATELLITE_COUNTRY_CODE_DATA_TABLE, SATELLITE_COUNTRY_CODE_TABLE_DETAILS);
        createTable(SATELLITE_COUNTRY_CODE_DATA_TABLE, SATELLITE_COUNTRY_CODE_TABLE_DETAILS);

        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            // Creating account type
            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, MEDIATED_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            // Creating Mediated Products
            buildAndPersistProduct(envBuilder, api, INCOMING_CALL_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), true, "0.00");

            buildAndPersistProduct(envBuilder, api, OUTGOING_CALL_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), true, "2.50");

            buildAndPersistProduct(envBuilder, api, ON_NET_CALL_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), true, "0.00");

            buildAndPersistProduct(envBuilder, api, FORWARDED_CALL_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), true, "2.50");

            callRateCardPrice = buildRateCardPriceModel();

            // Creating Company Level MetaField
            buildAndPersistMetafield(testBuilder, SapphireMediationConstants.CARRIER_TABLE_FIELD_NAME, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, SapphireMediationConstants.ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, SapphireMediationConstants.PEAK_FIELD_NAME, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, SapphireMediationConstants.SATELLITE_COUNTRY_CODE_DATA_TABLE_NAME, DataType.STRING, EntityType.COMPANY);

            // Setting Company Level Meta Fields
            setCompanyLevelMetaField(testBuilder.getTestEnvironment());

            // Creating Sapphire Job Launcher
            buildAndPersistMediationConfiguration(envBuilder, api, SAPPHIRE_MEDIATION_CONFIG_NAME, SAPPHIRE_MEDIATION_JOB_NAME);

            // Set up account_type_item_map table
            setUpAccountTypeItemMapTable(testBuilder.getTestEnvironment());

            // Set up carrier_map table
            setUpCarrierTable(envBuilder);

            //Set up SatelliteTable
            setUpSatelliteTable();


        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
            assertNotNull("Carrier User 1 Creation Failed", testEnvBuilder.idForCode(CARRIER_USER_1));
            assertNotNull("Carrier User 2 Creation Failed", testEnvBuilder.idForCode(CARRIER_USER_2));
            assertNotNull("Mediated Categroy Creation Failed ", testEnvBuilder.idForCode(MEDIATED_USAGE_CATEGORY));
            assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(SapphireMediationConstants.CARRIER_TABLE_FIELD_NAME));
            assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(SapphireMediationConstants.ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME));
            assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(SapphireMediationConstants.PEAK_FIELD_NAME));
            assertNotNull("Incoming Item Creation Failed ", testEnvBuilder.idForCode(INCOMING_CALL_ITEM));
            assertNotNull("Out going Item Creation Failed ", testEnvBuilder.idForCode(OUTGOING_CALL_ITEM));
            assertNotNull("On net Item Creation Failed ", testEnvBuilder.idForCode(ON_NET_CALL_ITEM));
            assertNotNull("Forwarded Item Creation Failed ", testEnvBuilder.idForCode(FORWARDED_CALL_ITEM));
            assertNotNull("Mediation Configuration  Creation Failed ", testEnvBuilder.idForCode(SAPPHIRE_MEDIATION_CONFIG_NAME));
        });
    }

    @AfterClass
    public void afterClass() {
        logger.info("Dropping {}", ACCOUNT_TYPE_ITEM_MAP_TABLE_NAME);
        dropTable(ACCOUNT_TYPE_ITEM_MAP_TABLE_NAME);

        logger.info("Dropping {}", CARRIER_MAP_TABLE_NAME);
        dropTable(CARRIER_MAP_TABLE_NAME);

        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        testBuilder = null;
    }

    @Test(enabled = true)
    public void test01MediationUpload() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        List<Integer> assets = new ArrayList<>();
        try {
            testBuilder.given(envBuilder -> {
                JbillingAPI api = envBuilder.getPrancingPonyApi();

                logger.info("creating user 1");
                UserWS user1 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_1)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();
                assertNotNull("Test User 1 Creation Failed", user1);
                logger.info("created user 1 {}", user1.getId());
                users.add(user1.getId());

                Integer asset1 = buildAndPersistAsset(envBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20051526");
                assertNotNull("Asset 1 Creation Failed", asset1);
                logger.info("created asset1 {}", asset1);
                assets.add(asset1);

                Map<Integer, BigDecimal> productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                Map<Integer, List<Integer>> productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset1));

                Integer order1 = createOrder("order1" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_1);

                assertNotNull("Order 1 Creation Failed", order1);
                logger.debug("Order 1 Created {}", order1);
                orders.add(order1);

                logger.info("creating user 2");
                UserWS user2 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_2)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 2 Creation Failed", user2);
                logger.info("created user 2 {}", user2.getId());
                users.add(user2.getId());

                Integer asset2 = buildAndPersistAsset(envBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20076579");
                assertNotNull("Asset 2 Creation Failed", asset2);
                logger.info("created asset2 {}", asset2);
                assets.add(asset2);

                productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset2));

                Integer order2 = createOrder("order2" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_2);

                assertNotNull("Order 2 Creation Failed", order2);
                logger.debug("Order 2 Created {}", order2);
                orders.add(order2);

                logger.info("creating user 3");
                UserWS user3 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_3)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 3 Creation Failed", user3);
                logger.info("created user 3 {}", user3.getId());
                users.add(user3.getId());

                Integer asset3 = buildAndPersistAsset(envBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20051520");
                assertNotNull("Asset 3 Creation Failed", asset3);
                logger.info("created asset3 {}", asset3);
                assets.add(asset3);

                productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset3));

                Integer order3 = createOrder("order3" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_3);

                assertNotNull("Order 3 Creation Failed", order3);
                logger.debug("Order 3 Created {}", order3);
                orders.add(order3);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull("Test User 1 Creation Failed", testEnvBuilder.idForCode(TEST_USER_1));
                assertNotNull("Test User 2 Creation Failed", testEnvBuilder.idForCode(TEST_USER_2));
            }).validate((testEnv, testEnvBuilder) -> {
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                Integer configId = getMediationConfiguration(api, SAPPHIRE_MEDIATION_JOB_NAME);
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME);
                api.processCDR(configId, Arrays.asList(convertFileToString(cdrFile)));
            }).validate((testEnv, testEnvBuilder) -> {
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Error Record ", Integer.valueOf(0), mediationProcess.getErrors());
                assertEquals("Mediation Done And Billable ", Integer.valueOf(4), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS carrierUsageOrder = api.getLatestOrder(testEnvBuilder.idForCode(CARRIER_USER_2));
                orders.add(carrierUsageOrder.getId());
                assertNotNull("Mediation Should Create Order", carrierUsageOrder);
                assertEquals("Carrier Order Amount ", new BigDecimal("0.00"), carrierUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                OrderWS usageOrder1 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_1));
                assertNotNull("Mediation Should Create Order", usageOrder1);
                assertEquals("Usage Order Amount ", new BigDecimal("1032.50"), usageOrder1.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder1.getId());

                OrderWS usageOrder2 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_2));
                assertNotNull("Mediation Should Create Order", usageOrder2);
                assertEquals("Usage Order Amount ", new BigDecimal("115.00"), usageOrder2.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder2.getId());

                OrderWS usageOrder3 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_3));
                assertNotNull("Mediation Should Create Order", usageOrder3);
                assertEquals("Usage Order Amount ", new BigDecimal("0.00"), usageOrder3.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder3.getId());
            });
        } finally {
            orders.stream().forEach(api :: deleteOrder);
            for(Integer user : users){
                api.deleteUser(user);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }
    }

    @Test(enabled = true)
    public void test02SkipAndErrorMediationUpload() {
        testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer configId = getMediationConfiguration(api, SAPPHIRE_MEDIATION_JOB_NAME);
            File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + SKIP_CDR_FILE_NAME);
            api.processCDR(configId, Arrays.asList(convertFileToString(cdrFile)));
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Error Record ", Integer.valueOf(1), mediationProcess.getErrors());
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
        });
    }

    @Test(enabled = true)
    public void test03RecycleMediation() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        List<Integer> assets = new ArrayList<>();
        try {
            testBuilder.given(envBuilder -> {
                JbillingAPI api = envBuilder.getPrancingPonyApi();
                Integer configId = getMediationConfiguration(api, SAPPHIRE_MEDIATION_JOB_NAME);
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + RECYCLE_CDR_FILE_NAME);
                api.processCDR(configId, Arrays.asList(convertFileToString(cdrFile)));
            }).validate((testEnv, testEnvBuilder) -> {
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Error Record ", Integer.valueOf(1), mediationProcess.getErrors());
                assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            }).validate((testEnv, testEnvBuilder) -> {
                logger.info("creating user 4");
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                UserWS user4 = testEnvBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_4)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 4 Creation Failed", user4);
                logger.info("created user 4 {}", user4.getId());
                users.add(user4.getId());

                Integer asset4 = buildAndPersistAsset(testEnvBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20051536");
                assertNotNull("Asset 4 Creation Failed", asset4);
                logger.info("created asset4 {}", asset4);
                assets.add(asset4);

                Map<Integer, BigDecimal> productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                Map<Integer, List<Integer>>productAssetMap  = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset4));

                Integer order4 = createOrder("order4" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_4);

                assertNotNull("Order 4 Creation Failed", order4);
                logger.debug("Order 4 Created {}", order4);
                orders.add(order4);
            }).validate((testEnv, testEnvBuilder) -> {
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                // trigger recycle mediation
                api.runRecycleForProcess(api.getMediationProcessStatus().getMediationProcessId());
                pauseUntilMediationStarts(30, api);
            }).validate((testEnv, testEnvBuilder) -> {
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Error Record ", Integer.valueOf(0), mediationProcess.getErrors());
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS usageOrder = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_4));
                orders.add(usageOrder.getId());
                assertNotNull("Mediation Should Create Order", usageOrder);
                assertEquals("Carrier Order Amount ", new BigDecimal("1032.50"), usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        } finally {
            for(Integer order : orders){
                api.deleteOrder(order);
            }
            for(Integer user : users){
                api.deleteUser(user);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }
    }

    /**
     * Test for Outgoing Call Using Route Rate Card
     * Two Scenarios - (1)Off Peak false and (2)Off Peak true
     */
    @Test(enabled = true)
    public void test04RouteRateCardMediation() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        List<Integer> assets = new ArrayList<>();
        try {
            testBuilder.given(envBuilder -> {
                Integer routeRateCardProduct = buildAndPersistProductWithPriceModel(envBuilder, api, OUTGOING_CALL_ITEM, false,
                        envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), callRateCardPrice ,true);
                jdbcTemplate.update("UPDATE account_type_item_map SET item_id = " + routeRateCardProduct +" WHERE cdr_type LIKE 'Out Going Call'");

                Integer routeRateCardProductId = buildAndPersistProductWithPriceModel(envBuilder, api, FORWARDED_CALL_ITEM_NEW, false,
                        envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), callRateCardPrice ,true);
                jdbcTemplate.update("UPDATE account_type_item_map SET item_id = " + routeRateCardProductId
                        +" WHERE cdr_type LIKE 'Forwarded Call'");
            }).validate((testEnv, testEnvBuilder) -> {
                logger.info("creating user 041");
                UserWS user041 = testEnvBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_041)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();
                assertNotNull("Test User 041 {} Creation Failed", user041);
                logger.info("created user 041 {}", user041.getId());
                users.add(user041.getId());
                Integer asset041 = buildAndPersistAsset(testEnvBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20076222");
                assertNotNull("Asset 041 {} Creation Failed", asset041);
                logger.info("created asset 041 {}", asset041);
                assets.add(asset041);

                Map<Integer, BigDecimal> productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                Map<Integer, List<Integer>> productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset041));

                Integer order041 = createOrder("order041" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_041);
                assertNotNull("Order 041 {} Creation Failed", order041);
                logger.debug("Order 041 Created {}", order041);
                orders.add(order041);
            }).validate((testEnv, testEnvBuilder) -> {
                logger.info("creating user 042");
                UserWS user042 = testEnvBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_042)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 042 {} Creation Failed", user042);
                logger.info("created user 042 {}", user042.getId());
                users.add(user042.getId());
                Integer asset042 = buildAndPersistAsset(testEnvBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20076223");
                assertNotNull("Asset 042 {} Creation Failed", asset042);
                logger.info("created asset 042 {}", asset042);
                assets.add(asset042);
                Map<Integer, BigDecimal> productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                Map<Integer, List<Integer>> productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset042));

                Integer order042 = createOrder("order042" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_042);
                assertNotNull("Order 042 {} Creation Failed", order042);
                logger.debug("Order 042 Created {}", order042);
                orders.add(order042);
            }).validate((testEnv, testEnvBuilder) -> {
                logger.info("creating user 043");
                UserWS user043 = testEnvBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_043)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();
                assertNotNull("Test User 043 Creation Failed", user043);
                logger.info("created user 043 {}", user043.getId());
                users.add(user043.getId());
                Integer asset043 = buildAndPersistAsset(testEnvBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20076224");
                assertNotNull("Asset 043 Creation Failed", asset043);
                logger.info("created asset 043 {}", asset043);
                assets.add(asset043);
                Map<Integer, BigDecimal> productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                Map<Integer, List<Integer>> productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset043));

                Integer order043 = createOrder("order043" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_043);
                assertNotNull("Order 043 Creation Failed", order043);
                logger.debug("Order 043 Created {}", order043);
                orders.add(order043);
            }).validate((testEnv, testEnvBuilder) -> {
                UserWS user044 = testEnvBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_044)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();
                assertNotNull("Test User 044 Creation Failed", user044);
                logger.info("created user 044 {}", user044.getId());
                users.add(user044.getId());
                Integer asset044 = buildAndPersistAsset(testEnvBuilder, NUMBER_ASSET_CATEGORY_ID, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, "20076225");
                assertNotNull("Asset 044 Creation Failed", asset044);
                logger.info("created asset 044 {}", asset044);
                assets.add(asset044);
                Map<Integer, BigDecimal> productQuantityMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                Map<Integer, List<Integer>> productAssetMap = Collections.singletonMap(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset044));

                Integer order044 = createOrder("order044" , new Date(), null, envHelper.getOrderPeriodMonth(api),
                        true, productQuantityMap, productAssetMap, TEST_USER_044);
                assertNotNull("Order 044 Creation Failed", order044);
                logger.debug("Order 044 Created {}", order044);
                orders.add(order044);
            }).validate((testEnv, testEnvBuilder) -> {
                Integer configId = getMediationConfiguration(api, SAPPHIRE_MEDIATION_JOB_NAME);
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + RATE_CARD_CDR_FILE_NAME);

                api.processCDR(configId, Arrays.asList(convertFileToString(cdrFile)));
                wait(30);
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);

                assertEquals("Mediation Error Record ", Integer.valueOf(0), mediationProcess.getErrors());
                assertEquals("Mediation Done And Billable ", Integer.valueOf(4), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());

                OrderWS usageOrder041 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_041));
                assertNotNull("Mediation Should Create Order 041", usageOrder041);
                assertEquals("Usage Order 041 Amount ", new BigDecimal("3.83"), usageOrder041.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder041.getId());
                OrderWS usageOrder042 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_042));
                assertNotNull("Mediation Should Create Order 042", usageOrder042);
                assertEquals("Usage Order 042 Amount ", new BigDecimal("3.50"), usageOrder042.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder042.getId());
                OrderWS usageOrder043 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_043));
                assertNotNull("Mediation Should Create Order 043", usageOrder043);
                assertEquals("Usage Order 043 Amount ", new BigDecimal("2.50"), usageOrder043.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder043.getId());
                OrderWS usageOrder044 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_044));
                assertNotNull("Mediation Should Create Order 044", usageOrder044);
                assertEquals("Usage Order 044 Amount ", new BigDecimal("3.50"), usageOrder044.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                orders.add(usageOrder044.getId());
                try{
                    Thread.sleep(120);
                }catch(Exception e){
                    logger.error(e.getMessage(), e);
                }
            });
        } finally {
            for(Integer order : orders){
                api.deleteOrder(order);
            }
            for(Integer user : users){
                api.deleteUser(user);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }

    }

    private void createTable(String tableName, Map<String, String> columnDetails) {
        try {
            String createTableQuery = "CREATE TABLE " + tableName;
            StringBuilder columnBuilder = new StringBuilder().append(" (");

            columnBuilder.append(columnDetails.entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(Collectors.joining(",")));
            columnBuilder.append(" )");
            jdbcTemplate.execute(createTableQuery + columnBuilder.toString());
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed During table creation ", ex);
        }
    }

    private void dropTable(String tableName) {
        jdbcTemplate.execute("DROP TABLE "+ tableName);
    }

    private String convertFileToString(File file) {
        try {
            String fileContent = FileUtils.readFileToString(file);
            logger.info("File data {}", fileContent);
            return fileContent;
        } catch(IOException ex) {
            logger.error("Error !", ex);
            fail("Failed During table creation ", ex);
            return null;
        }
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

    private Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, boolean allowDecimal, String rate) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .global(global)
                .withFlatPrice(rate)
                .allowDecimal(allowDecimal)
                .build();
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value =  new MetaFieldBuilder()
        .name(name)
        .dataType(dataType)
        .entityType(entityType)
        .primary(true)
        .build();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);

    }

    private Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
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

    private PriceModelWS buildRateCardPriceModel() {
        Integer routeRateCardId = jdbcTemplate.queryForObject("( select id from route_rate_card where table_name='route_rate_1_sapphire_ratecard' )",
                Integer.class);

        PriceModelWS routeRate = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
        SortedMap<String, String> attributes = new TreeMap<String, String>();
        attributes.put("route_rate_card_id", Integer.toString(routeRateCardId));
        attributes.put("cdr_duration_field_name", "duration");
        routeRate.setAttributes(attributes);
        return routeRate;
    }

    private Integer buildAndPersistProductWithPriceModel(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, PriceModelWS priceModelWS, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withDatePriceModel(com.sapienter.jbilling.server.util.Util.getEpochDate(), priceModelWS)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        JbillingAPI api = environment.getPrancingPonyApi();
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));
        values.add(new MetaFieldValueWS(SapphireMediationConstants.ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME, null, DataType.STRING, true, ACCOUNT_TYPE_ITEM_MAP_TABLE_NAME));
        values.add(new MetaFieldValueWS(SapphireMediationConstants.CARRIER_TABLE_FIELD_NAME, null, DataType.STRING, true, CARRIER_MAP_TABLE_NAME));
        values.add(new MetaFieldValueWS(SapphireMediationConstants.PEAK_FIELD_NAME, null, DataType.STRING, true, "20:00-23:59"));
        values.add(new MetaFieldValueWS(SapphireMediationConstants.SATELLITE_COUNTRY_CODE_DATA_TABLE_NAME, null, DataType.STRING, true, SATELLITE_COUNTRY_CODE_DATA_TABLE));
        int entityId = api.getCallerCompanyId();
        logger.debug("Created Company Level MetaFields {}", values);
        values.forEach(value -> {
            value.setEntityId(entityId);
        });
        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updateCompany(company);

    }

    private Integer buildAndPersistMediationConfiguration(TestEnvironmentBuilder envBuilder, JbillingAPI api, String configName, String jobLauncherName) {
        return envBuilder.mediationConfigBuilder(api)
                .withName(configName)
                .withLauncher(jobLauncherName)
                .withLocalInputDirectory(CDR_BASE_DIRECTORY)
                .build();
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

    private void setUpAccountTypeItemMapTable(TestEnvironment testEnvBuilder) {
        String accountTypeId = String.valueOf(testEnvBuilder.idForCode(ACCOUNT_NAME));
        String incomingItemId = String.valueOf(testEnvBuilder.idForCode(INCOMING_CALL_ITEM));
        String outGoingItemId = String.valueOf(testEnvBuilder.idForCode(OUTGOING_CALL_ITEM));
        String onNetItemId = String.valueOf(testEnvBuilder.idForCode(ON_NET_CALL_ITEM));
        String forwardedItemId = String.valueOf(testEnvBuilder.idForCode(FORWARDED_CALL_ITEM));

        // inserting in coming row
        jdbcTemplate.update("INSERT INTO account_type_item_map (account_type_id, cdr_type, item_id) "
                + "VALUES (?, ? , ?)", new Object[] {accountTypeId, SapphireMediationConstants.INCOMING_CALL_CDR_TYPE, incomingItemId});

        // out going in coming row
        jdbcTemplate.update("INSERT INTO account_type_item_map (account_type_id, cdr_type, item_id) "
                + "VALUES (?, ? , ?)", new Object[] {accountTypeId, SapphireMediationConstants.OUT_GOING_CALL_CDR_TYPE, outGoingItemId});

        // on net row
        jdbcTemplate.update("INSERT INTO account_type_item_map (account_type_id, cdr_type, item_id) "
                + "VALUES (?, ? , ?)", new Object[] {accountTypeId, SapphireMediationConstants.ON_NET_CALL_CDR_TYPE, onNetItemId});

        // forwarded row
        jdbcTemplate.update("INSERT INTO account_type_item_map (account_type_id, cdr_type, item_id) "
                + "VALUES (?, ? , ?)", new Object[] {accountTypeId, SapphireMediationConstants.FORWARDED_CALL_CDR_TYPE, forwardedItemId});
    }

    private void setUpCarrierTable(TestEnvironmentBuilder envBuilder) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        UserWS carrierUser1 = envBuilder.customerBuilder(api)
                .withUsername(CARRIER_USER_1)
                .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                .addTimeToUsername(false)
                .withNextInvoiceDate(new Date())
                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                .build();
        assertNotNull("Carrier User 1 Creation Failed", carrierUser1);

        UserWS carrierUser2 = envBuilder.customerBuilder(api)
                .withUsername(CARRIER_USER_2)
                .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                .addTimeToUsername(false)
                .withNextInvoiceDate(new Date())
                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                .build();
        assertNotNull("Carrier User 2 Creation Failed", carrierUser2);

        jdbcTemplate.update("INSERT INTO carrier_map (jbilling_user_id, trunk_group_id) "
                + "VALUES (?, ?)", new Object[] {carrierUser1.getId(), "1"});

        jdbcTemplate.update("INSERT INTO carrier_map (jbilling_user_id, trunk_group_id) "
                + "VALUES (?, ?)", new Object[] {carrierUser2.getId(), "4"});

    }

    private void setUpSatelliteTable() {
        jdbcTemplate.update("INSERT INTO satelltite_table (country_code, location) "
                + "VALUES (?, ?)", "-1", "Inmarsat Satellite");

        jdbcTemplate.update("INSERT INTO satelltite_table (country_code, location) "
                + "VALUES (?, ?)", "-3", "Inmarsat Satellite");
    }

    private Integer createOrder(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode) {
        this.testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            List<OrderLineWS> lines = productQuantityMap.entrySet()
                    .stream()
                    .map(lineItemQuatityEntry -> {
                        OrderLineWS line = new OrderLineWS();
                        line.setItemId(lineItemQuatityEntry.getKey());
                        line.setTypeId(Integer.valueOf(1));
                        ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                        line.setDescription(item.getDescription());
                        line.setQuantity(lineItemQuatityEntry.getValue());
                        line.setUseItem(true);
                        if(null!=productAssetMap && !productAssetMap.isEmpty()
                                && productAssetMap.containsKey(line.getItemId())) {
                            List<Integer> assets = productAssetMap.get(line.getItemId());
                            line.setAssetIds(assets.toArray(new Integer[0]));
                            line.setQuantity(assets.size());
                        }
                        return line;
                    }).collect(Collectors.toList());

            envBuilder.orderBuilder(api)
            .withCodeForTests(code)
            .forUser(envBuilder.idForCode(userCode))
            .withActiveSince(activeSince)
            .withActiveUntil(activeUntil)
            .withEffectiveDate(activeSince)
            .withPeriod(orderPeriodId)
            .withProrate(prorate)
            .withOrderLines(lines)
            .withOrderChangeStatus(3)
            .build();
        }).test((testEnv, envBuilder) ->
        assertNotNull("Order Creation Failed!", envBuilder.idForCode(code))
                );
        return testBuilder.getTestEnvironment().idForCode(code);
    }

    private void pauseUntilMediationStarts(long seconds, JbillingAPI api) {
        for (int i = 0; i < seconds; i++) {
            if (!api.isMediationProcessRunning()) {
                return ;
            }
            wait(1);
        }
        throw new RuntimeException("Mediation startup wait was timeout in "+ seconds);
    }

    private void wait(int seconds) {
        logger.debug("Waiting......{}",seconds);
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
