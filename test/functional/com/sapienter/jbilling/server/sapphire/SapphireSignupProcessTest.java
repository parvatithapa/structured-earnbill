package com.sapienter.jbilling.server.sapphire;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.signup.PaymentRequestWS;
import com.sapienter.jbilling.server.process.signup.PaymentResult;
import com.sapienter.jbilling.server.process.signup.SignupRequestWS;
import com.sapienter.jbilling.server.process.signup.SignupResponseWS;
import com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus;
import com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants;
import com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupProcessTask;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;



@Test(groups = { "sapphire" }, testName = "sapphireSignup")
@ContextConfiguration(classes = SapphireTestConfig.class)
public class SapphireSignupProcessTest extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer ENTITY_ID                                     = 1;
    private static final Integer CC_PM_ID                                      = 5;
    private static final String ACCOUNT_NAME                                   = "Sapphire Signup Account";
    private static final String PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME              = "plan_code_account_type_map";
    private static final String ASSET_CATEGORY_CODE                            = "Sapphire Asset Category";
    private static final String ADDON_CATEGORY_CODE                            = "Sapphire AddOnProduct Category";
    private static final String ASSET_CATEGORY_CODE2                           = "Sapphire Asset Category 2";
    private static final String PLAN_CATEGORY_CODE                             = "Sapphire Plans Category";
    private static final String ASSET_PRODUCT_CODE                             = "Asset-1";
    private static final String PLAN_PRODUCT_CODE                              = "Fee-2";
    private static final String PLAN_PRODUCT_CODE2                             = "Fee-2";
    private static final String PLAN_PRODUCT_CODE3                             = "Fee-3";
    private static final String PLAN_PRODUCT_CODE4                             = "Fee-4";
    private static final String ADDON_PRODUCT_CODE                             = "Add-1";
    private static final String ASSET_PRODUCT_CODE2                            = "Add-2";
    private static final String TEST_USER_1                                    = "User-PlanCode-1"+ UUID.randomUUID().toString();;
    private static final String TEST_USER_2                                    = "User-AddOnPro-1"+ UUID.randomUUID().toString();;
    private static final String TEST_USER_3                                    = "Test-User-3"+ UUID.randomUUID().toString();;
    private static final String TEST_USER_4                                    = "Test-User-4"+ UUID.randomUUID().toString();;
    private static final String PLAN_CODE_ACCOUNT_TYPE_FIELD_NAME              = "Plan Code Account Type Map";
    private static final String PLAN_CODE                                      = "Plan 1";
    private static final String PLAN_CODE2                                     = "Plan 2";
    private static final String CREDIT_CARD_NAME                               = "test_name";
    private static final String CREDIT_CARD_NUMBER                             = "************1152  ";
    private static final String USER_CREATION_MSG                              = "User can not be null";
    private static final String ORDER_CREATION_MSG                             = "Order not created";
    private static final String PAYMENT_AMOUNT                                 = "1000";
    private static final String PAYMENT_CREATION_MSG                           = "Payment can not be null";
    private static final String PAYMENT_AMOUNT_MSG                             = "Correct payment amount ";
    private static final String INVOICE_AMOUNT_MSG                             = "Correct invoice amount ";
    private static final String PAYMENT_RESULT_MSG                             = "Payment result should be successful ";
    private static final String EXPECTED_PAYMENT_AMOUNT                        = "1000.0000000000";
    private static final String TRANSACTION_ID                                 = "fa340b28-6d05-4bd7-8985-b63c6fa54ea9";
    private static final String FOUR_HUNDRED                                   = "400";
    private static final String CUSTOMER_PROVISIONING_STATUS_ENUM_NAME         = "Customer Provisioning Status";
    private static final String ONE                                            = "1";
    private static final Map<String, String> PLAN_CODE_ACCOUNT_TYPE_MAP;
    private TestBuilder testBuilder;
    private EnvironmentHelper envHelper;
    private JbillingAPI api;
    private ConfigurationBuilder configurationBuilder;
    private Integer proId;
    private Integer proId1;


    @Resource(name = "sapphireJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
            logger.debug("SapphireSignUpProcessTest.getTestEnvironment() {}",api );
        });
    }

    static {
        PLAN_CODE_ACCOUNT_TYPE_MAP = new LinkedHashMap<>();
        PLAN_CODE_ACCOUNT_TYPE_MAP.put("id", "SERIAL NOT NUll");
        PLAN_CODE_ACCOUNT_TYPE_MAP.put("plan_code", "VARCHAR(255)");
        PLAN_CODE_ACCOUNT_TYPE_MAP.put("account_type_id", "VARCHAR(255)");
        PLAN_CODE_ACCOUNT_TYPE_MAP.put("entity_id", "VARCHAR(255)");
        PLAN_CODE_ACCOUNT_TYPE_MAP.put("PRIMARY KEY", " ( id ) ");
    }

    @BeforeClass
    public void beforeClass() {
        logger.info("Creating Table {} with columns {}", PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME, PLAN_CODE_ACCOUNT_TYPE_MAP);
        createTable(PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME, PLAN_CODE_ACCOUNT_TYPE_MAP);
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            logger.debug("SapphireSignUpProcessTest.beforeClass() {}",api);

            configurationBuilder = envBuilder.configurationBuilder(api);
            if(ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(), SapphireSignupProcessTask.class.getName()))) {
                Hashtable<String, String> pluginParameters = new Hashtable<>();
                pluginParameters.put(SapphireSignupConstants.MAIN_SUBCRIPTION_ID_PARAM_NAME, "2");
                pluginParameters.put(SapphireSignupConstants.ORDER_PERIOD_ID_PARAM_NAME, "2");
                pluginParameters.put("customer provisioning metafield name", CUSTOMER_PROVISIONING_STATUS_ENUM_NAME);
                pluginParameters.put("AIT group name", "Contact Info");
                configurationBuilder.addPluginWithParameters(SapphireSignupProcessTask.class.getName(), pluginParameters);
            }
            configurationBuilder.build();

            // Creating account type
            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, "Contact Info",
                    Collections.singletonMap("firstName", DataType.STRING), CC_PM_ID);
            // Creating Customer Provisioning Status Enumeration.
            buildAndPersistEnumeration(testBuilder, Arrays.stream(UserProvisioninigStatus.values())
                    .map(UserProvisioninigStatus::getStatus)
                    .map(EnumerationValueWS::new)
                    .collect(Collectors.toList()),
                    CUSTOMER_PROVISIONING_STATUS_ENUM_NAME);

            // Setting Company Level Meta Fields
            buildAndPersistMetafield(testBuilder, CUSTOMER_PROVISIONING_STATUS_ENUM_NAME, DataType.ENUMERATION, EntityType.CUSTOMER);

            // Setting Company Level Meta Fields
            buildAndPersistMetafield(testBuilder, PLAN_CODE_ACCOUNT_TYPE_FIELD_NAME, DataType.STRING, EntityType.COMPANY);

            // Creating Asset category
            buildAndPersistCategory(envBuilder, api, ASSET_CATEGORY_CODE, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,1);
            buildAndPersistCategory(envBuilder, api, ASSET_CATEGORY_CODE2, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,1);

            // Creating Plan category
            buildAndPersistCategory(envBuilder, api, PLAN_CATEGORY_CODE, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0);

            //creating Add On Product category
            buildAndPersistCategory(envBuilder, api, ADDON_CATEGORY_CODE, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,0);

            Date epochDate = com.sapienter.jbilling.server.util.Util.getEpochDate();
            // Creating asset Product
            proId= buildAndPersistProductWithPriceModel(envBuilder,1, api, ASSET_PRODUCT_CODE, false, envBuilder.idForCode(ASSET_CATEGORY_CODE), true, getFlatPriceModel("225"),epochDate);
            proId1=buildAndPersistProductWithPriceModel(envBuilder,1, api, ASSET_PRODUCT_CODE2, false, envBuilder.idForCode(ASSET_CATEGORY_CODE2), true, getFlatPriceModel("277"),epochDate);

            buildAndPersistProductWithPriceModel(envBuilder,0, api, PLAN_PRODUCT_CODE, false, envBuilder.idForCode(ASSET_CATEGORY_CODE), true, getFlatPriceModel(FOUR_HUNDRED),epochDate);
            buildAndPersistProductWithPriceModel(envBuilder,0, api, PLAN_PRODUCT_CODE2, false, envBuilder.idForCode(PLAN_CATEGORY_CODE), true, getFlatPriceModel(FOUR_HUNDRED),epochDate);
            buildAndPersistProductWithPriceModel(envBuilder,0, api, PLAN_PRODUCT_CODE3, false, envBuilder.idForCode(PLAN_CATEGORY_CODE), true, getFlatPriceModel(FOUR_HUNDRED),epochDate);
            buildAndPersistProductWithPriceModel(envBuilder,0, api, PLAN_PRODUCT_CODE4, false, envBuilder.idForCode(ASSET_CATEGORY_CODE2), true, getFlatPriceModel(FOUR_HUNDRED),epochDate);
            buildAndPersistProductWithPriceModel(envBuilder,0, api, ADDON_PRODUCT_CODE, false, envBuilder.idForCode(ADDON_CATEGORY_CODE), true, getFlatPriceModel("277"),epochDate);

            //Creating Asset
            Integer asset1 = buildAndPersistAsset(envBuilder, envBuilder.idForCode(ASSET_CATEGORY_CODE),proId, "20051526");
            Map<Integer, Integer> productAssetMap = new HashMap<>();
            productAssetMap.put(proId, asset1);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(proId, BigDecimal.ONE);

            Integer asset2 = buildAndPersistAsset(envBuilder, envBuilder.idForCode(ASSET_CATEGORY_CODE2),proId1, "20051522");
            Map<Integer, Integer> productAssetMap1 = new HashMap<>();
            productAssetMap1.put(proId1, asset2);
            Map<Integer, BigDecimal> productQuantityMap1 = new HashMap<>();
            productQuantityMap1.put(proId1, BigDecimal.ONE);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2018);
            pricingDate.set(Calendar.MONTH, 11);
            pricingDate.set(Calendar.DAY_OF_MONTH, 27);

            Integer orderPeriodOneTime = envHelper.getOrderPeriodOneTime(api);
            //Creating Plan
            PlanItemWS newPlanItem = buildPlanItem(api,proId, envHelper.getOrderPeriodMonth(api), ONE, "250", pricingDate.getTime());
            PlanItemWS newPlanItem1 = buildPlanItem(api,proId1, envHelper.getOrderPeriodMonth(api), ONE, "200", pricingDate.getTime());
            PlanItemWS newPlanItem2 = buildPlanItem(api, envBuilder.env().idForCode(PLAN_PRODUCT_CODE2), orderPeriodOneTime, ONE, FOUR_HUNDRED,pricingDate.getTime());
            PlanItemWS newPlanItem3 = buildPlanItem(api, envBuilder.env().idForCode(PLAN_PRODUCT_CODE3), orderPeriodOneTime, ONE, FOUR_HUNDRED,pricingDate.getTime());

            buildAndPersistPlan(envBuilder, PLAN_CODE, PLAN_CODE, envHelper.getOrderPeriodMonth(api),
                    envBuilder.idForCode(PLAN_PRODUCT_CODE),newPlanItem, newPlanItem2);
            buildAndPersistPlan(envBuilder, PLAN_CODE2, PLAN_CODE2, envHelper.getOrderPeriodMonth(api),
                    envBuilder.idForCode(PLAN_PRODUCT_CODE4),newPlanItem1, newPlanItem3);

            setCompanyLevelMetaField(testBuilder.getTestEnvironment());

            setUpPlanCodeAccountTypeMapTable(testBuilder.getTestEnvironment());

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
            assertNotNull("Asset Category Creation Failed", testEnvBuilder.idForCode(ASSET_CATEGORY_CODE));
            assertNotNull("Fees Product Creation Failed", testEnvBuilder.idForCode(ASSET_PRODUCT_CODE));
            assertNotNull("Plan Product Creation Failed", testEnvBuilder.idForCode(PLAN_PRODUCT_CODE));
            assertNotNull("Plan2 product Creation Failed", testEnvBuilder.idForCode(PLAN_PRODUCT_CODE2));
            assertNotNull("Plan3 product Creation Failed", testEnvBuilder.idForCode(PLAN_PRODUCT_CODE3));
            assertNotNull("Plan4 product Creation Failed", testEnvBuilder.idForCode(PLAN_PRODUCT_CODE4));
        });
    }

    private PriceModelWS getFlatPriceModel(String value) {
        return new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(value), 1);
    }

    @Test(priority =1, enabled = true)
    public void test01SapphireSignupAPIUsingPlanCode() {
        SignupRequestWS signupRequest = new SignupRequestWS();
        testBuilder.given(envBuilder -> {
            UserWS newUser = new UserWS();
            TestEnvironment testEnvBuilder = testBuilder.getTestEnvironment();
            Integer  accountTypeId = testEnvBuilder.idForCode(ACCOUNT_NAME);
            Date nextInvoiceDate = new Date();

            MainSubscriptionWS billing = new MainSubscriptionWS();
            billing.setPeriodId(1);
            billing.setNextInvoiceDayOfPeriod(1);
            newUser.setMainSubscription(billing);
            newUser.setNextInvoiceDate(nextInvoiceDate );

            PaymentRequestWS paymentReqWS = new PaymentRequestWS();
            paymentReqWS.setAmount(PAYMENT_AMOUNT);
            paymentReqWS.setTransactionId(TRANSACTION_ID);
            paymentReqWS.setPaymentResult(PaymentResult.SUCCESS);

            newUser.setUserName(TEST_USER_1);
            newUser.setAccountTypeId(accountTypeId);

            //getting the plan code
            Integer  planCode = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE4);
            ItemDTOEx item= api.getItem(planCode, null, null);

            //Payment Information
            PaymentInformationWS paymentInfromationWS = new PaymentInformationWS();
            List<MetaFieldValueWS> metaFields = new ArrayList<>();
            paymentInfromationWS.setProcessingOrder(1);
            paymentInfromationWS.setPaymentMethodTypeId(CC_PM_ID);
            addMetafield(Constants.METAFIELD_NAME_CC_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, CREDIT_CARD_NAME.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_NUMBER, false, true, DataType.CHAR, 2, CREDIT_CARD_NUMBER.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_EXPIRY_DATE, false, true, DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT)
                    .print(getExpiryDate().getTime())).toCharArray(), metaFields);

            paymentInfromationWS.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
            newUser.getPaymentInstruments().add(paymentInfromationWS);

            //Process Sign Up API request
            signupRequest.setUser(newUser);
            signupRequest.setPlanCode(item.getNumber());
            signupRequest.setPaymentRequest(paymentReqWS);

        }).validate((testEnv, testEnvBuilder) -> {
            //Getting Sapphire Signup process API response
            SignupResponseWS response = api.processSignupRequest(signupRequest);
            assertNotNull(USER_CREATION_MSG,response.getUserId());
            OrderWS orderWS = api.getLatestOrder(response.getUserId());
            assertNotNull(ORDER_CREATION_MSG,orderWS);
            logger.debug("Order Id ::::  ::: ::: {}", orderWS.getId());
            logger.debug("Order Period ::::  ::: {}", orderWS.getPeriod());

            assertNotNull(PAYMENT_CREATION_MSG,response.getPaymentId());
            pauseUntilInvoiceGenerationFinishesForUser(response.getUserId(),10, api);

            PaymentWS paymentWS = api.getPayment(response.getPaymentId());
            assertEquals(PAYMENT_RESULT_MSG, paymentWS.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
            logger.debug("Payment amount  ::::{}  ::: ::: {}", paymentWS.getId(),paymentWS.getAmount());
            logger.debug("payment balance ::::{}  ::: {}", paymentWS.getId(), paymentWS.getBalance());
            assertEquals(PAYMENT_AMOUNT_MSG,EXPECTED_PAYMENT_AMOUNT ,paymentWS.getAmount());
            InvoiceWS invoiceWS = api.getLatestInvoice(response.getUserId());
            assertNotNull("Invoice is not created.",invoiceWS);
            assertEquals(INVOICE_AMOUNT_MSG, "1000.0000000000", invoiceWS.getTotal());
        });
    }

    @Test(priority = 2, enabled = true)
    public void test02SapphireSignupAPIUsingAddOnProductCode() {
        final SignupRequestWS signupRequest = new SignupRequestWS();
        testBuilder.given(envBuilder -> {
            UserWS newUser = new UserWS();
            TestEnvironment testEnvBuilder = testBuilder.getTestEnvironment();
            Integer  accountTypeId = testEnvBuilder.idForCode(ACCOUNT_NAME);
            Date nextInvoiceDate = new Date();

            MainSubscriptionWS billing = new MainSubscriptionWS();
            billing.setPeriodId(1);
            billing.setNextInvoiceDayOfPeriod(1);
            newUser.setMainSubscription(billing);
            newUser.setNextInvoiceDate(nextInvoiceDate );

            PaymentRequestWS paymentReqWS = new PaymentRequestWS();
            paymentReqWS.setAmount(PAYMENT_AMOUNT);
            paymentReqWS.setTransactionId(TRANSACTION_ID);
            paymentReqWS.setPaymentResult(PaymentResult.SUCCESS);

            newUser.setUserName(TEST_USER_2);
            newUser.setAccountTypeId(accountTypeId);

            //getting the plan code
            Integer  planCode = testEnvBuilder.idForCode(ASSET_PRODUCT_CODE);
            ItemDTOEx item= api.getItem(planCode, null, null);

            logger.debug("Item description :::::: {}", item.getDescription());
            logger.debug("Item Number :::::: {}",item.getNumber());
            Integer  planCode2 = testEnvBuilder.idForCode(ADDON_PRODUCT_CODE);
            ItemDTOEx item2= api.getItem(planCode2, null, null);

            String addOn =item2.getNumber();
            String[] ad = new String[1];
            ad[0]=addOn;

            //Payment Information
            PaymentInformationWS paymentInfromationWS = new PaymentInformationWS();
            List<MetaFieldValueWS> metaFields = new ArrayList<>();
            paymentInfromationWS.setProcessingOrder(1);
            paymentInfromationWS.setPaymentMethodTypeId(CC_PM_ID);
            addMetafield(Constants.METAFIELD_NAME_CC_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, CREDIT_CARD_NAME.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_NUMBER, false, true, DataType.CHAR, 2, CREDIT_CARD_NUMBER.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_EXPIRY_DATE, false, true, DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT)
                    .print(getExpiryDate().getTime())).toCharArray(), metaFields);

            paymentInfromationWS.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
            newUser.getPaymentInstruments().add(paymentInfromationWS);
            //Process Sign Up API request
            signupRequest.setUser(newUser);
            signupRequest.setAddonProductCodes(ad);
            signupRequest.setPaymentRequest(paymentReqWS);

        }).validate((testEnv, testEnvBuilder) -> {
            //Getting response from Sapphire Signup process API
            SignupResponseWS response = api.processSignupRequest(signupRequest);
            assertNotNull(USER_CREATION_MSG,response.getUserId());
            OrderWS orderWS = api.getLatestOrder(response.getUserId());
            assertNotNull(ORDER_CREATION_MSG,orderWS);
            logger.debug("Order Id ::: ::: {}", orderWS.getId() );
            logger.debug("Order Period ::::  ::: {}", orderWS.getPeriod());
            assertNotNull(PAYMENT_CREATION_MSG,response.getPaymentId());
            PaymentWS paymentWS = api.getPayment(response.getPaymentId());
            assertEquals(PAYMENT_RESULT_MSG, paymentWS.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
            assertEquals(PAYMENT_AMOUNT_MSG, EXPECTED_PAYMENT_AMOUNT, paymentWS.getAmount());
        });
    }

    @Test(priority = 3, enabled = true)
    public void test03SapphireSignupAPIUsingPlanAndAddOnProductCodes() {
        final SignupRequestWS signupRequest = new SignupRequestWS();
        testBuilder.given(envBuilder -> {
            logger.info("creating test03SapphireSignUpAPIUsingBothCodes ");
            UserWS newUser = new UserWS();
            TestEnvironment testEnvBuilder = testBuilder.getTestEnvironment();
            Integer  accountTypeId = testEnvBuilder.idForCode(ACCOUNT_NAME);
            Date nextInvoiceDate = new Date();

            MainSubscriptionWS billing = new MainSubscriptionWS();
            billing.setPeriodId(1);
            billing.setNextInvoiceDayOfPeriod(1);
            newUser.setMainSubscription(billing);
            newUser.setNextInvoiceDate(nextInvoiceDate );

            PaymentRequestWS paymentReqWS = new PaymentRequestWS();
            paymentReqWS.setAmount(PAYMENT_AMOUNT);
            paymentReqWS.setTransactionId(TRANSACTION_ID);
            paymentReqWS.setPaymentResult(PaymentResult.SUCCESS);

            newUser.setUserName(TEST_USER_3);
            newUser.setAccountTypeId(accountTypeId);

            //getting the plan code
            Integer  planCode = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE);
            ItemDTOEx item= api.getItem(planCode, null, null);

            Integer  planCode2 = testEnvBuilder.idForCode(ADDON_PRODUCT_CODE);
            ItemDTOEx item2= api.getItem(planCode2, null, null);

            String addOn =item2.getNumber();
            String[] addProduct = new String[1];
            addProduct[0]=addOn;
            //Payment Information
            PaymentInformationWS paymentInfromationWS = new PaymentInformationWS();
            List<MetaFieldValueWS> metaFields = new ArrayList<>();
            paymentInfromationWS.setProcessingOrder(1);
            paymentInfromationWS.setPaymentMethodTypeId(CC_PM_ID);
            addMetafield(Constants.METAFIELD_NAME_CC_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, CREDIT_CARD_NAME.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_NUMBER, false, true, DataType.CHAR, 2, CREDIT_CARD_NUMBER.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_EXPIRY_DATE, false, true, DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT)
                    .print(getExpiryDate().getTime())).toCharArray(), metaFields);

            paymentInfromationWS.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
            newUser.getPaymentInstruments().add(paymentInfromationWS);

            //Process Sign Up API request
            signupRequest.setUser(newUser);
            signupRequest.setPlanCode(item.getNumber());
            signupRequest.setAddonProductCodes(addProduct);
            signupRequest.setPaymentRequest(paymentReqWS);

        }).validate((testEnv, testEnvBuilder) -> {
            //Getting Sapphire Signup process API response
            SignupResponseWS response = api.processSignupRequest(signupRequest);
            assertNotNull(USER_CREATION_MSG,response.getUserId());
            OrderWS orderWS = api.getLatestOrder(response.getUserId());
            assertNotNull(ORDER_CREATION_MSG,orderWS);
            logger.debug("Order Id ::::  ::: {}", orderWS.getId());
            logger.debug("Order Period ::::  ::: {}", orderWS.getPeriod());

            assertNotNull(PAYMENT_CREATION_MSG,response.getPaymentId());
            pauseUntilInvoiceGenerationFinishesForUser(response.getUserId(),10, api);
            PaymentWS paymentWS = api.getPayment(response.getPaymentId());
            assertEquals(PAYMENT_RESULT_MSG, paymentWS.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
            assertEquals(PAYMENT_AMOUNT_MSG, EXPECTED_PAYMENT_AMOUNT, paymentWS.getAmount());

            InvoiceWS invoiceWS = api.getLatestInvoice(response.getUserId());
            assertNotNull("Invoice is not created.",invoiceWS);
            assertEquals(INVOICE_AMOUNT_MSG, "1327.0000000000", invoiceWS.getTotal());
        });
    }

    @Test(priority = 4, enabled = true)
    public void test04SapphireSignupAPIWithoutUsingPlanAndAddOnProductCodes() {
        final SignupRequestWS signupRequest = new SignupRequestWS();
        testBuilder.given(envBuilder -> {
            UserWS newUser = new UserWS();
            TestEnvironment testEnvBuilder = testBuilder.getTestEnvironment();
            Integer  accountTypeId = testEnvBuilder.idForCode(ACCOUNT_NAME);
            Date nextInvoiceDate = new Date();

            MainSubscriptionWS billing = new MainSubscriptionWS();
            billing.setPeriodId(1);
            billing.setNextInvoiceDayOfPeriod(1);
            newUser.setMainSubscription(billing);
            newUser.setNextInvoiceDate(nextInvoiceDate );

            PaymentRequestWS paymentReqWS = new PaymentRequestWS();
            paymentReqWS.setAmount(PAYMENT_AMOUNT);
            paymentReqWS.setTransactionId(TRANSACTION_ID);
            paymentReqWS.setPaymentResult(PaymentResult.SUCCESS);

            newUser.setUserName(TEST_USER_4);
            newUser.setAccountTypeId(accountTypeId);

            //getting the plan code
            Integer  planCode = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE);
            ItemDTOEx item= api.getItem(planCode, null, null);
            logger.debug("Item description :::::: {}", item.getDescription());
            String addOn =item.getDescription();
            String[] ad = new String[1];
            ad[0]=addOn;
            //Payment Information
            PaymentInformationWS paymentInfromationWS = new PaymentInformationWS();
            List<MetaFieldValueWS> metaFields = new ArrayList<>();
            paymentInfromationWS.setProcessingOrder(1);
            paymentInfromationWS.setPaymentMethodTypeId(CC_PM_ID);
            addMetafield(Constants.METAFIELD_NAME_CC_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, CREDIT_CARD_NAME.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_NUMBER, false, true, DataType.CHAR, 2, CREDIT_CARD_NUMBER.toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_EXPIRY_DATE, false, true, DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT)
                    .print(getExpiryDate().getTime())).toCharArray(), metaFields);
            paymentInfromationWS.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
            newUser.getPaymentInstruments().add(paymentInfromationWS);
            //Process Sign Up API request
            signupRequest.setUser(newUser);
            signupRequest.setPaymentRequest(paymentReqWS);
        }).validate((testEnv, testEnvBuilder) -> {
            //Getting response from Sapphire Signup process API
            SignupResponseWS response = api.processSignupRequest(signupRequest);
            assertNull("Plan code should be null ", signupRequest.getPlanCode());
            assertNull("Add on product code should be null", signupRequest.getPlanCode());
            assertEquals("[planCode and addonProductCodes are not present in request, atleast one parameter is required]", response.getErrorResponse().toString());
        });
    }

    @AfterClass
    public void afterClass() {
        logger.info("Dropping {}", PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME);
        dropTable(PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME);
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        testBuilder = null;
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value =  new MetaFieldBuilder()
        .name(name)
        .dataType(dataType)
        .entityType(entityType)
        .primary(true)
        .build();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
    }

    private Integer buildAndPersistEnumeration (TestBuilder testBuilder, List<EnumerationValueWS> values, String name) {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        EnumerationWS enUmeration = new EnumerationWS();

        enUmeration.setValues(values);
        enUmeration.setName(name);
        enUmeration.setEntityId(api.getCallerCompanyId());

        Integer enumId =  api.createUpdateEnumeration(enUmeration);
        testBuilder.getTestEnvironment().add(name, enumId, name,  api, TestEntityType.ENUMERATION);
        return enumId;

    }

    private void addMetafield (String metafieldNameCcCardholderName, boolean disabled, boolean mandatory,
            DataType string, int displayOrder, Object value, List<MetaFieldValueWS> metaFields) {
        MetaFieldValueWS metaFieldValue = new MetaFieldValueWS();
        metaFieldValue.setFieldName(metafieldNameCcCardholderName);
        metaFieldValue.getMetaField().setDisabled(disabled);
        metaFieldValue.getMetaField().setMandatory(mandatory);
        metaFieldValue.getMetaField().setDataType(string);
        metaFieldValue.getMetaField().setDisplayOrder(displayOrder);
        metaFieldValue.setValue(value);
        metaFields.add(metaFieldValue);
    }

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));
        values.add(new MetaFieldValueWS(PLAN_CODE_ACCOUNT_TYPE_FIELD_NAME, null, DataType.STRING, true,
                String.valueOf(PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME )));
        int entityId = api.getCallerCompanyId();
        logger.debug("Company Id {}", entityId);
        values.forEach(value -> {
            value.setEntityId(entityId);
        });
        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updateCompany(company);
    }

    private void setUpPlanCodeAccountTypeMapTable(TestEnvironment testEnvBuilder ) {
        String accountTypeId = String.valueOf(testEnvBuilder.idForCode(ACCOUNT_NAME));
        Integer  feesPlanCode = testEnvBuilder.idForCode(ASSET_PRODUCT_CODE);
        Integer basicPlanCode = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE);
        Integer basicPlanCode2 = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE2);
        Integer basicPlanCode3 = testEnvBuilder.idForCode(ADDON_PRODUCT_CODE);
        Integer basicPlanCode4 = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE3);
        Integer basicPlanCode5 = testEnvBuilder.idForCode(PLAN_PRODUCT_CODE4);

        ItemDTOEx item= api.getItem(feesPlanCode, null, null);
        ItemDTOEx item1= api.getItem(basicPlanCode, null, null);
        ItemDTOEx item2= api.getItem(basicPlanCode2, null, null);
        ItemDTOEx item3= api.getItem(basicPlanCode3, null, null);
        ItemDTOEx item4= api.getItem(basicPlanCode4, null, null);
        ItemDTOEx item5= api.getItem(basicPlanCode5, null, null);

        // inserting in coming row
        jdbcTemplate.update("INSERT INTO plan_code_account_type_map (plan_code,account_type_id, entity_id) "
                + "VALUES (?, ? , ?)", new Object[] {item.getNumber(),accountTypeId,ENTITY_ID});

        // out going in coming row
        jdbcTemplate.update("INSERT INTO plan_code_account_type_map (plan_code,account_type_id, entity_id) "
                + "VALUES (?, ? , ?)", new Object[] {item1.getNumber(),accountTypeId,ENTITY_ID});

        jdbcTemplate.update("INSERT INTO plan_code_account_type_map (plan_code,account_type_id, entity_id) "
                + "VALUES (?, ? , ?)", new Object[] {item2.getNumber(),accountTypeId,ENTITY_ID});

        jdbcTemplate.update("INSERT INTO plan_code_account_type_map (plan_code,account_type_id, entity_id) "
                + "VALUES (?, ? , ?)", new Object[] {item3.getNumber(),accountTypeId,ENTITY_ID});

        jdbcTemplate.update("INSERT INTO plan_code_account_type_map (plan_code,account_type_id, entity_id) "
                + "VALUES (?, ? , ?)", new Object[] {item4.getNumber(),accountTypeId,ENTITY_ID});
        jdbcTemplate.update("INSERT INTO plan_code_account_type_map (plan_code,account_type_id, entity_id) "
                + "VALUES (?, ? , ?)", new Object[] {item5.getNumber(),accountTypeId,ENTITY_ID});
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

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, String code, String desc,
            Integer periodId, Integer itemId, PlanItemWS... planItems) {
        logger.debug("creating plan {}", code);
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name,
            String accountInformationTypeName, Map<String, DataType> informationTypeMetaFields, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .addAccountInformationType(accountInformationTypeName, informationTypeMetaFields)
                .build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType, Integer allowAssetManagement) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .allowAssetManagement(allowAssetManagement)
                .global(global)
                .build();
    }

    private Integer buildAndPersistProductWithPriceModel(TestEnvironmentBuilder envBuilder,Integer asset, JbillingAPI api, String code,
            boolean global, Integer categoryId, boolean allowDecimal, PriceModelWS priceModelWS, Date date) {
        return envBuilder.itemBuilder(api)
                .item()
                .withAssetManagementEnabled(asset)
                .withCode(code)
                .withType(categoryId)
                .global(global)
                .withDatePriceModel(date, priceModelWS)
                .allowDecimal(allowDecimal)
                .build();
    }

    private Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber) {
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

    private void cleanupTask(List<Integer> paymentIds, List<Integer> invoiceIds, List<Integer> orderIds, List<Integer> userIds) {

        if (null != paymentIds){
            for (Integer paymenId : paymentIds) {
                api.deletePayment(paymenId);
            }
        }
        if (null != orderIds){
            for (Integer orderId : orderIds) {
                OrderWS orderWS = api.getOrder(orderId);
                if(null != orderWS.getChildOrders()){
                    for (OrderWS childOrder : orderWS.getChildOrders()) {
                        api.deleteOrder(childOrder.getId());
                    }
                }
                api.deleteOrder(orderId);
            }
        }
        if (null != invoiceIds){
            for (Integer invoiceId : invoiceIds) {
                api.deleteInvoice(invoiceId);
            }
        }
        if (null != userIds){
            for (Integer userId : userIds) {
                api.deleteUser(userId);
            }
        }
    }

    private List<Integer> getOrderIds(JbillingAPI api , Integer userId) {
        List<Integer> orderIds = new ArrayList<>();
        Integer[] onetimeOrders = api.getOrderByPeriod(userId,1);
        Integer[] monthlyOrders = api.getOrderByPeriod(userId,2);

        if(null != onetimeOrders){
            orderIds.addAll(Arrays.asList(onetimeOrders));
        }
        if(null != monthlyOrders){
            orderIds.addAll(Arrays.asList(monthlyOrders));
        }
        return orderIds;
    }

    /**
     * pauses main thread for given seconds in order to
     * complete invoice generation api execution.
     * @param userId
     * @param seconds
     * @param api
     */
    private void pauseUntilInvoiceGenerationFinishesForUser(Integer userId, long seconds, JbillingAPI api) {
        for (int i = 0; i < seconds; i++) {
            if (null!= api.getLatestInvoice(userId)) {
                return ;
            }
            sleep(1000L);
        }
        throw new RuntimeException("Invoice Generationwait was timeout in " + seconds);
    }

    private Date getExpiryDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(cal.YEAR, 2);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        return cal.getTime();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}