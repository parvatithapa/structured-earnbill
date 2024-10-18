package com.sapienter.jbilling.server.process;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;

@Test(groups = {"integration"}, testName = "AgeingMinimumBalanceTest", priority = 21)
public class AgeingMinimumBalanceTest {

    private static final Logger logger = LoggerFactory
            .getLogger(AgeingMinimumBalanceTest.class);
    private static final int AGEING_REVALUATION_PAYMENT_PREFERENCE = 88;
    private EnvironmentHelper environmentHelper;
    private TestBuilder testBuilder;

    // Fixed constants for now
    private static final Integer INVOICE_STATUS_UNPAID = Integer.valueOf(2);

    // Collection Steps
    private static final String ACTIVE                              = "Active";
    private static final String ACTIVE_PAST_DUE_STEP                = "Ageing Day 1";

    private static final String CATEGORY_CODE          = "TestCategory";
    private static final String PRODUCT_CODE           = "TestProduct";
    private static final String CREDIT_PRODUCT_CODE    = "TestCreditProduct";
    private static final String CREDIT_PRODUCT_CODE_2  = "TestCreditProduct2";
    private static final String ACCOUNT_TYPE_CODE      = "TestCollectionsAccount";
    private static final String CUSTOMER_CODE_1        = "TestCustomer";
    private static final String PAYMENT_TYPE_CODE      = "TestCCPaymentType";
    private static final String CUSTOMER_CODE_2        = "TestCustomerForPreference101";
    private static final String CUSTOMER_CODE_3        = "TestCustomerForPreference101";
    private static final String USERS_STATUS_SHOULD_BE = "User's status should be ";

    private static final String CC_CARD_HOLDER_NAME_MF = "cc.cardholder.name";
    private static final String CC_NUMBER_MF           = "cc.number";
    private static final String CC_EXPIRY_DATE_MF      = "cc.expiry.date";
    private static final String CC_TYPE_MF             = "cc.type";
    private static final String CC_GATEWAY_KEY_MF      = "cc.gateway.key";

    private static final String INVALID_NUMBER_ORDER_LINES      = "Invalid number Order Lines";
    private static final String INVOICE_SHOULD_BE_GENERATED     = "Invoice should be generated";
    private static final String INVOICE_IS_NOT_CREATED          = "Invoice is not created.";

    public final static int ONE_TIME_ORDER_PERIOD        = 1;
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final Integer PREFERENCE_AGEING_FOR_PAST_INVOICE = 101;
    private static final Integer PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING = 51;
    private static final String AGEING_MINIMUM_BALANCE = "100";
    private static final String TOTAL_DUE_INVOICE = "0";
    private static final String ENABLE_PAST_DUE_INVOICE = "1";

    private static Integer paymentMethodTypeId = null;

    @BeforeClass
    public void initializeTests(){
        testBuilder = getPluginsAndItemSetup();
    }

    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();

        // revert the preference values
        PreferenceWS preference51 = api.getPreference(PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING);
        preference51.setValue(TOTAL_DUE_INVOICE);
        api.updatePreference(preference51);

        PreferenceWS preference = api.getPreference(PREFERENCE_AGEING_FOR_PAST_INVOICE);
        preference.setValue(TOTAL_DUE_INVOICE);
        api.updatePreference(preference);

        //This call is not required as it's updating the status to ACTIVE of active users
        //updateAgedUsersStatusToActive(api);

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

    private TestBuilder getPluginsAndItemSetup(){
        return TestBuilder.newTest().givenForMultiple(envCreator -> {

            final JbillingAPI api = envCreator.getPrancingPonyApi();

            environmentHelper = EnvironmentHelper.getInstance(api);

            ConfigurationBuilder configurationBuilder = envCreator.configurationBuilder(api);
            configurationBuilder.build();
            configurationBuilder.updatePreference(api, AGEING_REVALUATION_PAYMENT_PREFERENCE, "1");

            Integer categoryId = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(false).build();
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(false).withType(categoryId)
                    .withFlatPrice("40").build();
            envCreator.itemBuilder(api).item().withCode(CREDIT_PRODUCT_CODE).global(false).withType(categoryId)
                    .withFlatPrice("-10").build();
            envCreator.itemBuilder(api).item().withCode(CREDIT_PRODUCT_CODE_2).global(false).withType(categoryId)
                    .withFlatPrice("-200").build();

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

    // Enable checking Minimum Ageing Balance with past due invoices only
    @Test(priority = 1, enabled=true)
    public void testScenario1(){

        Calendar calender = Calendar.getInstance();
        final Date activeSince = getDate(calender, 2019, 10, 1);
        final Date nextInvoiceDate1 = getDate(calender, 2019, 11, 1);
        final Date nextInvoiceDate2 = getDate(calender, 2020, 0, 1);
        final Date nextInvoiceDate3 = getDate(calender, 2020, 1, 1);
        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE_1, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    nextInvoiceDate1, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

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

            updateBillingProcessConfiguration(nextInvoiceDate1, api, 3);
        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode(CUSTOMER_CODE_1);
            Integer orderId = null;
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                // update the preference 51 for minimum balance ageing
                PreferenceWS preference51 = api.getPreference(PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING);
                preference51.setValue(AGEING_MINIMUM_BALANCE);
                api.updatePreference(preference51);

                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                // update the preference 101 for enable past due invoice only
                PreferenceWS preference = api.getPreference(PREFERENCE_AGEING_FOR_PAST_INVOICE);
                preference.setValue(ENABLE_PAST_DUE_INVOICE);
                api.updatePreference(preference);

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);

                //Create first invoice
                Integer[] invoiceIds1 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate1, 3, 21, false);
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS1.getId());

                Date invoiceDueDate1 = invoiceWS1.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate1).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                UserWS userWS1 = api.getUserWS(userId);
                userWS1.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS1);

                //Create second invoice
                Integer[] invoiceIds2 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate2, 3, 21, false);
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS2.getId());

                Date invoiceDueDate2 = invoiceWS2.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate2).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                UserWS userWS2 = api.getUserWS(userId);
                userWS2.setNextInvoiceDate(nextInvoiceDate3);
                api.updateUser(userWS2);

                //Create third invoice
                Integer[] invoiceIds3 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate3, 3, 21, false);
                assertEquals(invoiceIds3.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS3 = api.getInvoiceWS(invoiceIds3[0]);
                assertNotNull(invoiceWS3, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS3.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS3.getId());

                Date invoiceDueDate3 = invoiceWS3.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate3).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate3).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);
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

    // Disable  Minimum Ageing Balance with out past due invoices only
    @Test(priority = 2, enabled=true)
    public void testScenario2(){

        Calendar calender = Calendar.getInstance();
        final Date activeSince = getDate(calender, 2019, 10, 1);
        final Date nextInvoiceDate1 = getDate(calender, 2019, 11, 1);
        final Date nextInvoiceDate2 = getDate(calender, 2020, 0, 1);
        final Date nextInvoiceDate3 = getDate(calender, 2020, 1, 1);

        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE_2, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    nextInvoiceDate1, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

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

            updateBillingProcessConfiguration(nextInvoiceDate1, api, 3);
        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode(CUSTOMER_CODE_2);
            Integer orderId = null;
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
             // update the preference 51 for minimum balance ageing
                PreferenceWS preference51 = api.getPreference(PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING);
                preference51.setValue(AGEING_MINIMUM_BALANCE);
                api.updatePreference(preference51);

                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                // update the preference 101 for enable past due invoice only
                PreferenceWS preference = api.getPreference(PREFERENCE_AGEING_FOR_PAST_INVOICE);
                preference.setValue(TOTAL_DUE_INVOICE);
                api.updatePreference(preference);

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);

                //Create first invoice
                Integer[] invoiceIds1 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate1, 3, 21, false);
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS1.getId());

                Date invoiceDueDate1 = invoiceWS1.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate1).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate1).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                UserWS userWS1 = api.getUserWS(userId);
                userWS1.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS1);

                //Create second invoice
                Integer[] invoiceIds2 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate2, 3, 21, false);
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS2.getId());

                Date invoiceDueDate2 = invoiceWS2.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate2).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                api.triggerAgeing(new LocalDate(invoiceDueDate1).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);  // Status has active because preference51 limit has not reached

                UserWS userWS2 = api.getUserWS(userId);
                userWS2.setNextInvoiceDate(nextInvoiceDate3);
                api.updateUser(userWS2);

                //Create third invoice
                Integer[] invoiceIds3 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate3, 3, 21, false);
                assertEquals(invoiceIds3.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS3 = api.getInvoiceWS(invoiceIds3[0]);
                assertNotNull(invoiceWS3, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS3.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS3.getId());

                Date invoiceDueDate3 = invoiceWS3.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate3).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);
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

 // Enable checking Minimum Ageing Balance after payment has done
    @Test(priority = 3, enabled=true)
    public void testScenario3(){

        Calendar calender = Calendar.getInstance();
        final Date activeSince = getDate(calender, 2019, 10, 1);
        final Date nextInvoiceDate1 = getDate(calender, 2019, 11, 1);
        final Date nextInvoiceDate2 = getDate(calender, 2020, 0, 1);
        final Date nextInvoiceDate3 = getDate(calender, 2020, 1, 1);
        testBuilder.given(envBuilder ->{

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer customerId = createCustomer(envBuilder, CUSTOMER_CODE_3, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    nextInvoiceDate1, envBuilder.env().idForCode(PAYMENT_TYPE_CODE), true);

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

            updateBillingProcessConfiguration(nextInvoiceDate1, api, 3);
        }).test((env) -> {

            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode(CUSTOMER_CODE_3);
            Integer orderId = null;
            PaymentWS paymentWS = new PaymentWS();
            List<Integer> invoiceIdList = new ArrayList<>();
            try {
                // update the preference 51 for minimum balance ageing
                PreferenceWS preference51 = api.getPreference(PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING);
                preference51.setValue(AGEING_MINIMUM_BALANCE);
                api.updatePreference(preference51);

                OrderWS orderWS = api.getLatestOrder(userId);
                assertEquals(orderWS.getOrderLines().length, 1, INVALID_NUMBER_ORDER_LINES);
                orderId = orderWS.getId();

                // update the preference 101 for enable past due invoice only
                PreferenceWS preference = api.getPreference(PREFERENCE_AGEING_FOR_PAST_INVOICE);
                preference.setValue(ENABLE_PAST_DUE_INVOICE);
                api.updatePreference(preference);

                UserWS userWS = api.getUserWS(userId);
                assertEquals(userWS.getStatus(), ACTIVE);

                //Create first invoice
                Integer[] invoiceIds1 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate1, 3, 21, false);
                assertEquals(invoiceIds1.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS1 = api.getInvoiceWS(invoiceIds1[0]);
                assertNotNull(invoiceWS1, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS1.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS1.getId());

                Date invoiceDueDate1 = invoiceWS1.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate1).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                UserWS userWS1 = api.getUserWS(userId);
                userWS1.setNextInvoiceDate(nextInvoiceDate2);
                api.updateUser(userWS1);

                //Create second invoice
                Integer[] invoiceIds2 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate2, 3, 21, false);
                assertEquals(invoiceIds2.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS2 = api.getInvoiceWS(invoiceIds2[0]);
                assertNotNull(invoiceWS2, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS2.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS2.getId());

                Date invoiceDueDate2 = invoiceWS2.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate2).minusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE, INVOICE_STATUS_UNPAID, api);

                UserWS userWS2 = api.getUserWS(userId);
                userWS2.setNextInvoiceDate(nextInvoiceDate3);
                api.updateUser(userWS2);

                //Create third invoice
                Integer[] invoiceIds3 = api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate3, 3, 21, false);
                assertEquals(invoiceIds3.length,1, INVOICE_SHOULD_BE_GENERATED);

                InvoiceWS invoiceWS3 = api.getInvoiceWS(invoiceIds3[0]);
                assertNotNull(invoiceWS3, INVOICE_IS_NOT_CREATED);
                assertEquals(invoiceWS3.getStatusId(), INVOICE_STATUS_UNPAID);
                invoiceIdList.add(invoiceWS3.getId());

                Date invoiceDueDate3 = invoiceWS3.getDueDate();
                api.triggerAgeing(new LocalDate(invoiceDueDate3).plusDays(1).toDate());
                validateUserAndInvoiceStatus(userId, ACTIVE_PAST_DUE_STEP, INVOICE_STATUS_UNPAID, api);

                getPayment(api, invoiceWS1, userId, paymentWS,new LocalDate(invoiceDueDate3).plusDays(1).toDate());
                api.applyPayment(paymentWS, invoiceWS1.getId());
                paymentWS = api.getLatestPayment(userId);
                assertNotNull(paymentWS);
                UserWS userWS4 = api.getUserWS(userId);
                assertEquals(userWS4.getStatus(), ACTIVE, USERS_STATUS_SHOULD_BE+ACTIVE);
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

    private Date getDate(Calendar cal, int year, int month, int day) {
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        return cal.getTime();
    }

    private void updateAgedUsersStatusToActive(JbillingAPI api) {
        Integer[] agedUsers = api.getUsersNotInStatus(1);
        for(Integer id : agedUsers){
            logger.debug("aged user id : {} ",id);
            updateCustomerStatusToActive(id, api);
        }
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
                            .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS(CC_CARD_HOLDER_NAME_MF, code.toCharArray()))
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

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){
        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
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

    private void getPayment(JbillingAPI api, InvoiceWS invoiceWS1,
            Integer userId, PaymentWS paymentWS1,Date date) {
        paymentWS1.setUserId(userId);
        paymentWS1.setAmount(invoiceWS1.getBalance());
        paymentWS1.setIsRefund(Integer.valueOf(0));
        paymentWS1.setMethodId(Constants.PAYMENT_METHOD_VISA);
        paymentWS1.setCurrencyId(api.getCallerCurrencyId());
        paymentWS1.setPaymentInstruments(api.getUserWS(userId)
                .getPaymentInstruments());
        paymentWS1.setPaymentDate(date);
    }
}
