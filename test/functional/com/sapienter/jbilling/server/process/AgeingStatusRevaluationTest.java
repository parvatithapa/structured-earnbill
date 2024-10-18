package com.sapienter.jbilling.server.process;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.task.CancellationInvoiceAgeingTask;
import com.sapienter.jbilling.server.user.CancellationRequestWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;


@Test(groups = {"test-ageing-revaluation","ageing-revaluation"}, testName = "AgeingStatusRevaluationTest")
public class AgeingStatusRevaluationTest {

    private static final Logger logger = LoggerFactory
            .getLogger(AgeingStatusRevaluationTest.class);
    private static final int AGEING_REVALUATION_PAYMENT_PREFERENCE = 88;
    private EnvironmentHelper environmentHelper;
    private TestBuilder testBuilder;

    // Fixed constants for now
    private static final Integer INVOICE_STATUS_UNPAID = Integer.valueOf(2);

    // Collection Steps
    private static final String ACTIVE                              = "Active";
    private static final String ACTIVE_PAST_DUE_STEP                = "Active | Past Due";
    private static final String ACTIVE_NOTICE_TO_BLOCK_STEP         = "Active | Notice to Block";
    private static final String ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP = "Active | Blocked for Non-Payment";
    private static final String CANCELLED_CUSTOMER_REQUEST_STEP     = "Cancelled | Customer Request";
    private static final String CANCELLED_NOTICE_TO_COLLECTION_STEP = "Cancelled | Notice to Collection";
    private static final String CANCELLED_TP_COLLECTIONS_STEP       = "Cancelled | TP Collections";
    private static final String USER_CANCELLATION_STATUS            = "Cancelled on Request";
    private static final String ACTIVE_OVERDUE_STEP                 = "Overdue";
    private static final String ACTIVE_COLLECTIONS_STEP             = "Collections";
    private static final String ACTIVE_REMOVE_STEP                  = "Remove";
    private static final String ACTIVE_SECOND_RETRY_STEP            = "Second Retry";

    private static final String CATEGORY_CODE          = "TestCategory";
    private static final String PRODUCT_CODE           = "TestProduct";
    private static final String CREDIT_PRODUCT_CODE    = "TestCreditProduct";
    private static final String CREDIT_PRODUCT_CODE_2  = "TestCreditProduct2";
    private static final String ACCOUNT_TYPE_CODE      = "TestCollectionsAccount";
    private static final String CUSTOMER_CODE          = "TestCustomer";
    private static final String PAYMENT_TYPE_CODE      = "TestCCPaymentType";

    private static final String CC_CARD_HOLDER_NAME_MF = "cc.cardholder.name";
    private static final String CC_NUMBER_MF           = "cc.number";
    private static final String CC_EXPIRY_DATE_MF      = "cc.expiry.date";
    private static final String CC_TYPE_MF             = "cc.type";
    private static final String CC_GATEWAY_KEY_MF      = "cc.gateway.key";

    private static final String CREDIT_NOTE_IS_NOT_CREATED      = "credit note is not created.";
    private static final String CREDIT_NOTE_SHOULD_BE_GENERATED = "Credit Note should be generated";
    private static final String USERS_STATUS_SHOULD_BE          = "User's status should be ";
    private static final String INVALID_NUMBER_ORDER_LINES      = "Invalid number Order Lines";
    private static final String INVOICE_SHOULD_BE_GENERATED     = "Invoice should be generated";
    private static final String INVOICE_IS_NOT_CREATED          = "Invoice is not created.";

    public final static int ONE_TIME_ORDER_PERIOD        = 1;
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;

    private static final String GENERATE_CANCELLATION_INVOICE_TASK_CLASS_NAME =
                                "com.sapienter.jbilling.server.billing.task.GenerateCancellationInvoiceTask";

    private static Integer paymentMethodTypeId = null;

    @BeforeClass
    public void initializeTests(){
        cleanAgeingSteps();
        testBuilder = getPluginsAndItemSetup();
    }

    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        updateAgedUsersStatusToActive(api);
        PaymentMethodTypeWS paymentMethodTypeWS = api.getPaymentMethodType(paymentMethodTypeId);
        if(null != paymentMethodTypeWS) {
            paymentMethodTypeWS.setMethodName("pm" + Calendar.getInstance().getTimeInMillis());
            api.updatePaymentMethodType(paymentMethodTypeWS);
        }
        if(null!= environmentHelper) {
            environmentHelper = null;
        }
        if (null != testBuilder) {
            testBuilder = null;
        }
    }

    private void cleanAgeingSteps() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            AgeingWS[] ageingWSs = api.getAgeingConfiguration(api.getCallerLanguageId());
            for(AgeingWS ageingTemp : ageingWSs){
                if(ageingTemp.getInUse()){
                    updateAgedUsersStatusToActive(api);
                    logger.debug("status id : {}  days : {}  in use : {}",ageingTemp.getStatusId(), ageingTemp.getDays(), ageingTemp.getInUse());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

    private TestBuilder getPluginsAndItemSetup(){
        return TestBuilder.newTest().givenForMultiple(envCreator -> {

            final JbillingAPI api = envCreator.getPrancingPonyApi();

            environmentHelper = EnvironmentHelper.getInstance(api);

            ConfigurationBuilder configurationBuilder = envCreator.configurationBuilder(api);

            api.saveAgeingConfigurationWithCollectionType(buildAgeingSteps(api), api.getCallerLanguageId(), CollectionType.REGULAR);
            configurationBuilder.build();
            configurationBuilder.updatePreference(api, AGEING_REVALUATION_PAYMENT_PREFERENCE, "1");

            Integer categoryId = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(false).build();
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(false).withType(categoryId)
                    .withFlatPrice("0.50").build();
            envCreator.itemBuilder(api).item().withCode(CREDIT_PRODUCT_CODE).global(false).withType(categoryId)
                    .withFlatPrice("-0.50").build();
            envCreator.itemBuilder(api).item().withCode(CREDIT_PRODUCT_CODE_2).global(false).withType(categoryId)
                    .withFlatPrice("-0.30").build();

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

            paymentMethodTypeId = paymentTypeId;
            envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).withPaymentMethodTypeIds(new Integer[]{paymentTypeId}).build();
        });
    }

    /**
     * Test collection steps configuration setup 
     * 
    */
    @Test(priority = 0, enabled=true)
    public void testCollectionStepCheckTest(){

        testBuilder.test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            AgeingWS[] ageingList = api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR);

            validateStep(ageingList[0], ACTIVE_PAST_DUE_STEP, Integer.valueOf(1), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[1], ACTIVE_NOTICE_TO_BLOCK_STEP, Integer.valueOf(3), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[2], ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, Integer.valueOf(18), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[3], CANCELLED_CUSTOMER_REQUEST_STEP, Integer.valueOf(100), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.TRUE);
            validateStep(ageingList[4], CANCELLED_NOTICE_TO_COLLECTION_STEP, Integer.valueOf(125), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.TRUE);
            validateStep(ageingList[5], CANCELLED_TP_COLLECTIONS_STEP, Integer.valueOf(150), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.TRUE);
        });
    }

    @Test(priority = 1, enabled=true)
    public void testStatusTransitions(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -4)),01,getYear(addMonths(today, -4)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    activeSince, null, false);

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

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            try {
                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);

                Integer[] invoiceIds = api.createInvoiceWithDate(userId, nextInvoiceDate, 3, 21, false);
                assertEquals(invoiceIds.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                assertNotNull(invoiceWS, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                final Date invoiceDueDate = invoiceWS.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(18).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(100).toDate());
                validateUserAndInvoiceStatus(userId, CANCELLED_CUSTOMER_REQUEST_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(125).toDate());
                validateUserAndInvoiceStatus(userId, CANCELLED_NOTICE_TO_COLLECTION_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(150).toDate());
                validateUserAndInvoiceStatus(userId, CANCELLED_TP_COLLECTIONS_STEP, INVOICE_STATUS_UNPAID, api);

            } finally {
                if (invoiceWS != null) {
                    api.deleteInvoice(invoiceWS.getId());
                }
                updateCustomerStatusToActive(userId, api);
                api.deleteUser(userId);
            }
        });
    }

    /**
     * Make payment on oldest invoice - customer's status changes as the number of days in between the 
     * second oldest invoice due date and the payment date - and if the difference number falls in 
     * between the collections steps defined, lowest number (Days) collection step/status should be assigned to the customer
     */
    @Test(priority = 2, enabled=true)
    public void testScenario1(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -4)),01,getYear(addMonths(today, -4)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

        	JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
            InvoiceWS invoiceWS3 = null;
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

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)),01,getYear(addMonths(today, -2)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, addMonths(nextInvoiceDate,1));
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(13).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(16).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(28).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(31).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate3 = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate3);
                api.updateUser(userWS);

                //Create third invoice
                Integer[] invoiceIds3 = createInvoice(api, userId, addMonths(nextInvoiceDate,2));
                assertEquals(invoiceIds3.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS3 = api.getInvoiceWS(invoiceIds3[0]);
                assertNotNull(invoiceWS3, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS3.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS3.getId());

                getPayment(api, invoiceWS1, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS1.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                getPayment(api, invoiceWS2, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS2.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                Integer daysInBetweenLastInvoiceAndToday = daysBetweenUsingJoda(invoiceWS3.getDueDate(), today);

                UserWS userWS1 = api.getUserWS(userId);

                if(daysInBetweenLastInvoiceAndToday < Integer.valueOf(1)){
                    assertEquals(userWS1.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(1) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(3)){
                    assertEquals(userWS1.getStatus(), ACTIVE_PAST_DUE_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_PAST_DUE_STEP);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(3) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(18)){
                    assertEquals(userWS1.getStatus(), ACTIVE_NOTICE_TO_BLOCK_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_NOTICE_TO_BLOCK_STEP);
                }else {
                    assertEquals(userWS1.getStatus(), ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP);
                }

            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * Make payment on all overdue invoice in a single payment and customer's status changes to Active.
     */
    @Test(priority = 3, enabled=true)
    public void testScenario2(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -4)),01,getYear(addMonths(today, -4)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

        	JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
            InvoiceWS invoiceWS3 = null;
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

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)),01,getYear(addMonths(today, -2)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, addMonths(nextInvoiceDate,1));
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(13).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(16).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(28).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(31).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate3 = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate3);
                api.updateUser(userWS);

                //Create third invoice
                Integer[] invoiceIds3 = createInvoice(api, userId, addMonths(nextInvoiceDate,2));
                assertEquals(invoiceIds3.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS3 = api.getInvoiceWS(invoiceIds3[0]);
                assertNotNull(invoiceWS3, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS3.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS3.getId());

                getPayment(api, invoiceWS1, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS1.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                getPayment(api, invoiceWS2, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS2.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                Integer daysInBetweenLastInvoiceAndToday = daysBetweenUsingJoda(invoiceWS3.getDueDate(), today);

                UserWS userWS1 = api.getUserWS(userId);

                if(daysInBetweenLastInvoiceAndToday < Integer.valueOf(1)){
                    assertEquals(userWS1.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(1) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(3)){
                    assertEquals(userWS1.getStatus(), ACTIVE_PAST_DUE_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_PAST_DUE_STEP);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(3) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(18)){
                    assertEquals(userWS1.getStatus(), ACTIVE_NOTICE_TO_BLOCK_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_NOTICE_TO_BLOCK_STEP);
                }else {
                    assertEquals(userWS1.getStatus(), ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP);
                }

                getPayment(api, invoiceWS3, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS3.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);
                assertEquals(api.getUserWS(userId).getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);

            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * Make payment when Only one overdue invoice is present, customer's status becomes Active.  
     */
    @Test(priority = 4, enabled=true)
    public void testScenario3(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)),01,getYear(addMonths(today, -2)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

        	JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
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

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, addMonths(nextInvoiceDate,1));
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(13).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                getPayment(api, invoiceWS1, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS1.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                Date lastInvoiceDate = invoiceWS2.getDueDate();

                Integer daysInBetweenLastInvoiceAndToday = daysBetweenUsingJoda(lastInvoiceDate, today);

                UserWS userWS1 = api.getUserWS(userId);

                if(daysInBetweenLastInvoiceAndToday < Integer.valueOf(1)){
                    assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(1) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(3)){
                    assertEquals(userWS.getStatus(), ACTIVE_PAST_DUE_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_PAST_DUE_STEP);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(3) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(18)){
                    assertEquals(userWS.getStatus(), ACTIVE_NOTICE_TO_BLOCK_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_NOTICE_TO_BLOCK_STEP);
                }else {
                    assertEquals(userWS1.getStatus(), ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP);
                }

                getPayment(api, invoiceWS2, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS2.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);
                UserWS userWS2 = api.getUserWS(userId);
                assertEquals(userWS2.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);

            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * Make payment when NO overdue invoice is present, customer's status becomes Active.  
     */
    @Test(priority = 5, enabled=true)
    public void testScenario4(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, 0)),01,getYear(addMonths(today, 0)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

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

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());

                api.triggerAgeing(new LocalDate(nextInvoiceDate).plusDays(1).toDate());
                assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);

                api.triggerAgeing(new LocalDate(nextInvoiceDate).plusDays(2).toDate());
                assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);

                api.triggerAgeing(new LocalDate(nextInvoiceDate).plusDays(3).toDate());
                assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);

                api.triggerAgeing(new LocalDate(nextInvoiceDate).plusDays(4).toDate());
                assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);

                getPayment(api, invoiceWS1, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS1.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                api.triggerAgeing(new LocalDate(nextInvoiceDate).plusDays(5).toDate());
                assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);
            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * Make payment when user status is Cancelled on Request
     */
    @Test(priority = 6, enabled=true)
    public void testScenario5() {
        Date today = new Date();
        final Date activeSinceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)), 01, getYear(addMonths(today, -2)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)), 01, getYear(addMonths(today, -1)));

        testBuilder.given(envBuilder -> {
                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    ConfigurationBuilder configurationBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                    configurationBuilder.addPlugin(CancellationInvoiceAgeingTask.class.getName());
                    configurationBuilder.build();

                    Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

                    envBuilder.orderBuilder(api).forUser(customerId).withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                            .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                            .withActiveSince(activeSinceDate)
                            .withEffectiveDate(activeSinceDate)
                            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                            .withDueDateValue(1)
                            .withCodeForTests("code")
                            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                            .build();
                    updateBillingProcessConfiguration(nextInvoiceDate, api, 22);

                }).test((env) -> {
                    JbillingAPI api = env.getPrancingPonyApi();
                    InvoiceWS invoiceWS = null;
                    Integer userId = env.idForCode(CUSTOMER_CODE);
                    List<Integer> invoiceIdList = new ArrayList<>();
                    PaymentWS paymentWS = new PaymentWS();
                    PaymentWS paymentWS1 = null;
                    try {
                        UserWS userWS = api.getUserWS(userId);
                        assertEquals(userWS.getStatus(), ACTIVE);

                        Integer[] invoiceIds = api.createInvoiceWithDate(userId, nextInvoiceDate, 3, 22, false);
                        invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                        assertNotNull(invoiceWS, INVOICE_IS_NOT_CREATED);
                        invoiceIdList.add(invoiceWS.getId());
                        validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                        final Date invoiceDueDate = invoiceWS.getDueDate();

                        validateUserAndInvoiceStatusWithDueDate(userId, ACTIVE, INVOICE_STATUS_UNPAID, api, invoiceDueDate);
                        //trigger aging
                        api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                        validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                        api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(4).toDate());
                        validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);
                        Date cancellationDate = addDays(nextInvoiceDate, 15);
                        Integer customerId = api.getUserWS(userId).getCustomerId();
                        CancellationRequestWS crWS = constructCancellationRequestWS(cancellationDate, customerId, "user requested for cancellation!");
                        api.createCancellationRequest(crWS);

                        assertEquals(TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()), TestConstants.DATE_FORMAT.format(cancellationDate),
                                "Cancellation date was not set correctly for customer");

                        userWS.setNextInvoiceDate(addMonths(nextInvoiceDate, 1));
                        api.updateUser(userWS);

                        Integer pluginId = configurePluginSetCronExpression(api);
                        logger.debug("pluging configured with id {}", pluginId);
                        logger.debug("waiting for the plugin to trigger and generate cancellation invoice ");
                        wait(60000);
                        api.deletePlugin(pluginId);

                        InvoiceWS cancelInvoiceWS = api.getLatestInvoice(userId);
                        assertNotNull(cancelInvoiceWS, INVOICE_IS_NOT_CREATED);
                        invoiceIdList.add(cancelInvoiceWS.getId());
                        assertEquals(invoiceWS.getStatusId(), INVOICE_STATUS_UNPAID);

                        UserWS user = api.getUserWS(userId);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, USERS_STATUS_SHOULD_BE+"Cancelled on Request before payment");
                        getPayment(api, invoiceWS, userId, paymentWS);
                        api.applyPayment(paymentWS, invoiceWS.getId());
                        paymentWS1 = api.getLatestPayment(userId);

                        assertNotNull(paymentWS1);
                        assertEquals(user.getStatus(), USER_CANCELLATION_STATUS, USERS_STATUS_SHOULD_BE+"Cancelled on Request after payment ");
                    } catch(Exception e){
                        Assert.fail(e.getMessage());
                    } finally {
                        if(paymentWS1 != null){
                            api.deletePayment(paymentWS1.getId());
                        }
                        for(int i = invoiceIdList.size()-1; i >= 0; i--){
                            api.deleteInvoice(invoiceIdList.get(i));
                        }
                        updateCustomerStatusToActive(userId, api);

                    }
                });
    }

    /**
     * Paid oldest invoice with Credit Note when Only one overdue invoice is present = use case 6
     */
    @Test(priority = 7, enabled=true)
    public void testScenario6(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)),01,getYear(addMonths(today, -2)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
            InvoiceWS creditInvoiceWS3 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            List<Integer> invoiceIdList = new ArrayList<>();
            try {

                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = addMonths(nextInvoiceDate,1);

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, nextInvoiceDate2);
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(13).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(18).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(addMonths(nextInvoiceDate2, 1));
                api.updateUser(userWS);

                //one time credit adjustment order (with $-0.50)
                Integer oneTimeOrderId = 0;
                Date creditNoteDate = today;
                oneTimeOrderId = createOneTimeOrder(api, userId, creditNoteDate, "1", env.idForCode(CREDIT_PRODUCT_CODE));
                OrderWS oneTimeOrder =  api.getOrder(oneTimeOrderId);
                assertEquals(oneTimeOrder.getOrderLines()[0].getAmountAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("-0.50"));
                Integer adjustmentInvoiceId = api.createInvoiceFromOrder(oneTimeOrderId, null);
                assertNotNull(adjustmentInvoiceId, INVOICE_IS_NOT_CREATED);
                creditInvoiceWS3 = api.getInvoiceWS(adjustmentInvoiceId);
                invoiceIdList.add(creditInvoiceWS3.getId());

                Integer[] creditNotes = api.getLastCreditNotes(userId, 10);
                assertEquals(creditNotes.length,1, CREDIT_NOTE_SHOULD_BE_GENERATED);
                CreditNoteWS creditNoteWS = api.getCreditNote(creditNotes[0]);
                assertNotNull(creditNoteWS, CREDIT_NOTE_IS_NOT_CREATED);

                Date lastInvoiceDueDate = invoiceWS2.getDueDate();

                Integer daysInBetweenLastInvoiceAndToday = daysBetweenUsingJoda(lastInvoiceDueDate, today);

                userWS = api.getUserWS(userId);
                if(daysInBetweenLastInvoiceAndToday < Integer.valueOf(1)){
                    assertEquals(userWS.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(1) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(3)){
                    assertEquals(userWS.getStatus(), ACTIVE_PAST_DUE_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_PAST_DUE_STEP);
                }else if(daysInBetweenLastInvoiceAndToday >= Integer.valueOf(3) && daysInBetweenLastInvoiceAndToday < Integer.valueOf(18)){
                    assertEquals(userWS.getStatus(), ACTIVE_NOTICE_TO_BLOCK_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_NOTICE_TO_BLOCK_STEP);
                }else {
                    assertEquals(userWS.getStatus(), ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, USERS_STATUS_SHOULD_BE+ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP);
                }

            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * Paid oldest invoice partially with Credit Note when Only one overdue invoice is present = use case 7
     */
    @Test(priority = 8, enabled=true)
    public void testScenario7(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)),01,getYear(addMonths(today, -2)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
            InvoiceWS creditInvoiceWS3 = null;
            Integer userId = env.idForCode(CUSTOMER_CODE);
            Integer orderId = null;
            List<Integer> invoiceIdList = new ArrayList<>();
            try {

                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = addMonths(nextInvoiceDate,1);

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, nextInvoiceDate2);
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(13).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(18).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(addMonths(nextInvoiceDate2, 1));
                api.updateUser(userWS);

                String userStatusBeforeCreditNote = userWS.getStatus();

                //one time credit adjustment order (with $-0.30)
                Integer oneTimeOrderId = 0;
                Date creditNoteDate = today;
                oneTimeOrderId = createOneTimeOrder(api, userId, creditNoteDate, "1", env.idForCode(CREDIT_PRODUCT_CODE_2));
                OrderWS oneTimeOrder =  api.getOrder(oneTimeOrderId);
                assertEquals(oneTimeOrder.getOrderLines()[0].getAmountAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("-0.30"));
                Integer adjustmentInvoiceId = api.createInvoiceFromOrder(oneTimeOrderId, null);
                assertNotNull(adjustmentInvoiceId, INVOICE_IS_NOT_CREATED);
                creditInvoiceWS3 = api.getInvoiceWS(adjustmentInvoiceId);
                invoiceIdList.add(creditInvoiceWS3.getId());

                Integer[] creditNotes = api.getLastCreditNotes(userId, 10);
                assertEquals(creditNotes.length,1, CREDIT_NOTE_SHOULD_BE_GENERATED);
                CreditNoteWS creditNoteWS = api.getCreditNote(creditNotes[0]);
                assertNotNull(creditNoteWS, CREDIT_NOTE_IS_NOT_CREATED);

                // Invoice is paid partially with the credit note, hence the user ageing status revaluation will not happen
                // the user status will remain as it is before the credit note happened.
                assertEquals(api.getUserWS(userId).getStatus(), userStatusBeforeCreditNote,
                        USERS_STATUS_SHOULD_BE+userStatusBeforeCreditNote);

            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * User makes payment when user is having ageing status “Cancelled | Customer Request” with “stop activation on payment” checked.
     */
    @Test(priority = 9, enabled=true)
    public void testScenario8(){

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -5)),01,getYear(addMonths(today, -5)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -4)),01,getYear(addMonths(today, -4)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                            nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

            envBuilder.orderBuilder(api)
                .forUser(customerId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(activeSince)
                .withEffectiveDate(activeSince)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("code")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .build();

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
            InvoiceWS invoiceWS3 = null;
            InvoiceWS invoiceWS4 = null;
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

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = FullCreativeUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, addMonths(nextInvoiceDate,1));
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(13).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(16).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_NOTICE_TO_BLOCK_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(28).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(31).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate3 = FullCreativeUtil.getDate(getMonth(addMonths(today, -2)),01,getYear(addMonths(today, -2)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate3);
                api.updateUser(userWS);

                //Create third invoice
                Integer[] invoiceIds3 = createInvoice(api, userId, addMonths(nextInvoiceDate,2));
                assertEquals(invoiceIds3.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS3 = api.getInvoiceWS(invoiceIds3[0]);
                assertNotNull(invoiceWS3, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS3.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS3.getId());

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(61).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(78).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate4 = FullCreativeUtil.getDate(getMonth(addMonths(today, -1)),01,getYear(addMonths(today, -1)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate4);
                api.updateUser(userWS);

                //Create fourth invoice
                Integer[] invoiceIds4 = createInvoice(api, userId, addMonths(nextInvoiceDate,3));
                assertEquals(invoiceIds4.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS4 = api.getInvoiceWS(invoiceIds4[0]);
                assertNotNull(invoiceWS4, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS4.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS4.getId());

                validateUserAndInvoiceStatus(userId, ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(100).toDate());
                validateUserAndInvoiceStatus(userId, CANCELLED_CUSTOMER_REQUEST_STEP, INVOICE_STATUS_UNPAID, api);

                //Payment of oldest invoice
                getPayment(api, invoiceWS1, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS1.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                // validation after payment
                validateUserAndInvoiceStatus(userId, CANCELLED_CUSTOMER_REQUEST_STEP, INVOICE_STATUS_UNPAID, api);

            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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
     * Amaysim test scenario
     * Make payment when more than one overdue invoices are present
     */
    @Test(priority = 10, enabled=true)
    public void testScenario9() {

        Date today = new Date();
        final Date activeSince = FullCreativeUtil.getDate(getMonth(addMonths(today, -4)),01,getYear(addMonths(today, -4)));
        final Date nextInvoiceDate = FullCreativeUtil.getDate(getMonth(addMonths(today, -4)),01,getYear(addMonths(today, -4)));

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            updateAgedUsersStatusToActive(api); // update all the status of user to active
            // create new ageing steps for Amaysim
            api.saveAgeingConfigurationWithCollectionType(buildAmaysimAgeingSteps(api), api.getCallerLanguageId(), CollectionType.REGULAR);
            AgeingWS[] ageingList = api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR);
            validateStep(ageingList[0], ACTIVE_OVERDUE_STEP, Integer.valueOf(1), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[1], ACTIVE_COLLECTIONS_STEP, Integer.valueOf(2), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[2], ACTIVE_REMOVE_STEP, Integer.valueOf(3), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.FALSE);
            validateStep(ageingList[3], ACTIVE_SECOND_RETRY_STEP, Integer.valueOf(7), Boolean.TRUE, Boolean.FALSE,
                    Boolean.FALSE, Boolean.TRUE);

             Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    nextInvoiceDate, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

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

            updateBillingProcessConfiguration(nextInvoiceDate, api, 21);

        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS1 = null;
            InvoiceWS invoiceWS2 = null;
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

                //Create first invoice
                Integer[] invoiceIds1 = createInvoice(api, userId, addMonths(nextInvoiceDate,0));
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS1.getId());
                Date invoiceDueDate = invoiceWS1.getDueDate();

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_OVERDUE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(2).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_COLLECTIONS_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(3).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_REMOVE_STEP, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate).plusDays(7).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_SECOND_RETRY_STEP, INVOICE_STATUS_UNPAID, api);

                Date nextInvoiceDate2 = FullCreativeUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));

                userWS = api.getUserWS(userId);
                userWS.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS);

                //Create second invoice
                Integer[] invoiceIds2 = createInvoice(api, userId, addMonths(nextInvoiceDate,1));
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);
                invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);

                invoiceIdList.add(invoiceWS2.getId());

                api.triggerAgeing(new LocalDate(nextInvoiceDate2).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_SECOND_RETRY_STEP, INVOICE_STATUS_UNPAID, api);


                getPayment(api, invoiceWS1, userId, paymentWS1);
                api.applyPayment(paymentWS1, invoiceWS1.getId());

                paymentWS1 = api.getLatestPayment(userId);
                assertNotNull(paymentWS1);

                validateUserAndInvoiceStatus(userId, ACTIVE_SECOND_RETRY_STEP, INVOICE_STATUS_UNPAID, api);


            } finally {
                for(int i = invoiceIdList.size()-1; i >= 0; i--){
                    if(invoiceIdList.get(i) != null){
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

    private void updateAgedUsersStatusToActive(JbillingAPI api) {
        Integer[] agedUsers = api.getUsersNotInStatus(1);
        for(Integer id : agedUsers){
            logger.debug("aged user id : {} ",id);
            updateCustomerStatusToActive(id, api);
        }
    }

    private Integer[] createInvoice(JbillingAPI api, Integer userId, Date nextInvoiceDate) {
        UserWS userWS = api.getUserWS(userId);
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        Integer[] invoiceIds2 = api.createInvoiceWithDate(userId, nextInvoiceDate, 3, 21, false);
        return invoiceIds2;
    }

    private void validateUserAndInvoiceStatus(Integer userId, String userStatus, Integer invoiceStatus, JbillingAPI api) {
        UserWS user = api.getUserWS(userId);
        assertEquals(user.getStatus(), userStatus);
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertEquals(invoice.getStatusId(), invoiceStatus);
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder, String code, Integer accountTypeId, Date nid, Integer paymentTypeId, boolean goodCC){
        final JbillingAPI api = envBuilder.getPrancingPonyApi();

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
        AgeingWS[] ageingSteps = new AgeingWS[6];
        ageingSteps[0] = buildAgeingStep(ACTIVE_PAST_DUE_STEP, 1, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[1] = buildAgeingStep(ACTIVE_NOTICE_TO_BLOCK_STEP, 3, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[2] = buildAgeingStep(ACTIVE_BLOCKED_FOR_NON_PAYMENT_STEP, 18, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[3] = buildAgeingStep(CANCELLED_CUSTOMER_REQUEST_STEP, 100, true, false, false, true, api, CollectionType.REGULAR);
        ageingSteps[4] = buildAgeingStep(CANCELLED_NOTICE_TO_COLLECTION_STEP, 125, true, false, false, true, api, CollectionType.REGULAR);
        ageingSteps[5] = buildAgeingStep(CANCELLED_TP_COLLECTIONS_STEP, 150, true, false, false, true, api, CollectionType.REGULAR);
        return ageingSteps;
    }

    private AgeingWS[] buildAmaysimAgeingSteps(JbillingAPI api) {
        AgeingWS[] ageingSteps = new AgeingWS[4];
        ageingSteps[0] = buildAgeingStep(ACTIVE_OVERDUE_STEP, 1, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[1] = buildAgeingStep(ACTIVE_COLLECTIONS_STEP, 2, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[2] = buildAgeingStep(ACTIVE_REMOVE_STEP, 3, true, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[3] = buildAgeingStep(ACTIVE_SECOND_RETRY_STEP, 7, true, false, false, true, api, CollectionType.REGULAR);
        return ageingSteps;
    }

    private AgeingWS buildAgeingStep(String statusStep,Integer days,
                                     boolean sendNotification , boolean payment, boolean suspended, boolean stopActivationOnPayment, JbillingAPI api, CollectionType collectionType){

        AgeingWS ageingWS = new AgeingWS();
        ageingWS.setEntityId(api.getCallerCompanyId());
        ageingWS.setStatusStr(statusStep);
        ageingWS.setDays(days);
        ageingWS.setPaymentRetry(Boolean.valueOf(payment));
        ageingWS.setSendNotification(Boolean.valueOf(sendNotification));
        ageingWS.setSuspended(Boolean.valueOf(suspended));
        ageingWS.setStopActivationOnPayment(stopActivationOnPayment);
        ageingWS.setCollectionType(collectionType);
        return  ageingWS;
    }

    private void validateStep(AgeingWS ageingWS, String statusStr, Integer days,
                              Boolean sendNotification , Boolean payment, Boolean suspended, Boolean stopActivationOnPayment ){

        assertEquals(ageingWS.getStatusStr(), statusStr, "Invalid Step name");
        assertEquals(ageingWS.getDays(), days, "Invalid number of days");
        assertEquals(ageingWS.getPaymentRetry(), payment, "Invalid payment check");
        assertEquals(ageingWS.getSendNotification(), sendNotification, "Invalid notification check");
        assertEquals(ageingWS.getSuspended(), suspended, "Invalid suspended check");
        assertEquals(ageingWS.getStopActivationOnPayment(), stopActivationOnPayment, "Invalid stopActivationOnPayment check");
    }

    public Integer buildAndPersistOrderPeriod(TestEnvironmentBuilder envBuilder, JbillingAPI api,
                                              String description, Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api)
                .withDescription(description)
                .withValue(value)
                .withUnitId(unitId)
                .build();
    }

    private void updateBillingProcessConfiguration(Date nextRunDate, JbillingAPI api, int dueDays){

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

    private void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("Errors while waiting for operations to complete");
        }
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }

    private static Date addDays(Date inputDate, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
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

    private void validateUserAndInvoiceStatusWithDueDate(Integer userId, String userStatus, Integer invoiceStatus, JbillingAPI api, Date dueDate) {
        UserWS user = api.getUserWS(userId);
        assertEquals(user.getStatus(), userStatus);
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertEquals(invoice.getStatusId(), invoiceStatus);
        assertEquals(invoice.getDueDate(), dueDate);
    }

    private CancellationRequestWS constructCancellationRequestWS(Date cancellationDate, Integer customerId, String reasonText) {
        CancellationRequestWS cancellationRequestWS = new CancellationRequestWS();
        cancellationRequestWS.setCancellationDate(cancellationDate);
        cancellationRequestWS.setCreateTimestamp(new Date());
        cancellationRequestWS.setCustomerId(customerId);
        cancellationRequestWS.setReasonText(reasonText);
        cancellationRequestWS.setStatus(CancellationRequestStatus.APPLIED);
        return cancellationRequestWS;
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

    public String getTimeDiff(Date dateOne, Date dateTwo) {
        String diff = "";
        long timeDiff = Math.abs(dateOne.getTime() - dateTwo.getTime());
        diff = String.format("%d hour(s) %d min(s)", TimeUnit.MILLISECONDS.toHours(timeDiff), TimeUnit.MILLISECONDS.toMinutes(timeDiff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeDiff)));
        return diff;
    }

    public static int daysBetweenUsingJoda(Date d1, Date d2){
        return Days.daysBetween( new LocalDate(d1.getTime()), new LocalDate(d2.getTime())).getDays();
    }

    private void getPayment(JbillingAPI api, InvoiceWS invoiceWS1,
        Integer userId, PaymentWS paymentWS1) {
        paymentWS1.setUserId(userId);
        paymentWS1.setAmount(invoiceWS1.getBalance());
        paymentWS1.setIsRefund(Integer.valueOf(0));
        paymentWS1.setMethodId(Constants.PAYMENT_METHOD_VISA);
        paymentWS1.setCurrencyId(api.getCallerCurrencyId());
        paymentWS1.setPaymentInstruments(api.getUserWS(userId).getPaymentInstruments());
        paymentWS1.setPaymentDate(new Date());
    }

    public static Integer createOneTimeOrder(JbillingAPI api,Integer userId, Date activeSinceDate, String productQuantity, Integer productCode){
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

        oTOrder.setOrderLines(new OrderLineWS[]{oTline1});
        Integer oneTimeOrderId = api.createOrder(oTOrder, OrderChangeBL.buildFromOrder(oTOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        logger.debug("Created one time usage order with Id: {}", oneTimeOrderId);
        assertNotNull(oneTimeOrderId,"one time usage order creation failed");

        return oneTimeOrderId;
    }

}