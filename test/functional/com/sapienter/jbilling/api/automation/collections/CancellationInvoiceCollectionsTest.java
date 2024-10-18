package com.sapienter.jbilling.api.automation.collections;

import org.apache.commons.lang.ArrayUtils;
import org.testng.annotations.Test;
import static com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper.*;
import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.task.CancellationInvoiceAgeingTask;
import com.sapienter.jbilling.server.user.CancellationRequestWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.util.Constants;
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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * JBFC-789 Test cases for collections and ageing.
 *
 * @author Ashwinkumar Patra
 * @since 29-JUN-2016
 */


@Test(groups = {"api-automation"}, testName = "CancellationInvoiceCollectionsTest")
public class CancellationInvoiceCollectionsTest {

    private static final Logger logger = LoggerFactory
            .getLogger(CancellationInvoiceCollectionsTest.class);
    private EnvironmentHelper environmentHelper;
    private TestBuilder testBuilder;

    // Fixed constants for now
    private static final Integer INVOICE_STATUS_UNPAID = 2;

    private static final String PAYMENT_DUE_STEP = "Ageing Day 1";
    private static final String GRACE_PERIOD_STEP = "Ageing Day 4";
    private static final String FIRST_RETRY_STEP = "Ageing Day 5";
    private static final String SUSPENDED_STEP = "Ageing Day 15";
    private static final String TREAT_TO_COLLECTIONS = "Threat to Collections";
    private static final String SENT_TO_3RD_PARTY_COLLECTIONS = "Sent to 3rd Party Collections";

    private static final String CATEGORY_CODE = "TestCategory";
    private static final String PRODUCT_CODE = "TestProduct";
    private static final String ACCOUNT_TYPE_CODE = "TestCollectionsAccount";
    private static final String CUSTOMER_CODE = "TestCustomer-Harshad";
    private static final String CUSTOMER_CODE2 = "TestCustomer-Harshad";
    private static final String CUSTOMER_CODE3 = "TestCustomer-payment-01";
    private static final String CUSTOMER_CODE4 = "TestCustomer-payment-02";
    private static final String PAYMENT_TYPE_CODE = "TestCCPaymentType";

    private static final String CC_CARD_HOLDER_NAME_MF = "cc.cardholder.name";
    private static final String CC_NUMBER_MF = "cc.number";
    private static final String CC_EXPIRY_DATE_MF = "cc.expiry.date";
    private static final String CC_TYPE_MF = "cc.type";
    private static final String CC_GATEWAY_KEY_MF = "cc.gateway.key";
    private final static String CREDIT_CARD_NUMBER = "4111111111111152";
    private final static Integer CC_PM_ID = 5;

    private static final Date initialNextInvoiceDate = new LocalDateTime(2017, 8, 1, 0, 0).toDate();
    private static final Date activeSinceDate = new LocalDateTime(2017, 7, 1, 0, 0).toDate();
    private static Integer orderId = null;
    private static final String USER_CANCELLATION_STATUS = "Cancelled on Request";
    private static final String GENERATE_CANCELLATION_INVOICE_TASK_CLASS_NAME = "com.sapienter.jbilling.server.billing.task.GenerateCancellationInvoiceTask";
    private static final String PAYMENT_FAKE_TASK_CLASS_NAME = "com.sapienter.jbilling.server.pluggableTask.PaymentFakeTask";
    private static final String PAYMENT_FILTER_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask";
    private final String reasonText = "User has requested to cancel subscription orders";
    private Integer paymentFilterTaskId;

    @BeforeClass
    public void initializeTests() {
        testBuilder = getPluginsAndItemSetup();
    }


    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        PluggableTaskWS paymentFakeTask = api.getPluginWS(paymentFilterTaskId);
        Hashtable<String, String> parameters;
        paymentFakeTask.setProcessingOrder(1);
        paymentFakeTask.setTypeId(api.getPluginTypeWSByClassName(PAYMENT_FILTER_TASK_CLASS_NAME).getId());
        for(Integer userId : api.getUsersNotInStatus(1)) {
            updateCustomerStatusToActive(userId, api);
        }
        parameters = new Hashtable<>();
        parameters.put("enable_filter_user_id", "true");
        parameters.put("enable_filter_name", "true");
        parameters.put("enable_filter_cc_number", "true");
        parameters.put("enable_filter_address", "true");
        parameters.put("enable_filter_ip_address", "true");
        parameters.put("enable_filter_phone_numbe", "true");
        parameters.put("ip_address_ccf_id", "3");
        paymentFakeTask.setParameters(parameters);
        api.updatePlugin(paymentFakeTask);
        if (null != environmentHelper) {
            environmentHelper = null;
        }
        if (null != testBuilder) {
            testBuilder = null;
        }
    }


    private TestBuilder getPluginsAndItemSetup() {
        return TestBuilder.newTest(false).givenForMultiple(envCreator -> {

            final JbillingAPI api = envCreator.getPrancingPonyApi();

            environmentHelper = EnvironmentHelper.getInstance(api);

            ConfigurationBuilder configurationBuilder = envCreator
                    .configurationBuilder(api);
            createPluginConfig(api, configurationBuilder, findStepIdByStatus(api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR),
                    GRACE_PERIOD_STEP), environmentHelper.getUserOverdueNotificationId(api));
            configurationBuilder.addPlugin(CancellationInvoiceAgeingTask.class.getName());
            configurationBuilder.build();

            Integer categoryId = envCreator.itemBuilder(api)
                    .itemType()
                    .withCode(CATEGORY_CODE)
                    .global(false).build();

            envCreator.itemBuilder(api)
                    .item()
                    .withCode(PRODUCT_CODE).global(false)
                    .withType(categoryId)
                    .withFlatPrice("0.50")
                    .build();

            Integer paymentTypeId = envCreator
                    .paymentMethodTypeBuilder(api, PAYMENT_TYPE_CODE)
                    .withMethodName(PAYMENT_TYPE_CODE)
                    .withOwningEntityId(api.getCallerCompanyId())
                    .withTemplateId(-1)
                    .allAccountType(true)
                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_CARD_HOLDER_NAME_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.TITLE, null))
                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_NUMBER_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.PAYMENT_CARD_NUMBER, buildValidationRule(true, "PAYMENT_CARD", new String[]{"Payment card number is not valid"}, api, null)))
                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_EXPIRY_DATE_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.DATE, buildValidationRule(true, "REGEX", new String[]{"Expiry date should be in format MM/yyyy"},
                                    api, buildRuleAttribute("regularExpression", "(?:0[1-9]|1[0-2])/[0-9]{4}"))))
                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_TYPE_MF, DataType.STRING, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.CC_TYPE, null))
                    .addMetaField(ApiBuilderHelper.getMetaFieldWithValidationRule(CC_GATEWAY_KEY_MF, DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                            api.getCallerCompanyId(), MetaFieldType.GATEWAY_KEY, null))
                    .build().getId();

            Hashtable<String, String> parameters = new Hashtable<String, String>();
            PluggableTaskWS paymentFakeTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(PAYMENT_FILTER_TASK_CLASS_NAME).getId());
            paymentFilterTaskId = paymentFakeTask.getId();
            paymentFakeTask.setProcessingOrder(1);
            paymentFakeTask.setTypeId(api.getPluginTypeWSByClassName(PAYMENT_FAKE_TASK_CLASS_NAME).getId());

            parameters = new Hashtable<String, String>();
            parameters.put("all", "yes");
            parameters.put("processor_name", "first_fake_processor");
            paymentFakeTask.setParameters(parameters);
            api.updatePlugin(paymentFakeTask);

            // Build account type
            envCreator.accountTypeBuilder(api)
                    .withName(ACCOUNT_TYPE_CODE)
                    .withPaymentMethodTypeIds(new Integer[]{paymentTypeId})
                    .build();

        });
    }

    @Test(priority = 1)
    public void test001CollectionStepCheckTest() {
        /** In this test we are only checking if the configuration is correct for
         the Collection Steps**/

        testBuilder.test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            api.saveAgeingConfigurationWithCollectionType(buildCancellationAgeingSteps(api), api.getCallerLanguageId(), CollectionType.CANCELLATION_INVOICE);
            AgeingWS[] ageingList = api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR);

            //debug trace
            for (AgeingWS element : ageingList) {
                logger.debug("Ageing List :::::::::::::::: {}", element);
            }
            validateStep(ageingList[0], PAYMENT_DUE_STEP, 1, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
            validateStep(ageingList[1], GRACE_PERIOD_STEP, 4, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[2], FIRST_RETRY_STEP, 5, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
            validateStep(ageingList[3], SUSPENDED_STEP, 15, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);

            AgeingWS[] ageingList2 = api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.CANCELLATION_INVOICE);
            //debug trace
            for (AgeingWS element : ageingList2) {
                logger.debug("Ageing List :::::::::::::::: {}", element);
            }
            validateStep(ageingList2[0], TREAT_TO_COLLECTIONS, 6, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList2[1], SENT_TO_3RD_PARTY_COLLECTIONS, 18, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
        });
    }


    /**
     * Verify if CancellationInvoiceAgeingTask plugin is configured, cancellation collection steps will be  run by the ageing process
     */
    @Test(priority = 2)
    public void test001StatusTransitions() {

        final Date activeSince = new DateTime(activeSinceDate).toDate();
        testBuilder
                .given(envBuilder -> {

                    JbillingAPI api = envBuilder.getPrancingPonyApi();
                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince, null, false);
                    orderId = envBuilder.orderBuilder(api)
                            .forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                            .withActiveSince(activeSince)
                            .withEffectiveDate(activeSince)
                            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(1)
                            .withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                            .build();
                    UserWS userWS = api.getUserWS(customerId);
                    userWS.setNextInvoiceDate(initialNextInvoiceDate);
                    api.updateUser(userWS);

                })
                .test((env) -> {

                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    assertNotNull(customerId, "CustomerId should not be null");

                    CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
                    api.createCancellationRequest(crWS);
                    assertEquals(TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()), TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(6, 16, 2017)),
                            "Cancellation date was not set correctly for customer");


                    try {
                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), "Active");
                        Integer pluginId = configurePluginSetCronExpression(api);
                        logger.debug("pluging configured with id {}", pluginId);
                        pause(60000);
                        api.deletePlugin(pluginId);
                        Integer[] invoiceIds = api.getLastInvoices(userId, 1);

                        //logger.debug("CancellationInvoiceCollectionsTest.test002StatusTransitions()--invoice id--{}", invoiceIds[0]);
                        invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                        assertNotNull(invoiceWS, "Invoice is not created.");
                        assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                        UserWS user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, "User's status should be: ");

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(7).toDate());
                        validateUserAndInvoiceStatus(userId, TREAT_TO_COLLECTIONS, INVOICE_STATUS_UNPAID, api);

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(19).toDate());
                        validateUserAndInvoiceStatus(userId, SENT_TO_3RD_PARTY_COLLECTIONS, INVOICE_STATUS_UNPAID, api);

                    } finally {
                        if (invoiceWS != null) {
                            api.deleteInvoice(invoiceWS.getId());
                        }
                        api.deleteOrder(orderId);
                        updateCustomerStatusToActive(env.idForCode(CUSTOMER_CODE), api);
                        api.deleteUser(env.idForCode(CUSTOMER_CODE));
                    }
                });
    }

    /**
     * Verify if CancellationInvoiceAgeingTask plugin is configured, cancellation collection steps are not configured
     * The invoice should age as per the basic collection steps
     */
    @Test(priority = 0)
    public void test002StatusTransitions() {

        final Date activeSince = new DateTime(activeSinceDate).toDate();
        testBuilder
                .given(envBuilder -> {

                    JbillingAPI api = envBuilder.getPrancingPonyApi();
                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince, null, false);
                    orderId = envBuilder.orderBuilder(api)
                            .forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                            .withActiveSince(activeSince)
                            .withEffectiveDate(activeSince)
                            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(1)
                            .withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                            .build();
                    UserWS userWS = api.getUserWS(customerId);
                    userWS.setNextInvoiceDate(initialNextInvoiceDate);
                    api.updateUser(userWS);

                })
                .test((env) -> {

                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    assertNotNull(customerId, "CustomerId should not be null");

                    CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
                    api.createCancellationRequest(crWS);
                    assertEquals(TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()), TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(6, 16, 2017)),
                            "Cancellation date was not set correctly for customer");
                    Integer[] invoiceIds = null;
                    try {
                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), "Active");
                        Integer pluginId = configurePluginSetCronExpression(api);
                        logger.debug("pluging configured with id {}", pluginId);
                        pause(60000);
                        api.deletePlugin(pluginId);
                        invoiceIds = api.getLastInvoices(userId, 1);

                        //logger.debug("CancellationInvoiceCollectionsTest.test002StatusTransitions()--invoice id--{}", invoiceIds[0]);
                        invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                        assertNotNull(invoiceWS, "Invoice is not created.");
                        assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                        UserWS user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, "User's status should be: ");

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(2).toDate());
                        validateUserAndInvoiceStatus(userId, PAYMENT_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(5).toDate());
                        validateUserAndInvoiceStatus(userId, GRACE_PERIOD_STEP, INVOICE_STATUS_UNPAID, api);

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(6).toDate());
                        validateUserAndInvoiceStatus(userId, FIRST_RETRY_STEP, INVOICE_STATUS_UNPAID, api);

                    } finally {
                        if (invoiceIds != null) {
                            for (Integer invoiceId : invoiceIds) {
                                api.deleteInvoice(invoiceId);
                            }
                        }
                        api.deleteOrder(orderId);
                        updateCustomerStatusToActive(userId, api);
                        api.deleteUser(userId);
                    }
                });
    }

    /**
     * Verify if CancellationInvoiceAgeingTask plugin is not configured, basic ageing steps are run by the ageing process
     */
    @Test(priority = 5)
    public void test003StatusTransitions() {

        final Date activeSince = new DateTime(activeSinceDate).toDate();
        testBuilder
                .given(envBuilder -> {

                    JbillingAPI api = envBuilder.getPrancingPonyApi();
                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE2, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince, null, false);
                    orderId = envBuilder.orderBuilder(api)
                            .forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                            .withActiveSince(activeSince)
                            .withEffectiveDate(activeSince)
                            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(1)
                            .withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                            .build();
                    UserWS userWS = api.getUserWS(customerId);
                    userWS.setNextInvoiceDate(initialNextInvoiceDate);
                    api.updateUser(userWS);

                })
                .test((env) -> {

                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE2);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    assertNotNull(customerId, "CustomerId should not be null");

                    CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
                    api.createCancellationRequest(crWS);

                    assertEquals(TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()), TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(6, 16, 2017)),
                            "Cancellation date was not set correctly for customer");

                    try {
                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), "Active");
                        Integer[] invoiceIds = api.createInvoice(userId, true);

                        invoiceWS = api.getInvoiceWS(invoiceIds[0]);

                        assertNotNull(invoiceWS, "Invoice is not created.");
                        assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                        PluggableTaskWS[] pluggableTaskWSs = api.getPluginsWS(1, "com.sapienter.jbilling.server.process.task.CancellationInvoiceAgeingTask");
                        if (ArrayUtils.isNotEmpty(pluggableTaskWSs)) {
                            api.deletePlugin(pluggableTaskWSs[0].getId());
                        }
                        UserWS user = api.getUserWS(userId);
                        assertNotNull(user, "User should not be not null.");
                        api.triggerAgeing(new LocalDate(new Date()).plusDays(2).toDate());
                        validateUserAndInvoiceStatus(userId, PAYMENT_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(5).toDate());
                        validateUserAndInvoiceStatus(userId, GRACE_PERIOD_STEP, INVOICE_STATUS_UNPAID, api);

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(6).toDate());
                        validateUserAndInvoiceStatus(userId, FIRST_RETRY_STEP, INVOICE_STATUS_UNPAID, api);

                    } finally {
                        if (invoiceWS != null) {
                            api.deleteInvoice(invoiceWS.getId());
                        }
                        api.deleteOrder(orderId);
                        updateCustomerStatusToActive(env.idForCode(CUSTOMER_CODE), api);
                        api.deleteUser(env.idForCode(CUSTOMER_CODE));
                    }
                });
    }

    /**
     * Verify customer status remains Cancelled on request after making full payment of last invoice
     */
    @Test(priority = 3)
    public void test004UserStatus() {

        final Date activeSince = new DateTime(activeSinceDate).toDate();
        testBuilder
                .given(envBuilder -> {

                    JbillingAPI api = envBuilder.getPrancingPonyApi();
                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE3, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince, null, false);
                    orderId = envBuilder.orderBuilder(api)
                            .forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                            .withActiveSince(activeSince)
                            .withEffectiveDate(activeSince)
                            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(1)
                            .withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                            .build();
                    UserWS userWS = api.getUserWS(customerId);
                    userWS.setNextInvoiceDate(initialNextInvoiceDate);
                    api.updateUser(userWS);

                })
                .test((env) -> {

                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE3);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    assertNotNull(customerId, "CustomerId should not be null");

                    CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
                    api.createCancellationRequest(crWS);
                    assertEquals(TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()), TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(6, 16, 2017)),
                            "Cancellation date was not set correctly for customer");

                    try {
                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), "Active");
                        Integer pluginId = configurePluginSetCronExpression(api);
                        logger.debug("pluging configured with id {}", pluginId);
                        pause(120000);
                        api.deletePlugin(pluginId);
                        Integer[] invoiceIds = api.getLastInvoices(userId, 1);
                        //logger.debug("CancellationInvoiceCollectionsTest.test002StatusTransitions()--invoice id--{}", invoiceIds[0]);
                        invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                        assertNotNull(invoiceWS, "Invoice is not created.");
                        assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                        String invoiceBalance = invoiceWS.getTotal();

                        UserWS user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, "User's status should be: ");

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(7).toDate());
                        validateUserAndInvoiceStatus(userId, TREAT_TO_COLLECTIONS, INVOICE_STATUS_UNPAID, api);

                        Calendar paymentDate = Calendar.getInstance();
                        paymentDate.set(Calendar.YEAR, 2017);
                        paymentDate.set(Calendar.MONTH, 9);
                        paymentDate.set(Calendar.DAY_OF_MONTH, 25);

                        // Test user status after full payment
                        makePaymentCancellationInvoice(invoiceBalance, paymentDate.getTime(), false, CUSTOMER_CODE3, invoiceWS.getId());

                        user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, "Finally user's status should be: ");

                    } finally {
                        if (invoiceWS != null) {
                            api.deleteInvoice(invoiceWS.getId());
                        }
                        api.deleteOrder(orderId);
                        updateCustomerStatusToActive(env.idForCode(CUSTOMER_CODE3), api);
                        api.deleteUser(env.idForCode(CUSTOMER_CODE3));
                    }
                });
    }

    /**
     * 1. Verify customer status after one regular ageing and then requested for cancellation.
     * 2. Next ageing will needs to pick cancellation steps.
     * 3. Customer remains Cancelled on request after making full payment of last invoice
     */
    @Test(priority = 4)
    public void test005UserStatus() {

        final Date activeSince = new DateTime(activeSinceDate).toDate();
        testBuilder
                .given(envBuilder -> {

                    JbillingAPI api = envBuilder.getPrancingPonyApi();
                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE4, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince, null, false);
                    orderId = envBuilder.orderBuilder(api)
                            .forUser(customerId)
                            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                            .withActiveSince(activeSince)
                            .withEffectiveDate(activeSince)
                            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(1)
                            .withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                            .build();
                    UserWS userWS = api.getUserWS(customerId);
                    userWS.setNextInvoiceDate(initialNextInvoiceDate);
                    api.updateUser(userWS);

                })
                .test((env) -> {

                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE4);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    assertNotNull(customerId, "CustomerId should not be null");

                    Integer[] invoiceIds = api.createInvoiceWithDate(userId, initialNextInvoiceDate, null, null, false);

                    try {
                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), "Active");

                        //logger.debug("CancellationInvoiceCollectionsTest.test002StatusTransitions()--invoice id--{}", invoiceIds[0]);
                        invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                        assertNotNull(invoiceWS, "Invoice is not created.");
                        assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                        String invoiceBalance = invoiceWS.getTotal();

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(2).toDate());
                        validateUserAndInvoiceStatus(userId, PAYMENT_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                        CancellationRequestWS crWS = constructCancellationRequestWS(addDays(invoiceWS.getDueDate(), 1), customerId, reasonText);
                        api.createCancellationRequest(crWS);
                        assertEquals(TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()), TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(7, 03, 2017)),
                                "Cancellation date was not set correctly for customer");

                        //check if we can create multiple cancellation request or not.
                        CancellationRequestWS crWS1 = constructCancellationRequestWS(addDays(invoiceWS.getDueDate(), 5), customerId, reasonText);
                        try {
                            api.createCancellationRequest(crWS1);
                            assertEquals(true, false, "Can not create more than one cancellation request");
                        } catch (SessionInternalError e) {
                            logger.debug("Cancelled request not created with id {}.", crWS1.getId());
                        }

                        Integer pluginId = configurePluginSetCronExpression(api);
                        logger.debug("pluging configured with id {}", pluginId);
                        pause(120000);
                        api.deletePlugin(pluginId);

                        UserWS user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, "User's status should be: ");

                        api.triggerAgeing(new LocalDate(new Date()).plusDays(7).toDate());
                        validateUserAndInvoiceStatus(userId, TREAT_TO_COLLECTIONS, INVOICE_STATUS_UNPAID, api);

                        Calendar paymentDate = Calendar.getInstance();
                        paymentDate.set(Calendar.YEAR, 2017);
                        paymentDate.set(Calendar.MONTH, 9);
                        paymentDate.set(Calendar.DAY_OF_MONTH, 25);

                        // Test user status after full payment
                        makePaymentCancellationInvoice(invoiceBalance, paymentDate.getTime(), false, CUSTOMER_CODE4, invoiceWS.getId());

                        user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, "Finally user's status should be: ");

                    } finally {
                        if (invoiceWS != null) {
                            api.deleteInvoice(invoiceWS.getId());
                        }
                        api.deleteOrder(orderId);
                        updateCustomerStatusToActive(env.idForCode(CUSTOMER_CODE4), api);
                        api.deleteUser(env.idForCode(CUSTOMER_CODE4));
                    }
                });
    }

    private void validateUserAndInvoiceStatus(Integer userId, String userStatus, Integer invoiceStatus, JbillingAPI api) {
        UserWS user = api.getUserWS(userId);
        assertEquals(user.getStatus(), userStatus);
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertEquals(invoice.getStatusId(), invoiceStatus);
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder, String code, Integer accountTypeId, Date nid, Integer paymentTypeId, boolean goodCC) {
        final JbillingAPI api = envBuilder.getPrancingPonyApi();

        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                .withUsername(code)
                .withAccountTypeId(accountTypeId)
                .withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), 1));

        if (null != paymentTypeId) {
            char[] date = DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT)
                    .print(Util.truncateDate(new LocalDate().plusYears(10).toDate()).getTime())
                    .toCharArray();
            customerBuilder.addPaymentInstrument(customerBuilder.paymentInformation()
                    .withProcessingOrder(1)
                    .withPaymentMethodId(Constants.PAYMENT_METHOD_VISA)
                    .withPaymentMethodTypeId(paymentTypeId)
                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_CARD_HOLDER_NAME_MF, CUSTOMER_CODE.toCharArray()))
                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_NUMBER_MF, goodCC ? "4111111111111152".toCharArray() : "4111111111111111".toCharArray()))
                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_EXPIRY_DATE_MF, date))
                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_TYPE_MF, CreditCardType.VISA))
                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_GATEWAY_KEY_MF, "zzzxxxaaa".toCharArray())).build());
        }

        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nid);
        api.updateUser(user);
        return user.getId();
    }

    private ValidationRuleWS buildValidationRule(boolean enabled,
                                                 String ruleType, String[] errorMsgs, JbillingAPI api,
                                                 SortedMap<String, String> ruleAttrs) {
        ValidationRuleWS validationRule = new ValidationRuleWS();
        validationRule.setEnabled(enabled);
        validationRule.setRuleType(ruleType);
        for (String errorMsg : errorMsgs) {
            validationRule.addErrorMessage(api.getCallerLanguageId(), errorMsg);
        }
        validationRule.setRuleAttributes(ruleAttrs);

        return validationRule;
    }

    private SortedMap<String, String> buildRuleAttribute(String key,
                                                         String value) {
        SortedMap<String, String> atts = new TreeMap<>();
        atts.put(key, value);
        return atts;
    }

    private AgeingWS[] buildCancellationAgeingSteps(JbillingAPI api) {
        AgeingWS[] ageingSteps = new AgeingWS[2];
        ageingSteps[0] = buildAgeingStep(TREAT_TO_COLLECTIONS,
                6, false, false, false, api,
                CollectionType.CANCELLATION_INVOICE);
        ageingSteps[1] = buildAgeingStep(SENT_TO_3RD_PARTY_COLLECTIONS,
                18, true, false, false, api,
                CollectionType.CANCELLATION_INVOICE);
        return ageingSteps;
    }

    private void createPluginConfig(JbillingAPI api,
                                    ConfigurationBuilder configurationBuilder, Integer stepIdForPlugin,
                                    Integer notificationId) {
        String plugin = "com.sapienter.jbilling.server.user.tasks.UserAgeingNotificationTask";

        Hashtable<String, String> pluginParameters = new Hashtable<>();
        pluginParameters.put(String.valueOf(stepIdForPlugin),
                String.valueOf(notificationId));
        pluginParameters.put("0", "");
        addPluginWithParametersIfAbsent(configurationBuilder, plugin,
                api.getCallerCompanyId(), pluginParameters);
    }

    private AgeingWS buildAgeingStep(String statusStep, Integer days, boolean payment, boolean sendNotification, boolean suspended, JbillingAPI api, CollectionType collectionType) {

        AgeingWS ageingWS = new AgeingWS();
        ageingWS.setEntityId(api.getCallerCompanyId());
        ageingWS.setStatusStr(statusStep);
        ageingWS.setDays(days);
        ageingWS.setPaymentRetry(payment);
        ageingWS.setSendNotification(sendNotification);
        ageingWS.setSuspended(suspended);
        ageingWS.setStopActivationOnPayment(false);
        ageingWS.setCollectionType(collectionType);
        return ageingWS;
    }

    private void validateStep(AgeingWS ageingWS, String statusStr, Integer days, Boolean payment, Boolean sendNotification, Boolean suspended) {

        assertEquals(ageingWS.getStatusStr(), statusStr, "Invalid Step name");
        assertEquals(ageingWS.getDays(), days, "Invalid number of days");
        assertEquals(ageingWS.getPaymentRetry(), payment, "Invalid payment check");
        assertEquals(ageingWS.getSendNotification(), sendNotification, "Invalid notification check");
        assertEquals(ageingWS.getSuspended(), suspended, "Invalid suspended check");
    }

    public Integer buildAndPersistOrderPeriod(
            TestEnvironmentBuilder envBuilder, JbillingAPI api,
            String description, Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api).withDescription(description)
                .withValue(value).withUnitId(unitId).build();
    }

    private void addPluginWithParametersIfAbsent(
            ConfigurationBuilder configurationBuilder, String pluginClassName,
            Integer entityId, Hashtable<String, String> parameters) {

        if (!configurationBuilder.pluginExists(pluginClassName, entityId)) {
            configurationBuilder.addPluginWithParameters(pluginClassName,
                    parameters);
        }
    }

    private Integer findStepIdByStatus(AgeingWS[] ageingSteps, String status) {
        for (AgeingWS ageingWS : ageingSteps) {
            if (ageingWS.getStatusStr().equalsIgnoreCase(status)) {
                return ageingWS.getStatusId();
            }
        }
        throw new AssertionError("Should be found!");
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api) {

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(1);
        user.setStatus("Active");
        user.setPassword(null);
        api.updateUser(user);
    }

    private void pause(long t) {
        logger.debug("pausing for {} ms...", t);
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Integer configurePluginSetCronExpression(JbillingAPI api) {

        PluggableTaskWS invoiceBillingProcessLinkingTask = new PluggableTaskWS();
        invoiceBillingProcessLinkingTask.setProcessingOrder(10);
        PluggableTaskTypeWS invoiceBillingProcessLinkingTaskType =
                api.getPluginTypeWSByClassName(GENERATE_CANCELLATION_INVOICE_TASK_CLASS_NAME);
        invoiceBillingProcessLinkingTask.setTypeId(invoiceBillingProcessLinkingTaskType.getId());

        invoiceBillingProcessLinkingTask.setParameters(new Hashtable<String, String>(invoiceBillingProcessLinkingTask.getParameters()));
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("cron_exp", "0 0/1 * 1/1 * ? *");
        invoiceBillingProcessLinkingTask.setParameters(parameters);
        logger.debug("Running Cancellation Task.... with parameter as cron_exp 0 0/1 * 1/1 * ? *");
        return api.createPlugin(invoiceBillingProcessLinkingTask);
    }

    private void makePaymentCancellationInvoice(String amount, Date paymentDate, boolean isRefund, String customerCode, Integer invoiceId) {

        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal(amount));
        payment.setIsRefund(isRefund ? 1 : 0);
        payment.setPaymentDate(paymentDate);
        payment.setCreateDatetime(paymentDate);
        payment.setCurrencyId(1);
        payment.setUserId(testBuilder.getTestEnvironment().idForCode(customerCode));
        PaymentWS lastPayment = testBuilder.getTestEnvironment().getPrancingPonyApi().getLatestPayment(payment.getUserId());
        payment.setPaymentId(isRefund ? lastPayment.getId() : CC_PM_ID);
        payment.setResultId(Constants.RESULT_ENTERED);
        Calendar expiryDate = Calendar.getInstance();
        expiryDate.add(Calendar.YEAR, 10);

        payment.setPaymentInstruments(Arrays.asList(createCreditCard(UUID.randomUUID().toString(), CREDIT_CARD_NUMBER, expiryDate.getTime(), 2)));

        PaymentAuthorizationDTOEx authInfo = testBuilder.getTestEnvironment().getPrancingPonyApi().processPayment(payment, invoiceId);
        assertNotNull(authInfo, "Payment result not null");
    }

    private PaymentInformationWS createCreditCard(String cardHolderName,
                                                  String cardNumber, Date date, Integer methodId) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(CC_PM_ID);
        cc.setProcessingOrder(1);
        cc.setPaymentMethodId(methodId);

        //cc
        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CC_CARD_HOLDER_NAME_MF, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
        addMetaField(metaFields, CC_NUMBER_MF, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
        addMetaField(metaFields, CC_EXPIRY_DATE_MF, false, true,
                DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());

        // have to pass meta field card type for it to be set
        addMetaField(metaFields, CC_TYPE_MF, false, false,
                DataType.STRING, 4, CreditCardType.MASTER_CARD);
        addMetaField(metaFields, "cc.gateway.key", false, true, DataType.CHAR, 5, null);
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    private void addMetaField(List<MetaFieldValueWS> metaFields,
                              String fieldName, boolean disabled, boolean mandatory,
                              DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.setDisabled(disabled);
        ws.setMandatory(mandatory);
        ws.setDataType(dataType);
        ws.setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }
}