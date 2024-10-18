package com.sapienter.jbilling.fc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.builders.PaymentMethodTypeBuilder;

@Test(groups = "external-system", testName = "FullCreativeRemovePaymentInstrumentTest")
public class FullCreativeRemovePaymentInstrumentTest {

    private static final Logger logger = LoggerFactory.getLogger(FullCreativeRemovePaymentInstrumentTest.class);

    private JbillingAPI api;
    private static TestBuilder testBuilder;
    private TestEnvironment environment;

    private int btPaymentMethodTypeId;
    private int achPaymentMethodTypeId;

    private static final String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private static final String CC_MF_NUMBER = "cc.number";
    private static final String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private static final String CC_MF_TYPE = "cc.type";
    private static final String CREDIT_CARD_NUMBER = "378282246310005";
    private static final Integer migrationAcoountTypeId = 60103;
    private static final Integer standardAcoountTypeId = 60102;
    private static final String PLUGIN_PARAM_BT_ENVIRONMENT = "BT Environment";
    private static final String PLUGIN_PARAM_ALLOW_PAYMENT_ID = "Allowed Payment Method Ids";
    private static final String PLUGIN_PARAM_BUSSINESS_ID = "Bussiness Id";
    private static final String ACH_MF_ROUTING_NUMBER = "ach.routing.number";
    private static final String ACH_MF_BANK_NAME = "ach.bank.name";
    private static final String ACH_MF_CUSTOMER_NAME = "ach.customer.name";
    private static final String ACH_MF_ACCOUNT_NUMBER = "ach.account.number";
    private static final String ACH_MF_ACCOUNT_TYPE = "ach.account.type";
    private static final String ACH_ROUTING_NUMBER = "111111118";
    private static final String ACH_ACCOUNT_NUMBER = "1234567801";
    private static int ACH_PM_ID = 14;
    private static final String PLUGIN_CODE1 = "Plugin-Code1";
    private static final String PLUGIN_CODE2 = "Plugin-Code2";
    private static final String BT_PAYMENT_METHOD_NAME = "BT Credit Card";

    private static Integer brainTreePluginId;
    private static Integer saveCreditCardPluginId;
    private Integer tempUserCode;
    private int pluginIdACH;

    private static final String CUSTOMER_CODE = "CRTestCustomer" + System.currentTimeMillis();

    @BeforeClass
    public void initializeTests() throws JbillingAPIException, IOException {
        api = JbillingAPIFactory.getAPI();
        testBuilder = getTestEnvironment();
        environment = testBuilder.getTestEnvironment();
        configureBrainTreePlugin();
        configureAchPlugin();
    }

    public TestBuilder configureBrainTreePlugin() throws JbillingAPIException, IOException {
        return TestBuilder
                .newTest(false)
                .givenForMultiple(envCreator -> {
                    updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID1);
                    updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID2);
                    updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FILTER_TASK_ID);
                    updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CCF_TASK_ID);
                    updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CUREENCY_TASK_ID);

                    testBuilder.given(envBuilder -> {
                        btPaymentMethodTypeId = buildAndPersistPaymentMethodType(BT_PAYMENT_METHOD_NAME);
                    }).test((testEnv, testEnvBuilder) -> {
                        assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(BT_PAYMENT_METHOD_NAME));
                    });

                    // configure plugin BrainTreePaymentExternalTask
                        logger.debug("Configured BrainTree payment plugin");
                        PluggableTaskWS brainTreePlugin = new PluggableTaskWS();
                        PluggableTaskTypeWS pluginType = api
                                .getPluginTypeWSByClassName("com.sapienter.jbilling.server.payment.tasks.braintree.BrainTreePaymentExternalTask");
                        Hashtable<String, String> brainTreeparameters = new Hashtable<>();
                        brainTreeparameters.put(PLUGIN_PARAM_BUSSINESS_ID, "bf4d2b3b-e4f8-4066-90f1-c5d7941bbc33");
                        brainTreeparameters.put(PLUGIN_PARAM_BT_ENVIRONMENT, "sandbox");
                        brainTreeparameters.put(PLUGIN_PARAM_ALLOW_PAYMENT_ID,
                                String.valueOf(environment.idForCode(BT_PAYMENT_METHOD_NAME)));
                        brainTreeparameters.put("FC Web Service URL", "https://staging-adaptive-payments.appspot.com");

                        brainTreePlugin.setTypeId(pluginType.getId());
                        brainTreePlugin.setProcessingOrder(1);
                        brainTreePlugin.setParameters(brainTreeparameters);

                        brainTreePluginId = api.createPlugin(brainTreePlugin);

                        PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();

                        saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
                        saveCreditCardExternalPlugin.setProcessingOrder(1234);
                        saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(
                                FullCreativeTestConstants.SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME).getId());
                        Hashtable<String, String> saveCreditCardExternalparameters = new Hashtable<>();
                        saveCreditCardExternalparameters.clear();

                        saveCreditCardExternalparameters.put("removeOnFail", "false");
                        saveCreditCardExternalparameters.put("externalSavingPluginId", brainTreePluginId.toString());
                        saveCreditCardExternalparameters.put("contactType", "14");
                        saveCreditCardExternalparameters.put("obscureOnFail", "false");

                        saveCreditCardExternalPlugin.setParameters(saveCreditCardExternalparameters);
                        saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);
                    });
    }

    private TestBuilder configureAchPlugin() {
        return TestBuilder
                .newTest(false)
                .givenForMultiple(
                        envCreator -> {
                            final JbillingAPI api = envCreator.getPrancingPonyApi();

                            List<Integer> accountTypeIds = new ArrayList<>();
                            accountTypeIds.add(migrationAcoountTypeId);
                            accountTypeIds.add(standardAcoountTypeId);

                            PaymentMethodTypeWS temp = api.getPaymentMethodType(14);
                            temp.getAccountTypes().addAll(accountTypeIds);
                            achPaymentMethodTypeId = temp.getId();
                            api.updatePaymentMethodType(temp);

                            if (ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(),
                                    "com.sapienter.jbilling.server.payment.tasks.paypal.PayflowExternalACHTask"))) {
                                pluginIdACH = envCreator
                                        .pluginBuilder(api)
                                        .withCode(PLUGIN_CODE1)
                                        .withTypeId(
                                                api.getPluginTypeWSByClassName(
                                                        "com.sapienter.jbilling.server.payment.tasks.paypal.PayflowExternalACHTask")
                                                        .getId()).withOrder(30)
                                        .withParameter("PayflowEnvironment", "sandbox")
                                        .withParameter("PayflowVendor", "anctpaypal")
                                        .withParameter("PayflowUserId", "clientservices")
                                        .withParameter("PayflowPassword", "jimmy123").build().getId();
                                logger.debug("PayflowExternalACHTask: {}", pluginIdACH);
                            }

                            if (ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(),
                                    "com.sapienter.jbilling.server.payment.tasks.SaveACHExternallyTask"))) {
                                Integer pluginIdSaveACHExternallyTaskID = envCreator
                                        .pluginBuilder(api)
                                        .withCode(PLUGIN_CODE2)
                                        .withTypeId(
                                                api.getPluginTypeWSByClassName(
                                                        "com.sapienter.jbilling.server.payment.tasks.SaveACHExternallyTask")
                                                        .getId()).withOrder(340)
                                        .withParameter("externalSavingPluginId", String.valueOf(pluginIdACH))
                                        .withParameter("obscureOnFail", "1").withParameter("contactType", "14").build()
                                        .getId();

                                logger.debug("SaveACHExternallyTask: {}", pluginIdSaveACHExternallyTaskID);
                            }
                        });
    }

    private TestBuilder getTestEnvironment() {

        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }

    private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
        PluggableTaskWS plugIn = api.getPluginWS(pluginId);
        plugIn.setProcessingOrder(plugIn.getProcessingOrder() + 10);
        plugIn.setParameters(new Hashtable<>(plugIn.getParameters()));
        api.updatePlugin(plugIn);
    }

    @Test(enabled = true)
    public void test001RemoveAchPaymentInstrument() {

        testBuilder.given(
                envBuilder -> {

                    logger.debug("Creating user ...");
                    Calendar nextInvoiceDate = Calendar.getInstance();
                    nextInvoiceDate.set(Calendar.DATE, 1);
                    nextInvoiceDate.add(Calendar.MONTH, 1);

                    Calendar expiry = Calendar.getInstance();
                    expiry.add(Calendar.YEAR, 1);

                    try {
                        PaymentInformationWS ach = createACH("jBiller", "Shire Financial Bank", ACH_ROUTING_NUMBER,
                                ACH_ACCOUNT_NUMBER, Integer.valueOf(1));
                        UserWS user = createUser(nextInvoiceDate.getTime(), true, standardAcoountTypeId, 10, ach);
                        tempUserCode = user.getId();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).test(env -> {
                    int tempInstruments = 0;
                    try {
                        UserWS userWS = api.getUserWS(tempUserCode);
                        tempInstruments = userWS.getPaymentInstruments().size();
                        for (PaymentInformationWS instrument : userWS.getPaymentInstruments()) {
                            if (instrument.getPaymentMethodTypeId() == achPaymentMethodTypeId) {
                                api.removePaymentInstrument(instrument.getId());
                                tempInstruments = tempInstruments - 1;
                            }
                        }
                        assertEquals("Payment Instrument should've been removed", tempInstruments,
                                api.getUserWS(tempUserCode).getPaymentInstruments().size());
                    } finally {
                        api.deleteUser(tempUserCode);
                    }
                });
    }

    @Test(enabled = true)
    public void test002RemoveBrainTreePaymentInstrument() {

        testBuilder.given(envBuilder -> {

            logger.debug("Creating user ...");
            Calendar nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.set(Calendar.DATE, 1);
            nextInvoiceDate.add(Calendar.MONTH, 1);

            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.YEAR, 1);

            try {
                PaymentInformationWS cc = createCreditCard("Frodo Baggins", CREDIT_CARD_NUMBER, expiry.getTime());
                UserWS user = createUser(nextInvoiceDate.getTime(), true, standardAcoountTypeId, 10, cc);
                tempUserCode = user.getId();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).test(env -> {
            int tempInstruments = 0;
            try {
                UserWS userWS = api.getUserWS(tempUserCode);
                tempInstruments = userWS.getPaymentInstruments().size();
                for (PaymentInformationWS instrument : userWS.getPaymentInstruments()) {
                    if (instrument.getPaymentMethodTypeId() == btPaymentMethodTypeId) {
                        api.removePaymentInstrument(instrument.getId());
                        tempInstruments = tempInstruments - 1;
                    }
                }
                assertEquals("Payment Instrument should've been removed", tempInstruments, api.getUserWS(tempUserCode)
                        .getPaymentInstruments().size());
            } finally {
                api.deleteUser(tempUserCode);
            }
        });
    }

    private Integer buildAndPersistPaymentMethodType(String code) {
        List<Integer> accountTypeIds = new ArrayList<>();
        accountTypeIds.add(migrationAcoountTypeId);
        accountTypeIds.add(standardAcoountTypeId);
        PaymentMethodTypeWS paymentMethod = PaymentMethodTypeBuilder.getBuilder(api, environment, code)
                .withMethodName(code).withAccountTypes(accountTypeIds).withTemplateId(1)
                .withMetaFields(getMetafieldList()).isRecurring(true).build();
        List<MetaFieldWS> metaFields = getMetafieldList();
        paymentMethod.setMetaFields(metaFields.toArray(new MetaFieldWS[metaFields.size()]));
        api.updatePaymentMethodType(paymentMethod);
        return paymentMethod.getId();

    }

    private List<MetaFieldWS> getMetafieldList() {
        List<MetaFieldWS> list = new ArrayList<>();
        list.add(buildAndPersistMetafield(CC_MF_CARDHOLDER_NAME, DataType.CHAR, MetaFieldType.TITLE, Integer.valueOf(1)));
        list.add(buildAndPersistMetafield(CC_MF_NUMBER, DataType.CHAR, MetaFieldType.PAYMENT_CARD_NUMBER,
                Integer.valueOf(2)));
        list.add(buildAndPersistMetafield(CC_MF_EXPIRY_DATE, DataType.CHAR, MetaFieldType.DATE, Integer.valueOf(3)));
        list.add(buildAndPersistMetafield(CC_MF_TYPE, DataType.STRING, MetaFieldType.CC_TYPE, Integer.valueOf(4)));
        list.add(buildAndPersistMetafield("autopayment.authorization", DataType.BOOLEAN,
                MetaFieldType.AUTO_PAYMENT_AUTHORIZATION, Integer.valueOf(5)));
        list.add(buildAndPersistMetafield("Country", DataType.STRING, MetaFieldType.COUNTRY_CODE, Integer.valueOf(6)));
        list.add(buildAndPersistMetafield("BT Customer Id", DataType.CHAR, MetaFieldType.GATEWAY_KEY,
                Integer.valueOf(7)));
        return list;
    }

    private MetaFieldWS buildAndPersistMetafield(String name, DataType dataType, MetaFieldType fieldUsage,
            Integer displayOrder) {
        return new MetaFieldBuilder().name(name).dataType(dataType).entityType(EntityType.PAYMENT_METHOD_TYPE)
                .fieldUsage(fieldUsage).displayOrder(displayOrder).build();
    }

    private static PaymentInformationWS createCreditCard(String cardHolderName, String cardNumber, Date date) {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(environment.idForCode(BT_PAYMENT_METHOD_NAME));
        cc.setProcessingOrder(new Integer(1));
        cc.setPaymentMethodId(Integer.valueOf(2));

        List<MetaFieldValueWS> metaFields = new ArrayList<>(5);
        addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
        addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
        addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true, DataType.CHAR, 3, new SimpleDateFormat(
                Constants.CC_DATE_FORMAT).format(date).toCharArray());
        addMetaField(metaFields, "Country", false, false, DataType.STRING, 4, "US");

        // have to pass meta field card type for it to be set
        addMetaField(metaFields, CC_MF_TYPE, false, false, DataType.STRING, 5, CreditCardType.VISA);
        addMetaField(metaFields, "BT Customer Id", false, true, DataType.STRING, 6, null);
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    private static PaymentInformationWS createACH(String customerName, String bankName, String routingNumber,
            String accountNumber, Integer accountType) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(ACH_PM_ID);
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_ACH);
        cc.setProcessingOrder(new Integer(2));

        List<MetaFieldValueWS> metaFields = new ArrayList<>(5);
        addMetaField(metaFields, ACH_MF_ROUTING_NUMBER, false, true, DataType.CHAR, 1, routingNumber.toCharArray());
        addMetaField(metaFields, ACH_MF_CUSTOMER_NAME, false, true, DataType.STRING, 2, customerName);
        addMetaField(metaFields, ACH_MF_ACCOUNT_NUMBER, false, true, DataType.CHAR, 3, accountNumber.toCharArray());
        addMetaField(metaFields, ACH_MF_BANK_NAME, false, true, DataType.STRING, 4, bankName);
        addMetaField(metaFields, ACH_MF_ACCOUNT_TYPE, false, true, DataType.ENUMERATION, 5,
                accountType == 1 ? Constants.ACH_CHECKING : Constants.ACH_SAVING);

        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    private static void addMetaField(List<MetaFieldValueWS> metaFields, String fieldName, boolean disabled,
            boolean mandatory, DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }

    private static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress, Integer accountTypeID,
            Integer groupId, PaymentInformationWS cc) throws JbillingAPIException, IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS newUser = new UserWS();
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        newUser.setUserId(0);
        newUser.setUserName(CUSTOMER_CODE);
        newUser.setPassword("P@ssword12");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(accountTypeID);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(null);
        newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
        newUser.setInvoiceChild(new Boolean(false));

        logger.debug("User properties set");
        metaFieldValues = getMetafieldValues(populateBillingGroupAddress, groupId);
        newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

        logger.debug("Meta field values set");

        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        newUser.getPaymentInstruments().add(cc);

        logger.debug("Creating user ...");
        MainSubscriptionWS billing = new MainSubscriptionWS();
        billing.setPeriodId(2);
        billing.setNextInvoiceDayOfPeriod(1);
        newUser.setMainSubscription(billing);
        newUser.setNextInvoiceDate(nextInvoiceDate);
        newUser.setUserId(api.createUser(newUser));
        logger.debug("User created with id: {}", newUser.getUserId());

        return newUser;
    }

    private static List<MetaFieldValueWS> getMetafieldValues(boolean populateBillingGroupAddress, Integer groupId) {
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("Country");
        metaField1.getMetaField().setDataType(DataType.STRING);
        metaField1.setValue("US");
        metaFieldValues.add(metaField1);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("State/Province");
        metaField2.getMetaField().setDataType(DataType.STRING);
        metaField2.setValue("OR");
        metaFieldValues.add(metaField2);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("First Name");
        metaField3.getMetaField().setDataType(DataType.STRING);
        metaField3.setValue("Frodo");
        metaFieldValues.add(metaField3);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("Last Name");
        metaField4.getMetaField().setDataType(DataType.STRING);
        metaField4.setValue("Baggins");
        metaFieldValues.add(metaField4);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("Address 1");
        metaField5.getMetaField().setDataType(DataType.STRING);
        metaField5.setValue("Baggins");
        metaFieldValues.add(metaField5);

        MetaFieldValueWS metaField6 = new MetaFieldValueWS();
        metaField6.setFieldName("City");
        metaField6.getMetaField().setDataType(DataType.STRING);
        metaField6.setValue("Baggins");
        metaFieldValues.add(metaField6);

        MetaFieldValueWS metaField7 = new MetaFieldValueWS();
        metaField7.setFieldName("Email Address");
        metaField7.getMetaField().setDataType(DataType.STRING);
        metaField7.setValue("test@shire.com");
        metaFieldValues.add(metaField7);

        MetaFieldValueWS metaField8 = new MetaFieldValueWS();
        metaField8.setFieldName("Postal Code");
        metaField8.getMetaField().setDataType(DataType.STRING);
        metaField8.setValue("K0");
        metaFieldValues.add(metaField8);

        if (populateBillingGroupAddress) {

            MetaFieldValueWS metaField9 = new MetaFieldValueWS();
            metaField9.setFieldName("COUNTRY_CODE");
            metaField9.getMetaField().setDataType(DataType.STRING);
            metaField9.setValue("CA");
            metaField9.setGroupId(groupId);
            metaFieldValues.add(metaField9);

            MetaFieldValueWS metaField10 = new MetaFieldValueWS();
            metaField10.setFieldName("STATE_PROVINCE");
            metaField10.getMetaField().setDataType(DataType.STRING);
            metaField10.setValue("OR");
            metaField10.setGroupId(groupId);
            metaFieldValues.add(metaField10);

            MetaFieldValueWS metaField11 = new MetaFieldValueWS();
            metaField11.setFieldName("ORGANIZATION");
            metaField11.getMetaField().setDataType(DataType.STRING);
            metaField11.setValue("Frodo");
            metaField11.setGroupId(groupId);
            metaFieldValues.add(metaField11);

            MetaFieldValueWS metaField12 = new MetaFieldValueWS();
            metaField12.setFieldName("LAST_NAME");
            metaField12.getMetaField().setDataType(DataType.STRING);
            metaField12.setValue("Baggins");
            metaField12.setGroupId(groupId);
            metaFieldValues.add(metaField12);

            MetaFieldValueWS metaField13 = new MetaFieldValueWS();
            metaField13.setFieldName("ADDRESS1");
            metaField13.getMetaField().setDataType(DataType.STRING);
            metaField13.setValue("Baggins");
            metaField13.setGroupId(groupId);
            metaFieldValues.add(metaField13);

            MetaFieldValueWS metaField14 = new MetaFieldValueWS();
            metaField14.setFieldName("CITY");
            metaField14.getMetaField().setDataType(DataType.STRING);
            metaField14.setValue("Baggins");
            metaField14.setGroupId(groupId);
            metaFieldValues.add(metaField14);

            MetaFieldValueWS metaField15 = new MetaFieldValueWS();
            metaField15.setFieldName("BILLING_EMAIL");
            metaField15.getMetaField().setDataType(DataType.STRING);
            metaField15.setValue("test@shire.com");
            metaField15.setGroupId(groupId);
            metaFieldValues.add(metaField15);

            MetaFieldValueWS metaField16 = new MetaFieldValueWS();
            metaField16.setFieldName("POSTAL_CODE");
            metaField16.getMetaField().setDataType(DataType.STRING);
            metaField16.setValue("K0");
            metaField16.setGroupId(groupId);
            metaFieldValues.add(metaField16);

        }
        return metaFieldValues;
    }

    @AfterClass
    private void cleanUp() {
        api.deletePlugin(brainTreePluginId);
        api.deletePlugin(saveCreditCardPluginId);
        updatePaymentMethodType(btPaymentMethodTypeId, "deleted-");
    }

    private void updatePaymentMethodType(Integer paymentMethodTypeId, String prefix) {
        PaymentMethodTypeWS paymentMethodTypeWS = api.getPaymentMethodType(paymentMethodTypeId);
        if (null != paymentMethodTypeWS) {
            String tempName = prefix + System.currentTimeMillis();
            paymentMethodTypeWS.setMethodName(tempName.substring(tempName.length() - 19));
            api.updatePaymentMethodType(paymentMethodTypeWS);
        }
    }
}
