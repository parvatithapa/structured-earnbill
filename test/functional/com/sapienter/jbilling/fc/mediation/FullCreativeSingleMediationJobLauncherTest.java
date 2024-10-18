package com.sapienter.jbilling.fc.mediation;

import static org.junit.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.api.automation.orders.OrdersTestHelper;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.fc.FullCreativeTestConfig;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
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
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

@Test(groups = { "fullcreative" }, testName = "FullCreativeSingleMediationJobLauncherTest")
@ContextConfiguration(classes = FullCreativeTestConfig.class)
public class FullCreativeSingleMediationJobLauncherTest extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private static final Integer CC_PM_ID = 5;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private String subScriptionProd01 = "testPlanSubscriptionItem"+ System.currentTimeMillis();;
    private String plan01 = "100 free minute Plan";
    private String usagePoolO1 = "UP with 100 Quantity" + System.currentTimeMillis();
    private String testAccount = "Account Type";
    private String user01 = UUID.randomUUID().toString();
    private String user02 = UUID.randomUUID().toString();
    private String user03 = UUID.randomUUID().toString();
    private String user04 = UUID.randomUUID().toString();
    private static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID= 320104;
    private String testCat1 = UUID.randomUUID().toString();
    private OrdersTestHelper testHelper;
    // Mediated Usage Products
    private static final int INBOUND_USAGE_PRODUCT_ID = 320101;
    private static final int CHAT_USAGE_PRODUCT_ID = 320102;
    private static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
    private static final String SUBSCRIPTION_ORDER_CODE1 = "subscriptionOrder1"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE2 = "subscriptionOrder2"+ UUID.randomUUID().toString();
    private static final String USER_ASSERT = "User Created {}";
    private static final String USER_CREATION_ASSERT = "User Creation Failed";
    private static final String ORDER_CREATION_ASSERT = "Order Creation Failed";
    private static final String USER_INVOICE_ASSERT = "Creating User with next invoice date {}";
    private static final String LIVE_ANSWER_META_FIELD_NAME = "Set ItemId For Live Answer";
    private static final String  FC_MEDIATION_CONFIG_NAME                  = "fcMediationJob";
    private static final String  FC_MEDIATION_JOB_NAME                     = "fcMediationJobLauncher";
    private static final List<String> CDR_TYPES = Arrays.asList("Inbound", "CHAT", "AR");

    private static final String CDR_FORMAT  = "%s,%s,tressie.johnson,%s,%s,07/05/2016,12:00:16 AM,4,3,407,2,0,50,47,0,null";

    @Resource(name = "fullCreativeJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator ->
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi())
        );
    }

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testHelper = OrdersTestHelper.INSTANCE;
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            // Creating account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            // Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, subScriptionProd01, false, envBuilder.idForCode(testCat1), "100.00", true);

            // Usage product item ids
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

            buildAndPersistMetafield(testBuilder, LIVE_ANSWER_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY);
            // creating Plans, Usage Pools for scenario 01

            // creating usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder, api, usagePoolO1, "100", envBuilder.idForCode(testCat1), items);

            // creating 100 min plan
            buildAndPersistPlan(envBuilder, api, plan01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(subScriptionProd01), Arrays.asList(envBuilder.idForCode(usagePoolO1)), planItemProd01WS, planItemProd02WS,
                    planItemProd03WS);

            // Setting Company Level Meta Fields
            setCompanyLevelMetaField(testBuilder.getTestEnvironment());

            // Creating Movius Job Launcher
            buildAndPersistMediationConfiguration(envBuilder, api, FC_MEDIATION_CONFIG_NAME, FC_MEDIATION_JOB_NAME);

            // Configuring Teclo Usage Manager Task
            FullCreativeUtil.updatePlugin(FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("MediatedUsage Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(usagePoolO1));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(plan01));
        });
    }

    @Test
    public void test01FCMediationUpload() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user01, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE1 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user01);

            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user01));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE1));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user01));
            logger.debug("build crds {}", cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record Count", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user01));
            assertNotNull("Mediation Should Create Order", order);
            assertNotNull("Mediated Order Should have lines", order.getOrderLines());
            assertEquals("Mediated Order Should have ", 3, order.getOrderLines().length);
            assertEquals("Free Usage Quantity", "100.0000000000", order.getFreeUsageQuantity());
            assertEquals("Free Usage Quantity", "100.0000000000", order.getFreeUsageQuantity());
            assertEquals("Order Total Amount", "47.5000000000", order.getTotal());

        });
    }

    @Test
    public void test02FCRecycleMediation() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        final Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user02, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, null, user02);

            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user02));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE2));
        }).validate((testEnv, testEnvBuilder) -> {
            Map<String, String> assetCDRMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            int i=0;
            for(Integer assetId : assets) {
                AssetWS asset = api.getAsset(assetId);
                assetCDRMap.put(CDR_TYPES.get(i++), asset.getIdentifier());
            }
            List<String> cdrs = buildCDR(assetCDRMap);
            logger.debug("build crds {}", cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);

            // creating order with assets
            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user02);

            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record Count", Integer.valueOf(3), mediationProcess.getErrors());
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            api.runRecycleForProcess(api.getMediationProcessStatus().getMediationProcessId());
            pauseUntilMediationStarts(30, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record Count", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user02));
            assertNotNull("Mediation Should Create Order", order);
            assertNotNull("Mediated Order Should have lines", order.getOrderLines());
            assertEquals("Mediated Order Should have ", 3, order.getOrderLines().length);
            assertEquals("Free Usage Quantity", "100.0000000000", order.getFreeUsageQuantity());
            assertEquals("Free Usage Quantity", "100.0000000000", order.getFreeUsageQuantity());
            assertEquals("Order Total Amount", "47.5000000000", order.getTotal());

        });

    }

    @Test(enabled=true)
    public void test03MediationRecordsByStatusAndCdrType() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user03, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE1 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user03);

            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user03));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE1));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user03));
            logger.debug("build crds {}", cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());

            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record Count", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user03));
            assertNotNull("Mediation Should Create Order", order);
            assertNotNull("Mediated Order Should have lines", order.getOrderLines());
            assertEquals("Mediated Order Should have ", 3, order.getOrderLines().length);
            assertEquals("Free Usage Quantity", "100.0000000000", order.getFreeUsageQuantity());
            assertEquals("Free Usage Quantity", "100.0000000000", order.getFreeUsageQuantity());
            assertEquals("Order Total Amount", "47.5000000000", order.getTotal());

            CDR_TYPES.forEach(cdrType -> {
                JbillingMediationRecord[]  mediationRecords = api.getMediationRecordsByStatusAndCdrType(mediationProcess.getId(),
                        0, 10, null, null, "PROCESSED", cdrType);
                logger.debug("Mediation Records {}", mediationRecords);

                assertNotNull("Mediation records not null!", mediationRecords);
                assertEquals("Mediation records count should be 1", Integer.valueOf(1), (Integer) Arrays.asList(mediationRecords).size());
                assertEquals("Mediation records Cdr type should be Inbound", cdrType, mediationRecords[0].getCdrType());
                assertEquals("Mediation records status should be PROCESSED", "PROCESSED", mediationRecords[0].getStatus().toString());
            });
        });
    }

    @Test
    public void test04UndoMediation() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        final Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user04, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, null, user04);

            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user04));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE2));
        }).validate((testEnv, testEnvBuilder) -> {
            Map<String, String> assetCDRMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            int i=0;
            for(Integer assetId : assets) {
                AssetWS asset = api.getAsset(assetId);
                assetCDRMap.put(CDR_TYPES.get(i++), asset.getIdentifier());
            }
            List<String> cdrs = buildCDR(assetCDRMap);
            logger.debug("build crds {}", cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);

            // creating order with assets
            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user04);
            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record Count", Integer.valueOf(3), mediationProcess.getErrors());
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            api.undoMediation(api.getMediationProcessStatus().getMediationProcessId());
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record Count", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user04));
            assertNotNull("Mediation Should Create Order", order);
            assertNotNull("Mediated Order Should have lines", order.getOrderLines());
            assertEquals("Mediated Order Should have ", 1, order.getOrderLines().length);
            logger.debug("Call counter {}", getCallCounterByOrderId(order.getId()));
            assertEquals("Call counter Should be ", Integer.valueOf(0), getCallCounterByOrderId(order.getId()));

        });

    }

    @AfterClass
    public void tearDown() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        FullCreativeUtil.updatePlugin(FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    private void triggerMediation(TestEnvironmentBuilder envBuilder, String jobConfigName, List<String> cdr) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer configId = getMediationConfiguration(api, jobConfigName);
        api.processCDR(configId, cdr);
    }

    private Map<String, String> createCDRAssetMap(String userName) {
        List<String> identifiers = getAssetIdentifiers(userName);
        Map<String, String> assetCDRMap = new HashMap<>();
        int i=0;
        for(String identifer : identifiers) {
            assetCDRMap.put(CDR_TYPES.get(i++), identifer);
        }
        return assetCDRMap;
    }

    private List<String> buildCDR(Map<String,String> assetCDRMap) {
        List<String> cdrs = new ArrayList<>();
        for(Entry<String, String> assetCDREntry : assetCDRMap.entrySet()) {
            cdrs.add(String.format(CDR_FORMAT, UUID.randomUUID().toString(), assetCDREntry.getValue(),
                    assetCDREntry.getKey(), assetCDREntry.getValue()));
        }
        return cdrs;
    }

    private List<String> getAssetIdentifiers(String userName) {
        if( null == userName || userName.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> identifiers = new ArrayList<>();
        testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            OrderWS [] orders = api.getUserSubscriptions(envBuilder.idForCode(userName));
            Arrays.stream(orders)
            .forEach(order -> {
                Arrays.stream(order.getOrderLines())
                .forEach(line -> {
                    Integer[] assetIds = line.getAssetIds();
                    if(null!=assetIds && assetIds.length!= 0 ) {
                        Arrays.stream(assetIds)
                        .forEach(assetId ->
                            identifiers.add(api.getAsset(assetId).getIdentifier())
                        );
                    }
                });

            });

        });
        return identifiers;
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

    private Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity){
        return Collections.singletonMap(productId, quantity);
    }

    private List<Integer> getAssetIdByProductId(JbillingAPI api, Integer productId, int noOfAsset) {
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(noOfAsset);
        criteria.setOffset(0);
        criteria.setSort("id");
        criteria.setTotal(-1);
        criteria.setFilters(new BasicFilter[]{basicFilter});

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
        assertNotNull("No available asset found for product "+productId, assetsResult);
        AssetWS[] availableAssets = assetsResult.getObjects();
        assertTrue("No assets found for product .", null != availableAssets && availableAssets.length != 0);
        return Arrays.stream(availableAssets)
                .map(AssetWS::getId)
                .collect(Collectors.toList());
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
            .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID)
            .build();
        }).test((testEnv, envBuilder) ->
            assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code))
        );
        return testBuilder.getTestEnvironment().idForCode(code);
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

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        JbillingAPI api = environment.getPrancingPonyApi();
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));

        values.add(new MetaFieldValueWS(LIVE_ANSWER_META_FIELD_NAME, null, DataType.STRING, true,
                String.valueOf(FullCreativeTestConstants.INBOUND_USAGE_PRODUCT_ID)));
        int entityId = api.getCallerCompanyId();
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void pauseUntilMediationStarts(long seconds, JbillingAPI api) {
        for (int i = 0; i < seconds; i++) {
            if (!api.isMediationProcessRunning()) {
                return ;
            }
            sleep(1000L);
        }
        throw new RuntimeException("Mediation startup wait was timeout in "+ seconds);
    }

    private Integer getCallCounterByOrderId(Integer orderId) {
        try {
            String sql = "SELECT SUM(call_counter) FROM order_line WHERE order_id = ? ";
            return jdbcTemplate.queryForObject(sql, Integer.class, orderId);
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed getCallCounterByOrderId",  ex);
            return -1;
        }
    }
}
