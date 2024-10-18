/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.adennet.ws.AddOnProductWS;
import com.sapienter.jbilling.server.adennet.ws.AdennetPlanWS;
import com.sapienter.jbilling.server.adennet.ws.BalanceResponseWS;
import com.sapienter.jbilling.server.adennet.ws.FeeWS;
import com.sapienter.jbilling.server.adennet.ws.PlanChangeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.PlanDescriptionWS;
import com.sapienter.jbilling.server.adennet.ws.PrimaryPlanWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.SubscriptionWS;
import com.sapienter.jbilling.server.adennet.ws.ums.TransactionResponseWS;
import com.sapienter.jbilling.server.creditnote.CreditNoteInvoiceMapWS;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestOperations;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertNotNull;

@ContextConfiguration(classes = AdennetTestConfig.class)
public class AdennetBaseConfiguration extends AbstractTestNGSpringContextTests {

    protected static final Logger logger   = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected static final String CONTACT_INFORMATION = "Contact Information";
    protected static final String ACCOUNT_NAME = "Individual";
    protected static final Integer CURRENCY_YE = 1;
    protected static final Integer MONTHLY_ORDER_PERIOD = 2;
    protected static final String CUSTOMER_DETAILS = "Contact Information";
    protected static final String IMSI_NUMBER = "IMSI Number";
    protected static final Integer PRANCING_PONY = 1;

    protected static final String ADD_ON_PRODUCTS = "Add-on Products";
    protected static final String PREPAID_INTERNET_PACKAGES = "Prepaid Internet Packages";
    protected static final String CATEGORY_SUBSCRIBER_NUMBERS = "Subscriber Numbers";
    protected static final String PRODUCT_DESCRIPTION_DOWNGRADE_FEES = "Downgrade Fees";
    protected static final String PRODUCT_CODE_DOWNGRADE_FEES = "AN_DOWNGRADE_FEES";
    protected static final String PRODUCT_CODE_MODEM_FEES = "AN_MODEM_FEES";
    protected static final String PRODUCT_DESCRIPTION_MODEM_FEES = "Modem Fees";
    protected static final String PRODUCT_CODE_SIM_CARD_FEES = "AN_SIM_CARD_FEES";
    protected static final String PRODUCT_DESCRIPTION_SIM_CARD_FEES = "Sim Card Fees";
    protected static final String PRODUCT_CODE_SERVICE_NUMBER = "AN_SERVICE_NUMBER";
    protected static final String PRODUCT_DESCRIPTION_SERVICE_NUMBER = "Service Number";
    private static final String EXTERANL_SERVICE_CLASS_NAME = AdennetExternalConfigurationTask.class.getName();
    protected static final String JBILLING_API_ADENNET = "http://localhost:8080/jbilling/api/adennet";
    protected static String USAGE_MANAGEMENT_SERVICE = "http://localhost:7080/usage-management-service";
    protected static final int SCALE_TWO = 2;
    protected static final String PLAN_NUMBER_80 = "AN_80_GB";
    protected static final String PLAN_NUMBER_60 = "AN_60_GB";
    protected static final String PLAN_NUMBER_40 = "AN_40_GB";
    protected static final String PLAN_NUMBER_20 = "AN_20_GB";
    protected static final String PLAN_NAME_80 = "80 GB";
    protected static final String PLAN_NAME_60 = "60 GB";
    protected static final String PLAN_NAME_40 = "40 GB";
    protected static final String PLAN_NAME_20 = "20 GB";

    protected static final String METAFIELD_DOWNLOAD_CAPACITY = "Download Capacity";
    protected static final String USAGE_QUATA_80_GB = "81920";
    protected static final String USAGE_QUATA_60_GB = "61440";
    protected static final String USAGE_QUATA_40_GB = "40960";
    protected static final String USAGE_QUATA_20_GB = "20480";

    protected static final String METAFIELD_PLAN_FEE = "Plan Fee";
    protected static final String PLAN_FEE_60_GB = "9000";
    protected static final String PLAN_FEE_80_GB = "12000";
    protected static final String PLAN_FEE_40_GB = "6000";
    protected static final String PLAN_FEE_20_GB = "3000";

    
    protected static final String ADENNET = "Adennet";
    protected static final String TEST_CASE = "Test Case";
    
    protected static final String EXPECTED_NO_PLAN_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER = "Expected no plan but found a plan for userId=%s and subscriberNumber=%s";
    protected static final String NO_ACTIVE_PLAN_FOUND_FOR_USER_ID_AND_SUBSCRIBER_NUMBER = "No active plan found for userId=%s and subscriberNumber=%s";
    protected static final String EXPECTED_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER = "Expected plan for userId=%s and subscriberNumber=%s";
    protected static final String EXPECTED_METHOD_NOT_ALLOWED_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER = "Expected method not allowed but found a plan for userId=%s and subscriberNumber=%s";
    protected static final Integer PLAN_ID_INVALID = 18;
    protected static final String SUBSCRIBER_NUMBER_INVALID = "7246581235";
    protected static final Integer USER_ID_INVALID = 121212;
    protected static final String PLAN_CREATION_FAILED = "Plan Creation Failed ";
    protected static final String EXPECTED_NOT_NULL_USER_WS_BUT_FOUND_NULL = "Expected not null userWS but found null";
    protected static final String ADENNET_USER_WS_ID = "AdennetUserWS Id : {}";
    protected static final String ASSET_WS = "AssetWS : {}";
    protected static final String EXPECTED_NOT_NULL_ASSET_WS_BUT_FOUND_NULL = "Expected not null AssetWS but found null";
    protected static final String RECHARGE_REQUEST_WS = "RechargeRequestWS : {}";
    protected static final String TRANSACTION_ID_SHOULD_NOT_BE_NULL = "Transaction id should not be null ";
    protected static final String TRANSACTION_ID = "Transaction Id : {}";
    protected static final String NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL = "New recharge request should not be null";
    protected static final String CHANGE_PLAN_RESPONSE = "ChangePlanResponse : {}";

    @Resource(name = "adennetJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Resource(name = "externalClient")
    protected RestOperations externalClient;

    protected JbillingAPI api;
    protected TestBuilder testBuilder;
    protected Integer planId80Gb;
    protected Integer planId60Gb;
    protected Integer planId40Gb;
    protected Integer planId20Gb;

    boolean shouldExecute;

    public static Long randomLong(Long Min, Long Max) {
        return (long) (Math.random() * (Max - Min)) + Min;
    }

    protected TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
        });
    }

    @BeforeTest(alwaysRun = true)
    public void initializeTests() throws Exception {
        logger.debug("initializeTests ::: Started");
        super.springTestContextPrepareTestInstance();
        logger.debug("AdennetBaseConfiguration.initializeTests");
        testBuilder = getTestEnvironment();
        shouldExecute = true;
        if (shouldExecute) {
            testBuilder.given(envBuilder -> {   

                buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, null);
                buildAndPersistMetafield(testBuilder, METAFIELD_DOWNLOAD_CAPACITY, DataType.STRING, EntityType.PLAN);
                buildAndPersistMetafield(testBuilder, METAFIELD_PLAN_FEE, DataType.STRING, EntityType.PLAN);

                Calendar pricingDate = Calendar.getInstance();
                pricingDate.set(Calendar.YEAR, 1970);
                pricingDate.set(Calendar.MONTH, 1);
                pricingDate.set(Calendar.DAY_OF_MONTH, 1);

                buildAndPersistCategory(envBuilder, api, ADD_ON_PRODUCTS, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, Integer.valueOf(0), Boolean.TRUE);

                buildAndPersistCategory(envBuilder, api, PREPAID_INTERNET_PACKAGES, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, Integer.valueOf(0), Boolean.TRUE);

                buildAndPersistCategoryWithAssetMetaFields(envBuilder, api, CATEGORY_SUBSCRIBER_NUMBERS, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM
                        , 1, true, createAssetMetaField());

                buildAndPersistFlatProduct(envBuilder, api, PRODUCT_CODE_DOWNGRADE_FEES, PRODUCT_DESCRIPTION_DOWNGRADE_FEES, false,
                        envBuilder.idForCode(PREPAID_INTERNET_PACKAGES), "100.00", true, 0, false);

                buildAndPersistFlatProduct(envBuilder, api, PRODUCT_CODE_MODEM_FEES, PRODUCT_DESCRIPTION_MODEM_FEES, false,
                        envBuilder.idForCode(ADD_ON_PRODUCTS), "200.00", true, 0, false);

                buildAndPersistFlatProduct(envBuilder, api, PRODUCT_CODE_SIM_CARD_FEES, PRODUCT_DESCRIPTION_SIM_CARD_FEES, false,
                        envBuilder.idForCode(ADD_ON_PRODUCTS), "100.00", true, 0, false);


                // Plan item product Creation (asset allow product)
                buildAndPersistProduct(envBuilder, api, PRODUCT_CODE_SERVICE_NUMBER, true,
                        PRODUCT_DESCRIPTION_SERVICE_NUMBER, false, envBuilder.idForCode(CATEGORY_SUBSCRIBER_NUMBERS), 1,
                        true, null, false);

                // Add to plan item
                PlanItemWS planItemProd = buildPlanItem(api, envBuilder.idForCode(PRODUCT_CODE_SERVICE_NUMBER), MONTHLY_ORDER_PERIOD, "1", "0.00", null);

                // Plan product Creation 80 GB
                buildAndPersistFlatProduct(envBuilder, api, PLAN_NUMBER_80, PLAN_NAME_80, false,
                        envBuilder.idForCode(CATEGORY_SUBSCRIBER_NUMBERS), "0.00", true, 0, true);

                planId80Gb = buildAndPersistPlan(envBuilder, api, PLAN_NUMBER_80, null, MONTHLY_ORDER_PERIOD,
                        envBuilder.idForCode(PLAN_NUMBER_80), Collections.emptyList(), planItemProd);

                logger.debug("Plan Id for 80 GB : {}", planId80Gb);
                setPlanLevelMetaField(planId80Gb, METAFIELD_DOWNLOAD_CAPACITY, USAGE_QUATA_80_GB);
                setPlanLevelMetaField(planId80Gb, METAFIELD_PLAN_FEE, PLAN_FEE_80_GB);


                // Plan product Creation 60 GB
                buildAndPersistFlatProduct(envBuilder, api, PLAN_NUMBER_60, PLAN_NAME_60, false,
                        envBuilder.idForCode(CATEGORY_SUBSCRIBER_NUMBERS), "0.00", true, 0, true);

                planId60Gb = buildAndPersistPlan(envBuilder, api, PLAN_NUMBER_60, null, MONTHLY_ORDER_PERIOD,
                        envBuilder.idForCode(PLAN_NUMBER_60), Collections.emptyList(), planItemProd);
                logger.debug("Plan Id for 60 GB : {} ",planId60Gb);

                setPlanLevelMetaField(planId60Gb, METAFIELD_DOWNLOAD_CAPACITY, USAGE_QUATA_60_GB);
                setPlanLevelMetaField(planId60Gb, METAFIELD_PLAN_FEE, PLAN_FEE_60_GB);

                // Plan product Creation 40 GB
                buildAndPersistFlatProduct(envBuilder, api, PLAN_NUMBER_40, PLAN_NAME_40, false,
                        envBuilder.idForCode(CATEGORY_SUBSCRIBER_NUMBERS), "0.00", true, 0, true);

                planId40Gb = buildAndPersistPlan(envBuilder, api, PLAN_NUMBER_40, null, MONTHLY_ORDER_PERIOD,
                        envBuilder.idForCode(PLAN_NUMBER_40), Collections.emptyList(), planItemProd);
                logger.debug("Plan Id for 40 GB : {}",planId40Gb);

                setPlanLevelMetaField(planId40Gb, METAFIELD_DOWNLOAD_CAPACITY, USAGE_QUATA_40_GB);
                setPlanLevelMetaField(planId40Gb, METAFIELD_PLAN_FEE, PLAN_FEE_40_GB);

                // Plan product Creation 20 GB
                buildAndPersistFlatProduct(envBuilder, api, PLAN_NUMBER_20, PLAN_NAME_20, false,
                        envBuilder.idForCode(CATEGORY_SUBSCRIBER_NUMBERS), "0.00", true, 0, true);

                planId20Gb = buildAndPersistPlan(envBuilder, api, PLAN_NUMBER_20, null, MONTHLY_ORDER_PERIOD,
                        envBuilder.idForCode(PLAN_NUMBER_20), Collections.emptyList(), planItemProd);
                logger.debug("Plan Id for 20 GB : {}",planId20Gb);


                setPlanLevelMetaField(planId20Gb, METAFIELD_DOWNLOAD_CAPACITY, USAGE_QUATA_20_GB);
                setPlanLevelMetaField(planId20Gb, METAFIELD_PLAN_FEE, PLAN_FEE_20_GB);

                configureAllAdennetPlugins(envBuilder);

                USAGE_MANAGEMENT_SERVICE =
                        getValueFromExternalConfigParams(EXTERANL_SERVICE_CLASS_NAME,
                                AdennetExternalConfigurationTask.BILLINGHUB_USAGE_MANAGEMENT_SERVICE_URL,api);



            }).test((testEnv, testEnvBuilder) -> {
                assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
                assertNotNull("MetaField Creation Failed", testEnvBuilder.idForCode(METAFIELD_DOWNLOAD_CAPACITY));
                assertNotNull("MetaField Creation Failed", testEnvBuilder.idForCode(METAFIELD_PLAN_FEE));
                assertNotNull("Subscriber Categroy Creation Failed ", testEnvBuilder.idForCode(CATEGORY_SUBSCRIBER_NUMBERS));
                assertNotNull("Add on product Categroy Creation Failed ", testEnvBuilder.idForCode(ADD_ON_PRODUCTS));
                assertNotNull("Prepare interent packages Categroy Creation Failed ", testEnvBuilder.idForCode(PREPAID_INTERNET_PACKAGES));
                assertNotNull("Download fees product Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CODE_DOWNGRADE_FEES));
                assertNotNull("Modem product Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CODE_MODEM_FEES));
                assertNotNull("Sim card fees product Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CODE_SIM_CARD_FEES));
                assertNotNull("Plan item product Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CODE_SERVICE_NUMBER));
                assertNotNull(PLAN_CREATION_FAILED + PLAN_NAME_80, testEnvBuilder.idForCode(PLAN_NUMBER_80));
                assertNotNull(PLAN_CREATION_FAILED + PLAN_NAME_60, testEnvBuilder.idForCode(PLAN_NUMBER_60));
                assertNotNull(PLAN_CREATION_FAILED + PLAN_NAME_40, testEnvBuilder.idForCode(PLAN_NUMBER_40));
                assertNotNull(PLAN_CREATION_FAILED + PLAN_NAME_20, testEnvBuilder.idForCode(PLAN_NUMBER_20));
            });
        }

    }

    protected Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean useCode, String description,
                                             boolean global, Integer categoryId, Integer allowAsset,
                                             boolean allowDecimal, String metaFieldValue, boolean isPlan) {
        logger.debug("creating product {}", code);
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .useExactCode(useCode)
                .withDescription(description)
                .withType(categoryId)
                .withZeroPrice("0.00")
                .global(global)
                .allowDecimal(allowDecimal)
                .withAssetManagementEnabled(allowAsset)
                .isPlan(isPlan)
                .build();
    }

    protected Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name,
                                                 Integer... paymentMethodTypeId) {
        Map<String, DataType> fieldsAdennetIndividual = new Hashtable();
        fieldsAdennetIndividual.put("First Name", DataType.STRING);
        fieldsAdennetIndividual.put("Last Name", DataType.STRING);
        fieldsAdennetIndividual.put("City", DataType.STRING);
        fieldsAdennetIndividual.put("State", DataType.STRING);
        fieldsAdennetIndividual.put("Postal Code", DataType.STRING);
        fieldsAdennetIndividual.put("Country", DataType.STRING);
        fieldsAdennetIndividual.put("Email Address", DataType.STRING);
        fieldsAdennetIndividual.put("Contact Number", DataType.STRING);

        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api).withName(name).withPaymentMethodTypeIds(paymentMethodTypeId)
                .useExactDescription(true).addAccountInformationType(CONTACT_INFORMATION, fieldsAdennetIndividual)
                .build();

        return accountTypeWS.getId();
    }

    protected Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global,
                                              ItemBuilder.CategoryType categoryType, Integer allowAssetManagement, Boolean useExactCode) {
        return envBuilder.itemBuilder(api).itemType().withCode(code).withCategoryType(categoryType).global(global)
                .allowAssetManagement(allowAssetManagement).useExactCode(useExactCode).build();
    }

    protected String getTenDigitNumber() {
        return String.valueOf((long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L);
    }

    protected String getTwoDigitNumber() {
        return String.valueOf((long) Math.floor(Math.random() * 9_0L) + 1_0L);
    }

    protected Integer buildAndPersistCategoryWithAssetMetaFields(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
                                                                 boolean global, ItemBuilder.CategoryType categoryType, Integer allowAssetManagement, Boolean useExactCode,
                                                                 Set<MetaFieldWS> assetMetaFields) {
        return envBuilder.itemBuilder(api).itemType().withCode(code).withCategoryType(categoryType).global(global)
                .allowAssetManagement(allowAssetManagement).useExactCode(useExactCode).withAssetMetaFields(assetMetaFields).build();
    }

    protected Set<MetaFieldWS> createAssetMetaField() {
        Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>();
        logger.debug("Creating metafield!");
        // First Meta Filed
        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setDataType(DataType.STRING);
        metaField.setName(IMSI_NUMBER);
        metaField.setDisabled(false);
        metaField.setDisplayOrder(1);
        metaField.setEntityId(PRANCING_PONY);
        metaField.setEntityType(EntityType.ASSET);
        metaField.setMandatory(true);
        metaField.setPrimary(false);
        api.createMetaField(metaField);
        metaFields.add(metaField);
        return metaFields;
    }

    protected void setPlanLevelMetaField(Integer planId, String name, String value) {
        //logger.debug("setting the plan level metafields for plan {}", planId);
        PlanWS plan = api.getPlanWS(planId);
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
        Arrays.asList(plan.getMetaFields()).forEach(mf -> {
            if (mf.getFieldName().equals(name)) {
                mf.setValue(value);
                values.add(mf);
            }
        });
        values.forEach(mfValue -> mfValue.setEntityId(api.getCallerCompanyId()));
        plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updatePlan(plan);
    }

    protected Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value = new MetaFieldBuilder().name(name).dataType(dataType).entityType(entityType).primary(true).build();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
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

    protected Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String description,
                                                 boolean global, Integer categoryId, String flatPrice, boolean allowDecimal, Integer allowAssets, boolean isPlan) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withType(categoryId).withFlatPrice(flatPrice)
                .global(global).useExactCode(true).allowDecimal(allowDecimal).withAssetManagementEnabled(allowAssets).build();
    }

    protected PlanItemWS buildPlanItem(JbillingAPI api, Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {

        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(new PriceModelWS(PriceModelStrategy.ZERO.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .addModel(new Date(01 / 01 / 1970), new PriceModelWS(PriceModelStrategy.ZERO.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .withBundledPeriodId(periodId)
                .withBundledQuantity(quantity)
                .build();
    }

    protected Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
                                          Integer periodId, Integer itemId, List<MetaFieldValueWS> metaFieldValues, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withPlanItems(Arrays.asList(planItems))
                .withMetaFields(metaFieldValues)
                .build().getId();
    }

    protected UserWS getAdennetTestUserWS(TestEnvironmentBuilder envBuilder, String userName) {

        Map<String, Object> fieldsAdennetCustomerDetails = new Hashtable();
        fieldsAdennetCustomerDetails.put("First Name", "Adennet_1");
        fieldsAdennetCustomerDetails.put("Last Name", "jun15");
        fieldsAdennetCustomerDetails.put("City", "pune");
        fieldsAdennetCustomerDetails.put("State", "MH");
        fieldsAdennetCustomerDetails.put("Contact Number", "11001100110");

        UserWS newUser = envBuilder.customerBuilder(api).withUsername(userName)
                .withCurrency(CURRENCY_YE)
                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
                .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME))
                .withAITGroup(CUSTOMER_DETAILS, fieldsAdennetCustomerDetails).build();

        newUser = api.getUserWS(newUser.getId());
        return newUser;
    }

    protected Integer getAccountIdForName(TestEnvironment testEnvironment, String accountName) {
        Integer accountTypeId = testEnvironment.idForCode(accountName);
        if (null == accountTypeId) {
            AccountTypeWS[] allAccountTypes = api.getAllAccountTypes();
            logger.debug("AdennetBaseConfiguration.getAccountIdForName===accountName===={}");
            for (AccountTypeWS accountTypeWS : allAccountTypes) {
                String content = accountTypeWS.getDescription(api.getCallerLanguageId()).getContent();
                if (content.equalsIgnoreCase(accountName)) {
                    accountTypeId = accountTypeWS.getId();
                    break;
                }
            }
        }
        logger.debug("AdennetBaseConfiguration.getAccountIdForName====account type id ===" + accountTypeId);
        return accountTypeId;
    }

    public Integer getDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public Calendar getDate(Integer addMonths, Integer dayOfMonth) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, addMonths);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return calendar;
    }

    public Date getLocalDateAsDate(LocalDate localDate) {
        return (null != localDate) ? (Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())) : null;
    }

    protected void configureAdennetExternalConfigurationTask(ConfigurationBuilder confBuilder) {

        Hashtable<String, String> pluginParameters = new Hashtable<>();
        pluginParameters.put("adennet_sim_price_id", getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_SIM_CARD_FEES).toString());
        pluginParameters.put("adennet_add_on_product_id", getItemIdByCode(testBuilder.getTestEnvironment(), ADD_ON_PRODUCTS).toString());
        pluginParameters.put("adennet_downgrade_fee_id", getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_DOWNGRADE_FEES).toString());
        pluginParameters.put("billinghub_usage_management_url", USAGE_MANAGEMENT_SERVICE);

        pluginParameters.put("image_server_address", "127.0.0.1");
        pluginParameters.put("image_server_username", "dummy");
        pluginParameters.put("image_server_password", "fake");
        pluginParameters.put("image_server_folder_location", "/temp");

        int processingOrder = randomLong(1000L, 10000L).intValue();

        confBuilder.addPluginWithParameters(AdennetExternalConfigurationTask.class.getName(), pluginParameters)
                .withProcessingOrder(AdennetExternalConfigurationTask.class.getName(), processingOrder);
    }

    protected void configureAllAdennetPlugins(TestEnvironmentBuilder envBuilder) {
        ConfigurationBuilder configurationBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
        configureAdennetExternalConfigurationTask(configurationBuilder);
        //TODO Configure release asset on marked finished
        configurationBuilder.build();
    }

    protected RechargeRequestWS getRechargeRequestWSForTopUp(Integer userId, BigDecimal amount, OffsetDateTime rechargeTime, String doneBy, String source) {
        RechargeRequestWS rechargeRequestWS = new RechargeRequestWS();
        rechargeRequestWS.setEntityId(1);
        rechargeRequestWS.setUserId(userId);
        rechargeRequestWS.setRechargeAmount(amount);
        rechargeRequestWS.setRechargeDateTime(rechargeTime.toString());
        rechargeRequestWS.setRechargedBy(doneBy);
        rechargeRequestWS.setSource(source);
        return rechargeRequestWS;
    }

    protected RechargeRequestWS getRechargeRequestWSForNewUser(Integer userId, String subscriberNumber, PrimaryPlanWS primaryPlanWS, List<FeeWS> feesWSList,
                                                               List<AddOnProductWS> addOnProductWSList, boolean activeNow,
                                                               BigDecimal amount, OffsetDateTime rechargeTime, String doneBy, String source) {
        RechargeRequestWS rechargeRequestWS = new RechargeRequestWS();
        rechargeRequestWS.setEntityId(1);
        rechargeRequestWS.setUserId(userId);
        rechargeRequestWS.setSubscriberNumber(subscriberNumber);
        rechargeRequestWS.setPrimaryPlan(primaryPlanWS);
        rechargeRequestWS.setFees(feesWSList);
        rechargeRequestWS.setAddOnProducts(addOnProductWSList);
        rechargeRequestWS.setRechargeAmount(amount);
        rechargeRequestWS.setActivatePrimaryPlanImmediately(activeNow);
        rechargeRequestWS.setRechargeDateTime(rechargeTime.toString());
        rechargeRequestWS.setRechargedBy(doneBy);
        rechargeRequestWS.setSource(source);
        return rechargeRequestWS;
    }
    protected BalanceResponseWS getWalletBalance(Integer userId) {
        String url = USAGE_MANAGEMENT_SERVICE+"/wallet/"+userId+"/balance";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<BalanceResponseWS> responseEntity = externalClient.exchange(url,
                HttpMethod.GET, request, BalanceResponseWS.class);
        BalanceResponseWS balanceResponseWS = responseEntity.getBody();
        assertNotNull("Balance response should not be null", balanceResponseWS);
        return balanceResponseWS;
    }

    protected TransactionResponseWS rechargeTopUp(RechargeRequestWS rechargeRequestWS) {
        TransactionResponseWS transactionResponseWS = null;
        try {
            String urlUsageManagementService = USAGE_MANAGEMENT_SERVICE + "/recharge";
            ResponseEntity<TransactionResponseWS> result = externalClient.postForEntity(urlUsageManagementService, rechargeRequestWS, TransactionResponseWS.class);
            transactionResponseWS = result.getBody();
            assertNotNull("TransactionResponseWS should not be null", transactionResponseWS);
        } catch (Exception exception) {
            logger.error("Recharge failed---" + rechargeRequestWS.toString(), exception);
        }
        return transactionResponseWS;
    }

    protected String doRecharge(RechargeRequestWS rechargeRequestWS) {
        try {
            String urlGetUserWS = JBILLING_API_ADENNET +"/recharge";
            String url = urlGetUserWS;
            String authStr = "sysadmin;1:123qwe";
            String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<RechargeRequestWS> request = new HttpEntity<>(rechargeRequestWS, headers);
            return externalClient.postForObject(url, request, String.class);
        } catch (HttpClientErrorException exception) {
            logger.error("postAndUpdateCreateOrder failed for errorResponse={}", exception);
        }
        return null;
    }

    protected Integer buildAndPersistAssetWithIMSINumber(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId,
                                                         String phoneNumber, String code, String serviceId) {
        ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
        Integer assetStatusId = itemTypeWS
                .getAssetStatuses()
                .stream()
                .filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription().equals("Available"))
                .collect(Collectors.toList()).get(0).getId();

        MetaFieldValueWS value = new MetaFieldValueWS("IMSI Number", null, DataType.STRING, false, phoneNumber);
        value.setStringValue(serviceId);

        List<MetaFieldValueWS> metaFieldValueWS = new ArrayList();
        metaFieldValueWS.add(value);

        return envBuilder.assetBuilder(api).withItemId(itemId).withAssetStatusId(assetStatusId).global(true)
                .withMetafields(metaFieldValueWS).withIdentifier(phoneNumber).withCode(code).build();
    }

    protected PrimaryPlanWS getPrimaryPlanWS
            (Integer planId, String description, String usageQuota, Integer validityInDays, String planPrice) {
        PrimaryPlanWS primaryPlanWS = new PrimaryPlanWS();
        primaryPlanWS.setId(planId);
        primaryPlanWS.setDescription(description);
        primaryPlanWS.setUsageQuota(Integer.valueOf(usageQuota));
        primaryPlanWS.setValidityInDays(validityInDays);
        primaryPlanWS.setPrice(new BigDecimal(planPrice));
        return primaryPlanWS;
    }

    protected List<AddOnProductWS> getAddOnProductWSList(Integer[] ids) {
        List<AddOnProductWS> addOnProductList = new ArrayList<>();

        for (Integer id : ids) {
            AddOnProductWS addOnProductWS = new AddOnProductWS();
            ItemDTOEx item = api.getItem(id, null, null);
            addOnProductWS.setId(id);
            addOnProductWS.setName(item.getDescription());
            addOnProductWS.setPrice(item.getPriceAsDecimal());
            addOnProductList.add(addOnProductWS);
        }
        return addOnProductList;
    }

    protected List<FeeWS> getFeesWSList(Integer[] ids) {
        List<FeeWS> feesWSList = new ArrayList<>();
        for (Integer id : ids) {
            FeeWS feeWS = new FeeWS();
            ItemDTOEx item = api.getItem(id, null, null);
            feeWS.setId(id);
            feeWS.setDescription(item.getDescription());
            feeWS.setAmount(item.getPriceAsDecimal());
            feesWSList.add(feeWS);
        }
        return feesWSList;
    }

    protected void clearTestDataForUser(Integer userId) {

        Integer[] allPayments = api.getPaymentsByUserId(userId);
        if (ArrayUtils.isNotEmpty(allPayments)) {
            Arrays.stream(allPayments).sorted(Comparator.reverseOrder()).forEach(api::removeAllPaymentLinks);
        }
        if (ArrayUtils.isNotEmpty(allPayments)) {
            Arrays.stream(allPayments).sorted(Comparator.reverseOrder()).forEach(api::deletePayment);
        }
        Integer[] allInvoices = api.getAllInvoices(userId);
        List<Date> invoiceDates = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(allInvoices)) {
            Arrays.stream(allInvoices).forEach(i -> {
                invoiceDates.add(api.getInvoiceWS(i).getCreateDatetime());
            });
        }

        if (CollectionUtils.isNotEmpty(invoiceDates)) {
            Date invoiceCreationStartDate = Collections.min(invoiceDates);
            Date invoiceCreationEndDate = Collections.max(invoiceDates);

            CreditNoteInvoiceMapWS[] creditNoteInvoiceMap = api.getCreditNoteInvoiceMaps(invoiceCreationStartDate, invoiceCreationEndDate);
            if (ArrayUtils.isNotEmpty(creditNoteInvoiceMap)) {
                for (CreditNoteInvoiceMapWS creditNoteInvoiceMapWS : creditNoteInvoiceMap) {
                    api.removeCreditNoteLink(creditNoteInvoiceMapWS.getInvoiceId(), creditNoteInvoiceMapWS.getCreditNoteId());
                }
            }
            CreditNoteWS[] allCreditNotes = api.getAllCreditNotes(api.getCallerCompanyId());
            if (ArrayUtils.isNotEmpty(allCreditNotes)) {
                for (CreditNoteWS creditNote : allCreditNotes) {
                    if (userId.equals(creditNote.getUserId())) {
                        api.deleteCreditNote(creditNote.getId());
                    }
                }
            }
        }

        if (ArrayUtils.isNotEmpty(allInvoices)) {
            Arrays.stream(allInvoices).sorted(Comparator.reverseOrder()).forEach(api::deleteInvoice);
        }

        Integer[] allOrders = api.getOrdersByDate(userId, getLocalDateAsDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1)),
                new Date());
        if (ArrayUtils.isNotEmpty(allOrders)) {
            Arrays.stream(allOrders).sorted(Comparator.reverseOrder()).forEach(api::deleteOrder);
        }
        api.deleteUser(userId);
    }

    private Integer getPaymentMethodId(Integer paymentMethodId, String methodName) {
        if (null == paymentMethodId) {
            logger.debug("AdennetBaseConfiguration.getPaymentMethodId===methodName=== {}", methodName);
            PaymentMethodTypeWS[] allPaymentMethodTypes = api.getAllPaymentMethodTypes();
            for (PaymentMethodTypeWS paymentMethodTypeWS : allPaymentMethodTypes) {
                if (paymentMethodTypeWS.getMethodName().equalsIgnoreCase(methodName)) {
                    paymentMethodId = paymentMethodTypeWS.getId();
                    break;
                }
            }
        }
        logger.debug("AdennetBaseConfiguration.getPaymentMethodId===methodId===={}", paymentMethodId);
        return paymentMethodId;
    }

    protected Integer getCategoryIdByName(TestEnvironment testEnvironment, String categoryName) {
        Integer categoryId = testEnvironment.idForCode(categoryName);
        if (null == categoryId) {
            ItemTypeWS[] categoriesByEntityId = api.getAllItemCategoriesByEntityId(api.getCallerCompanyId());
            logger.debug("AdennetBaseConfiguration.getCategoryId====categoryName===={}", categoryName);
            for (ItemTypeWS itemTypeWS : categoriesByEntityId) {
                if (itemTypeWS.getDescription().equalsIgnoreCase(categoryName)) {
                    categoryId = itemTypeWS.getId();
                    break;
                }
            }
        }
        logger.debug("AdennetBaseConfiguration.getCategoryId====id====={}", categoryId);
        return categoryId;
    }

    protected Integer getItemIdByCode(TestEnvironment testEnvironment, String code) {
        Integer itemId = testEnvironment.idForCode(code);
        if (null == itemId) {
            logger.debug("AdennetBaseConfiguration.getItemIdByCode=====code======{}", code);
            itemId = api.getItemID(code);
        }
        logger.debug("AdennetBaseConfiguration.getItemIdByCode====item.id===={}", itemId);
        return itemId;
    }

    protected AdennetPlanWS checkMethodNotAllowedResponseForGetPlanDetailsApi(Integer userId, String subscribernumber) {
        try {
            String url = JBILLING_API_ADENNET + "/users/";
            String authStr = "sysadmin;1:123qwe";
            String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<AdennetPlanWS> adennetPlanWSResponseEntity = externalClient.exchange(url + userId + "/plan/" + subscribernumber, HttpMethod.PUT, request, AdennetPlanWS.class);
            return adennetPlanWSResponseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            logger.error("getPlanDetails failed for userId={} and subscriberNumber={}, errorResponse={}", userId, subscribernumber, exception.getResponseBodyAsString());
        }
        return null;
    }

    protected Integer checkMethodNotAllowedResponseForChangePlanApi(PlanChangeRequestWS planChangeRequestWS) {
        try {
            String url = JBILLING_API_ADENNET+"/users/plan";
            String authStr = "sysadmin;1:123qwe";
            String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<PlanChangeRequestWS> request = new HttpEntity<>(planChangeRequestWS, headers);
            externalClient.exchange(url, HttpMethod.POST, request, Void.class);
            return 1;
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            logger.error("changePlan failed for userId={} and subscriberNumber={}, errorResponse={}", planChangeRequestWS.getUserId(), planChangeRequestWS.getSubscriptions().get(0).getNumber(), exception.getResponseBodyAsString());
        }
        return null;
    }

    protected Integer changePlan(PlanChangeRequestWS planChangeRequestWS) {
        try {
            String url = JBILLING_API_ADENNET + "/users/plan";
            String authStr = "sysadmin;1:123qwe";
            String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<PlanChangeRequestWS> request = new HttpEntity<>(planChangeRequestWS, headers);
            externalClient.exchange(url, HttpMethod.PUT, request, Void.class);
            return 1;
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            logger.error("changePlan failed for userId={} and subscriberNumber={}, errorResponse={}", planChangeRequestWS.getUserId(), planChangeRequestWS.getSubscriptions().get(0).getNumber(), exception.getResponseBodyAsString());
        }
        return null;
    }

    protected AdennetPlanWS getPlanDetailsByUserIdAndSubscriberNumber(Integer userId, String subscribernumber) {
        try {
            String url = JBILLING_API_ADENNET + "/users/";
            String authStr = "sysadmin;1:123qwe";
            String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<AdennetPlanWS> adennetPlanWSResponseEntity = externalClient.exchange(url + userId + "/plan/" + subscribernumber, HttpMethod.GET, request, AdennetPlanWS.class);
            return adennetPlanWSResponseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            logger.error("getPlanDetails failed for userId={} and subscriberNumber={}, errorResponse={}", userId, subscribernumber, exception.getResponseBodyAsString());
        }
        return null;
    }

    protected AdennetPlanWS getPlanDetailsByUserId(Integer userId) {
        String url = JBILLING_API_ADENNET + "/users/";
        String authStr = "sysadmin;1:123qwe";
        String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Basic " + base64Creds);
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<AdennetPlanWS> adennetPlanWSResponseEntity = externalClient.exchange(url + userId + "/plan", HttpMethod.GET, request, AdennetPlanWS.class);
        return adennetPlanWSResponseEntity.getBody();
    }

    protected PlanChangeRequestWS getPlanChangeRequestWS(Integer userId, String startDate, String endDate, SubscriptionWS subscriptionWS) {
        PlanChangeRequestWS planChangeRequestWS = new PlanChangeRequestWS();
        planChangeRequestWS.setUserId(userId);
        planChangeRequestWS.setSubscriptions(new ArrayList<>());
        planChangeRequestWS.getSubscriptions().add(subscriptionWS);
        planChangeRequestWS.setStartDate(startDate);
        planChangeRequestWS.setEndDate(endDate);
        return planChangeRequestWS;
    }

    protected PlanDescriptionWS getPlanDescriptionWS(Integer planId, String planDescription, Integer ValidityInDays,
                                                     Integer usageQuota, BigDecimal price) {
        PlanDescriptionWS planDescriptionWS = new PlanDescriptionWS();
        planDescriptionWS.setId(planId);
        planDescriptionWS.setDescription(planDescription);
        planDescriptionWS.setValidityInDays(ValidityInDays);
        planDescriptionWS.setUsageQuota(usageQuota);
        planDescriptionWS.setPrice(price);
        return planDescriptionWS;
    }
    public String getValueFromExternalConfigParams(String task, ParameterDescription param, JbillingAPI api) {
        Integer entityId = api.getCallerCompanyId();
        logger.debug("Getting parameter info for task : {} and param : ",task, param.getName());
        try {
            PluggableTaskWS[] pluginsWS = api.getPluginsWS(api.getCallerCompanyId(), task);
            Map<String, String> parameters = pluginsWS[0].getParameters();
            return parameters.get(param.getName());

        } catch (Exception exception) {
            logger.error("loadExternalServiceConfigParams failed for entity={}", entityId, task);
            throw new SessionInternalError("loadExternalServiceConfigParams failed for entity " +
                    entityId, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @AfterTest(alwaysRun = true)
    public void tearDown() {
        shouldExecute = false;
        if (shouldExecute) {
            testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
            testBuilder.removeEntitiesCreatedOnJBilling();
        }

    }

}
