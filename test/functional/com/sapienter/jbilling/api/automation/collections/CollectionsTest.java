package com.sapienter.jbilling.api.automation.collections;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.selenium.sanity.JBillingSanityTest;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Hashtable;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * JBQA-41
 * Test cases for collections and ageing.
 *
 * @author Hristijan Todorovski
 * @author Vojislav Stanojevikj
 * @since 29-JUN-2016
 */

@Test(groups = {"api-automation"}, testName = "CollectionsTest")
public class CollectionsTest {

    private static final Logger logger = LoggerFactory.getLogger(CollectionsTest.class);
    private EnvironmentHelper environmentHelper;
    private TestBuilder testBuilder;

    // Fixed constants for now
    private static final Integer INVOICE_STATUS_UNPAID = Integer.valueOf(2);

    private static final String PAYMENT_DUE_STEP = "Payment Due";
    private static final String GRACE_PERIOD_STEP = "Grace Period";
    private static final String FIRST_RETRY_STEP = "First Retry";
    private static final String SUSPENDED_STEP = "Suspended";

    private static final String CATEGORY_CODE = "TestCategory";
    private static final String PRODUCT_CODE = "TestProduct";
    private static final String ACCOUNT_TYPE_CODE = "TestCollectionsAccount";
    private static final String CUSTOMER_CODE = "TestCustomer";
    private static final String PAYMENT_TYPE_CODE = "TestCCPaymentType";

    private static final String CC_CARD_HOLDER_NAME_MF = "cc.cardholder.name";
    private static final String CC_NUMBER_MF = "cc.number";
    private static final String CC_EXPIRY_DATE_MF = "cc.expiry.date";
    private static final String CC_TYPE_MF = "cc.type";
    private static final String CC_GATEWAY_KEY_MF = "cc.gateway.key";

    private static final Date initialNextInvoiceDate = new LocalDateTime(2013, 10, 1, 0, 0).toDate();


    @BeforeClass
    public void initializeTests(){
        testBuilder = getPluginsAndItemSetup();
    }

    @AfterClass
    public void tearDown(){

        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        JbillingAPI api = testBuilder.getTestEnvironment().getResellerApi();
        api.saveAgeingConfigurationWithCollectionType(new AgeingWS[0], api.getCallerLanguageId(), CollectionType.REGULAR);
        if(null!= environmentHelper){
            environmentHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
    }

    private TestBuilder getPluginsAndItemSetup(){
        return TestBuilder.newTest().givenForMultiple(envCreator -> {

            final JbillingAPI api = envCreator.getResellerApi();
            final JbillingAPI parentApi = envCreator.getPrancingPonyApi();

            environmentHelper = EnvironmentHelper.getInstance(api, parentApi);

            ConfigurationBuilder configurationBuilder = envCreator.configurationBuilder(api);

            api.saveAgeingConfigurationWithCollectionType(buildAgeingSteps(api), api.getCallerLanguageId(), CollectionType.REGULAR);
            createPluginConfig(api, configurationBuilder, findStepIdByStatus(api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR), GRACE_PERIOD_STEP),
                    environmentHelper.getUserOverdueNotificationId(api));
            configurationBuilder.build();

            Integer categoryId = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(false).build();
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(false).withType(categoryId)
                    .withFlatPrice("0.50").build();


            Integer paymentTypeId = envCreator.paymentMethodTypeBuilder(api, PAYMENT_TYPE_CODE)
                    .withMethodName(PAYMENT_TYPE_CODE).withOwningEntityId(api.getCallerCompanyId())
                    .withTemplateId(Integer.valueOf(-1))
                    .allAccountType(true)
                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_CARD_HOLDER_NAME_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.TITLE, null))

                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_NUMBER_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.PAYMENT_CARD_NUMBER,
                            buildValidationRule(true, "PAYMENT_CARD", new String[]{"Payment card number is not valid"}, api, null)))

                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_EXPIRY_DATE_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.DATE,
                            buildValidationRule(true, "REGEX", new String[]{"Expiry date should be in format MM/yyyy"}, api,
                                    buildRuleAttribute("regularExpression", "(?:0[1-9]|1[0-2])/[0-9]{4}"))))

                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_TYPE_MF, DataType.STRING,
                            EntityType.PAYMENT_METHOD_TYPE, api.getCallerCompanyId(), MetaFieldType.CC_TYPE, null))

                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_GATEWAY_KEY_MF, DataType.CHAR,
                            EntityType.PAYMENT_METHOD_TYPE, api.getCallerCompanyId(), MetaFieldType.GATEWAY_KEY, null))

                    .build().getId();

            envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).withPaymentMethodTypeIds(new Integer[]{paymentTypeId}).build();
        });
    }

    @Test(priority = 0)
    public void test001CollectionStepCheckTest(){

        /*
        In this test we are only checking if the configuration is correct for the Collection Steps
         */

        testBuilder.test((env) -> {

            JbillingAPI api = env.getResellerApi();

            AgeingWS[] ageingList = api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR);
            validateStep(ageingList[0], PAYMENT_DUE_STEP, Integer.valueOf(0), Boolean.FALSE, Boolean.FALSE,
                    Boolean.FALSE);
            validateStep(ageingList[1], GRACE_PERIOD_STEP, Integer.valueOf(2), Boolean.FALSE, Boolean.TRUE,
                    Boolean.FALSE);
            validateStep(ageingList[2], FIRST_RETRY_STEP, Integer.valueOf(3), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE);
            validateStep(ageingList[3], SUSPENDED_STEP, Integer.valueOf(7), Boolean.FALSE, Boolean.FALSE,
                    Boolean.TRUE);
        });
    }

    @Test(priority = 1)
    public void test002StatusTransitions(){

        final Date activeSince = new DateTime(initialNextInvoiceDate).toDate();
        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getResellerApi();
            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince, null, false);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(initialNextInvoiceDate, api);

        }).test((env) -> {

            JbillingAPI api = env.getResellerApi();
            InvoiceWS invoiceWS = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            try {
                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), "Active");

                triggerBillingProcess(activeSince, api);

                invoiceWS = api.getLatestInvoice(userId);
                assertNotNull(invoiceWS, "Invoice is not created.");
                assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                api.triggerAgeing(new LocalDate(activeSince).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, PAYMENT_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(activeSince).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, GRACE_PERIOD_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(activeSince).plusDays(4).toDate());
                validateUserAndInvoiceStatus(userId, FIRST_RETRY_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(activeSince).plusDays(8).toDate());
                validateUserAndInvoiceStatus(userId, SUSPENDED_STEP, INVOICE_STATUS_UNPAID, api);

            } finally {
                if (invoiceWS != null) {
                    api.deleteInvoice(invoiceWS.getId());
                }
                updateCustomerStatusToActive(userId, api);
            }
        });
    }

    @Test(priority = 2)
    public void test003SuccessfulPayment(){

        final Date activeSince = new DateTime(initialNextInvoiceDate).plusMonths(1).toDate();
        testBuilder.given(envBuilder ->{

            final JbillingAPI api = envBuilder.getResellerApi();
            final Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    activeSince, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                    .forUser(customerId)
                    .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withActiveSince(activeSince)
                    .withEffectiveDate(activeSince)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withCodeForTests("code")
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .build();

            updateBillingProcessConfiguration(activeSince, api);
            triggerBillingProcess(activeSince, api);
            api.triggerAgeing(new LocalDate(activeSince).plusDays(1).toDate());
            api.triggerAgeing(new LocalDate(activeSince).plusDays(3).toDate());
            api.triggerAgeing(new LocalDate(activeSince).plusDays(4).toDate());
            pause(2000);
        }).test((env)-> {
            JbillingAPI api = env.getResellerApi();
            PaymentWS paymentWS = null;
            Integer invoiceId = null;
            Integer customerId = env.idForCode(CUSTOMER_CODE);
            try {
                InvoiceWS invoiceWS = api.getLatestInvoice(customerId);
                assertNotNull(invoiceWS);
                invoiceId = invoiceWS.getId();
                paymentWS = api.getLatestPayment(customerId);
                assertNotNull(paymentWS);
                assertEquals(paymentWS.getAmountAsDecimal(), invoiceWS.getTotalAsDecimal());
                assertEquals(paymentWS.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
            }finally {

                if(paymentWS != null){
                    api.deletePayment(paymentWS.getId());
                }
                if(invoiceId != null){
                    api.deleteInvoice(invoiceId);
                }
                updateCustomerStatusToActive(customerId, api);
            }
        });
    }

    @Test(priority = 3)
    public void test004FailedPayment(){
        final Date activeSince = new DateTime(initialNextInvoiceDate).plusMonths(2).toDate();
        testBuilder.given(envBuilder ->{

            final JbillingAPI api = envBuilder.getResellerApi();
            final Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince,
                    envBuilder.env().idForCode(PAYMENT_TYPE_CODE), false);

            envBuilder.orderBuilder(api)
                    .forUser(customerId)
                    .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withActiveSince(activeSince)
                    .withEffectiveDate(activeSince)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withCodeForTests("code")
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .build();

            updateBillingProcessConfiguration(activeSince, api);
            triggerBillingProcess(activeSince, api);
            api.triggerAgeing(new LocalDate(activeSince).plusDays(1).toDate());
            api.triggerAgeing(new LocalDate(activeSince).plusDays(3).toDate());
            api.triggerAgeing(new LocalDate(activeSince).plusDays(4).toDate());
        }).test((env) -> {
            final JbillingAPI api = env.getResellerApi();
            final Integer customerId = env.idForCode(CUSTOMER_CODE);
            Integer invoiceId = null;
            PaymentWS paymentWS = null;
            try {
                InvoiceWS invoiceWS = api.getLatestInvoice(customerId);
                assertNotNull(invoiceWS);
                invoiceId = invoiceWS.getId();
                paymentWS = api.getLatestPayment(customerId);
                assertNotNull(paymentWS);
                assertEquals(paymentWS.getAmountAsDecimal(), invoiceWS.getTotalAsDecimal());
                assertEquals(paymentWS.getResultId(), Constants.PAYMENT_RESULT_FAILED);
            } finally {
                if (paymentWS != null) {
                    api.deletePayment(paymentWS.getId());
                }
                if (invoiceId != null) {
                    api.deleteInvoice(invoiceId);
                }
                updateCustomerStatusToActive(customerId, api);
            }
        });
    }

    @Test(priority = 4)
    public void test005SuccessfulPaymentWithEnteredPayment(){
        final Date activeSince = new DateTime(initialNextInvoiceDate).plusMonths(3).toDate();
        testBuilder.given(envBuilder ->{

            final JbillingAPI api = envBuilder.getResellerApi();
            final Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince,
                    envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                    .forUser(customerId)
                    .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withActiveSince(activeSince)
                    .withEffectiveDate(activeSince)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withCodeForTests("code")
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .build();

            updateBillingProcessConfiguration(activeSince, api);
            triggerBillingProcess(activeSince, api);
            Integer invoiceId = api.getLatestInvoice(customerId).getId();

            PaymentWS paymentWS = new PaymentWS();
            paymentWS.setUserId(customerId);
            paymentWS.setAmount(new BigDecimal("0.25"));
            paymentWS.setIsRefund(Integer.valueOf(0));
            paymentWS.setMethodId(Constants.PAYMENT_METHOD_VISA);
            paymentWS.setCurrencyId(api.getCallerCurrencyId());
            paymentWS.setPaymentInstruments(api.getUserWS(customerId).getPaymentInstruments());
            paymentWS.setPaymentDate(new Date());
            api.applyPayment(paymentWS, invoiceId);

        }).test((env)-> {
            final JbillingAPI api = env.getResellerApi();
            final Integer customerId = env.idForCode(CUSTOMER_CODE);

            PaymentWS paymentWS1 = null;
            PaymentWS paymentWS2 = null;
            Integer invoiceId = null;
            try {
                InvoiceWS invoiceWS = api.getLatestInvoice(customerId);
                invoiceId = invoiceWS.getId();

                paymentWS1 = api.getLatestPayment(customerId);
                assertNotNull(paymentWS1);
                assertEquals(paymentWS1.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("0.25").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Amount");
                assertEquals(paymentWS1.getResultId(), Constants.PAYMENT_RESULT_ENTERED);

                api.triggerAgeing(new LocalDate(activeSince).plusDays(1).toDate());
                api.triggerAgeing(new LocalDate(activeSince).plusDays(3).toDate());
                api.triggerAgeing(new LocalDate(activeSince).plusDays(4).toDate());

                pause(2000);
                paymentWS2 = api.getLatestPayment(customerId);
                assertNotNull(paymentWS2);
                assertEquals(paymentWS2.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("0.25").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Amount");
                assertEquals(paymentWS2.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
            }finally {

                if(paymentWS1 != null){
                    api.deletePayment(paymentWS1.getId());
                }

                if(paymentWS2 != null){
                    api.deletePayment(paymentWS2.getId());
                }

                if(invoiceId != null){
                    api.deleteInvoice(invoiceId);
                }
                updateCustomerStatusToActive(customerId, api);
            }
        });
    }

    private void pause(long t) {
        logger.debug("pausing for {} ms...", t);
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }



    @Test(priority = 5)
    public void test006SApplyLateFeeOnCollectionStep(){
        final String FEE_PRODUCT_CODE = "Fee";
        final Date activeSince = new DateTime(initialNextInvoiceDate).plusMonths(4).toDate();
        testBuilder.given(envBuilder ->{

            final JbillingAPI api = envBuilder.getResellerApi();
            final Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    activeSince, null, false);

            envBuilder.orderBuilder(api)
                    .forUser(customerId)
                    .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withActiveSince(activeSince)
                    .withEffectiveDate(activeSince)
                    .withCodeForTests("code")
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .build();

            ConfigurationBuilder configurationBuilder = envBuilder.configurationBuilder(api);

            Integer feeProductId = envBuilder.itemBuilder(api).item().withCode(FEE_PRODUCT_CODE).global(false)
                    .withType(envBuilder.env().idForCode(CATEGORY_CODE)).withFlatPrice("1").build();

            createFeePluginConfig(api, configurationBuilder,
                    findStepIdByStatus(api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR), FIRST_RETRY_STEP), feeProductId);
            configurationBuilder.build();
            updateBillingProcessConfiguration(activeSince, api);
            triggerBillingProcess(activeSince, api);
            api.triggerAgeing(new LocalDate(activeSince).plusDays(1).toDate());
            api.triggerAgeing(new LocalDate(activeSince).plusDays(3).toDate());
        }).test((env) -> {
            final Integer customerId = env.idForCode(CUSTOMER_CODE);
            final JbillingAPI api = env.getResellerApi();
            final Integer invoiceId = api.getLatestInvoice(customerId).getId();
            Integer orderId = null;
            try {
                api.triggerAgeing(new LocalDate(activeSince).plusDays(5).toDate());
                OrderWS orderWS = api.getLatestOrder(customerId);
                assertEquals(orderWS.getOrderLines().length, 1, "Invalid number Order Lines");
                orderId = orderWS.getId();
                OrderLineWS orderLineWS = orderWS.getOrderLines()[0];
                assertNotNull(orderLineWS);
                assertEquals(orderLineWS.getItemId(), env.idForCode(FEE_PRODUCT_CODE), "Invalid Item");
                assertEquals(orderLineWS.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("1").setScale(2, BigDecimal.ROUND_CEILING), "Invalid quantity");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("1").setScale(2, BigDecimal.ROUND_CEILING));
            } finally {
                if (invoiceId != null) {
                    api.deleteInvoice(invoiceId);
                }
                if (null != orderId) {
                    api.deleteOrder(orderId);
                }
                updateCustomerStatusToActive(customerId, api);
            }
        });
    }

    private void validateUserAndInvoiceStatus(Integer userId, String userStatus, Integer invoiceStatus, JbillingAPI api) {
        UserWS user = api.getUserWS(userId);
        assertEquals(user.getStatus(), userStatus);
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertEquals(invoice.getStatusId(), invoiceStatus);
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder, String code, Integer accountTypeId, Date nid, Integer paymentTypeId, boolean goodCC){
        final JbillingAPI api = envBuilder.getResellerApi();

        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                .withUsername(code).withAccountTypeId(accountTypeId)
                .withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), Integer.valueOf(1)));
        if (null != paymentTypeId){
            char[] date = DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(Util.truncateDate(new LocalDate().plusYears(10).toDate()).getTime()).toCharArray();
            customerBuilder.addPaymentInstrument(
                    customerBuilder.paymentInformation()
                            .withProcessingOrder(Integer.valueOf(1))
                            .withPaymentMethodId(Constants.PAYMENT_METHOD_VISA)
                            .withPaymentMethodTypeId(paymentTypeId)
                            .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_CARD_HOLDER_NAME_MF, CUSTOMER_CODE.toCharArray()))
                            .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_NUMBER_MF, goodCC ? "4111111111111152".toCharArray() : "4111111111111111".toCharArray()))
                            .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_EXPIRY_DATE_MF, date))
                            .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_TYPE_MF, CreditCardType.VISA))
                            .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_GATEWAY_KEY_MF, "zzzxxxaaa".toCharArray()))
                            .build()
            );
        }
        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nid);
        api.updateUser(user);
        return user.getId();
    }

    private ValidationRuleWS buildValidationRule(boolean enabled, String ruleType, String[] errorMsgs,
                                                 JbillingAPI api, SortedMap<String, String> ruleAttrs){
        ValidationRuleWS validationRule = new ValidationRuleWS();
        validationRule.setEnabled(enabled);
        validationRule.setRuleType(ruleType);
        for (String errorMsg : errorMsgs){
            validationRule.addErrorMessage(api.getCallerLanguageId(), errorMsg);
        }
        validationRule.setRuleAttributes(ruleAttrs);

        return validationRule;
    }

    private SortedMap<String, String> buildRuleAttribute(String key, String value){
        SortedMap<String, String> atts = new TreeMap<>();
        atts.put(key, value);
        return atts;
    }

    private AgeingWS[] buildAgeingSteps(JbillingAPI api) {
        AgeingWS[] ageingSteps = new AgeingWS[4];
        ageingSteps[0] = buildAgeingStep(PAYMENT_DUE_STEP, 0, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[1] = buildAgeingStep(GRACE_PERIOD_STEP, 2, false, true, false, api, CollectionType.REGULAR);
        ageingSteps[2] = buildAgeingStep(FIRST_RETRY_STEP, 3, true, false, false, api, CollectionType.REGULAR);
        ageingSteps[3] = buildAgeingStep(SUSPENDED_STEP, 7, false, false, true, api, CollectionType.REGULAR);
        return ageingSteps;
    }

    private void createPluginConfig(JbillingAPI api, ConfigurationBuilder configurationBuilder,
                                    Integer stepIdForPlugin, Integer notificationId) {
        String plugin = "com.sapienter.jbilling.server.user.tasks.UserAgeingNotificationTask";

        Hashtable<String, String> pluginParameters = new Hashtable<>();
        pluginParameters.put(String.valueOf(stepIdForPlugin), String.valueOf(notificationId));
        pluginParameters.put("0", "");
        addPluginWithParametersIfAbsent(configurationBuilder, plugin, api.getCallerCompanyId(), pluginParameters);
    }

    private void createFeePluginConfig(JbillingAPI api, ConfigurationBuilder configurationBuilder,
                                    Integer stepIdForPlugin, Integer feeProductId) {
        String plugin = "com.sapienter.jbilling.server.pluggableTask.BasicPenaltyTask";
        Hashtable<String, String> pluginParameters = new Hashtable<>();
        pluginParameters.put("item", String.valueOf(feeProductId));
        pluginParameters.put("ageing_step", String.valueOf(stepIdForPlugin));
        addPluginWithParametersIfAbsent(configurationBuilder, plugin, api.getCallerCompanyId(), pluginParameters);
    }


    private AgeingWS buildAgeingStep(String statusStep,Integer days,
                                     boolean payment , boolean sendNotification, boolean suspended, JbillingAPI api, CollectionType collectionType){

        AgeingWS ageingWS = new AgeingWS();
        ageingWS.setEntityId(api.getCallerCompanyId());
        ageingWS.setStatusStr(statusStep);
        ageingWS.setDays(days);
        ageingWS.setPaymentRetry(Boolean.valueOf(payment));
        ageingWS.setSendNotification(Boolean.valueOf(sendNotification));
        ageingWS.setSuspended(Boolean.valueOf(suspended));
        ageingWS.setStopActivationOnPayment(false);
        ageingWS.setCollectionType(collectionType);
        return  ageingWS;
    }

    private void validateStep(AgeingWS ageingWS, String statusStr, Integer days,
                              Boolean payment , Boolean sendNotification, Boolean suspended ){

        assertEquals(ageingWS.getStatusStr(), statusStr, "Invalid Step name");
        assertEquals(ageingWS.getDays(), days, "Invalid number of days");
        assertEquals(ageingWS.getPaymentRetry(), payment, "Invalid payment check");
        assertEquals(ageingWS.getSendNotification(), sendNotification, "Invalid notification check");
        assertEquals(ageingWS.getSuspended(), suspended, "Invalid suspended check");
    }

    public Integer buildAndPersistOrderPeriod(TestEnvironmentBuilder envBuilder, JbillingAPI api,
                                              String description, Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api)
                .withDescription(description)
                .withValue(value)
                .withUnitId(unitId)
                .build();
    }

    private void updateBillingProcessConfiguration(Date nextRunDate, JbillingAPI api){

        BillingProcessConfigurationWS billingProcessConfiguration = api.getBillingProcessConfiguration();
        billingProcessConfiguration.setMaximumPeriods(100);
        billingProcessConfiguration.setNextRunDate(nextRunDate);
        billingProcessConfiguration.setPeriodUnitId(Constants.PERIOD_UNIT_MONTH);
        billingProcessConfiguration.setReviewStatus(Integer.valueOf(0));
        billingProcessConfiguration.setGenerateReport(Integer.valueOf(0));
        billingProcessConfiguration.setOnlyRecurring(Integer.valueOf(0));
        api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
    }

    private void triggerBillingProcess(Date runDate, JbillingAPI api){
        api.triggerBilling(runDate);
        while (api.isBillingRunning(api.getCallerCompanyId())){
            wait(2000);
        }
    }

    private void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("Errors while waiting for operations to complete");
        }
    }

    private void addPluginWithParametersIfAbsent(ConfigurationBuilder configurationBuilder, String pluginClassName,
                                                 Integer entityId, Hashtable<String, String> parameters){

        if (!configurationBuilder.pluginExists(pluginClassName, entityId)){
            configurationBuilder.addPluginWithParameters(pluginClassName, parameters);
        }
    }

    private Integer findStepIdByStatus(AgeingWS[] ageingSteps, String status){
        for(AgeingWS ageingWS : ageingSteps){
            if(ageingWS.getStatusStr().equalsIgnoreCase(status)){
                return ageingWS.getStatusId();
            }
        }
        throw new AssertionError("Should be found!");
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        user.setStatus("Active");
        api.updateUser(user);
    }
}
