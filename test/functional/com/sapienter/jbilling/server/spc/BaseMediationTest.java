package com.sapienter.jbilling.server.spc;

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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;

@Test(groups = { "spc" })
public class BaseMediationTest {

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

    public static final String PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT = "Quantity Resolution Unit";
    public static final String PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE = "Internet Technology";
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
                    // Creating SPC rating unit
                    buildAndPersistRatingUnit();

                    // Creating SPC data rating unit
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

                    // Creating SPC Job Launcher
                    buildAndPersistMediationConfiguration(envBuilder, api, SPC_MEDIATION_CONFIG_NAME, SPC_MEDIATION_JOB_NAME);

                }).test((testEnv, testEnvBuilder) -> {
                    assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
                    assertNotNull("Mediated Categroy Creation Failed ", testEnvBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY));
                    assertNotNull("Mediation Configuration Creation Failed ", testEnvBuilder.idForCode(SPC_MEDIATION_CONFIG_NAME));
                    assertNotNull("Internet Asset Product Creation Failed", testEnvBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE));
                });
            }

            @AfterClass
            public void tearDown() {
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
                    Files.write(file.toPath(), lineContent.getBytes(), StandardOpenOption.APPEND);
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
}
