package com.sapienter.jbilling.server.spc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicInteger;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.evaluation.task.SPCMediationEvaluationStrategyTask;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.RouteRecordWS;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.spc.SpcJMRPostProcessorTask;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.NameValueString;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;

@Test(groups = { "spc" })
@ContextConfiguration(classes = SPCTestConfig.class)
public class BaseMediationTest extends AbstractTestNGSpringContextTests{

    //@formatter:off
    protected static final Logger logger                        = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected static final String BASE_DIR                      = "base_dir";
    protected static final String CDR_BASE_DIRECTORY            = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/cdr";
    protected static final String ROUTE_RATE_CARD_FILE          = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/rrc/";
    protected static final String PLAN_LEVEL_METAFIELD          = "Plan Rating";
    protected static final String ENUMERATION_METAFIELD_NAME    = "Plan Rating";
    protected static final String ORDER_CREATION_ASSERT         = "Order Creation Failed";
    protected static final String SPC_MEDIATED_USAGE_CATEGORY   = "SPC Mediation Usage Category";
    protected static final String SPC_MEDIATION_JOB_NAME        = "spcMediationJobLauncher";
    protected static final String CODE_STRING                   = "CODE_STRING";
    protected static final String ROUTE_ID                      = "route_id";
    protected static final String ACCOUNT_NAME                  = "SPC Test Account";
    private static final Integer CC_PM_ID                       = 5;
    private static final String SPC_MEDIATION_CONFIG_NAME       = "spcMediationJob";
    private static final int ORDER_CHANGE_STATUS_APPLY_ID       = 3;
    private static final File TEMP_DIR_PATH                     = new File(System.getProperty("java.io.tmpdir"));
    private static final String DATA_RATING_UNIT_NAME           = "SPC Data Rating Unit";
    public static final String INTERNET_ASSET_PLAN_ITEM_CODE   = "internet-user-names";
    public static final String SPC_CUSTOMER_CARE_ITEM_CODE     = "Calls to Southern Phone";
    public static final String SPC_CUSTOMER_CARE_ITEM_ID       = SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_CUSTOMER_CARE_NUMBER_ITEM_ID;
    public static final int BASIC_ITEM_MANAGER_PLUGIN_ID = 1;
    public static final String PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT = "Quantity Resolution Unit";
    public static final String PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE = "Internet Technology";
    private final static String PARAM_TAX_TABLE_NAME = "tax_scheme";
    public Integer mediationEvaluationStrategyPluginId;
    @Resource(name = "spcJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    protected static final List<EnumerationValueWS> ENUM_INTERNET_TECHNOLOGY_VALUES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS("ADSL"));
            add(new EnumerationValueWS("NBN"));


        }};
        protected static final List<EnumerationValueWS> ENUM_QUANTITY_RESOLUTION_UNIT_VALUES = new ArrayList<EnumerationValueWS>()  {
            {
                add(new EnumerationValueWS("Download"));
                add(new EnumerationValueWS("Upload"));
                add(new EnumerationValueWS("Total"));

            }};
            private static final String CUSTOMER_CARE__TABLE_NAME  = "CC Table Name";
            private static final String CUSTOMER_CARE_TABLE_VALUE  = "route_1_calltozero";
            private static final String INSERT_QUERY_TEMPLATE;

            private static final Map<String, String> COLUMN_CONSTRAINST_MAP;
            private static final Map<String, String> COLUMN_DETAIL_MAP;
            private static List<String[]> CUSTOMER_CARE_RECORDS;
            static {
                COLUMN_CONSTRAINST_MAP = new LinkedHashMap<>();
                COLUMN_DETAIL_MAP      = new LinkedHashMap<>();

                COLUMN_CONSTRAINST_MAP.put("id", "SERIAL NOT NUll");
                COLUMN_DETAIL_MAP.put("calltozero", "VARCHAR(255)");
                COLUMN_CONSTRAINST_MAP.putAll(COLUMN_DETAIL_MAP);
                COLUMN_CONSTRAINST_MAP.put("PRIMARY KEY", " ( id ) ");

                INSERT_QUERY_TEMPLATE = new StringBuilder().append("INSERT INTO ")
                        .append(CUSTOMER_CARE_TABLE_VALUE)
                        .append(" ")
                        .append('(')
                        .append(COLUMN_DETAIL_MAP.entrySet().stream().map(Entry::getKey).collect(Collectors.joining(",")))
                        .append(')')
                        .append(" VALUES (")
                        .append(COLUMN_DETAIL_MAP.entrySet().stream().map(entry -> "?").collect(Collectors.joining(",")))
                        .append(" )")
                        .toString();

                String[] record1 = {"02244747100"};
                String[] record2 = {"02131464"};
                String[] record3 = {"021300790585"};
                String[] record4 = {"021800017461"};
                CUSTOMER_CARE_RECORDS = Arrays.asList(record1, record2, record3, record4);
            }
            //@formatter:on

            protected TestBuilder testBuilder;
            protected JbillingAPI api;
            protected Integer spcRatingUnitId;
            protected Integer spcDataUnitId;

            protected TestBuilder getTestEnvironment() {
                return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
                    EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
                    api = testEnvCreator.getPrancingPonyApi();
                });
            }

            @SuppressWarnings({})
            @BeforeClass
            public void initializeTests() {
                testBuilder = getTestEnvironment();
                testBuilder.given(envBuilder -> {
                    Hashtable<String, String> parameters = getParamsForJMRPostProcessor() ;
                    configureJMrPostProcessor(api, parameters);

                    // Creating SPC rating unit
                    buildAndPersistRatingUnit();

                  //creating data table with name 'route_1_taxes'
                    createTable(CUSTOMER_CARE_TABLE_VALUE, COLUMN_CONSTRAINST_MAP);
                    CUSTOMER_CARE_RECORDS.stream().forEach(this :: insertCustomerCareNumbers);
                    String priceUnitName = "Bytes";
                    String incrementUnitName = "GB";
                    String incrementUnitQuantity = "1073741824";
                    buildAndPersistDataRatingUnit(DATA_RATING_UNIT_NAME, priceUnitName, incrementUnitName, incrementUnitQuantity);

                    // Creating account type
                    buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);

                    // Creating mediated usage category
                    buildAndPersistCategory(envBuilder, api, SPC_MEDIATED_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

                    buildAndPersistFlatProduct(envBuilder, api, INTERNET_ASSET_PLAN_ITEM_CODE, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "0.00", true, 1, false);

                    buildAndPersistFlatProduct(envBuilder, api, SPC_CUSTOMER_CARE_ITEM_CODE, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "0.00", true, 0, false);

                    if (!isMetaFieldPresent(EntityType.COMPANY, SPC_CUSTOMER_CARE_ITEM_ID)){
                        buildAndPersistMetafield(testBuilder, SPC_CUSTOMER_CARE_ITEM_ID, DataType.STRING,
                                EntityType.COMPANY);
                    }

                    // Creating SPC Job Launcher
                    buildAndPersistMediationConfiguration(envBuilder, api, SPC_MEDIATION_CONFIG_NAME, SPC_MEDIATION_JOB_NAME);
                    setCompanyLevelMetaField(testBuilder.getTestEnvironment(), SPC_CUSTOMER_CARE_ITEM_ID,
                            envBuilder.idForCode(SPC_CUSTOMER_CARE_ITEM_CODE).toString());

                }).test((testEnv, testEnvBuilder) -> {
                    assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
                    assertNotNull("Mediated Categroy Creation Failed ", testEnvBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY));
                    assertNotNull("Mediation Configuration Creation Failed ", testEnvBuilder.idForCode(SPC_MEDIATION_CONFIG_NAME));
                    assertNotNull("Internet Asset Product Creation Failed", testEnvBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE));
                });
            }

    private Hashtable<String, String> getParamsForJMRPostProcessor() {
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("rounding mode", "ROUND_HALF_UP");
        parameters.put("rounding scale", "4");
        parameters.put("minimum charge", "0.00");
        parameters.put("tax table name", "tax_scheme");
        parameters.put("tax date format", "dd-MM-yyyy");
        parameters.put("Credit pool table name", "credit_pool");
        parameters.put("Reduce pricing_field in JMR table", Boolean.TRUE.toString());
        return parameters;
    }

    @AfterClass
            public void tearDown() {
                dropTable(CUSTOMER_CARE_TABLE_VALUE);
                testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
                testBuilder.removeEntitiesCreatedOnJBilling();
                testBuilder = null;
            }



            protected PriceModelWS buildRateCardPriceModel(Integer routeRateCardId) {
                return buildRateCardPriceModel(routeRateCardId, "duration");
            }

            PriceModelWS buildRateCardPriceModel(Integer routeRateCardId, String quantityFieldName) {
                PriceModelWS routeRate = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
                SortedMap<String, String> attributes = new TreeMap<>();
                attributes.put("route_rate_card_id", Integer.toString(routeRateCardId));
                attributes.put("cdr_duration_field_name", quantityFieldName);
                routeRate.setAttributes(attributes);
                return routeRate;
            }

            protected Integer buildAndPersistEnumeration (TestEnvironmentBuilder envBuilder, List<EnumerationValueWS> values, String name) {

                EnumerationWS enUmeration = new EnumerationWS();

                enUmeration.setValues(values);
                enUmeration.setName(name);
                enUmeration.setEntityId(envBuilder.getPrancingPonyApi().getCallerCompanyId());

                Integer enumId =  envBuilder.getPrancingPonyApi().createUpdateEnumeration(enUmeration);
                envBuilder.env().add(name, enumId, name,  envBuilder.getPrancingPonyApi(), TestEntityType.ENUMERATION);
                return enumId;

            }

            OrderLineWS getLineByItemId(OrderWS order, Integer itemId) {
                for (OrderLineWS orderLine : order.getOrderLines()) {
                    if (orderLine.getItemId().equals(itemId)) {
                        return orderLine;
                    }
                }
                return null;
            }

            protected Integer createOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId,
                    boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode) {
                this.testBuilder.given(envBuilder -> {
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
                                if (null != productAssetMap && !productAssetMap.isEmpty()
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

            protected Integer createOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate,
                    Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode,
                    Integer billingType) {
                Integer createdOrder = createOrder(code, activeSince, activeUntil, orderPeriodId, prorate, productQuantityMap, productAssetMap, userCode);
                OrderWS order = api.getOrder(createdOrder);
                order.setBillingTypeId(billingType);
                api.updateOrder(order, null);
                return createdOrder;
            }

            protected Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity) {
                return Collections.singletonMap(productId, quantity);
            }

            protected void setPlanLevelMetaField(Integer planId, String name) {
                logger.debug("setting the plan level metafields for plan {}", planId);
                PlanWS plan = api.getPlanWS(planId);
                List<MetaFieldValueWS> values = new ArrayList<>();
                values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
                Arrays.asList(plan.getMetaFields()).forEach(mf -> {
                    if (mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                        mf.setValue(name);
                        values.add(mf);
                    }
                });
                values.forEach(value ->
                value.setEntityId(api.getCallerCompanyId())
                        );
                plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
                api.updatePlan(plan);
            }

            protected void setPlanLevelMetaFieldForInternet(Integer planId, String it, String qru) {
                logger.debug("setting the plan level metafields for plan {}", planId);
                PlanWS plan = api.getPlanWS(planId);
                List<MetaFieldValueWS> values = new ArrayList<>();
                values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
                Arrays.asList(plan.getMetaFields()).forEach(mf -> {
                    if (mf.getFieldName().equals(PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT)) {
                        mf.setValue(qru);
                        values.add(mf);
                    }
                    else    if (mf.getFieldName().equals(PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE)) {
                        mf.setValue(it);
                        values.add(mf);
                    }
                });
                values.forEach(value ->
                value.setEntityId(api.getCallerCompanyId())
                        );
                plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
                api.updatePlan(plan);
            }

            protected Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer... paymentMethodTypeId) {
                AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                        .withName(name)
                        .withPaymentMethodTypeIds(paymentMethodTypeId)
                        .build();
                return accountTypeWS.getId();
            }

            protected Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global,
                    ItemBuilder.CategoryType categoryType) {
                return envBuilder.itemBuilder(api)
                        .itemType()
                        .withCode(code)
                        .withCategoryType(categoryType)
                        .global(global)
                        .allowAssetManagement(1)
                        .build();
            }

            private void insertCustomerCareNumbers(String[] record) {
                logger.debug("inserting the tax rate details to table!");
                try {
                    jdbcTemplate.update(INSERT_QUERY_TEMPLATE, new Object[] {
                            record[0]
                    });
                } catch(Exception ex) {
                    logger.error("Error !", ex);
                    fail("Failed Insertion In data table "+ CUSTOMER_CARE__TABLE_NAME, ex);
                }
            }

            protected Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
                MetaFieldWS value = new MetaFieldBuilder()
                .name(name)
                .dataType(dataType)
                .entityType(entityType)
                .primary(true)
                .build();
                Integer id = api.createMetaField(value);
                testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
                return testBuilder.getTestEnvironment().idForCode(name);
            }

            protected Integer buildAndPersistMediationConfiguration(TestEnvironmentBuilder envBuilder, JbillingAPI api, String configName,
                    String jobLauncherName) {
                return envBuilder.mediationConfigBuilder(api)
                        .withName(configName)
                        .withLauncher(jobLauncherName)
                        .withLocalInputDirectory(com.sapienter.jbilling.common.Util.getSysProp(BASE_DIR) + "spc-mediation-test")
                        .build();
            }

            protected Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {
                MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
                for (MediationConfigurationWS mediationConfigurationWS : allMediationConfigurations) {
                    if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                            (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                        return mediationConfigurationWS.getId();
                    }
                }
                return null;
            }

            private void buildAndPersistRatingUnit() {
                String ratingUnitName = "SPC Rating Unit";
                if (!isRatingUnitPresent(ratingUnitName)) {
                    RatingUnitWS ratingUnitWS = new RatingUnitWS();
                    ratingUnitWS.setName(ratingUnitName);
                    ratingUnitWS.setPriceUnitName("Minute");
                    ratingUnitWS.setIncrementUnitName("Seconds");
                    ratingUnitWS.setIncrementUnitQuantity("1");
                    spcRatingUnitId = api.createRatingUnit(ratingUnitWS);
                }
            }

            private void buildAndPersistDataRatingUnit(String ratingUnitName, String priceUnitName, String incrementUnitName, String incrementUnitQuantity) {
                if (!isRatingUnitPresent(ratingUnitName)) {
                    RatingUnitWS ratingUnitWS = new RatingUnitWS();
                    ratingUnitWS.setName(ratingUnitName);
                    ratingUnitWS.setPriceUnitName(priceUnitName);
                    ratingUnitWS.setIncrementUnitName(incrementUnitName);
                    ratingUnitWS.setIncrementUnitQuantity(incrementUnitQuantity);
                    spcDataUnitId = api.createRatingUnit(ratingUnitWS);
                }
            }

            private void sleep(long millis) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            protected void pauseUntilMediationCompletes(long seconds, JbillingAPI api) {
                sleep(3000L); // initial wait.
                for (int i = 0; i < seconds; i++) {
                    if (!api.isMediationProcessRunning()) {
                        return;
                    }
                    sleep(1000L);
                }
                throw new RuntimeException("Mediation startup wait was timeout in " + seconds);
            }

            protected void waitFor(long seconds) {
                logger.debug("wait for {}", seconds);
                for (int i = 0; i < seconds; i++) {
                    logger.debug("...{}", i);
                    sleep(1000L);
                }
                logger.debug("wait was timedout in {}", seconds);
            }

            protected Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber) {
                return buildAndPersistAsset(envBuilder, categoryId, itemId, phoneNumber, phoneNumber);
            }

            protected Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber, String code) {
                ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
                Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                        filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                        .equals("Available")).collect(Collectors.toList()).get(0).getId();
                return envBuilder.assetBuilder(api)
                        .withItemId(itemId)
                        .withAssetStatusId(assetStatusId)
                        .global(true)
                        .withIdentifier(phoneNumber)
                        .withCode(code)
                        .build();
            }

            protected PlanItemWS buildPlanItem(Integer itemId, Integer periodId, String quantity, PriceModelWS price, Date pricingDate) {
                return PlanBuilder.PlanItemBuilder.getBuilder()
                        .withItemId(itemId)
                        .addModel(null != pricingDate ? pricingDate : com.sapienter.jbilling.server.util.Util.getEpochDate(), price)
                        .withBundledPeriodId(periodId)
                        .withBundledQuantity(quantity)
                        .build();
            }

            protected Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
                    boolean global, Integer categoryId, String flatPrice, boolean allowDecimal, Integer allowAssets, boolean isPlan) {
                return envBuilder.itemBuilder(api)
                        .item()
                        .withCode(code)
                        .withType(categoryId)
                        .withFlatPrice(flatPrice)
                        .global(global)
                        .useExactCode(true)
                        .allowDecimal(allowDecimal)
                        .withAssetManagementEnabled(allowAssets)
                        .build();
            }

            protected Integer buildAndPersistFlatProductWithRating(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
                    boolean global, Integer categoryId, String flatPrice, boolean allowDecimal, Integer allowAssets, boolean isPlan, Date ratingDate,
                    RatingConfigurationWS ratingConfiguration) {
                return envBuilder.itemBuilder(api)
                        .item()
                        .withCode(code)
                        .withType(categoryId)
                        .withFlatPrice(flatPrice)
                        .global(global)
                        .useExactCode(true)
                        .allowDecimal(allowDecimal)
                        .withAssetManagementEnabled(allowAssets)
                        .addRatingConfigurationWithDate(ratingDate, ratingConfiguration)
                        .build();
            }

            protected static MatchingFieldWS getMatchingField(String description, String orderSequence, String mediationField, String matchingField,
                    Integer routeId, Integer routeRateCardId) throws SessionInternalError {
                MatchingFieldWS matchingFieldWS = new MatchingFieldWS();
                matchingFieldWS.setDescription(description);
                matchingFieldWS.setOrderSequence(orderSequence);
                matchingFieldWS.setMediationField(mediationField);
                matchingFieldWS.setMatchingField(matchingField);
                matchingFieldWS.setRequired(Boolean.TRUE);
                matchingFieldWS.setType(MatchingFieldType.EXACT.toString());
                matchingFieldWS.setRouteId(routeId);
                matchingFieldWS.setRouteRateCardId(routeRateCardId);
                matchingFieldWS.setMandatoryFieldsQuery("obsoleted");
                return matchingFieldWS;
            }

            /**
             * Creates file in temp directory with header and lines.
             *
             * @param fileName
             * @param fileExtension
             * @param header
             * @param lines
             * @return
             */
            protected String createFileWithData(String fileName, String fileExtension, String header, List<String> lines) {
                if (CollectionUtils.isEmpty(lines)) {
                    throw new IllegalArgumentException("Please proives lines");
                }
                if (!fileExtension.startsWith(".")) {
                    fileExtension = "." + fileExtension;
                }
                File file = new File(TEMP_DIR_PATH, fileName + System.currentTimeMillis() + fileExtension);
                if (file.exists()) {
                    file.delete();
                }
                try {
                    if (!file.createNewFile()) {
                        throw new RuntimeException("File " + fileName + "Creation failed!");
                    }
                    if (StringUtils.isNotEmpty(header)) {
                        Files.write(file.toPath(), (header + System.lineSeparator()).getBytes());
                    }
                    String lineContent = lines.stream().collect(Collectors.joining(System.lineSeparator()));
                    Files.write(file.toPath(), lineContent.concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);

                    return file.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException("Error in createFileWithData", e);
                }
            }

            protected Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
                    Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
                return envBuilder.planBuilder(api, code)
                        .withDescription(desc)
                        .withPeriodId(periodId)
                        .withItemId(itemId)
                        .withUsagePoolsIds(usagePools)
                        .withPlanItems(Arrays.asList(planItems))
                        .build().getId();
            }

            protected void setCompanyLevelMetaField(TestEnvironment environment, String fieldName, Object fieldValue) {
                CompanyWS company = api.getCompany();
                List<MetaFieldValueWS> values = new ArrayList<>();
                values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));
                values.add(new MetaFieldValueWS(fieldName, null, DataType.STRING, true, fieldValue));
                int entityId = api.getCallerCompanyId();
                logger.debug("Created Company Level MetaFields {}", values);
                values.forEach(value -> {
                    value.setEntityId(entityId);
                });
                company.setTimezone(company.getTimezone());
                company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
                api.updateCompany(company);

            }

            /**
             * @param ratingUnitName
             * @return
             */
            private boolean isRatingUnitPresent(String ratingUnitName) {
                boolean isRatingUnitPresent = false;
                for (RatingUnitWS ratingUnit : api.getAllRatingUnits()) {
                    if (ratingUnit.getName().equalsIgnoreCase(ratingUnitName)) {
                        if(ratingUnit.getName().contains(DATA_RATING_UNIT_NAME)) {
                            spcDataUnitId = ratingUnit.getId();
                        } else {
                            spcRatingUnitId = ratingUnit.getId();
                        }
                        isRatingUnitPresent = true;
                        break;
                    }
                }
                return isRatingUnitPresent;
            }

            /**
             * @param api
             * @return
             */
            protected boolean isMetaFieldPresent(EntityType entityType, String metaFieldName) {
                boolean companyLevelMFfound = false;
                for (MetaFieldWS mfws : api.getMetaFieldsForEntity(entityType.toString())) {
                    if (mfws.getName().equalsIgnoreCase(metaFieldName)) {
                        logger.debug("Metafield with name {} already present.", mfws.getName());
                        companyLevelMFfound = true;
                        break;
                    }
                }
                return companyLevelMFfound;
            }

            protected void updateExistingPlugin(JbillingAPI api, Integer pluginId, String className, Map<String, String> params) {
                PluggableTaskWS plugin = api.getPluginWS(pluginId);
                if(null == plugin) {
                    Assert.notNull(plugin, " no plugin found for id "+ pluginId + " for entity "+ api.getCallerCompanyId());
                }
                plugin.setTypeId(api.getPluginTypeWSByClassName(className).getId());
                Hashtable<String, String> parameters = new Hashtable<>();
                if(MapUtils.isNotEmpty(params)) {
                    parameters.putAll(params);
                }
                plugin.setParameters(parameters);
                api.updatePlugin(plugin);
            }

            protected void createMediationEvaluationStrategyPlugin() {
                String className = SPCMediationEvaluationStrategyTask.class.getName();
                if(ArrayUtils.isNotEmpty(api.getPluginsWS(api.getCallerCompanyId(), className))) {
                    return;
                }
                PluggableTaskWS plugin = new PluggableTaskWS();
                plugin.setProcessingOrder(1);
                PluggableTaskTypeWS pluginType = api.getPluginTypeWSByClassName(className);
                plugin.setTypeId(pluginType.getId());
                mediationEvaluationStrategyPluginId = api.createPlugin(plugin);
            }

            private void dropTable(String tableName) {
                logger.debug("droping the table {}", tableName);
                jdbcTemplate.execute("DROP TABLE "+ tableName);
            }
            
            private void createTable(String tableName, Map<String, String> columnDetails) {
                logger.debug("creating the table {}", tableName);
                try {
                    String createTableQuery = "CREATE TABLE IF NOT  EXISTS  "+ tableName;
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

            private Integer configureJMrPostProcessor(JbillingAPI api, Hashtable<String, String> parameters) {
                String taskName = SpcJMRPostProcessorTask.class.getName();
                PluggableTaskWS[] tasks = api.getPluginsWS(api.getCallerCompanyId(), taskName);
                if(ArrayUtils.isNotEmpty(tasks)) {
                    for(PluggableTaskWS task : tasks) {
                        api.deletePlugin(task.getId());
                    }
                }
                // configure JMRPostProcessorTask plugin.
                PluggableTaskWS plugin = new PluggableTaskWS();
                plugin.setTypeId(api.getPluginTypeWSByClassName(SpcJMRPostProcessorTask.class.getName()).getId());
                plugin.setProcessingOrder(1);
                plugin.setParameters(parameters);
                return api.createPlugin(plugin);
            }

    protected void validatePricingFields ( JbillingMediationRecord[] viewEvents ) {
        for ( JbillingMediationRecord jbillingMediationRecord : viewEvents ) {
            String pricingFields = jbillingMediationRecord.getPricingFields ( );
            TestCase.assertFalse ( "pricing fields should not contain CDR_IDENTIFIER" , pricingFields.contains ( "CDR_IDENTIFIER" ) );
            if (!(pricingFields.contains("data") || pricingFields.contains("aaptiu"))) {
                 TestCase.assertTrue("pricing fields should contain Service number", pricingFields.contains("SERVICE_NUMBER"));
             }

        }
    }
}