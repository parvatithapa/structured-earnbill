package com.sapienter.jbilling.fc;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder.PaymentInformationBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;

/**
 * This class is for testing payment Via Unified BrainTree Plugin
 * This class has testcases for processSignupPayment and processPayment
 * 
 * @author Ashwinkumar
 */
@Test(groups = "external-system", testName = "FullCreativePaymentViaUnifiedBrainTreeTest")
public class FullCreativePaymentViaUnifiedBrainTreeTest {

    
    private static final String BT_CUSTOMER_CODE                    = "BT_Test_Customer";
    private static final String BT_CC                               = "BT_CC";
    private static final String BT_ACH                              = "BT_ACH";
    private static final String BT_ECO                              = "BT_ECO";
    private static final String UNIFIED_BT_PLUGIN_NAME              = "com.sapienter.jbilling.server.payment.tasks.unified.braintree.UnifiedBrainTreePaymentExternalTask";
    private static final String FAKE_PAYMENT_PLUGIN_NAME            = "com.sapienter.jbilling.server.pluggableTask.PaymentFakeTask";
    private static final String FAKE_PAYMENT_ROUTER_PLUGIN_NAME     = "com.sapienter.jbilling.server.payment.tasks.PaymentRouterCCFTask";
    private static final String FAKE_PAYMENT_FILTER_PLUGIN_NAME     = "com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask";
    private static final String FAKE_PAYMENT_ROUTR_CUR_PLUGIN_NAME  = "com.sapienter.jbilling.server.payment.tasks.PaymentRouterCurrencyTask";
    private static final String BT_CC_BUSINESS_ID                   = "bf4d2b3b-e4f8-4066-90f1-c5d7941bbc33";
    private static final String BT_ACH_BUSINESS_ID                  = "bf758cd7-61ca-41da-9e47-609ac0ce0fc3";
    private static final Logger logger                              = LoggerFactory.getLogger(FullCreativePaymentViaUnifiedBrainTreeTest.class);

    // Fixed constants for now
    private static final Integer INVOICE_STATUS_UNPAID              = Integer.valueOf(2);

    private static final String ACTIVE                              = "Active";
    private static final String CATEGORY_CODE                       = "TestCategory";
    private static final String PRODUCT_CODE                        = "TestProduct";
    private static final String ACCOUNT_TYPE_CODE                   = "TestAccountType";
    private static final String CUSTOMER_CODE                       = "TestCustomer";
    private static final String BT_CC_PAYMENT_TYPE_CODE             = "Test_BT_CC";
    private static final String BT_ACH_PAYMENT_TYPE_CODE            = "Test_BT_ACH";
    private static final String BT_ECO_PAYMENT_TYPE_CODE            = "Test_BT_ECO";
    private static final String BT_ID                               = "BT ID";
    private static final String TYPE                                = "Type";
    private static final int ORDER_CHANGE_STATUS_APPLY_ID           = 3;
    private static final String PLUGIN_PARAM_BUSSINESS_ID           = "Bussiness Id";
    private static final String INVALID_NUMBER_ORDER_LINES          = "Invalid number Order Lines";
    private static final String INVOICE_SHOULD_BE_GENERATED         = "Invoice should be generated";
    private static final String INVOICE_IS_NOT_CREATED              = "Invoice is not created.";
    private static final String PROCESSING_TOKEN_PAYMENT            = "Processing token payment...";
    private static final String PAYMENT_RESULT_NOT_NULL             = "Payment result not null";
    private static final String PAYMENT_AUTH_RESULT_SHOULD_BE_OK    = "Payment Authorization result should be OK";
    private static final String BT_ID_CC_VALID_VALUE                = "0f436b8a-fa97-4fda-929e-60439717d042";
    private static final String BT_ID_ACH_VALID_VALUE               = "4ab3565d-7df5-4850-8f03-b36d4e6eb4ab";
    private static final String BT_ID_ECO_VALID_VALUE               = "29449382-7904-48be-91d7-1b8730787ae5";
    private static final String BT_ID_CC_INVALID_VALUE              = "29449382-7904-48be-91d7-1b8730782r23";
    private TestBuilder testBuilder;
    private EnvironmentHelper environmentHelper;
    private Integer achPaymentMethodTypeId                   = null;
    private Integer ccPaymentMethodTypeId                    = null;
    private Integer ecoPaymentMethodTypeId                   = null;
    private Integer btCCPaymentTypeId;
    private Integer btACHPaymentTypeId;
    private Integer btECOPaymentTypeId;
    private Integer btAccountTypeId;
    private String btCustomerName;

    @BeforeClass
    public void initializeTests() {
        testBuilder = getPluginsAndItemSetup();
    }

    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        try {
            testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        } finally {
            updatePluginsProcessingOrder(api, api.getCallerId(), -100, FAKE_PAYMENT_PLUGIN_NAME,
                    FAKE_PAYMENT_ROUTER_PLUGIN_NAME, FAKE_PAYMENT_FILTER_PLUGIN_NAME,
                    FAKE_PAYMENT_ROUTR_CUR_PLUGIN_NAME);
            updatePaymentMethodType(api, achPaymentMethodTypeId, "ach");
            updatePaymentMethodType(api, ccPaymentMethodTypeId, "cc");
            updatePaymentMethodType(api, ecoPaymentMethodTypeId, "eco");
            if (null != environmentHelper) {
                environmentHelper = null;
            }
            if (null != testBuilder) {
                testBuilder = null;
            }
        }
    }

    private TestBuilder getPluginsAndItemSetup() {
        return TestBuilder.newTest().givenForMultiple(
                envCreator -> {

                    final JbillingAPI api = envCreator.getPrancingPonyApi();

                    Integer entityId = api.getCallerId();

                    ConfigurationBuilder configurationBuilder = envCreator.configurationBuilder(api);

                    updatePluginsProcessingOrder(api, entityId, 100, FAKE_PAYMENT_PLUGIN_NAME,
                            FAKE_PAYMENT_ROUTER_PLUGIN_NAME, FAKE_PAYMENT_FILTER_PLUGIN_NAME,
                            FAKE_PAYMENT_ROUTR_CUR_PLUGIN_NAME);

                    Hashtable<String, String> unifiedBrainTreeparameters = new Hashtable<String, String>();
                    unifiedBrainTreeparameters.put(PLUGIN_PARAM_BUSSINESS_ID, BT_CC_BUSINESS_ID);

                    if (!configurationBuilder.pluginExists(UNIFIED_BT_PLUGIN_NAME, entityId)) {
                        configurationBuilder
                                .addPluginWithParameters(UNIFIED_BT_PLUGIN_NAME, unifiedBrainTreeparameters)
                                .withProcessingOrder(UNIFIED_BT_PLUGIN_NAME, 1);
                    }
                    configurationBuilder.build();

                    environmentHelper = EnvironmentHelper.getInstance(api);
                    Integer categoryId = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(false)
                            .build();
                    envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(false).withType(categoryId)
                            .withFlatPrice("0.50").build();

                    btACHPaymentTypeId = envCreator
                            .paymentMethodTypeBuilder(api, BT_ACH_PAYMENT_TYPE_CODE)
                            .withMethodName(BT_ACH_PAYMENT_TYPE_CODE)
                            .withOwningEntityId(api.getCallerCompanyId())
                            .withTemplateId(Integer.valueOf(1))
                            .allAccountType(true)
                            .isRecurring(true)
                            .addMetaField(
                                    buildAndPersistMetafield(BT_ID, DataType.CHAR, MetaFieldType.GATEWAY_KEY, 1, null,
                                            false, true))
                            .addMetaField(buildAndPersistMetafield(TYPE, DataType.STRING, null, 2, null, true, false))
                            .build().getId();
                    logger.debug("ACH Payment iD  {}:", btACHPaymentTypeId);

                    btCCPaymentTypeId = envCreator
                            .paymentMethodTypeBuilder(api, BT_CC_PAYMENT_TYPE_CODE)
                            .withMethodName(BT_CC_PAYMENT_TYPE_CODE)
                            .withOwningEntityId(api.getCallerCompanyId())
                            .withTemplateId(Integer.valueOf(1))
                            .allAccountType(true)
                            .isRecurring(true)
                            .addMetaField(
                                    buildAndPersistMetafield(BT_ID, DataType.CHAR, MetaFieldType.GATEWAY_KEY, 1, null,
                                            false, true))
                            .addMetaField(buildAndPersistMetafield(TYPE, DataType.STRING, null, 2, null, true, false))
                            .build().getId();
                    logger.debug("CC Payment iD  {}:", btCCPaymentTypeId);

                    btECOPaymentTypeId = envCreator
                            .paymentMethodTypeBuilder(api, BT_ECO_PAYMENT_TYPE_CODE)
                            .withMethodName(BT_ECO_PAYMENT_TYPE_CODE)
                            .withOwningEntityId(api.getCallerCompanyId())
                            .withTemplateId(Integer.valueOf(1))
                            .allAccountType(true)
                            .isRecurring(true)
                            .addMetaField(
                                    buildAndPersistMetafield(BT_ID, DataType.CHAR, MetaFieldType.GATEWAY_KEY, 1, null,
                                            false, true))
                            .addMetaField(buildAndPersistMetafield(TYPE, DataType.STRING, null, 2, null, true, false))
                            .build().getId();
                    logger.debug("ECO Payment iD  {}:", btECOPaymentTypeId);

                    removeUnwantedMetafields(api, btACHPaymentTypeId, btCCPaymentTypeId, btECOPaymentTypeId);
                    achPaymentMethodTypeId = btACHPaymentTypeId;
                    ccPaymentMethodTypeId = btCCPaymentTypeId;
                    ecoPaymentMethodTypeId = btECOPaymentTypeId;
                    AccountTypeWS accountType = envCreator
                            .accountTypeBuilder(api)
                            .withName(ACCOUNT_TYPE_CODE)
                            .withPaymentMethodTypeIds(
                                    new Integer[] { btACHPaymentTypeId, btCCPaymentTypeId, btECOPaymentTypeId })
                            .build();
                    assertNotNull(accountType);
                    btAccountTypeId = accountType.getId();
                });
    }

    /**
     * Make payment with BT CC.
     */
    @Test(priority = 1, enabled = true)
    public void test001MakePaymentWithValidBTIDForCC() {
        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01,
                getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(
                envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE,
                            envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate,
                            envBuilder.env().idForCode(BT_CC_PAYMENT_TYPE_CODE), BT_ID_CC_VALID_VALUE, BT_CC);
                    envBuilder.orderBuilder(api).forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID).withActiveSince(activeSince)
                            .withEffectiveDate(activeSince).withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(Integer.valueOf(1)).withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api)).build();

                    updateBillingProcessConfiguration(nextInvoiceDate, api, 21);
                }).test(env -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            PaymentWS paymentWS1 = new PaymentWS();
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);
                // Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate, 0));
                assertEquals(invoiceIds1.length, 1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                getPayment(api, invoiceWS1.getBalance(), userId, paymentWS1, null, Integer.valueOf(0), null);
                logger.debug(PROCESSING_TOKEN_PAYMENT);
                PaymentAuthorizationDTOEx authInfo = api.processPayment(paymentWS1, null);
                assertNotNull(authInfo, PAYMENT_RESULT_NOT_NULL);
                assertTrue(authInfo.getResult().booleanValue(), PAYMENT_AUTH_RESULT_SHOULD_BE_OK);
                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

            } finally {
                for (int i = invoiceIdList.size() - 1; i >= 0; i--) {
                    if (invoiceIdList.get(i) != null) {
                        api.deleteInvoice(invoiceIdList.get(i));
                    }
                }
                if (null != orderId) {
                    api.deleteOrder(orderId);
                }
                updateCustomerStatusToActive(userId, api);
                api.deleteUser(userId);
            }
        });
    }

    /**
     * Make payment with Invalid BT Credit Card ID.
     */
    @Test(priority = 2, enabled = true)
    public void test002MakePaymentWithInValidBTIDForCC() {
        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01,
                getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(
                envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE,
                            envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate,
                            envBuilder.env().idForCode(BT_CC_PAYMENT_TYPE_CODE), BT_ID_CC_INVALID_VALUE, BT_CC);
                    envBuilder.orderBuilder(api).forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID).withActiveSince(activeSince)
                            .withEffectiveDate(activeSince).withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(Integer.valueOf(1)).withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api)).build();

                    updateBillingProcessConfiguration(nextInvoiceDate, api, 21);
                }).test(env -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            PaymentWS paymentWS1 = new PaymentWS();
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);
                // Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate, 0));
                assertEquals(invoiceIds1.length, 1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                getPayment(api, invoiceWS1.getBalance(), userId, paymentWS1, null, Integer.valueOf(0), null);
                logger.debug(PROCESSING_TOKEN_PAYMENT);
                PaymentAuthorizationDTOEx authInfo = api.processPayment(paymentWS1, null);
                assertNotNull(authInfo, PAYMENT_RESULT_NOT_NULL);
                assertTrue(!authInfo.getResult().booleanValue(), "Payment Authorization result should be FALSE");
                assertEquals(authInfo.getCode1(), "", "Payment transction value should be empty");
                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

            } finally {
                for (int i = invoiceIdList.size() - 1; i >= 0; i--) {
                    if (invoiceIdList.get(i) != null) {
                        api.deleteInvoice(invoiceIdList.get(i));
                    }
                }
                if (null != orderId) {
                    api.deleteOrder(orderId);
                }
                updateCustomerStatusToActive(userId, api);
                api.deleteUser(userId);
            }
        });
    }

    @Test(priority = 3, enabled = true)
    public void test003ProcessSignupPaymentAPIWithBTIDForCC() {
        Date today = new Date();
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(envBuilder ->
            btCustomerName = BT_CUSTOMER_CODE + System.currentTimeMillis()
        ).test(env -> {
            Integer userId = null;
            JbillingAPI api = env.getPrancingPonyApi();
            try {
                PaymentWS paymentWS1 = new PaymentWS();
                CustomerBuilder customerBuilder = CustomerBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                UserWS user = getUser(customerBuilder, api, btCustomerName, nextInvoiceDate, btCCPaymentTypeId,
                        BT_ID_CC_VALID_VALUE, BT_CC);
                getPayment(api, "1", user.getId(), paymentWS1, user.getPaymentInstruments(), Integer.valueOf(0), null);
                logger.debug("user ::::::::::::: {}", user);
                userId = api.processSignupPayment(user, paymentWS1);
                logger.debug("userId ::::::::::::: {}", userId);
                assertNotNull(userId, "User Id can not be null");
                user = api.getUserWS(userId);
                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                assertNotNull(user, "User can not be null");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (null != userId) {
                    updateCustomerStatusToActive(userId, api);
                    api.deleteUser(userId);
                }
            }
        });
    }

    @Test(priority = 4, enabled = true)
    public void test004ProcessSignupPaymentAPIWithInvalidBTIDForCC() {
        Date today = new Date();
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(envBuilder ->
            btCustomerName = BT_CUSTOMER_CODE + System.currentTimeMillis()
        ).test(env -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = null;
            try {
                PaymentWS paymentWS1 = new PaymentWS();
                CustomerBuilder customerBuilder = CustomerBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                UserWS user = getUser(customerBuilder, api, btCustomerName, nextInvoiceDate, btCCPaymentTypeId,
                        BT_ID_CC_INVALID_VALUE, BT_CC);
                getPayment(api, "1", user.getId(), paymentWS1, user.getPaymentInstruments(), Integer.valueOf(0), null);
                logger.debug("user ::::::::::::: {}", user);

                userId = api.processSignupPayment(user, paymentWS1);
            } catch (Exception e) {
                assertNotNull(e, "method should throw exception");
                assertTrue(e.getMessage().contains("BusinessId associated with customerKey is not found"));
            } finally {
                if (null != userId) {
                    updateCustomerStatusToActive(userId, api);
                    api.deleteUser(userId);
                }
            }
        });
    }

    /**
     * Make refund payment with BT CC.
     */
    @Test(priority = 5, enabled = true)
    public void test005RefundPaymentWithValidBTIDForCC() {
        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01,
                getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder
                .given(envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE,
                            envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate,
                            envBuilder.env().idForCode(BT_CC_PAYMENT_TYPE_CODE), BT_ID_CC_VALID_VALUE, BT_CC);
                    envBuilder.orderBuilder(api).forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID).withActiveSince(activeSince)
                            .withEffectiveDate(activeSince).withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(Integer.valueOf(1)).withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api)).build();

                    updateBillingProcessConfiguration(nextInvoiceDate, api, 21);
                })
                .test(env -> {

                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS1 = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE);
                    Integer orderId = null;
                    PaymentWS paymentWS = new PaymentWS();
                    PaymentWS refundPaymentWS = new PaymentWS();
                    List<Integer> invoiceIdList = new ArrayList<>();
                    try {
                        OrderWS orderWS = api.getLatestOrder(userId);
                        assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                        orderId = orderWS.getId();

                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), ACTIVE);
                        // Create first invoice
                        Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate, 0));
                        assertEquals(invoiceIds1.length, 1, INVOICE_SHOULD_BE_GENERATED);
                        invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                        assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                        assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                        invoiceIdList.add(invoiceWS1.getId());
                        getPayment(api, "2.00", userId, paymentWS, null, Integer.valueOf(0), null);
                        logger.debug(PROCESSING_TOKEN_PAYMENT);
                        PaymentAuthorizationDTOEx authInfo = api.processPayment(paymentWS, null);
                        assertNotNull(authInfo, PAYMENT_RESULT_NOT_NULL);
                        assertTrue(authInfo.getResult().booleanValue(), PAYMENT_AUTH_RESULT_SHOULD_BE_OK);
                        paymentWS = api.getLatestPayment(userId);
                        assertNotNull(paymentWS);
                        wait(60000, "Wait before doing refund payment");
                        getPayment(api, paymentWS.getBalance(), userId, refundPaymentWS, null, Integer.valueOf(1),
                                paymentWS.getId());
                        logger.debug(PROCESSING_TOKEN_PAYMENT);
                        PaymentAuthorizationDTOEx authInfo2 = api.processPayment(refundPaymentWS, null);
                        assertNotNull(authInfo2, "Refund Payment result not null");
                        assertTrue(!authInfo2.getResult().booleanValue(),
                                "Payment Authorization result should be FALSE, Since it takes 24 hours for the transaction to settle");
                        logger.debug(api.getLatestPayment(userId).toString());
                        logger.debug("=========================================={}=================================",
                                authInfo2.getResponseMessage());
                        assertTrue(authInfo2.getResponseMessage().contains("TRANSACTION_CANNOT_REFUND_UNLESS_SETTLED"));
                    } finally {
                        for (int i = invoiceIdList.size() - 1; i >= 0; i--) {
                            if (invoiceIdList.get(i) != null) {
                                api.deleteInvoice(invoiceIdList.get(i));
                            }
                        }
                        if (null != orderId) {
                            api.deleteOrder(orderId);
                        }
                        updateCustomerStatusToActive(userId, api);
                        api.deleteUser(userId);
                    }
                });
    }

    /**
     * Make refund payment with BT CC for invalid amount.
     */
    @Test(priority = 6, enabled = true)
    public void test006RefundPaymentWithInValidAmount() {
        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01,
                getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(
                envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE,
                            envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate,
                            envBuilder.env().idForCode(BT_CC_PAYMENT_TYPE_CODE), BT_ID_CC_VALID_VALUE, BT_CC);
                    envBuilder.orderBuilder(api).forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID).withActiveSince(activeSince)
                            .withEffectiveDate(activeSince).withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(Integer.valueOf(1)).withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api)).build();

                    updateBillingProcessConfiguration(nextInvoiceDate, api, 21);
                }).test(env -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            PaymentWS paymentWS = new PaymentWS();
            PaymentWS refundPaymentWS = new PaymentWS();
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);
                // Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate, 0));
                assertEquals(invoiceIds1.length, 1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                getPayment(api, "2.00", userId, paymentWS, null, Integer.valueOf(0), null);
                logger.debug(PROCESSING_TOKEN_PAYMENT);
                PaymentAuthorizationDTOEx authInfo = api.processPayment(paymentWS, null);
                assertNotNull(authInfo, PAYMENT_RESULT_NOT_NULL);
                assertTrue(authInfo.getResult().booleanValue(), PAYMENT_AUTH_RESULT_SHOULD_BE_OK);
                paymentWS = api.getLatestPayment(userId);
                assertNotNull(paymentWS);
                wait(60000, "Wait before doing refund payment");
                getPayment(api, "3.00", userId, refundPaymentWS, null, Integer.valueOf(1), paymentWS.getId());
                logger.debug(PROCESSING_TOKEN_PAYMENT);
                api.processPayment(refundPaymentWS, null);
            } catch (Exception e) {
                assertNotNull(e, "method should throw exception");
                assertTrue(e.getMessage().contains("the refund amount is in-correct"));
            } finally {
                for (int i = invoiceIdList.size() - 1; i >= 0; i--) {
                    if (invoiceIdList.get(i) != null) {
                        api.deleteInvoice(invoiceIdList.get(i));
                    }
                }
                if (null != orderId) {
                    api.deleteOrder(orderId);
                }
                updateCustomerStatusToActive(userId, api);
                api.deleteUser(userId);
            }
        });
    }

    /**
     * Make payment with ACH.
     */
    @Test(priority = 11, enabled = true)
    public void test011MakePaymentWithValidBTIDForACH() {
        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01,
                getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(
                envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    updateUnifiedBTPlugin(api, BT_ACH_BUSINESS_ID);

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE,
                            envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate,
                            envBuilder.env().idForCode(BT_CC_PAYMENT_TYPE_CODE), BT_ID_ACH_VALID_VALUE, BT_ACH);
                    envBuilder.orderBuilder(api).forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID).withActiveSince(activeSince)
                            .withEffectiveDate(activeSince).withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(Integer.valueOf(1)).withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api)).build();

                    updateBillingProcessConfiguration(nextInvoiceDate, api, 21);
                }).test(env -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            PaymentWS paymentWS1 = new PaymentWS();
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);
                // Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate, 0));
                assertEquals(invoiceIds1.length, 1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                getPayment(api, invoiceWS1.getBalance(), userId, paymentWS1, null, Integer.valueOf(0), null);
                logger.debug(PROCESSING_TOKEN_PAYMENT);
                PaymentAuthorizationDTOEx authInfo = api.processPayment(paymentWS1, null);
                assertNotNull(authInfo, PAYMENT_RESULT_NOT_NULL);
                assertTrue(authInfo.getResult().booleanValue(), PAYMENT_AUTH_RESULT_SHOULD_BE_OK);
                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

            } finally {
                for (int i = invoiceIdList.size() - 1; i >= 0; i--) {
                    if (invoiceIdList.get(i) != null) {
                        api.deleteInvoice(invoiceIdList.get(i));
                    }
                }
                if (null != orderId) {
                    api.deleteOrder(orderId);
                }
                updateCustomerStatusToActive(userId, api);
                api.deleteUser(userId);
            }
        });
    }

    /**
     * Make payment with Paypal ECO.
     */
    @Test(priority = 21, enabled = false)
    public void test021MakePaymentWithValidBTIDForECO() {
        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01,
                getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)), 01,
                getYear(addMonths(today, 0)));
        testBuilder.given(
                envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE,
                            envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate,
                            envBuilder.env().idForCode(BT_CC_PAYMENT_TYPE_CODE), BT_ID_ECO_VALID_VALUE, BT_ECO);
                    envBuilder.orderBuilder(api).forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID).withActiveSince(activeSince)
                            .withEffectiveDate(activeSince).withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(Integer.valueOf(1)).withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api)).build();

                    updateBillingProcessConfiguration(nextInvoiceDate, api, 21);
                }).test(env -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            PaymentWS paymentWS1 = new PaymentWS();
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);
                // Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate, 0));
                assertEquals(invoiceIds1.length, 1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                getPayment(api, invoiceWS1.getBalance(), userId, paymentWS1, null, Integer.valueOf(0), null);
                logger.debug(PROCESSING_TOKEN_PAYMENT);
                PaymentAuthorizationDTOEx authInfo = api.processPayment(paymentWS1, null);
                assertNotNull(authInfo, PAYMENT_RESULT_NOT_NULL);
                assertTrue(authInfo.getResult().booleanValue(), PAYMENT_AUTH_RESULT_SHOULD_BE_OK);
                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

            } finally {
                for (int i = invoiceIdList.size() - 1; i >= 0; i--) {
                    if (invoiceIdList.get(i) != null) {
                        api.deleteInvoice(invoiceIdList.get(i));
                    }
                }
                if (null != orderId) {
                    api.deleteOrder(orderId);
                }
                updateCustomerStatusToActive(userId, api);
                api.deleteUser(userId);
            }
        });
    }

    private Integer[] createInvoice(JbillingAPI api, Integer userId, Date nextInvoiceDate) {
        UserWS userWS = api.getUserWS(userId);
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        return api.createInvoiceWithDate(userId, nextInvoiceDate, 3, 21, false);
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder, String code, Integer accountTypeId, Date nid,
            Integer paymentTypeId, String bt_id, String btPaymentType) {
        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        CustomerBuilder customerBuilder = envBuilder
                .customerBuilder(api)
                .withUsername(code)
                .withAccountTypeId(accountTypeId)
                .withMainSubscription(
                        new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), Integer.valueOf(1)));
        if (null != paymentTypeId) {
            PaymentInformationBuilder paymentInfoBuilder = customerBuilder
                    .paymentInformation()
                    .withProcessingOrder(Integer.valueOf(1))
                    .withPaymentMethodId(Constants.PAYMENT_METHOD_VISA)
                    .withPaymentMethodTypeId(paymentTypeId)
                    .addMetaFieldValue(
                            ApiBuilderHelper.getMetaFieldValueWS(BT_ID,
                                    StringUtils.isEmpty(bt_id) ? BT_ID_CC_VALID_VALUE : bt_id));
            if (btPaymentType.equalsIgnoreCase(BT_CC)) {
                paymentInfoBuilder.addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(TYPE, "btcc"));
            } else if (btPaymentType.equalsIgnoreCase(BT_ACH)) {
                paymentInfoBuilder.addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(TYPE, "ach"));
            } else if (btPaymentType.equalsIgnoreCase(BT_ECO)) {
                paymentInfoBuilder.addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(TYPE, "eco"));
            }
            customerBuilder.addPaymentInstrument(paymentInfoBuilder.build());
        }
        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nid);
        api.updateUser(user);
        return user.getId();
    }

    public UserWS getUser(CustomerBuilder customerBuilder, JbillingAPI api, String userName, Date nextInvoiceDate,
            Integer paymentTypeId, String bt_id, String btPaymentType)
            throws JbillingAPIException, IOException {
        UserWS newUser = new UserWS();
        // List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
        newUser.setUserId(0);
        newUser.setUserName(userName);
        newUser.setPassword("P@ssword12");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(btAccountTypeId);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(null);
        newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
        newUser.setInvoiceChild(Boolean.FALSE);
        logger.debug("Meta field values set");
        PaymentInformationWS cc = new PaymentInformationWS();

        if (null != paymentTypeId) {
            PaymentInformationBuilder paymentInfoBuilder = customerBuilder
                    .paymentInformation()
                    .withProcessingOrder(Integer.valueOf(1))
                    .withPaymentMethodId(Constants.PAYMENT_METHOD_VISA)
                    .withPaymentMethodTypeId(paymentTypeId)
                    .addMetaFieldValue(
                            ApiBuilderHelper.getMetaFieldValueWS(BT_ID,
                                    StringUtils.isEmpty(bt_id) ? BT_ID_CC_VALID_VALUE : bt_id));
            if (btPaymentType.equalsIgnoreCase(BT_CC)) {
                paymentInfoBuilder.addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(TYPE, "btcc"));
            } else if (btPaymentType.equalsIgnoreCase(BT_ACH)) {
                paymentInfoBuilder.addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(TYPE, "ach"));
            } else if (btPaymentType.equalsIgnoreCase(BT_ECO)) {
                paymentInfoBuilder.addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(TYPE, "eco"));
            }
            cc = paymentInfoBuilder.build();
        }

        newUser.getPaymentInstruments().add(cc);

        logger.debug("Creating user ...");
        MainSubscriptionWS billing = new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api),
                Integer.valueOf(1));
        newUser.setMainSubscription(billing);
        newUser.setNextInvoiceDate(nextInvoiceDate);
        logger.debug("User created with id: {}", newUser.getUserId());

        return newUser;
    }

    public Integer buildAndPersistOrderPeriod(TestEnvironmentBuilder envBuilder, JbillingAPI api, String description,
            Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api).withDescription(description).withValue(value).withUnitId(unitId)
                .build();
    }

    private void updateBillingProcessConfiguration(Date nextRunDate, JbillingAPI api, int dueDays) {

        BillingProcessConfigurationWS billingProcessConfiguration = api.getBillingProcessConfiguration();
        billingProcessConfiguration.setMaximumPeriods(100);
        billingProcessConfiguration.setNextRunDate(nextRunDate);
        billingProcessConfiguration.setPeriodUnitId(Constants.PERIOD_UNIT_MONTH);
        billingProcessConfiguration.setReviewStatus(Integer.valueOf(0));
        billingProcessConfiguration.setGenerateReport(Integer.valueOf(0));
        billingProcessConfiguration.setOnlyRecurring(Integer.valueOf(0));
        billingProcessConfiguration.setDueDateUnitId(3);
        billingProcessConfiguration.setDueDateValue(dueDays);
        api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
    }

    private void wait(int millis, String msg) {
        try {
            logger.debug("========={}===========wait={}sec", msg, millis / 1000);
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("Errors while waiting for operations to complete");
        }
    }

    private void updateUnifiedBTPlugin(JbillingAPI api, String btAchBusinessId) {
        PluggableTaskWS[] unifiedBTPlugins = api.getPluginsWS(api.getCallerCompanyId(), UNIFIED_BT_PLUGIN_NAME);
        for (PluggableTaskWS unifiedBTPlugin : unifiedBTPlugins) {
            Hashtable<String, String> parameters = new Hashtable();
            parameters.putAll(unifiedBTPlugin.getParameters());
            parameters.put(PLUGIN_PARAM_BUSSINESS_ID, btAchBusinessId);
            unifiedBTPlugin.setParameters(parameters);
            api.updatePlugin(unifiedBTPlugin);
        }
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api) {

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }

    private static Integer getMonth(Date inputDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.MONTH));
    }

    private static Date addMonths(Date inputDate, int months) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    private static Integer getYear(Date inputDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.YEAR));
    }

    public String getTimeDiff(Date dateOne, Date dateTwo) {
        String diff = "";
        long timeDiff = Math.abs(dateOne.getTime() - dateTwo.getTime());
        diff = String.format(
                "%d hour(s) %d min(s)",
                TimeUnit.MILLISECONDS.toHours(timeDiff),
                TimeUnit.MILLISECONDS.toMinutes(timeDiff)
                        - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeDiff)));
        return diff;
    }

    public static int daysBetweenUsingJoda(Date d1, Date d2) {
        return Days.daysBetween(new LocalDate(d1.getTime()), new LocalDate(d2.getTime())).getDays();
    }

    private void getPayment(JbillingAPI api, String amount, Integer userId, PaymentWS paymentWS,
            List<PaymentInformationWS> paymentInformationList, Integer isRefund, Integer existingPaymentId) {
        paymentWS.setUserId(userId);
        paymentWS.setAmount(amount);
        paymentWS.setIsRefund(isRefund);
        paymentWS.setMethodId(Constants.PAYMENT_METHOD_VISA);
        paymentWS.setCurrencyId(api.getCallerCurrencyId());
        if (userId.equals(0)) {
            paymentWS.setPaymentInstruments(paymentInformationList);
        } else {
            paymentWS.setPaymentInstruments(api.getUserWS(userId).getPaymentInstruments());
        }
        paymentWS.setPaymentDate(new Date());
        paymentWS.setPaymentId(existingPaymentId);
    }

    public static Integer createOneTimeOrder(JbillingAPI api, Integer userId, Date activeSinceDate,
            String productQuantity, Integer productCode) {
        logger.debug("Creating One time usage order...");
        OrderWS oTOrder = new OrderWS();
        oTOrder.setUserId(userId);
        oTOrder.setActiveSince(activeSinceDate);
        oTOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        oTOrder.setPeriod(1); // Onetime
        oTOrder.setCurrencyId(1);

        OrderLineWS oTline1 = new OrderLineWS();
        oTline1.setItemId(productCode);
        oTline1.setDescription("Credit Adjustment");
        oTline1.setQuantity(productQuantity);
        oTline1.setTypeId(1);
        oTline1.setPrice("-0.50");
        oTline1.setAmount("-0.50");
        oTline1.setUseItem(true);

        oTOrder.setOrderLines(new OrderLineWS[] { oTline1 });
        Integer oneTimeOrderId = api.createOrder(oTOrder,
                OrderChangeBL.buildFromOrder(oTOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        logger.debug("Created one time usage order with Id: {}", oneTimeOrderId);
        assertNotNull(oneTimeOrderId, "one time usage order creation failed");

        return oneTimeOrderId;
    }

    private void updatePluginsProcessingOrder(final JbillingAPI api, Integer entityId, Integer value,
            String... pluginNames) {
        for (String pluginName : pluginNames) {
            for (PluggableTaskWS task : api.getPluginsWS(entityId, pluginName)) {
                task.setProcessingOrder(task.getProcessingOrder() + value);
                api.updatePlugin(task);
            }
        }
    }

    private void updatePaymentMethodType(JbillingAPI api, Integer paymentMethodTypeId, String prefix) {
        PaymentMethodTypeWS paymentMethodTypeWS = api.getPaymentMethodType(paymentMethodTypeId);
        if (null != paymentMethodTypeWS) {
            paymentMethodTypeWS.setMethodName(prefix + Calendar.getInstance().getTimeInMillis());
            api.updatePaymentMethodType(paymentMethodTypeWS);
        }
    }

    private MetaFieldWS buildAndPersistMetafield(String name, DataType dataType, MetaFieldType fieldUsage,
            Integer displayOrder, MetaFieldValueWS defaultValue, boolean disabled, boolean mandatory) {
        return new MetaFieldBuilder().name(name).dataType(dataType).entityType(EntityType.PAYMENT_METHOD_TYPE)
                .fieldUsage(fieldUsage).displayOrder(displayOrder).defaultValue(defaultValue).disabled(disabled)
                .mandatory(mandatory).build();
    }

    private void removeUnwantedMetafields(JbillingAPI api, Integer... paymentMethodIds) {
        for (Integer id : paymentMethodIds) {
            PaymentMethodTypeWS methodType = api.getPaymentMethodType(id);
            MetaFieldWS[] metaFieldWSs = methodType.getMetaFields();
            List<MetaFieldWS> newMetaFields = new ArrayList<>();
            for (MetaFieldWS temp : metaFieldWSs) {
                if (temp.getName().equalsIgnoreCase(BT_ID) || temp.getName().equalsIgnoreCase(TYPE)) {
                    newMetaFields.add(temp);
                }
            }
            MetaFieldWS[] metaFieldWS2 = new MetaFieldWS[newMetaFields.size()];
            metaFieldWS2 = newMetaFields.toArray(metaFieldWS2);
            methodType.setMetaFields(metaFieldWS2);
            api.updatePaymentMethodType(methodType);
        }
    }

}
